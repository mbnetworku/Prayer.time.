package com.prayertime.tv

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.graphics.Typeface
import android.os.Bundle
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
    private var lastAdhanAt = 0L

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
    private var lastFired = ""
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
        adhan = AdhanPlayer(this)
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        if (settingsOpened) { settingsOpened = false; applyChanges() }
        handler.post(ticker)
    }

    override fun onPause() { handler.removeCallbacks(ticker); super.onPause() }
    override fun onDestroy() { adhan.stop(); super.onDestroy() }

    private fun tv(size: Float, bold: Boolean = false, c: Int = textColor) = TextView(this).apply {
        setTextColor(c); textSize = size * prefs.getInt("fontScale", 100) / 100f; gravity = Gravity.CENTER
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun buildUi(): View {
        nameViews.clear(); timeViews.clear(); rows.clear()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(48, 24, 48, 24)
        }
        // Right side (first child in RTL): prayer table.
        val table = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            if (glassOn) { background = Config.glass(density, 28); setPadding(dp(16), dp(12), dp(16), dp(12)) }
        }
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((t, w) in listOf("الصلاة" to 1.2f, "الأذان" to 1f)) {
            head.addView(tv(16f).apply { text = t; alpha = 0.7f }, LinearLayout.LayoutParams(0, -2, w))
        }
        table.addView(head)
        for (i in names.indices) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(16, 0, 16, 0)
            }
            val n = tv(34f, true).apply { text = names[i] }
            val t = tv(38f, true).apply { textDirection = View.TEXT_DIRECTION_LTR }
            row.addView(n, LinearLayout.LayoutParams(0, -2, 1.2f))
            row.addView(t, LinearLayout.LayoutParams(0, -2, 1f))
            nameViews += n; timeViews += t; rows += row
            table.addView(row, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        root.addView(table, LinearLayout.LayoutParams(0, -1, 1f).apply { setMargins(dp(8), dp(8), dp(8), dp(8)) })

        // Left side: title, dates, clock, countdown, verse.
        info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            if (glassOn) { background = Config.glass(density, 28); setPadding(dp(16), dp(12), dp(16), dp(12)) }
        }
        title = tv(34f, true); hijri = tv(26f); greg = tv(22f)
        clock = tv(80f, true).apply { textDirection = View.TEXT_DIRECTION_LTR }
        countdown = tv(26f)
        verse = tv(28f).apply {
            setPadding(dp(24), dp(14), dp(24), dp(14))
            background = if (glassOn) Config.glass(density, 20) else GradientDrawable().apply {
                cornerRadius = dp(16).toFloat(); setColor(Color.argb(50, 255, 255, 255)); setStroke(dp(1), textColor)
            }
        }
        val hint = tv(14f).apply { text = "اضغط OK للإعدادات - أثناء الأذان: OK لإيقاف الصوت"; alpha = 0.5f }
        for (v in listOf(title, hijri, greg, clock, countdown, verse, hint)) {
            info.addView(v, LinearLayout.LayoutParams(-2, -2).apply { topMargin = 12; bottomMargin = 12 })
        }
        root.addView(info, LinearLayout.LayoutParams(0, -1, 1.2f).apply { setMargins(dp(8), dp(8), dp(8), dp(8)) })
        updateTile()

        // Layers: background colour, wallpaper, dim overlay, content, settings button.
        val frame = FrameLayout(this).apply {
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            if (glassOn && !prefs.contains("bgColor")) background = Config.glassBackground() else setBackgroundColor(bgColor)
        }
        val wall = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        loadWallpaper(wall)
        // Frosted look: blur the wallpaper behind the glass panels (Android 12+).
        if (glassOn && prefs.getBoolean("blurWall", true) && Build.VERSION.SDK_INT >= 31)
            wall.setRenderEffect(RenderEffect.createBlurEffect(dp(18).toFloat(), dp(18).toFloat(), Shader.TileMode.CLAMP))
        frame.addView(wall, FrameLayout.LayoutParams(-1, -1))
        frame.addView(View(this).apply { setBackgroundColor(Color.argb(dimPercent * 255 / 100, 0, 0, 0)) },
            FrameLayout.LayoutParams(-1, -1))
        frame.addView(root, FrameLayout.LayoutParams(-1, -1))
        val gear = TextView(this).apply {
            text = "الإعدادات"; textSize = 16f; setTextColor(textColor)
            setPadding(dp(18), dp(8), dp(18), dp(8))
            background = if (glassOn) Config.glass(density, 22) else GradientDrawable().apply { cornerRadius = dp(20).toFloat(); setColor(Color.argb(110, 0, 0, 0)); setStroke(dp(1), textColor) }
            isClickable = true; isFocusable = true
            setOnClickListener { openSettings() }
        }
        frame.addView(gear, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.LEFT).apply { setMargins(dp(16), dp(12), 0, 0) })
        return frame
    }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    private fun loadWallpaper(iv: ImageView) {
        val v = prefs.getString("bgImage", "") ?: ""
        try {
            when {
                v.startsWith("preset:") -> {
                    val c = Config.presets[v.removePrefix("preset:").toInt().coerceIn(0, Config.presets.size - 1)].colors
                    iv.setImageDrawable(GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, c))
                }
                v.startsWith("uri:") -> {
                    val uri = Uri.parse(v.removePrefix("uri:"))
                    val b = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, b) }
                    var sample = 1
                    while (b.outWidth / sample > 2000) sample *= 2
                    val o = BitmapFactory.Options().apply { inSampleSize = sample }
                    contentResolver.openInputStream(uri)?.use { iv.setImageBitmap(BitmapFactory.decodeStream(it, null, o)) }
                }
            }
        } catch (_: Exception) { /* image gone or unreadable: fall back to the plain colour */ }
    }

    private fun hm(h: Double): String = if (h.isNaN()) "--:--" else {
        val tot = Math.round(h * 60).toInt().let { (it % 1440 + 1440) % 1440 }
        var hh = tot / 60; val mm = tot % 60
        if (!use24) { hh %= 12; if (hh == 0) hh = 12 }
        String.format(Locale.US, "%02d:%02d", hh, mm)
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
            timeViews[i].text = hm(hours[i])
        }
    }
    private var names2 = names

    private val ticker = object : Runnable {
        override fun run() {
            val now = ZonedDateTime.now(ZoneId.of(region.zone))
            info.visibility = if (timesOnly) View.GONE else View.VISIBLE
            if (cachedDate != now.toLocalDate() || cachedRegion != prefs.getInt("region", 0)) load(now)
            clock.text = String.format(Locale.US, "%s:%02d", hm(now.hour + now.minute / 60.0), now.second)

            var next = today.indexOfFirst { it != null && it.isAfter(now) }
            val nextTime = if (next >= 0) today[next] else tomorrowFajr
            if (next < 0) next = 0
            nextTime?.let {
                val s = Duration.between(now, it).seconds.coerceAtLeast(0)
                countdown.text = String.format(Locale.US, "المتبقي لـ%s  %d:%02d:%02d",
                    names2[next], s / 3600, s % 3600 / 60, s % 60)
            }
            for (i in rows.indices) rows[i].background = when {
                i != next -> null
                glassOn -> Config.glass(density, 16, highlightColor)
                else -> GradientDrawable().apply { setColor(highlightColor); cornerRadius = dp(12).toFloat() }
            }

            // Fire adhan (not for sunrise) within 30s after the exact time.
            for (i in today.indices) {
                val t = today[i] ?: continue
                if (i == 1 || i == 3) continue
                val since = Duration.between(t, now).seconds
                val key = "${now.toLocalDate()}-$i"
                if (since in 0..30 && key != lastFired) {
                    lastFired = key
                    lastAdhanAt = System.currentTimeMillis()
                    if (soundOn) playAzan()
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
        if (prefs.getBoolean("tileAfterAdhan", true) && System.currentTimeMillis() - lastAdhanAt < 5 * 60_000) {
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
    }

    private var settingsOpened = false

    private fun openSettings() {
        settingsOpened = true
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}
