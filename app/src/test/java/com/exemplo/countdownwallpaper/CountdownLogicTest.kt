package com.exemplo.countdownwallpaper

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownLogicTest {

    @Test
    fun `diasFaltam calcula diferenca em dias`() {
        assertEquals(
            6,
            CountdownLogic.diasFaltam(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 21))
        )
    }

    @Test
    fun `diasFaltam zera no mesmo dia`() {
        val hoje = LocalDate.of(2026, 11, 19)
        assertEquals(0, CountdownLogic.diasFaltam(hoje, hoje))
    }

    @Test
    fun `textoDias plural`() {
        assertEquals("6 DIAS", CountdownLogic.textoDias(6))
    }

    @Test
    fun `textoDias vespera`() {
        assertEquals("AMANHÃ!", CountdownLogic.textoDias(1))
    }

    @Test
    fun `textoDias hoje`() {
        assertEquals("É HOJE!", CountdownLogic.textoDias(0))
    }

    @Test
    fun `textoDias passado`() {
        assertEquals("JÁ SAIU!", CountdownLogic.textoDias(-3))
    }
}
