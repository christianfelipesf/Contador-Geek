package com.exemplo.countdownwallpaper

import android.content.Context
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
import android.util.Log
import android.view.SurfaceHolder
import android.view.WindowManager
import java.time.LocalDate
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
    override fun onCreateEngine(): Engine = CountdownEngine()

    private data class Evento(
        val titulo: String,
        val subtitulo: String,
        val data: LocalDate,
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
        const val BANNERS = "banners"       // banners geek no fundo (roxo por cima)
        const val BANNER_MS = "banner_ms"   // tempo parado em cada banner
        const val BANNER_TR = "banner_tr"   // duração da transição entre banners
        const val BANNER_PAN = "banner_pan" // velocidade do pan lateral (px/s, 0 = parado)
        const val BANNER_SHADE = "banner_shade" // cobertura roxa sobre o banner (30..95%)
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

        private val eventos = listOf(
            Evento("VISION QUEST", "Ep 3 e 4 - Disney+", LocalDate.of(2026, 10, 21), "👁", Color.parseColor("#8B5CF6")),
            Evento("ZELDA OOT REMAKE", "Switch 2 - 05/11", LocalDate.of(2026, 11, 5), "🗡️", Color.parseColor("#22C55E")),
            Evento("GTA 6", "PS5 / Xbox - 19/11", LocalDate.of(2026, 11, 19), "🚗", Color.parseColor("#F59E0B")),
            Evento("VINGADORES DOOMSDAY", "Cinema - 18/12", LocalDate.of(2026, 12, 18), "🛡️", Color.parseColor("#EF4444"))
        )

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
        private val cardTex = IntArray(4)
        private val cardRects = Array(4) { RectF() }
        private var cardH = 0f
        private var cardRealH = 0f // altura visual do card (20% menor na vertical)
        private var margin = 0f
        private var topY = 0f
        private val sprHTop = Sprite()
        private val sprHTitle = Sprite()
        private val sprFooter = Sprite()
        private val labels = Array(4) { Sprite() }
        private val subs = Array(4) { Sprite() }
        private val days = Array(4) { Sprite() }
        private var cachedDay: LocalDate? = null
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
            (listOf(sprHTop, sprHTitle, sprFooter) + labels + subs + days)
                .forEach { it.tex = 0 }
            lastSettings = null
            cachedDay = null
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
                (listOf(sprHTop, sprHTitle, sprFooter) + labels + subs + days)
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
            // Bloco de cards mais pra cima e mais próximos: começa em 0.20
            // da altura e ocupa 0.60 no total (antes 0.227 → 0.893).
            // Card 20% menor na vertical, centralizado no mesmo slot.
            margin = wf * 0.148f
            cardH = (hf * 0.60f) / eventos.size
            cardRealH = (cardH - 20f) * 0.8f
            val top = hf * 0.20f
            topY = top
            val cw = (wf - margin * 2f).toInt()
            val ch = cardRealH.toInt()
            eventos.forEachIndexed { i, ev ->
                val y0 = top + i * cardH
                val dy = (cardH - cardRealH) / 2f
                cardRects[i].set(margin, y0 + dy, wf - margin, y0 + dy + cardRealH)
                if (cardTex[i] != 0) GlEngine.deleteTex(cardTex[i])
                val bmp = GlEngine.bakeCardBmp(cw, ch, ev.cor)
                cardTex[i] = GlEngine.uploadBitmap(bmp)
                bmp.recycle()
                replace(
                    labels[i],
                    GlEngine.uploadText(
                        "${ev.emoji}  ${ev.titulo}",
                        (wf * 0.045f).coerceAtMost(cardRealH * 0.27f), Color.WHITE, true
                    )
                )
                replace(
                    subs[i],
                    GlEngine.uploadText(
                        ev.subtitulo,
                        (wf * 0.035f).coerceAtMost(cardRealH * 0.21f),
                        Color.parseColor("#E2E8F0"), false
                    )
                )
            }
            replace(sprHTop, GlEngine.uploadText("FALTAM", wf * 0.038f, Color.parseColor("#DDD6FE"), false))
            replace(sprHTitle, GlEngine.uploadText("COUNTDOWN GEEK", wf * 0.075f, Color.WHITE, true))
            replace(sprFooter, GlEngine.uploadText("Atualiza sozinho todo dia", wf * 0.038f, Color.parseColor("#DDD6FE"), false))
            rebakeDays()
            // (Re)dimensiona o FBO meia-res pra nova superfície.
            try { fbo?.ensure(w, h) } catch (e: Exception) { }
        }

        /** Só os dias (chamado quando o dia vira). Roda na GL thread. */
        private fun rebakeDays() {
            val today = LocalDate.now()
            val wf = layW
            eventos.forEachIndexed { i, ev ->
                val txt = CountdownLogic.textoDias(CountdownLogic.diasFaltam(today, ev.data))
                val size = (if (txt.length > 7) wf * 0.06f else wf * 0.08f)
                    .coerceAtMost(cardRealH * 0.42f)
                replace(days[i], GlEngine.uploadText(txt, size, Color.WHITE, true))
            }
            cachedDay = today
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

        private fun centerY(top: Float, idx: Int) = top + idx * cardH + cardH / 2f

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
            if (s != lastSettings) {
                cTopInt = parseColor(s.cTop, Prefs.DEF_TOP)
                cMidInt = parseColor(s.cMid, Prefs.DEF_MID)
                cBotInt = parseColor(s.cBot, Prefs.DEF_BOT)
                lastSettings = s
            }
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
            val t = (animMs / 1000.0).toFloat() * s.speed
            val stars = s.particles.coerceIn(0, 80).toFloat()

            val today = LocalDate.now()
            if (today != cachedDay) rebakeDays()

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

            val top = topY

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
                val glowY = centerY(top, cur) + (centerY(top, nxt) - centerY(top, cur)) * easeTravel
                val pulse = 0.6f + 0.4f * sin(t * 2.0f)
                val hw = (layW - margin * 2f) / 2f + 18f
                val hh = cardRealH / 2f + 6f
                q.draw(
                    haloTex, cxScreen - hw, shiftY + glowY - hh, hw * 2f, hh * 2f, w, h,
                    196f / 255f, 132f / 255f, 252f / 255f, (70 + 40 * pulse) / 255f
                )
            }

            // Cards + textos (layout original: emoji+título à esquerda, dias à direita)
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
                q.drawWhite(lb.tex, rl + 32f, rt + rh * 0.30f - lb.base, lb.w, lb.h, w, h)
                val sb = subs[i]
                q.drawWhite(sb.tex, rl + 32f, rt + rh * 0.58f - sb.base, sb.w, sb.h, w, h)
                val db = days[i]
                q.drawWhite(db.tex, rl + rw - 28f - db.w, rt + rh * 0.62f - db.base, db.w, db.h, w, h)
            }

            // rodapé
            q.drawWhite(
                sprFooter.tex, cxScreen - sprFooter.w / 2f, layH * 0.965f - sprFooter.base,
                sprFooter.w, sprFooter.h, w, h
            )
        }
    }
}
