# Changelog

Every change to this project is recorded here.

## Unreleased
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
