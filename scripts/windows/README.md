# SimpMusic Windows .EXE Setup Installer Suite (v1.6.1+)

This directory contains the offline Windows `.EXE` setup installer tools for SimpMusic Desktop.

## Overview

SimpMusic builds Windows distribution artifacts via Conveyor (`.msix` app packages) and Compose Desktop (`jpackage` native distributions). To provide a seamless, double-clickable `SimpMusic-Setup-<version>-windows-x64.exe` setup wizard (with automatic UAC elevation, certificate trust for signed packages, and desktop/Start Menu shortcut creation), this tool suite provides:

1. **`SimpMusic.iss`**: An Inno Setup 6+ script configured for SimpMusic Desktop. It packages `simpmusic-*.msix`, `simpmusic.crt`, and `install.bat` into a single high-compression `SimpMusic-Setup-<version>-windows-x64.exe` setup wizard.
2. **`generate_sfx_exe.ps1` (and `generate_sfx_exe.bat`)**: An automated build script that detects if Inno Setup (`ISCC.exe`) is installed and compiles `SimpMusic.iss`. If Inno Setup is not installed, it automatically falls back to Windows built-in `IExpress.exe` (included natively on all Windows installations) to generate a self-extracting `.exe` setup installer that prompts for installation, elevates UAC, imports the certificate, and installs the package.

## Local & Build Usage

### On Windows hosts:
After building the Windows MSIX via Conveyor (`conveyor -Kapp.machines=windows.amd64 make windows-msix` / `make site`) or `jpackage` (`./gradlew :desktopApp:packageExe`), simply double-click `generate_sfx_exe.bat` or run:

```powershell
.\generate_sfx_exe.ps1 -Version "1.6.1"
```

The compiled setup executable will be placed in `installers/SimpMusic-Setup-1.6.1-windows-x64.exe`.

### On Linux / CI Runner (GitHub Actions):
If building on Linux (`ubuntu-22.04` runner in `desktop-package.yml`), you can generate the Windows `.EXE` self-extracting installer directly using 7-Zip (`7zz` + `7zSD.sfx`):

```bash
# Example snippet for .github/workflows/desktop-package.yml:
curl -fsSL https://github.com/ip7z/7zip/releases/download/26.01/7z2601-extra.7z -o /tmp/7z-extra.7z
7z e -y /tmp/7z-extra.7z -o/tmp/sfx 7zSD.sfx
7z a -t7z -mx=9 setup.7z install.bat simpmusic.crt "$msix"
cat << 'EOF' > config.txt
;!@Install@!UTF-8!
Title="SimpMusic Setup"
BeginPrompt="Do you want to install SimpMusic?"
RunProgram="install.bat"
;!@InstallEnd@!
EOF
cat /tmp/sfx/7zSD.sfx config.txt setup.7z > SimpMusic-Setup-windows-x64.exe
```

## Compose Desktop Native `.EXE`
In addition to the SFX suite, `desktopApp/build.gradle.kts` explicitly configures `TargetFormat.Exe` in `nativeDistributions` when running on Windows hosts:

```bash
./gradlew :desktopApp:packageExe --no-configuration-cache
```
