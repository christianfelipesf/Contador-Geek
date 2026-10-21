package com.exemplo.countdownwallpaper

import java.time.LocalDate

/**
 * Datas dos eventos em um único lugar.
 *
 * Antes a lista ficava espalhada: as datas soltas aqui, o título/subtítulo/cor
 * chumbados no `CountdownWallpaperService` e a prévia com uma lista própria.
 * Agora o wallpaper e a prévia leem [TODOS] — a quantidade de cards sai daqui,
 * então acrescentar um evento não exige mexer em mais nada além das strings.
 *
 * PRA ADICIONAR UM EVENTO:
 *  1. acrescente um [Def] em [TODOS];
 *  2. crie as strings `ev_<id>_titulo` / `ev_<id>_sub` nos três values*
 *     (values / values-pt / values-es) e a cor `ev_<id>` em colors.xml.
 * O layout, o rebake e a prévia já se ajustam sozinhos ao tamanho da lista.
 */
object Eventos {
    val VISION: LocalDate = LocalDate.of(2026, 10, 21)
    val ZELDA: LocalDate = LocalDate.of(2026, 11, 5)
    val GTA: LocalDate = LocalDate.of(2026, 11, 19)
    val DOOMSDAY: LocalDate = LocalDate.of(2026, 12, 18)

    /**
     * Um card do wallpaper.
     * [subComData] diz se o subtítulo recebe a data curta (no formato do idioma
     * do sistema) — o do Vision não tem data, por isso o flag.
     */
    data class Def(
        val data: LocalDate,
        val emoji: String,
        val titulo: Int,
        val sub: Int,
        val subComData: Boolean,
        val cor: Int
    )

    val TODOS: List<Def> = listOf(
        Def(VISION, "👁", R.string.ev_vision_titulo, R.string.ev_vision_sub, false, R.color.ev_vision),
        Def(ZELDA, "🗡️", R.string.ev_zelda_titulo, R.string.ev_zelda_sub, true, R.color.ev_zelda),
        Def(GTA, "🚗", R.string.ev_gta_titulo, R.string.ev_gta_sub, true, R.color.ev_gta),
        Def(DOOMSDAY, "🛡️", R.string.ev_doomsday_titulo, R.string.ev_doomsday_sub, true, R.color.ev_doomsday)
    )
}
