# SimpMusic Windows .EXE Installer Generator
#
# Generates a standalone, double-clickable SimpMusic-Setup-<version>-windows-x64.exe
# installer from the build output (`output/simpmusic-*.msix` and `simpmusic.crt`).
#
# If Inno Setup (`ISCC.exe`) is installed, it compiles `SimpMusic.iss` into an .EXE setup.
# Otherwise, it uses Windows built-in `IExpress.exe` (available natively on all Windows OSes)
# to create a self-extracting .EXE setup installer that requests UAC elevation and executes `install.bat`.

param (
    [string]$Version = "1.6.1",
    [string]$OutputFolder = "$PSScriptRoot\..\..\installers"
)

$ErrorActionPreference = "Stop"
$ScriptFolder = $PSScriptRoot
$ProjectRoot = [System.IO.Path]::GetFullPath("$ScriptFolder\..\..")
$OutDir = [System.IO.Path]::GetFullPath("$OutputFolder")

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

Write-Host "=== Building SimpMusic v$Version .EXE Installer ==="

# Check for Inno Setup compiler (ISCC.exe)
$iscc = Get-Command "ISCC.exe" -ErrorAction SilentlyContinue
if (-not $iscc) {
    $defaultIsccPath = "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe"
    if (Test-Path $defaultIsccPath) {
        $iscc = $defaultIsccPath
    }
}

if ($iscc) {
    Write-Host "[1/2] Inno Setup detected at: $iscc. Compiling SimpMusic.iss..."
    & $iscc /DAppVersion=$Version "$ScriptFolder\SimpMusic.iss"
    Write-Host "[2/2] Inno Setup .EXE build complete: $OutDir\SimpMusic-Setup-$Version-windows-x64.exe"
    exit 0
}

# Fallback to Windows built-in IExpress self-extracting EXE setup
Write-Host "[1/2] Inno Setup not found. Using Windows built-in IExpress to generate self-extracting .EXE installer..."

$MsixFile = Get-ChildItem -Path "$ProjectRoot\output\simpmusic-*.msix" | Select-Object -First 1
$CrtFile = "$ProjectRoot\output\simpmusic.crt"
$InstallBat = "$ScriptFolder\install.bat"

if (-not $MsixFile -or -not (Test-Path $CrtFile)) {
    Write-Host "[WARNING] MSIX package or certificate not found in output folder. Ensure `conveyor make windows-msix` has run."
}

$SedFile = "$OutDir\SimpMusic-Setup.sed"
$ExeFile = "$OutDir\SimpMusic-Setup-$Version-windows-x64.exe"

$SedContent = @"
[Version]
Class=IEXPRESS
SEDVersion=3
[Options]
PackagePurpose=InstallApp
ShowInstallProgramWindow=1
HideExtractAnimation=0
UseLongFileName=1
InsideCompressed=0
CAB_FixedSize=0
CAB_ResvCodeSigning=0
RebootMode=N
InstallPrompt=%InstallPrompt%
DisplayLicense=%DisplayLicense%
FinishMessage=%FinishMessage%
TargetName=%TargetName%
FriendlyName=%FriendlyName%
AppLaunched=%AppLaunched%
PostInstallCmd=%PostInstallCmd%
AdminQuietInstCmd=%AdminQuietInstCmd%
UserQuietInstCmd=%UserQuietInstCmd%
SourceFiles=SourceFiles

[Strings]
InstallPrompt=Do you want to install SimpMusic v$Version?
DisplayLicense=
FinishMessage=SimpMusic installation started! Follow the command prompt window to completion.
TargetName=$ExeFile
FriendlyName=SimpMusic Setup v$Version
AppLaunched=cmd.exe /c install.bat
PostInstallCmd=<None>
AdminQuietInstCmd=
UserQuietInstCmd=
FILE0="install.bat"
FILE1="simpmusic.crt"
FILE2="$($MsixFile.Name)"

[SourceFiles]
SourceFiles0=$ScriptFolder\
SourceFiles1=$ProjectRoot\output\

[SourceFiles0]
%FILE0%=

[SourceFiles1]
%FILE1%=
%FILE2%=
"@

Set-Content -Path $SedFile -Value $SedContent -Encoding Ascii
Write-Host "Running IExpress to package .EXE..."
Start-Process -FilePath "iexpress.exe" -ArgumentList "/N `"$SedFile`"" -Wait -NoNewWindow
Remove-Item -Path $SedFile -Force -ErrorAction SilentlyContinue

if (Test-Path $ExeFile) {
    Write-Host "[2/2] .EXE installer built successfully: $ExeFile"
} else {
    Write-Error "Failed to generate .EXE installer with IExpress."
}
