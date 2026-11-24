package com.example.myapplication.tetris

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.sin

class SoundManager {
    private val executor = Executors.newSingleThreadExecutor()
    var isMuted = false

    fun playMove() {
        if (isMuted) return
        executor.execute {
            playTone(440.0, 30, 0.15)
        }
    }

    fun playRotate() {
        if (isMuted) return
        executor.execute {
            playTone(587.33, 40, 0.2)
        }
    }

    fun playHold() {
        if (isMuted) return
        executor.execute {
            playTone(523.25, 50, 0.2)
        }
    }

    fun playHardDrop() {
        if (isMuted) return
        executor.execute {
            playToneArpeggio(doubleArrayOf(300.0, 150.0), intArrayOf(30, 40), 0.3)
        }
    }

    fun playLineClear(linesCount: Int) {
        if (isMuted) return
        executor.execute {
            val baseFreq = if (linesCount >= 4) 523.25 else 440.0 // C5 for Tetris!
            val freqs = if (linesCount >= 4) {
                doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // C-E-G-C (Tetris!)
            } else {
                doubleArrayOf(baseFreq, baseFreq * 1.25, baseFreq * 1.5)
            }
            val durations = IntArray(freqs.size) { 60 }
            playToneArpeggio(freqs, durations, 0.4)
        }
    }

    fun playGameOver() {
        if (isMuted) return
        executor.execute {
            playToneArpeggio(
                doubleArrayOf(440.0, 392.0, 349.23, 293.66, 220.0),
                intArrayOf(100, 100, 100, 120, 250),
                0.35
            )
        }
    }

    private fun playTone(freq: Double, durationMs: Int, volume: Double) {
        try {
            val sampleRate = 22050
            val numSamples = (sampleRate * durationMs / 1000.0).toInt()
            if (numSamples <= 0) return
            val sample = ByteArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = 1.0 - (i.toDouble() / numSamples) // Fade out
                val wave = sin(2.0 * Math.PI * freq * t)
                val squareWave = if (wave > 0) 1.0 else -1.0 // Retro 8-bit square wave
                val value = (squareWave * volume * envelope * 127).toInt().coerceIn(-128, 127)
                sample[i] = value.toByte()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_8BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(numSamples)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(sample, 0, sample.size)
            audioTrack.play()
            Thread.sleep(durationMs.toLong() + 20)
            audioTrack.release()
        } catch (_: Exception) {
            // Ignore audio track initialization errors on some restrictive devices
        }
    }

    private fun playToneArpeggio(freqs: DoubleArray, durations: IntArray, volume: Double) {
        for (i in freqs.indices) {
            playTone(freqs[i], durations[i], volume)
        }
    }

    fun release() {
        executor.shutdown()
    }
}
