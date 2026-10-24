package com.prayertime.tv

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import java.util.TimeZone

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("prayer", Context.MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var adhan: AdhanPlayer

    private val textColor get() = prefs.getInt("textColor", Config.defText)
    private val highlightColor get() = prefs.getInt("hlColor", Config.defHighlight)
    private val bgColor get() = prefs.getInt("bgColor", Color.BLACK)
    private val glassOn get() = prefs.getBoolean("glass", true)
    private val density get() = resources.displayMetrics.density
    private val portrait get() = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    /** Scales text to the screen: 1.0 on a 1080p TV, smaller on phones (portrait or landscape). */
    private val screenScale: Float get() {
        val w = resources.configuration.screenWidthDp.toFloat()
        val h = resources.configuration.screenHeightDp.toFloat()
        val s = if (w >= h) minOf(w / 960f, h / 540f) else minOf(w / 540f, h / 960f)
        return s.coerceIn(0.5f, 1.6f)
    }
    private val dimPercent get() = prefs.getInt("dim", 0)
    private val names = listOf("الفجر", "الشروق", "الظهر", "الجمعة", "العصر", "المغرب", "العشاء")
    private val hijriMonths = listOf("محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى",
        "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
    private val days = mapOf(1 to "الاثنين", 2 to "الثلاثاء", 3 to "الأربعاء", 4 to "الخميس",
        5 to "الجمعة", 6 to "السبت", 7 to "الأحد")
    private val verses = listOf(
        "ولسوف يعطيك ربك فترضى",
        "إِنَّ الصَّلَاةَ كَانَتْ عَلَى الْمُؤْمِنِينَ كِتَابًا مَوْقُوتًا",
        "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
        "وَأَقِيمُوا الصَّلَاةَ وَآتُوا الزَّكَاةَ",
        "قال ﷺ : من حسن إسلام المرء تركه ما لا يعنيه",
        "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
        "اللهم إنك عفو تحب العفو فاعف عني",
        "سبحان الله وبحمده، سبحان الله العظيم")
    private val afterAdhanDua = "اللهم رب هذه الدعوة التامة والصلاة القائمة، آت محمداً الوسيلة والفضيلة، " +
        "وابعثه مقاماً محموداً الذي وعدته"

    private lateinit var info: LinearLayout
    private lateinit var title: TextView
    private lateinit var hijri: TextView
    private lateinit var greg: TextView
    private lateinit var clock: TextView
    private lateinit var countdown: TextView
    private lateinit var verse: TextView
    private val nameViews = ArrayList<TextView>()
    private val timeViews = ArrayList<TextView>()
    private val rows = ArrayList<LinearLayout>()

    private var cachedDate: LocalDate? = null
    private var cachedRegion = -1
    private var today: List<ZonedDateTime?> = emptyList()
    private var tomorrowFajr: ZonedDateTime? = null
    private var tick = 0

    private val region get() = Regions.all[prefs.getInt("region", 0).coerceIn(0, Regions.all.size - 1)]
    private val use24 get() = prefs.getBoolean("h24", false)
    private val soundOn get() = prefs.getBoolean("sound", true)
    private val timesOnly get() = prefs.getBoolean("timesOnly", false)
    private val azan get() = prefs.getString("azan", "adhan") ?: "adhan"
    private val azanUri get() = prefs.getString("azanUri", "") ?: ""
    private val hijriAdj get() = prefs.getInt("hijriAdj", 0)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        adhan = AdhanPlayer.get(this)
        // Phones (Android 13+): allow the adhan notification with Pause / Stop buttons.
        if (Build.VERSION.SDK_INT >= 33 && !Config.isTv(this) &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 5)
        Config.applyOrientation(this)
        setContentView(buildUi())
        AdhanScheduler.schedule(this)
    }

    override fun onResume() {
        super.onResume()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        if (settingsOpened) { settingsOpened = false; Config.applyOrientation(this); applyChanges() }
        handler.post(ticker)
    }

    override fun onPause() { handler.removeCallbacks(ticker); super.onPause() }

    private fun tv(size: Float, bold: Boolean = false, c: Int = textColor) = TextView(this).apply {
        setTextColor(c); textSize = size * prefs.getInt("fontScale", 100) / 100f * screenScale; gravity = Gravity.CENTER
        if (bold) setTypeface(typeface, Typeface.BOLD)
        // Soft shadow keeps text readable over a photo.
        if (Config.hasImage(this@MainActivity) || prefs.contains("listImage")) setShadowLayer(dp(4).toFloat(), 0f, dp(1).toFloat(), Color.argb(200, 0, 0, 0))
    }

    private lateinit var nextName: TextView
    private lateinit var nextTimeView: TextView
    private lateinit var progFill: View
    private lateinit var progRest: View

    // Over a photo the glass is dark-frosted so text stays readable.
    private fun card(radius: Int = 26) = if (glassOn) Config.glass(density, radius, dark = Config.hasImage(this))
        else GradientDrawable().apply { setColor(Color.argb(60, 255, 255, 255)); cornerRadius = dp(radius).toFloat() }

    /** Home screen: top bar (city, dates, settings) + hero (clock, next-prayer card, message) + prayer list. */
    private fun buildUi(): View {
        nameViews.clear(); timeViews.clear(); rows.clear()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(if (portrait) 28 else 14), dp(20), dp(14))
        }

        // ---- top bar
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        title = tv(26f, true).apply { gravity = Gravity.START }
        val dates = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            background = card(20); setPadding(dp(18), dp(6), dp(18), dp(6))
        }
        hijri = tv(17f, true); greg = tv(14f).apply { alpha = 0.75f }
        dates.addView(hijri); dates.addView(greg)
        val gear = tv(15f, true).apply {
            text = "الإعدادات"; setPadding(dp(18), dp(10), dp(18), dp(10)); background = card(22)
            isClickable = true; isFocusable = true; setOnClickListener { openSettings() }
        }
        bar.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        if (!portrait) bar.addView(dates, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(12) })
        bar.addView(gear)
        root.addView(bar, LinearLayout.LayoutParams(-1, -2))
        if (portrait) root.addView(dates, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })

        val main = LinearLayout(this).apply { orientation = if (portrait) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL }
        // Portrait (phone): everything takes its natural height and the page scrolls - nothing is squeezed.
        if (portrait) root.addView(android.widget.ScrollView(this).apply { isVerticalScrollBarEnabled = false; addView(main) },
            LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(12) })
        else root.addView(main, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(12) })

        // ---- prayer list
        val table = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = card(); setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        for (i in names.indices) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(18), dp(if (portrait) 12 else 0), dp(18), dp(if (portrait) 12 else 0))
            }
            val dot = View(this).apply {
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(textColor) }
            }
            val n = tv(30f, true).apply { text = names[i]; gravity = Gravity.START or Gravity.CENTER_VERTICAL }
            val t = tv(32f, true).apply { textDirection = View.TEXT_DIRECTION_RTL; gravity = Gravity.END or Gravity.CENTER_VERTICAL }
            row.addView(dot, LinearLayout.LayoutParams(dp(10), dp(10)).apply { marginEnd = dp(14) })
            row.addView(n, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(t, LinearLayout.LayoutParams(-2, -2))
            nameViews += n; timeViews += t; rows += row
            table.addView(row, (if (portrait) LinearLayout.LayoutParams(-1, -2) else LinearLayout.LayoutParams(-1, 0, 1f)).apply { setMargins(0, dp(3), 0, dp(3)) })
        }

        // ---- hero: clock, next-prayer card, rotating message
        info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        clock = tv(if (portrait) 64f else 92f, true).apply { textDirection = View.TEXT_DIRECTION_RTL }
        val nextCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            background = if (glassOn) (if (Config.hasImage(this@MainActivity)) Config.glass(density, 24, dark = true) else Config.glass(density, 24, highlightColor))
                else GradientDrawable().apply { setColor(highlightColor); cornerRadius = dp(24).toFloat() }
            setPadding(dp(24), dp(14), dp(24), dp(16))
        }
        val nextLine = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        nextName = tv(26f, true); nextTimeView = tv(26f).apply { alpha = 0.85f; textDirection = View.TEXT_DIRECTION_RTL }
        nextLine.addView(tv(18f).apply { text = "الصلاة القادمة  "; alpha = 0.8f })
        nextLine.addView(nextName); nextLine.addView(tv(26f).apply { text = "  •  "; alpha = 0.5f }); nextLine.addView(nextTimeView)
        countdown = tv(40f, true).apply { textDirection = View.TEXT_DIRECTION_LTR }
        val track = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply { setColor(Color.argb(50, 255, 255, 255)); cornerRadius = dp(4).toFloat() }
        }
        progFill = View(this).apply { background = GradientDrawable().apply { setColor(textColor); cornerRadius = dp(4).toFloat() } }
        progRest = View(this)
        track.addView(progFill, LinearLayout.LayoutParams(0, -1, 0f)); track.addView(progRest, LinearLayout.LayoutParams(0, -1, 1f))
        nextCard.addView(nextLine)
        nextCard.addView(countdown, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(4) })
        nextCard.addView(track, LinearLayout.LayoutParams(-1, dp(8)).apply { topMargin = dp(10) })
        verse = tv(24f).apply {
            setPadding(dp(24), dp(14), dp(24), dp(14)); background = card(20)
        }
        info.addView(clock)
        info.addView(nextCard, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10); setMargins(dp(16), dp(10), dp(16), 0) })
        info.addView(verse, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(16), dp(16), dp(16), 0) })

        // Optional picture behind the prayer list (Settings > Appearance > Prayer list background).
        val listBox = FrameLayout(this)
        val listImage = prefs.getString("listImage", "") ?: ""
        if (listImage.isNotEmpty()) {
            fun rounded(c: Int) = GradientDrawable().apply { setColor(c); cornerRadius = dp(26).toFloat() }
            listBox.addView(ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP; background = rounded(Color.BLACK); clipToOutline = true
                Config.loadImage(this@MainActivity, listImage, this)
            }, FrameLayout.LayoutParams(-1, -1))
            listBox.addView(View(this).apply { background = rounded(Color.argb(prefs.getInt("listDim", 40) * 255 / 100, 0, 0, 0)) },
                FrameLayout.LayoutParams(-1, -1))
        }
        listBox.addView(table, FrameLayout.LayoutParams(-1, -1))
        fun lp(w: Float) = (if (portrait) LinearLayout.LayoutParams(-1, 0, w) else LinearLayout.LayoutParams(0, -1, w))
        if (portrait) {
            main.addView(info, LinearLayout.LayoutParams(-1, -2))
            main.addView(listBox, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
        } else {
            main.addView(listBox, lp(0.9f).apply { marginEnd = dp(16) })
            main.addView(info, lp(1.2f))
        }
        updateTile()

        // Layers: background colour, wallpaper, dim overlay, content.
        val frame = FrameLayout(this).apply {
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            if (glassOn && !prefs.contains("bgColor")) background = Config.glassBackground() else setBackgroundColor(bgColor)
        }
        val wall = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        Config.loadWallpaper(this, wall)
        // Frosted look: blur the wallpaper behind the glass panels (Android 12+).
        if (glassOn && prefs.getBoolean("blurWall", true) && Build.VERSION.SDK_INT >= 31)
            wall.setRenderEffect(RenderEffect.createBlurEffect(dp(18).toFloat(), dp(18).toFloat(), Shader.TileMode.CLAMP))
        frame.addView(wall, FrameLayout.LayoutParams(-1, -1))
        frame.addView(View(this).apply { setBackgroundColor(Color.argb(dimPercent * 255 / 100, 0, 0, 0)) },
            FrameLayout.LayoutParams(-1, -1))
        frame.addView(root, FrameLayout.LayoutParams(-1, -1))
        return frame
    }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()


    private fun hm(h: Double): String = if (h.isNaN()) "--:--" else {
        val tot = Math.round(h * 60).toInt().let { (it % 1440 + 1440) % 1440 }
        var hh = tot / 60; val mm = tot % 60
        if (!use24) { hh %= 12; if (hh == 0) hh = 12 }
        String.format(Locale.US, "%02d:%02d", hh, mm)
    }

    /** صباحاً / مساءً (or ص / م) after a time in 12-hour mode; pref "ampm" = full / short / none. */
    private fun period(h: Double): String {
        if (use24 || h.isNaN()) return ""
        val am = ((Math.round(h * 60).toInt() % 1440 + 1440) % 1440) < 720
        return when (prefs.getString("ampm", "short")) {
            "none" -> ""
            "short" -> if (am) "ص" else "م"
            else -> if (am) "صباحاً" else "مساءً"
        }
    }

    /** Time with the period word in smaller text, e.g. "04:24 صباحاً". */
    private fun withPeriod(time: String, h: Double, ratio: Float): CharSequence {
        val p = period(h)
        if (p.isEmpty()) return time
        val sb = SpannableStringBuilder(time).append(" ")
        val start = sb.length
        sb.append(p)
        sb.setSpan(RelativeSizeSpan(ratio), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return sb
    }

    private fun load(now: ZonedDateTime) {
        val r = region
        val zone = ZoneId.of(r.zone)
        val d = now.toLocalDate()
        fun at(date: LocalDate, h: Double): ZonedDateTime? =
            if (h.isNaN()) null else date.atStartOfDay(zone).plusSeconds((h * 3600).toLong())
        fun calc(date: LocalDate): DayTimes {
            val off = TimeZone.getTimeZone(r.zone).getOffset(
                date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()) / 3600000.0
            return PrayerCalc.compute(date, r.lat, r.lng, off, r.method)
        }
        val t = calc(d)
        val friday = d.dayOfWeek.value == 5
        // Index 3 is the Jumuah row: same time as Dhuhr, shown on Fridays only.
        // Manual corrections from Settings > Adjust times (minutes per prayer, pref adj_0..adj_5).
        val hours = t.asList().mapIndexed { i, h -> h + prefs.getInt("adj_$i", 0) / 60.0 }
            .toMutableList().also { it.add(3, it[2]) }
        today = hours.mapIndexed { i, h -> if (i == 3 && !friday) null else at(d, h) }
        tomorrowFajr = at(d.plusDays(1), calc(d.plusDays(1)).fajr + prefs.getInt("adj_0", 0) / 60.0)
        cachedDate = d; cachedRegion = prefs.getInt("region", 0)

        names2 = names
        val hd = Hijri.of(d.plusDays(hijriAdj.toLong()))
        title.text = "مواقيت الصلاة - " + r.nameAr.substringBefore(" - ")
        hijri.text = "${days[d.dayOfWeek.value]} ${hd[2]} ${hijriMonths[(hd[1] - 1).coerceIn(0, 11)]} ${hd[0]}"
        greg.text = String.format(Locale.US, "%02d/%02d/%d", d.dayOfMonth, d.monthValue, d.year)
        rows[3].visibility = if (friday) View.VISIBLE else View.GONE
        for (i in names.indices) {
            nameViews[i].text = names2[i]
            timeViews[i].text = withPeriod(hm(hours[i]), hours[i], 0.45f)
        }
    }
    private var names2 = names

    private val ticker = object : Runnable {
        override fun run() {
            val now = ZonedDateTime.now(ZoneId.of(region.zone))
            info.visibility = if (timesOnly) View.GONE else View.VISIBLE
            if (cachedDate != now.toLocalDate() || cachedRegion != prefs.getInt("region", 0)) load(now)
            val nowH = now.hour + now.minute / 60.0
            clock.text = withPeriod(String.format(Locale.US, "%s:%02d", hm(nowH), now.second), nowH, 0.35f)

            var next = today.indexOfFirst { it != null && it.isAfter(now) }
            val nextTime = if (next >= 0) today[next] else tomorrowFajr
            if (next < 0) next = 0
            nextTime?.let {
                val s = Duration.between(now, it).seconds.coerceAtLeast(0)
                countdown.text = String.format(Locale.US, "%d:%02d:%02d", s / 3600, s % 3600 / 60, s % 60)
                nextName.text = names2[next]
                nextTimeView.text = timeViews.getOrNull(next)?.text ?: ""
                // Progress from the previous prayer to the next one.
                val prev = today.filterNotNull().lastOrNull { !it.isAfter(now) } ?: now.toLocalDate().atStartOfDay(now.zone)
                val total = Duration.between(prev, it).seconds.coerceAtLeast(1)
                val done = (1f - s.toFloat() / total).coerceIn(0f, 1f)
                (progFill.layoutParams as LinearLayout.LayoutParams).weight = done
                (progRest.layoutParams as LinearLayout.LayoutParams).weight = 1f - done
                progFill.requestLayout()
            }
            for (i in rows.indices) {
                val passed = today.getOrNull(i)?.let { !it.isAfter(now) } ?: false
                rows[i].background = when {
                    i != next -> null
                    glassOn -> if (Config.hasImage(this@MainActivity)) Config.glass(density, 18, null, true) else Config.glass(density, 18, highlightColor)
                    else -> GradientDrawable().apply { setColor(highlightColor); cornerRadius = dp(18).toFloat() }
                }
                rows[i].alpha = if (passed && i != next) 0.7f else 1f
            }

            // Fire adhan (not for sunrise) within 30s after the exact time.
            for (i in today.indices) {
                val t = today[i] ?: continue
                if (i == 1 || i == 3) continue
                val since = Duration.between(t, now).seconds
                // Same key as the background alarm (index without the Jumuah row) so it plays once.
                val key = "${now.toLocalDate()}-${if (i > 3) i - 1 else i}"
                if (since in 0..30 && Times.claim(this@MainActivity, key)) {
                    if (soundOn) playAzan()
                    else if (prefs.getBoolean("prayerNote", true)) AdhanNotification.remind(this@MainActivity)
                }
            }
            tick++
            updateTile()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onKeyDown(keyCode: Int, e: KeyEvent): Boolean {
        // While the adhan plays, OK / Back / Stop / Play-Pause only stop the sound.
        if (adhan.isPlaying && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_BACK ||
                keyCode == KeyEvent.KEYCODE_MEDIA_STOP || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)) {
            adhan.stop(); return true
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER ||
            keyCode == KeyEvent.KEYCODE_MENU) { openSettings(); return true }
        return super.onKeyDown(keyCode, e)
    }

    private fun playAzan() = adhan.playSelected()

    /** Rotating message card ("live tile"): after-adhan dua for 5 minutes, otherwise built-in + user messages. */
    private fun updateTile() {
        if (prefs.getBoolean("tileAfterAdhan", true) && System.currentTimeMillis() - prefs.getLong("lastAdhanAt", 0) < 5 * 60_000) {
            verse.visibility = View.VISIBLE; verse.text = afterAdhanDua; return
        }
        val list = (if (prefs.getBoolean("tilesBuiltin", true)) verses else emptyList()) + Config.customTiles(prefs)
        if (list.isEmpty()) { verse.visibility = View.GONE; return }
        verse.visibility = View.VISIBLE
        val every = prefs.getInt("tileSeconds", 30).coerceAtLeast(5)
        verse.text = list[(tick / every) % list.size]
    }

    /** Re-creates the screen so changes made in settings show immediately. */
    private fun applyChanges() {
        setContentView(buildUi())
        cachedDate = null
        load(ZonedDateTime.now(ZoneId.of(region.zone)))
        PrayerWidget.updateAll(this)
        AdhanScheduler.schedule(this)
    }

    private var settingsOpened = false

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyChanges()
    }

    private fun openSettings() {
        settingsOpened = true
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}
