package com.exemplo.countdownwallpaper

import android.content.Context

/**
 * Textos da contagem, injetáveis.
 *
 * O [CountdownLogic] é lógica pura (sem Android) e continua coberto por teste:
 * ele só pergunta "como escrevo X?" pra quem sabe — no app quem responde é o
 * [TextosRecursos] (strings.xml do idioma atual), nos testes é o [TextosPadrao].
 */
interface Textos {
    fun dias(n: Long): String
    fun amanha(): String
    fun hoje(): String
    fun jaSaiu(): String
    fun horas(horas: Long): String
}

/** Padrão embutido (português), usado nos testes e como rede de segurança. */
object TextosPadrao : Textos {
    override fun dias(n: Long) = "$n DIAS"
    override fun amanha() = "AMANHÃ!"
    override fun hoje() = "É HOJE!"
    override fun jaSaiu() = "JÁ SAIU!"
    override fun horas(horas: Long) = "${horas}h"
}

/**
 * Textos vindos dos recursos Android (values / values-pt / values-es).
 * Sem cache de propósito: se o idioma do app mudar, a próxima leitura já
 * devolve o texto novo.
 */
class TextosRecursos(private val ctx: Context) : Textos {
    override fun dias(n: Long): String =
        ctx.resources.getQuantityString(R.plurals.wp_dias, n.toInt(), n)

    override fun amanha() = ctx.getString(R.string.wp_amanha)
    override fun hoje() = ctx.getString(R.string.wp_hoje)
    override fun jaSaiu() = ctx.getString(R.string.wp_ja_saiu)
    override fun horas(horas: Long) = ctx.getString(R.string.wp_horas, horas)
}
