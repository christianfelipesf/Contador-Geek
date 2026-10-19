package com.exemplo.countdownwallpaper

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Lógica pura da contagem (sem Android) — coberta por teste unitário.
 * Os textos vêm de fora ([Textos]): no app são os strings.xml do idioma atual.
 */
object CountdownLogic {
    fun diasFaltam(hoje: LocalDate, alvo: LocalDate): Long =
        ChronoUnit.DAYS.between(hoje, alvo)

    fun textoDias(dias: Long, textos: Textos = TextosPadrao): String = when {
        dias > 1 -> textos.dias(dias)
        dias == 1L -> textos.amanha()
        dias == 0L -> textos.hoje()
        else -> textos.jaSaiu()
    }

    /**
     * Horas que sobram ALÉM dos dias inteiros já mostrados: "12h".
     * Usa a meia-noite do dia alvo como referência, então a conta fecha com
     * o texto de dias (nada de "3 dias" em cima e "3d 7h" embaixo).
     * Devolve "" quando não sobra hora ou a data já passou.
     */
    fun textoHoras(
        agora: LocalDateTime,
        alvoMeiaNoite: LocalDateTime,
        textos: Textos = TextosPadrao
    ): String {
        val minutos = ChronoUnit.MINUTES.between(agora, alvoMeiaNoite)
        if (minutos <= 0) return ""
        val horasTotais = minutos / 60
        val dias = ChronoUnit.DAYS.between(agora.toLocalDate(), alvoMeiaNoite.toLocalDate())
        val horas = horasTotais - (dias - 1) * 24
        // Na virada exata da meia-noite a conta dá 24; "24h" não existe num
        // relógio e aparecia por um instante em "AMANHÃ!". De 1 a 23 vale.
        return if (horas > 0L && horas < 24L) textos.horas(horas) else ""
    }

    /**
     * Fração do tempo que AINDA falta dentro da janela [inicio, alvo], de 0 a 1.
     * 1 = acabou de entrar na contagem (nada passou); 0 = chegou a data.
     * É o que escurece o card da esquerda pra direita.
     */
    fun fracaoRestante(
        agora: LocalDateTime,
        inicio: LocalDateTime,
        alvo: LocalDateTime
    ): Float {
        val total = ChronoUnit.MINUTES.between(inicio, alvo)
        if (total <= 0L) return 0f
        val restante = ChronoUnit.MINUTES.between(agora, alvo)
        return (restante.toDouble() / total.toDouble()).coerceIn(0.0, 1.0).toFloat()
    }
}
