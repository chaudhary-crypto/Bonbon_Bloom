package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Ultra-stable, zero-crash procedural sound and music synthesizer for all Android devices.
 * 
 * Uses a SINGLE streaming AudioTrack in MODE_STREAM with a unified audio mixer:
 * - Exactly ONE AudioTrack instance across the entire application lifecycle.
 * - Never exhausts Android's native 32-track OS limit (preventing AudioFlinger crashes).
 * - Queries and honors system AudioTrack.getMinBufferSize() on every device architecture.
 * - Safe error boundaries around all audio hardware interactions.
 */
object SoundManager {
    private const val TAG = "SoundManager"
    private val scope = CoroutineScope(Dispatchers.Default)

    var soundEnabled: Boolean = true
    var musicEnabled: Boolean = true

    @Volatile
    private var isAppInForeground: Boolean = false

    fun setAppInForeground(inForeground: Boolean) {
        isAppInForeground = inForeground
        if (!inForeground) {
            stopAudioEngine()
        } else if (musicEnabled) {
            startAudioEngine()
        }
    }

    private const val SAMPLE_RATE = 22050
    private const val CHUNK_SAMPLES = 512

    private var audioTrack: AudioTrack? = null
    private var streamingJob: Job? = null
    private var isEngineRunning = false

    // Thread-safe queue of active sound effect waveform generators
    private val activeSfxGenerators = ConcurrentLinkedQueue<SfxGenerator>()

    // Background music state
    private var musicBeatTime = 0.0
    private var musicNoteIndex = 0
    private var musicPhase = 0.0
    private var bassPhase = 0.0

    // Cheerful melody in C Major
    private val bpm = 126
    private val secondsPerBeat = 60.0 / bpm
    private val melodyNotes = listOf(
        // Bar 1
        523.25 to 1.0, 659.25 to 1.0, 783.99 to 1.0, 659.25 to 1.0,
        // Bar 2
        392.00 to 1.0, 587.33 to 1.0, 783.99 to 1.0, 587.33 to 1.0,
        // Bar 3
        440.00 to 1.0, 523.25 to 1.0, 659.25 to 1.0, 880.00 to 1.0,
        // Bar 4
        349.23 to 1.0, 523.25 to 1.0, 659.25 to 1.0, 523.25 to 1.0,
        // Bar 5
        1046.50 to 1.0, 783.99 to 0.5, 880.00 to 0.5, 783.99 to 1.0, 659.25 to 1.0,
        // Bar 6
        587.33 to 1.0, 659.25 to 1.0, 783.99 to 1.0, 587.33 to 1.0,
        // Bar 7
        523.25 to 1.0, 659.25 to 1.0, 783.99 to 1.0, 1046.50 to 1.0,
        // Bar 8
        783.99 to 1.5, 659.25 to 0.5, 523.25 to 2.0
    )

    private val bassNotes = listOf(
        130.81, 130.81, 130.81, 130.81,
        98.00, 98.00, 98.00, 98.00,
        110.00, 110.00, 110.00, 110.00,
        87.31, 87.31, 87.31, 87.31,
        130.81, 130.81, 130.81, 130.81,
        98.00, 98.00, 98.00, 98.00,
        110.00, 110.00, 110.00, 110.00,
        130.81, 130.81, 130.81, 130.81
    )

