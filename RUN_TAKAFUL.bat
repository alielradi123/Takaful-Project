@echo off
chcp 65001 >nul
echo ==================================================
echo   تـكـافـل — تشغيل النظام (Takaful System Runner)
echo ==================================================
echo.
echo 1. Starting Admin Dashboard on port 8000...
start "Takaful Admin Server" powershell -ExecutionPolicy Bypass -NoProfile -Command "& { c:\Users\hp\AndroidStudioProjects\Takaful\web\serve.ps1 -Port 8000 }"

echo 2. Starting Donor Portal on port 8090...
start "Takaful Donor Server" powershell -ExecutionPolicy Bypass -NoProfile -Command "& { c:\Users\hp\AndroidStudioProjects\Takaful\web\donor\donor-serve.ps1 -Port 8090 }"

echo.
echo Waiting for servers to start...
timeout /t 3 /nobreak >nul

echo 3. Opening browsers...
start http://localhost:8000/index.html
start http://localhost:8090/donor/index.html

echo.
echo النظام يعمل الآن. يمكنك إغلاق هذه النافذة.
echo The system is running. You can close this window.
echo (Do not close the two blue PowerShell windows unless you want to stop the servers)
echo ==================================================
timeout /t 5 >nul
