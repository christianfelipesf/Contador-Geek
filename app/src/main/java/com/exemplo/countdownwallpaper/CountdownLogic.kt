package com.exemplo.countdownwallpaper

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Lógica pura da contagem (sem Android) — coberta por teste unitário. */
object CountdownLogic {
    fun diasFaltam(hoje: LocalDate, alvo: LocalDate): Long =
        ChronoUnit.DAYS.between(hoje, alvo)

    fun textoDias(dias: Long): String = when {
        dias > 1 -> "$dias DIAS"
        dias == 1L -> "AMANHÃ!"
        dias == 0L -> "É HOJE!"
        else -> "JÁ SAIU!"
    }
}
