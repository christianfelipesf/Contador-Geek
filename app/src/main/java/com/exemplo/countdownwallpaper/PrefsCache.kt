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
    val hoverMs: Long = 9000L, // tempo parado por card (padrão 9s)
    val hoverTr: Long = 600L,
    val zoom: Boolean = true,
    val cardScale: Int = 100, // tamanho dos cards em % (60..140)
    val showBanners: Boolean = true,
    val bannerMs: Long = 18000L,
    val bannerTr: Long = 1500L,
    val bannerPan: Int = 10,
    val bannerShade: Int = 68,
    val cTop: String = Prefs.DEF_TOP,
    val cMid: String = Prefs.DEF_MID,
    val cBot: String = Prefs.DEF_BOT,
    val saver: Boolean = false // modo economizador
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
            val base = RenderSettings(
                speed = prefs.getFloat(Prefs.SPEED, 1f).coerceIn(0.25f, 2f),
                targetFrameMs = target.coerceIn(16L, 100L),
                particles = prefs.getInt(Prefs.PARTICLES, 30).coerceIn(0, 80),
                hover = prefs.getBoolean(Prefs.HOVER, true),
                hoverMs = prefs.getLong(Prefs.HOVER_MS, 9000L).coerceAtLeast(1000L),
                hoverTr = prefs.getLong(Prefs.HOVER_TR, 600L).coerceIn(0L, 2000L),
                zoom = prefs.getBoolean(Prefs.ZOOM, true),
                cardScale = prefs.getInt(Prefs.CARD_SCALE, 100).coerceIn(60, 140),
                showBanners = prefs.getBoolean(Prefs.BANNERS, true),
                bannerMs = prefs.getLong(Prefs.BANNER_MS, 18000L).coerceIn(8000L, 30000L),
                bannerTr = prefs.getLong(Prefs.BANNER_TR, 1500L).coerceIn(500L, 3000L),
                bannerPan = prefs.getInt(Prefs.BANNER_PAN, 10).coerceIn(0, 60),
                bannerShade = prefs.getInt(Prefs.BANNER_SHADE, 68).coerceIn(30, 95),
                cTop = prefs.getString(Prefs.C_TOP, Prefs.DEF_TOP) ?: Prefs.DEF_TOP,
                cMid = prefs.getString(Prefs.C_MID, Prefs.DEF_MID) ?: Prefs.DEF_MID,
                cBot = prefs.getString(Prefs.C_BOT, Prefs.DEF_BOT) ?: Prefs.DEF_BOT,
                saver = prefs.getBoolean(Prefs.SAVER, false)
            )
            if (base.saver) economia(base) else base
        } catch (e: Exception) {
            RenderSettings()
        }
    }

    /**
     * Modo economizador: sobrepõe os valores SÓ no snapshot (nada é gravado),
     * então desligar volta exatamente pras configs que o usuário escolheu.
     * - ~0,5 fps: praticamente só redesenha quando o dia vira;
     * - zero estrelas (o hash por pixel é o maior custo do shader);
     * - sem destaque/zoom animado dos cards;
     * - banner parado, sem pan e sem troca.
     */
    private fun economia(base: RenderSettings): RenderSettings = base.copy(
        targetFrameMs = 2000L,
        particles = 0,
        hover = false,
        zoom = false,
        bannerPan = 0,
        bannerTr = 500L,
        bannerMs = Long.MAX_VALUE / 4
    )
}
