# Arcadia Player Management System - PowerShell Launcher
$ErrorActionPreference = "Stop"
Set-Location -Path $PSScriptRoot

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "     Arcadia Player Management System" -ForegroundColor Green
Write-Host "===================================================" -ForegroundColor Cyan

# Locate Java
$javaPath = "java"
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    $adoptium = "$env:LOCALAPPDATA\Programs\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe"
    if (Test-Path $adoptium) {
        $javaPath = $adoptium
    } else {
        Write-Error "Java runtime was not found. Please ensure JDK 17+ is installed."
        exit 1
    }
}

# Compile if needed
if (-not (Test-Path "bin\game\Main.class")) {
    Write-Host "Compiling Java sources..." -ForegroundColor Yellow
    if (-not (Test-Path "bin")) { New-Item -ItemType Directory -Path "bin" | Out-Null }
    $javacPath = "javac"
    if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
        $adoptiumJavac = "$env:LOCALAPPDATA\Programs\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\javac.exe"
        if (Test-Path $adoptiumJavac) {
            $javacPath = $adoptiumJavac
        }
    }
    $sources = (Get-ChildItem -Path src -Recurse -Filter "*.java").FullName
    & $javacPath -cp "lib/*" -d bin $sources
    Copy-Item -Path "web" -Destination "bin\web" -Recurse -Force
    Write-Host "Compilation complete." -ForegroundColor Green
}

Write-Host "`nStarting Arcadia Server at http://localhost:8080 ..." -ForegroundColor Cyan
Write-Host "Note: Ensure MySQL is started in XAMPP." -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop the server.`n"

# Open browser
Start-Process "http://localhost:8080"

# Run server
& $javaPath -cp "bin;lib/*" game.Main
