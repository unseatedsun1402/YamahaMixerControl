@echo off
setlocal

set "PORT=18080"
set "PREVIEW_URL=http://127.0.0.1:%PORT%/MidiControl/main.html"

curl.exe --fail --silent --show-error --output NUL --connect-timeout 2 "%PREVIEW_URL%"
if not errorlevel 1 (
    echo Mixer preview is already running: %PREVIEW_URL%
    exit /b 0
)

set "PORT_PID="
for /f "tokens=5" %%P in ('netstat -ano -p tcp ^| findstr /R /C:":%PORT% .*LISTENING"') do set "PORT_PID=%%P"
if defined PORT_PID (
    echo Port %PORT% is occupied by process ID %PORT_PID%, but the mixer preview did not respond at %PREVIEW_URL%.
    exit /b 1
)

if defined JAVA_TOOL_OPTIONS (
    set "JAVA_TOOL_OPTIONS=%JAVA_TOOL_OPTIONS% -Djava.awt.headless=true"
) else (
    set "JAVA_TOOL_OPTIONS=-Djava.awt.headless=true"
)

pushd "%~dp0.."
echo Starting mixer preview on port %PORT%.
echo Open %PREVIEW_URL% after Spring Boot reports that it has started.
call gradlew.bat bootRun --args="--server.port=%PORT%"
set "EXIT_CODE=%ERRORLEVEL%"
popd

exit /b %EXIT_CODE%
