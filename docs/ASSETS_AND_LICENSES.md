# Assets and licenses

Google Play requires that you **own or are licensed to use** everything inside the app. Fill in this table before publishing.

| File | What it is | Source / reciter | License / permission | OK for Play? |
|---|---|---|---|---|
| `app/src/main/res/raw/adhan.mp3` | adhan recording | *[fill in]* | *[fill in]* | *[ ]* |
| `app/src/main/res/raw/adhan_2.mp3` | adhan recording | *[fill in]* | *[fill in]* | *[ ]* |
| `app/src/main/res/raw/adhan_3.mp3` | adhan recording | *[fill in]* | *[fill in]* | *[ ]* |
| `app/src/main/res/raw/adhan_4.mp3` | adhan recording | *[fill in]* | *[fill in]* | *[ ]* |

The recordings were supplied by the repository owner; their origin and license were **not** stated.
If you cannot show permission for a recording, do not publish it.

**Safe default:** the "Release AAB (Google Play)" workflow leaves all `adhan*.mp3` files **out** unless you tick the box.
Without them the app still works: it plays a short beep and users choose their own audio file in Settings → Adhan.

Everything else is original: the icon / banner artwork, the prayer-time calculation (an implementation of the public
praytimes.org-style astronomical formulas written for this project), the code and the texts. The prayer-time formulas are
not copyrightable; no third-party libraries are used (only Android's own libraries and the Google desugaring library
`desugar_jdk_libs`, Apache-2.0).

## Fonts
All four bundled fonts are licensed under the **SIL Open Font License 1.1**, which allows bundling them in an app (also
commercially) as long as the license text ships with them (it does, as `OFL-*.txt` next to the fonts) and the fonts are
not sold on their own. Files are in `app/src/main/assets/fonts/`.

| Font | Files | Designer / project | License text |
|---|---|---|---|
| Tajawal | `Tajawal-Regular/Bold.ttf` | Boutros International | `OFL-Tajawal.txt` |
| Almarai | `Almarai-Regular/Bold.ttf` | The Almarai Project Authors | `OFL-Almarai.txt` |
| Amiri | `Amiri-Regular/Bold.ttf` | The Amiri Project Authors | `OFL-Amiri.txt` |
| Scheherazade New | `ScheherazadeNew-Regular/Bold.ttf` | SIL Global | `OFL-ScheherazadeNew.txt` |
