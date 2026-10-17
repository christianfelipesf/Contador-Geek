package com.exemplo.countdownwallpaper

import android.content.Context
import android.content.SharedPreferences
import com.exemplo.countdownwallpaper.CountdownWallpaperService.Prefs

/**
 * Snapshot imutável das configs. Lido 1x e cacheado em RAM:
 * zero I/O na thread GL (antes era getSharedPreferences + parseColor por frame).
 */
data class RenderSettings(
    val speed: Float = 1f,
    val targetFrameMs: Long = 33L, // equilíbrio 30fps; 16 = performance
    val particles: Int = 30,
    val hover: Boolean = true,
    val hoverMs: Long = 3000L,
    val hoverTr: Long = 600L,
    val zoom: Boolean = true,
    val showBanners: Boolean = true,
    val bannerMs: Long = 18000L,
    val bannerTr: Long = 1500L,
    val bannerPan: Int = 10,
    val bannerShade: Int = 68,
    val cTop: String = Prefs.DEF_TOP,
    val cMid: String = Prefs.DEF_MID,
    val cBot: String = Prefs.DEF_BOT
)

class PrefsCache(private val appContext: Context) {

    @Volatile var current: RenderSettings = load()
        private set

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)

    private val listener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refresh() }

    init {
        try { prefs.registerOnSharedPreferenceChangeListener(listener) } catch (e: Exception) { }
    }

    fun refresh() {
        current = load()
    }

    fun release() {
        try { prefs.unregisterOnSharedPreferenceChangeListener(listener) } catch (e: Exception) { }
    }

    private fun load(): RenderSettings {
        return try {
            // FRAME_MS antigo (16/33/66/120) migra para target: <=20 => 16 (perf), senão 33/66.
            val rawFrame = try { prefs.getInt(Prefs.FRAME_MS, 33) } catch (e: ClassCastException) { 33 }
            val target = when {
                prefs.contains(Prefs.FRAME_TARGET_MS) -> prefs.getLong(Prefs.FRAME_TARGET_MS, 33L)
                rawFrame <= 20 -> 16L
                rawFrame >= 100 -> 66L
                rawFrame >= 50 -> 50L
                else -> 33L
            }
            RenderSettings(
                speed = prefs.getFloat(Prefs.SPEED, 1f).coerceIn(0.25f, 2f),
                targetFrameMs = target.coerceIn(16L, 100L),
                particles = prefs.getInt(Prefs.PARTICLES, 30).coerceIn(0, 80),
                hover = prefs.getBoolean(Prefs.HOVER, true),
                hoverMs = prefs.getLong(Prefs.HOVER_MS, 3000L).coerceAtLeast(1000L),
                hoverTr = prefs.getLong(Prefs.HOVER_TR, 600L).coerceIn(0L, 2000L),
                zoom = prefs.getBoolean(Prefs.ZOOM, true),
                showBanners = prefs.getBoolean(Prefs.BANNERS, true),
                bannerMs = prefs.getLong(Prefs.BANNER_MS, 18000L).coerceIn(8000L, 30000L),
                bannerTr = prefs.getLong(Prefs.BANNER_TR, 1500L).coerceIn(500L, 3000L),
                bannerPan = prefs.getInt(Prefs.BANNER_PAN, 10).coerceIn(0, 60),
                bannerShade = prefs.getInt(Prefs.BANNER_SHADE, 68).coerceIn(30, 95),
                cTop = prefs.getString(Prefs.C_TOP, Prefs.DEF_TOP) ?: Prefs.DEF_TOP,
                cMid = prefs.getString(Prefs.C_MID, Prefs.DEF_MID) ?: Prefs.DEF_MID,
                cBot = prefs.getString(Prefs.C_BOT, Prefs.DEF_BOT) ?: Prefs.DEF_BOT
            )
        } catch (e: Exception) {
            RenderSettings()
        }
    }
}
