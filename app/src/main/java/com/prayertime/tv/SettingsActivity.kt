package com.prayertime.tv

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/**
 * Settings: one full screen in an iOS-like style - side menu with coloured icons, large page title,
 * options grouped in (glass) cards with dividers. Works with mouse, touch and remote (focused items
 * get a blue outline). Language: Arabic / English.
 */
class SettingsActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("prayer", Context.MODE_PRIVATE) }
    private lateinit var adhan: AdhanPlayer
    private val lang get() = prefs.getString("lang", null)
        ?: if (java.util.Locale.getDefault().language == "ar") "ar" else "en"
    private val isAr get() = lang == "ar"
    private fun t(ar: String, en: String) = if (isAr) ar else en

    private val pageBg = Color.parseColor("#0E1116")
    private val cardBg = Color.parseColor("#1C1F26")
    private val blue = Color.parseColor("#0A84FF")
    private val red = Color.parseColor("#FF453A")
    private val secondary = Color.argb(165, 255, 255, 255)
    private val reqImage = 21
    private val reqListImage = 23
    private val reqAudio = 22
    private lateinit var azanBox: LinearLayout
    private var card: LinearLayout? = null // the group card new items are added to

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        adhan = AdhanPlayer.get(this)
        Config.applyOrientation(this)
        setContentView(withWallpaper(build()))
    }

    override fun onPause() {
        super.onPause()
        AdhanScheduler.schedule(this) // region / adjustments may have changed
    }

    override fun onResume() {
        super.onResume()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    /** Puts the user's wallpaper (image or gradient) behind the settings, slightly darkened. */
    private fun withWallpaper(page: View): View {
        if (prefs.getString("bgImage", "").isNullOrEmpty()) return page
        val frame = android.widget.FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        val wall = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        Config.loadWallpaper(this, wall)
        frame.addView(wall, android.widget.FrameLayout.LayoutParams(-1, -1))
        frame.addView(View(this).apply { setBackgroundColor(Color.argb(90, 0, 0, 0)) }, android.widget.FrameLayout.LayoutParams(-1, -1))
        frame.addView(page, android.widget.FrameLayout.LayoutParams(-1, -1))
        return frame
    }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun edit(f: SharedPreferences.Editor.() -> Unit) { prefs.edit().apply(f).apply() }
    private fun lp(w: Int, h: Int, weight: Float = 0f) = LinearLayout.LayoutParams(w, h, weight)

    private val glassOn get() = prefs.getBoolean("glass", true)
    private val density get() = resources.displayMetrics.density
    private val portrait get() = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    // ---- drawing helpers -------------------------------------------------------------------

    private fun rounded(color: Int, radiusDp: Int, stroke: Int = 0) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radiusDp).toFloat()
        if (stroke != 0) setStroke(dp(1).coerceAtLeast(1), stroke)
    }

    private fun cardBackground() = if (glassOn) Config.glass(density, 18) else rounded(cardBg, 18)

    /** Background with a clear blue outline when the item has remote focus or is pressed. */
    private fun focusable(normal: Drawable?, radiusDp: Int): Drawable = StateListDrawable().apply {
        val hi = GradientDrawable().apply {
            setColor(Color.argb(70, 10, 132, 255)); cornerRadius = dp(radiusDp).toFloat(); setStroke(dp(2), blue)
        }
        addState(intArrayOf(android.R.attr.state_pressed), hi)
        addState(intArrayOf(android.R.attr.state_focused), hi)
        addState(intArrayOf(), normal ?: ColorDrawable(Color.TRANSPARENT))
    }

    private fun pill(selected: Boolean) = focusable(
        if (selected) rounded(blue, 20) else rounded(Color.argb(30, 255, 255, 255), 20, Color.argb(40, 255, 255, 255)), 20)

    private fun text(s: String, size: Float, bold: Boolean = false, c: Int = Color.WHITE) = TextView(this).apply {
        text = s; textSize = size; setTextColor(c)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun tappable(v: View, radiusDp: Int, normal: Drawable?, onClick: () -> Unit) {
        v.background = focusable(normal, radiusDp)
        v.isClickable = true; v.isFocusable = true
        v.setOnClickListener { onClick() }
    }

    /** Small rounded button (− / + / Delete / Apply ...). */
    private fun smallButton(label: String, color: Int = Color.argb(55, 255, 255, 255), onClick: () -> Unit) =
        text(label, 17f, true).apply {
            gravity = Gravity.CENTER; minWidth = dp(44); setPadding(dp(14), dp(8), dp(14), dp(8))
            tappable(this, 20, rounded(color, 20), onClick)
        }

    private fun input(hintText: String) = EditText(this).apply {
        hint = hintText; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
        background = rounded(Color.argb(30, 255, 255, 255), 10, Color.argb(50, 255, 255, 255))
        setPadding(dp(12), dp(10), dp(12), dp(10))
    }

    // ---- page structure: large title + grouped cards ---------------------------------------

    private fun pageTitle(body: LinearLayout, title: String, subtitle: String) {
        body.addView(text(title, 30f, true), lp(-1, -2).apply { topMargin = dp(8) })
        body.addView(text(subtitle, 14f, c = secondary), lp(-1, -2).apply { topMargin = dp(2) })
    }

    /** Starts a new card; the items added after this go inside it. */
    private fun group(body: LinearLayout, header: String? = null) {
        if (header != null) body.addView(text(header, 13f, true, secondary).apply { setPadding(dp(16), 0, dp(16), 0) },
            lp(-1, -2).apply { topMargin = dp(22) })
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = cardBackground(); setPadding(0, dp(4), 0, dp(4))
        }
        body.addView(c, lp(-1, -2).apply { topMargin = dp(if (header != null) 6 else 22) })
        card = c
    }

    /** Adds a view to the current card, with a thin divider between items. */
    private fun item(body: LinearLayout, v: View) {
        val c = card ?: run { group(body); card!! }
        if (c.childCount > 0) c.addView(View(this).apply { setBackgroundColor(Color.argb(38, 255, 255, 255)) },
            lp(-1, 1).apply { marginStart = dp(16) })
        c.addView(v, lp(-1, -2))
    }

    private fun labelColumn(label: String, sub: String?) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(text(label, 17f))
        if (sub != null) addView(text(sub, 13f, c = secondary))
    }

    private fun row(body: LinearLayout, label: String, control: View?, sub: String? = null) {
        val r = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(52); setPadding(dp(16), dp(10), dp(16), dp(10))
        }
        r.addView(labelColumn(label, sub), lp(0, -2, 1f))
        if (control != null) r.addView(control)
        item(body, r)
    }

    /** Full-width tappable action row (blue text, red for destructive actions). */
    private fun action(body: LinearLayout, label: String, color: Int = blue, onClick: () -> Unit) {
        val v = text(label, 17f, false, color).apply { setPadding(dp(16), dp(14), dp(16), dp(14)) }
        tappable(v, 12, null, onClick)
        item(body, v)
    }

    private fun switchOf(key: String, def: Boolean, onChange: ((Boolean) -> Unit)? = null) = Switch(this).apply {
        isChecked = prefs.getBoolean(key, def)
        if (Build.VERSION.SDK_INT >= 23) {
            val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
            thumbTintList = ColorStateList(states, intArrayOf(Color.WHITE, Color.parseColor("#D1D1D6")))
            trackTintList = ColorStateList(states, intArrayOf(Color.parseColor("#34C759"), Color.parseColor("#636366")))
        }
        setOnCheckedChangeListener { _, c -> edit { putBoolean(key, c) }; onChange?.invoke(c) }
    }

    /** Pill choices (no drop-down / pop-up). The selected one is filled blue. */
    private fun fillChips(box: LinearLayout, items: List<String>, selected: () -> Int, perRow: Int, onPick: (Int) -> Unit) {
        box.removeAllViews()
        val views = ArrayList<TextView>()
        fun paint() {
            val sel = selected()
            views.forEachIndexed { i, v -> v.background = pill(i == sel) }
        }
        items.chunked(perRow).forEachIndexed { r, chunk ->
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            chunk.forEachIndexed { k, label ->
                val idx = r * perRow + k
                val v = text(label, 15f, true).apply {
                    gravity = Gravity.CENTER; maxLines = 2; setPadding(dp(8), dp(10), dp(8), dp(10))
                    isClickable = true; isFocusable = true
                    setOnClickListener { onPick(idx); paint() }
                }
                views += v
                line.addView(v, lp(0, -2, 1f).apply { setMargins(dp(4), dp(4), dp(4), dp(4)) })
            }
            repeat(perRow - chunk.size) { line.addView(View(this), lp(0, 1, 1f)) }
            box.addView(line, lp(-1, -2))
        }
        paint()
    }

    /** Label + pill choices inside the current card. Returns the box so it can be refilled. */
    private fun choices(body: LinearLayout, label: String, items: List<String>, perRow: Int,
                        selected: () -> Int, sub: String? = null, onPick: (Int) -> Unit): LinearLayout {
        val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(12)) }
        c.addView(labelColumn(label, sub))
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        c.addView(box, lp(-1, -2).apply { topMargin = dp(8) })
        fillChips(box, items, selected, perRow, onPick)
        item(body, c)
        return box
    }

    /** Row showing the current value; tapping opens the list inside the card (no pop-up). */
    private fun dropdown(body: LinearLayout, label: String, items: List<String>,
                         selected: () -> Int, onPick: (Int) -> Unit) {
        val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(52); setPadding(dp(16), dp(10), dp(16), dp(10))
        }
        head.addView(text(label, 17f), lp(0, -2, 1f))
        val value = text("", 16f, c = secondary)
        val arrow = text("  ▼", 12f, c = secondary)
        head.addView(value); head.addView(arrow)
        fun refresh() { value.text = items[selected().coerceIn(0, items.size - 1)] }
        refresh()
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; visibility = View.GONE; setPadding(dp(8), 0, dp(8), dp(8))
        }
        val views = ArrayList<TextView>()
        fun paint() {
            val sel = selected()
            views.forEachIndexed { i, v -> v.background = focusable(if (i == sel) rounded(blue, 12) else null, 12) }
        }
        items.forEachIndexed { i, s ->
            val v = text(s, 16f).apply {
                setPadding(dp(16), dp(10), dp(16), dp(10)); isClickable = true; isFocusable = true
                setOnClickListener { onPick(i); refresh(); list.visibility = View.GONE; arrow.text = "  ▼" }
            }
            views += v
            list.addView(v, lp(-1, -2))
        }
        tappable(head, 12, null) {
            paint()
            val open = list.visibility == View.GONE
            list.visibility = if (open) View.VISIBLE else View.GONE
            arrow.text = if (open) "  ▲" else "  ▼"
        }
        c.addView(head, lp(-1, -2)); c.addView(list, lp(-1, -2))
        item(body, c)
    }

    // ---- adhan choices -------------------------------------------------------------------

    private fun azanItems(): List<Pair<String, String>> {
        val l = Config.azanOptions().mapIndexed { i, n -> Pair(t("الأذان ${i + 1}", "Adhan ${i + 1}"), n) }.toMutableList()
        if (!prefs.getString("azanUri", "").isNullOrEmpty()) l += Pair(t("ملف من الجهاز", "File from device"), "uri")
        return l
    }

    private fun currentAzan() = prefs.getString("azan", "adhan") ?: "adhan"
    private fun azanSelected() = azanItems().indexOfFirst { it.second == currentAzan() }.coerceAtLeast(0)
    private fun azanPicked(pos: Int) {
        val v = azanItems().getOrNull(pos)?.second
        if (v != null) { edit { putString("azan", v) }; adhan.playSelected() }
    }

    private fun fillAzan() = fillChips(azanBox, azanItems().map { it.first }, ::azanSelected, if (portrait) 2 else 3, ::azanPicked)

    // ---- the screen ------------------------------------------------------------------------

    private var tab: Int
        get() = prefs.getInt("settingsTab", 0)
        set(v) { edit { putInt("settingsTab", v) } }

    private class Tab(val title: String, val sub: String, val icon: Int, val color: Int, val page: (LinearLayout) -> Unit)

    private fun build(): View {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = when {
                prefs.getString("bgImage", "").isNullOrEmpty().not() -> null // the wallpaper shows through
                glassOn -> Config.glassBackground()
                else -> ColorDrawable(pageBg)
            }
            layoutDirection = if (isAr) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        }

        // Top bar: back button + "Settings".
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(6))
        }
        val back = text((if (isAr) "›  " else "‹  ") + t("رجوع", "Back"), 17f, true).apply {
            setPadding(dp(16), dp(8), dp(18), dp(8))
        }
        tappable(back, 20, rounded(Color.argb(40, 255, 255, 255), 20, Color.argb(50, 255, 255, 255))) { finish() }
        top.addView(back)
        top.addView(text(t("الإعدادات", "Settings"), 20f, true).apply { gravity = Gravity.CENTER }, lp(0, -2, 1f))
        top.addView(View(this), lp(dp(90), 1))
        page.addView(top, lp(-1, -2))

        // Landscape: side menu card. Portrait (phone): scrollable row of menu items at the top.
        val main = LinearLayout(this).apply {
            orientation = if (portrait) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        }
        page.addView(main, lp(-1, 0, 1f))
        val menu = LinearLayout(this).apply {
            orientation = if (portrait) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            if (!portrait) background = cardBackground()
        }
        if (portrait) main.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(menu) }, lp(-1, -2))
        else main.addView(ScrollView(this).apply { addView(menu) }, lp(dp(270), -1).apply { setMargins(dp(16), dp(8), dp(4), dp(16)) })

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(if (portrait) 14 else 28), dp(8), dp(if (portrait) 14 else 28), dp(40))
        }
        val scroll = ScrollView(this).apply { addView(body) }
        main.addView(scroll, if (portrait) lp(-1, 0, 1f) else lp(0, -1, 1f))

        val tabs = listOf(
            Tab(t("عام", "General"), t("المنطقة والساعة والعرض", "Region, clock and display"),
                R.drawable.ic_set_general, Color.parseColor("#8E8E93")) { pageGeneral(it) },
            Tab(t("تعديل المواقيت", "Adjust times"), t("تقديم أو تأخير أي صلاة بالدقائق", "Move any prayer earlier or later"),
                R.drawable.ic_set_time, Color.parseColor("#FF9F0A")) { pageAdjust(it) },
            Tab(t("الأذان", "Adhan"), t("الصوت والمدة والاختيار", "Sound, length and choice"),
                R.drawable.ic_set_adhan, Color.parseColor("#30D158")) { pageAdhan(it) },
            Tab(t("المظهر", "Appearance"), t("النمط والألوان والخلفية والخط", "Style, colors, wallpaper and text"),
                R.drawable.ic_set_appearance, Color.parseColor("#BF5AF2")) { pageAppearance(it) },
            Tab(t("الأدعية والرسائل", "Duas & messages"), t("البطاقة المتغيرة على الشاشة", "The rotating card on screen"),
                R.drawable.ic_set_duas, Color.parseColor("#5AC8FA")) { pageTiles(it) },
            Tab(t("اللغة", "Language"), t("لغة شاشة الإعدادات", "Language of the settings screen"),
                R.drawable.ic_set_language, blue) { pageLanguage(it) })

        val items = ArrayList<View>()
        fun show(i: Int) {
            tab = i
            items.forEachIndexed { k, v ->
                v.background = focusable(if (k == i) rounded(Color.argb(55, 255, 255, 255), 14) else null, 14)
            }
            body.removeAllViews(); card = null
            pageTitle(body, tabs[i].title, tabs[i].sub)
            tabs[i].page(body)
            scroll.scrollTo(0, 0)
        }
        tabs.forEachIndexed { i, tb ->
            val v = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(9), dp(14), dp(9))
                isClickable = true; isFocusable = true
                setOnClickListener { show(i) }
            }
            v.addView(ImageView(this).apply {
                setImageResource(tb.icon); background = rounded(tb.color, 9); setPadding(dp(6), dp(6), dp(6), dp(6))
            }, lp(dp(34), dp(34)))
            v.addView(text(tb.title, 16f, true), lp(-2, -2).apply { marginStart = dp(12) })
            items += v
            menu.addView(v, if (portrait) lp(-2, -2).apply { marginEnd = dp(6) } else lp(-1, -2).apply { bottomMargin = dp(4) })
        }
        show(tab.coerceIn(0, tabs.size - 1))
        return page
    }

    // ---- pages -----------------------------------------------------------------------------

    private fun pageGeneral(body: LinearLayout) {
        group(body, t("الموقع", "Location"))
        dropdown(body, t("المنطقة", "Region"), Regions.all.map { if (isAr) it.nameAr else it.nameEn },
            { prefs.getInt("region", 0) }) { pos -> edit { putInt("region", pos) } }

        group(body, t("الساعة", "Clock"))
        row(body, t("نظام 24 ساعة", "24-hour clock"), switchOf("h24", false))
        val ampm = listOf("short", "full", "none")
        choices(body, t("صباحاً / مساءً", "AM / PM label"), listOf("ص / م", "صباحاً / مساءً", t("بدون", "None")), 3,
            { ampm.indexOf(prefs.getString("ampm", "short")).coerceAtLeast(0) },
            t("في نظام 12 ساعة", "With the 12-hour clock")) { pos -> edit { putString("ampm", ampm[pos]) } }
        choices(body, t("تعديل التاريخ الهجري", "Hijri date adjustment"), listOf("-1", "0", "+1"), 3,
            { prefs.getInt("hijriAdj", 0) + 1 },
            t("إذا كان التاريخ الهجري يختلف بيوم", "If the Hijri date is a day off")) { pos -> edit { putInt("hijriAdj", pos - 1) } }

        group(body, t("الشاشة", "Display"))
        val modes = listOf("auto", "landscape", "portrait")
        choices(body, t("اتجاه الشاشة", "Screen orientation"),
            listOf(t("تلقائي", "Auto"), t("أفقي (تلفاز)", "Landscape (TV)"), t("عمودي", "Portrait")), 3,
            { modes.indexOf(prefs.getString("orientation", "auto")).coerceAtLeast(0) },
            if (Config.isTv(this)) t("تم اكتشاف تلفاز", "TV detected") else null) { pos ->
            edit { putString("orientation", modes[pos]) }
            Config.applyOrientation(this)
        }
        row(body, t("عرض المواقيت فقط", "Prayer times only"), switchOf("timesOnly", false),
            t("إخفاء التاريخ والساعة والرسائل", "Hide dates, clock and messages"))
    }

    /** Shift each prayer by -60..+60 minutes when the calculated time differs from the local mosque. */
    private fun pageAdjust(body: LinearLayout) {
        group(body, t("الصلوات — الجمعة تتبع الظهر", "Prayers — Jumuah follows Dhuhr"))
        val names = listOf(t("الفجر", "Fajr"), t("الشروق", "Sunrise"), t("الظهر", "Dhuhr"),
            t("العصر", "Asr"), t("المغرب", "Maghrib"), t("العشاء", "Isha"))
        val labels = ArrayList<TextView>()
        fun show(i: Int) {
            val v = prefs.getInt("adj_$i", 0)
            labels[i].text = (if (v > 0) "+" else "") + v + t(" د", " min")
        }
        names.forEachIndexed { i, name ->
            val value = text("", 17f, true).apply { gravity = Gravity.CENTER; textDirection = View.TEXT_DIRECTION_LTR }
            labels += value
            val box = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            fun step(d: Int) { edit { putInt("adj_$i", (prefs.getInt("adj_$i", 0) + d).coerceIn(-60, 60)) }; show(i) }
            box.addView(smallButton("−") { step(-1) })
            box.addView(value, lp(dp(84), -2))
            box.addView(smallButton("+") { step(1) })
            row(body, name, box)
            show(i)
        }
        group(body)
        action(body, t("إعادة كل التعديلات إلى 0", "Reset all adjustments to 0"), red) {
            edit { for (i in 0..5) remove("adj_$i") }
            names.indices.forEach { show(it) }
        }
    }

    private fun pageAdhan(body: LinearLayout) {
        group(body, t("التشغيل", "Playback"))
        row(body, t("تشغيل صوت الأذان", "Play adhan sound"), switchOf("sound", true) { AdhanScheduler.schedule(this) },
            t("عند دخول وقت كل صلاة", "At each prayer time"))
        row(body, t("الأذان والتطبيق مغلق", "Adhan when the app is closed"),
            switchOf("bgAdhan", true) { AdhanScheduler.schedule(this) },
            t("يعمل في الخلفية وحتى مع إطفاء الشاشة", "Runs in the background, even with the screen off"))
        row(body, t("إشعار وقت الصلاة", "Prayer-time notification"),
            switchOf("prayerNote", true) { AdhanScheduler.schedule(this) },
            t("رسالة باسم الصلاة عند دخول الوقت، حتى لو الصوت متوقف", "A message with the prayer name when the time comes, even if the sound is off"))
        if (!AdhanScheduler.exactAllowed(this) && Build.VERSION.SDK_INT >= 31) {
            action(body, t("السماح بالمنبهات الدقيقة (مطلوب للأذان في وقته)", "Allow exact alarms (needed for an on-time adhan)"), red) {
                try { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:$packageName"))) }
                catch (_: Exception) { }
            }
        }
        if (Build.VERSION.SDK_INT >= 23 && !Config.isTv(this)) {
            action(body, t("إعدادات توفير البطارية (اختر: بدون قيود)", "Battery optimization settings (choose: not optimized)")) {
                try { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } catch (_: Exception) { }
            }
        }
        val lengths = listOf(10, 30, 60, 120, 0)
        choices(body, t("مدة الأذان", "Adhan length"),
            listOf(t("10 ثوانٍ", "10 sec"), t("30 ثانية", "30 sec"), t("دقيقة", "1 min"),
                t("دقيقتان", "2 min"), t("كامل", "Full")), if (portrait) 3 else 5,
            { lengths.indexOf(prefs.getInt("azanLen", 0)).coerceAtLeast(0) }) { pos ->
            edit { putInt("azanLen", lengths[pos]) }
        }

        group(body, t("صوت الأذان", "Adhan voice"))
        azanBox = choices(body, t("اختر الأذان", "Choose adhan"), azanItems().map { it.first },
            if (portrait) 2 else 3, ::azanSelected, t("اضغط للاستماع", "Tap to listen"), ::azanPicked)
        action(body, t("اختيار ملف صوت من الجهاز…", "Choose an audio file from the device…")) {
            pickFile("audio/*", reqAudio)
        }

        group(body, t("تجربة", "Test"))
        action(body, t("تشغيل الأذان المختار", "Play the selected adhan")) { adhan.playSelected() }
        action(body, t("إيقاف مؤقت / استئناف", "Pause / resume")) { adhan.togglePause() }
        action(body, t("إيقاف الصوت", "Stop sound"), red) { adhan.stop() }
    }

    private fun pageAppearance(body: LinearLayout) {
        group(body, t("النمط", "Style"))
        row(body, t("الزجاج السائل", "Liquid glass"), switchOf("glass", true) { recreate() },
            t("بطاقات زجاجية شفافة", "Translucent glass cards"))
        row(body, t("تمويه صورة الخلفية", "Blur wallpaper"), switchOf("blurWall", true), t("أندرويد 12 وأحدث", "Android 12 and newer"))
        val scales = listOf(80, 90, 100, 110, 125, 150)
        choices(body, t("حجم الخط", "Text size"), scales.map { "$it%" }, if (portrait) 3 else 6,
            { scales.indexOf(prefs.getInt("fontScale", 100)).coerceAtLeast(0) }) { pos ->
            edit { putInt("fontScale", scales[pos]) }
        }

        group(body, t("الألوان", "Colors"))
        colorRow(body, t("لون النص", "Text color"), "textColor", Config.defText)
        colorRow(body, t("تمييز الصلاة القادمة", "Next-prayer highlight"), "hlColor", Config.defHighlight)
        colorRow(body, t("لون الخلفية", "Background color"), "bgColor", Color.BLACK)

        group(body, t("الخلفية", "Wallpaper"))
        val hasImage = (prefs.getString("bgImage", "") ?: "").startsWith("uri:")
        val wallItems = listOf(t("بدون", "None")) + Config.presets.map { if (isAr) it.ar else it.en } +
            (if (hasImage) listOf(t("صورتك", "Your image")) else emptyList())
        choices(body, t("تدرج لوني", "Gradient"), wallItems, 3, {
            val bg = prefs.getString("bgImage", "") ?: ""
            when {
                bg.startsWith("preset:") -> (bg.removePrefix("preset:").toIntOrNull() ?: 0) + 1
                bg.startsWith("uri:") -> wallItems.size - 1
                else -> 0
            }
        }) { pos ->
            when {
                pos == 0 -> edit { remove("bgImage") }
                pos <= Config.presets.size -> edit { putString("bgImage", "preset:${pos - 1}") }
            }
        }
        action(body, t("اختيار صورة خلفية من الجهاز…", "Choose a wallpaper image from the device…")) {
            pickFile("image/*", reqImage)
        }
        val dimLabel = text("${prefs.getInt("dim", 0)}%", 15f, c = secondary).apply { minWidth = dp(44) }
        val dimBar = SeekBar(this).apply {
            max = 85; progress = prefs.getInt("dim", 0)
            progressTintList = ColorStateList.valueOf(blue)
            thumbTintList = ColorStateList.valueOf(Color.WHITE)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    dimLabel.text = "$p%"; if (fromUser) edit { putInt("dim", p) }
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        val dimBox = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        dimBox.addView(dimBar, lp(dp(if (portrait) 150 else 240), -2))
        dimBox.addView(dimLabel)
        row(body, t("تعتيم الخلفية", "Dimming"), dimBox)

        group(body, t("خلفية قائمة المواقيت", "Prayer list background"))
        action(body, t("اختيار صورة خلف المواقيت…", "Choose a picture behind the prayer times…")) {
            pickFile("image/*", reqListImage)
        }
        if (!prefs.getString("listImage", "").isNullOrEmpty()) {
            val lDims = listOf(0, 20, 40, 60, 80)
            choices(body, t("تعتيم الصورة", "Picture dimming"), lDims.map { "$it%" }, 5,
                { lDims.indexOf(prefs.getInt("listDim", 40)).coerceAtLeast(0) },
                t("أعلى = نص أوضح", "Higher = easier to read")) { pos -> edit { putInt("listDim", lDims[pos]) } }
            action(body, t("إزالة الصورة", "Remove picture"), red) { edit { remove("listImage") }; recreate() }
        }

        group(body)
        action(body, t("إعادة الألوان والخلفية الافتراضية", "Reset colors and wallpaper"), red) {
            edit { remove("textColor"); remove("hlColor"); remove("bgColor"); remove("bgImage"); remove("dim") }
            recreate()
        }
    }

    private fun colorRow(body: LinearLayout, label: String, key: String, def: Int) {
        val swatch = View(this)
        fun paint() {
            swatch.background = focusable(GradientDrawable().apply {
                setColor(prefs.getInt(key, def)); setStroke(dp(2), Color.WHITE); cornerRadius = dp(15).toFloat()
            }, 15)
        }
        paint()
        swatch.layoutParams = lp(dp(30), dp(30))
        swatch.isClickable = true; swatch.isFocusable = true
        row(body, label, swatch)

        // Palette opens inside the card under the row.
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; visibility = View.GONE; setPadding(dp(12), 0, dp(12), dp(12))
        }
        fun choose(c: Int) { edit { putInt(key, c) }; paint(); panel.visibility = View.GONE }
        for (chunk in Config.palette.chunked(if (portrait) 6 else 12)) {
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (hex in chunk) {
                val c = Color.parseColor(hex)
                line.addView(View(this).apply {
                    background = focusable(GradientDrawable().apply {
                        setColor(c); setStroke(dp(1), Color.argb(120, 255, 255, 255)); cornerRadius = dp(20).toFloat()
                    }, 20)
                    isClickable = true; isFocusable = true
                    setOnClickListener { choose(c) }
                }, lp(dp(40), dp(40)).apply { setMargins(dp(4), dp(4), dp(4), dp(4)) })
            }
            panel.addView(line)
        }
        val hexBox = input("#RRGGBB").apply { setSingleLine(); textDirection = View.TEXT_DIRECTION_LTR }
        val btns = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        btns.addView(hexBox, lp(dp(160), -2))
        btns.addView(smallButton(t("تطبيق", "Apply"), blue) {
            try {
                val raw = hexBox.text.toString().trim()
                choose(Color.parseColor(if (raw.startsWith("#")) raw else "#$raw"))
            } catch (_: Exception) {
                Toast.makeText(this, t("رمز اللون غير صحيح", "Invalid color code"), Toast.LENGTH_SHORT).show()
            }
        }, lp(-2, -2).apply { setMargins(dp(8), 0, dp(8), 0) })
        btns.addView(smallButton(t("الافتراضي", "Default")) { choose(def) })
        panel.addView(btns, lp(-1, -2).apply { topMargin = dp(8) })
        card?.addView(panel, lp(-1, -2))
        swatch.setOnClickListener { panel.visibility = if (panel.visibility == View.GONE) View.VISIBLE else View.GONE }
    }

    private fun pageTiles(body: LinearLayout) {
        group(body, t("العرض", "Display"))
        row(body, t("الآيات والأدعية المدمجة", "Built-in verses and duas"), switchOf("tilesBuiltin", true))
        row(body, t("دعاء ما بعد الأذان", "After-adhan dua"), switchOf("tileAfterAdhan", true),
            t("يظهر 5 دقائق بعد كل أذان", "Shown for 5 minutes after each adhan"))
        val secs = listOf(10, 20, 30, 60)
        choices(body, t("مدة عرض كل رسالة", "Time per message"), secs.map { t("$it ث", "$it s") }, 4,
            { secs.indexOf(prefs.getInt("tileSeconds", 30)).coerceAtLeast(0) }) { pos ->
            edit { putInt("tileSeconds", secs[pos]) }
        }

        group(body, t("رسائلك وأدعيتك", "Your messages and duas"))
        val listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        item(body, listBox)
        fun refresh() {
            listBox.removeAllViews()
            val items = Config.customTiles(prefs)
            if (items.isEmpty()) listBox.addView(text(t("لا توجد رسائل بعد", "No messages yet"), 15f, c = secondary)
                .apply { setPadding(dp(16), dp(14), dp(16), dp(14)) })
            items.forEachIndexed { i, msg ->
                if (i > 0) listBox.addView(View(this).apply { setBackgroundColor(Color.argb(38, 255, 255, 255)) },
                    lp(-1, 1).apply { marginStart = dp(16) })
                val r = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(16), dp(10), dp(16), dp(10))
                }
                r.addView(text(msg, 16f), lp(0, -2, 1f))
                r.addView(smallButton(t("حذف", "Delete"), red) {
                    Config.saveTiles(prefs, items.filterIndexed { k, _ -> k != i }); refresh()
                })
                listBox.addView(r, lp(-1, -2))
            }
        }
        refresh()
        val add = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }
        val field = input(t("اكتب دعاءً أو رسالة…", "Type a dua or message…")).apply { minLines = 2 }
        add.addView(field, lp(0, -2, 1f))
        add.addView(smallButton(t("إضافة", "Add"), blue) {
            val msg = field.text.toString().trim()
            if (msg.isNotEmpty()) { Config.saveTiles(prefs, Config.customTiles(prefs) + msg); field.setText(""); refresh() }
        }, lp(-2, -2).apply { marginStart = dp(10) })
        item(body, add)
    }

    private fun pageLanguage(body: LinearLayout) {
        group(body)
        choices(body, t("لغة الإعدادات", "Settings language"), listOf("العربية", "English"), 2,
            { if (isAr) 0 else 1 }, t("شاشة المواقيت تبقى بالعربية", "The prayer screen stays in Arabic")) { pos ->
            val new = if (pos == 0) "ar" else "en"
            if (new != lang) { edit { putString("lang", new) }; recreate() }
        }
    }

    // ---- file pickers ----------------------------------------------------------------------

    private fun pickFile(mime: String, req: Int) {
        fun intentFor(action: String) = Intent(action).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = mime
            if (action == Intent.ACTION_OPEN_DOCUMENT)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try { startActivityForResult(intentFor(Intent.ACTION_OPEN_DOCUMENT), req) }
        catch (_: ActivityNotFoundException) {
            try { startActivityForResult(intentFor(Intent.ACTION_GET_CONTENT), req) }
            catch (_: ActivityNotFoundException) {
                Toast.makeText(this, t("لا يوجد تطبيق لاختيار الملفات على هذا الجهاز",
                    "No file-picker app is installed on this device"), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        val uri = data?.data
        if (res != RESULT_OK || uri == null) return
        try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        catch (_: Exception) { /* not persistable: works until the app is restarted */ }
        if (req == reqImage) { edit { putString("bgImage", "uri:$uri") }; recreate() }
        else if (req == reqListImage) { edit { putString("listImage", uri.toString()) }; recreate() }
        else if (req == reqAudio) {
            edit { putString("azan", "uri"); putString("azanUri", uri.toString()) }
            fillAzan(); adhan.playSelected()
        }
    }
}
