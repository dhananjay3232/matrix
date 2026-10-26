@echo off
REM Installs Matrix Home on the Haier TV and makes it the home screen.
REM Usage: install.bat path\to\app-debug.apk
set TV=10.250.45.229:5555
set APK=%~1
if "%APK%"=="" set APK=app-debug.apk
adb connect %TV%
adb -s %TV% install -r "%APK%"
adb -s %TV% shell cmd package set-home-activity com.dhananjay.matrixhome/.MainActivity
adb -s %TV% shell cmd notification allow_listener com.dhananjay.matrixhome/com.dhananjay.matrixhome.NotifService
REM Real screensaver: drifting clock
adb -s %TV% shell settings put secure screensaver_components com.dhananjay.matrixhome/.ClockDream
adb -s %TV% shell settings put secure screensaver_enabled 1
adb -s %TV% shell input keyevent KEYCODE_HOME
echo Done. Press Home on the remote.
pause
