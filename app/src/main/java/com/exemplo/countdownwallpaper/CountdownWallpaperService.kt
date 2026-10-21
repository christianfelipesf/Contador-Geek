package com.exemplo.countdownwallpaper

import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.RectF
import android.opengl.GLES20
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.text.format.DateFormat
import android.util.Log
import android.view.SurfaceHolder
import android.view.WindowManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.sin

/**
 * Live wallpaper — Engine V2 (potente e fluida):
 * - EGL GLES3 com fallback GLES2 ([PowerEgl]), fundo em meia-resolução (FBO)
 *   que corta o fillrate em ~75%.
 * - Vsync de verdade via Choreographer ([VsyncLoop]) com throttle 30fps
 *   equilíbrio (sem Thread.sleep fora de fase).
 * - Zero I/O por frame: [PrefsCache] snapshot em RAM; cores parseadas 1x.
 * - Rebake (bitmaps/texturas) só no resize ou na virada do dia, na GL thread
 *   de fundo — nunca na main.
 */
class CountdownWallpaperService : WallpaperService() {

    private var engineRef: CountdownEngine? = null

    override fun onCreateEngine(): Engine = CountdownEngine().also { engineRef = it }

    /**
     * Idioma do app mudou (Ajustes do sistema, ou Ajustes > Apps > Idioma no
     * Android 13+/One UI 5+): reassa os textos do wallpaper no idioma novo.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        engineRef?.recarregarIdioma()
    }

    /**
     * Eventos com nome/subtítulo/cor vindos dos recursos do idioma atual.
     * A lista (e portanto a QUANTIDADE de cards) sai de [Eventos.TODOS]: os
     * arrays da engine são dimensionados por ela, então adicionar um evento
     * lá não estoura mais nada.
     */
    private fun montarEventos(ctx: Context, inicio: LocalDate): List<Evento> =
        Eventos.TODOS.map { d ->
            val sub = if (d.subComData) {
                ctx.getString(d.sub, dataCurta(ctx, d.data))
            } else {
                ctx.getString(d.sub)
            }
            Evento(
                ctx.getString(d.titulo), sub,
                d.data, inicio, d.emoji, ctx.getColor(d.cor)
            )
        }

    /**
     * Data curta no formato do idioma do sistema, montada pelo próprio Android
     * (sketch "MMMd"): pt "18 dez." / en "Dec 18" / es "18 dic".
     */
    private fun dataCurta(ctx: Context, d: LocalDate): String = try {
        val loc = ctx.resources.configuration.locales[0]
        val padrao = DateFormat.getBestDateTimePattern(loc, "MMMd")
        DateTimeFormatter.ofPattern(padrao, loc).format(d)
    } catch (e: Exception) {
        "%02d/%02d".format(d.dayOfMonth, d.monthValue)
    }

    private data class Evento(
        val titulo: String,
        val subtitulo: String,
        val data: LocalDate,
        val inicio: LocalDate,
        val emoji: String,
        val cor: Int
    )

    private data class Banner(val tex: Int, val w: Float, val h: Float)

    /** Configurações editáveis na tela do app (MainActivity). */
    object Prefs {
        const val FILE = "cw_settings"
        const val SPEED = "speed"           // multiplicador 0.25..2.0
        const val FRAME_MS = "frame_ms"     // legado (migrado p/ FRAME_TARGET_MS)
        const val FRAME_TARGET_MS = "frame_target_ms" // 16 perf / 33 eco 30fps / 50 / 66
        const val PARTICLES = "particles"   // 0..80 (estrelas no shader)
        const val HOVER = "hover"           // liga/desliga seleção
        const val HOVER_MS = "hover_ms"     // tempo parado em cada card
        const val HOVER_TR = "hover_tr"     // duração da transição entre cards (ms)
        const val ZOOM = "zoom"             // card cresce ao selecionar
        const val CARD_SCALE = "card_scale" // tamanho dos cards em % (60..140)
        const val BANNERS = "banners"       // banners geek no fundo (roxo por cima)
        const val BANNER_MS = "banner_ms"   // tempo parado em cada banner
        const val BANNER_TR = "banner_tr"   // duração da transição entre banners
        const val BANNER_PAN = "banner_pan" // velocidade do pan lateral (px/s, 0 = parado)
        const val BANNER_SHADE = "banner_shade" // cobertura roxa sobre o banner (30..95%)
        const val SAVER = "saver"           // modo economizador (mínimo consumo)
        const val C_TOP = "c_top"
        const val C_MID = "c_mid"
        const val C_BOT = "c_bot"
        const val DEF_TOP = "#150826"
        const val DEF_MID = "#6D28D9"
        const val DEF_BOT = "#2E1065"
    }

