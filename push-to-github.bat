@echo off
title Push to GitHub
cd /d "%~dp0"

set "GIT_CMD=git"
where git >nul 2>nul
if %errorlevel% neq 0 (
    if exist "%LOCALAPPDATA%\GitHubDesktop\app-3.6.4\resources\app\git\cmd\git.exe" (
        set "GIT_CMD=%LOCALAPPDATA%\GitHubDesktop\app-3.6.4\resources\app\git\cmd\git.exe"
    )
)

"%GIT_CMD%" remote get-url origin >nul 2>nul
if %errorlevel% neq 0 (
    echo ====================================================
    echo             Push to GitHub Setup
    echo ====================================================
    echo.
    echo 1. Create a new repository on GitHub: https://github.com/new
    echo    (Do not check 'Add README' or 'Add .gitignore')
    echo.
    echo 2. Copy the repository URL (e.g. https://github.com/naveenbs8921/PlayerManagementSystem.git)
    echo.
    set /p REPO_URL="Paste your GitHub repository URL here: "
    if "%REPO_URL%"=="" (
        echo No URL entered.
        pause
        exit /b 1
    )
    "%GIT_CMD%" remote add origin %REPO_URL%
)

echo.
echo Checking for changes...
"%GIT_CMD%" add .
"%GIT_CMD%" commit -m "Update: Player Management System with Combat Arena & Web UI" >nul 2>nul

echo Pushing to GitHub (branch: main)...
"%GIT_CMD%" push -u origin main

if %errorlevel% equ 0 (
    echo.
    echo ====================================================
    echo   [SUCCESS] Code successfully pushed to GitHub!
    echo ====================================================
) else (
    echo.
    echo [NOTE] If this is your first push with GitHub, a browser window
    echo        may pop up asking you to sign in and authorize Git.
)

pause
