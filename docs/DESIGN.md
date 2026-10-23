# Design notes

- `PrayerCalc.kt` - offline calculation (praytimes.org algorithm). Methods are defined by Fajr angle and Isha angle or minutes after Maghrib.
- `Regions.kt` - list of cities: Arabic name, coordinates, time zone, method. Add a city by adding one line.
- `AdhanPlayer.kt` - plays `res/raw/adhan`, falls back to a chime.
- `MainActivity.kt` - UI built in code (RTL layout), 1-second ticker, adhan trigger (within 30 s after the prayer time, once per prayer), settings dialogs. Preferences are stored in SharedPreferences `prayer`.
- The Iqama column was removed from the table (v1.1).
- Digits are always formatted with `Locale.US` so they stay 0-9.
