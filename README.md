<div align="center">

# ⏳ Countdown Geek

**Papel de parede animado que mostra, ao vivo, quanto falta para os lançamentos que você não quer perder.**

Gradiente roxo em OpenGL ES, estrelas no shader e cartões que vão escurecendo conforme o tempo passa.

<img src="print/Screenshot_20261017_103832_One%20UI%20Home.jpg" width="250" alt="Countdown Geek rodando no Galaxy A17 5G">

[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B%20%28API%2026%29-3DDC84?logo=android&logoColor=white)](#-requisitos)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![OpenGL ES 2.0](https://img.shields.io/badge/OpenGL%20ES-2.0-5586A4?logo=opengl&logoColor=white)](#-como-funciona)
[![Testes](https://img.shields.io/badge/testes-18%20passando-success)](#-testes)
[![APK](https://img.shields.io/badge/APK-2%2C3%20MB-blue)](#-gerar-o-apk)
[![Baixar APK](https://img.shields.io/badge/%E2%AC%87-baixar%20APK-8B5CF6?logo=android&logoColor=white)](https://github.com/christianfelipesf/Contador-Geek/releases/latest/download/CountdownGeek-release.apk)
[![Baixar PC](https://img.shields.io/badge/%E2%AC%87-baixar%20PC-8B5CF6?logo=windows&logoColor=white)](https://github.com/christianfelipesf/Contador-Geek/releases/latest/download/CountdownGeek-PC.zip)
[![Idiomas](https://img.shields.io/badge/idiomas-pt%20%7C%20en%20%7C%20es-orange)](#-idiomas)

</div>

---

## ✨ O que ele faz

- **4 contagens regressivas** nos cartões, sempre atualizadas sozinhas: o cartão é regerado (novo bitmap) quando o dia ou a hora vira.
- **Dias + horas**: `62 DIAS` com `10h` embaixo — nada de minutos poluindo a tela. Na véspera aparece **`AMANHÃ!`**, no dia **`É HOJE!`** (com as horas que ainda faltam pra acabar o dia) e depois **`JÁ SAIU!`**.
- **Barra de progresso dentro do cartão**: o trecho escuro mostra quanto do tempo já passou, com uma **barrinha colorida** colada na quina esquerda e um traço marcador na altura cheia.
- **Fundo roxo** com gradiente, estrelas e brilhos que se mexem devagar (shader, não vídeo).
- **Banners geek** opcionais no fundo, cobertos por uma camada roxa ajustável.
- **Destaque por vez**: um cartão se ilumina (e cresce, se quiser) por 9 segundos, depois passa pro próximo.
- **Modo economizador**: mínimo consumo, sem aquecer, sem perder o visual.

<div align="right"><a href="#-countdown-geek">↑ voltar ao topo</a></div>

## 🗓️ As contagens

| Evento | Data | Cor |
|---|---|---|
| 👁 **Vision Quest** — Ep 3 e 4 · Disney+ | 21/10/2026 | `#8B5CF6` |
| 🗡️ **Zelda OOT Remake** — Switch 2 | 05/11/2026 | `#22C55E` |
| 🚗 **GTA 6** — PS5 / Xbox | 19/11/2026 | `#F59E0B` |
| 🛡️ **Vingadores Doomsday** — Cinema | 18/12/2026 | `#EF4444` |

> As datas ficam em um único lugar (`Eventos.TODOS`, em `Eventos.kt`) e a proporção da barra é calculada a partir de `15/10/2026` — dia em que a contagem começou.
>
> **Pra adicionar um evento:** acrescente um `Def` em `Eventos.TODOS` e as strings `ev_<id>_titulo` / `ev_<id>_sub` (mais a cor `ev_<id>`) — o número de cartões, o layout e a prévia se ajustam sozinhos. Na versão PC a mesma lista fica em `pc/js/countdown.js`.

<div align="right"><a href="#-countdown-geek">↑ voltar ao topo</a></div>

## 🧩 Recursos

| Personalização | Animação | Sistema |
|---|---|---|
| Cor do gradiente: 6 temas prontos (Roxo, Oceano, Fogo, Verde, Sunset, Meia-noite) ou hex manual | Velocidade global 25%–200% | **Modo economizador** (~0,5 fps, zero partículas) |
| Cobertura roxa sobre os banners (30%–95%) | Fluidez: Performance / Equilíbrio / Eco / Super eco | **3 idiomas** com troca por app |
| Pan lateral dos banners (0–60 px/s) | 0–80 partículas (estrelas) | **Responsivo**: celular pequeno, tablet e paisagem |
| Tempo por card (1–15 s) e transição | Destaque de um card por vez + zoom | Backup/Smart Switch das configurações |
| Tempo por banner, transição e zoom | Animação do fundo congelável | Ícone temático (Material You) + splash |

<div align="right"><a href="#-countdown-geek">↑ voltar ao topo</a></div>

## 🖼️ Ver sem compilar

Três prévias que abrem no navegador — úteis para mexer em layout/datas antes de gerar APK:

| Arquivo | O que mostra |
|---|---|
| [`preview.html`](preview.html) | O papel de parede animado completo, no navegador |
| [`preview_cards.html`](preview_cards.html) | Os cartões em **5 telas** (celular pequeno, A17 5G, tablet em pé, tablet deitado, dobrável) usando as mesmas proporções do app |
| [`preview_icone.html`](preview_icone.html) | O ícone do launcher (normal, redondo, adaptativo e monocromático) |

<div align="right"><a href="#-countdown-geek">↑ voltar ao topo</a></div>

## 🖥️ Versão para PC

A pasta [`pc/`](pc/) tem uma versão desktop em **HTML5 puro**, com as mesmas
datas, banners, temas, modo economizador e 3 idiomas. Você pode baixar o zip na
release ou rodar direto do repositório:

```powershell
pc\abrir-pc.cmd        # abre em modo app, em tela cheia
```

[⬇ Baixar CountdownGeek-PC.zip](https://github.com/christianfelipesf/Contador-Geek/releases/latest/download/CountdownGeek-PC.zip)
(extraia a pasta e abra `abrir-pc.cmd` no Windows, ou `index.html` em qualquer
navegador).

| O que | Onde |
|---|---|
| Página do wallpaper | [`pc/index.html`](pc/index.html) |
| Atalho Windows (Chrome/Edge) | [`pc/abrir-pc.cmd`](pc/abrir-pc.cmd) |
| Guia completo (Lively Wallpaper, Electron, atalhos e ajustes) | [`pc/README.md`](pc/README.md) |
| Baixar o zip pronto | [CountdownGeek-PC.zip](https://github.com/christianfelipesf/Contador-Geek/releases/latest/download/CountdownGeek-PC.zip) |
| Testes da lógica no Node | `cd pc && npm test` (12 testes) |

No Windows, dá para usar como papel de parede de verdade pelo
[Lively Wallpaper](https://www.rocksdanister.com/lively/): **Add Wallpaper** →
selecione `pc/index.html` → **Set as Wallpaper**.

<div align="right"><a href="#-countdown-geek">↑ voltar ao topo</a></div>

## 🏗️ Como funciona

```
Choreographer (vsync)
      └── VsyncLoop  (throttle para 30 fps, fallback por handler)
              └── GL thread
                    ├── shader de fundo: gradiente + estrelas + brilhos
                    ├── banner cover-fit + camada roxa ajustável
                    └── cartões assados em bitmap: título, subtítulo, dias, horas e barra
```

| Arquivo | Papel |
|---|---|
| `CountdownLogic.kt` | **Lógica pura** (dias, horas extras, fração da barra) — sem Android, 100% testada |
| `Textos.kt` | Textos injetados por interface: `TextosRecursos` (strings.xml) no app, `TextosPadrao` nos testes |
| `CountdownWallpaperService.kt` | O `WallpaperService`: layout responsivo, hover, banners, ciclo de rebake, preferências |
| `GlEngine.kt` | Shaders, `QuadRenderer` e o bitmap do cartão (quina, barrinha, trecho escuro, marcador) |
| `PowerEngine.kt` | `PowerEgl` (EGL, ES2 com fallback ES3) e `HalfResFbo` (fundo em meia-resolução) |
| `VsyncLoop.kt` | Loop no vsync de verdade, com throttle e fallback por handler |
| `PrefsCache.kt` | Snapshot imutável das configs, lido **1 vez** — zero I/O na thread GL |
| `MainActivity.kt` | Tela de ajustes (tema, sliders, cores, idioma) com prévia ao vivo |
| `BannerPreviewView.kt` | A prévia ao vivo desenhada na tela de ajustes |

<details>
<summary><b>🔥 Economia de bateria e performance</b></summary>

- **Renderização**: `eglMakeCurrent` só quando necessário, `eglSwapInterval(1)` (sem tearing), desenho nunca bloqueia a main thread.
- **Fundo em meia-resolução** (`HalfResFbo`): corta ~75% do fillrate do shader mais caro e o upscale bilinear sai de graça. Em GPU fraca (Mali-G68 do A17 5G) é o que mais destrava.
- **Frames**: 16 ms (60 fps) · 33 ms (30 fps, padrão) · 50 ms · 66 ms (15 fps) · **2000 ms** no economizador.
- **Modo economizador** (`PrefsCache.economia()`): sobrepõe os valores **só na RAM** — desliga estrelas, destaque, zoom e pan, congela o tempo do shader e redesenha de 2 em 2 segundos. Ao desligar, tudo volta exatamente como você deixou.
- **Cards assados em bitmap**: o texto e a barra viram textura uma vez por dia/hora, não por frame.
- Estrelas no shader são um hash por pixel — por isso o slider de partículas vai a **0** e não há custo de CPU.

</details>

<div align="right"><a href="#-countdown-geek">↑ voltar ao topo</a></div>

## 📱 Várias telas (celular, tablet, paisagem)

Todo o layout é **proporcional à tela** — nenhum tamanho fixo em pixels:

| Tela | Layout |
|---|---|
| Retrato (celular pequeno, comum ou tablet em pé) | 1 coluna × 4 cartões |
| Paisagem, tablet deitado, dobrável aberto (a partir de 1,1:1) | 2 colunas × 2 linhas |

- Quina do cartão, barrinha colorida, respiros internos, tamanho das fontes e o halo do destaque escalam junto (num cartão de 820 px a quina dá os mesmos 32 px de sempre).
- Títulos **encolhem sozinhos** se ameaçarem encostar nos dias/horas.
- Tela de ajustes: dimensões próprias para tablet (`values-sw600dp`) e paisagem (`values-land`), com a prévia centralizada em vez de esticada.

<div align="right"><a href="#-countdown-geek">↑ voltar ao topo</a></div>

## 🌎 Idiomas

| Idioma | Nome do app | Onde |
|---|---|---|
| 🇺🇸 Inglês | Countdown Geek | `values/strings.xml` (padrão/fallback internacional) |
| 🇧🇷🇵🇹 Português | **Contador Geek** | `values-pt/strings.xml` (cobre pt-BR e pt-PT) |
| 🇪🇸 Espanhol | Contador Geek | `values-es/strings.xml` |

- O **nome é traduzido junto**: aparece no launcher, no seletor de papéis de parede, no título grande do wallpaper (`CONTADOR GEEK`) e nos textos de ajuda.

- **Troca por app** (`locales_config.xml` + `android:localeConfig`): Ajustes → Apps → Countdown Geek → Idioma (Android 13+ / One UI 5+). Também tem um seletor dentro do app.
- **Plural de verdade** (`1 DAY` / `2 DAYS`) e **datas no formato do idioma** (`18 de dez.`, `Dec 18`, `18 dic`).
- Trocar o idioma **não exige reaplicar o wallpaper**: o serviço reassa tudo sozinho.
- Traduzir é só copiar uma pasta `values-<idioma>/strings.xml` e trocar os textos.

<div align="right"><a href="#-countdown-geek">↑ voltar ao topo</a></div>

## 🛠️ Requisitos

- **JDK 17** · **Gradle 8.7** · **Android SDK**: platform 34 + build-tools 34.0.0
- AGP 8.5.2 · Kotlin 1.9.24 · `compileSdk 34` · `minSdk 26` · `targetSdk 34`
- Dependências: `core-ktx`, `kotlinx-coroutines-android`, JUnit 4 (nenhuma lib de UI)

<div align="right"><a href="#-countdown-geek">↑ voltar ao topo</a></div>

## 📦 Gerar o APK

```powershell
# na raiz do projeto
gradle :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease --offline --console=plain
```

Saída:

| Build | Caminho | Tamanho |
|---|---|---|
| Release (assinado, instala por cima) | `app\build\outputs\apk\release\app-release.apk` | ~2,3 MB |
| Debug | `app\build\outputs\apk\debug\app-debug.apk` | ~2,8 MB |

- `versionName` = `1.0.` + data/hora (ex.: `1.0.20261017-1058`) e `versionCode` = segundos desde 01/01/2026 — gerados automaticamente a cada build.
- A assinatura do release vem de `local.properties` (fora do Git). Sem as chaves, o release sai sem assinar.
- `--offline` funciona depois do primeiro build (que baixa as dependências).
- Sem Android Studio? Siga [`COMPILAR-SEM-ANDROID-STUDIO.txt`](COMPILAR-SEM-ANDROID-STUDIO.txt) (5 opções, da nuvem ao Termux) ou [`LEIA-ME-COMO-GERAR-APK.txt`](LEIA-ME-COMO-GERAR-APK.txt).
- **Na nuvem, sem instalar nada**: o workflow [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml) roda os testes, compila o debug e publica o APK direto no **GitHub Releases** (veja abaixo).

<div align="right"><a href="#-countdown-geek">↑ voltar ao topo</a></div>

## 🚀 GitHub Releases (download automático)

Todo push na `main` compila e **publica o APK na página de Releases**. Dá pra instalar direto do celular, sem baixar zip de artifact:

```text
https://github.com/christianfelipesf/Contador-Geek/releases/latest/download/CountdownGeek-release.apk
```

| Quando | O que acontece |
|---|---|
| Push na `main` ou execução manual (*Actions → Build & Release APK → Run workflow*) | Atualiza a release de tag `latest` — **o link acima nunca muda** |
| Push de uma tag `v*` (`git tag v1.0 && git push origin v1.0`) | Cria a release daquela versão, com o mesmo APK |
| Sem os secrets de assinatura configurados | Publica o **debug** no lugar do release (senão sairia um APK sem assinatura, que não instala) |

O workflow roda os 18 testes → compila debug e release (assinado) → sobe os dois como *artifact* do Actions → anexa na release com o resumo pronto (versão, commit, data e passo a passo de instalação).

### Configurar a assinatura (uma vez, ~2 min)

A chave **nunca** vai pro repositório: ela é guardada num secret, em base64.

1. Gere o base64 da keystore:

   ```powershell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\Android\countdown-release.keystore")) | Set-Clipboard
   ```
2. No GitHub: **Settings → Secrets and variables → Actions → New repository secret**, criando os quatro:

| Secret | Valor |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | o texto que ficou na área de transferência |
| `RELEASE_STORE_PASSWORD` | senha da keystore (`release.storePassword`) |
| `RELEASE_KEY_ALIAS` | alias da chave (`release.keyAlias`) |
| `RELEASE_KEY_PASSWORD` | senha da chave (`release.keyPassword`) |

> ⚠️ **Use a mesma keystore do `local.properties`.** É ela que garante que o APK da release instala por cima da versão que já está no celular — assinatura diferente exigiria desinstalar antes.
>
> Sem os secrets, a release sai apenas com `CountdownGeek-debug.apk`; o link `…/CountdownGeek-release.apk` só passa a existir depois que a assinatura estiver configurada (basta rodar o workflow de novo em *Actions → Run workflow*).

<div align="right"><a href="#-countdown-geek">↑ voltar ao topo</a></div>

## ✅ Testes

`CountdownLogic` é Kotlin puro (sem Android), então a contagem é testada de verdade:

```powershell
gradle :app:testDebugUnitTest --offline --console=plain
```

**19 testes** cobrindo dias restantes, plural, véspera/hoje/já saiu, horas extras (incluindo o resto do dia do evento, o fim da tarde na véspera e a meia-noite exata), a fração da barra nos extremos e os textos vindos do idioma injetado.

<div align="right"><a href="#-countdown-geek">↑ voltar ao topo</a></div>

## 📁 Estrutura

```
.
├── app/src/main/
│   ├── java/com/exemplo/countdownwallpaper/   # código Kotlin
│   ├── assets/banners/                        # imagens dos banners
│   └── res/
│       ├── values/            values-pt/  values-es/   # textos
│       ├── values-sw600dp/    values-land/             # telas grandes
│       ├── values-v31/                                 # splash screen
│       ├── drawable/ mipmap-anydpi-v26/                # ícones
│       └── xml/                                        # wallpaper, idiomas, backup
├── app/src/test/                                       # 18 testes unitários
├── pc/                                                 # versão para PC (HTML5)
├── preview.html  preview_cards.html  preview_icone.html
└── print/                                              # capturas no aparelho
```

<div align="right"><a href="#-countdown-geek">↑ voltar ao topo</a></div>

## 📝 Notas

- A tela de ajustes é **sempre escura**, de propósito — é a identidade do app, mesmo com o One UI no modo claro.
- Nos Samsung (One UI), o tema usa `Theme.DeviceDefault` como base: checkbox, sliders e spinner saem no visual nativo, tingidos de roxo.
- O wallpaper foi calibrado no **Galaxy A17 5G** (1080 × 2340), mas o layout escala para qualquer tela.

<div align="center">
<sub>Feito para nunca mais perder um lançamento. 🚀</sub>
</div>
