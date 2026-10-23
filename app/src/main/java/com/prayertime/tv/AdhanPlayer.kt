package com.prayertime.tv

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.Handler
import android.os.Looper

/**
 * Plays res/raw/adhan.(mp3|ogg|wav) if the file was added to the project,
 * otherwise falls back to a short chime so there is always a sound.
 */
class AdhanPlayer(private val ctx: Context) {
    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    fun play(resName: String = "adhan") {
        stop()
        playing = true
        val id = ctx.resources.getIdentifier(resName, "raw", ctx.packageName)
        if (id != 0) {
            try {
                player = MediaPlayer.create(ctx, id)?.apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
                    )
                    setOnCompletionListener { stop() }
                    start()
                }
                if (player != null) { limitLength(); return }
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
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
                )
                setDataSource(ctx, uri)
                setOnCompletionListener { stop() }
                prepare()
                start()
            }
            limitLength()
            return
        } catch (_: Exception) {
            player?.release(); player = null
        }
        chime()
    }

    /** Stops the adhan after the length chosen in settings (pref azanLen, seconds; 0 = full adhan). */
    private fun limitLength() {
        val sec = ctx.getSharedPreferences("prayer", Context.MODE_PRIVATE).getInt("azanLen", 0)
        if (sec > 0) handler.postDelayed({ stop() }, sec * 1000L)
    }

    private fun chime() {
        val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        for (i in 0 until 4) {
            handler.postDelayed({ tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 600) }, i * 1200L)
        }
        handler.postDelayed({ tone.release(); playing = false }, 6000L)
    }

    val isPlaying get() = playing
    private var playing = false

    fun stop() {
        playing = false
        handler.removeCallbacksAndMessages(null)
        player?.let { try { it.stop() } catch (_: Exception) {}; it.release() }
        player = null
    }
}
