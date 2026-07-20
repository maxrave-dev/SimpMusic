@echo off
REM Helper batch script to run generate_sfx_exe.ps1 and build the SimpMusic .EXE installer
setlocal EnableExtensions
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0generate_sfx_exe.ps1" %*
endlocal