    inner class CountdownEngine : Engine() {

        // FBO desligado: o fundo volta a renderizar direto em tela cheia
        // (caminho da engine antiga, provado). O FBO de meia-resolução
        // economiza GPU mas virou suspeito nº 1 do preto em tela cheia —
        // ele só é exercitado de verdade na superfície grande aplicada.
        // Para reativar: USE_FBO = true.
        private val USE_FBO = false
        private val TAG = "CountdownWP"

        // ===== DATAS ALVO - edite aqui se mudar =====
        // Hoje de referência: 15/10/2026

        // Início de todas as contagens: é a base do "quanto falta" que
        // escurece o card da esquerda pra direita (1 = tudo, 0 = chegou).
        private val inicioContagem = LocalDate.of(2026, 10, 15)

        // Nome, subtítulo e cor saem dos recursos (strings.xml/colors.xml do
        // idioma atual). Refaz tudo se o idioma do app mudar — ver
        // [recarregarIdioma].
        private var eventos: List<Evento> = montarEventos(appCtx(), inicioContagem)
        private var textos: Textos = TextosRecursos(appCtx())

        private fun appCtx(): Context = this@CountdownWallpaperService.applicationContext

        /**
         * Idioma do app mudou: relê eventos e textos e manda reassert tudo.
         * A ordem importa — `needRebake` é @Volatile e escrito por último, então
         * a thread GL enxerga os campos novos quando começar o rebake.
         */
        fun recarregarIdioma() {
            val ctx = appCtx()
            eventos = montarEventos(ctx, inicioContagem)
            textos = TextosRecursos(ctx)
            needRebake = true
        }

        // Deslocamento aleatório sorteado uma vez e guardado: a estreia começa
        // num card qualquer, e as próximas voltas continuam sem reiniciar.
        private val hoverOffset: Int by lazy {
            val svc = this@CountdownWallpaperService
            val prefs = svc.getSharedPreferences("countdown_hover", Context.MODE_PRIVATE)
            val saved = prefs.getInt("offset_v1", -1)
            if (saved >= 0) saved
            else {
                val r = (0 until eventos.size).random()
                prefs.edit().putInt("offset_v1", r).apply()
                r
            }
        }

        private fun parseColor(s: String?, fb: String): Int = try {
            Color.parseColor(s)
        } catch (e: Exception) {
            Color.parseColor(fb)
        }

        // ---- ciclo de vida ----
        @Volatile private var visible = false
        private var holderRef: SurfaceHolder? = null
        @Volatile private var surfW = 0
        @Volatile private var surfH = 0
        @Volatile private var needRebake = true
        // A superfície do wallpaper costuma ser MAIS LARGA que a tela (rolagem
        // entre páginas). O layout usa o tamanho real da tela; xPix/yPix dizem
        // onde está a janela visível dentro da superfície.
        @Volatile private var xPix = 0
        @Volatile private var yPix = 0
        private var layoutW = 0
        private var layoutH = 0
        private var layW = 0f
        private var layH = 0f

        // ---- threads: main (vsync) + GL (desenho) ----
        private var glThread: HandlerThread? = null
        private var glHandler: Handler? = null
        private val mainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }
        private var vsync: VsyncLoop? = null
        private var prefsCache: PrefsCache? = null

        // ---- estado GL (só tocado na GL thread) ----
        // Tempo de animação ACUMULADO (Double) e só corre enquanto visível.
        // Motivos: (1) fica sempre < 1h, então o float mandado pro shader não
        // perde precisão mesmo com o wallpaper ligado por dias; (2) ao voltar
        // da tela apagada o delta é limitado — sem salto/glitch na animação.
        private var animMs = 0.0
        private var lastFrameMs = 0L
        @Volatile private var resetAnim = true
        private var egl: PowerEgl? = null
        private var eglReady = false
        private var bg: GlEngine.BgRenderer? = null
        private var quads: GlEngine.QuadRenderer? = null
        private var fbo: HalfResFbo? = null
        private var haloTex = 0
        // Tamanho = número de eventos: adicionar um em [Eventos.TODOS] já
        // redimensiona tudo (antes era 4 fixo e um 5º evento estourava o
        // render de vez — IndexOutOfBounds a cada frame, tela preta).
        private val cardTex = IntArray(eventos.size)
        private val cardRects = Array(eventos.size) { RectF() }
        private var cardH = 0f
        private var cardRealH = 0f // altura visual do card (20% menor na vertical)
        // Tamanho em pixels do bitmap do fundo do card (pra reasser a cada hora).
        private var cardPxW = 0
        private var cardPxH = 0
        // Grade do layout: 1 coluna no retrato, 2 no paisagem/tablet deitado.
        private var colunas = 1
        // Respiros internos do card, proporcionais ao tamanho dele (funciona
        // igual num celular pequeno e num tablet).
        private var padEsq = 32f
        private var padDir = 28f
        private var margin = 0f
        private val sprHTop = Sprite()
        private val sprHTitle = Sprite()
        private val sprFooter = Sprite()
        private val labels = Array(eventos.size) { Sprite() }
        private val subs = Array(eventos.size) { Sprite() }
        private val days = Array(eventos.size) { Sprite() }
        private val horas = Array(eventos.size) { Sprite() }
        // Quanto ainda falta de cada card (0..1): largura da sombra escura.
        private val fracRestante = FloatArray(eventos.size) { 1f }
        private var cachedDay: LocalDate? = null
        private var cachedHora = -1
        // Cores parseadas 1x por snapshot (não por frame).
        private var lastSettings: RenderSettings? = null
        private var cTopInt = Color.parseColor(Prefs.DEF_TOP)
        private var cMidInt = Color.parseColor(Prefs.DEF_MID)
        private var cBotInt = Color.parseColor(Prefs.DEF_BOT)

