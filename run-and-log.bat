@echo off
echo ========================================
echo Building and Installing App...
echo ========================================

:: Build the debug APK
call gradlew.bat assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    pause
    exit /b 1
)

:: Install the APK
echo.
echo Installing APK...
adb install -r app\build\outputs\apk\debug\app-debug.apk

if %ERRORLEVEL% NEQ 0 (
    echo Installation failed!
    pause
    exit /b 1
)

:: Launch the app
echo.
echo Launching app...
adb shell am start -n com.trustonic.overlaynewdevice/com.d.locker.lock.activities.MainActivity

:: Clear logcat and show real-time logs
echo.
echo ========================================
echo Showing Real-Time Logs...
echo ========================================
echo Press Ctrl+C to stop viewing logs
echo.

adb logcat -c
adb logcat -s MainAppActivity:* *:E

pause
