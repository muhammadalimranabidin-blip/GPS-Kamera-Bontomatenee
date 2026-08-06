@echo off
setlocal
call "%~dp0AMBIL_GRADLE_WRAPPER_WINDOWS.bat"
if errorlevel 1 exit /b 1
call "%~dp0gradlew.bat" assembleDebug
if errorlevel 1 exit /b 1
copy /Y "%~dp0app\build\outputs\apk\debug\app-debug.apk" "%~dp0GPS-Kamera-Bontomatene-debug.apk"
echo.
echo APK berhasil dibuat:
echo %~dp0GPS-Kamera-Bontomatene-debug.apk
endlocal
pause
