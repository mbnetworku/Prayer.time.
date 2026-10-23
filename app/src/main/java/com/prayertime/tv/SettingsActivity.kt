package com.prayertime.tv

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/** One screen with every setting. Works with mouse, touch and remote. Language: Arabic / English. */
class SettingsActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("prayer", Context.MODE_PRIVATE) }
    private lateinit var adhan: AdhanPlayer
    private val lang get() = prefs.getString("lang", null)
        ?: if (java.util.Locale.getDefault().language == "ar") "ar" else "en"
    private val isAr get() = lang == "ar"
    private fun t(ar: String, en: String) = if (isAr) ar else en

    private val pageBg = Color.parseColor("#0E1116")
    private val cardBg = Color.parseColor("#171C24")
    private val accent = Config.defText
    private val reqImage = 21
    private val reqAudio = 22
    private lateinit var azanBox: LinearLayout

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        adhan = AdhanPlayer(this)
        setContentView(build())
    }

    override fun onResume() {
        super.onResume()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    override fun onPause() { adhan.stop(); super.onPause() }
    override fun onDestroy() { adhan.stop(); super.onDestroy() }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun edit(f: SharedPreferences.Editor.() -> Unit) { prefs.edit().apply(f).apply() }

    private fun round(c: Int) = GradientDrawable().apply { setColor(c); cornerRadius = dp(10).toFloat() }

    private fun text(s: String, size: Float, bold: Boolean = false, c: Int = Color.WHITE) = TextView(this).apply {
        text = s; textSize = size; setTextColor(c)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun button(label: String, onClick: () -> Unit) = TextView(this).apply {
        text = label; textSize = 17f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        setPadding(dp(20), dp(10), dp(20), dp(10))
        background = round(Color.parseColor("#2A5FA8"))
        isClickable = true; isFocusable = true
        setOnClickListener { onClick() }
    }

    private fun section(body: LinearLayout, title: String) {
        body.addView(text(title, 20f, true, accent),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24) })
    }

    private fun row(body: LinearLayout, label: String, control: View) {
        val r = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8)); background = round(cardBg)
        }
        r.addView(text(label, 18f), LinearLayout.LayoutParams(0, -2, 1f))
        r.addView(control)
        body.addView(r, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
    }

    private fun wide(body: LinearLayout, v: View) {
        body.addView(v, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(8) })
    }

    /** Inline choice buttons (no drop-down / pop-up). The selected one is highlighted. */
    private fun fillChips(box: LinearLayout, items: List<String>, selected: () -> Int, perRow: Int, onPick: (Int) -> Unit) {
        box.removeAllViews()
        val views = ArrayList<TextView>()
        fun paint() {
            val sel = selected()
            views.forEachIndexed { i, v ->
                v.background = round(if (i == sel) Color.parseColor("#2A5FA8") else Color.parseColor("#232A35"))
            }
        }
        items.chunked(perRow).forEachIndexed { r, chunk ->
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            chunk.forEachIndexed { k, label ->
                val idx = r * perRow + k
                val v = TextView(this).apply {
                    text = label; textSize = 16f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
                    setPadding(dp(10), dp(12), dp(10), dp(12))
                    isClickable = true; isFocusable = true
                    setOnClickListener { onPick(idx); paint() }
                }
                views += v
                line.addView(v, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(4), dp(4), dp(4), dp(4)) })
            }
            repeat(perRow - chunk.size) { line.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f)) }
            box.addView(line)
        }
        paint()
    }

    /** Inline drop-down: shows the current value; clicking opens the list inside the page (no pop-up). */
    private fun dropdown(body: LinearLayout, label: String, items: List<String>,
                         selected: () -> Int, onPick: (Int) -> Unit) {
        body.addView(text(label, 18f), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        val head = TextView(this).apply {
            textSize = 17f; setTextColor(Color.WHITE); setPadding(dp(16), dp(12), dp(16), dp(12))
            background = round(Color.parseColor("#232A35")); isClickable = true; isFocusable = true
        }
        fun refresh() { head.text = items[selected().coerceIn(0, items.size - 1)] + "   ▼" }
        refresh()
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; visibility = View.GONE
            background = round(cardBg); setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        val views = ArrayList<TextView>()
        fun paint() {
            val sel = selected()
            views.forEachIndexed { i, v -> v.background = if (i == sel) round(Color.parseColor("#2A5FA8")) else null }
        }
        items.forEachIndexed { i, label2 ->
            val v = TextView(this).apply {
                text = label2; textSize = 16f; setTextColor(Color.WHITE)
                setPadding(dp(16), dp(10), dp(16), dp(10)); isClickable = true; isFocusable = true
                setOnClickListener { onPick(i); refresh(); list.visibility = View.GONE }
            }
            views += v
            list.addView(v, LinearLayout.LayoutParams(-1, -2))
        }
        head.setOnClickListener { paint(); list.visibility = if (list.visibility == View.GONE) View.VISIBLE else View.GONE }
        body.addView(head, LinearLayout.LayoutParams(-1, -2))
        body.addView(list, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
    }

    private fun chipGroup(body: LinearLayout, label: String, items: List<String>, perRow: Int,
                          selected: () -> Int, onPick: (Int) -> Unit) {
        body.addView(text(label, 18f), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fillChips(box, items, selected, perRow, onPick)
        body.addView(box, LinearLayout.LayoutParams(-1, -2))
    }

    private fun switchOf(key: String, def: Boolean) = Switch(this).apply {
        isChecked = prefs.getBoolean(key, def)
        setOnCheckedChangeListener { _, c -> edit { putBoolean(key, c) } }
    }

    // ---- adhan choices -------------------------------------------------------------------

    private fun azanItems(): List<Pair<String, String>> {
        val l = Config.azanOptions().mapIndexed { i, n -> Pair(t("الأذان ${i + 1}", "Adhan ${i + 1}"), n) }.toMutableList()
        if (!prefs.getString("azanUri", "").isNullOrEmpty()) l += Pair(t("ملف من الجهاز", "File from device"), "uri")
        return l
    }

    private fun currentAzan() = prefs.getString("azan", "adhan") ?: "adhan"

    private fun fillAzan() {
        fillChips(azanBox, azanItems().map { it.first },
            { azanItems().indexOfFirst { it.second == currentAzan() }.coerceAtLeast(0) }, 3) { pos ->
            val v = azanItems().getOrNull(pos)?.second
            if (v != null) { edit { putString("azan", v) }; adhan.playSelected() }
        }
    }

    // ---- the page --------------------------------------------------------------------------

    private var tab: Int
        get() = prefs.getInt("settingsTab", 0)
        set(v) { edit { putInt("settingsTab", v) } }

    /** Full fresh screen: header, side menu with categories, and the selected category's page. */
    private fun build(): View {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(pageBg)
            layoutDirection = if (isAr) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(24), dp(12), dp(24), dp(12)); setBackgroundColor(cardBg)
        }
        header.addView(button(t("رجوع", "Back")) { finish() })
        header.addView(text(t("الإعدادات", "Settings"), 26f, true).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(View(this), LinearLayout.LayoutParams(dp(80), 1))
        page.addView(header, LinearLayout.LayoutParams(-1, -2))

        val main = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        page.addView(main, LinearLayout.LayoutParams(-1, 0, 1f))

        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(16), dp(12), dp(16)); setBackgroundColor(cardBg)
        }
        main.addView(menu, LinearLayout.LayoutParams(dp(220), -1))

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(32), dp(8), dp(32), dp(40))
        }
        val scroll = ScrollView(this).apply { addView(body) }
        main.addView(scroll, LinearLayout.LayoutParams(0, -1, 1f))

        val tabs = listOf(
            t("عام", "General") to { b: LinearLayout -> pageGeneral(b) },
            t("تعديل المواقيت", "Adjust times") to { b: LinearLayout -> pageAdjust(b) },
            t("الأذان", "Adhan") to { b: LinearLayout -> pageAdhan(b) },
            t("المظهر", "Appearance") to { b: LinearLayout -> pageAppearance(b) },
            t("الأدعية والرسائل", "Duas & messages") to { b: LinearLayout -> pageTiles(b) },
            t("اللغة", "Language") to { b: LinearLayout -> pageLanguage(b) })
        val items = ArrayList<TextView>()
        fun show(i: Int) {
            tab = i
            items.forEachIndexed { k, v -> v.background = round(if (k == i) Color.parseColor("#2A5FA8") else Color.TRANSPARENT) }
            body.removeAllViews(); adhan.stop()
            tabs[i].second(body)
            scroll.scrollTo(0, 0)
        }
        tabs.forEachIndexed { i, (label, _) ->
            val v = text(label, 19f, true).apply {
                setPadding(dp(16), dp(14), dp(16), dp(14)); isClickable = true; isFocusable = true
                setOnClickListener { show(i) }
            }
            items += v
            menu.addView(v, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
        }
        show(tab.coerceIn(0, tabs.size - 1))
        return page
    }

    /** Shift each prayer by -60..+60 minutes when the calculated time differs from the local mosque. */
    private fun pageAdjust(body: LinearLayout) {
        section(body, t("تعديل أوقات الصلاة (بالدقائق)", "Adjust prayer times (minutes)"))
        body.addView(text(t("استخدم + و − لتقديم أو تأخير أي صلاة. الجمعة تتبع الظهر.",
            "Use + and − to move a prayer earlier or later. Jumuah follows Dhuhr."), 15f, c = Color.GRAY))
        val names = listOf(t("الفجر", "Fajr"), t("الشروق", "Sunrise"), t("الظهر", "Dhuhr"),
            t("العصر", "Asr"), t("المغرب", "Maghrib"), t("العشاء", "Isha"))
        val labels = ArrayList<TextView>()
        fun show(i: Int) {
            val v = prefs.getInt("adj_$i", 0)
            labels[i].text = (if (v > 0) "+" else "") + v + t(" د", " min")
        }
        names.forEachIndexed { i, name ->
            val value = text("", 18f, true).apply { gravity = Gravity.CENTER; textDirection = View.TEXT_DIRECTION_LTR }
            labels += value
            val box = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            fun step(d: Int) { edit { putInt("adj_$i", (prefs.getInt("adj_$i", 0) + d).coerceIn(-60, 60)) }; show(i) }
            box.addView(button("−") { step(-1) })
            box.addView(value, LinearLayout.LayoutParams(dp(110), -2))
            box.addView(button("+") { step(1) })
            row(body, name, box)
            show(i)
        }
        wide(body, button(t("إعادة كل التعديلات إلى 0", "Reset all adjustments to 0")) {
            edit { for (i in 0..5) remove("adj_$i") }
            names.indices.forEach { show(it) }
        })
    }

    private fun pageTiles(body: LinearLayout) {
        section(body, t("الأدعية والرسائل على الشاشة", "Duas & messages on screen"))
        row(body, t("عرض الآيات والأدعية المدمجة", "Show built-in verses and duas"), switchOf("tilesBuiltin", true))
        row(body, t("عرض دعاء ما بعد الأذان (5 دقائق)", "Show the after-adhan dua (5 minutes)"), switchOf("tileAfterAdhan", true))
        val secs = listOf(10, 20, 30, 60)
        chipGroup(body, t("مدة عرض كل رسالة", "Time per message"), secs.map { t("$it ثانية", "$it sec") }, 4,
            { secs.indexOf(prefs.getInt("tileSeconds", 30)).coerceAtLeast(0) }) { pos ->
            edit { putInt("tileSeconds", secs[pos]) }
        }

        body.addView(text(t("رسائلك وأدعيتك", "Your messages and duas"), 18f),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
        val listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(listBox, LinearLayout.LayoutParams(-1, -2))
        fun refresh() {
            listBox.removeAllViews()
            val items = Config.customTiles(prefs)
            if (items.isEmpty()) listBox.addView(text(t("لا توجد رسائل بعد", "No messages yet"), 16f, c = Color.GRAY))
            items.forEachIndexed { i, msg ->
                val r = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(16), dp(8), dp(16), dp(8)); background = round(cardBg)
                }
                r.addView(text(msg, 17f), LinearLayout.LayoutParams(0, -2, 1f))
                r.addView(button(t("حذف", "Delete")) {
                    Config.saveTiles(prefs, items.filterIndexed { k, _ -> k != i }); refresh()
                })
                listBox.addView(r, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
            }
        }
        refresh()
        val input = EditText(this).apply {
            hint = t("اكتب دعاءً أو رسالة…", "Type a dua or message…")
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); minLines = 2
        }
        body.addView(input, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        wide(body, button(t("إضافة", "Add")) {
            val msg = input.text.toString().trim()
            if (msg.isNotEmpty()) { Config.saveTiles(prefs, Config.customTiles(prefs) + msg); input.setText(""); refresh() }
        })
    }

    private fun pageLanguage(body: LinearLayout) {
        section(body, t("اللغة", "Language"))
        chipGroup(body, t("لغة الإعدادات", "Settings language"), listOf("العربية", "English"), 2,
            { if (isAr) 0 else 1 }) { pos ->
            val new = if (pos == 0) "ar" else "en"
            if (new != lang) { edit { putString("lang", new) }; recreate() }
        }

    }

    private fun pageGeneral(body: LinearLayout) {
        section(body, t("عام", "General"))
        dropdown(body, t("المنطقة", "Region"), Regions.all.map { if (isAr) it.nameAr else it.nameEn },
            { prefs.getInt("region", 0) }) { pos -> edit { putInt("region", pos) } }
        row(body, t("نظام 24 ساعة", "24-hour clock"), switchOf("h24", false))
        row(body, t("عرض المواقيت فقط (إخفاء التاريخ والساعة)", "Show prayer times only (hide dates and clock)"),
            switchOf("timesOnly", false))
        chipGroup(body, t("تعديل التاريخ الهجري (أيام)", "Hijri date adjustment (days)"), listOf("-1", "0", "+1"), 3,
            { prefs.getInt("hijriAdj", 0) + 1 }) { pos -> edit { putInt("hijriAdj", pos - 1) } }

    }

    private fun pageAdhan(body: LinearLayout) {
        section(body, t("الأذان", "Adhan"))
        row(body, t("تشغيل صوت الأذان", "Play adhan sound"), switchOf("sound", true))
        val lengths = listOf(10, 30, 60, 120, 0)
        chipGroup(body, t("مدة الأذان", "Adhan length"),
            listOf(t("10 ثوانٍ", "10 sec"), t("30 ثانية", "30 sec"), t("دقيقة", "1 min"),
                t("دقيقتان", "2 min"), t("الأذان كاملاً", "Full adhan")), 5,
            { lengths.indexOf(prefs.getInt("azanLen", 0)).coerceAtLeast(0) }) { pos ->
            edit { putInt("azanLen", lengths[pos]) }
        }
        body.addView(text(t("اختيار الأذان (اضغط للاستماع)", "Choose adhan (tap to listen)"), 18f),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        azanBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(azanBox, LinearLayout.LayoutParams(-1, -2))
        fillAzan()
        wide(body, button(t("اختيار ملف صوت من الجهاز…", "Choose an audio file from the device…")) {
            pickFile("audio/*", reqAudio)
        })
        wide(body, button(t("إيقاف الصوت", "Stop sound")) { adhan.stop() })

    }

    private fun pageAppearance(body: LinearLayout) {
        section(body, t("المظهر", "Appearance"))
        val scales = listOf(80, 90, 100, 110, 125, 150)
        chipGroup(body, t("حجم الخط", "Text size"), scales.map { "$it%" }, 6,
            { scales.indexOf(prefs.getInt("fontScale", 100)).coerceAtLeast(0) }) { pos ->
            edit { putInt("fontScale", scales[pos]) }
        }
        colorRow(body, t("لون النص", "Text color"), "textColor", Config.defText)
        colorRow(body, t("لون تمييز الصلاة القادمة", "Next-prayer highlight color"), "hlColor", Config.defHighlight)
        colorRow(body, t("لون الخلفية", "Background color"), "bgColor", Color.BLACK)

        val hasImage = (prefs.getString("bgImage", "") ?: "").startsWith("uri:")
        val wallItems = listOf(t("بدون", "None")) + Config.presets.map { if (isAr) it.ar else it.en } +
            (if (hasImage) listOf(t("صورتك", "Your image")) else emptyList())
        chipGroup(body, t("الخلفية (تدرج)", "Wallpaper (gradient)"), wallItems, 3, {
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
        wide(body, button(t("اختيار صورة خلفية من الجهاز…", "Choose a wallpaper image from the device…")) {
            pickFile("image/*", reqImage)
        })

        val dimLabel = text("${prefs.getInt("dim", 0)}%", 16f)
        val dimBar = SeekBar(this).apply {
            max = 85; progress = prefs.getInt("dim", 0)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    dimLabel.text = "$p%"; if (fromUser) edit { putInt("dim", p) }
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        val dimBox = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        dimBox.addView(dimBar, LinearLayout.LayoutParams(dp(260), -2))
        dimBox.addView(dimLabel)
        row(body, t("تعتيم الخلفية", "Wallpaper dimming"), dimBox)

        wide(body, button(t("إعادة الألوان والخلفية الافتراضية", "Reset colors and wallpaper")) {
            edit { remove("textColor"); remove("hlColor"); remove("bgColor"); remove("bgImage"); remove("dim") }
            recreate()
        })
    }

    private fun colorRow(body: LinearLayout, label: String, key: String, def: Int) {
        val swatch = View(this).apply { isClickable = true; isFocusable = true }
        fun paint() {
            swatch.background = GradientDrawable().apply {
                setColor(prefs.getInt(key, def)); setStroke(dp(2), Color.WHITE); cornerRadius = dp(8).toFloat()
            }
        }
        paint()
        swatch.layoutParams = LinearLayout.LayoutParams(dp(64), dp(40))
        row(body, label, swatch)

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; visibility = View.GONE; setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        fun choose(c: Int) { edit { putInt(key, c) }; paint(); panel.visibility = View.GONE }
        for (chunk in Config.palette.chunked(8)) {
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (hex in chunk) {
                val c = Color.parseColor(hex)
                line.addView(View(this).apply {
                    background = GradientDrawable().apply { setColor(c); setStroke(dp(2), Color.GRAY); cornerRadius = dp(8).toFloat() }
                    isClickable = true; isFocusable = true
                    setOnClickListener { choose(c) }
                }, LinearLayout.LayoutParams(dp(48), dp(48)).apply { setMargins(dp(4), dp(4), dp(4), dp(4)) })
            }
            panel.addView(line)
        }
        val hexBox = EditText(this).apply {
            hint = "#RRGGBB"; setSingleLine(); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
            textDirection = View.TEXT_DIRECTION_LTR
        }
        val btns = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        btns.addView(hexBox, LinearLayout.LayoutParams(dp(180), -2))
        btns.addView(button(t("تطبيق", "Apply")) {
            try {
                val raw = hexBox.text.toString().trim()
                choose(Color.parseColor(if (raw.startsWith("#")) raw else "#$raw"))
            } catch (_: Exception) {
                Toast.makeText(this, t("رمز اللون غير صحيح", "Invalid color code"), Toast.LENGTH_SHORT).show()
            }
        }, LinearLayout.LayoutParams(-2, -2).apply { setMargins(dp(8), 0, dp(8), 0) })
        btns.addView(button(t("الافتراضي", "Default")) { choose(def) })
        panel.addView(btns)
        body.addView(panel)
        swatch.setOnClickListener { panel.visibility = if (panel.visibility == View.GONE) View.VISIBLE else View.GONE }
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
        else if (req == reqAudio) {
            edit { putString("azan", "uri"); putString("azanUri", uri.toString()) }
            fillAzan(); adhan.playSelected()
        }
    }
}
