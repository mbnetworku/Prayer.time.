package com.prayertime.tv

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock

/**
 * One adhan player for the whole app (screens and notification buttons share it).
 * Plays res/raw/adhan* or a file chosen by the user; falls back to a short chime.
 * While a recording plays, a notification offers Pause/Resume and Stop.
 */
// Safe: only the application context is kept (no Activity), so nothing leaks.
@android.annotation.SuppressLint("StaticFieldLeak")
class AdhanPlayer private constructor(private val ctx: Context) {
    companion object {
        @Volatile private var instance: AdhanPlayer? = null
        fun get(ctx: Context): AdhanPlayer = instance ?: synchronized(this) {
            instance ?: AdhanPlayer(ctx.applicationContext).also { instance = it }
        }
    }

    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var playing = false
    private val limitStop = Runnable { stop() }
    private var limitEnd = 0L   // elapsedRealtime when the length limit stops the adhan (0 = no limit)
    private var limitLeft = 0L  // remaining limit while paused

    /** Called once when the adhan stops (used by the background service). */
    var onStop: (() -> Unit)? = null

    val isPlaying get() = playing
    var isPaused = false
        private set

    /** Alarm volume by default: audible even if media volume is 0 and allowed through Do Not Disturb "alarms". */
    private fun attributes(): AudioAttributes {
        val alarm = ctx.getSharedPreferences("prayer", Context.MODE_PRIVATE).getBoolean("azanAlarm", true)
        return AudioAttributes.Builder()
            .setUsage(if (alarm) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    }

    fun play(resName: String = "adhan") {
        stop()
        playing = true
        val id = ctx.resources.getIdentifier(resName, "raw", ctx.packageName)
        if (id != 0) {
            try {
                // Attributes must be given at creation (MediaPlayer is already prepared afterwards).
                player = MediaPlayer.create(ctx, id, attributes(), AudioManager.AUDIO_SESSION_ID_GENERATE)?.apply {
                    try { setWakeMode(ctx, PowerManager.PARTIAL_WAKE_LOCK) } catch (_: Exception) { }
                    setOnCompletionListener { stop() }
                    start()
                }
                if (player != null) { started(); return }
            } catch (_: Exception) { }
        }
        chime()
    }

    /** Plays the adhan chosen in settings (built-in file or a file from the device). */
    fun playSelected() {
        val p = ctx.getSharedPreferences("prayer", Context.MODE_PRIVATE)
        val choice = p.getString("azan", "adhan") ?: "adhan"
        val uri = p.getString("azanUri", "") ?: ""
        if (choice == "uri" && uri.isNotEmpty()) playUri(Uri.parse(uri))
        else play(if (choice in Config.azanOptions()) choice else "adhan")
    }

    /** Plays an audio file chosen by the user (content:// uri). */
    fun playUri(uri: Uri) {
        stop()
        playing = true
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(attributes())
                setWakeMode(ctx, PowerManager.PARTIAL_WAKE_LOCK)
                setDataSource(ctx, uri)
                setOnCompletionListener { stop() }
                prepare()
                start()
            }
            started()
            return
        } catch (_: Exception) {
            player?.release(); player = null
        }
        chime()
    }

    /** Length limit (pref azanLen, seconds; 0 = full adhan) + notification with controls. */
    private fun started() {
        val sec = ctx.getSharedPreferences("prayer", Context.MODE_PRIVATE).getInt("azanLen", 0)
        if (sec > 0) {
            limitEnd = SystemClock.elapsedRealtime() + sec * 1000L
            handler.postDelayed(limitStop, sec * 1000L)
        }
        AdhanNotification.show(ctx, paused = false)
    }

    fun pause() {
        val p = player
        if (p == null || !playing) { AdhanNotification.cancel(ctx); return }
        if (isPaused) return
        try { p.pause() } catch (_: Exception) { }
        isPaused = true
        if (limitEnd > 0) { limitLeft = limitEnd - SystemClock.elapsedRealtime(); handler.removeCallbacks(limitStop) }
        AdhanNotification.show(ctx, paused = true)
    }

    fun resume() {
        val p = player
        if (p == null || !playing) { AdhanNotification.cancel(ctx); return }
        if (!isPaused) return
        try { p.start() } catch (_: Exception) { stop(); return }
        isPaused = false
        if (limitEnd > 0) {
            limitEnd = SystemClock.elapsedRealtime() + limitLeft
            handler.postDelayed(limitStop, limitLeft.coerceAtLeast(0))
        }
        AdhanNotification.show(ctx, paused = false)
    }

    fun togglePause() = if (isPaused) resume() else pause()

    private fun chime() {
        val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        for (i in 0 until 4) {
            handler.postDelayed({ tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 600) }, i * 1200L)
        }
        handler.postDelayed({ tone.release(); stop() }, 6000L)
    }

    fun stop() {
        playing = false
        isPaused = false
        limitEnd = 0L
        handler.removeCallbacksAndMessages(null)
        player?.let { try { it.stop() } catch (_: Exception) {}; it.release() }
        player = null
        AdhanNotification.cancel(ctx)
        onStop?.let { onStop = null; it() }
    }
}
