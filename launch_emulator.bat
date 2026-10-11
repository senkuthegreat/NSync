@echo off
title NSync Emulator
echo ==============================================
echo   Launching NSync on Android Emulator
echo ==============================================
echo.
echo Starting Pixel_Test emulator...
start "" "%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe" -avd Pixel_Test -gpu host
echo.
echo Waiting for emulator to connect...
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" wait-for-device
echo.
echo Emulator connected! Unlocking and launching NSync...
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" shell input keyevent 82
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" install -r "docs\downloads\NSync.apk"
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" shell am start -n com.example.nsutconnect/.MainActivity
echo.
echo ==============================================
echo   NSync is now running on your emulator!
echo ==============================================
echo.
pause
