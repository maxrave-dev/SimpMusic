@echo off
REM SimpMusic offline installer for Windows.
REM Double-click this file. UAC will prompt for admin (needed for cert + sideload).
REM
REM Bundle requirement (same folder as this .bat):
REM   - simpmusic.crt
REM   - simpmusic-<version>.msix     (any .msix matching simpmusic-*.msix)
REM
REM Only x64 MSIX is shipped — Windows 11 ARM64 hosts run it under Prism
REM emulation. ARM64 native build is blocked upstream by a missing
REM androidx.sqlite-bundled windows-arm64 native binary.

setlocal EnableExtensions
cd /d "%~dp0"

REM --- 1. Self-elevate to admin -----------------------------------------------
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo Requesting admin elevation...
    powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
)

echo.
echo === SimpMusic offline installer ===
echo.

REM --- 2. Install cert to Trusted People (idempotent via certutil) -----------
if not exist "%~dp0simpmusic.crt" (
    echo [ERROR] simpmusic.crt not found next to install.bat
    pause
    exit /b 1
)

echo [1/3] Importing simpmusic.crt to LocalMachine\TrustedPeople ...
certutil -addstore -f "TrustedPeople" "%~dp0simpmusic.crt"
if %errorlevel% neq 0 (
    echo [ERROR] Certificate import failed.
    pause
    exit /b 1
)

REM --- 3. Find the bundled MSIX ----------------------------------------------
set "MSIX="
for %%f in ("%~dp0simpmusic-*.msix") do set "MSIX=%%f"

if not defined MSIX (
    echo [ERROR] No simpmusic-*.msix found in this folder.
    pause
    exit /b 1
)

echo.
echo [2/3] Installing MSIX: "%MSIX%"
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "try { Add-AppxPackage -Path '%MSIX%' -ForceApplicationShutdown -ErrorAction Stop; exit 0 } catch { Write-Host $_; exit 1 }"

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Add-AppxPackage failed. Common fixes:
    echo   - Enable 'Install apps from any source, including loose files'
    echo     ^(Settings -^> Privacy ^& security -^> For developers^)
    echo   - Restart Windows and run install.bat again
    pause
    exit /b 1
)

REM --- 4. Launch app ---------------------------------------------------------
echo.
echo [3/3] Launching SimpMusic ...
start "" "shell:appsFolder\Simpmusic_ejp2bhxmz1qq6!Simpmusic"

echo.
echo SimpMusic installed successfully.
echo To uninstall: Settings -^> Apps -^> Installed apps -^> SimpMusic -^> Uninstall
echo.
pause
endlocal
