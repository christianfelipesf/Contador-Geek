# 🖥️ Countdown Geek — versão para PC

A mesma contagem regressiva do papel de parede Android, agora em **HTML5 puro**:
gradiente animado, estrelas, banners com crossfade/pan, cartões com barra de
progresso e destaque de um card por vez.

- **Sem instalar nada** para usar: abra `index.html` ou dê dois cliques em
  `abrir-pc.cmd`.
- **Sem internet**: fontes do sistema, imagens locais e configurações em
  `localStorage`.
- **Três idiomas**: português, inglês e espanhol (detecta o idioma do sistema).
- **Mesmas datas e banners** do app Android.

> Testado no Microsoft Edge (Chromium) no Windows 10/11. Chrome também funciona.
> A pasta inteira pode ser usada no [Lively Wallpaper](https://www.rocksdanister.com/lively/)
> como papel de parede animado.

---

## 🚀 Abrir

### Opção 1 — atalho (recomendado)

Dê dois cliques em **`abrir-pc.cmd`**.

Ele procura Chrome/Edge e abre o `index.html` em modo app, em tela cheia:

```text
pc\abrir-pc.cmd
```

Para o modo quiosque (sem barra, sem saída fácil — use `Alt+F4`):

```powershell
pc\abrir-pc.cmd -Kiosk
```

### Opção 2 — navegador

Abra `pc/index.html` no Chrome/Edge e pressione **F** para tela cheia.

### Opção 3 — app Electron (opcional)

Precisa de Node.js. Na pasta `pc`:

```powershell
npm install
npm start
```

Janela normal (para testar/editar):

```powershell
npm start -- --windowed
```

---

## 🖼️ Usar como papel de parede do Windows

### Lively Wallpaper (grátis)

1. Instale o [Lively Wallpaper](https://www.rocksdanister.com/lively/) ou pegue
   na Microsoft Store.
2. **Add Wallpaper** → escolha `pc/index.html` (ou a pasta `pc`).
3. Selecione o monitor e clique em **Set as Wallpaper**.
4. Em **Settings**, ative *Pause on fullscreen* / *Pause on battery* se quiser.

O Lively aceita páginas HTML locais e mantém o JavaScript rodando atrás dos
ícones. O `abrir-pc.cmd` **não** é necessário nesse caso.

### Outros apps

Qualquer programa que aceite um wallpaper HTML (Wallpaper Engine, Lively,
browsers em quiosque etc.) pode apontar para `pc/index.html`. Mantenha a pasta
`pc` inteira junta: o HTML carrega `css/`, `js/` e `banners/` por caminho
relativo.

---

## 🎮 Controles

| Tecla / ação | O que faz |
|---|---|
| **S** | abre/fecha as configurações |
| **F** | entra/sai da tela cheia |
| **Esc** | fecha as configurações (e sai da tela cheia, se estiver nela) |
| mexer o mouse | mostra os botões de configurações e tela cheia |
| 3,5 s parado | esconde os botões e o cursor (modo papel de parede) |

Tudo que você mudar no painel é aplicado na hora e salvo neste computador.
**Restaurar padrão** apaga essas preferências.

---

## ⚙️ O que dá para ajustar

| Grupo | Opções |
|---|---|
| Aparência | 6 temas prontos (Roxo, Oceano, Fogo, Verde, Sunset, Meia-noite) e cor personalizada de topo/meio/base |
| Animação | velocidade 25%–200%, fluidez 60/30/20/15 fps, 0–80 partículas, modo economizador |
| Cards | destaque de um por vez, tempo por card (3–15 s), transição (0–2000 ms), zoom |
| Banners | liga/desliga, tempo por banner (8–30 s), transição (500–3000 ms), pan lateral (0–60 px/s), cobertura roxa (30–95%) |
| Sistema | idioma (sistema/pt/en/es), restaurar padrão |

O **modo economizador** congela o fundo, desliga estrelas/destaque/zoom e
atualiza a contagem só de 2 em 2 segundos — igual ao app Android.

---

## 🗓️ Datas

As datas ficam em **um único lugar**: `pc/js/countdown.js`, no array `EVENTOS`:

| Evento | Data | Cor |
|---|---|---|
| 👁️ Vision Quest — Ep 3 e 4 | 21/10/2026 | `#8B5CF6` |
| 🗡️ Zelda OOT Remake | 05/11/2026 | `#22C55E` |
| 🚗 GTA 6 | 19/11/2026 | `#F59E0B` |
| 🛡️ Vingadores Doomsday | 18/12/2026 | `#EF4444` |

A barra de progresso usa `15/10/2026` como início, a mesma data do Android.
Para trocar um evento, edite `titulo`, `subtitulo`, `data`, `cor` e `emoji` lá.

---

## 🧪 Testes

A lógica pura (dias, horas, fração da barra, idiomas) é testada no Node:

```powershell
cd pc
npm test
```

ou, sem npm:

```powershell
node --test test/countdown.test.js test/markup.test.js
```

São 12 testes, incluindo o caso de **meia-noite exata** (que no app antigo
mostrava `24h`) e verificações de IDs únicos no HTML.

---

## 📁 Estrutura

```text
pc/
├── index.html          # tela do wallpaper
├── css/style.css       # visual responsivo (1 coluna no retrato, 2x2 no paisagem)
├── js/countdown.js     # lógica pura + datas (também roda no Node)
├── js/app.js           # interface, banners, estrelas, configurações
├── banners/            # as 4 imagens (copiadas de app/src/main/assets)
├── electron/main.js    # envelope opcional para desktop
├── abrir-pc.cmd        # atalho Windows (modo app)
├── abrir-pc.ps1        # implementação do atalho
├── test/               # testes da lógica
└── package.json        # Electron + npm test
```

---

## ⚠️ Notas

- O arquivo é **local**: nada é enviado para a internet.
- O navegador guarda as preferências por origem. Se você abrir por
  `file://`, o `localStorage` fica associado ao caminho do arquivo; se mover a
  pasta, as configurações voltam ao padrão.
- Em monitores 16:9 o layout usa **2 colunas × 2 linhas**; em janelas verticais
  ou telas estreitas, **1 coluna × 4 cartões** — a mesma regra do app.
- Se o navegador bloquear tela cheia automática, pressione **F** uma vez.
