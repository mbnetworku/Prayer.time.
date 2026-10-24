# Signing the Google Play release

The test APKs on the *Releases* page are signed with a **public** key (`app/prayer-debug.jks`). That key is in this repo,
so it must **never** be used for Google Play. Play needs your own **private upload key**.

## 1. Create the upload key (once, on your computer)
You need Java installed (`keytool` comes with it). Run:

```
keytool -genkeypair -v -keystore upload-keystore.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

It asks for a password and some details. **Write the password down.**
**Back up `upload-keystore.jks` and the password** (cloud drive + USB). Do not commit the file (it is git-ignored).
With *Play App Signing* (on by default) Google keeps the real app-signing key; if you lose the upload key you can ask Google to reset it.

## 2. Turn the key into text for GitHub
- Linux / Mac: `base64 -w0 upload-keystore.jks` (Mac: `base64 -i upload-keystore.jks`)
- Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("upload-keystore.jks"))`

## 3. Add 4 secrets in GitHub
Repository → **Settings → Secrets and variables → Actions → New repository secret**:

| Secret name | Value |
|---|---|
| `KEYSTORE_BASE64` | the long base64 text from step 2 |
| `KEYSTORE_PASSWORD` | the keystore password |
| `KEY_ALIAS` | `upload` (the alias you used) |
| `KEY_PASSWORD` | the key password (same as the keystore password if you pressed Enter) |

## 4. Build the bundle
Repository → **Actions → "Release AAB (Google Play)" → Run workflow**.
- Leave **"Include the bundled adhan recordings"** **unchecked** unless you own / are licensed to use them (see `docs/ASSETS_AND_LICENSES.md`).
- When it finishes, open the run and download the artifact **PrayerTimes-play-aab** (a zip containing the `.aab`). Upload the `.aab` in Play Console.
- The artifact **PrayerTimes-release-apk-for-testing** is a signed APK for testing the same build on your device.

## Notes
- Each run gets a higher version number automatically (the workflow run number), as Play requires.
- Installing the Play version over a sideloaded test APK needs one uninstall first (different signing keys).
