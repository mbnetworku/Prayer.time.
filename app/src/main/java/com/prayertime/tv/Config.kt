package com.prayertime.tv

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import org.json.JSONArray

/** Shared by the main screen and the settings screen. */
object Config {
    val defText = Color.parseColor("#8EC5FF")
    val defHighlight = Color.parseColor("#14305A")

    class Preset(val ar: String, val en: String, val colors: IntArray)

    val presets = listOf(
        Preset("أزرق ليلي", "Night blue", intArrayOf(Color.parseColor("#0B1B3A"), Color.BLACK)),
        Preset("أخضر داكن", "Dark green", intArrayOf(Color.parseColor("#0B3D2E"), Color.parseColor("#02130E"))),
        Preset("بنفسجي", "Purple", intArrayOf(Color.parseColor("#2A1458"), Color.parseColor("#0A0418"))),
        Preset("ذهبي داكن", "Dark gold", intArrayOf(Color.parseColor("#3A2A0B"), Color.parseColor("#0F0A02"))),
        Preset("رمادي", "Grey", intArrayOf(Color.parseColor("#2B2B2B"), Color.BLACK)))

    val palette = listOf(
        "#FFFFFF", "#F5F5F5", "#BDBDBD", "#757575", "#212121", "#000000", "#8EC5FF", "#42A5F5",
        "#1565C0", "#14305A", "#0B1B3A", "#00BCD4", "#81C784", "#2E7D32", "#0B3D2E", "#FFEB3B",
        "#FFC107", "#FF9800", "#E53935", "#B71C1C", "#EC407A", "#AB47BC", "#5E35B1", "#6D4C41")

    /** Adhan files are the raw resources named adhan, adhan_2, adhan_3, ... */
    fun azanOptions(): List<String> =
        R.raw::class.java.fields.map { it.name }.filter { it == "adhan" || it.startsWith("adhan_") }.sorted()

    /** User's own on-screen messages / duas (pref "tiles", JSON array of strings). */
    fun customTiles(p: SharedPreferences): List<String> = try {
        val a = JSONArray(p.getString("tiles", "[]") ?: "[]")
        List(a.length()) { a.getString(it) }
    } catch (_: Exception) { emptyList() }

    fun saveTiles(p: SharedPreferences, list: List<String>) {
        p.edit().putString("tiles", JSONArray(list).toString()).apply()
    }

    // ---- Liquid glass style (pref "glass", default on) ----------------------------------

    /** Liquid glass panel (see GlassDrawable): clear body, top sheen, light-catching rim. strong = brighter clear glass. */
    fun glass(density: Float, radiusDp: Int, tint: Int? = null, strong: Boolean = false, dark: Boolean = false): android.graphics.drawable.Drawable =
        GlassDrawable(density, radiusDp, tint, strong, dark)

    /** True when the user picked their own wallpaper image: glass then stays clear (no colour tint). */
    fun hasImage(ctx: Context) = (ctx.getSharedPreferences("prayer", Context.MODE_PRIVATE)
        .getString("bgImage", "") ?: "").startsWith("uri:")

