package com.prayertime.tv

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/**
 * Plays res/raw/adhan.(mp3|ogg|wav) if the file was added to the project,
 * otherwise falls back to a short chime so there is always a sound.
 */
class AdhanPlayer(private val ctx: Context) {
    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    fun play() {
        stop()
        playing = true
        val id = ctx.resources.getIdentifier("adhan", "raw", ctx.packageName)
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
                if (player != null) return
            } catch (_: Exception) { }
        }
        chime()
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
