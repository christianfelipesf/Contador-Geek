package com.exemplo.countdownwallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

class CountdownWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = CountdownEngine()

    private data class Evento(val titulo: String, val subtitulo: String, val data: LocalDate, val emoji: String, val cor: Int)

    inner class CountdownEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false

        // ===== DATAS ALVO - edite aqui se mudar =====
        // Hoje de referência: 15/10/2026

        private val eventos = listOf(
            Evento("VISION QUEST", "Ep 3 e 4 - Disney+", LocalDate.of(2026, 10, 21), "👁", Color.parseColor("#8B5CF6")),
            Evento("ZELDA OOT REMAKE", "Switch 2 - 05/11", LocalDate.of(2026, 11, 5), "🗡️", Color.parseColor("#22C55E")),
            Evento("GTA 6", "PS5 / Xbox - 19/11", LocalDate.of(2026, 11, 19), "🚗", Color.parseColor("#F59E0B")),
            Evento("VINGADORES DOOMSDAY", "Cinema - 18/12", LocalDate.of(2026, 12, 18), "🛡️", Color.parseColor("#EF4444"))
        )

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                // atualiza a cada 1 minuto (60000ms). Suficiente p/ contagem em dias.
                if (visible) handler.postDelayed(this, 60_000)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                drawFrame()
                handler.postDelayed(drawRunnable, 60_000)
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

        private fun draw(c: Canvas) {
            val w = c.width.toFloat()
            val h = c.height.toFloat()

            // Fundo gradiente escuro
            val bg = Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, h,
                    Color.parseColor("#0B1020"),
                    Color.parseColor("#1E1B4B"),
                    Shader.TileMode.CLAMP)
            }
            c.drawRect(0f, 0f, w, h, bg)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = w * 0.075f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CBD5E1")
                textSize = w * 0.038f
                textAlign = Paint.Align.CENTER
            }
            val daysPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = w * 0.11f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
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

            // Título topo
            c.drawText("FALTAM", w / 2, h * 0.10f, subPaint)
            c.drawText("COUNTDOWN GEEK", w / 2, h * 0.145f, titlePaint)

            // Cards
            val top = h * 0.19f
            val cardH = (h * 0.74f) / 4
            val margin = w * 0.06f

            eventos.forEachIndexed { i, ev ->
                val y0 = top + i * cardH
                val rect = RectF(margin, y0 + 10, w - margin, y0 + cardH - 10)
                cardPaint.color = Color.parseColor("#1F2937")
                c.drawRoundRect(rect, 32f, 32f, cardPaint)

                // barra lateral colorida
                val barPaint = Paint().apply { color = ev.cor }
                c.drawRoundRect(RectF(rect.left, rect.top, rect.left + 14f, rect.bottom), 14f, 14f, barPaint)

                val dias = diasFaltam(ev.data)
                val textoDias = when {
                    dias > 1 -> "$dias DIAS"
                    dias == 1L -> "AMANHÃ!"
                    dias == 0L -> "É HOJE!"
                    else -> "JÁ SAIU!"
                }

                // emoji + titulo
                c.drawText("${ev.emoji}  ${ev.titulo}", rect.left + 32f, rect.top + cardH * 0.32f, labelPaint)
                c.drawText(ev.subtitulo, rect.left + 32f, rect.top + cardH * 0.55f, datePaint)

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
