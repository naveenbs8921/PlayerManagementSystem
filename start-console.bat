@echo off
title Arcadia - Console Mode
cd /d "%~dp0"

set "JAVA_CMD=java"
where java >nul 2>nul
if %errorlevel% neq 0 (
    if exist "C:\Users\%USERNAME%\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe" (
        set "JAVA_CMD=C:\Users\%USERNAME%\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe"
    )
)

set APP_MODE=console
"%JAVA_CMD%" -cp "bin;lib/*" game.Main

pause
