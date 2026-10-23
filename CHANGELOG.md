# Changelog

Every change to this project is recorded here.

## Unreleased
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
