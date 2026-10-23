package com.prayertime.tv

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.Locale
import java.util.TimeZone

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("prayer", Context.MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var adhan: AdhanPlayer

    private val blue = Color.parseColor("#8EC5FF")
    private val names = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
    private val hijriMonths = listOf("محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى",
        "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
    private val days = mapOf(1 to "الاثنين", 2 to "الثلاثاء", 3 to "الأربعاء", 4 to "الخميس",
        5 to "الجمعة", 6 to "السبت", 7 to "الأحد")
    private val verses = listOf(
        "ولسوف يعطيك ربك فترضى",
        "إِنَّ الصَّلَاةَ كَانَتْ عَلَى الْمُؤْمِنِينَ كِتَابًا مَوْقُوتًا",
        "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
        "وَأَقِيمُوا الصَّلَاةَ وَآتُوا الزَّكَاةَ",
        "قال ﷺ : من حسن إسلام المرء تركه ما لا يعنيه")

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
        handler.post(ticker)
    }

    override fun onPause() { handler.removeCallbacks(ticker); super.onPause() }
    override fun onDestroy() { adhan.stop(); super.onDestroy() }

    private fun tv(size: Float, bold: Boolean = false, c: Int = blue) = TextView(this).apply {
        setTextColor(c); textSize = size; gravity = Gravity.CENTER
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.BLACK)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(48, 24, 48, 24)
        }
        // Right side (first child in RTL): prayer table.
        val table = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
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
        root.addView(table, LinearLayout.LayoutParams(0, -1, 1f))

        // Left side: title, dates, clock, countdown, verse.
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        title = tv(34f, true); hijri = tv(26f); greg = tv(22f)
        clock = tv(80f, true).apply { textDirection = View.TEXT_DIRECTION_LTR }
        countdown = tv(26f)
        verse = tv(28f)
        val hint = tv(14f).apply { text = "اضغط OK للإعدادات - أثناء الأذان: OK لإيقاف الصوت"; alpha = 0.5f }
        for (v in listOf(title, hijri, greg, clock, countdown, verse, hint)) {
            info.addView(v, LinearLayout.LayoutParams(-2, -2).apply { topMargin = 12; bottomMargin = 12 })
        }
        root.addView(info, LinearLayout.LayoutParams(0, -1, 1.2f))
        return root
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
        today = t.asList().map { at(d, it) }
        tomorrowFajr = at(d.plusDays(1), calc(d.plusDays(1)).fajr)
        cachedDate = d; cachedRegion = prefs.getInt("region", 0)

        val friday = d.dayOfWeek.value == 5
        names2 = names.toMutableList().also { if (friday) it[2] = "الجمعة" }
        val hd = HijrahDate.from(d.plusDays(hijriAdj.toLong()))
        title.text = "مواقيت الصلاة - " + r.nameAr.substringBefore(" - ")
        hijri.text = "${days[d.dayOfWeek.value]} ${hd.get(ChronoField.DAY_OF_MONTH)} " +
            "${hijriMonths[hd.get(ChronoField.MONTH_OF_YEAR) - 1]} ${hd.get(ChronoField.YEAR)}"
        greg.text = String.format(Locale.US, "%02d/%02d/%d", d.dayOfMonth, d.monthValue, d.year)
        for (i in names.indices) {
            nameViews[i].text = names2[i]
            timeViews[i].text = hm(t.asList()[i])
        }
    }
    private var names2 = names

    private val ticker = object : Runnable {
        override fun run() {
            val now = ZonedDateTime.now(ZoneId.of(region.zone))
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
            for (i in rows.indices) rows[i].setBackgroundColor(
                if (i == next) Color.parseColor("#14305A") else Color.TRANSPARENT)

            // Fire adhan (not for sunrise) within 30s after the exact time.
            for (i in today.indices) {
                val t = today[i] ?: continue
                if (i == 1) continue
                val since = Duration.between(t, now).seconds
                val key = "${now.toLocalDate()}-$i"
                if (since in 0..30 && key != lastFired) {
                    lastFired = key
                    if (soundOn) adhan.play()
                }
            }
            if (tick++ % 30 == 0) verse.text = verses[(tick / 30) % verses.size]
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
            keyCode == KeyEvent.KEYCODE_MENU) { showSettings(); return true }
        return super.onKeyDown(keyCode, e)
    }

    private fun showSettings() {
        val items = arrayOf(
            "المنطقة: " + region.nameAr,
            "صوت الأذان: " + if (soundOn) "مفعل" else "متوقف",
            "نظام الساعة: " + if (use24) "24 ساعة" else "12 ساعة",
            "تعديل التاريخ الهجري: " + hijriAdj,
            "تجربة الصوت")
        AlertDialog.Builder(this).setTitle("الإعدادات").setItems(items) { _, w ->
            when (w) {
                0 -> pickRegion()
                1 -> prefs.edit().putBoolean("sound", !soundOn).apply()
                2 -> prefs.edit().putBoolean("h24", !use24).apply()
                3 -> prefs.edit().putInt("hijriAdj", if (hijriAdj >= 1) -1 else hijriAdj + 1).apply()
                4 -> adhan.play()
            }
            if (w in 1..3) { cachedDate = null; showSettings() }
        }.show()
    }

    private fun pickRegion() {
        AlertDialog.Builder(this).setTitle("اختر المنطقة")
            .setSingleChoiceItems(Regions.all.map { it.nameAr }.toTypedArray(),
                prefs.getInt("region", 0)) { d, w ->
                prefs.edit().putInt("region", w).apply(); d.dismiss()
            }.show()
    }
}
