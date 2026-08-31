@echo off
chcp 65001 > nul
set "SRC=C:\Users\hp\.gemini\antigravity-ide\brain\481191d9-426e-40e9-963f-be69394c523e\media__1782456352953.jpg"
set "DST1=c:\Users\hp\AndroidStudioProjects\Takaful\web\donor\bok-qr.jpg"
set "DST2=c:\Users\hp\AndroidStudioProjects\Takaful\web\bok-qr.jpg"
set "DST3=c:\Users\hp\AndroidStudioProjects\Takaful\app\src\main\res\drawable\bok_qr.jpg"

echo جاري نسخ رمز QR الحقيقي لبنك الخرطوم...
copy /Y "%SRC%" "%DST1%"
copy /Y "%SRC%" "%DST2%"
copy /Y "%SRC%" "%DST3%"
echo.
echo تم النسخ بنجاح! يرجى إغلاق هذه النافذة.
pause > nul
