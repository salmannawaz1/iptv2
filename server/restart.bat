@echo off
echo Stopping backend...
taskkill /F /IM node.exe 2>nul
timeout /t 2 >nul

echo Creating sallu1 user...
node create_sallu1.js

echo.
echo Starting backend...
start "IPTV Backend" cmd /k "npm start"

echo.
echo Backend restarting...
timeout /t 5 >nul