    /** Shows the chosen wallpaper (gradient preset or the user's image) in [iv]. */
    fun loadWallpaper(ctx: android.content.Context, iv: android.widget.ImageView) {
        val prefs = ctx.getSharedPreferences("prayer", Context.MODE_PRIVATE)
        val v = prefs.getString("bgImage", "") ?: ""
        try {
            when {
                v.startsWith("preset:") -> {
                    val c = Config.presets[v.removePrefix("preset:").toInt().coerceIn(0, Config.presets.size - 1)].colors
                    iv.setImageDrawable(GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, c))
                }
                v.startsWith("uri:") -> loadImage(ctx, v.removePrefix("uri:"), iv)
            }
        } catch (_: Exception) { /* image gone or unreadable: fall back to the plain colour */ }
    }

    private val imageCache = android.util.LruCache<String, android.graphics.Bitmap>(2)

    /** Loads a user image (content uri) into [iv], scaled down to ~1920 px wide; decoded once, then cached in memory. */
    fun loadImage(ctx: android.content.Context, uriString: String, iv: android.widget.ImageView) {
        try {
            imageCache.get(uriString)?.let { iv.setImageBitmap(it); return }
            val uri = android.net.Uri.parse(uriString)
            val b = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, b) }
            var sample = 1
            while (b.outWidth / sample > 1920 || b.outHeight / sample > 1920) sample *= 2
            val o = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = ctx.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, o) }
            if (bmp != null) { imageCache.put(uriString, bmp); iv.setImageBitmap(bmp) }
        } catch (_: Throwable) { /* unreadable image or out of memory: keep the plain background */ }
    }


    /** Deep backdrop with soft coloured glows, so the glass has something to refract. */
    fun glassBackground(): android.graphics.drawable.Drawable {
        val base = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(
            Color.parseColor("#06122E"), Color.parseColor("#1B0F3D"), Color.parseColor("#04262E")))
        fun blob(color: Int, cx: Float, cy: Float, radius: Float) = GradientDrawable().apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            colors = intArrayOf(color, Color.TRANSPARENT)
            setGradientCenter(cx, cy); gradientRadius = radius
        }
        return android.graphics.drawable.LayerDrawable(arrayOf(base,
            blob(Color.argb(150, 64, 120, 255), 0.15f, 0.2f, 900f),
            blob(Color.argb(130, 190, 70, 220), 0.85f, 0.35f, 800f),
            blob(Color.argb(120, 20, 200, 190), 0.45f, 1.0f, 900f)))
    }

    // ---- Screen orientation (pref "orientation": auto / landscape / portrait) ------------

    /** True on Android TV / TV boxes. */
    fun isTv(ctx: Context): Boolean {
        val ui = ctx.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        return ui?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
            ctx.packageManager.hasSystemFeature("android.software.leanback")
    }

    /** Auto: TV = landscape, phone/tablet = follow the device. Or forced landscape / portrait. */
    fun applyOrientation(a: Activity) {
        val mode = a.getSharedPreferences("prayer", Context.MODE_PRIVATE).getString("orientation", "auto")
        a.requestedOrientation = when (mode) {
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else -> if (isTv(a)) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    else ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        }
    }

    // ---- App font and text size ----------------------------------------------------------

    private var regularFont: android.graphics.Typeface? = null
    private var boldFont: android.graphics.Typeface? = null

    /** The app's own font (Tajawal, SIL OFL) from assets/fonts, or null to use the device font (pref "appFont"). */
    fun font(ctx: Context, bold: Boolean): android.graphics.Typeface? {
        if (!ctx.getSharedPreferences("prayer", Context.MODE_PRIVATE).getBoolean("appFont", true)) return null
        try {
            if (regularFont == null) regularFont = android.graphics.Typeface.createFromAsset(ctx.assets, "fonts/Tajawal-Regular.ttf")
            if (boldFont == null) boldFont = android.graphics.Typeface.createFromAsset(ctx.assets, "fonts/Tajawal-Bold.ttf")
        } catch (_: Throwable) { return null }
        return if (bold) boldFont else regularFont
    }

    /**
     * Applies the app font and size to a text view. Size is in dp, so it does NOT change with the device's
     * font-size setting, unless the user turned on "Follow device text size" (pref "systemTextSize").
     */
    fun style(ctx: Context, tv: android.widget.TextView, size: Float, bold: Boolean) {
        val followDevice = ctx.getSharedPreferences("prayer", Context.MODE_PRIVATE).getBoolean("systemTextSize", false)
        tv.setTextSize(if (followDevice) android.util.TypedValue.COMPLEX_UNIT_SP else android.util.TypedValue.COMPLEX_UNIT_DIP, size)
        val f = font(ctx, bold)
        if (f != null) tv.typeface = f
        else if (bold) tv.setTypeface(tv.typeface, android.graphics.Typeface.BOLD)
    }
}
