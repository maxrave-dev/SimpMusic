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
    set "SIMPMUSIC_INSTALL_BAT=%~f0"
    powershell -NoProfile -Command "Start-Process -FilePath $env:SIMPMUSIC_INSTALL_BAT -Verb RunAs"
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
set "SIMPMUSIC_DIR=%~dp0"
set "MSIX="
set /a MSIX_COUNT=0
for %%f in ("%~dp0simpmusic-*.msix") do set /a MSIX_COUNT+=1
for /f "usebackq delims=" %%f in (`powershell -NoProfile -Command "Get-ChildItem -LiteralPath $env:SIMPMUSIC_DIR -Filter 'simpmusic-*.msix' -File | Sort-Object -Property @{Expression={ [regex]::Replace($_.Name, '\d+', { $args[0].Value.PadLeft(10, '0') }) }; Descending=$true}, @{Expression='LastWriteTimeUtc'; Descending=$true} | Select-Object -First 1 -ExpandProperty FullName"`) do set "MSIX=%%f"

if not defined MSIX (
    echo [ERROR] No simpmusic-*.msix found in this folder.
    pause
    exit /b 1
)
if %MSIX_COUNT% gtr 1 (
    echo [WARN] Multiple simpmusic-*.msix files found; using "%MSIX%"
)

echo.
echo [2/3] Installing/updating MSIX: "%MSIX%"
REM -ForceUpdateFromAnyVersion overwrites an existing install in place (keeps app
REM data) even when the version is identical, so no manual uninstall is needed.
REM If that still fails (e.g. signing publisher changed), fall back to removing
REM the old package and doing a clean install.
set "SIMPMUSIC_MSIX=%MSIX%"
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "try { Add-AppxPackage -Path $env:SIMPMUSIC_MSIX -ForceApplicationShutdown -ForceUpdateFromAnyVersion -ErrorAction Stop; exit 0 } catch { Write-Host 'In-place update failed; removing existing SimpMusic and reinstalling...'; Get-AppxPackage -Name 'Simpmusic' | Remove-AppxPackage -ErrorAction SilentlyContinue; try { Add-AppxPackage -Path $env:SIMPMUSIC_MSIX -ForceApplicationShutdown -ErrorAction Stop; exit 0 } catch { Write-Host $_; exit 1 } }"

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
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$aumid = 'Simpmusic_ejp2bhxmz1qq6!Simpmusic'; try { $pkg = Get-AppxPackage -Name 'Simpmusic' | Sort-Object Version -Descending | Select-Object -First 1; if ($pkg -and $pkg.PackageFamilyName) { $manifest = Get-AppxPackageManifest -Package $pkg.PackageFullName; $appId = $manifest.Package.Applications.Application | Select-Object -First 1 -ExpandProperty Id; if ($appId) { $aumid = ('{0}!{1}' -f $pkg.PackageFamilyName, $appId) } } } catch {}; Start-Process -FilePath 'explorer.exe' -ArgumentList ('shell:appsFolder\' + $aumid)"

echo.
echo SimpMusic installed successfully.
echo To uninstall: Settings -^> Apps -^> Installed apps -^> SimpMusic -^> Uninstall
echo.
pause
endlocal
