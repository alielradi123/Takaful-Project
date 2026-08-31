@echo off
setlocal EnableDelayedExpansion
color 0A
echo.
echo ============================================================
echo   GRADLE CACHE FIX - SOLUTION FINALE
echo ============================================================
echo.

echo [1/5] Stopping Gradle daemons...
call "%~dp0gradlew.bat" --stop >nul 2>&1
echo     Done.

echo.
echo [2/5] Killing ALL Java / Android Studio processes...
taskkill /F /IM java.exe /T >nul 2>&1
taskkill /F /IM javaw.exe /T >nul 2>&1
taskkill /F /IM gradle.exe /T >nul 2>&1
taskkill /F /IM studio64.exe /T >nul 2>&1
ping 127.0.0.1 -n 4 >nul
echo     Done.

echo.
echo [3/5] Cleaning corrupted transforms cache...
set GRADLE_CACHE=%USERPROFILE%\.gradle\caches

if exist "%GRADLE_CACHE%\8.13\transforms" (
    echo     Removing transforms folder...
    rd /s /q "%GRADLE_CACHE%\8.13\transforms" >nul 2>&1
    if exist "%GRADLE_CACHE%\8.13\transforms" (
        echo     Using robocopy trick...
        mkdir "%GRADLE_CACHE%\__empty__" >nul 2>&1
        robocopy "%GRADLE_CACHE%\__empty__" "%GRADLE_CACHE%\8.13\transforms" /MIR /NFL /NDL /NJH /NJS >nul 2>&1
        rd /s /q "%GRADLE_CACHE%\8.13\transforms" >nul 2>&1
        rd /s /q "%GRADLE_CACHE%\__empty__" >nul 2>&1
    )
    echo     Transforms cache cleaned!
) else (
    echo     Already clean.
)

echo.
echo [4/5] Cleaning project local caches...
if exist "%~dp0.gradle" (
    rd /s /q "%~dp0.gradle" >nul 2>&1
    echo     Project .gradle deleted.
)
if exist "%~dp0app\build" (
    rd /s /q "%~dp0app\build" >nul 2>&1
    echo     app/build deleted.
)
if exist "%~dp0build" (
    rd /s /q "%~dp0build" >nul 2>&1
    echo     build deleted.
)

echo.
echo [5/5] Cleaning ALL Gradle daemon data...
if exist "%USERPROFILE%\.gradle\daemon" (
    rd /s /q "%USERPROFILE%\.gradle\daemon" >nul 2>&1
    echo     Gradle daemon data cleaned.
)

echo.
echo ============================================================
echo   CLEANUP COMPLETE!
echo ============================================================
echo.
echo   Next steps:
echo   1. Open Android Studio
echo   2. Click: File ^> Sync Project with Gradle Files
echo   3. Wait for the build to complete
echo.
echo ============================================================
pause
