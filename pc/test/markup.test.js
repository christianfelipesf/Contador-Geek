"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

const root = path.join(__dirname, "..");
const html = fs.readFileSync(path.join(root, "index.html"), "utf8");
const app = fs.readFileSync(path.join(root, "js", "app.js"), "utf8");

function ids() {
    return [...html.matchAll(/\bid="([^"]+)"/g)].map((m) => m[1]);
}

test("index.html nao tem IDs duplicados", () => {
    const vistos = new Set();
    const repetidos = [];
    for (const id of ids()) {
        if (vistos.has(id)) repetidos.push(id);
        vistos.add(id);
    }
    assert.deepEqual(repetidos, []);
});

test("todo ID buscado por $() no app existe no HTML", () => {
    const existentes = new Set(ids());
    const referencias = [...app.matchAll(/\$\("([^"]+)"\)/g)].map((m) => m[1]);
    const ausentes = [...new Set(referencias)].filter((id) => !existentes.has(id));
    assert.deepEqual(ausentes, []);
});

test("todo label[for] aponta para um controle existente", () => {
    const existentes = new Set(ids());
    const destinos = [...html.matchAll(/<label\b[^>]*\bfor="([^"]+)"/g)].map((m) => m[1]);
    const quebrados = destinos.filter((id) => !existentes.has(id));
    assert.deepEqual(quebrados, []);
});
