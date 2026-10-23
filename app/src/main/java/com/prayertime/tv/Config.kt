package com.prayertime.tv

import android.graphics.Color

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
}
