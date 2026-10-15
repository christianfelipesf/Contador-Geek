# Countdown Geek — Live Wallpaper

Live wallpaper animado com contagem regressiva geek:
**Vision Quest • Zelda OOT Remake • GTA 6 • Vingadores: Doomsday**,
com gradiente roxo animado, brilhos orbitando, partículas e seleção de card em "hover" (7s cada, de cima pra baixo).

## Pré-requisitos

| Ferramenta | Versão | Observação |
|---|---|---|
| JDK | 17 (Temurin) | O Gradle 8.7 **não** roda em Java 25+ |
| Gradle | 8.7 | `gradle --version` pra conferir |
| Android SDK | platform `android-34` + `build-tools;34.0.0` | via `sdkmanager` |
| GitHub Actions | — | build na nuvem, zero instalação (ver abaixo) |

Config do projeto: AGP 8.5.2, Kotlin 1.9.24, `compileSdk/targetSdk 34`, `minSdk 26`.

## Opção A — compilar na nuvem (recomendado, nada pra instalar)

O workflow `.github/workflows/build-apk.yml` já faz tudo (SDK do runner + testes + build):

```powershell
git add -A; git commit -m "build"; git push
```

Depois: GitHub → **Actions → Build Wallpaper APK → último run → artefato `countdown-wallpaper-apk`** (`app-debug.apk` dentro).

## Opção B — compilar no PC (Windows, sem Android Studio)

```powershell
# 1. JDK 17 + Gradle 8.7
winget install EclipseAdoptium.Temurin.17.JDK
# Gradle: baixe https://services.gradle.org/distributions/gradle-8.7-bin.zip
# extraia em C:\Gradle e ponha C:\Gradle\gradle-8.7\bin no Path

# 2. Só o cmdline-tools (~130 MB): https://developer.android.com/studio#command-line-tools-only
# Descompacte para: C:\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat

# 3. Variáveis (troque de sessão ou rode como admin p/ Machine)
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:ANDROID_HOME="C:\Android\Sdk"; $env:ANDROID_SDK_ROOT="C:\Android\Sdk"

# 4. Plataformas + licenças
sdkmanager --install "platform-tools" "platforms;android-34" "build-tools;34.0.0"
cmd /c "(for /L %i in (1,1,80) do @echo y) | sdkmanager --licenses"

# 5. Teste + build
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
# APK em: app\build\outputs\apk\debug\app-debug.apk
```

Guia detalhado (com prós/contras de cada método): `COMPILAR-SEM-ANDROID-STUDIO.txt`.

## Teste básico

Teste unitário rápido (JUnit, sem emulador) da lógica de contagem — `CountdownLogic` (`diasFaltam`/`textoDias`):

```powershell
gradle :app:testDebugUnitTest
```

Relatório em: `app\build\reports\tests\testDebugUnitTest\index.html`

## Instalar e ativar no celular

1. Passe o `app-debug.apk` pro celular e instale.
2. Abra o app **Countdown Wallpaper** → **"Definir como papel de parede"**
   (ou segure na tela inicial → Papel de parede → Papéis de parede animados → Countdown Geek).

## Configurações (dentro do app)

Na tela do app dá pra ajustar sem recompilar (vale na hora):
- **Animação:** velocidade (25–200%), fluidez (~8–60 fps, fundo em GPU), partículas (0–80)
- **Seleção:** hover liga/desliga, tempo parado por card (3–15s), transição entre cards (0–2s), zoom
- **Cores:** temas prontos (Roxo, Oceano, Fogo) ou hex personalizado + restaurar padrão

## Preview no PC

Abra `preview.html` no navegador: mesmo layout, gradiente animado e contagem ao vivo (espelho do wallpaper).

## Estrutura

```
app/src/main/java/com/exemplo/countdownwallpaper/
  CountdownWallpaperService.kt  # live wallpaper (gradiente + cards + hover)
  CountdownLogic.kt             # lógica pura da contagem (testável)
  MainActivity.kt               # tela com botão "Definir como papel de parede"
app/src/main/res/drawable/cover_*.xml  # placeholders das capas (troque por PNG/JPG de mesmo nome)
app/src/test/.../CountdownLogicTest.kt  # teste unitário
```

## Problemas comuns (todos já corrigidos aqui)

- `sdkmanager: comando não encontrado` → use o caminho cheio `C:\Android\Sdk\cmdline-tools\latest\bin\sdkmanager`.
- Licenças recusadas no PowerShell → aceite via `cmd /c "(for /L ...) | sdkmanager --licenses"`.
- `Could not set unknown property 'isMinifyEnabled'` → arquivos `.gradle` usam sintaxe Groovy (`minifyEnabled false`), não Kotlin.
- `android.useAndroidX is not enabled` → `gradle.properties` precisa de `android.useAndroidX=true`.
- `android:exported needs to be specified` → todo componente com `intent-filter` (targetSdk 34) exige `android:exported`.
- `Class is not allowed here` (Kotlin) → `data class` não pode ficar dentro de `inner class`; vai no nível do serviço.
- Gradle com Java 25 → force o JDK 17 via `JAVA_HOME`.
