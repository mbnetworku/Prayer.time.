package com.prayertime.tv

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import java.util.TimeZone

/** Home-screen widget (phones/tablets): next prayer and today's six times. Refreshes at each prayer time. */
class PrayerWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) = update(ctx, mgr, ids)

    companion object {
        private val namesAr = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
        private val namesEn = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")
        private val ids = listOf(
            R.id.n0 to R.id.t0, R.id.n1 to R.id.t1, R.id.n2 to R.id.t2,
            R.id.n3 to R.id.t3, R.id.n4 to R.id.t4, R.id.n5 to R.id.t5)

        /** Called by the app after settings change, and by the alarm at each prayer time. */
        fun updateAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val widgetIds = mgr.getAppWidgetIds(ComponentName(ctx, PrayerWidget::class.java))
            if (widgetIds.isNotEmpty()) update(ctx, mgr, widgetIds)
        }

        private fun flags() = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)

        private fun update(ctx: Context, mgr: AppWidgetManager, widgetIds: IntArray) {
            val prefs = ctx.getSharedPreferences("prayer", Context.MODE_PRIVATE)
            val r = Regions.all[prefs.getInt("region", 0).coerceIn(0, Regions.all.size - 1)]
            val zone = ZoneId.of(r.zone)
            val now = ZonedDateTime.now(zone)
            val ar = Locale.getDefault().language == "ar"
            val use24 = prefs.getBoolean("h24", false)

            fun hours(d: LocalDate): List<Double> {
                val off = TimeZone.getTimeZone(r.zone).getOffset(
                    d.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()) / 3600000.0
                return PrayerCalc.compute(d, r.lat, r.lng, off, r.method).asList()
                    .mapIndexed { i, h -> h + prefs.getInt("adj_$i", 0) / 60.0 }
            }
            fun at(d: LocalDate, h: Double) = d.atStartOfDay(zone).plusSeconds((h * 3600).toLong())
            fun hm(h: Double): String {
                if (h.isNaN()) return "--:--"
                val tot = Math.round(h * 60).toInt().let { (it % 1440 + 1440) % 1440 }
                var hh = tot / 60
                if (!use24) { hh %= 12; if (hh == 0) hh = 12 }
                return String.format(Locale.US, "%02d:%02d", hh, tot % 60)
            }

            fun period(h: Double): String {
                if (use24 || h.isNaN() || prefs.getString("ampm", "short") == "none") return ""
                val am = ((Math.round(h * 60).toInt() % 1440 + 1440) % 1440) < 720
                return if (ar) (if (am) " ص" else " م") else (if (am) " AM" else " PM")
            }

            val today = now.toLocalDate()
            val t = hours(today)
            var next = t.indices.firstOrNull { !t[it].isNaN() && at(today, t[it]).isAfter(now) }
            val nextAt = if (next != null) at(today, t[next]) else at(today.plusDays(1), hours(today.plusDays(1))[0])
            val nextHours = if (next != null) t[next] else hours(today.plusDays(1))[0]
            if (next == null) next = 0
            val names = if (ar) namesAr else namesEn

            val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), flags())
            for (id in widgetIds) {
                val v = RemoteViews(ctx.packageName, R.layout.widget_prayer)
                v.setTextViewText(R.id.city, if (ar) r.nameAr else r.nameEn)
                v.setTextViewText(R.id.next, (if (ar) "القادمة: " else "Next: ") + names[next] + "  " + hm(nextHours) + period(nextHours))
                ids.forEachIndexed { i, (n, tv) ->
                    v.setTextViewText(n, names[i]); v.setTextViewText(tv, hm(t[i]))
                }
                v.setOnClickPendingIntent(R.id.widget_root, open)
                mgr.updateAppWidget(id, v)
            }

            // Refresh right after the next prayer so "Next" moves on.
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val tick = PendingIntent.getBroadcast(ctx, 1,
                Intent(ctx, PrayerWidget::class.java).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds), flags())
            am.set(AlarmManager.RTC, nextAt.toInstant().toEpochMilli() + 60_000, tick)
        }
    }
}
