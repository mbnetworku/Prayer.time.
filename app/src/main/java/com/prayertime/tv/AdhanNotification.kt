package com.prayertime.tv

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.util.Locale

/** Notification shown while the adhan plays: Pause/Resume and Stop buttons (works from the notification bar and lock screen). */
object AdhanNotification {
    private const val CHANNEL = "adhan"
    const val ID = 1001
    const val ACTION_PAUSE = "com.prayertime.tv.ADHAN_PAUSE"
    const val ACTION_RESUME = "com.prayertime.tv.ADHAN_RESUME"
    const val ACTION_STOP = "com.prayertime.tv.ADHAN_STOP"

    private val namesAr = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
    private val namesEn = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")

    private val green = android.graphics.Color.parseColor("#30D158")

    /** Bilingual text for the prayer whose time just came (from the last adhan key "yyyy-MM-dd-index"). */
    private class Info(val title: String, val textAr: String, val textEn: String, val time: String)

    private fun prayerInfo(ctx: Context): Info? {
        val p = ctx.getSharedPreferences("prayer", Context.MODE_PRIVATE)
        val key = p.getString("lastAdhanKey", "") ?: ""
        val k = key.substringAfterLast('-').toIntOrNull() ?: return null
        val ar = namesAr.getOrNull(k) ?: return null
        val en = namesEn.getOrNull(k) ?: return null
        val time = try {
            val h = Times.hours(ctx, java.time.LocalDate.parse(key.substring(0, 10)))[k]
            formatTime(p, h)
        } catch (_: Exception) { "" }
        return Info("$ar  •  $en", "حان وقت صلاة $ar", "It's time for $en prayer", time)
    }

    private fun formatTime(p: android.content.SharedPreferences, h: Double): String {
        if (h.isNaN()) return ""
        val tot = (Math.round(h * 60).toInt() % 1440 + 1440) % 1440
        var hh = tot / 60
        val mm = tot % 60
        if (p.getBoolean("h24", false)) return String.format(Locale.US, "%02d:%02d", hh, mm)
        val am = hh < 12
        hh %= 12; if (hh == 0) hh = 12
        val arabic = Locale.getDefault().language == "ar"
        val period = when (p.getString("ampm", "short")) {
            "none" -> ""
            "full" -> if (arabic) (if (am) " صباحاً" else " مساءً") else (if (am) " AM" else " PM")
            else -> if (arabic) (if (am) " ص" else " م") else (if (am) " AM" else " PM")
        }
        return String.format(Locale.US, "%02d:%02d", hh, mm) + period
    }

    /** A plain notification (message + default notification sound) for when the adhan sound is off. */
    @Suppress("DEPRECATION")
    fun remind(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ar = Locale.getDefault().language == "ar"
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel("reminder", if (ar) "تذكير بالصلاة" else "Prayer reminder",
                NotificationManager.IMPORTANCE_DEFAULT))
        }
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), flags())
        val info = prayerInfo(ctx)
        val title = info?.let { if (it.time.isNotEmpty()) it.title + "  —  " + it.time else it.title }
            ?: if (ar) "مواقيت الصلاة" else "Prayer Times"
        val body = info?.let { it.textAr + "\n" + it.textEn } ?: "حان وقت الصلاة\nIt's prayer time"
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(ctx, "reminder") else Notification.Builder(ctx)
        b.setSmallIcon(R.drawable.ic_stat_adhan)
            .setColor(green)
            .setContentTitle(title)
            .setContentText(body.replace("\n", " · "))
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(open)
        nm.notify(1002, b.build())
    }

    private fun flags() = PendingIntent.FLAG_UPDATE_CURRENT or
        (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)

    fun show(ctx: Context, paused: Boolean) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(ID, build(ctx, paused))
    }

    @Suppress("DEPRECATION")
    fun build(ctx: Context, paused: Boolean): Notification {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ar = Locale.getDefault().language == "ar"
        if (Build.VERSION.SDK_INT >= 26) {
            // Low importance: the notification itself makes no sound (the adhan is already playing).
            nm.createNotificationChannel(NotificationChannel(CHANNEL, if (ar) "الأذان" else "Adhan",
                NotificationManager.IMPORTANCE_LOW))
        }
        fun action(a: String) = PendingIntent.getBroadcast(ctx, a.hashCode(),
            Intent(ctx, AdhanActionReceiver::class.java).setAction(a), flags())
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), flags())

        val info = prayerInfo(ctx)
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(ctx, CHANNEL) else Notification.Builder(ctx)
        b.setSmallIcon(R.drawable.ic_stat_adhan)
            .setColor(green)
            .setContentTitle(info?.title ?: "الأذان  •  Adhan")
            .setContentText(when {
                paused -> "متوقف مؤقتاً · Paused"
                info != null -> info.textAr + " · " + info.textEn
                else -> "يُرفع الأذان الآن · Adhan is playing"
            })
            .setSubText(info?.time?.takeIf { it.isNotEmpty() })
            .setOngoing(!paused)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .setDeleteIntent(action(ACTION_STOP))
            .addAction(
                if (paused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (paused) (if (ar) "استئناف" else "Resume") else (if (ar) "إيقاف مؤقت" else "Pause"),
                action(if (paused) ACTION_RESUME else ACTION_PAUSE))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, if (ar) "إيقاف" else "Stop", action(ACTION_STOP))
            .setStyle(Notification.MediaStyle().setShowActionsInCompactView(0, 1))
        return b.build()
    }

    fun cancel(ctx: Context) {
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(ID)
    }
}

/** Handles the notification buttons. */
class AdhanActionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val p = AdhanPlayer.get(ctx)
        when (intent.action) {
            AdhanNotification.ACTION_PAUSE -> p.pause()
            AdhanNotification.ACTION_RESUME -> p.resume()
            AdhanNotification.ACTION_STOP -> p.stop()
        }
    }
}
