# Changelog

Every change to this project is recorded here.

## Unreleased
- **Adhan volume fix**: the adhan now plays on the **alarm volume** (Settings > Adhan > Play adhan on alarm volume, on by default), so it is heard even if media volume is 0 and it passes Do Not Disturb when alarms are allowed. Turn it off to use the media volume.
- **Bug fix**: built-in adhan files now get their audio attributes at creation (setting them on an already prepared player could fail and fall back to the short beep on some devices).
- The adhan notification now names the prayer ("حان وقت صلاة الفجر" / "It's time for Fajr prayer").
- **Prayer-time notification** (Settings > Adhan, on by default): when the adhan sound is switched off, a normal notification with the prayer name still appears at each prayer time. Works in the background like the adhan; turn it off to get no message either.
- Fix overlapping on phones (portrait): the clock, next-prayer card, message and prayer list now take their natural height and the screen scrolls, instead of being squeezed on top of each other.
- Readability over a photo: panels become dark frosted glass, text gets a soft shadow, the next-prayer card uses dark glass, and past prayers are less faded (70 %).
- **Prayer list background**: Settings > Appearance > Prayer list background - choose your own picture shown inside the prayer-times panel (rounded, behind the names and times), with picture dimming (0-80 %) and a remove button. Independent of the full-screen wallpaper.
- With your own wallpaper image, the glass stays clear: the next-prayer card and highlighted row use bright clear glass instead of the blue/purple tint, panels are more transparent, and the settings screen shows your image behind it instead of the purple backdrop.
- **New liquid glass look**: panels are now drawn by `GlassDrawable` - a mostly clear body, a soft light sheen across the top, an inner glow and a bright rim that catches light at the corners (instead of flat translucent boxes). The backdrop is a deep gradient with soft blue, purple and teal glows behind the glass. Applies to the home screen, settings and highlights.
- **Home screen redesigned**: top bar with city, Hijri/Gregorian dates and the settings button; large clock; a highlighted **next prayer** card with name, time, big countdown and a progress bar from the previous prayer; message card below. The prayer list uses rows with dots - past prayers are dimmed, the next prayer is highlighted. Portrait: clock and next-prayer card on top, list below.
- **Background adhan**: an exact alarm starts the adhan at each prayer (Fajr, Dhuhr, Asr, Maghrib, Isha) even when the app is closed or the screen is off; it plays through a foreground service with the Pause/Resume/Stop notification. Alarms are re-scheduled after reboot, app update, clock or time-zone change, and whenever settings change. The open screen and the alarm never play the same adhan twice. Skipped if the device was off more than 10 minutes past the prayer.
- Settings > Adhan: **Adhan when the app is closed** switch (on by default), plus shortcuts to allow exact alarms (only shown if Android blocks them) and to battery optimization settings (phones).
- **Settings redesigned** (iOS-style): side menu with coloured icons (General, Adjust times, Adhan, Appearance, Duas & messages, Language), large page title with a short description, options grouped into glass cards with thin dividers and section headers, pill-shaped choices, iOS-style switches, blue action rows (red for reset/stop), round colour swatches and a palette that opens inside the card. Every button, choice and menu item shows a blue outline when selected with a TV remote. Same settings as before.
- 12-hour clock: times show **صباحاً** (AM) / **مساءً** (PM) in smaller text after the time - prayer table and main clock; the widget's next prayer shows ص/م (AM/PM in English). Settings > General > AM / PM label: ص / م (default), full words صباحاً / مساءً, or none.
- **Adhan notification** with **Pause / Resume** and **Stop** buttons (notification bar and lock screen) while the adhan plays; swiping it away stops the adhan. Android 13+ asks for notification permission once (phones only). The adhan length limit pauses with the adhan.
- One shared adhan player for the whole app; leaving the settings screen no longer cuts off the sound (stop it with the notification, the Stop button, or OK on the remote). Settings > Adhan: new **Pause / resume** button.
- **Home-screen widget** for phones/tablets: city, next prayer and today's six times on a glass background; refreshes after each prayer and when settings change; tap to open the app. Labels follow the device language.
- **Orientation**: Settings > General > Screen orientation - **Auto** (TV detected -> landscape; phone/tablet -> follows the device), **Landscape (TV)** or **Portrait**.
- Portrait layout: clock panel on top, prayer table below; text scales to the screen size (phones get smaller text, 1080p TV unchanged). Settings in portrait show the menu as a row at the top.
- **Runs on Android 5.0 and newer** (was Android 8.0+): `minSdk` 21 with Java time desugaring; Hijri date uses the Umm al-Qura calendar on Android 7+ and an arithmetic Islamic calendar on Android 5-6 (use the Hijri adjustment if it is a day off); launcher icon for Android 5-7 added. The wallpaper blur needs Android 12+; on older versions the glass style still works without blur.
- **Liquid glass style** (on by default): frosted translucent glass panels with soft light edges for the prayer table, clock panel, message card, settings button and next-prayer highlight, over a deep blue/purple/teal gradient. The settings screen uses the same glass cards, chips and buttons. The wallpaper is blurred behind the glass on Android 12+.
- Settings > Appearance: switches for **Liquid glass style** and **Blur wallpaper**.
- New settings page **Adjust times**: move Fajr, Sunrise, Dhuhr (and Jumuah), Asr, Maghrib or Isha earlier/later by -60..+60 minutes with + / − buttons, plus reset. The adhan plays at the adjusted time.
- Settings > Appearance: **Text size** for the main screen (80 %, 90 %, 100 %, 110 %, 125 %, 150 %).
- Main screen: the verse line is now a rotating **message card (live tile)**.
- New settings page **Duas & messages**: show/hide the built-in verses and duas, time per message (10/20/30/60 s), show the after-adhan dua for 5 minutes after each adhan, and add / delete your own duas or messages.
- Settings > Adhan: **Adhan length** - 10 sec, 30 sec, 1 min, 2 min or full adhan (applies to prayer-time playback and to the preview).
- Settings > General: the long region list is now an inline drop-down (shows the selected city; click to open the list inside the page, click a city to close it).
- New app icon (adaptive launcher icon) and TV banner: gold crescent, star, white mosque dome and minaret on dark blue.
- App name follows the device language: **مواقيت الصلاة** on Arabic devices, **Prayer Times** on English / other languages.
- Settings: a fresh full-screen page (system bars hidden) with a side menu - General, Adhan, Appearance, Language - showing one category at a time. The last opened category is remembered.
- Settings language now defaults to the device language (Arabic if the device is Arabic, otherwise English) until changed in Settings > Language.
- Added a fourth adhan recording supplied by the repo owner: `adhan_4.mp3` ("الأذان 4" / "Adhan 4" in Settings).
- Settings screen: removed all drop-down lists (they opened as pop-ups). Region, adhan, language, Hijri adjustment and wallpaper are now buttons shown directly on the page; the selected one is highlighted. Only the system file picker still opens separately.
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
