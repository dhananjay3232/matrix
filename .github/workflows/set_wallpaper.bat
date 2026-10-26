@echo off
REM Usage: set_wallpaper.bat photo.jpg
set TV=10.250.45.229:5555
if "%~1"=="" (echo Usage: set_wallpaper.bat photo.jpg & exit /b 1)
adb -s %TV% shell mkdir -p /sdcard/Android/data/com.dhananjay.matrixhome/files/wallpapers
adb -s %TV% push "%~1" /sdcard/Android/data/com.dhananjay.matrixhome/files/wallpapers/
echo Now open Matrix Home settings ^> Wallpaper and press OK until your photo shows.
pause
