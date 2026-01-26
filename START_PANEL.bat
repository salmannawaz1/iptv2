@echo off
echo ========================================
echo Starting IPTV Admin Panel
echo ========================================
echo.
cd /d "%~dp0client"
echo Installing dependencies (if needed)...
call npm install
echo.
echo Starting panel on http://192.168.100.78:5173
echo.
npm run dev -- --host 192.168.100.78
pause
