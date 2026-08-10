@rem ============================================================================
@rem Gradle wrapper bootstrap script for Inventory Hover Highlight plugin
@rem ============================================================================
@echo off
setlocal enabledelayedexpansion

set DIR=%~dp0
set GRADLE_DIR=%DIR%.gradle\gradle-8.5
set GRADLE_ZIP=%DIR%.gradle\gradle-8.5-bin.zip
set GRADLE_BAT=%GRADLE_DIR%\gradle-8.5\bin\gradle.bat

if exist "%GRADLE_BAT%" goto RUN_GRADLE

if not exist "%DIR%.gradle" mkdir "%DIR%.gradle"

if exist "%GRADLE_ZIP%" del /f /q "%GRADLE_ZIP%"
if exist "%GRADLE_DIR%" rmdir /s /q "%GRADLE_DIR%"

echo Downloading Gradle 8.5...
powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.5-bin.zip' -OutFile '%GRADLE_ZIP%'"

echo Extracting Gradle 8.5...
powershell -Command "Expand-Archive -Path '%GRADLE_ZIP%' -DestinationPath '%GRADLE_DIR%' -Force"

if exist "%GRADLE_ZIP%" del /f /q "%GRADLE_ZIP%"

:RUN_GRADLE
if exist "%GRADLE_BAT%" (
    call "%GRADLE_BAT%" %*
) else (
    echo Error: Failed to setup Gradle executable.
    exit /b 1
)
