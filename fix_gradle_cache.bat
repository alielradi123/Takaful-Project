@echo off
echo ========================================
echo   Fixing Gradle Transforms Cache
echo ========================================
echo.

echo [1/3] Stopping all Gradle daemons...
call gradlew.bat --stop 2>nul
echo Done.
echo.

echo [2/3] Deleting corrupted transforms cache...
if exist "%USERPROFILE%\.gradle\caches\8.13\transforms" (
    rmdir /s /q "%USERPROFILE%\.gradle\caches\8.13\transforms"
    echo Transforms cache deleted successfully.
) else (
    echo Transforms cache not found (already clean).
)
echo.

echo [3/3] Running clean build...
call gradlew.bat --no-build-cache clean assembleDebug
echo.

if %ERRORLEVEL% EQU 0 (
    echo ========================================
    echo   BUILD SUCCESSFUL!
    echo ========================================
) else (
    echo ========================================
    echo   Build failed. Check errors above.
    echo ========================================
)
echo.
pause
