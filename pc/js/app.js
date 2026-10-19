/* =========================================================
   Countdown Geek — PC (interface)
   Depende de js/countdown.js (CountdownLogic).
   ========================================================= */

(function () {
    "use strict";

    var CL = window.CountdownLogic;
    if (!CL) {
        document.body.textContent = "Erro: js/countdown.js não carregou.";
        return;
    }

    var STORAGE_KEY = "countdownGeekPc.settings.v1";
    var BANNER_FILES = [
        "doomsday.jpg",
        "gta6.jpg",
        "vision_quest_2026.jpg",
        "zelda.jpg"
    ];

    var DEFAULTS = {
        theme: {
            top: "#150826",
            mid: "#6D28D9",
            bot: "#2E1065"
        },
        speed: 1,
        fps: 30,
        particles: 30,
        hover: true,
        hoverMs: 9000,
        hoverTr: 600,
        zoom: true,
        banners: true,
        bannerMs: 18000,
        bannerTr: 1500,
        bannerPan: 10,
        bannerShade: 68,
        saver: false,
        lang: "auto"
    };

    var THEMES = [
        {
            id: "roxo",
            nome: { pt: "Roxo", en: "Purple", es: "Morado" },
            cores: ["#150826", "#6D28D9", "#2E1065"]
        },
        {
            id: "oceano",
            nome: { pt: "Oceano", en: "Ocean", es: "Océano" },
            cores: ["#020617", "#0369A1", "#082F49"]
        },
        {
            id: "fogo",
            nome: { pt: "Fogo", en: "Fire", es: "Fuego" },
            cores: ["#1C0A00", "#C2410C", "#431407"]
        },
        {
            id: "verde",
            nome: { pt: "Verde", en: "Green", es: "Verde" },
            cores: ["#02120A", "#16A34A", "#052E16"]
        },
        {
            id: "sunset",
            nome: { pt: "Sunset", en: "Sunset", es: "Atardecer" },
            cores: ["#20063B", "#EC4899", "#F59E0B"]
        },
        {
            id: "meia-noite",
            nome: { pt: "Meia-noite", en: "Midnight", es: "Medianoche" },
            cores: ["#000000", "#1D4ED8", "#020617"]
        }
    ];

    var I18N = {
        pt: {
            faltam: "FALTAM",
            wallpaperTitulo: "CONTADOR GEEK",
            rodape: "Atualiza sozinho todo dia",
            versaoPc: "Versão PC",
            hojePrefixo: "hoje",
            dias: "DIAS",
            amanha: "AMANHÃ!",
            hoje: "É HOJE!",
            jaSaiu: "JÁ SAIU!",

            config: "Configurações",
            fechar: "Fechar",
            telaCheia: "Tela cheia",
            secAparencia: "Aparência",
            tema: "Tema",
            corTopo: "Topo",
            corMeio: "Meio",
            corBase: "Base",
            ajudaCores: "As cores valem na hora e ficam salvas neste computador.",

            secAnimacao: "Animação",
            velocidade: "Velocidade",
            fluidez: "Fluidez",
            fps60: "Performance ~60 fps",
            fps30: "Equilíbrio ~30 fps (recomendado)",
            fps20: "Eco ~20 fps",
            fps15: "Super eco ~15 fps",
            particulas: "Partículas",
            saver: "Modo economizador (mínimo consumo)",
            ajudaSaver: "Congela o fundo, desliga as estrelas e o destaque, e atualiza só de 2 em 2 segundos.",

            secSelecao: "Seleção dos cards",
            hover: "Destacar um card por vez",
            zoom: "Card cresce ao selecionar (zoom)",
            hoverMs: "Tempo parado por card",
            hoverTr: "Transição entre cards",

            secBanners: "Banners no fundo",
            banners: "Banners geek no fundo (roxo por cima)",
            bannerMs: "Tempo parado por banner",
            bannerTr: "Transição entre banners",
            pan: "Pan lateral",
            panParado: "parado",
            shade: "Cobertura roxa",

            secSistema: "Sistema",
            idioma: "Idioma",
            idiomaSistema: "Sistema",
            restaurar: "Restaurar padrão",
            ajudaAtalhos: "Atalhos: S abre/fecha esta tela · F alterna a tela cheia · Esc fecha.",
            confirmarRestaurar: "Restaurar todas as configurações?",
            dica: "S = configurações · F = tela cheia",
            tituloDocumento: "Contador Geek — PC"
        },
        en: {
            faltam: "LEFT",
            wallpaperTitulo: "COUNTDOWN GEEK",
            rodape: "Updates by itself every day",
            versaoPc: "PC version",
            hojePrefixo: "today",
            dias: "DAYS",
            amanha: "TOMORROW!",
            hoje: "IT'S TODAY!",
            jaSaiu: "OUT NOW!",

            config: "Settings",
            fechar: "Close",
            telaCheia: "Fullscreen",
            secAparencia: "Appearance",
            tema: "Theme",
            corTopo: "Top",
            corMeio: "Middle",
            corBase: "Bottom",
            ajudaCores: "Colors apply instantly and are saved on this computer.",

            secAnimacao: "Animation",
            velocidade: "Speed",
            fluidez: "Smoothness",
            fps60: "Performance ~60 fps",
            fps30: "Balanced ~30 fps (recommended)",
            fps20: "Eco ~20 fps",
            fps15: "Super eco ~15 fps",
            particulas: "Particles",
            saver: "Power saver mode (minimal usage)",
            ajudaSaver: "Freezes the background, turns off stars and highlighting, and redraws only every 2 seconds.",

            secSelecao: "Card selection",
            hover: "Highlight one card at a time",
            zoom: "Card grows when selected (zoom)",
            hoverMs: "Time on each card",
            hoverTr: "Transition between cards",

            secBanners: "Background banners",
            banners: "Geek banners in the background (purple overlay)",
            bannerMs: "Time on each banner",
            bannerTr: "Transition between banners",
            pan: "Side pan",
            panParado: "still",
            shade: "Purple coverage",

            secSistema: "System",
            idioma: "Language",
            idiomaSistema: "System",
            restaurar: "Restore defaults",
            ajudaAtalhos: "Shortcuts: S opens/closes this panel · F toggles fullscreen · Esc closes.",
            confirmarRestaurar: "Restore all settings?",
            dica: "S = settings · F = fullscreen",
            tituloDocumento: "Countdown Geek — PC"
        },
        es: {
            faltam: "FALTAN",
            wallpaperTitulo: "CONTADOR GEEK",
            rodape: "Se actualiza solo cada día",
            versaoPc: "Versión PC",
            hojePrefixo: "hoy",
            dias: "DÍAS",
            amanha: "¡MAÑANA!",
            hoje: "¡ES HOY!",
            jaSaiu: "¡YA SALIÓ!",

            config: "Ajustes",
            fechar: "Cerrar",
            telaCheia: "Pantalla completa",
            secAparencia: "Apariencia",
            tema: "Tema",
            corTopo: "Arriba",
            corMeio: "Medio",
            corBase: "Abajo",
            ajudaCores: "Los colores se aplican al instante y se guardan en este equipo.",

            secAnimacao: "Animación",
            velocidade: "Velocidad",
            fluidez: "Fluidez",
            fps60: "Rendimiento ~60 fps",
            fps30: "Equilibrio ~30 fps (recomendado)",
            fps20: "Eco ~20 fps",
            fps15: "Súper eco ~15 fps",
            particulas: "Partículas",
            saver: "Modo ahorro (consumo mínimo)",
            ajudaSaver: "Congela el fondo, apaga las estrellas y el resalte, y redibuja solo cada 2 segundos.",

            secSelecao: "Selección de tarjetas",
            hover: "Resaltar una tarjeta a la vez",
            zoom: "La tarjeta crece al seleccionarla (zoom)",
            hoverMs: "Tiempo por tarjeta",
            hoverTr: "Transición entre tarjetas",

            secBanners: "Banners de fondo",
            banners: "Banners geek de fondo (morado encima)",
            bannerMs: "Tiempo por banner",
            bannerTr: "Transición entre banners",
            pan: "Desplazamiento lateral",
            panParado: "parado",
            shade: "Cobertura morada",

            secSistema: "Sistema",
            idioma: "Idioma",
            idiomaSistema: "Sistema",
            restaurar: "Restaurar valores",
            ajudaAtalhos: "Atajos: S abre/cierra este panel · F alterna pantalla completa · Esc cierra.",
            confirmarRestaurar: "¿Restaurar todos los ajustes?",
            dica: "S = ajustes · F = pantalla completa",
            tituloDocumento: "Contador Geek — PC"
        }
    };

    var el = {};
    var settings = sanitizeSettings(DEFAULTS);
    var eff = settings;
    var lang = "pt";
    var agora = new Date();
    var cards = [];
    var painelAberto = false;
    var rafId = 0;
    var lastFrame = 0;
    var lastContagem = 0;
    var ultimoDia = "";
    var ultimaHora = -1;
    var lastParticles = -1;
    var idleTimer = 0;
    var resizeTimer = 0;
    var saveTimer = 0;
    var fitPendente = false;
    var hintTimer = 0;
    var showHintTimer = 0;

    var bannerState = {
        prontos: false,
        disponiveis: false,
        lista: [],
        ordem: [],
        ptr: 0,
        current: null,
        cover: 1,
        dipping: false,
        lastSwitch: 0,
        panPos: 0.5,
        panDir: 1,
        panFix: 0.5
    };

    var stars = {
        canvas: null,
        ctx: null,
        w: 0,
        h: 0,
        dpr: 1,
        lista: []
    };

    // =====================================================
    // utilidades
    // =====================================================

    function $(id) {
        return document.getElementById(id);
    }

    function clamp(v, min, max, fallback) {
        v = Number(v);
        if (!isFinite(v)) return fallback;
        return Math.min(max, Math.max(min, v));
    }

    function hexValido(v, fallback) {
        return typeof v === "string" && /^#[0-9a-fA-F]{6}$/.test(v) ? v.toLowerCase() : fallback;
    }

    function t(key) {
        var dic = I18N[lang] || I18N.en;
        return Object.prototype.hasOwnProperty.call(dic, key) ? dic[key] : (I18N.en[key] || key);
    }

    function localeLang() {
        if (lang === "pt") return "pt-BR";
        if (lang === "es") return "es-ES";
        return "en-US";
    }

    function resolveLang(pref) {
        if (pref === "pt" || pref === "en" || pref === "es") return pref;
        var nav = (navigator.language || navigator.userLanguage || "en").toLowerCase();
        if (nav.indexOf("pt") === 0) return "pt";
        if (nav.indexOf("es") === 0) return "es";
        return "en";
    }

    // =====================================================
    // configurações
    // =====================================================

    function sanitizeSettings(raw) {
        raw = raw || {};
        var themeRaw = raw.theme || {};
        var fps = Number(raw.fps);
        if ([15, 20, 30, 60].indexOf(fps) < 0) fps = DEFAULTS.fps;

        return {
            theme: {
                top: hexValido(themeRaw.top, DEFAULTS.theme.top),
                mid: hexValido(themeRaw.mid, DEFAULTS.theme.mid),
                bot: hexValido(themeRaw.bot, DEFAULTS.theme.bot)
            },
            speed: clamp(raw.speed, 0.25, 2, DEFAULTS.speed),
            fps: fps,
            particles: Math.round(clamp(raw.particles, 0, 80, DEFAULTS.particles)),
            hover: raw.hover !== false,
            hoverMs: Math.round(clamp(raw.hoverMs, 3000, 15000, DEFAULTS.hoverMs)),
            hoverTr: Math.round(clamp(raw.hoverTr, 0, 2000, DEFAULTS.hoverTr)),
            zoom: raw.zoom !== false,
            banners: raw.banners !== false,
            bannerMs: Math.round(clamp(raw.bannerMs, 8000, 30000, DEFAULTS.bannerMs)),
            bannerTr: Math.round(clamp(raw.bannerTr, 500, 3000, DEFAULTS.bannerTr)),
            bannerPan: Math.round(clamp(raw.bannerPan, 0, 60, DEFAULTS.bannerPan)),
            bannerShade: Math.round(clamp(raw.bannerShade, 30, 95, DEFAULTS.bannerShade)),
            saver: raw.saver === true,
            lang: ["auto", "pt", "en", "es"].indexOf(raw.lang) >= 0 ? raw.lang : "auto"
        };
    }

    function carregarSettings() {
        var salvo = null;
        try {
            salvo = JSON.parse(localStorage.getItem(STORAGE_KEY) || "null");
        } catch (e) {
            salvo = null;
        }
        if (!salvo) return sanitizeSettings(DEFAULTS);
        return sanitizeSettings({
            theme: Object.assign({}, DEFAULTS.theme, salvo.theme || {}),
            speed: salvo.speed,
            fps: salvo.fps,
            particles: salvo.particles,
            hover: salvo.hover,
            hoverMs: salvo.hoverMs,
            hoverTr: salvo.hoverTr,
            zoom: salvo.zoom,
            banners: salvo.banners,
            bannerMs: salvo.bannerMs,
            bannerTr: salvo.bannerTr,
            bannerPan: salvo.bannerPan,
            bannerShade: salvo.bannerShade,
            saver: salvo.saver,
            lang: salvo.lang
        });
    }

    function salvarSettings() {
        clearTimeout(saveTimer);
        saveTimer = setTimeout(function () {
            try {
                localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
            } catch (e) {
                /* localStorage indisponível: segue só em memória */
            }
        }, 120);
    }

    function salvarSettingsAgora() {
        clearTimeout(saveTimer);
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
        } catch (e) {
            /* ignora */
        }
    }

    function recalcularEfetivo() {
        if (settings.saver) {
            eff = Object.assign({}, settings, {
                speed: 0,
                fps: 0.5,
                particles: 0,
                hover: false,
                zoom: false,
                bannerPan: 0,
                bannerMs: Infinity
            });
        } else {
            eff = Object.assign({}, settings);
        }
        eff.showBanners = settings.banners && bannerState.disponiveis;

        document.body.classList.toggle("saver", settings.saver);
        document.body.classList.toggle("sem-zoom", !settings.zoom || settings.saver);
        document.body.classList.toggle("sem-banners", !eff.showBanners);

        var root = document.documentElement;
        root.style.setProperty("--sel-scale", (settings.zoom && !settings.saver) ? "1.045" : "1");
        root.style.setProperty("--hover-tr", settings.hoverTr + "ms");

        var speed = Math.max(0.25, settings.speed);
        var fundoDur = (26 / speed).toFixed(2) + "s";
        if (el.fundoGrad) el.fundoGrad.style.animationDuration = fundoDur;
        root.style.setProperty("--orb1-dur", (16 / speed).toFixed(2) + "s");
        root.style.setProperty("--orb2-dur", (20 / speed).toFixed(2) + "s");

        if (eff.particles !== lastParticles) {
            lastParticles = eff.particles;
            construirEstrelas();
        }
        if (eff.particles === 0 || eff.saver) limparEstrelas();
    }

    // =====================================================
    // tema / textos
    // =====================================================

    function aplicarTema() {
        var root = document.documentElement;
        root.style.setProperty("--top", settings.theme.top);
        root.style.setProperty("--mid", settings.theme.mid);
        root.style.setProperty("--bot", settings.theme.bot);

        if (el.corTopo) {
            el.corTopo.value = settings.theme.top;
            el.corMeio.value = settings.theme.mid;
            el.corBase.value = settings.theme.bot;
            el.hexTopo.textContent = settings.theme.top.toUpperCase();
            el.hexMeio.textContent = settings.theme.mid.toUpperCase();
            el.hexBase.textContent = settings.theme.bot.toUpperCase();
        }
        marcarTemaAtivo();
    }

    function temaAtualIndex() {
        var top = settings.theme.top.toLowerCase();
        var mid = settings.theme.mid.toLowerCase();
        var bot = settings.theme.bot.toLowerCase();
        for (var i = 0; i < THEMES.length; i++) {
            var c = THEMES[i].cores;
            if (c[0].toLowerCase() === top && c[1].toLowerCase() === mid && c[2].toLowerCase() === bot) {
                return i;
            }
        }
        return -1;
    }

    function marcarTemaAtivo() {
        if (!el.temas) return;
        var idx = temaAtualIndex();
        for (var i = 0; i < el.temas.children.length; i++) {
            var ativo = i === idx;
            el.temas.children[i].classList.toggle("ativo", ativo);
            el.temas.children[i].setAttribute("aria-pressed", ativo ? "true" : "false");
        }
    }

    function montarTemas() {
        if (!el.temas) return;
        el.temas.innerHTML = "";
        THEMES.forEach(function (tema) {
            var b = document.createElement("button");
            b.type = "button";
            b.className = "tema";
            b.style.background = "linear-gradient(135deg, " + tema.cores[0] + " 0%, " +
                tema.cores[1] + " 52%, " + tema.cores[2] + " 100%)";
            b.setAttribute("aria-label", tema.nome[lang] || tema.nome.en);
            b.setAttribute("aria-pressed", "false");

            var span = document.createElement("span");
            span.textContent = tema.nome[lang] || tema.nome.en;
            b.appendChild(span);

            b.addEventListener("click", function () {
                settings.theme = {
                    top: tema.cores[0],
                    mid: tema.cores[1],
                    bot: tema.cores[2]
                };
                aplicarTema();
                salvarSettings();
                mostrarAtividade();
            });
            el.temas.appendChild(b);
        });
        marcarTemaAtivo();
    }

    function aplicarIdioma() {
        lang = resolveLang(settings.lang);
        document.documentElement.lang = localeLang();
        document.documentElement.setAttribute("data-lang", lang);
        document.title = t("tituloDocumento");

        var nos = document.querySelectorAll("[data-i18n]");
        for (var i = 0; i < nos.length; i++) {
            var chave = nos[i].getAttribute("data-i18n");
            nos[i].textContent = t(chave);
        }

        if (el.titulo) el.titulo.textContent = t("wallpaperTitulo");
        if (el.btnConfig) {
            el.btnConfig.title = t("config") + " (S)";
            el.btnConfig.setAttribute("aria-label", t("config"));
        }
        if (el.btnFullscreen) {
            el.btnFullscreen.title = t("telaCheia") + " (F)";
            el.btnFullscreen.setAttribute("aria-label", t("telaCheia"));
        }
        if (el.btnFechar) el.btnFechar.setAttribute("aria-label", t("fechar"));

        montarTemas();
        atualizarCabecalho();
        sincronizarRotulos();
        renderContagem();
    }

    function atualizarCabecalho() {
        if (!el.hoje) return;
        el.hoje.textContent = t("hojePrefixo") + " · " + CL.formatarDataCompleta(agora, lang);
    }

    function sincronizarRotulos() {
        if (!el.velValor) return;
        el.velValor.textContent = Math.round(settings.speed * 100) + "%";
        el.partValor.textContent = String(settings.particles);
        el.hoverValor.textContent = Math.round(settings.hoverMs / 1000) + "s";
        el.hovertrValor.textContent = settings.hoverTr + "ms";
        el.bannerValor.textContent = Math.round(settings.bannerMs / 1000) + "s";
        el.bannertrValor.textContent = settings.bannerTr + "ms";
        el.panValor.textContent = settings.bannerPan === 0
            ? t("panParado")
            : settings.bannerPan + " px/s";
        el.shadeValor.textContent = settings.bannerShade + "%";
    }

    // =====================================================
    // cards
    // =====================================================

    function montarCards() {
        el.lista.innerHTML = "";
        cards = CL.EVENTOS.map(function (ev) {
            var card = document.createElement("article");
            card.className = "card";
            card.style.setProperty("--cor", ev.cor);

            var barra = document.createElement("div");
            barra.className = "barra";

            var escuro = document.createElement("div");
            escuro.className = "escuro";

            var marcador = document.createElement("div");
            marcador.className = "marcador";

            var conteudo = document.createElement("div");
            conteudo.className = "card-conteudo";

            var info = document.createElement("div");
            info.className = "info";

            var titulo = document.createElement("div");
            titulo.className = "titulo";

            var subtitulo = document.createElement("div");
            subtitulo.className = "subtitulo";

            info.appendChild(titulo);
            info.appendChild(subtitulo);

            var dias = document.createElement("div");
            dias.className = "dias";

            var diasTexto = document.createElement("span");
            diasTexto.className = "dias-texto";

            var horas = document.createElement("span");
            horas.className = "horas";

            dias.appendChild(diasTexto);
            dias.appendChild(horas);

            conteudo.appendChild(info);
            conteudo.appendChild(dias);

            card.appendChild(barra);
            card.appendChild(escuro);
            card.appendChild(marcador);
            card.appendChild(conteudo);

            el.lista.appendChild(card);
            return card;
        });
    }

    function renderContagem() {
        if (!cards.length) return;
        var dados = CL.EVENTOS.map(function (ev) {
            return CL.eventoParaUI(ev, lang, agora);
        });

        for (var i = 0; i < cards.length; i++) {
            var card = cards[i];
            var d = dados[i];
            card.style.setProperty("--cor", d.cor);
            card.style.setProperty("--restante", d.restante.toFixed(5));

            var titulo = card.querySelector(".titulo");
            var subtitulo = card.querySelector(".subtitulo");
            var diasTexto = card.querySelector(".dias-texto");
            var horas = card.querySelector(".horas");

            titulo.textContent = d.emoji + "\u00A0\u00A0" + d.titulo;
            subtitulo.textContent = d.subtitulo;

            var texto = d.chaveDias === "dias"
                ? (d.dias + " " + t("dias"))
                : t(d.chaveDias);

            diasTexto.textContent = texto;
            diasTexto.classList.toggle("longo", texto.length > 7);
            horas.textContent = d.horas > 0 ? d.horas + "h" : "";

            card.setAttribute("aria-label", d.titulo + ": " + texto + (d.horas > 0 ? " " + d.horas + "h" : ""));
        }
        agendarAjusteTitulos();
    }

    function agendarAjusteTitulos() {
        if (fitPendente) return;
        fitPendente = true;
        requestAnimationFrame(function () {
            fitPendente = false;
            ajustarTitulos();
        });
    }

    /**
     * O app Android encolhe o título até caber ao lado dos dias. Aqui é a
     * mesma ideia: parte do tamanho do CSS e reduz até não encostar.
     */
    function ajustarTitulos() {
        var titulos = document.querySelectorAll(".titulo");
        for (var i = 0; i < titulos.length; i++) {
            var tx = titulos[i];
            tx.style.fontSize = "";
            var max = tx.clientWidth;
            if (!max) continue;

            var tamanho = parseFloat(getComputedStyle(tx).fontSize);
            if (!isFinite(tamanho)) continue;
            var minimo = Math.max(9, tamanho * 0.55);
            var voltas = 0;

            while (tx.scrollWidth > max + 1 && tamanho > minimo && voltas < 90) {
                tamanho -= 0.5;
                tx.style.fontSize = tamanho + "px";
                voltas++;
            }
        }
    }

    // =====================================================
    // banners de fundo
    // =====================================================

    function iniciarBanners() {
        var pendentes = BANNER_FILES.length;
        if (!pendentes) {
            bannerState.prontos = true;
            bannerState.disponiveis = false;
            recalcularEfetivo();
            return;
        }

        BANNER_FILES.forEach(function (arquivo) {
            var im = new Image();
            var finalizado = false;

            function fim() {
                if (finalizado) return;
                finalizado = true;
                pendentes--;
                if (pendentes === 0) {
                    bannerState.prontos = true;
                    bannerState.disponiveis = bannerState.lista.length > 0;
                    if (bannerState.disponiveis) {
                        embaralharBanners();
                        bannerState.ptr = 0;
                        definirBanner(bannerState.ordem[0]);
                        bannerState.cover = 1;
                        bannerState.dipping = false;
                        bannerState.lastSwitch = performance.now();
                    }
                    recalcularEfetivo();
                }
            }

            im.onload = function () {
                if (im.naturalWidth > 0 && im.naturalHeight > 0) {
                    bannerState.lista.push({
                        src: "banners/" + arquivo,
                        w: im.naturalWidth,
                        h: im.naturalHeight
                    });
                }
                fim();
            };
            im.onerror = fim;
            im.src = "banners/" + arquivo;
        });
    }

    function embaralharBanners() {
        bannerState.ordem = bannerState.lista.map(function (_, i) { return i; });
        for (var i = bannerState.ordem.length - 1; i > 0; i--) {
            var j = Math.floor(Math.random() * (i + 1));
            var tmp = bannerState.ordem[i];
            bannerState.ordem[i] = bannerState.ordem[j];
            bannerState.ordem[j] = tmp;
        }
        // evita repetir o banner que estava na tela
        var atualIdx = bannerState.lista.indexOf(bannerState.current);
        if (bannerState.ordem.length > 1 && bannerState.ordem[0] === atualIdx) {
            var troca = 1;
            var aux = bannerState.ordem[0];
            bannerState.ordem[0] = bannerState.ordem[troca];
            bannerState.ordem[troca] = aux;
        }
    }

    function definirBanner(idxLista) {
        var item = bannerState.lista[idxLista];
        if (!item) return;
        bannerState.current = item;
        el.bannerImg.style.backgroundImage = 'url("' + encodeURI(item.src) + '")';
        bannerState.panPos = Math.random();
        bannerState.panDir = Math.random() < 0.5 ? -1 : 1;
        bannerState.panFix = Math.random();
        atualizarPan(0);
    }

    function avancarBanner() {
        if (bannerState.lista.length < 2) {
            atualizarPan(0);
            return;
        }
        bannerState.ptr++;
        if (bannerState.ptr >= bannerState.ordem.length) {
            embaralharBanners();
            bannerState.ptr = 0;
        }
        definirBanner(bannerState.ordem[bannerState.ptr]);
    }

    function atualizarBanners(now, dt) {
        if (!eff.showBanners || !bannerState.prontos || !bannerState.current) return;

        if (eff.saver) {
            bannerState.cover = settings.bannerShade / 100;
            el.bannerOverlay.style.opacity = bannerState.cover.toFixed(3);
            atualizarPan(0);
            return;
        }

        if (!bannerState.lastSwitch) bannerState.lastSwitch = now;

        if (!bannerState.dipping && now - bannerState.lastSwitch >= eff.bannerMs) {
            bannerState.dipping = true;
        }

        var alvo = bannerState.dipping ? 1 : settings.bannerShade / 100;
        var passo = dt / Math.max(200, eff.bannerTr);

        if (bannerState.cover < alvo) {
            bannerState.cover = Math.min(alvo, bannerState.cover + passo);
        } else if (bannerState.cover > alvo) {
            bannerState.cover = Math.max(alvo, bannerState.cover - passo);
        }

        if (bannerState.dipping && bannerState.cover >= 0.999) {
            avancarBanner();
            bannerState.dipping = false;
            bannerState.lastSwitch = now;
        }

        el.bannerOverlay.style.opacity = bannerState.cover.toFixed(3);
        atualizarPan(dt);
    }

    function atualizarPan(dt) {
        var cur = bannerState.current;
        if (!cur || !cur.w || !cur.h) return;

        var vw = window.innerWidth;
        var vh = window.innerHeight;
        var escala = Math.max(vw / cur.w, vh / cur.h);
        var dw = cur.w * escala;
        var dh = cur.h * escala;
        var overX = Math.max(0, dw - vw);
        var overY = Math.max(0, dh - vh);
        var panV = eff.bannerPan;
        var dtS = Math.max(0, dt) / 1000;
        var posX;
        var posY;

        if (overX >= overY) {
            if (overX > 1 && panV > 0) {
                bannerState.panPos += bannerState.panDir * (panV * dtS) / overX;
                if (bannerState.panPos > 1) {
                    bannerState.panPos = 2 - bannerState.panPos;
                    bannerState.panDir = -1;
                } else if (bannerState.panPos < 0) {
                    bannerState.panPos = -bannerState.panPos;
                    bannerState.panDir = 1;
                }
                bannerState.panPos = clamp(bannerState.panPos, 0, 1, 0.5);
            }
            posX = overX > 0 ? bannerState.panPos * 100 : 50;
            posY = overY > 0 ? bannerState.panFix * 100 : 50;
        } else {
            if (overY > 1 && panV > 0) {
                bannerState.panPos += bannerState.panDir * (panV * dtS) / overY;
                if (bannerState.panPos > 1) {
                    bannerState.panPos = 2 - bannerState.panPos;
                    bannerState.panDir = -1;
                } else if (bannerState.panPos < 0) {
                    bannerState.panPos = -bannerState.panPos;
                    bannerState.panDir = 1;
                }
                bannerState.panPos = clamp(bannerState.panPos, 0, 1, 0.5);
            }
            posX = overX > 0 ? bannerState.panFix * 100 : 50;
            posY = overY > 0 ? bannerState.panPos * 100 : 50;
        }

        el.bannerImg.style.backgroundPosition = posX.toFixed(2) + "% " + posY.toFixed(2) + "%";
    }

    function reiniciarBanners() {
        bannerState.cover = 1;
        bannerState.dipping = false;
        bannerState.lastSwitch = performance.now();
    }

    // =====================================================
    // estrelas
    // =====================================================

    function redimensionarEstrelas() {
        if (!stars.canvas) return;
        stars.w = Math.max(1, window.innerWidth);
        stars.h = Math.max(1, window.innerHeight);
        stars.dpr = Math.min(2, Math.max(1, window.devicePixelRatio || 1));
        stars.canvas.width = Math.round(stars.w * stars.dpr);
        stars.canvas.height = Math.round(stars.h * stars.dpr);
        stars.ctx.setTransform(stars.dpr, 0, 0, stars.dpr, 0, 0);
        construirEstrelas();
    }

    function construirEstrelas() {
        if (!stars.ctx) return;
        stars.lista = [];
        var qtd = Math.round(eff.particles);
        for (var i = 0; i < qtd; i++) {
            stars.lista.push({
                x: Math.random() * stars.w,
                y: Math.random() * stars.h,
                r: 0.8 + Math.random() * 2.5,
                s: 3 + Math.random() * 13,
                ph: Math.random() * Math.PI * 2
            });
        }
        if (!qtd) limparEstrelas();
    }

    function limparEstrelas() {
        if (stars.ctx && stars.w && stars.h) {
            stars.ctx.clearRect(0, 0, stars.w, stars.h);
        }
    }

    function atualizarEstrelas(now, dt) {
        if (!stars.ctx || !stars.lista.length) return;
        if (eff.particles <= 0 || eff.saver) return;

        var ctx = stars.ctx;
        ctx.clearRect(0, 0, stars.w, stars.h);
        var t = now / 1000;
        var vel = 0.55 + eff.speed * 0.45;

        for (var i = 0; i < stars.lista.length; i++) {
            var p = stars.lista[i];
            p.y -= p.s * (dt / 1000) * vel;
            if (p.y < -6) {
                p.y = stars.h + 6;
                p.x = Math.random() * stars.w;
            }
            var alpha = 0.30 + 0.45 * Math.abs(Math.sin(t * 1.7 + p.ph));
            ctx.globalAlpha = alpha;
            ctx.fillStyle = "#E9D5FF";
            ctx.beginPath();
            ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
            ctx.fill();
        }
        ctx.globalAlpha = 1;
    }

    // =====================================================
    // destaque (hover) dos cards
    // =====================================================

    var ultimoSelecionado = -1;
    var hoverOffset = 0;

    function atualizarHover(now) {
        if (!eff.hover || cards.length === 0) {
            selecionarCard(-1);
            return;
        }

        var n = cards.length;
        var dwell = Math.max(1000, eff.hoverMs);
        var tr = Math.min(eff.hoverTr, dwell);
        var periodo = dwell + tr;
        var passo = Math.floor(now / periodo) + hoverOffset;
        var idx = ((passo % n) + n) % n;
        var prox = (idx + 1) % n;
        var dentro = now % periodo;
        var selecionado = dentro < dwell ? idx : prox;
        selecionarCard(selecionado);
    }

    function selecionarCard(idx) {
        if (idx === ultimoSelecionado) return;
        ultimoSelecionado = idx;
        for (var i = 0; i < cards.length; i++) {
            cards[i].classList.toggle("sel", i === idx);
        }
    }

    // =====================================================
    // relógio / loop
    // =====================================================

    function atualizarRelogio(nowPerf) {
        var d = new Date();
        var dia = d.getFullYear() * 10000 + (d.getMonth() + 1) * 100 + d.getDate();
        var hora = d.getHours();
        var mudou = dia !== ultimoDia || hora !== ultimaHora;

        if (mudou || nowPerf - lastContagem >= 30000) {
            agora = d;
            ultimoDia = dia;
            ultimaHora = hora;
            lastContagem = nowPerf;
            if (mudou) atualizarCabecalho();
            renderContagem();
        }
    }

    function frame(now) {
        rafId = requestAnimationFrame(frame);
        if (document.hidden) return;

        var intervalo = eff.saver ? 2000 : (1000 / Math.max(1, eff.fps));
        if (lastFrame && now - lastFrame < intervalo) return;

        var dt = lastFrame ? now - lastFrame : intervalo;
        lastFrame = now;
        if (dt > 220) dt = 220;

        atualizarBanners(now, dt);
        atualizarEstrelas(now, dt);
        atualizarHover(now);
        atualizarRelogio(now);
    }

    // =====================================================
    // painel / atalhos
    // =====================================================

    function mostrarAtividade() {
        document.body.classList.remove("ocioso");
        document.body.classList.add("ui-visivel");
        removerDica();
        clearTimeout(idleTimer);
        idleTimer = setTimeout(function () {
            document.body.classList.remove("ui-visivel");
            if (!painelAberto) document.body.classList.add("ocioso");
        }, 3500);
    }

    function abrirPainel() {
        if (painelAberto) return;
        painelAberto = true;
        removerDica();
        document.body.classList.remove("ocioso");
        document.body.classList.add("ui-visivel");
        clearTimeout(idleTimer);

        el.scrim.hidden = false;
        el.painel.setAttribute("aria-hidden", "false");

        requestAnimationFrame(function () {
            document.body.classList.add("painel-aberto");

            // O painel ainda está com visibility:hidden neste frame; o foco
            // só funciona depois que a classe o torna visível.
            requestAnimationFrame(function () {
                try {
                    el.btnFechar.focus({ preventScroll: true });
                } catch (e) {
                    el.btnFechar.focus();
                }
            });
        });
    }

    function fecharPainel() {
        if (!painelAberto) return;
        painelAberto = false;
        document.body.classList.remove("painel-aberto");
        el.painel.setAttribute("aria-hidden", "true");

        setTimeout(function () {
            if (!painelAberto) el.scrim.hidden = true;
        }, 340);

        try {
            el.btnConfig.focus({ preventScroll: true });
        } catch (e) {
            el.btnConfig.focus();
        }
        mostrarAtividade();
    }

    function alternarPainel() {
        if (painelAberto) fecharPainel();
        else abrirPainel();
    }

    function alternarTelaCheia() {
        var raiz = document.documentElement;
        if (!document.fullscreenElement) {
            if (raiz.requestFullscreen) {
                var p = raiz.requestFullscreen();
                if (p && p.catch) p.catch(function () { /* navegador recusou */ });
            }
        } else if (document.exitFullscreen) {
            document.exitFullscreen();
        }
    }

    function removerDica() {
        clearTimeout(hintTimer);
        clearTimeout(showHintTimer);
        document.body.classList.remove("dica-visivel");
    }

    function mostrarDica() {
        document.body.classList.add("dica-visivel");
        clearTimeout(hintTimer);
        hintTimer = setTimeout(function () {
            document.body.classList.remove("dica-visivel");
        }, 7000);
    }

    // =====================================================
    // controles
    // =====================================================

    function preencherControles() {
        el.velocidade.value = String(Math.round(settings.speed * 100));
        el.fluidez.value = String(settings.fps);
        el.particulas.value = String(settings.particles);
        el.hoverMs.value = String(Math.round(settings.hoverMs / 1000));
        el.hoverTr.value = String(settings.hoverTr);
        el.bannerMs.value = String(Math.round(settings.bannerMs / 1000));
        el.bannerTr.value = String(settings.bannerTr);
        el.bannerPan.value = String(settings.bannerPan);
        el.bannerShade.value = String(settings.bannerShade);
        el.hover.checked = settings.hover;
        el.zoom.checked = settings.zoom;
        el.banners.checked = settings.banners;
        el.saver.checked = settings.saver;
        el.idioma.value = settings.lang;
        sincronizarRotulos();
        marcarTemaAtivo();
    }

    function ligarControles() {
        el.velocidade.addEventListener("input", function () {
            settings.speed = clamp(Number(el.velocidade.value) / 100, 0.25, 2, 1);
            sincronizarRotulos();
            recalcularEfetivo();
            salvarSettings();
        });

        el.fluidez.addEventListener("change", function () {
            var v = Number(el.fluidez.value);
            settings.fps = [15, 20, 30, 60].indexOf(v) >= 0 ? v : 30;
            recalcularEfetivo();
            salvarSettings();
        });

        el.particulas.addEventListener("input", function () {
            settings.particles = Math.round(clamp(el.particulas.value, 0, 80, 30));
            sincronizarRotulos();
            recalcularEfetivo();
            salvarSettings();
        });

        el.hover.addEventListener("change", function () {
            settings.hover = el.hover.checked;
            recalcularEfetivo();
            salvarSettings();
        });

        el.hoverMs.addEventListener("input", function () {
            settings.hoverMs = Math.round(clamp(el.hoverMs.value, 3, 15, 9)) * 1000;
            sincronizarRotulos();
            recalcularEfetivo();
            salvarSettings();
        });

        el.hoverTr.addEventListener("input", function () {
            settings.hoverTr = Math.round(clamp(el.hoverTr.value, 0, 2000, 600));
            sincronizarRotulos();
            recalcularEfetivo();
            salvarSettings();
        });

        el.zoom.addEventListener("change", function () {
            settings.zoom = el.zoom.checked;
            recalcularEfetivo();
            salvarSettings();
        });

        el.banners.addEventListener("change", function () {
            settings.banners = el.banners.checked;
            if (settings.banners) reiniciarBanners();
            recalcularEfetivo();
            salvarSettings();
        });

        el.bannerMs.addEventListener("input", function () {
            settings.bannerMs = Math.round(clamp(el.bannerMs.value, 8, 30, 18)) * 1000;
            sincronizarRotulos();
            recalcularEfetivo();
            salvarSettings();
        });

        el.bannerTr.addEventListener("input", function () {
            settings.bannerTr = Math.round(clamp(el.bannerTr.value, 500, 3000, 1500));
            sincronizarRotulos();
            recalcularEfetivo();
            salvarSettings();
        });

        el.bannerPan.addEventListener("input", function () {
            settings.bannerPan = Math.round(clamp(el.bannerPan.value, 0, 60, 10));
            sincronizarRotulos();
            recalcularEfetivo();
            salvarSettings();
        });

        el.bannerShade.addEventListener("input", function () {
            settings.bannerShade = Math.round(clamp(el.bannerShade.value, 30, 95, 68));
            sincronizarRotulos();
            recalcularEfetivo();
            salvarSettings();
        });

        el.saver.addEventListener("change", function () {
            settings.saver = el.saver.checked;
            recalcularEfetivo();
            salvarSettings();
        });

        el.idioma.addEventListener("change", function () {
            settings.lang = el.idioma.value;
            aplicarIdioma();
            salvarSettings();
        });

        el.corTopo.addEventListener("input", function () {
            settings.theme.top = hexValido(el.corTopo.value, settings.theme.top);
            aplicarTema();
            salvarSettings();
        });
        el.corMeio.addEventListener("input", function () {
            settings.theme.mid = hexValido(el.corMeio.value, settings.theme.mid);
            aplicarTema();
            salvarSettings();
        });
        el.corBase.addEventListener("input", function () {
            settings.theme.bot = hexValido(el.corBase.value, settings.theme.bot);
            aplicarTema();
            salvarSettings();
        });

        el.btnRestaurar.addEventListener("click", function () {
            if (!confirm(t("confirmarRestaurar"))) return;
            settings = sanitizeSettings(DEFAULTS);
            try {
                localStorage.removeItem(STORAGE_KEY);
            } catch (e) { /* ignora */ }
            preencherControles();
            aplicarTema();
            aplicarIdioma();
            recalcularEfetivo();
            redimensionarEstrelas();
            reiniciarBanners();
            salvarSettingsAgora();
        });
    }

    // =====================================================
    // eventos globais
    // =====================================================

    function ligarEventosGlobais() {
        el.btnConfig.addEventListener("click", abrirPainel);
        el.btnFechar.addEventListener("click", fecharPainel);
        el.btnFullscreen.addEventListener("click", alternarTelaCheia);
        el.scrim.addEventListener("click", fecharPainel);

        document.addEventListener("keydown", function (e) {
            if (e.key === "Escape") {
                if (painelAberto) {
                    e.preventDefault();
                    fecharPainel();
                }
                return;
            }

            var tag = (e.target && e.target.tagName) || "";
            if (/^(INPUT|SELECT|TEXTAREA)$/.test(tag)) return;
            if (e.ctrlKey || e.metaKey || e.altKey) return;

            if (e.key === "s" || e.key === "S") {
                e.preventDefault();
                mostrarAtividade();
                alternarPainel();
            } else if (e.key === "f" || e.key === "F") {
                e.preventDefault();
                mostrarAtividade();
                alternarTelaCheia();
            }
        });

        ["mousemove", "pointerdown", "wheel"].forEach(function (evento) {
            window.addEventListener(evento, mostrarAtividade, { passive: true });
        });

        window.addEventListener("resize", function () {
            clearTimeout(resizeTimer);
            resizeTimer = setTimeout(function () {
                redimensionarEstrelas();
                agendarAjusteTitulos();
            }, 140);
        });

        document.addEventListener("visibilitychange", function () {
            if (!document.hidden) {
                lastFrame = 0;
                bannerState.lastSwitch = performance.now();
                atualizarRelogio(performance.now());
            }
        });

        window.addEventListener("beforeunload", salvarSettingsAgora);
    }

    // =====================================================
    // init
    // =====================================================

    function cachearElementos() {
        el.body = document.body;
        el.lista = $("lista");
        el.titulo = $("titulo");
        el.hoje = $("hoje");
        el.fundoGrad = $("fundo-grad");
        el.bannerImg = $("banner-img");
        el.bannerOverlay = $("banner-overlay");
        el.bannerWrap = $("banner-wrap");
        el.estrelas = $("estrelas");
        el.acoes = $("acoes");
        el.dica = $("dica");
        el.scrim = $("scrim");
        el.painel = $("painel");
        el.btnConfig = $("btn-config");
        el.btnFullscreen = $("btn-fullscreen");
        el.btnFechar = $("btn-fechar");
        el.btnRestaurar = $("btn-restaurar");

        el.temas = $("temas");
        el.corTopo = $("cor-topo");
        el.corMeio = $("cor-meio");
        el.corBase = $("cor-base");
        el.hexTopo = $("hex-topo");
        el.hexMeio = $("hex-meio");
        el.hexBase = $("hex-base");

        el.velocidade = $("velocidade");
        el.velValor = $("vel-valor");
        el.fluidez = $("fluidez");
        el.particulas = $("particulas");
        el.partValor = $("part-valor");
        el.saver = $("saver");
        el.hover = $("hover");
        el.zoom = $("zoom");
        el.hoverMs = $("hover-ms");
        el.hoverValor = $("hover-valor");
        el.hoverTr = $("hover-tr");
        el.hovertrValor = $("hovertr-valor");
        el.banners = $("banners");
        el.bannerMs = $("banner-ms");
        el.bannerValor = $("banner-valor");
        el.bannerTr = $("banner-tr");
        el.bannertrValor = $("bannertr-valor");
        el.bannerPan = $("banner-pan");
        el.panValor = $("pan-valor");
        el.bannerShade = $("banner-shade");
        el.shadeValor = $("shade-valor");
        el.idioma = $("idioma");

        stars.canvas = el.estrelas;
        stars.ctx = stars.canvas ? stars.canvas.getContext("2d") : null;
    }

    function init() {
        cachearElementos();

        settings = carregarSettings();
        hoverOffset = Math.floor(Math.random() * CL.EVENTOS.length);

        montarCards();
        preencherControles();
        ligarControles();
        aplicarTema();
        aplicarIdioma();
        recalcularEfetivo();
        redimensionarEstrelas();
        iniciarBanners();
        ligarEventosGlobais();

        agora = new Date();
        ultimoDia = "";
        ultimaHora = -1;
        lastContagem = 0;
        atualizarCabecalho();
        renderContagem();

        mostrarAtividade();
        showHintTimer = setTimeout(mostrarDica, 900);

        rafId = requestAnimationFrame(frame);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
