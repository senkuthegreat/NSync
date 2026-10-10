@echo off
title NSync Emulator
echo ==============================================
echo   Launching NSync on Android Emulator
echo ==============================================
echo.
echo Starting Pixel_Test emulator...
start "" "C:\Users\rohit\AppData\Local\Android\Sdk\emulator\emulator.exe" -avd Pixel_Test
echo.
echo Waiting for emulator to connect...
timeout /t 5 >nul
echo.
echo Emulator window is now running!
