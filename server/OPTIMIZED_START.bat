@echo off
echo =========================================
echo Starting OPTIMIZED IPTV Backend
echo =========================================
echo.
echo - Added M3U caching (30 min TTL)
echo - Reduced API calls
echo - Better logging
echo.
taskkill /F /IM node.exe 2>nul
timeout /t 2 >nul
npm start
