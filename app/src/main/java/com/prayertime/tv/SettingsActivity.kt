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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/** One screen with every setting. Works with mouse, touch and remote. Language: Arabic / English. */
class SettingsActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("prayer", Context.MODE_PRIVATE) }
    private lateinit var adhan: AdhanPlayer
    private val lang get() = prefs.getString("lang", "ar") ?: "ar"
    private val isAr get() = lang == "ar"
    private fun t(ar: String, en: String) = if (isAr) ar else en

    private val pageBg = Color.parseColor("#0E1116")
    private val cardBg = Color.parseColor("#171C24")
    private val accent = Config.defText
    private val reqImage = 21
    private val reqAudio = 22
    private var azanSpinner: Spinner? = null
    private var azanAdapter: ArrayAdapter<String>? = null

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        adhan = AdhanPlayer(this)
        setContentView(build())
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

    private fun spinnerOf(items: List<String>, sel: Int, onPick: (Int) -> Unit): Spinner {
        val sp = Spinner(this)
        val ad = ArrayAdapter(this, android.R.layout.simple_spinner_item, items.toMutableList())
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sp.adapter = ad
        sp.setSelection(sel.coerceAtLeast(0), false)
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { onPick(pos) }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        return sp
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

    private fun refreshAzan() {
        val items = azanItems()
        val ad = azanAdapter ?: return
        ad.clear(); ad.addAll(items.map { it.first })
        azanSpinner?.setSelection(items.indexOfFirst { it.second == currentAzan() }.coerceAtLeast(0), false)
    }

    // ---- the page --------------------------------------------------------------------------

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

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(32), dp(8), dp(32), dp(40))
        }
        page.addView(ScrollView(this).apply { addView(body) }, LinearLayout.LayoutParams(-1, 0, 1f))

        // Language of this settings screen.
        section(body, t("اللغة", "Language"))
        row(body, t("لغة الإعدادات", "Settings language"),
            spinnerOf(listOf("العربية", "English"), if (isAr) 0 else 1) { pos ->
                val new = if (pos == 0) "ar" else "en"
                if (new != lang) { edit { putString("lang", new) }; recreate() }
            })

        // General.
        section(body, t("عام", "General"))
        row(body, t("المنطقة", "Region"),
            spinnerOf(Regions.all.map { if (isAr) it.nameAr else it.nameEn },
                prefs.getInt("region", 0)) { pos -> edit { putInt("region", pos) } })
        row(body, t("نظام 24 ساعة", "24-hour clock"), switchOf("h24", false))
        row(body, t("عرض المواقيت فقط (إخفاء التاريخ والساعة)", "Show prayer times only (hide dates and clock)"),
            switchOf("timesOnly", false))
        row(body, t("تعديل التاريخ الهجري (أيام)", "Hijri date adjustment (days)"),
            spinnerOf(listOf("-1", "0", "+1"), prefs.getInt("hijriAdj", 0) + 1) { pos ->
                edit { putInt("hijriAdj", pos - 1) }
            })

        // Adhan.
        section(body, t("الأذان", "Adhan"))
        row(body, t("تشغيل صوت الأذان", "Play adhan sound"), switchOf("sound", true))
        val sp = spinnerOf(azanItems().map { it.first }, azanItems().indexOfFirst { it.second == currentAzan() }) { pos ->
            val v = azanItems().getOrNull(pos)?.second
            if (v != null && v != currentAzan()) { edit { putString("azan", v) }; adhan.playSelected() }
        }
        azanSpinner = sp
        @Suppress("UNCHECKED_CAST")
        azanAdapter = sp.adapter as ArrayAdapter<String>
        row(body, t("اختيار الأذان", "Choose adhan"), sp)
        wide(body, button(t("اختيار ملف صوت من الجهاز…", "Choose an audio file from the device…")) {
            pickFile("audio/*", reqAudio)
        })
        wide(body, button(t("تجربة / إيقاف الصوت", "Test / stop sound")) {
            if (adhan.isPlaying) adhan.stop() else adhan.playSelected()
        })

        // Appearance.
        section(body, t("المظهر", "Appearance"))
        colorRow(body, t("لون النص", "Text color"), "textColor", Config.defText)
        colorRow(body, t("لون تمييز الصلاة القادمة", "Next-prayer highlight color"), "hlColor", Config.defHighlight)
        colorRow(body, t("لون الخلفية", "Background color"), "bgColor", Color.BLACK)

        val wallItems = listOf(t("بدون", "None")) + Config.presets.map { if (isAr) it.ar else it.en } +
            (if ((prefs.getString("bgImage", "") ?: "").startsWith("uri:")) listOf(t("صورتك", "Your image")) else emptyList())
        val bg = prefs.getString("bgImage", "") ?: ""
        val wallSel = when {
            bg.startsWith("preset:") -> (bg.removePrefix("preset:").toIntOrNull() ?: 0) + 1
            bg.startsWith("uri:") -> wallItems.size - 1
            else -> 0
        }
        row(body, t("الخلفية (تدرج)", "Wallpaper (gradient)"), spinnerOf(wallItems, wallSel) { pos ->
            val cur = prefs.getString("bgImage", "") ?: ""
            when {
                pos == 0 -> if (cur.isNotEmpty()) edit { remove("bgImage") }
                pos <= Config.presets.size -> if (cur != "preset:${pos - 1}") edit { putString("bgImage", "preset:${pos - 1}") }
            }
        })
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
        return page
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
            refreshAzan(); adhan.playSelected()
        }
    }
}
