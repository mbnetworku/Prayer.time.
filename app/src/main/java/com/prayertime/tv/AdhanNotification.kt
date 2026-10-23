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
    private const val ID = 1001
    const val ACTION_PAUSE = "com.prayertime.tv.ADHAN_PAUSE"
    const val ACTION_RESUME = "com.prayertime.tv.ADHAN_RESUME"
    const val ACTION_STOP = "com.prayertime.tv.ADHAN_STOP"

    private fun flags() = PendingIntent.FLAG_UPDATE_CURRENT or
        (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)

    @Suppress("DEPRECATION")
    fun show(ctx: Context, paused: Boolean) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
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

        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(ctx, CHANNEL) else Notification.Builder(ctx)
        b.setSmallIcon(R.drawable.ic_stat_adhan)
            .setContentTitle(if (ar) "الأذان" else "Adhan")
            .setContentText(when {
                paused && ar -> "متوقف مؤقتاً"
                paused -> "Paused"
                ar -> "يُرفع الأذان الآن"
                else -> "Adhan is playing"
            })
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
        nm.notify(ID, b.build())
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
