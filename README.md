# Prayer Time TV (Android TV)

Landscape prayer-times display: Arabic text, English (0-9) digits, adhan sound, region picker, offline calculation.

- Press **OK / Menu** on the remote to open settings (region, adhan on/off, 12/24h, Hijri adjust, test sound).
- **Adhan audio:** put your own file at `app/src/main/res/raw/adhan.mp3` (or .ogg/.wav). Without it a simple chime plays.
- **Build:** push to GitHub; Actions > "Build APK" produces the debug APK artifact. Or open in Android Studio and Run.
- Install on TV: `adb connect <tv-ip>` then `adb install app-debug.apk`, or copy the APK with a USB drive.
