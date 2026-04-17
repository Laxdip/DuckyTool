@echo off
:: ============================================================
:: DuckyTool - Run Script (Windows)
:: ============================================================

setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "SRC_DIR=%SCRIPT_DIR%src"
set "BIN_DIR=%SCRIPT_DIR%build"

:: ── Check Java ───────────────────────────────────────────────
where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] java.exe not found. Please install Java 11 or later and add it to PATH.
    pause
    exit /b 1
)

:: ── Compile ───────────────────────────────────────────────────
echo Compiling DuckyTool...
if not exist "%BIN_DIR%" mkdir "%BIN_DIR%"

javac -source 11 -target 11 -d "%BIN_DIR%" ^
    "%SRC_DIR%\KeyMapper.java" ^
    "%SRC_DIR%\FileHandler.java" ^
    "%SRC_DIR%\Encoder.java" ^
    "%SRC_DIR%\Decoder.java" ^
    "%SRC_DIR%\Main.java"

if errorlevel 1 (
    echo [ERROR] Compilation failed.
    pause
    exit /b 1
)

echo Compilation successful.
echo.

:: ── Run ───────────────────────────────────────────────────────
if "%~1"=="" (
    :: Interactive mode
    java -cp "%BIN_DIR%" Main
) else if "%~3" NEQ "" (
    :: CLI mode
    java -cp "%BIN_DIR%" Main "%~1" "%~2" "%~3"
) else (
    echo Usage:
    echo   run.bat                                            (interactive menu)
    echo   run.bat encode samples\input.txt samples\output.bin
    echo   run.bat decode samples\output.bin samples\decoded.txt
    pause
    exit /b 1
)
