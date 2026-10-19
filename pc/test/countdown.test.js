"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const CL = require("../js/countdown.js");

test("diasFaltam conta dias de calendário", () => {
    assert.equal(
        CL.diasFaltam(new Date(2026, 9, 15), new Date(2026, 9, 21)),
        6
    );
    assert.equal(
        CL.diasFaltam(new Date(2026, 10, 19), new Date(2026, 10, 19)),
        0
    );
});

test("chaveDias cobre ontem, hoje, amanhã e futuro", () => {
    assert.equal(CL.chaveDias(-1), "jaSaiu");
    assert.equal(CL.chaveDias(0), "hoje");
    assert.equal(CL.chaveDias(1), "amanha");
    assert.equal(CL.chaveDias(2), "dias");
});

test("horasRestantes devolve o resto além dos dias inteiros", () => {
    assert.equal(
        CL.horasRestantes(
            new Date(2026, 9, 17, 12, 0),
            new Date(2026, 11, 18, 0, 0)
        ),
        12
    );
    assert.equal(
        CL.horasRestantes(
            new Date(2026, 9, 20, 16, 28),
            new Date(2026, 9, 21, 0, 0)
        ),
        7
    );
});

test("horasRestantes não mostra 24h na virada exata da meia-noite", () => {
    assert.equal(
        CL.horasRestantes(
            new Date(2026, 9, 20, 0, 0, 0),
            new Date(2026, 9, 21, 0, 0, 0)
        ),
        0
    );
    assert.equal(
        CL.horasRestantes(
            new Date(2026, 9, 20, 0, 0, 1),
            new Date(2026, 9, 21, 0, 0, 0)
        ),
        23
    );
});

test("horasRestantes fica vazio quando já passou", () => {
    assert.equal(
        CL.horasRestantes(
            new Date(2026, 9, 22, 0, 0),
            new Date(2026, 9, 21, 0, 0)
        ),
        0
    );
});

test("fracaoRestante vai de 1 a 0 e não estoura", () => {
    const inicio = new Date(2026, 9, 15, 0, 0);
    const alvo = new Date(2026, 9, 21, 0, 0);

    assert.equal(CL.fracaoRestante(new Date(2026, 9, 15, 0, 0), inicio, alvo), 1);
    assert.equal(CL.fracaoRestante(new Date(2026, 9, 18, 0, 0), inicio, alvo), 0.5);
    assert.equal(CL.fracaoRestante(new Date(2026, 9, 25, 0, 0), inicio, alvo), 0);
    assert.equal(CL.fracaoRestante(new Date(2026, 9, 1, 0, 0), inicio, alvo), 1);
});

test("eventoParaUI monta dias, horas, barra e subtítulo em pt", () => {
    const ev = CL.eventoParaUI(CL.EVENTOS[0], "pt", new Date(2026, 9, 19, 12, 0, 0));

    assert.equal(ev.titulo, "VISION QUEST");
    assert.equal(ev.subtitulo, "Ep 3 e 4 - Disney+");
    assert.equal(ev.dias, 2);
    assert.equal(ev.chaveDias, "dias");
    assert.equal(ev.horas, 12);
    assert.ok(Math.abs(ev.restante - 0.25) < 0.001);
});

test("eventoParaUI troca idioma e data", () => {
    const en = CL.eventoParaUI(CL.EVENTOS[3], "en", new Date(2026, 9, 19, 12, 0, 0));
    const es = CL.eventoParaUI(CL.EVENTOS[3], "es", new Date(2026, 9, 19, 12, 0, 0));

    assert.equal(en.titulo, "AVENGERS DOOMSDAY");
    assert.equal(en.subtitulo, "In cinemas - Dec 18");
    assert.equal(es.titulo, "VENGADORES DOOMSDAY");
    assert.equal(es.subtitulo, "Cine - 18 dic");
});

test("datas curtas e completas usam o idioma", () => {
    assert.equal(CL.formatarDataCurta(new Date(2026, 9, 21), "en"), "Oct 21");
    assert.match(CL.formatarDataCurta(new Date(2026, 9, 21), "pt"), /21.*out/i);
    assert.equal(CL.formatarDataCompleta(new Date(2026, 9, 19), "pt"), "19/10/2026");
});
