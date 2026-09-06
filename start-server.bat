@echo off
title Arcadia - Player Management System
cd /d "%~dp0"

echo ===================================================
echo     Arcadia Player Management System
echo ===================================================
echo.

:: Detect Java
set "JAVA_CMD=java"
where java >nul 2>nul
if %errorlevel% neq 0 (
    if exist "C:\Users\%USERNAME%\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe" (
        set "JAVA_CMD=C:\Users\%USERNAME%\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe"
    ) else (
        echo [ERROR] Java was not found in PATH or standard Adoptium path.
        echo Please make sure JDK is installed.
        pause
        exit /b 1
    )
)

:: Check bin folder, compile if missing
if not exist "bin\game\Main.class" (
    echo Compiling project...
    if not exist "bin" mkdir bin
    set "JAVAC_CMD=javac"
    where javac >nul 2>nul
    if %errorlevel% neq 0 (
        if exist "C:\Users\%USERNAME%\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\javac.exe" (
            set "JAVAC_CMD=C:\Users\%USERNAME%\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\javac.exe"
        )
    )
    "%JAVAC_CMD%" -cp "lib/*" -d bin src\game\*.java src\game\dao\*.java src\game\db\*.java src\game\manager\*.java src\game\model\*.java src\game\server\*.java
    if %errorlevel% neq 0 (
        echo [ERROR] Compilation failed.
        pause
        exit /b 1
    )
    xcopy /s /e /y /i web bin\web >nul 2>nul
    echo Compilation complete.
    echo.
)

echo Starting Backend & Frontend Server at http://localhost:8080 ...
echo (Make sure MySQL is running in XAMPP!)
echo.
echo Press Ctrl+C in this window to stop the server.
echo ===================================================
echo.

:: Automatically open browser after 2 seconds in background
start /b cmd /c "timeout /t 2 /nobreak >nul & start http://localhost:8080"

:: Start the Java HTTP Server
"%JAVA_CMD%" -cp "bin;lib/*" game.Main

pause
