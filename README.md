# Prayer Time TV (Android TV)

Works on **Android 5.0 and newer** (TV boxes, TVs and phones), fully offline.

Landscape prayer-times display: Arabic text, English (0-9) digits, adhan sound, region picker, offline calculation.

- Press **OK / Menu** on the remote to open settings (region, adhan on/off, 12/24h, Hijri adjust, test sound).
- While the adhan plays, press **OK** (or Back/Stop) to stop the sound.
- **Settings (one screen, mouse friendly, Arabic/English):** click the **الإعدادات** button (top-left). Colours, wallpaper (gradient or your own image), dimming, adhan choice (built-in or a file from the device), region, clock format, times-only mode.
- **Several adhans:** add `adhan_2.mp3`, `adhan_3.mp3`, ... next to `adhan.mp3`; pick one in Settings > اختيار الأذان.
- **Times only:** Settings > العرض switches to a table-only screen.
- **Adhan audio:** put your own file at `app/src/main/res/raw/adhan.mp3` (or .ogg/.wav). Without it a simple chime plays.
- **Download the APK:** Releases page of this repo (newest build, built automatically on every push to master). A copy of Build 11 is also in the repo root: `PrayerTimeTV.apk`.
- Install and build details: [docs/INSTALL.md](docs/INSTALL.md). Code overview: [docs/DESIGN.md](docs/DESIGN.md). History: [CHANGELOG.md](CHANGELOG.md).
