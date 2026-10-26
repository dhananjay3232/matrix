@echo off
REM Go back to the Google TV launcher.
set TV=10.250.45.229:5555
adb -s %TV% shell cmd package set-home-activity com.google.android.apps.tv.launcherx/.home.HomeActivity
adb -s %TV% uninstall com.dhananjay.matrixhome
pause
