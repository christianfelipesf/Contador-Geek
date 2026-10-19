param(
    [switch]$Kiosk
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$page = Join-Path $root "index.html"

if (-not (Test-Path $page)) {
    Write-Host "index.html nao encontrado em $root" -ForegroundColor Red
    Read-Host "Pressione Enter para fechar"
    exit 1
}

$uri = ([System.Uri]$page).AbsoluteUri

$candidatos = @(
    "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
    "${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe",
    "$env:LOCALAPPDATA\Google\Chrome\Application\chrome.exe",
    "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe",
    "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe"
)

$browser = $candidatos | Where-Object { Test-Path $_ } | Select-Object -First 1

if ($browser) {
    $perfil = Join-Path $env:LOCALAPPDATA "CountdownGeekPc\browser"
    New-Item -ItemType Directory -Force -Path $perfil | Out-Null

    $argumentos = @(
        "--app=$uri",
        "--no-first-run",
        "--disable-features=Translate,MediaRouter",
        "--user-data-dir=`"$perfil`""
    )

    if ($Kiosk) {
        $argumentos += "--kiosk"
    } else {
        $argumentos += "--start-fullscreen"
    }

    Start-Process -FilePath $browser -ArgumentList $argumentos
} else {
    # Sem Chrome/Edge: abre no navegador padrao.
    Start-Process $page
}
