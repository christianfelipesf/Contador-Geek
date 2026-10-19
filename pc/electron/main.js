/*
 * Envelope opcional para rodar o wallpaper como um app de desktop.
 * O modo principal (e recomendado para wallpaper) é abrir o index.html
 * no navegador ou usar o Lively Wallpaper — veja o README.md da pasta pc.
 */
const path = require("path");
const { app, BrowserWindow } = require("electron");

const janela = !process.argv.includes("--windowed");
const kiosk = process.argv.includes("--kiosk");

function criarJanela() {
    const win = new BrowserWindow({
        width: 1600,
        height: 900,
        fullscreen: janela,
        kiosk: kiosk,
        frame: !janela,
        backgroundColor: "#0B1020",
        show: false,
        autoHideMenuBar: true,
        title: "Contador Geek — PC",
        webPreferences: {
            contextIsolation: true,
            nodeIntegration: false,
            sandbox: true,
            backgroundThrottling: false
        }
    });

    win.loadFile(path.join(__dirname, "..", "index.html"));
    win.once("ready-to-show", () => win.show());
    win.setMenuBarVisibility(false);

    // F11 alterna a janela cheia sem depender do menu do Electron.
    win.webContents.on("before-input-event", (event, input) => {
        if (input.type === "keyDown" && input.key === "F11") {
            win.setFullScreen(!win.isFullScreen());
            event.preventDefault();
        }
    });
}

app.whenReady().then(() => {
    criarJanela();

    app.on("activate", () => {
        if (BrowserWindow.getAllWindows().length === 0) criarJanela();
    });
});

app.on("window-all-closed", () => {
    if (process.platform !== "darwin") app.quit();
});
