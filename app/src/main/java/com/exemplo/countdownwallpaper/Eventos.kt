package com.exemplo.countdownwallpaper

import java.time.LocalDate

/**
 * Datas dos eventos em um único lugar.
 *
 * Antes a lista ficava só no `CountdownWallpaperService`; a prévia da tela de
 * ajustes usava datas fixas antigas (que já tinham passado e apareciam como
 * "0"). Agora as duas leem daqui.
 */
object Eventos {
    val VISION: LocalDate = LocalDate.of(2026, 10, 21)
    val ZELDA: LocalDate = LocalDate.of(2026, 11, 5)
    val GTA: LocalDate = LocalDate.of(2026, 11, 19)
    val DOOMSDAY: LocalDate = LocalDate.of(2026, 12, 18)

    val TODOS: List<LocalDate> = listOf(VISION, ZELDA, GTA, DOOMSDAY)
}
