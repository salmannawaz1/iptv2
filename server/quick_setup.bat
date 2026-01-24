@echo off
echo Killing old node processes...
taskkill /F /IM node.exe 2>nul

echo.
echo Creating sallu1 user with M3U URL...
node create_sallu1.js

echo.
echo Starting backend with logging...
npm start
