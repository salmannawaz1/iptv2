@echo off
echo ========================================
echo Starting IPTV Backend Server
echo ========================================
echo.
cd /d "%~dp0server"
echo Starting on http://192.168.100.78:5000
echo.
npm start
pause
