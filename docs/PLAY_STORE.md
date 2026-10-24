# Publishing on Google Play — checklist

Legend: ✅ done in this repo · 👤 you do it · ⚠️ decide / check first

## 0. Before anything else
- ⚠️ **Package name.** The app id is `com.prayertime.tv` (`app/build.gradle.kts` → `applicationId`). It can **never change** after the first upload. If you want another id (for example `com.yourname.prayertimes`), tell me / change it **before** step 4.
- ⚠️ **Adhan recordings.** Fill in `docs/ASSETS_AND_LICENSES.md`. The release workflow leaves the recordings out by default.
- ✅ Minimum Android: **5.0 (API 21)** — the first version that has Android TV. Target Android: **16 (API 36)**.
- ✅ The app needs no internet permission and collects no data.

## 1. Create the developer account 👤
https://play.google.com/console → pay the one-time fee, verify your identity.
*Personal accounts created recently may need a **closed test with ~12 testers for 14 days** before going to production — check Google's current rules in the Console.*

## 2. Privacy policy 👤
- Edit `docs/PRIVACY_POLICY.md` **and** `docs/privacy.html`: replace `[DATE]`, `[YOUR NAME]`, `[YOUR CONTACT EMAIL]`.
- Host it: GitHub repo → **Settings → Pages → Branch: master, folder: /docs → Save**.
  The policy URL becomes `https://mbnetworku.github.io/Prayer.time./privacy.html` (this is the URL to give Play).

## 3. Signing key 👤
Follow `docs/RELEASE_SIGNING.md` (create the upload key, add 4 GitHub secrets).

## 4. Build the bundle ✅/👤
GitHub → Actions → **Release AAB (Google Play)** → Run workflow → download the artifact `PrayerTimes-play-aab`.

## 5. Create the app in Play Console 👤
1. **Create app** → name, default language, App, Free, accept policies.
2. **Store listing** — copy text from `docs/STORE_LISTING.md`; upload `store/icon-512.png`, `store/feature-graphic-1024x500.png`, your screenshots.
3. **Android TV**: *Release → Setup → Advanced settings → Form factors → Android TV* and add the TV banner `store/tv-banner-320x180.png` and 1920×1080 TV screenshots.
4. **App content**: Privacy policy URL, Ads = No, App access = all functionality available, Content rating questionnaire, Target audience, Data safety, and the declarations for **exact alarms** and **foreground service (media playback)** — texts are in `docs/STORE_LISTING.md`.
5. **Testing → Internal testing → Create release →** upload the `.aab` → add yourself as a tester → install from the Play link and test (adhan at prayer time with the app closed, notification buttons, widget, TV).
6. When ready: **Closed testing** (if Google requires it for your account) → **Production**.

## 6. Updates later
Push code to GitHub → run "Release AAB" again (version number goes up by itself) → upload the new `.aab` to a new release.

## What the app already does for Play ✅
- Target API 36, minimum API 21, 64-bit compatible (no native code), signed App Bundle via secrets.
- No internet / location / contacts / storage permissions; files chosen through Android's picker only.
- Notification permission requested at run time (Android 13+).
- Foreground service declared with type `mediaPlayback`; exact alarm permission declared.

## Known points to double-check on real devices before production
- Adhan starting with the app closed on your phone (set battery to *Not optimized*; Xiaomi / Huawei / Samsung can block alarms).
- Home-screen widget on your launcher.
- Android TV remote / mouse navigation.
