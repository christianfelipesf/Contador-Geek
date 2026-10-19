/*
 * Countdown Geek — lógica pura da contagem (PC).
 *
 * O arquivo roda em dois lugares:
 *  - no navegador (script clássico, sem módulo: funciona abrindo o HTML
 *    direto do disco, file://, onde import/export seria bloqueado);
 *  - no Node, para os testes (module.exports).
 *
 * As datas abaixo são o único lugar onde elas ficam no PC. No Android elas
 * continuam em CountdownWallpaperService.kt / Eventos.kt.
 */
(function (root, factory) {
    if (typeof module === "object" && module.exports) {
        module.exports = factory();
    } else {
        root.CountdownLogic = factory();
    }
})(typeof globalThis !== "undefined" ? globalThis : this, function () {
    "use strict";

    var MS_DAY = 86400000;

    /** Início de todas as contagens (mesma data do app Android). */
    var INICIO = dateAt(2026, 10, 15);

    var EVENTOS = [
        {
            id: "vision",
            emoji: "👁️",
            cor: "#8B5CF6",
            data: dateAt(2026, 10, 21),
            titulo: {
                pt: "VISION QUEST",
                en: "VISION QUEST",
                es: "VISION QUEST"
            },
            subtitulo: {
                pt: "Ep 3 e 4 - Disney+",
                en: "Ep 3 & 4 - Disney+",
                es: "Ep 3 y 4 - Disney+"
            }
        },
        {
            id: "zelda",
            emoji: "🗡️",
            cor: "#22C55E",
            data: dateAt(2026, 11, 5),
            titulo: {
                pt: "ZELDA OOT REMAKE",
                en: "ZELDA OOT REMAKE",
                es: "ZELDA OOT REMAKE"
            },
            subtitulo: {
                pt: "Switch 2 - {data}",
                en: "Switch 2 - {data}",
                es: "Switch 2 - {data}"
            }
        },
        {
            id: "gta",
            emoji: "🚗",
            cor: "#F59E0B",
            data: dateAt(2026, 11, 19),
            titulo: {
                pt: "GTA 6",
                en: "GTA 6",
                es: "GTA 6"
            },
            subtitulo: {
                pt: "PS5 / Xbox - {data}",
                en: "PS5 / Xbox - {data}",
                es: "PS5 / Xbox - {data}"
            }
        },
        {
            id: "doomsday",
            emoji: "🛡️",
            cor: "#EF4444",
            data: dateAt(2026, 12, 18),
            titulo: {
                pt: "VINGADORES DOOMSDAY",
                en: "AVENGERS DOOMSDAY",
                es: "VENGADORES DOOMSDAY"
            },
            subtitulo: {
                pt: "Cinema - {data}",
                en: "In cinemas - {data}",
                es: "Cine - {data}"
            }
        }
    ];

    /** Date local à meia-noite (mês 1..12, como se escreve). */
    function dateAt(ano, mes, dia) {
        return new Date(ano, mes - 1, dia, 0, 0, 0, 0);
    }

    function inicioDoDia(data) {
        return new Date(data.getFullYear(), data.getMonth(), data.getDate());
    }

    /** Dias de calendário entre duas datas (igual ao ChronoUnit.DAYS do app). */
    function diasFaltam(hoje, alvo) {
        return Math.round((inicioDoDia(alvo) - inicioDoDia(hoje)) / MS_DAY);
    }

    /**
     * Horas que sobram ALÉM dos dias inteiros mostrados: 1..23.
     * Devolve 0 quando não sobra hora, quando já passou ou na virada exata
     * da meia-noite (o app antigo mostrava "24h" nesse instante).
     */
    function horasRestantes(agora, alvoMeiaNoite) {
        var minutos = Math.floor((alvoMeiaNoite - agora) / 60000);
        if (minutos <= 0) return 0;
        var horasTotais = Math.floor(minutos / 60);
        var dias = diasFaltam(agora, alvoMeiaNoite);
        var horas = horasTotais - (dias - 1) * 24;
        return horas >= 1 && horas <= 23 ? horas : 0;
    }

    /**
     * Fração do tempo que AINDA falta dentro da janela [inicio, alvo], 0..1.
     * 1 = acabou de entrar na contagem; 0 = chegou a data.
     */
    function fracaoRestante(agora, inicio, alvo) {
        var total = alvo - inicio;
        if (total <= 0) return 0;
        var restante = alvo - agora;
        return Math.min(1, Math.max(0, restante / total));
    }

    /** "dias" | "amanha" | "hoje" | "jaSaiu" — o texto fica no i18n do app. */
    function chaveDias(dias) {
        if (dias > 1) return "dias";
        if (dias === 1) return "amanha";
        if (dias === 0) return "hoje";
        return "jaSaiu";
    }

    function localeDe(lang) {
        if (lang === "pt") return "pt-BR";
        if (lang === "es") return "es-ES";
        return "en-US";
    }

    /** Data curta no formato do idioma: "21 de out.", "Oct 21", "21 oct". */
    function formatarDataCurta(data, lang) {
        try {
            return new Intl.DateTimeFormat(localeDe(lang), {
                day: "numeric",
                month: "short"
            }).format(data);
        } catch (e) {
            return String(data.getDate()).padStart(2, "0") + "/" +
                String(data.getMonth() + 1).padStart(2, "0");
        }
    }

    /** Data completa para o cabeçalho: "19/10/2026". */
    function formatarDataCompleta(data, lang) {
        try {
            return new Intl.DateTimeFormat(localeDe(lang), {
                day: "2-digit",
                month: "2-digit",
                year: "numeric"
            }).format(data);
        } catch (e) {
            return String(data.getDate()).padStart(2, "0") + "/" +
                String(data.getMonth() + 1).padStart(2, "0") + "/" +
                data.getFullYear();
        }
    }

    /** Tudo que a interface precisa de um evento, já no idioma escolhido. */
    function eventoParaUI(evento, lang, agora) {
        var dias = diasFaltam(agora, evento.data);
        var alvoMeiaNoite = inicioDoDia(evento.data);
        var horas = horasRestantes(agora, alvoMeiaNoite);
        var dataCurta = formatarDataCurta(evento.data, lang);
        var subtitulo = (evento.subtitulo[lang] || evento.subtitulo.en)
            .replace("{data}", dataCurta);

        return {
            id: evento.id,
            emoji: evento.emoji,
            cor: evento.cor,
            titulo: evento.titulo[lang] || evento.titulo.en,
            subtitulo: subtitulo,
            data: evento.data,
            dataCurta: dataCurta,
            dias: dias,
            chaveDias: chaveDias(dias),
            horas: horas,
            restante: fracaoRestante(agora, INICIO, alvoMeiaNoite)
        };
    }

    return {
        INICIO: INICIO,
        EVENTOS: EVENTOS,
        MS_DAY: MS_DAY,
        dateAt: dateAt,
        inicioDoDia: inicioDoDia,
        diasFaltam: diasFaltam,
        horasRestantes: horasRestantes,
        fracaoRestante: fracaoRestante,
        chaveDias: chaveDias,
        localeDe: localeDe,
        formatarDataCurta: formatarDataCurta,
        formatarDataCompleta: formatarDataCompleta,
        eventoParaUI: eventoParaUI
    };
});
