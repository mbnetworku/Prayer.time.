package com.prayertime.tv

import java.time.LocalDate
import kotlin.math.*

/** Calculation method: Fajr angle, and Isha either as an angle or minutes after Maghrib. */
data class Method(val fajr: Double, val isha: Double, val ishaMinutes: Int = 0, val asrFactor: Int = 1)

object Methods {
    val UMM_AL_QURA = Method(18.5, 0.0, ishaMinutes = 90)
    val QATAR = Method(18.0, 0.0, ishaMinutes = 90)
    val MWL = Method(18.0, 17.0)
    val EGYPT = Method(19.5, 17.5)
    val KARACHI = Method(18.0, 18.0, asrFactor = 2)
    val ISNA = Method(15.0, 15.0)
    val GULF = Method(18.0, 17.5)
    val UAE = Method(18.2, 18.2)
    val MOROCCO = Method(19.0, 17.0)
    val TURKEY = Method(18.0, 17.0)
    val SEA = Method(20.0, 18.0)
}

/** Times as local clock hours (e.g. 13.5 = 13:30); NaN if not computable. */
class DayTimes(val fajr: Double, val sunrise: Double, val dhuhr: Double,
               val asr: Double, val maghrib: Double, val isha: Double) {
    fun asList() = listOf(fajr, sunrise, dhuhr, asr, maghrib, isha)
}

/** Port of the well-known praytimes.org algorithm. */
object PrayerCalc {
    private fun rad(d: Double) = Math.toRadians(d)
    private fun deg(r: Double) = Math.toDegrees(r)
    private fun fix(a: Double, b: Double) = a - b * floor(a / b)

    private fun julian(year: Int, month: Int, day: Int): Double {
        var y = year; var m = month
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunPosition(jd: Double): Pair<Double, Double> { // declination, equation of time
        val d = jd - 2451545.0
        val g = fix(357.529 + 0.98560028 * d, 360.0)
        val q = fix(280.459 + 0.98564736 * d, 360.0)
        val l = fix(q + 1.915 * sin(rad(g)) + 0.020 * sin(rad(2 * g)), 360.0)
        val e = 23.439 - 0.00000036 * d
        val ra = fix(deg(atan2(cos(rad(e)) * sin(rad(l)), cos(rad(l)))) / 15, 24.0)
        return Pair(deg(asin(sin(rad(e)) * sin(rad(l)))), q / 15 - ra)
    }

    private fun midDay(jd: Double, t: Double) = fix(12 - sunPosition(jd + t).second, 24.0)

    private fun sunAngleTime(jd: Double, lat: Double, angle: Double, t: Double, ccw: Boolean): Double {
        val decl = sunPosition(jd + t).first
        val noon = midDay(jd, t)
        val x = (-sin(rad(angle)) - sin(rad(decl)) * sin(rad(lat))) / (cos(rad(decl)) * cos(rad(lat)))
        if (x < -1 || x > 1) return Double.NaN
        val a = deg(acos(x)) / 15
        return noon + if (ccw) -a else a
    }

    private fun asrTime(jd: Double, lat: Double, factor: Int, t: Double): Double {
        val decl = sunPosition(jd + t).first
        val angle = -deg(atan(1.0 / (factor + tan(rad(abs(lat - decl))))))
        return sunAngleTime(jd, lat, angle, t, false)
    }

    fun compute(date: LocalDate, lat: Double, lng: Double, tzHours: Double, m: Method): DayTimes {
        val jd = julian(date.year, date.monthValue, date.dayOfMonth) - lng / (15 * 24)
        var fajr = 5.0; var sunrise = 6.0; var dhuhr = 12.0
        var asr = 13.0; var sunset = 18.0; var isha = 18.0
        repeat(2) {
            fajr = sunAngleTime(jd, lat, m.fajr, fajr / 24, true)
            sunrise = sunAngleTime(jd, lat, 0.833, sunrise / 24, true)
            dhuhr = midDay(jd, dhuhr / 24)
            asr = asrTime(jd, lat, m.asrFactor, asr / 24)
            sunset = sunAngleTime(jd, lat, 0.833, sunset / 24, false)
            isha = if (m.ishaMinutes > 0) 18.0 else sunAngleTime(jd, lat, m.isha, isha / 24, false)
            if (fajr.isNaN()) fajr = 5.0
            if (isha.isNaN()) isha = 18.0
        }
        val shift = tzHours - lng / 15
        val sr = sunrise + shift
        val ss = sunset + shift
        var f = fajr + shift
        var i = if (m.ishaMinutes > 0) ss + m.ishaMinutes / 60.0 else isha + shift
        // High-latitude fallback when the sun never reaches the twilight angle.
        if (sunAngleTime(jd, lat, m.fajr, 5.0 / 24, true).isNaN()) f = sr - 1.5
        if (m.ishaMinutes == 0 && sunAngleTime(jd, lat, m.isha, 18.0 / 24, false).isNaN()) i = ss + 1.5
        return DayTimes(f, sr, dhuhr + shift, asr + shift, ss, i)
    }
}
