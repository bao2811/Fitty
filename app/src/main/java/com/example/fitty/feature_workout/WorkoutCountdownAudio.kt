package com.example.fitty.feature_workout

import android.media.AudioManager
import android.media.ToneGenerator

internal object WorkoutCountdownSignal {
    fun shouldPlayTick(elapsedSeconds: Int, requiredSeconds: Int): Boolean {
        if (elapsedSeconds <= 0 || elapsedSeconds > requiredSeconds) return false
        return elapsedSeconds % 3 == 0
    }
}

internal class WorkoutCountdownAudioPlayer {
    private var toneGenerator: ToneGenerator? = null

    fun playTick() {
        runCatching {
            val generator = toneGenerator ?: ToneGenerator(AudioManager.STREAM_MUSIC, 80)
                .also { toneGenerator = it }
            generator.startTone(ToneGenerator.TONE_PROP_BEEP, 180)
        }
    }

    fun release() {
        runCatching {
            toneGenerator?.release()
            toneGenerator = null
        }
    }
}
