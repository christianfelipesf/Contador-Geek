package com.exemplo.countdownwallpaper

import java.time.LocalDate
import java.time.LocalDateTime
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

    @Test
    fun `textoHoras sobrando horas em varios dias`() {
        assertEquals(
            "12h",
            CountdownLogic.textoHoras(
                LocalDateTime.of(2026, 10, 17, 12, 0),
                LocalDateTime.of(2026, 12, 18, 0, 0)
            )
        )
    }

    @Test
    fun `textoHoras na vespera`() {
        assertEquals(
            "7h",
            CountdownLogic.textoHoras(
                LocalDateTime.of(2026, 10, 20, 16, 28),
                LocalDateTime.of(2026, 10, 21, 0, 0)
            )
        )
    }

    @Test
    fun `textoHoras vazio quando passou`() {
        assertEquals(
            "",
            CountdownLogic.textoHoras(
                LocalDateTime.of(2026, 10, 22, 0, 0),
                LocalDateTime.of(2026, 10, 21, 0, 0)
            )
        )
    }

    @Test
    fun `fracaoRestante no inicio da janela`() {
        assertEquals(
            1f,
            CountdownLogic.fracaoRestante(
                LocalDateTime.of(2026, 10, 15, 0, 0),
                LocalDateTime.of(2026, 10, 15, 0, 0),
                LocalDateTime.of(2026, 10, 21, 0, 0)
            ),
            0.001f
        )
    }

    @Test
    fun `fracaoRestante na metade da janela`() {
        assertEquals(
            0.5f,
            CountdownLogic.fracaoRestante(
                LocalDateTime.of(2026, 10, 18, 0, 0),
                LocalDateTime.of(2026, 10, 15, 0, 0),
                LocalDateTime.of(2026, 10, 21, 0, 0)
            ),
            0.001f
        )
    }

    @Test
    fun `fracaoRestante zera no alvo e nao passa`() {
        assertEquals(
            0f,
            CountdownLogic.fracaoRestante(
                LocalDateTime.of(2026, 10, 25, 0, 0),
                LocalDateTime.of(2026, 10, 15, 0, 0),
                LocalDateTime.of(2026, 10, 21, 0, 0)
            ),
            0.001f
        )
    }

    @Test
    fun `fracaoRestante antes da janela fica em 1`() {
        assertEquals(
            1f,
            CountdownLogic.fracaoRestante(
                LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 10, 15, 0, 0),
                LocalDateTime.of(2026, 10, 21, 0, 0)
            ),
            0.001f
        )
    }

    /** Textos injetados (estilo inglês): prova que a lógica não tem idioma fixo. */
    private object TextosEn : Textos {
        override fun dias(n: Long) = "$n DAYS"
        override fun amanha() = "TOMORROW!"
        override fun hoje() = "IT'S TODAY!"
        override fun jaSaiu() = "OUT NOW!"
        override fun horas(horas: Long) = "${horas}h"
    }

    @Test
    fun `textoDias usa o idioma injetado`() {
        assertEquals("5 DAYS", CountdownLogic.textoDias(5, TextosEn))
        assertEquals("TOMORROW!", CountdownLogic.textoDias(1, TextosEn))
        assertEquals("IT'S TODAY!", CountdownLogic.textoDias(0, TextosEn))
        assertEquals("OUT NOW!", CountdownLogic.textoDias(-2, TextosEn))
    }

    @Test
    fun `textoHoras usa o idioma injetado`() {
        assertEquals(
            "12h",
            CountdownLogic.textoHoras(
                LocalDateTime.of(2026, 10, 17, 12, 0),
                LocalDateTime.of(2026, 12, 18, 0, 0),
                TextosEn
            )
        )
    }

    @Test
    fun `sem injecao continua no padrao embutido`() {
        assertEquals("3 DIAS", CountdownLogic.textoDias(3, TextosPadrao))
        assertEquals("É HOJE!", CountdownLogic.textoDias(0, TextosPadrao))
    }
}