        // ---- banners no fundo (roxo por cima) ----
        private val banners = mutableListOf<Banner>()
        private var bannerOrder = mutableListOf<Int>()
        private var bannerPtr = 0
        private var gradCover = 1f          // 1 = só roxo; ~0.68 = banner visível (ajustável)
        private var coverTarget = 0.68f
        private var dipping = false
        private var lastBannerSwitch = 0L
        private var lastBannerFrame = 0L
        private var panPos = 0.5f          // posição 0..1 dentro da sobra (velocidade constante)
        private var panDir = 1f
        private var panFix = 0.5f           // offset fixo aleatório no eixo sem pan
        private val bannerUv = floatArrayOf(0f, 0f, 1f, 1f)

        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)
            holderRef = holder
            Log.i(TAG, "surfaceCreated")
            ensureThread()
            glHandler?.post {
                try {
                    initGlLocked()
                } catch (e: Exception) {
                    Log.e(TAG, "init falhou", e)
                    eglReady = false
                }
                mainHandler.post { if (visible) vsync?.start() }
            }
            if (visible) vsync?.start()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfW = width
            surfH = height
            Log.i(TAG, "surfaceChanged ${width}x${height}")
            updateScreenSize()
            needRebake = true
            resetAnim = true
            egl?.onSurfaceLost()
        }

        override fun onOffsetsChanged(
            xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float,
            xPixelOffset: Int, yPixelOffset: Int
        ) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset)
            xPix = xPixelOffset
            yPix = yPixelOffset
        }

        @Suppress("DEPRECATION")
        private fun updateScreenSize() {
            try {
                val wm = this@CountdownWallpaperService
                    .getSystemService(Context.WINDOW_SERVICE) as WindowManager
                if (Build.VERSION.SDK_INT >= 30) {
                    val b = wm.currentWindowMetrics.bounds
                    layoutW = b.width()
                    layoutH = b.height()
                } else {
                    val dm = android.util.DisplayMetrics()
                    wm.defaultDisplay.getMetrics(dm)
                    layoutW = dm.widthPixels
                    layoutH = dm.heightPixels
                }
            } catch (e: Exception) {
            }
            if (layoutW <= 0) layoutW = surfW
            if (layoutH <= 0) layoutH = surfH
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            Log.i(TAG, "surfaceDestroyed")
            vsync?.stop()
            val gh = glHandler
            val t = glThread
            if (gh != null && t != null) {
                gh.post {
                    teardownGlLocked()
                    try { t.quitSafely() } catch (e: Exception) { }
                }
            } else {
                try { t?.quitSafely() } catch (e: Exception) { }
            }
            glThread = null
            glHandler = null
            vsync = null
            try { prefsCache?.release() } catch (e: Exception) { }
            prefsCache = null
            holderRef = null
            super.onSurfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            Log.i(TAG, "visibility=$v")
            if (v) {
                // Retoma a animação de onde parou (religa o relógio dos banners
                // no próximo frame) em vez de continuar de um delta gigante.
                resetAnim = true
                prefsCache?.refresh()
                vsync?.start()
            } else {
                vsync?.stop()
            }
        }

        private fun ensureThread() {
            if (glThread != null && glHandler != null && vsync != null) return
            if (prefsCache == null) {
                prefsCache = PrefsCache(this@CountdownWallpaperService.applicationContext)
            }
            if (glThread == null) {
                val t = HandlerThread("power-gl")
                t.start()
                glThread = t
                glHandler = Handler(t.looper)
            }
            if (vsync == null) {
                val gh = glHandler ?: return
                val pc = prefsCache ?: return
                vsync = VsyncLoop(
                    mainHandler = mainHandler,
                    glHandler = gh,
                    settings = { pc.current },
                    onDrawFrame = { drawFrameLocked() }
                )
            }
        }

        /** Roda na GL thread. Cria EGL + renderers. */
        private fun initGlLocked() {
            val surf = holderRef?.surface ?: return
            if (!surf.isValid) {
                Log.w(TAG, "init: surface inválida")
                return
            }
            if (egl == null) egl = PowerEgl()
            if (!eglReady) {
                val ok = egl?.init(surf) == true
                Log.i(TAG, "egl.init=$ok gles3=${egl?.isGles3}")
                if (ok) {
                    eglReady = true
                    createGlResourcesLocked()
                }
            }
        }

        /**
         * (Re)cria programas + texturas no contexto atual. Texturas e
         * programas do contexto antigo morrem junto com ele — por isso
         * recria TUDO a cada init. Sem isso, após perda do contexto
         * (apagar/acender tela, trocar wallpaper) vinha a tela preta.
         */
        private fun createGlResourcesLocked() {
            bg = GlEngine.BgRenderer().apply { init() }
            quads = GlEngine.QuadRenderer().apply { init() }
            if (fbo == null) fbo = HalfResFbo() else fbo?.release()
            loadBannersLocked()
            gradCover = 1f
            coverTarget = 0.68f // default; ajustado por frame via s.bannerShade
            dipping = false
            lastBannerSwitch = 0L
            lastBannerFrame = 0L
            val halo = GlEngine.radialBmp(128)
            haloTex = GlEngine.uploadBitmap(halo)
            halo.recycle()
            // Handles do contexto morto não valem mais: rebake recria.
            for (i in cardTex.indices) cardTex[i] = 0
            (listOf(sprHTop, sprHTitle, sprFooter) + labels + subs + days + horas)
                .forEach { it.tex = 0 }
            lastSettings = null
            cachedDay = null
            cachedHora = -1
            needRebake = true
        }

        /** Roda na GL thread, via VsyncLoop (já no ritmo do vsync). Sem sleep. */
        private var loggedFirstFrame = false

        private fun drawFrameLocked() {
            try {
                if (!visible) return
                val w = surfW
                val h = surfH
                if (w <= 0 || h <= 0) return
                val surf = holderRef?.surface ?: return
                if (!surf.isValid) return
                if (!eglReady) {
                    initGlLocked()
                    if (!eglReady) return
                }
                if (egl?.ensureCurrent() != true) {
                    Log.w(TAG, "makeCurrent falhou; re-init no próximo frame")
                    eglReady = false
                    return
                }
                if (needRebake) {
                    rebakeAll(w, h)
                    needRebake = false
                }
                drawGL()
                if (!loggedFirstFrame) {
                    loggedFirstFrame = true
                    Log.i(TAG, "primeiro frame desenhado ${w}x${h}")
                }
                if (egl?.swap() != true) {
                    // Superfície/contexto morreu: força re-init completo no
                    // próximo frame em vez de ficar no preto congelado.
                    Log.w(TAG, "swap falhou; re-init no próximo frame")
                    eglReady = false
                    egl?.onSurfaceLost()
                }
            } catch (e: Exception) {
                // NUNCA deixar a exceção matar a thread GL (vira preto
                // permanente): loga e tenta re-init no próximo frame.
                Log.e(TAG, "frame falhou", e)
                needRebake = true
                eglReady = false
                egl?.onSurfaceLost()
            }
        }

        /** Roda na GL thread. */
        private fun teardownGlLocked() {
            try {
                egl?.ensureCurrent()
                fbo?.release()
                banners.forEach { try { GlEngine.deleteTex(it.tex) } catch (e: Exception) { } }
                banners.clear()
                bannerOrder.clear()
                GlEngine.deleteTex(haloTex)
                GlEngine.deleteTex(*cardTex)
                (listOf(sprHTop, sprHTitle, sprFooter) + labels + subs + days + horas)
                    .forEach { GlEngine.deleteTex(it.tex); it.tex = 0 }
                egl?.release()
            } catch (e: Exception) {
            }
            egl = null
            eglReady = false
            bg = null
            quads = null
            haloTex = 0
        }

        private fun replace(s: Sprite, b: Baked) {
            if (s.tex != 0) GlEngine.deleteTex(s.tex)
            s.tex = b.tex
            s.w = b.w
            s.h = b.h
            s.base = b.base
        }

        /** Reassa cards + textos (tamanho da tela/superfície mudou). Roda na GL thread. */
        private fun rebakeAll(w: Int, h: Int) {
            // Layout no tamanho REAL da tela, não no da superfície (que é mais larga).
            val lw = (if (layoutW > 0) layoutW else w)
            val lh = (if (layoutH > 0) layoutH else h)
            layW = lw.toFloat()
            layH = lh.toFloat()
            val wf = layW
            val hf = layH

            // ---- layout responsivo ----
            // Retrato (de celular pequeno a tablet em pé): 1 coluna com os 4
            // cards empilhados, como sempre.
            // Paisagem / tablet deitado / dobrável aberto: 2 colunas x 2 linhas —
            // em 1 coluna os cards viram tiras finas e o texto dos dias não caberia.
            // Tudo aqui é proporção da tela, então funciona de 480x800 a 2560x1600.
            val paisagem = wf / hf >= 1.1f
            colunas = if (paisagem) 2 else 1
            val linhas = (eventos.size + colunas - 1) / colunas

            // Tamanho dos cards (60%..140%). A grade é escalada e centralizada
            // na área original, sempre limitada pra caber na tela.
            val escala = ((prefsCache?.current?.cardScale ?: 100) / 100f).coerceIn(0.6f, 1.4f)
            val areaW = wf * (if (paisagem) 0.88f else 0.76f)
            val areaH = hf * (if (paisagem) 0.58f else 0.60f)
            val spanW = (areaW * escala).coerceAtMost(wf * 0.96f)
            val spanH = (areaH * escala).coerceAtMost(hf * 0.72f)
            margin = (wf - spanW) / 2f
            val gapX = if (colunas > 1) wf * 0.03f else 0f
            cardH = spanH / linhas
            cardRealH = (cardH - 20f) * 0.8f
            val top = hf * (if (paisagem) 0.26f else 0.20f) + (areaH - spanH) / 2f
            val cw = ((spanW - gapX * (colunas - 1)) / colunas).toInt()
            val ch = cardRealH.toInt()
            cardPxW = cw
            cardPxH = ch
            // Respiros internos proporcionais (32px e 28px num card de 820).
            padEsq = cw * 0.039f
            padDir = cw * 0.034f
            val dy = (cardH - cardRealH) / 2f
            eventos.forEachIndexed { i, ev ->
                val col = i % colunas
                val lin = i / colunas
                val x0 = margin + col * (cw + gapX)
                val y0 = top + lin * cardH
                cardRects[i].set(x0, y0 + dy, x0 + cw, y0 + dy + cardRealH)
                // Título: encolhe sozinho se passar de 64% da largura do card,
                // pra sempre sobrar espaço pros dias/horas à direita.
                val titulo = "${ev.emoji}  ${ev.titulo}"
                val maxTitulo = cw * 0.64f
                var tSize = (cw * 0.053f).coerceAtMost(cardRealH * 0.25f)
                var tBaked = GlEngine.uploadText(titulo, tSize, Color.WHITE, true)
                if (tBaked.w > maxTitulo && maxTitulo > 40f) {
                    val ajuste = (tSize * maxTitulo / tBaked.w).coerceAtLeast(cw * 0.032f)
                    GlEngine.deleteTex(tBaked.tex)
                    tBaked = GlEngine.uploadText(titulo, ajuste, Color.WHITE, true)
                }
                replace(labels[i], tBaked)
                replace(
                    subs[i],
                    GlEngine.uploadText(
                        ev.subtitulo,
                        (cw * 0.043f).coerceAtMost(cardRealH * 0.19f),
                        appCtx().getColor(R.color.wp_secundario), false
                    )
                )
            }
            // Cabeçalho/rodapé: proporcionais à tela, mas com teto pela altura
            // (num tablet deitado o texto não fica gigante).
            val szCab = minOf(wf * 0.038f, hf * 0.024f)
            val szTitulo = minOf(wf * 0.075f, hf * 0.050f)
            val cabecalho = appCtx().getColor(R.color.wp_cabecalho)
            replace(sprHTop, GlEngine.uploadText(appCtx().getString(R.string.wp_faltam), szCab, cabecalho, false))
            replace(sprHTitle, GlEngine.uploadText(appCtx().getString(R.string.wp_titulo), szTitulo, Color.WHITE, true))
            replace(sprFooter, GlEngine.uploadText(appCtx().getString(R.string.wp_rodape), szCab, cabecalho, false))
            // Contagem + fundo dos cards (o "quanto falta" vai assado neles).
            rebakeContagem(LocalDateTime.now())
            // (Re)dimensiona o FBO meia-res pra nova superfície.
            try { fbo?.ensure(w, h) } catch (e: Exception) { }
        }

        /**
         * Reassa a contagem dos 4 cards: dias (grande), hora fina (pequena) e o
         * fundo com a fração que ainda falta (largura do trecho escuro). Roda na
         * GL thread; chamado quando o dia/hora vira.
         */
        private fun rebakeContagem(agora: LocalDateTime) {
            val hoje = agora.toLocalDate()
            val cardW = cardRects[0].width()
            eventos.forEachIndexed { i, ev ->
                val alvo = ev.data.atStartOfDay()
                val txt = CountdownLogic.textoDias(CountdownLogic.diasFaltam(hoje, ev.data), textos)

                // Tamanho base menor que antes (não encosta no título) e, se
                // ainda sobrar pouco espaço ao lado do nome, encolhe sozinho.
                var size = (if (txt.length > 7) cardPxW * 0.059f else cardPxW * 0.074f)
                    .coerceAtMost(cardRealH * 0.30f)
                var baked = GlEngine.uploadText(txt, size, Color.WHITE, true)
                // largura livre = card - respiro esq - título - respiro - respiro dir
                val livre = cardW - padEsq - labels[i].w - cardPxW * 0.032f - padDir
                if (baked.w > livre) {
                    if (livre < 40f) {
                        // título gigante: melhor não desenhar os dias do que sobrepor
                        GlEngine.deleteTex(baked.tex)
                        if (days[i].tex != 0) {
                            GlEngine.deleteTex(days[i].tex)
                            days[i].tex = 0
                        }
                    } else {
                        val ajustado = (size * livre / baked.w).coerceAtLeast(cardPxW * 0.032f)
                        GlEngine.deleteTex(baked.tex)
                        baked = GlEngine.uploadText(txt, ajustado, Color.WHITE, true)
                        replace(days[i], baked)
                    }
                } else {
                    replace(days[i], baked)
                }

                val fino = CountdownLogic.textoHoras(agora, alvo, textos)
                if (fino.isEmpty()) {
                    if (horas[i].tex != 0) {
                        GlEngine.deleteTex(horas[i].tex)
                        horas[i].tex = 0
                    }
                } else {
                    replace(
                        horas[i],
                        GlEngine.uploadText(
                            fino,
                            (cardPxW * 0.041f).coerceAtMost(cardRealH * 0.18f),
                            appCtx().getColor(R.color.wp_horas), false
                        )
                    )
                }
                fracRestante[i] = CountdownLogic.fracaoRestante(agora, ev.inicio.atStartOfDay(), alvo)
                // O fundo é reassado junto: o escuro começa exatamente onde a
                // contagem parou, já recortado nas quinas do card.
                rebakeCard(i)
            }
            cachedDay = hoje
            cachedHora = agora.hour
        }

        /** (Re)assa o fundo do card: barrinha + escuro do "quanto falta" + traço. */
        private fun rebakeCard(i: Int) {
            if (cardPxW < 2 || cardPxH < 2) return
            if (cardTex[i] != 0) GlEngine.deleteTex(cardTex[i])
            val ev = eventos[i]
            val bmp = GlEngine.bakeCardBmp(cardPxW, cardPxH, ev.cor, fracRestante[i], ev.cor)
            cardTex[i] = GlEngine.uploadBitmap(bmp)
            bmp.recycle()
        }

        /** Carrega os banners de assets/banners (1x por contexto GL). Roda na GL thread. */
        private fun loadBannersLocked() {
            banners.forEach { try { GlEngine.deleteTex(it.tex) } catch (e: Exception) { } }
            banners.clear()
            bannerOrder.clear()
            bannerPtr = 0
            try {
                val am = applicationContext.assets
                val files = am.list("banners")?.sorted()?.filter {
                    it.endsWith(".jpg", true) || it.endsWith(".jpeg", true) ||
                        it.endsWith(".png", true) || it.endsWith(".webp", true)
                } ?: emptyList()
                for (name in files) {
                    try {
                        val data = am.open("banners/$name").use { it.readBytes() }
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
                        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) continue
                        var sample = 1
                        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) > 1024) sample *= 2
                        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                        val bmp = BitmapFactory.decodeByteArray(data, 0, data.size, opts) ?: continue
                        val tex = GlEngine.uploadBitmap(bmp)
                        banners.add(Banner(tex, bmp.width.toFloat(), bmp.height.toFloat()))
                        bmp.recycle()
                    } catch (e: Exception) {
                        Log.w(TAG, "banner $name ignorado: ${e.message}")
                    }
                }
                // Ordem embaralhada pra não ficar enjoativo.
                bannerOrder = banners.indices.toMutableList().also { it.shuffle() }
                randomizePan()
                Log.i(TAG, "banners carregados: ${banners.size}")
            } catch (e: Exception) {
                Log.w(TAG, "sem banners: ${e.message}")
            }
        }

        private fun randomizePan() {
            panPos = Math.random().toFloat()
            panDir = if (Math.random() < 0.5) -1f else 1f
            panFix = Math.random().toFloat()
        }

        private fun advanceBanner() {
            if (banners.isEmpty()) return
            bannerPtr += 1
            if (bannerPtr >= bannerOrder.size) {
                // Reembaralha a cada volta (evita repetir o último/primeiro).
                bannerOrder = banners.indices.toMutableList().also { it.shuffle() }
                bannerPtr = 0
            }
        }

        private fun centroY(idx: Int): Float {
            val r = cardRects[idx]
            return if (r.height() > 0f) r.centerY() else 0f
        }

        private fun drawGL() {
            val q = quads ?: return
            val b = bg ?: return
            val w = surfW.toFloat()
            val h = surfH.toFloat()
            if (w < 1 || h < 1 || layW < 1 || layH < 1) return
            // Janela visível dentro da superfície larga (parallax do launcher).
            val shiftX = xPix.toFloat().coerceIn(0f, (w - layW).coerceAtLeast(0f))
            val shiftY = yPix.toFloat().coerceIn(0f, (h - layH).coerceAtLeast(0f))
            val cxScreen = shiftX + layW / 2f

            // Snapshot em RAM — zero I/O aqui (antes: getSharedPreferences por frame).
            val s = prefsCache?.current ?: RenderSettings()
            val escalaMudou = lastSettings?.cardScale != s.cardScale
            if (s != lastSettings) {
                cTopInt = parseColor(s.cTop, Prefs.DEF_TOP)
                cMidInt = parseColor(s.cMid, Prefs.DEF_MID)
                cBotInt = parseColor(s.cBot, Prefs.DEF_BOT)
                lastSettings = s
            }
            // Tamanho dos cards muda o layout: precisa re-assar os retângulos.
            if (escalaMudou) needRebake = true
            // Tempo acumulado: corre só com o wallpaper visível e tem o delta
            // limitado. Voltar depois de horas com a tela apagada (ou de um
            // travamento) não dá mais salto no gradiente/glows/banners.
            val nowUptime = SystemClock.uptimeMillis()
            if (resetAnim) {
                resetAnim = false
                lastFrameMs = nowUptime
                // Reancora o relógio dos banners: sem pan/crossfade gigantes.
                lastBannerSwitch = 0L
                lastBannerFrame = 0L
                dipping = false
                gradCover = s.bannerShade / 100f
            }
            val dtAnim =
                if (lastFrameMs == 0L) 0L
                else (nowUptime - lastFrameMs).coerceIn(0L, 200L)
            lastFrameMs = nowUptime
            animMs += dtAnim.toDouble()
            if (animMs >= 3_600_000.0) animMs -= 3_600_000.0
            // Economizador: fundo congelado. Um glow pulando a 0,5 fps ficaria
            // feio e ainda acordaria a GPU à toa — parado, o custo é ~zero.
            val t = if (s.saver) 0f else (animMs / 1000.0).toFloat() * s.speed
            val stars = s.particles.coerceIn(0, 80).toFloat()

            val agora = LocalDateTime.now()
            if (agora.toLocalDate() != cachedDay || agora.hour != cachedHora) rebakeContagem(agora)

            // ---- banners: crossfade em ordem aleatória + pan leve ----
            var bTex = 0
            var bUv: FloatArray? = null
            if (s.showBanners && banners.isNotEmpty() && bannerOrder.isNotEmpty()) {
                val now = SystemClock.uptimeMillis()
                if (lastBannerSwitch == 0L) {
                    lastBannerSwitch = now
                    lastBannerFrame = now
                    randomizePan()
                }
                if (!dipping && now - lastBannerSwitch > s.bannerMs) {
                    dipping = true
                    coverTarget = 1f // esconde atrás do roxo puro pra trocar
                }
                if (!dipping) coverTarget = s.bannerShade / 100f // slider aplica ao vivo
                // Delta limitado: um dt enorme (voltando de tela apagada) fazia
                // o pan "pular" pra fora da imagem e o crossfade estourar.
                val dt = (now - lastBannerFrame).coerceIn(0L, 200L)
                lastBannerFrame = now
                val step = dt / s.bannerTr.toFloat().coerceAtLeast(200f)
                gradCover = when {
                    gradCover < coverTarget -> (gradCover + step).coerceAtMost(coverTarget)
                    gradCover > coverTarget -> (gradCover - step).coerceAtLeast(coverTarget)
                    else -> gradCover
                }
                if (dipping && gradCover >= 0.999f) {
                    advanceBanner()
                    randomizePan()
                    lastBannerSwitch = now
                    dipping = false
                    coverTarget = s.bannerShade / 100f // repouso ajustável
                }
                // cover-fit: preenche a tela; se sobrar pra fora do frame,
                // transiciona de lado levemente (pan no eixo com mais sobra,
                // posição inicial aleatória pra não enjoar).
                val bi = banners[bannerOrder[bannerPtr % bannerOrder.size]]
                val scale = max(w / bi.w, h / bi.h)
                val winW = (w / scale / bi.w).coerceIn(0f, 1f)
                val winH = (h / scale / bi.h).coerceIn(0f, 1f)
                val overX = 1f - winW
                val overY = 1f - winH
                // Pan com velocidade ajustável (px da imagem por segundo, igual
                // pra todos os banners): 0 = parado. Ping-pong nas bordas.
                val dtS = dt / 1000f
                val panV = s.bannerPan.toFloat()
                val ox: Float
                val oy: Float
                if (overX >= overY) {
                    val opx = overX * bi.w
                    if (opx > 1f && panV > 0f) {
                        panPos += panDir * (panV * dtS) / opx
                        if (panPos > 1f) { panPos = 2f - panPos; panDir = -1f }
                        else if (panPos < 0f) { panPos = -panPos; panDir = 1f }
                        panPos = panPos.coerceIn(0f, 1f)
                    }
                    ox = overX * panPos
                    oy = overY * panFix
                } else {
                    val opy = overY * bi.h
                    if (opy > 1f && panV > 0f) {
                        panPos += panDir * (panV * dtS) / opy
                        if (panPos > 1f) { panPos = 2f - panPos; panDir = -1f }
                        else if (panPos < 0f) { panPos = -panPos; panDir = 1f }
                        panPos = panPos.coerceIn(0f, 1f)
                    }
                    ox = overX * panFix
                    oy = overY * panPos
                }
                bannerUv[0] = ox
                bannerUv[1] = oy
                bannerUv[2] = winW
                bannerUv[3] = winH
                bTex = bi.tex
                bUv = bannerUv
            }

            // ---- fundo: direto em tela cheia (caminho provado da engine antiga).
            // O FBO de meia-resolução está desligado (USE_FBO) até a causa do
            // preto em tela cheia aplicada ser confirmada isolada.
            var bgDirect = true
            val fb = fbo
            if (USE_FBO && fb != null && fb.ensure(w.toInt(), h.toInt())) {
                if (fb.begin()) {
                    GLES20.glClearColor(0f, 0f, 0f, 1f)
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                    b.draw(fb.halfW(), fb.halfH(), t, cTopInt, cMidInt, cBotInt, stars,
                        bTex, bUv, gradCover)
                    if (fb.endAndUpscale(q, w, h)) bgDirect = false
                }
            }
            if (bgDirect) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                GLES20.glViewport(0, 0, surfW, surfH)
                GLES20.glClearColor(0f, 0f, 0f, 1f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                b.draw(w, h, t, cTopInt, cMidInt, cBotInt, stars,
                    bTex, bUv, gradCover)
                GLES20.glEnable(GLES20.GL_BLEND)
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            }

            // Título topo (centralizado na janela visível)
            q.drawWhite(sprHTop.tex, cxScreen - sprHTop.w / 2f, layH * 0.10f - sprHTop.base, sprHTop.w, sprHTop.h, w, h)
            q.drawWhite(sprHTitle.tex, cxScreen - sprHTitle.w / 2f, layH * 0.145f - sprHTitle.base, sprHTitle.w, sprHTitle.h, w, h)

            // "hover": um card por vez, de cima pra baixo, estreia sorteada,
            // relógio corrido (sair/voltar não reinicia).
            // Cada ciclo = tempo parado (dwell) + transição deslizando pro próximo.
            val hoverOn = s.hover
            val zoomOn = s.zoom
            val n = eventos.size
            val dwellMs = s.hoverMs.coerceAtLeast(1000L)
            val travelMs = s.hoverTr.coerceIn(0L, dwellMs)
            val periodMs = dwellMs + travelMs
            val nowMs = System.currentTimeMillis()
            val stepIdx = (((nowMs / periodMs) + hoverOffset) % n).toInt()
            val within = nowMs % periodMs
            val cur = if (hoverOn) stepIdx else -1
            val nxt = if (hoverOn) (cur + 1) % n else -1
            val travelFrac = if (travelMs > 0) {
                ((within - dwellMs).coerceAtLeast(0L).toFloat() / travelMs).coerceIn(0f, 1f)
            } else 1f
            val easeTravel = travelFrac * travelFrac * (3f - 2f * travelFrac)
            if (hoverOn) {
                val glowY = centroY(cur) + (centroY(nxt) - centroY(cur)) * easeTravel
                val pulse = 0.6f + 0.4f * sin(t * 2.0f)
                val baseAtual = cardRects[cur]
                val hw = baseAtual.width() / 2f + cardPxW * 0.022f
                val hh = baseAtual.height() / 2f + cardPxW * 0.0075f
                q.draw(
                    haloTex, cxScreen - hw, shiftY + glowY - hh, hw * 2f, hh * 2f, w, h,
                    196f / 255f, 132f / 255f, 252f / 255f, (70 + 40 * pulse) / 255f
                )
            }

            // Cards + textos (emoji+título à esquerda; dias + horas à direita).
            // O fundo de cada card já vem com a barrinha colorida e o escuro
            // do "quanto falta" (com o traço marcador) desenhados dentro dele.
            eventos.forEachIndexed { i, _ ->
                val base = cardRects[i]
                val glowHere = when (i) {
                    cur -> 1f - easeTravel
                    nxt -> easeTravel
                    else -> 0f
                }
                val sc = if (zoomOn) 1f + 0.06f * glowHere else 1f
                val cx = base.centerX()
                val cy = base.centerY()
                val rl = shiftX + cx - base.width() * sc / 2f
                val rt = shiftY + cy - base.height() * sc / 2f
                val rw = base.width() * sc
                val rh = base.height() * sc
                q.drawWhite(cardTex[i], rl, rt, rw, rh, w, h)

                val lb = labels[i]
                q.drawWhite(lb.tex, rl + padEsq, rt + rh * 0.30f - lb.base, lb.w, lb.h, w, h)
                val sb = subs[i]
                q.drawWhite(sb.tex, rl + padEsq, rt + rh * 0.58f - sb.base, sb.w, sb.h, w, h)
                val daysSpr = days[i]
                if (daysSpr.tex != 0) {
                    q.drawWhite(
                        daysSpr.tex, rl + rw - padDir - daysSpr.w, rt + rh * 0.46f - daysSpr.base,
                        daysSpr.w, daysSpr.h, w, h
                    )
                }
                val hb = horas[i]
                if (hb.tex != 0) {
                    q.drawWhite(
                        hb.tex, rl + rw - padDir - hb.w, rt + rh * 0.72f - hb.base,
                        hb.w, hb.h, w, h
                    )
                }
            }

            // rodapé
            q.drawWhite(
                sprFooter.tex, cxScreen - sprFooter.w / 2f, layH * 0.965f - sprFooter.base,
                sprFooter.w, sprFooter.h, w, h
            )
        }
    }
}
