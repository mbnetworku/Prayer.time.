# Changelog

Every change to this project is recorded here.

## Unreleased
- Added two more adhan recordings supplied by the repo owner: `adhan_2.mp3` and `adhan_3.mp3`. They appear in Settings > Choose adhan as "الأذان 2" / "Adhan 2" and "الأذان 3" / "Adhan 3".
- Settings are now one dedicated full screen (`SettingsActivity`) instead of stacked pop-up lists. Opened by the **الإعدادات** button (or OK/Menu); everything is on one scrollable page and works with mouse, touch or remote.
- Settings language switch: Arabic / English (region names are translated too). The prayer-times screen stays Arabic with 0-9 digits.
- A copy of the latest APK (Build 11) is kept in the repo root as `PrayerTimeTV.apk`. The Releases page always has the newest build.
- Mouse/touch friendly: a visible **الإعدادات** button (top-left) opens the settings; all lists and pickers work with a mouse.
- Customisation: text colour, highlight colour (next prayer), background colour (colour palette or any #RRGGBB), wallpaper (5 built-in gradients or an image from the device), background dimming, reset to defaults.
- Adhan: besides the built-in `adhan*.mp3` files you can pick any audio file from the device.
- versionCode/versionName now follow the GitHub build number, so each new build installs as an update over the previous one (same signing key since Build 8).
- Builds are now signed with one fixed key (`app/prayer-debug.jks`) so a new APK installs over the previous one. Uninstall the old app once before installing this build.
- docs/INSTALL.md: how to handle the Google Play Protect warning.
- The Jumuah (الجمعة) row is now a separate row after Dhuhr and is shown on Fridays only; Dhuhr always shows.
- Settings: new **display mode** (full / times only) hides the dates, clock and countdown so only the prayer table shows.
- Settings: new **adhan choice**. Every `res/raw/adhan.*` and `adhan_2`, `adhan_3`, ... file is listed as "الأذان 1, 2, ..."; picking one previews it.
- Removed the Iqama (الإقامة) column from the prayer table.
- While the adhan is playing, OK / Back / Stop / Play-Pause on the remote stops the sound.
- Added the adhan audio as `app/src/main/res/raw/adhan.mp3` (supplied by the repo owner); the app now plays it instead of the fallback chime.
- CI: each successful build on `master` is published as a GitHub Release with `PrayerTimeTV.apk` attached.
- Docs: added `docs/INSTALL.md` and `docs/DESIGN.md`.

## 1.0 - 2026-10-23
- First version of the Android TV app (landscape, Arabic text, 0-9 digits).
- Offline prayer-time calculation (port of praytimes.org) for 34 regions.
- Adhan sound at each prayer time (custom `res/raw/adhan.*` or fallback chime).
- Settings via OK/Menu: region, sound on/off, 12/24h, Hijri day adjust, test sound.
- GitHub Actions workflow that builds the debug APK.
