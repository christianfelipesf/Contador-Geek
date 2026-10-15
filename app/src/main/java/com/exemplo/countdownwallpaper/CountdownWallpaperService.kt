package com.exemplo.countdownwallpaper

import android.content.Context
import android.graphics.Color
import android.graphics.RectF
import android.opengl.GLES20
import android.os.Build
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.view.WindowManager
import java.time.LocalDate
import kotlin.math.sin

/**
 * Live wallpaper com fundo 100% em OpenGL ES (fragment shader na GPU).
 * Cards/textos são texturas assadas uma vez e redesenhadas como quads.
 * CPU por frame ~= 0: por isso não trava, mesmo a 30-60fps.
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

    /** Configurações editáveis na tela do app (MainActivity). */
    object Prefs {
        const val FILE = "cw_settings"
        const val SPEED = "speed"           // multiplicador 0.25..2.0
        const val FRAME_MS = "frame_ms"     // intervalo entre frames (~33 = 30fps)
        const val PARTICLES = "particles"   // 0..80 (estrelas no shader)
        const val HOVER = "hover"           // liga/desliga seleção
        const val HOVER_MS = "hover_ms"     // tempo parado em cada card
        const val HOVER_TR = "hover_tr"     // duração da transição entre cards (ms)
        const val ZOOM = "zoom"             // card cresce ao selecionar
        const val C_TOP = "c_top"
        const val C_MID = "c_mid"
        const val C_BOT = "c_bot"
        const val DEF_TOP = "#150826"
        const val DEF_MID = "#6D28D9"
        const val DEF_BOT = "#2E1065"
    }

    inner class CountdownEngine : Engine() {
        private fun settings() =
            this@CountdownWallpaperService.getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)

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

        // ---- ciclo de vida: thread GL própria, EGL criado/destruído nela ----
        @Volatile private var visible = false
        @Volatile private var running = false
        private var holderRef: SurfaceHolder? = null
        private var glThread: Thread? = null
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

        // ---- estado GL (só tocado na thread GL) ----
        private val startTime = SystemClock.uptimeMillis()
        private var gl: GlEngine.GlCore? = null
        private var bg: GlEngine.BgRenderer? = null
        private var quads: GlEngine.QuadRenderer? = null
        private var haloTex = 0
        private val cardTex = IntArray(4)
        private val cardRects = Array(4) { RectF() }
        private var cardH = 0f
        private var margin = 0f
        private var topY = 0f
        private val sprHTop = Sprite()
        private val sprHTitle = Sprite()
        private val sprFooter = Sprite()
        private val labels = Array(4) { Sprite() }
        private val subs = Array(4) { Sprite() }
        private val days = Array(4) { Sprite() }
        private var cachedDay: LocalDate? = null

        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)
            holderRef = holder
            startLoop()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfW = width
            surfH = height
            updateScreenSize()
            needRebake = true
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
            running = false
            try {
                glThread?.join(1500)
            } catch (e: Exception) {
            }
            glThread = null
            holderRef = null
            super.onSurfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
        }

        private fun startLoop() {
            if (running) return
            running = true
            glThread = Thread({ glLoop() }, "cw-gl").apply { start() }
        }

        private fun glLoop() {
            var eglOk = false
            while (running) {
                val surf = holderRef?.surface
                if (surf != null && surf.isValid) {
                    if (!eglOk) {
                        val g = GlEngine.GlCore()
                        if (g.init(surf)) {
                            gl = g
                            bg = GlEngine.BgRenderer().apply { init() }
                            quads = GlEngine.QuadRenderer().apply { init() }
                            val halo = GlEngine.radialBmp(128)
                            haloTex = GlEngine.uploadBitmap(halo)
                            halo.recycle()
                            eglOk = true
                            needRebake = true
                        } else {
                            try {
                                Thread.sleep(500)
                            } catch (e: InterruptedException) {
                            }
                            continue
                        }
                    }
                    if (needRebake && surfW > 0 && surfH > 0) {
                        gl?.makeCurrent()
                        rebakeAll(surfW, surfH)
                        needRebake = false
                    }
                    if (visible && surfW > 0 && surfH > 0) {
                        val t0 = SystemClock.uptimeMillis()
                        gl?.makeCurrent()
                        drawGL()
                        gl?.swap()
                        val frameMs = settings().getInt(Prefs.FRAME_MS, 33).toLong()
                        val wait = frameMs - (SystemClock.uptimeMillis() - t0)
                        if (wait > 0) {
                            try {
                                Thread.sleep(wait)
                            } catch (e: InterruptedException) {
                            }
                        }
                    } else {
                        try {
                            Thread.sleep(250)
                        } catch (e: InterruptedException) {
                        }
                    }
                } else {
                    try {
                        Thread.sleep(250)
                    } catch (e: InterruptedException) {
                    }
                }
            }
            // teardown na própria thread GL
            try {
                gl?.makeCurrent()
                GlEngine.deleteTex(haloTex)
                GlEngine.deleteTex(*cardTex)
                (listOf(sprHTop, sprHTitle, sprFooter) + labels + subs + days)
                    .forEach { GlEngine.deleteTex(it.tex); it.tex = 0 }
                gl?.release()
            } catch (e: Exception) {
            }
            gl = null
            bg = null
            quads = null
        }

        private fun replace(s: Sprite, b: Baked) {
            if (s.tex != 0) GlEngine.deleteTex(s.tex)
            s.tex = b.tex
            s.w = b.w
            s.h = b.h
            s.base = b.base
        }

        /** Reassa cards + textos (tamanho da tela/superfície mudou). */
        private fun rebakeAll(w: Int, h: Int) {
            // Layout no tamanho REAL da tela, não no da superfície (que é mais larga).
            val lw = (if (layoutW > 0) layoutW else w)
            val lh = (if (layoutH > 0) layoutH else h)
            layW = lw.toFloat()
            layH = lh.toFloat()
            val wf = layW
            val hf = layH
            // Cards 20% mais estreitos e 10% mais baixos, centralizados na
            // mesma região de antes (0.19 → 0.93 da altura).
            margin = wf * 0.148f
            cardH = (hf * 0.666f) / eventos.size
            val top = hf * 0.227f
            topY = top
            val cw = (wf - margin * 2f).toInt()
            val ch = (cardH - 20f).toInt()
            eventos.forEachIndexed { i, ev ->
                val y0 = top + i * cardH
                cardRects[i].set(margin, y0 + 10, wf - margin, y0 + cardH - 10)
                if (cardTex[i] != 0) GlEngine.deleteTex(cardTex[i])
                val bmp = GlEngine.bakeCardBmp(cw, ch, ev.cor)
                cardTex[i] = GlEngine.uploadBitmap(bmp)
                bmp.recycle()
                replace(
                    labels[i],
                    GlEngine.uploadText(
                        "${ev.emoji}  ${ev.titulo}",
                        (wf * 0.045f).coerceAtMost(cardH * 0.22f), Color.WHITE, true
                    )
                )
                replace(
                    subs[i],
                    GlEngine.uploadText(
                        ev.subtitulo,
                        (wf * 0.035f).coerceAtMost(cardH * 0.17f),
                        Color.parseColor("#E2E8F0"), false
                    )
                )
            }
            replace(sprHTop, GlEngine.uploadText("FALTAM", wf * 0.038f, Color.parseColor("#DDD6FE"), false))
            replace(sprHTitle, GlEngine.uploadText("COUNTDOWN GEEK", wf * 0.075f, Color.WHITE, true))
            replace(sprFooter, GlEngine.uploadText("Atualiza sozinho todo dia", wf * 0.038f, Color.parseColor("#DDD6FE"), false))
            rebakeDays()
        }

        /** Só os dias (chamado quando o dia vira). */
        private fun rebakeDays() {
            val today = LocalDate.now()
            val wf = layW
            eventos.forEachIndexed { i, ev ->
                val txt = CountdownLogic.textoDias(CountdownLogic.diasFaltam(today, ev.data))
                val size = (if (txt.length > 7) wf * 0.075f else wf * 0.10f)
                    .coerceAtMost((cardH - 20f) * 0.42f)
                replace(days[i], GlEngine.uploadText(txt, size, Color.WHITE, true))
            }
            cachedDay = today
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
            val prefs = settings()
            val speed = prefs.getFloat(Prefs.SPEED, 1f)
            val t = (SystemClock.uptimeMillis() - startTime) / 1000f * speed

            val cTop = parseColor(prefs.getString(Prefs.C_TOP, Prefs.DEF_TOP), Prefs.DEF_TOP)
            val cMid = parseColor(prefs.getString(Prefs.C_MID, Prefs.DEF_MID), Prefs.DEF_MID)
            val cBot = parseColor(prefs.getString(Prefs.C_BOT, Prefs.DEF_BOT), Prefs.DEF_BOT)
            val stars = prefs.getInt(Prefs.PARTICLES, 30).coerceIn(0, 80).toFloat()

            val today = LocalDate.now()
            if (today != cachedDay) rebakeDays()

            GLES20.glViewport(0, 0, surfW, surfH)
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            b.draw(w, h, t, cTop, cMid, cBot, stars)

            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

            val top = topY

            // Título topo (centralizado na janela visível)
            q.drawWhite(sprHTop.tex, cxScreen - sprHTop.w / 2f, layH * 0.10f - sprHTop.base, sprHTop.w, sprHTop.h, w, h)
            q.drawWhite(sprHTitle.tex, cxScreen - sprHTitle.w / 2f, layH * 0.145f - sprHTitle.base, sprHTitle.w, sprHTitle.h, w, h)

            // "hover": um card por vez, de cima pra baixo, estreia sorteada,
            // relógio corrido (sair/voltar não reinicia).
            // Cada ciclo = tempo parado (dwell) + transição deslizando pro próximo.
            val hoverOn = prefs.getBoolean(Prefs.HOVER, true)
            val zoomOn = prefs.getBoolean(Prefs.ZOOM, true)
            val n = eventos.size
            val dwellMs = prefs.getLong(Prefs.HOVER_MS, 3000L).coerceAtLeast(1000L)
            val travelMs = prefs.getLong(Prefs.HOVER_TR, 600L).coerceIn(0L, dwellMs)
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
                val hh = cardH / 2f + 6f
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
                val s = if (zoomOn) 1f + 0.06f * glowHere else 1f
                val cx = base.centerX()
                val cy = base.centerY()
                val rl = shiftX + cx - base.width() * s / 2f
                val rt = shiftY + cy - base.height() * s / 2f
                val rw = base.width() * s
                val rh = base.height() * s
                q.drawWhite(cardTex[i], rl, rt, rw, rh, w, h)

                val lb = labels[i]
                q.drawWhite(lb.tex, rl + 32f, rt + cardH * 0.38f - lb.base, lb.w, lb.h, w, h)
                val sb = subs[i]
                q.drawWhite(sb.tex, rl + 32f, rt + cardH * 0.62f - sb.base, sb.w, sb.h, w, h)
                val db = days[i]
                q.drawWhite(db.tex, rl + rw - 28f - db.w, rt + cardH * 0.65f - db.base, db.w, db.h, w, h)
            }

            // rodapé
            q.drawWhite(
                sprFooter.tex, cxScreen - sprFooter.w / 2f, layH * 0.965f - sprFooter.base,
                sprFooter.w, sprFooter.h, w, h
            )
        }
    }
}
