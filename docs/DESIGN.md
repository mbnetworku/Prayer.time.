# Design notes

- `PrayerCalc.kt` - offline calculation (praytimes.org algorithm). Methods are defined by Fajr angle and Isha angle or minutes after Maghrib.
- `Regions.kt` - list of cities: Arabic name, coordinates, time zone, method. Add a city by adding one line.
- `AdhanPlayer.kt` - plays `res/raw/adhan`, falls back to a chime.
- `MainActivity.kt` - UI built in code (RTL layout), 1-second ticker, adhan trigger (within 30 s after the prayer time, once per prayer), settings dialogs. Preferences are stored in SharedPreferences `prayer`.
- The Iqama column was removed from the table (v1.1).
- Digits are always formatted with `Locale.US` so they stay 0-9.
- Display mode `timesOnly` (pref) hides the left info panel. Adhan choice (pref `azan`) is the name of a raw resource: `adhan` or `adhan_*`, found by reflection on `R.raw`.
- Theme prefs: `textColor`, `hlColor`, `bgColor`, `dim` (0-85 %), `bgImage` (`preset:N` or `uri:<content uri>`), `azan` (`adhan*` or `uri`) + `azanUri`. The UI is a FrameLayout (colour, wallpaper, dim, content, settings button) and is rebuilt by `applyChanges()` after any change. If the TV has no file-picker app, the picker shows a message; use the built-in gradients and adhans then.
- `SettingsActivity.kt` is the only settings UI (built in code, language pref `lang` = `ar`/`en`, strings via `t(ar, en)`). `Config.kt` holds shared palette, wallpaper presets and the list of adhan resources. MainActivity rebuilds itself in `onResume` after returning from settings.
- App name: `res/values/strings.xml` (English, default) and `res/values-ar/strings.xml` (Arabic). Icon: `mipmap-anydpi-v26/ic_launcher*.xml` (adaptive: `drawable/ic_launcher_background.xml` + `ic_launcher_foreground.xml`); TV banner: `drawable/banner.xml`.
- SettingsActivity pages: `pageGeneral`, `pageAdhan`, `pageAppearance`, `pageLanguage`; selected tab saved in pref `settingsTab`.
- Live tile: `MainActivity.updateTile()` - prefs `tilesBuiltin`, `tileAfterAdhan`, `tileSeconds`, `tiles` (JSON array via `Config.customTiles/saveTiles`). Text size: pref `fontScale` (percent) applied in `MainActivity.tv()`.
- Manual time corrections: prefs `adj_0`..`adj_5` (minutes; Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha) added in `MainActivity.load()`; Jumuah copies the adjusted Dhuhr.
