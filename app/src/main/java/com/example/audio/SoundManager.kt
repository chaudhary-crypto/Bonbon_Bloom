package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * High-performance, zero-latency procedural sound and music synthesizer.
 * Generates sweet, casual match-3 sound effects and cheerful looping background music
 * directly using native Android AudioTrack without requiring any heavy external MP3 assets.
 */
object SoundManager {
    private val scope = CoroutineScope(Dispatchers.Default)

    var soundEnabled: Boolean = true
    var musicEnabled: Boolean = true
        set(value) {
            field = value
            if (value) {
                startMusic()
            } else {
                stopMusic()
            }
        }

    private var musicJob: Job? = null
    private var musicTrack: AudioTrack? = null

    private const val SAMPLE_RATE = 22050

    fun playSwap() {
        if (!soundEnabled) return
        scope.launch {
            // Sweet swoosh sound: quick pitch glide up
            val durationMs = 70
            val numSamples = (SAMPLE_RATE * durationMs / 1000)
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val freq = 350.0 + (550.0 - 350.0) * (i.toDouble() / numSamples)
                val envelope = sin(PI * (i.toDouble() / numSamples))
                val sample = (sin(2 * PI * freq * t) * envelope * 24000).toInt().toShort()
                buffer[i] = sample
            }
            playPcmBuffer(buffer)
        }
    }

    fun playInvalidSwap() {
        if (!soundEnabled) return
        scope.launch {
            // Soft double thud
            val durationMs = 120
            val numSamples = (SAMPLE_RATE * durationMs / 1000)
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val freq = 140.0 - 50.0 * (i.toDouble() / numSamples)
                val envelope = (1.0 - (i.toDouble() / numSamples))
                val sample = (sin(2 * PI * freq * t) * envelope * 20000).toInt().toShort()
                buffer[i] = sample
            }
            playPcmBuffer(buffer)
        }
    }

    /**
     * Ascending chime tones for cascade combos:
     * C5, D5, E5, G5, A5, C6, D6, E6
     */
    fun playMatch(comboLevel: Int = 1) {
        if (!soundEnabled) return
        scope.launch {
            val baseFrequencies = listOf(523.25, 587.33, 659.25, 783.99, 880.00, 1046.50, 1174.66, 1318.51)
            val index = (comboLevel - 1).coerceIn(0, baseFrequencies.size - 1)
            val freq = baseFrequencies[index]

            val durationMs = 180
            val numSamples = (SAMPLE_RATE * durationMs / 1000)
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / numSamples
                // Exponential decay envelope
                val envelope = (1.0 - progress) * (1.0 - progress)
                // Fundamental + harmonic for sweet marimba bell tone
                val wave = sin(2 * PI * freq * t) + 0.4 * sin(2 * PI * freq * 2 * t) + 0.15 * sin(2 * PI * freq * 3 * t)
                val sample = (wave * envelope * 18000).toInt().coerceIn(-32767, 32767).toShort()
                buffer[i] = sample
            }
            playPcmBuffer(buffer)
        }
    }

    fun playRocketBlast() {
        if (!soundEnabled) return
        scope.launch {
            // Whoosh laser rocket blast
            val durationMs = 280
            val numSamples = (SAMPLE_RATE * durationMs / 1000)
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / numSamples
                val freq = 800.0 - 600.0 * progress
                val noise = (Random.nextFloat() * 2 - 1) * 0.35f
                val envelope = sin(PI * progress)
                val sample = ((sin(2 * PI * freq * t) * 0.65 + noise) * envelope * 26000).toInt().coerceIn(-32767, 32767).toShort()
                buffer[i] = sample
            }
            playPcmBuffer(buffer)
        }
    }

    fun playBombExplosion() {
        if (!soundEnabled) return
        scope.launch {
            // Booming deep impact
            val durationMs = 350
            val numSamples = (SAMPLE_RATE * durationMs / 1000)
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / numSamples
                val freq = 160.0 - 110.0 * progress
                val noise = (Random.nextFloat() * 2 - 1) * 0.5f * (1.0 - progress)
                val envelope = (1.0 - progress)
                val sample = ((sin(2 * PI * freq * t) * 0.5 + noise) * envelope * 28000).toInt().coerceIn(-32767, 32767).toShort()
                buffer[i] = sample
            }
            playPcmBuffer(buffer)
        }
    }

    fun playRainbowBlast() {
        if (!soundEnabled) return
        scope.launch {
            // Sparkling rainbow magic sweep
            val durationMs = 450
            val numSamples = (SAMPLE_RATE * durationMs / 1000)
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / numSamples
                val freq = 600.0 + 1200.0 * progress
                val sparkle = sin(2 * PI * freq * 1.5 * t) * 0.3
                val envelope = sin(PI * progress)
                val sample = ((sin(2 * PI * freq * t) + sparkle) * envelope * 20000).toInt().coerceIn(-32767, 32767).toShort()
                buffer[i] = sample
            }
            playPcmBuffer(buffer)
        }
    }

    fun playComboFanfare(comboLevel: Int) {
        if (!soundEnabled) return
        scope.launch {
            // 2-tone celebratory fanfare
            val chordNotes = when (comboLevel) {
                2 -> listOf(659.25, 783.99)       // E5 -> G5
                3 -> listOf(783.99, 987.77)       // G5 -> B5
                4 -> listOf(880.00, 1174.66)      // A5 -> D6
                else -> listOf(1046.50, 1318.51)  // C6 -> E6
            }
            val noteDuration = 120
            val numSamples = (SAMPLE_RATE * noteDuration * chordNotes.size / 1000)
            val buffer = ShortArray(numSamples)

            chordNotes.forEachIndexed { noteIndex, freq ->
                val startIdx = noteIndex * (SAMPLE_RATE * noteDuration / 1000)
                val noteLen = SAMPLE_RATE * noteDuration / 1000
                for (i in 0 until noteLen) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val progress = i.toDouble() / noteLen
                    val envelope = (1.0 - progress)
                    val sample = (sin(2 * PI * freq * t) * envelope * 22000).toInt().toShort()
                    val targetIdx = startIdx + i
                    if (targetIdx < buffer.size) {
                        buffer[targetIdx] = sample
                    }
                }
            }
            playPcmBuffer(buffer)
        }
    }

    fun playLevelWin() {
        if (!soundEnabled) return
        scope.launch {
            // Joyous 5-note arpeggio (C - E - G - C - E)
            val notes = listOf(523.25, 659.25, 783.99, 1046.50, 1318.51)
            val noteDurationMs = 130
            val totalSamples = (SAMPLE_RATE * noteDurationMs * notes.size / 1000)
            val buffer = ShortArray(totalSamples)

            notes.forEachIndexed { noteIndex, freq ->
                val startIdx = noteIndex * (SAMPLE_RATE * noteDurationMs / 1000)
                val len = SAMPLE_RATE * noteDurationMs / 1000
                for (i in 0 until len) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val progress = i.toDouble() / len
                    val envelope = (1.0 - progress)
                    val sample = (sin(2 * PI * freq * t) * envelope * 24000).toInt().toShort()
                    val targetIdx = startIdx + i
                    if (targetIdx < buffer.size) {
                        buffer[targetIdx] = sample
                    }
                }
            }
            playPcmBuffer(buffer)
        }
    }

    fun playLevelFail() {
        if (!soundEnabled) return
        scope.launch {
            val notes = listOf(440.0, 392.0, 349.23, 293.66)
            val noteDurationMs = 150
            val totalSamples = (SAMPLE_RATE * noteDurationMs * notes.size / 1000)
            val buffer = ShortArray(totalSamples)

            notes.forEachIndexed { noteIndex, freq ->
                val startIdx = noteIndex * (SAMPLE_RATE * noteDurationMs / 1000)
                val len = SAMPLE_RATE * noteDurationMs / 1000
                for (i in 0 until len) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val progress = i.toDouble() / len
                    val envelope = (1.0 - progress)
                    val sample = (sin(2 * PI * freq * t) * envelope * 20000).toInt().toShort()
                    val targetIdx = startIdx + i
                    if (targetIdx < buffer.size) {
                        buffer[targetIdx] = sample
                    }
                }
            }
            playPcmBuffer(buffer)
        }
    }

    fun playButtonClick() {
        if (!soundEnabled) return
        scope.launch {
            val durationMs = 40
            val numSamples = (SAMPLE_RATE * durationMs / 1000)
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val envelope = (1.0 - (i.toDouble() / numSamples))
                val sample = (sin(2 * PI * 800.0 * t) * envelope * 20000).toInt().toShort()
                buffer[i] = sample
            }
            playPcmBuffer(buffer)
        }
    }

    fun playBooster() {
        if (!soundEnabled) return
        scope.launch {
            val durationMs = 250
            val numSamples = (SAMPLE_RATE * durationMs / 1000)
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / numSamples
                val freq = 400.0 + 800.0 * progress
                val envelope = sin(PI * progress)
                val sample = (sin(2 * PI * freq * t) * envelope * 22000).toInt().toShort()
                buffer[i] = sample
            }
            playPcmBuffer(buffer)
        }
    }

    private fun playPcmBuffer(buffer: ShortArray) {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            scope.launch {
                delay((buffer.size.toDouble() / SAMPLE_RATE * 1000).toLong() + 50)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    /**
     * Synthesizes and loops a cheerful, warm casual melody for Crush Candy in the background!
     */
    fun startMusic() {
        if (!musicEnabled) return
        if (musicJob?.isActive == true) return

        musicJob = scope.launch {
            // Cheerful sweet casual melody loop in C Major / A Minor
            // (Note frequency in Hz, duration in beats)
            val bpm = 124
            val beatMs = (60000.0 / bpm).toInt()
            val sixteenthMs = beatMs / 4

            // Melody pattern (notes)
            val melody = listOf(
                // Bar 1 (C Major)
                523.25 to 1.0, 659.25 to 1.0, 783.99 to 1.0, 659.25 to 1.0,
                // Bar 2 (G Major)
                392.00 to 1.0, 587.33 to 1.0, 783.99 to 1.0, 587.33 to 1.0,
                // Bar 3 (A Minor)
                440.00 to 1.0, 523.25 to 1.0, 659.25 to 1.0, 880.00 to 1.0,
                // Bar 4 (F Major)
                349.23 to 1.0, 523.25 to 1.0, 659.25 to 1.0, 523.25 to 1.0,
                // Bar 5 (Sweet High Variation)
                1046.50 to 1.0, 783.99 to 0.5, 880.00 to 0.5, 783.99 to 1.0, 659.25 to 1.0,
                // Bar 6
                587.33 to 1.0, 659.25 to 1.0, 783.99 to 1.0, 587.33 to 1.0,
                // Bar 7
                523.25 to 1.0, 659.25 to 1.0, 783.99 to 1.0, 1046.50 to 1.0,
                // Bar 8 (Resolve to C)
                783.99 to 1.5, 659.25 to 0.5, 523.25 to 2.0
            )

            // Bassline roots corresponding to chords
            val bassRoots = listOf(
                130.81, 130.81, 130.81, 130.81, // C
                98.00, 98.00, 98.00, 98.00,     // G
                110.00, 110.00, 110.00, 110.00, // A
                87.31, 87.31, 87.31, 87.31,     // F
                130.81, 130.81, 130.81, 130.81,
                98.00, 98.00, 98.00, 98.00,
                110.00, 110.00, 110.00, 110.00,
                130.81, 130.81, 130.81, 130.81
            )

            while (isActive && musicEnabled) {
                var beatCounter = 0
                for ((freq, beats) in melody) {
                    if (!isActive || !musicEnabled) break
                    val noteDurationMs = (beats * beatMs).toInt()
                    val numSamples = (SAMPLE_RATE * noteDurationMs / 1000)
                    val buffer = ShortArray(numSamples)
                    val bassFreq = bassRoots.getOrElse(beatCounter % bassRoots.size) { 130.81 }

                    for (i in 0 until numSamples) {
                        val t = i.toDouble() / SAMPLE_RATE
                        val progress = i.toDouble() / numSamples
                        // Soft marimba bell envelope
                        val envelope = (1.0 - progress * 0.7) * (1.0 - progress * 0.7)
                        val melodyWave = sin(2 * PI * freq * t) * 0.5 + 0.15 * sin(2 * PI * freq * 2 * t)
                        val bassWave = sin(2 * PI * bassFreq * t) * 0.3
                        val sample = ((melodyWave + bassWave) * envelope * 10000).toInt().coerceIn(-32767, 32767).toShort()
                        buffer[i] = sample
                    }
                    playPcmBuffer(buffer)
                    beatCounter += beats.toInt().coerceAtLeast(1)
                    delay(noteDurationMs.toLong())
                }
                delay(200) // Small pause between loops
            }
        }
    }

    fun stopMusic() {
        musicJob?.cancel()
        musicJob = null
        try {
            musicTrack?.stop()
            musicTrack?.release()
            musicTrack = null
        } catch (_: Exception) {}
    }
}
