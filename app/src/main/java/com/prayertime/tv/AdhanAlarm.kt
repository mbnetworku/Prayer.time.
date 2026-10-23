package com.prayertime.tv

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone

/** Prayer times shared by the screen, the widget and the background alarms. */
object Times {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("prayer", Context.MODE_PRIVATE)

    fun region(ctx: Context) = Regions.all[prefs(ctx).getInt("region", 0).coerceIn(0, Regions.all.size - 1)]

    /** Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha as local clock hours, including manual adjustments. */
    fun hours(ctx: Context, d: LocalDate): List<Double> {
        val r = region(ctx)
        val zone = ZoneId.of(r.zone)
        val off = TimeZone.getTimeZone(r.zone).getOffset(d.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()) / 3600000.0
        val p = prefs(ctx)
        return PrayerCalc.compute(d, r.lat, r.lng, off, r.method).asList()
            .mapIndexed { i, h -> h + p.getInt("adj_$i", 0) / 60.0 }
    }

    fun at(ctx: Context, d: LocalDate, h: Double): ZonedDateTime =
        d.atStartOfDay(ZoneId.of(region(ctx).zone)).plusSeconds((h * 3600).toLong())

    /**
     * Each adhan plays once even if both the open screen and the background alarm reach it.
     * key = "date-prayerIndex"; returns true for the first caller.
     */
    @Synchronized fun claim(ctx: Context, key: String): Boolean {
        val p = prefs(ctx)
        if (key.isEmpty() || p.getString("lastAdhanKey", "") == key) return false
        p.edit().putString("lastAdhanKey", key).putLong("lastAdhanAt", System.currentTimeMillis()).commit()
        return true
    }
}

/** Schedules an exact alarm for the next adhan (Fajr, Dhuhr, Asr, Maghrib, Isha). */
object AdhanScheduler {
    const val ACTION_FIRE = "com.prayertime.tv.ADHAN_FIRE"
    private val prayers = listOf(0, 2, 3, 4, 5) // no adhan at sunrise

    private fun pending(ctx: Context) = PendingIntent.getBroadcast(ctx, 77,
        Intent(ctx, AdhanAlarmReceiver::class.java).setAction(ACTION_FIRE),
        PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0))

    fun enabled(ctx: Context): Boolean {
        val p = ctx.getSharedPreferences("prayer", Context.MODE_PRIVATE)
        return p.getBoolean("bgAdhan", true) && p.getBoolean("sound", true)
    }

    /** False only on Android 12+ when the user has turned off "Alarms & reminders" for the app. */
    fun exactAllowed(ctx: Context): Boolean {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
    }

    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(ctx)
        am.cancel(pi)
        if (!enabled(ctx)) return
        val now = ZonedDateTime.now(ZoneId.of(Times.region(ctx).zone))
        for (day in 0..1) {
            val d = now.toLocalDate().plusDays(day.toLong())
            val hs = Times.hours(ctx, d)
            for (k in prayers) {
                if (hs[k].isNaN()) continue
                val t = Times.at(ctx, d, hs[k])
                if (!t.isAfter(now)) continue
                val millis = t.toInstant().toEpochMilli()
                ctx.getSharedPreferences("prayer", Context.MODE_PRIVATE).edit()
                    .putString("nextAdhanKey", "$d-$k").putLong("nextAdhanAt", millis).apply()
                when {
                    !exactAllowed(ctx) -> am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
                    Build.VERSION.SDK_INT >= 23 -> am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
                    else -> am.setExact(AlarmManager.RTC_WAKEUP, millis, pi)
                }
                return
            }
        }
    }
}

/** Alarm fired: play the adhan through the foreground service, then schedule the next one. */
class AdhanAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == AdhanScheduler.ACTION_FIRE && AdhanScheduler.enabled(ctx)) {
            val p = ctx.getSharedPreferences("prayer", Context.MODE_PRIVATE)
            val late = System.currentTimeMillis() - p.getLong("nextAdhanAt", 0) > 10 * 60_000 // device was off
            if (!late && Times.claim(ctx, p.getString("nextAdhanKey", "") ?: "")) {
                val svc = Intent(ctx, AdhanService::class.java)
                try {
                    if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(svc) else ctx.startService(svc)
                } catch (_: Exception) {
                    AdhanPlayer.get(ctx).playSelected() // service not allowed: play directly
                }
            }
        }
        AdhanScheduler.schedule(ctx)
    }
}

/** Re-schedules after reboot, app update or a clock / time-zone change. */
class AdhanBootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = AdhanScheduler.schedule(ctx)
}

/** Keeps the app alive while the adhan plays in the background (screen off / app closed). */
class AdhanService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val n = AdhanNotification.build(this, paused = false)
        if (Build.VERSION.SDK_INT >= 29) startForeground(AdhanNotification.ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else startForeground(AdhanNotification.ID, n)
        val player = AdhanPlayer.get(this)
        player.onStop = null
        player.playSelected()
        player.onStop = { finish() }
        if (!player.isPlaying) finish()
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun finish() {
        AdhanPlayer.get(this).onStop = null
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE) else stopForeground(true)
        stopSelf()
    }
}
