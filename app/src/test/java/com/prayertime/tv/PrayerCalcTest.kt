package com.prayertime.tv

import java.time.LocalDate
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerCalcTest {
    private fun hoursFor(r: Region, d: LocalDate): List<Double> {
        val ms = d.atStartOfDay(java.time.ZoneId.of(r.zone)).plusHours(12).toInstant().toEpochMilli()
        val off = TimeZone.getTimeZone(r.zone).getOffset(ms) / 3600000.0
        return PrayerCalc.compute(d, r.lat, r.lng, off, r.method).asList()
    }

    private fun minutes(h: Int, m: Int) = h * 60 + m

    /**
     * Reference: the mosque display in Bahrain on 23 Oct 2026 (Fajr 04:23, Sunrise 05:40, Dhuhr 11:23,
     * Asr 02:37 pm, Maghrib 05:03 pm, Isha 06:19 pm). The app must be within 2 minutes of every value.
     */
    @Test fun bahrainMatchesMosqueDisplay() {
        val bahrain = Regions.all.first { it.nameEn.startsWith("Manama") }
        val got = hoursFor(bahrain, LocalDate.of(2026, 10, 23)).map { Math.round(it * 60).toInt() }
        val expected = listOf(minutes(4, 23), minutes(5, 40), minutes(11, 23), minutes(14, 37), minutes(17, 3), minutes(18, 19))
        val names = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")
        for (i in expected.indices) {
            assertEquals("${names[i]}: got ${got[i] / 60}:${"%02d".format(got[i] % 60)}", expected[i].toDouble(), got[i].toDouble(), 2.0)
        }
    }

    /** Every region, on the 4 key dates of the year: all six times exist and are in the right order. */
    @Test fun allRegionsGiveOrderedTimesAllYear() {
        val dates = listOf(LocalDate.of(2026, 3, 20), LocalDate.of(2026, 6, 21), LocalDate.of(2026, 9, 23), LocalDate.of(2026, 12, 21))
        for (r in Regions.all) for (d in dates) {
            val t = hoursFor(r, d)
            assertTrue("${r.nameEn} $d has NaN: $t", t.none { it.isNaN() })
            for (i in 0 until 5) assertTrue("${r.nameEn} $d not ordered: $t", t[i] < t[i + 1])
        }
    }

    /** Makkah, Umm al-Qura: Isha is exactly 90 minutes after Maghrib. */
    @Test fun ummAlQuraIshaIs90MinutesAfterMaghrib() {
        val makkah = Regions.all.first { it.nameEn.startsWith("Makkah") }
        val t = hoursFor(makkah, LocalDate.of(2026, 10, 23))
        assertEquals(90.0, (t[5] - t[4]) * 60, 0.5)
    }

    @Test fun regionsHaveBothNames() {
        for (r in Regions.all) assertTrue(r.nameAr.isNotBlank() && r.nameEn.isNotBlank() && r.nameEn != r.nameAr)
    }
}
