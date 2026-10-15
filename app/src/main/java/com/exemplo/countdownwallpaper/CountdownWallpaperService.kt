package com.exemplo.countdownwallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.cos
import kotlin.math.sin

class CountdownWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = CountdownEngine()

    private data class Star(val fx: Float, val fy: Float, val r: Float, val sp: Float, val ph: Float)

    private data class Evento(
        val titulo: String,
        val subtitulo: String,
        val data: LocalDate,
        val emoji: String,
        val cor: Int,
        val coverRes: Int
    )

    inner class CountdownEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private val startTime = SystemClock.uptimeMillis()
        private val coverCache = mutableMapOf<Int, Bitmap?>()

        // ===== DATAS ALVO - edite aqui se mudar =====
        // Hoje de referência: 15/10/2026

        private val eventos = listOf(
            Evento("VISION QUEST", "Ep 3 e 4 - Disney+", LocalDate.of(2026, 10, 21), "👁", Color.parseColor("#8B5CF6"), R.drawable.cover_vision),
            Evento("ZELDA OOT REMAKE", "Switch 2 - 05/11", LocalDate.of(2026, 11, 5), "🗡️", Color.parseColor("#22C55E"), R.drawable.cover_zelda),
            Evento("GTA 6", "PS5 / Xbox - 19/11", LocalDate.of(2026, 11, 19), "🚗", Color.parseColor("#F59E0B"), R.drawable.cover_gta6),
            Evento("VINGADORES DOOMSDAY", "Cinema - 18/12", LocalDate.of(2026, 12, 18), "🛡️", Color.parseColor("#EF4444"), R.drawable.cover_avengers)
        )

        // Deslocamento aleatório sorteado uma vez e guardado: a estreia começa
        // num card qualquer, e as próximas voltas continuam sem reiniciar.
        private val hoverOffset: Int by lazy {
            val svc = this@CountdownWallpaperService
            val prefs = svc.getSharedPreferences("countdown_hover", android.content.Context.MODE_PRIVATE)
            val saved = prefs.getInt("offset_v1", -1)
            if (saved >= 0) saved
            else {
                val r = (0 until eventos.size).random()
                prefs.edit().putInt("offset_v1", r).apply()
                r
            }
        }

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                // ~12 fps: animação visível sem torrar bateria
                if (visible) handler.postDelayed(this, 80)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                drawFrame()
                handler.postDelayed(drawRunnable, 80)
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            drawFrame()
        }

        private fun diasFaltam(alvo: LocalDate): Long {
            val hoje = LocalDate.now()
            return ChronoUnit.DAYS.between(hoje, alvo)
        }

        private fun getCover(resId: Int): Bitmap? {
            if (coverCache.containsKey(resId)) return coverCache[resId]
            return try {
                val d = ContextCompat.getDrawable(this@CountdownWallpaperService, resId)
                    ?: return null.also { coverCache[resId] = null }
                val size = 256
                val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                d.setBounds(0, 0, size, size)
                d.draw(canvas)
                coverCache[resId] = bmp
                bmp
            } catch (e: Exception) {
                coverCache[resId] = null
                null
            }
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) draw(canvas)
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
            }
        }

        // ---- Fundo roxo ANIMADO: diagonal que desliza + 2 brilhos orbitando ----
        // Partículas subindo (parece vivo de longe)
        private val stars = List(55) { i ->
            val rnd = kotlin.random.Random(1000 + i)
            Star(rnd.nextFloat(), rnd.nextFloat(), 2f + rnd.nextFloat() * 5f, 0.004f + rnd.nextFloat() * 0.015f, rnd.nextFloat() * 6.28f)
        }

        private fun purple(hue: Float, sat: Float, v: Float, alpha: Int): Int {
            val c = Color.HSVToColor(floatArrayOf(hue, sat, v))
            return Color.argb(alpha, Color.red(c), Color.green(c), Color.blue(c))
        }

        private fun drawAnimatedPurpleBackground(c: Canvas, w: Float, h: Float, t: Float) {
            // matiz roxo oscilando: 265 (violeta) <-> 305 (magenta) — bem perceptível
            val hue = 282f + 22f * sin(t * 0.3f)
            val cTop = purple(hue - 14f, 0.85f, 0.16f, 255)
            val cMid = purple(hue, 0.9f, 0.55f, 255)
            val cBot = purple(hue + 16f, 0.85f, 0.28f, 255)
            val mid = 0.45f + 0.18f * sin(t * 0.3f)
            val base = Paint().apply {
                shader = LinearGradient(
                    0f, 0f,
                    w * (0.5f + 0.5f * sin(t * 0.22f)),
                    h,
                    intArrayOf(cTop, cMid, cBot),
                    floatArrayOf(0f, mid.coerceIn(0.12f, 0.85f), 1f),
                    Shader.TileMode.CLAMP
                )
            }
            c.drawRect(0f, 0f, w, h, base)

            // brilho 1: violeta claro orbitando rápido no topo
            val ox1 = w * (0.5f + 0.38f * sin(t * 0.35f))
            val oy1 = h * (0.25f + 0.14f * cos(t * 0.3f))
            val r1 = w * 0.7f
            val glow1 = Paint().apply {
                shader = RadialGradient(
                    ox1, oy1, r1,
                    intArrayOf(Color.argb(120, 192, 132, 252), Color.TRANSPARENT),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            c.drawCircle(ox1, oy1, r1, glow1)

            // brilho 2: rosa orbitando na base (desencontrado do 1)
            val ox2 = w * (0.5f + 0.38f * cos(t * 0.26f + 1.5f))
            val oy2 = h * (0.72f + 0.14f * sin(t * 0.28f + 0.7f))
            val r2 = w * 0.8f
            val glow2 = Paint().apply {
                shader = RadialGradient(
                    ox2, oy2, r2,
                    intArrayOf(Color.argb(100, 240, 120, 200), Color.TRANSPARENT),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            c.drawCircle(ox2, oy2, r2, glow2)

            // brilho 3: feixe varrendo a tela (vai e volta) — dá o "movimento" óbvio
            val sx = w * (0.5f + 0.55f * sin(t * 0.2f))
            val sweep = Paint().apply {
                shader = LinearGradient(
                    sx - w * 0.35f, 0f, sx + w * 0.35f, 0f,
                    intArrayOf(Color.TRANSPARENT, Color.argb(55, 255, 255, 255), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            c.drawRect(0f, 0f, w, h, sweep)

            // partículas subindo com cintilação
            val dot = Paint(Paint.ANTI_ALIAS_FLAG)
            for (s in stars) {
                val yy = ((s.fy - t * s.sp) % 1f + 1f) % 1f
                val tw = 0.5f + 0.5f * sin(t * 2.2f + s.ph)
                dot.color = Color.argb((60 + 140 * tw).toInt(), 230, 220, 255)
                c.drawCircle(s.fx * w, yy * h, s.r, dot)
            }
        }

        private fun drawCover(c: Canvas, bmp: Bitmap?, dst: RectF, radius: Float) {
            if (bmp == null) return
            val path = Path().apply { addRoundRect(dst, radius, radius, Path.Direction.CW) }
            c.save()
            c.clipPath(path)
            val bw = bmp.width.toFloat()
            val bh = bmp.height.toFloat()
            val scale = maxOf(dst.width() / bw, dst.height() / bh)
            val dw = bw * scale
            val dh = bh * scale
            val left = dst.centerX() - dw / 2f
            val top = dst.centerY() - dh / 2f
            c.drawBitmap(bmp, null, RectF(left, top, left + dw, top + dh), Paint(Paint.FILTER_BITMAP_FLAG))
            c.restore()
        }

        private fun draw(c: Canvas) {
            val w = c.width.toFloat()
            val h = c.height.toFloat()
            val t = (SystemClock.uptimeMillis() - startTime) / 1000f

            drawAnimatedPurpleBackground(c, w, h, t * 0.65f)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = w * 0.075f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#DDD6FE")
                textSize = w * 0.038f
                textAlign = Paint.Align.CENTER
            }
            val daysPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = w * 0.11f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(200, 30, 16, 60)
            }
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = w * 0.045f
                isFakeBoldText = true
                textAlign = Paint.Align.LEFT
            }
            val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2E8F0")
                textSize = w * 0.035f
                textAlign = Paint.Align.LEFT
            }
            val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = w * 0.09f
                textAlign = Paint.Align.CENTER
            }

            // Título topo
            c.drawText("FALTAM", w / 2, h * 0.10f, subPaint)
            c.drawText("COUNTDOWN GEEK", w / 2, h * 0.145f, titlePaint)

            // Cards
            val top = h * 0.19f
            val cardH = (h * 0.74f) / 4
            val margin = w * 0.06f

            // "hover" que seleciona um card por vez, de cima pra baixo (5s cada).
            // Relógio corrido + deslocamento sorteado: sair/voltar da tela
            // inicial NÃO reinicia a seleção de volta pro 1º.
            val n = eventos.size
            val HOVER_MS = 7000L
            val nowMs = System.currentTimeMillis()
            val stepMod = (((nowMs / HOVER_MS) + hoverOffset) % n).toInt()
            val frac = ((nowMs % HOVER_MS).toFloat() / HOVER_MS).coerceIn(0f, 1f)
            val ease = frac * frac * (3f - 2f * frac)
            val cur = stepMod
            val prev = ((stepMod - 1 + n) % n)
            fun centerY(idx: Int) = top + idx * cardH + cardH / 2f
            val glowY = centerY(prev) + (centerY(cur) - centerY(prev)) * ease
            val pulse = 0.6f + 0.4f * sin(t * 1.2f)
            // bolha de luz atrás do card selecionado, deslizando junto
            val hg = Paint().apply {
                shader = RadialGradient(
                    w / 2f, glowY, w * 0.75f,
                    intArrayOf(Color.argb((90 + 50 * pulse).toInt(), 196, 132, 252), Color.TRANSPARENT),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            c.drawCircle(w / 2f, glowY, w * 0.75f, hg)

            eventos.forEachIndexed { i, ev ->
                val y0 = top + i * cardH
                val base = RectF(margin, y0 + 10, w - margin, y0 + cardH - 10)

                // "hover": o selecionado cresce animado (sem borda)
                val glowHere = when (i) {
                    cur -> ease
                    prev -> 1f - ease
                    else -> 0f
                }
                val s = 1f + 0.06f * glowHere
                val rect = RectF(
                    base.centerX() - base.width() * s / 2f,
                    base.centerY() - base.height() * s / 2f,
                    base.centerX() + base.width() * s / 2f,
                    base.centerY() + base.height() * s / 2f
                )
                c.drawRoundRect(rect, 32f, 32f, cardPaint)

                // barra lateral colorida
                val barPaint = Paint().apply { color = ev.cor }
                c.drawRoundRect(RectF(rect.left, rect.top, rect.left + 14f, rect.bottom), 14f, 14f, barPaint)

                // miniatura da capa (foto) à esquerda
                val thumbSize = cardH - 44f
                val thumbRect = RectF(rect.left + 28f, rect.top + 22f, rect.left + 28f + thumbSize, rect.top + 22f + thumbSize)
                val bmp = getCover(ev.coverRes)
                if (bmp != null) {
                    drawCover(c, bmp, thumbRect, 20f)
                } else {
                    c.drawText(ev.emoji, thumbRect.centerX(), thumbRect.centerY() + emojiPaint.textSize * 0.35f, emojiPaint)
                }

                val dias = diasFaltam(ev.data)
                val textoDias = when {
                    dias > 1 -> "$dias DIAS"
                    dias == 1L -> "AMANHÃ!"
                    dias == 0L -> "É HOJE!"
                    else -> "JÁ SAIU!"
                }

                val textX = thumbRect.right + 20f
                c.drawText(ev.titulo, textX, rect.top + cardH * 0.32f, labelPaint)
                c.drawText(ev.subtitulo, textX, rect.top + cardH * 0.55f, datePaint)

                // dias grandes à direita
                val oldAlign = daysPaint.textAlign
                daysPaint.textAlign = Paint.Align.RIGHT
                daysPaint.textSize = if (textoDias.length > 7) w * 0.075f else w * 0.10f
                c.drawText(textoDias, rect.right - 28f, rect.top + cardH * 0.65f, daysPaint)
                daysPaint.textAlign = oldAlign
            }

            // rodapé
            c.drawText("Atualiza sozinho todo dia", w / 2, h * 0.965f, subPaint)
        }
    }
}
