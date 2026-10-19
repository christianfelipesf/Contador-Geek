package com.exemplo.countdownwallpaper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import com.exemplo.countdownwallpaper.CountdownWallpaperService.Prefs
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

/**
 * Mini-preview ao vivo do wallpaper (Canvas 2D, aproximado):
 * gradiente + banner com cover-fit/pan/crossfade + overlay roxo + mock dos cards.
 * Reage aos sliders na hora (listener de prefs). Roda só com a tela visível.
 */
class BannerPreviewView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {

    private val prefs: SharedPreferences =
        ctx.getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)

    private var bitmaps: List<Bitmap> = emptyList()
    private var order = mutableListOf<Int>()
    private var ptr = 0
    private var switchAt = 0L
    private var dipping = false
    private var cover = 1f
    private var panPos = 0.5f
    private var panDir = 1f
    private var panFix = 0.5f
    private var lastT = 0L
    private var running = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val overlayPaint = Paint()
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(190, 24, 12, 46)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C4B5FD")
        textAlign = Paint.Align.CENTER
    }
    private val daysPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    // (rótulo curto, data-alvo) — mesmas datas do wallpaper (Eventos.kt).
    private val mockEvents = listOf(
        "VISION QUEST" to Eventos.VISION,
        "ZELDA" to Eventos.ZELDA,
        "GTA 6" to Eventos.GTA,
        "DOOMSDAY" to Eventos.DOOMSDAY
    )

    private val prefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> invalidate() }

    private val ticker = object : Runnable {
        override fun run() {
            if (!running) return
            invalidate()
            postOnAnimation(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        loadBitmaps()
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        panPos = Math.random().toFloat()
        panDir = if (Math.random() < 0.5) -1f else 1f
        panFix = Math.random().toFloat()
        iniciarTicker()
    }

    override fun onDetachedFromWindow() {
        pararTicker()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        bitmaps.forEach { it.recycle() }
        bitmaps = emptyList()
        super.onDetachedFromWindow()
    }

    /**
     * A prévia é animada; sem isso ela continuava desenhando (e gastando
     * bateria) quando o app ia para segundo plano ou o diálogo era coberto.
     */
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == View.VISIBLE) iniciarTicker() else pararTicker()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == View.VISIBLE) iniciarTicker() else pararTicker()
    }

    private fun iniciarTicker() {
        if (running || !isAttachedToWindow || windowVisibility != View.VISIBLE) return
        running = true
        lastT = SystemClock.uptimeMillis()
        switchAt = lastT
        removeCallbacks(ticker)
        postOnAnimation(ticker)
    }

    private fun pararTicker() {
        running = false
        removeCallbacks(ticker)
    }

    private fun loadBitmaps() {
        try {
            val files = context.assets.list("banners")?.sorted()?.filter {
                it.endsWith(".jpg") || it.endsWith(".png")
            } ?: return
            bitmaps = files.mapNotNull {
                try {
                    context.assets.open("banners/$it").use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } catch (e: Exception) { null }
            }
            order = bitmaps.indices.shuffled().toMutableList()
        } catch (e: Exception) { /* sem preview de banner */ }
    }

    private fun colorPref(key: String, def: String): Int {
        return try {
            Color.parseColor(prefs.getString(key, def) ?: def)
        } catch (e: Exception) {
            Color.parseColor(def)
        }
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val now = SystemClock.uptimeMillis()
        val dt = (now - lastT).coerceAtLeast(0L)
        lastT = now
        val dtS = dt / 1000f

        val showB = prefs.getBoolean(Prefs.BANNERS, true)
        val shade = prefs.getInt(Prefs.BANNER_SHADE, 68).coerceIn(30, 95) / 100f
        val bMs = prefs.getLong(Prefs.BANNER_MS, 18000L).coerceIn(8000L, 30000L)
        val bTr = prefs.getLong(Prefs.BANNER_TR, 1500L).coerceIn(500L, 3000L)
        val panV = prefs.getInt(Prefs.BANNER_PAN, 10).coerceIn(0, 60).toFloat()

        // ---- base: gradiente topo/meio/base (mesmas cores do wallpaper) ----
        val top = colorPref(Prefs.C_TOP, Prefs.DEF_TOP)
        val mid = colorPref(Prefs.C_MID, Prefs.DEF_MID)
        val bot = colorPref(Prefs.C_BOT, Prefs.DEF_BOT)
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(top, mid, bot), floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // ---- banner: mesma lógica do engine (dip -> troca -> repouso no shade) ----
        if (showB && bitmaps.isNotEmpty() && order.isNotEmpty()) {
            if (!dipping && now - switchAt > bMs) dipping = true
            val target = if (dipping) 1f else shade
            val step = dt / bTr.toFloat().coerceAtLeast(200f)
            cover = when {
                cover < target -> (cover + step).coerceAtMost(target)
                cover > target -> (cover - step).coerceAtLeast(target)
                else -> cover
            }
            if (dipping && cover >= 0.999f) {
                ptr = (ptr + 1) % order.size
                panPos = Math.random().toFloat()
                panDir = if (Math.random() < 0.5) -1f else 1f
                panFix = Math.random().toFloat()
                switchAt = now
                dipping = false
            }
            val bm = bitmaps[order[ptr % order.size]]
            val bw = bm.width.toFloat()
            val bh = bm.height.toFloat()
            if (bw > 0 && bh > 0) {
                val scale = max(w / bw, h / bh)
                val winW = (w / scale / bw).coerceIn(0f, 1f)
                val winH = (h / scale / bh).coerceIn(0f, 1f)
                val overX = 1f - winW
                val overY = 1f - winH
                val ox: Float
                val oy: Float
                if (overX >= overY) {
                    val opx = overX * bw
                    if (opx > 1f && panV > 0f) {
                        panPos += panDir * (panV * dtS) / opx
                        if (panPos > 1f) { panPos = 2f - panPos; panDir = -1f }
                        else if (panPos < 0f) { panPos = -panPos; panDir = 1f }
                    }
                    ox = overX * panPos
                    oy = overY * panFix
                } else {
                    val opy = overY * bh
                    if (opy > 1f && panV > 0f) {
                        panPos += panDir * (panV * dtS) / opy
                        if (panPos > 1f) { panPos = 2f - panPos; panDir = -1f }
                        else if (panPos < 0f) { panPos = -panPos; panDir = 1f }
                    }
                    ox = overX * panFix
                    oy = overY * panPos
                }
                val src = Rect(
                    (ox * bw).toInt(), (oy * bh).toInt(),
                    ((ox + winW) * bw).toInt(), ((oy + winH) * bh).toInt()
                )
                c.drawBitmap(bm, src, RectF(0f, 0f, w, h), null)
                // dim 0.6x + overlay do gradiente (igual ao shader: mix(bc, col, cover))
                overlayPaint.shader = null
                overlayPaint.color = Color.argb(102, 0, 0, 0)
                c.drawRect(0f, 0f, w, h, overlayPaint)
                overlayPaint.shader = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(top, mid, bot), floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
                overlayPaint.alpha = (cover * 255).toInt().coerceIn(0, 255)
                c.drawRect(0f, 0f, w, h, overlayPaint)
            }
        }

        // ---- mock dos cards 2x2 ----
        if (mockEvents.isNotEmpty()) {
            val today = LocalDate.now()
            val gap = w * 0.03f
            val cardW = (w - gap * 3f) / 2f
            val cardH = h * 0.30f
            val topY = h * 0.34f
            labelPaint.textSize = cardW * 0.09f
            daysPaint.textSize = cardW * 0.22f
            mockEvents.take(4).forEachIndexed { i, (label, date) ->
                val cx = gap + (i % 2) * (cardW + gap)
                val cy = topY + (i / 2) * (cardH + gap)
                c.drawRoundRect(RectF(cx, cy, cx + cardW, cy + cardH), 18f, 18f, cardPaint)
                val days = ChronoUnit.DAYS.between(today, date).coerceAtLeast(0)
                c.drawText(label, cx + cardW / 2f, cy + cardH * 0.30f, labelPaint)
                c.drawText(days.toString(), cx + cardW / 2f, cy + cardH * 0.72f, daysPaint)
            }
        }
    }
}
