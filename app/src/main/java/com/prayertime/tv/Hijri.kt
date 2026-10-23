package com.prayertime.tv

import android.os.Build
import java.time.LocalDate

/** Hijri date that works on every Android version. Returns [year, month 1-12, day]. */
object Hijri {
    fun of(date: LocalDate): IntArray {
        // Android 7+: Umm al-Qura calendar from the system ICU library.
        if (Build.VERSION.SDK_INT >= 24) try {
            val utc = android.icu.util.TimeZone.getTimeZone("UTC")
            val g = android.icu.util.GregorianCalendar(utc)
            g.clear(); g.set(date.year, date.monthValue - 1, date.dayOfMonth, 12, 0)
            val cal = android.icu.util.IslamicCalendar(utc, android.icu.util.ULocale.ENGLISH)
            cal.calculationType = android.icu.util.IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
            cal.timeInMillis = g.timeInMillis
            return intArrayOf(cal.get(android.icu.util.Calendar.YEAR),
                cal.get(android.icu.util.Calendar.MONTH) + 1, cal.get(android.icu.util.Calendar.DAY_OF_MONTH))
        } catch (_: Throwable) { }
        return tabular(date)
    }

    /** Arithmetic (tabular) Islamic calendar for Android 5-6; may differ by a day (use Hijri adjustment). */
    private fun tabular(date: LocalDate): IntArray {
        val jd = date.toEpochDay() + 2440588
        var l = jd - 1948440 + 10632
        val n = (l - 1) / 10631
        l = l - 10631 * n + 354
        val j = ((10985 - l) / 5316) * ((50 * l) / 17719) + (l / 5670) * ((43 * l) / 15238)
        l = l - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        val m = (24 * l) / 709
        val d = l - (709 * m) / 24
        val y = 30 * n + j - 30
        return intArrayOf(y.toInt(), m.toInt(), d.toInt())
    }
}
