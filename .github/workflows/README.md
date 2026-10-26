# Matrix Home - custom Android TV launcher

Kotlin + Jetpack Compose. Built for the Haier MatrixTV BE (Android 14, ~1.8 GB RAM).

## Features
- Apple TV style rows on a gradient background; auto dark/light theme (light 7am-6pm)
- Clock, date, weather (Open-Meteo, no key; city picked in settings)
- Rows: Favorites, Recent, Inputs (Live TV, HDMI 1-4, AV), All apps (A-Z)
- Haier / MediaTek / system apps hidden by default (toggle in settings)
- Long-press OK on an app: add/remove favorite, move, hide, app info, uninstall
- Remote: D-pad, voice/search key, number keys 1-9 = Nth favorite,
  Red = launcher settings, Green = quick settings, Yellow = Live TV, Blue = notifications, mouse/air-mouse clicks
- Quick settings (Wi-Fi, Bluetooth, display, sound), notification list, idle clock screen
- Weather for ANY city: Settings > Weather city > type a name > Search (popular Indian cities listed too)
- Real system screensaver ("Matrix Clock", black screen, slowly drifting clock). install.bat turns it on;
  change the timeout in TV Settings > Device preferences > Screen saver
- Wallpaper picker: Settings > Wallpaper cycles Auto / Ocean / Sunset / Forest / Graphite gradients
  and any photos you add (see below)
- No ads, no sponsored rows, no video: light on RAM

## Build the APK (pick one)
**A. GitHub (no installs):** create a free GitHub repo, upload this whole folder, open the
Actions tab, run "Build Matrix Home APK", download `MatrixHome-apk` -> `app-debug.apk`.

**B. Android Studio:** File > Open this folder, let it sync, Build > Build APK(s).
APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Install on the TV
Run `install.bat app-debug.apk` in CMD (uses `adb -s 10.250.45.229:5555`, needed because you have two ADB devices).
It installs, sets Matrix Home as home, and grants notification access.
If the TV ignores set-home-activity: press Home and choose Matrix Home > "Always".

## Add your own wallpaper photos
Run `set_wallpaper.bat photo.jpg` (JPG/PNG/WebP, 1920x1080 or larger is best), then in the launcher open
Matrix Home settings > Wallpaper and press OK until your photo shows.

## Undo
`uninstall_restore.bat` restores the Google TV launcher and removes Matrix Home.

## Notes
- Screensaver: if the TV's own screen-saver menu doesn't list Matrix Clock, pick it there manually.
- Not compiled yet: expect to fix a small error or two on first build.
- Live TV / HDMI tiles use the TV's own input list; the Live TV tile opens the MediaTek tvcenter app.
