; SimpMusic Inno Setup Script (Conveyor / jpackage / standalone Windows .exe installer)
; Recommended by desktopApp/build.gradle.kts to provide a seamless double-click .exe installer
; with automatic UAC elevation, certificate trust for signed MSIX packages, and desktop/start menu shortcuts.

[Setup]
AppName=SimpMusic
AppVersion=1.6.1
AppPublisher=maxrave-dev
AppPublisherURL=https://simpmusic.org
AppSupportURL=https://github.com/maxrave-dev/SimpMusic/issues
AppUpdatesURL=https://github.com/maxrave-dev/SimpMusic/releases
DefaultDirName={autopf}\SimpMusic
DefaultGroupName=SimpMusic
AllowNoIcons=yes
LicenseFile=..\..\LICENSE
OutputDir=..\..\installers
OutputBaseFilename=SimpMusic-Setup-1.6.1-windows-x64
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
SetupIconFile=..\..\composeApp\icon\circle_app_icon.ico
PrivilegesRequired=admin
ArchitecturesAllowed=x64 compatible
ArchitecturesInstallIn64BitMode=x64 compatible

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
; Support installing either from standalone app binaries (jpackage/conveyor windows-app) OR wrapping the offline package
Source: "..\..\output\simpmusic-*.msix"; DestDir: "{tmp}"; Flags: ignoreversion optional
Source: "..\..\output\simpmusic.crt"; DestDir: "{tmp}"; Flags: ignoreversion optional
Source: "install.bat"; DestDir: "{tmp}"; Flags: ignoreversion optional

[Registry]
; Register simpmusic:// custom URL protocol scheme
Root: HKCR; Subkey: "simpmusic"; ValueType: string; ValueName: ""; ValueData: "URL:SimpMusic Protocol"; Flags: uninsdeletekey
Root: HKCR; Subkey: "simpmusic"; ValueType: string; ValueName: "URL Protocol"; ValueData: ""
Root: HKCR; Subkey: "simpmusic\shell\open\command"; ValueType: string; ValueName: ""; ValueData: """{app}\SimpMusic.exe"" ""%1"""

[Icons]
Name: "{group}\SimpMusic"; Filename: "{app}\SimpMusic.exe"
Name: "{autodesktop}\SimpMusic"; Filename: "{app}\SimpMusic.exe"; Tasks: desktopicon

[Run]
; If we packaged MSIX+CRT in {tmp}, run the silent certificate import and installation
Filename: "certutil.exe"; Parameters: "-addstore -f ""TrustedPeople"" ""{tmp}\simpmusic.crt"""; StatusMsg: "Trusting SimpMusic certificate..."; Flags: runhidden; Check: FileExists(ExpandConstant('{tmp}\simpmusic.crt'))
Filename: "powershell.exe"; Parameters: "-NoProfile -ExecutionPolicy Bypass -Command ""try { Add-AppxPackage -Path '{tmp}\simpmusic-*.msix' -ForceApplicationShutdown -ForceUpdateFromAnyVersion -ErrorAction Stop } catch { Get-AppxPackage -Name 'Simpmusic' | Remove-AppxPackage -ErrorAction SilentlyContinue; Add-AppxPackage -Path '{tmp}\simpmusic-*.msix' -ForceApplicationShutdown }"""; StatusMsg: "Installing SimpMusic Windows package..."; Flags: runhidden; Check: FileExists(ExpandConstant('{tmp}\simpmusic-*.msix'))
Filename: "{app}\SimpMusic.exe"; Description: "{cm:LaunchProgram,SimpMusic}"; Flags: nowait postinstall skipifsilent; Check: FileExists(ExpandConstant('{app}\SimpMusic.exe'))
