@echo off
setlocal
set "TARGET=%~dp0gradle\wrapper\gradle-wrapper.jar"
if exist "%TARGET%" (
  echo Gradle Wrapper sudah tersedia.
  exit /b 0
)
echo Mengunduh Gradle Wrapper resmi...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$u='https://github.com/gradle/gradle/raw/refs/tags/v8.11.1/gradle/wrapper/gradle-wrapper.jar'; Invoke-WebRequest -UseBasicParsing -Uri $u -OutFile '%TARGET%'"
if errorlevel 1 (
  echo Gagal mengunduh Gradle Wrapper.
  exit /b 1
)
echo Selesai.
endlocal
