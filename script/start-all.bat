@echo off
setlocal EnableExtensions
chcp 65001 >nul

set "ROOT=D:\zhaoysg\Projects\java\aaa"
set "FRONTEND_DIR=%ROOT%\ruoyi-vben-ui"
set "BACKEND_JAR=%ROOT%\ruoyi-admin\target\ruoyi-admin.jar"
set "BACKEND_URL=http://127.0.0.1:8080"
set "FRONTEND_URL=http://127.0.0.1:5666"

echo ================================================
echo Build and start metadata stack
echo ================================================
echo.

echo [1/4] Stop existing backend process...
for /f "tokens=1,*" %%i in ('jps -lv 2^>nul') do (
    echo %%j | findstr /I /C:"ruoyi-admin\target\ruoyi-admin.jar" >nul
    if not errorlevel 1 (
        taskkill /F /PID %%i /T >nul 2>&1
    )
)

echo [2/4] Stop existing frontend process on port 5666...
for /f "tokens=5" %%i in ('netstat -ano ^| findstr /R /C:":5666 .*LISTENING"') do (
    taskkill /F /PID %%i /T >nul 2>&1
)

echo [3/4] Build backend with Maven...
call mvn -f "%ROOT%\pom.xml" clean package -DskipTests -T 4 -pl ruoyi-modules/ruoyi-metadata,ruoyi-admin -am
if errorlevel 1 (
    echo.
    echo [ERROR] Maven build failed.
    pause
    exit /b 1
)

echo [4/4] Start backend and frontend...
start "RuoYi-Backend" cmd /k "cd /d %ROOT% && java -jar %BACKEND_JAR% --spring.profiles.active=dev"
start "RuoYi-Frontend" cmd /k "cd /d %FRONTEND_DIR% && pnpm dev:antd"

echo.
echo ================================================
echo Backend : %BACKEND_URL%
echo Frontend: %FRONTEND_URL%
echo ================================================
pause