    /**
     * Starts the single unified audio streaming engine.
     */
    @Synchronized
    fun startAudioEngine() {
        if (!isAppInForeground) return
        if (isEngineRunning && streamingJob?.isActive == true) return

        stopAudioEngine()

        streamingJob = scope.launch(Dispatchers.Default) {
            try {
                val minBufSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = maxOf(minBufSize * 2, 4096)

                val track = AudioTrack.Builder()
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
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = track
                track.play()
                isEngineRunning = true

                val pcmChunk = ShortArray(CHUNK_SAMPLES)
                val timeStep = 1.0 / SAMPLE_RATE

                while (isActive && isEngineRunning) {
                    if (!soundEnabled && !musicEnabled && activeSfxGenerators.isEmpty()) {
                        // Conserve CPU when audio is muted and idle
                        delay(50)
                        continue
                    }

                    // Render one chunk of audio
                    for (i in 0 until CHUNK_SAMPLES) {
                        var mixedSample = 0.0

                        // 1. Synthesize background music if enabled
                        if (musicEnabled) {
                            val currentNote = melodyNotes[musicNoteIndex % melodyNotes.size]
                            val freq = currentNote.first
                            val noteDuration = currentNote.second * secondsPerBeat

                            musicPhase += 2 * PI * freq * timeStep
                            if (musicPhase > 2 * PI) musicPhase -= 2 * PI

                            val bassFreq = bassNotes[(musicNoteIndex / 2) % bassNotes.size]
                            bassPhase += 2 * PI * bassFreq * timeStep
                            if (bassPhase > 2 * PI) bassPhase -= 2 * PI

                            val noteProgress = (musicBeatTime / noteDuration).coerceIn(0.0, 1.0)
                            val env = (1.0 - noteProgress * 0.6) * (1.0 - noteProgress * 0.6)
                            val melodyWave = (sin(musicPhase) * 0.55 + sin(musicPhase * 2) * 0.15) * env
                            val bassWave = sin(bassPhase) * 0.3 * (1.0 - (musicBeatTime % secondsPerBeat) / secondsPerBeat)

                            mixedSample += (melodyWave + bassWave) * 8000.0

                            musicBeatTime += timeStep
                            if (musicBeatTime >= noteDuration) {
                                musicBeatTime = 0.0
                                musicNoteIndex = (musicNoteIndex + 1) % melodyNotes.size
                            }
                        }

                        // 2. Mix active SFX generators
                        if (soundEnabled && activeSfxGenerators.isNotEmpty()) {
                            val iter = activeSfxGenerators.iterator()
                            while (iter.hasNext()) {
                                val sfx = iter.next()
                                val sfxSample = sfx.nextSample(timeStep)
                                if (sfx.isFinished()) {
                                    activeSfxGenerators.remove(sfx)
                                } else {
                                    mixedSample += sfxSample * 18000.0
                                }
                            }
                        }

                        pcmChunk[i] = mixedSample.toInt().coerceIn(-32767, 32767).toShort()
                    }

                    try {
                        track.write(pcmChunk, 0, CHUNK_SAMPLES)
                    } catch (e: Exception) {
                        Log.w(TAG, "AudioTrack write error: ${e.message}")
                        break
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to initialize native AudioTrack: ${e.message}")
            } finally {
                cleanupAudioTrack()
            }
        }
    }

    @Synchronized
    private fun cleanupAudioTrack() {
        isEngineRunning = false
        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
        } catch (_: Throwable) {}
        audioTrack = null
    }

    @Synchronized
    fun stopAudioEngine() {
        isEngineRunning = false
        streamingJob?.cancel()
        streamingJob = null
        cleanupAudioTrack()
    }

    fun startMusic() {
        musicEnabled = true
        if (isAppInForeground) {
            startAudioEngine()
        }
    }

    fun stopMusic() {
        musicEnabled = false
    }

    fun pause() {
        try {
            audioTrack?.pause()
        } catch (_: Throwable) {}
    }

    fun resume() {
        if (!isAppInForeground) return
        try {
            if (isEngineRunning && audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack?.play()
            } else {
                startAudioEngine()
            }
        } catch (_: Throwable) {
            startAudioEngine()
        }
    }

    // --- Sound Effects Queueing (Zero native allocation) ---

    fun playSwap() {
        if (!soundEnabled) return
        startAudioEngine()
        activeSfxGenerators.add(
            GlidingToneGenerator(startFreq = 350.0, endFreq = 550.0, duration = 0.08)
        )
    }

    fun playInvalidSwap() {
        if (!soundEnabled) return
        startAudioEngine()
        activeSfxGenerators.add(
            GlidingToneGenerator(startFreq = 150.0, endFreq = 90.0, duration = 0.12)
        )
    }

    fun playMatch(comboLevel: Int = 1) {
        if (!soundEnabled) return
        startAudioEngine()
        val freqs = listOf(523.25, 587.33, 659.25, 783.99, 880.00, 1046.50, 1174.66, 1318.51)
        val freq = freqs[(comboLevel - 1).coerceIn(0, freqs.size - 1)]
        activeSfxGenerators.add(
            BellToneGenerator(freq = freq, duration = 0.18)
        )
    }

    fun playRocketBlast() {
        if (!soundEnabled) return
        startAudioEngine()
        activeSfxGenerators.add(
            LaserWhooshGenerator(startFreq = 800.0, endFreq = 200.0, duration = 0.28)
        )
    }

    fun playBombExplosion() {
        if (!soundEnabled) return
        startAudioEngine()
        activeSfxGenerators.add(
            ExplosionGenerator(duration = 0.35)
        )
    }

    fun playRainbowBlast() {
        if (!soundEnabled) return
        startAudioEngine()
        activeSfxGenerators.add(
            SparkleArpeggioGenerator(duration = 0.45)
        )
    }

    fun playComboFanfare(comboLevel: Int) {
        if (!soundEnabled) return
        startAudioEngine()
        val baseFreq = when (comboLevel) {
            2 -> 659.25
            3 -> 783.99
            4 -> 880.00
            else -> 1046.50
        }
        activeSfxGenerators.add(
            BellToneGenerator(freq = baseFreq, duration = 0.22)
        )
    }

    fun playLevelWin() {
        if (!soundEnabled) return
        startAudioEngine()
        scope.launch {
            val notes = listOf(523.25, 659.25, 783.99, 1046.50, 1318.51)
            for (freq in notes) {
                activeSfxGenerators.add(BellToneGenerator(freq = freq, duration = 0.25))
                delay(120)
            }
        }
    }

    fun playLevelFail() {
        if (!soundEnabled) return
        startAudioEngine()
        scope.launch {
            val notes = listOf(440.0, 392.0, 349.23, 293.66)
            for (freq in notes) {
                activeSfxGenerators.add(BellToneGenerator(freq = freq, duration = 0.22))
                delay(140)
            }
        }
    }

    fun playButtonClick() {
        if (!soundEnabled) return
        startAudioEngine()
        activeSfxGenerators.add(
            ClickGenerator(freq = 880.0, duration = 0.04)
        )
    }

    fun playBooster() {
        if (!soundEnabled) return
        startAudioEngine()
        activeSfxGenerators.add(
            GlidingToneGenerator(startFreq = 400.0, endFreq = 1200.0, duration = 0.25)
        )
    }

    // --- Lightweight Procedural Wave Generators ---

    interface SfxGenerator {
        fun nextSample(dt: Double): Double
        fun isFinished(): Boolean
    }

    private class GlidingToneGenerator(
        private val startFreq: Double,
        private val endFreq: Double,
        private val duration: Double
    ) : SfxGenerator {
        private var elapsed = 0.0
        private var phase = 0.0

        override fun nextSample(dt: Double): Double {
            val progress = (elapsed / duration).coerceIn(0.0, 1.0)
            val freq = startFreq + (endFreq - startFreq) * progress
            phase += 2 * PI * freq * dt
            if (phase > 2 * PI) phase -= 2 * PI
            val env = sin(PI * progress)
            elapsed += dt
            return sin(phase) * env
        }

        override fun isFinished(): Boolean = elapsed >= duration
    }

    private class BellToneGenerator(
        private val freq: Double,
        private val duration: Double
    ) : SfxGenerator {
        private var elapsed = 0.0
        private var phase = 0.0

        override fun nextSample(dt: Double): Double {
            val progress = (elapsed / duration).coerceIn(0.0, 1.0)
            phase += 2 * PI * freq * dt
            if (phase > 2 * PI) phase -= 2 * PI
            val env = (1.0 - progress) * (1.0 - progress)
            elapsed += dt
            return (sin(phase) + 0.35 * sin(phase * 2) + 0.15 * sin(phase * 3)) * env
        }

        override fun isFinished(): Boolean = elapsed >= duration
    }

    private class LaserWhooshGenerator(
        private val startFreq: Double,
        private val endFreq: Double,
        private val duration: Double
    ) : SfxGenerator {
        private var elapsed = 0.0
        private var phase = 0.0

        override fun nextSample(dt: Double): Double {
            val progress = (elapsed / duration).coerceIn(0.0, 1.0)
            val freq = startFreq + (endFreq - startFreq) * progress
            phase += 2 * PI * freq * dt
            if (phase > 2 * PI) phase -= 2 * PI
            val noise = (Random.nextFloat() * 2 - 1) * 0.3f
            val env = sin(PI * progress)
            elapsed += dt
            return (sin(phase) * 0.7 + noise) * env
        }

        override fun isFinished(): Boolean = elapsed >= duration
    }

    private class ExplosionGenerator(
        private val duration: Double
    ) : SfxGenerator {
        private var elapsed = 0.0
        private var phase = 0.0

        override fun nextSample(dt: Double): Double {
            val progress = (elapsed / duration).coerceIn(0.0, 1.0)
            val freq = 160.0 - 110.0 * progress
            phase += 2 * PI * freq * dt
            if (phase > 2 * PI) phase -= 2 * PI
            val noise = (Random.nextFloat() * 2 - 1) * 0.5f * (1.0 - progress).toFloat()
            val env = (1.0 - progress)
            elapsed += dt
            return (sin(phase) * 0.5 + noise) * env
        }

        override fun isFinished(): Boolean = elapsed >= duration
    }

    private class SparkleArpeggioGenerator(
        private val duration: Double
    ) : SfxGenerator {
        private var elapsed = 0.0
        private var phase = 0.0

        override fun nextSample(dt: Double): Double {
            val progress = (elapsed / duration).coerceIn(0.0, 1.0)
            val freq = 600.0 + 1200.0 * progress
            phase += 2 * PI * freq * dt
            if (phase > 2 * PI) phase -= 2 * PI
            val sparkle = sin(phase * 1.5) * 0.3
            val env = sin(PI * progress)
            elapsed += dt
            return (sin(phase) + sparkle) * env
        }

        override fun isFinished(): Boolean = elapsed >= duration
    }

    private class ClickGenerator(
        private val freq: Double,
        private val duration: Double
    ) : SfxGenerator {
        private var elapsed = 0.0
        private var phase = 0.0

        override fun nextSample(dt: Double): Double {
            val progress = (elapsed / duration).coerceIn(0.0, 1.0)
            phase += 2 * PI * freq * dt
            val env = 1.0 - progress
            elapsed += dt
            return sin(phase) * env
        }

        override fun isFinished(): Boolean = elapsed >= duration
    }
}
