# Install on Android TV

1. Open the repo's **Releases** page and download `PrayerTimeTV.apk` from the latest build.
2. Copy it to the TV:
   - USB drive: copy the file, open it with a file manager app on the TV, allow "install unknown apps".
   - or ADB: `adb connect <tv-ip>:5555` then `adb install -r PrayerTimeTV.apk`.
3. Open **مواقيت الصلاة** from the TV launcher.
4. Press **OK** on the remote -> choose the region.

## Custom adhan sound
Add your file as `app/src/main/res/raw/adhan.mp3` (lowercase name, .mp3/.ogg/.wav), commit and push; a new APK is built automatically. Without it, a short chime plays.

## Build locally
Open the folder in Android Studio and press Run, or run `gradle assembleDebug` (JDK 17 + Android SDK).
