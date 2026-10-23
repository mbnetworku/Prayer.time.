package com.prayertime.tv

import android.content.SharedPreferences
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

    /** Frosted glass panel: translucent white (or tinted) gradient, soft light edge, rounded corners. */
    fun glass(density: Float, radiusDp: Int, tint: Int? = null): GradientDrawable {
        val top = if (tint != null) Color.argb(170, Color.red(tint), Color.green(tint), Color.blue(tint)) else Color.argb(64, 255, 255, 255)
        val bottom = if (tint != null) Color.argb(110, Color.red(tint), Color.green(tint), Color.blue(tint)) else Color.argb(22, 255, 255, 255)
        return GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(top, bottom)).apply {
            cornerRadius = radiusDp * density
            setStroke(density.toInt().coerceAtLeast(1), Color.argb(110, 255, 255, 255))
        }
    }

    /** Deep colourful backdrop that makes the glass visible when no wallpaper is chosen. */
    fun glassBackground() = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(
        Color.parseColor("#0F2A5C"), Color.parseColor("#3A1C71"), Color.parseColor("#0E5E6F")))
}
