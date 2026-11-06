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
 * Ultra-stable, zero-crash procedural sound and music synthesizer for Bonbon Bloom.
 * 
 * Key guarantees:
 * - Background music is DISABLED by default so users are never irritated by unexpected chiptunes.
 * - When sound effects play, the audio engine runs strictly for the duration of the effect.
 * - Once sound effects finish and music is not enabled, the audio track stops immediately.
 * - In background (onPause/onStop), the audio engine is unconditionally stopped and released.
 * - Uses a SINGLE streaming AudioTrack in MODE_STREAM with a unified audio mixer.
 */
object SoundManager {
    private const val TAG = "SoundManager"
    private val scope = CoroutineScope(Dispatchers.Default)

    var soundEnabled: Boolean = true
    var musicEnabled: Boolean = false // Default OFF to ensure quiet, peaceful gameplay

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

    // Gentle, soft ambient melody in C Major (played ONLY if player explicitly enables music)
    private val bpm = 112
    private val secondsPerBeat = 60.0 / bpm
    private val melodyNotes = listOf(
        523.25 to 1.0, 659.25 to 1.0, 783.99 to 1.0, 659.25 to 1.0,
        392.00 to 1.0, 587.33 to 1.0, 783.99 to 1.0, 587.33 to 1.0,
        440.00 to 1.0, 523.25 to 1.0, 659.25 to 1.0, 880.00 to 1.0,
        349.23 to 1.0, 523.25 to 1.0, 659.25 to 1.0, 523.25 to 1.0,
        1046.50 to 1.0, 783.99 to 0.5, 880.00 to 0.5, 783.99 to 1.0, 659.25 to 1.0,
        587.33 to 1.0, 659.25 to 1.0, 783.99 to 1.0, 587.33 to 1.0,
        523.25 to 1.0, 659.25 to 1.0, 783.99 to 1.0, 1046.50 to 1.0,
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
     * Starts the audio engine on demand (e.g. to play SFX or when music is explicitly turned on).
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
                var idleFrames = 0

                while (isActive && isEngineRunning) {
                    // If music is off and no sound effects are actively playing
                    if (!musicEnabled && activeSfxGenerators.isEmpty()) {
                        idleFrames++
                        if (idleFrames > 8) { // ~180ms of silence after last SFX
                            break // Automatically shut down AudioTrack to save battery and guarantee zero background noise
                        }
                        delay(25)
                        continue
                    }

                    idleFrames = 0

                    // Render one chunk of audio
                    for (i in 0 until CHUNK_SAMPLES) {
                        var mixedSample = 0.0

                        // 1. Synthesize gentle background music ONLY if explicitly enabled
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
                            val melodyWave = (sin(musicPhase) * 0.45 + sin(musicPhase * 2) * 0.1) * env
                            val bassWave = sin(bassPhase) * 0.25 * (1.0 - (musicBeatTime % secondsPerBeat) / secondsPerBeat)

                            mixedSample += (melodyWave + bassWave) * 3500.0

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
        if (activeSfxGenerators.isEmpty()) {
            stopAudioEngine()
        }
    }

    fun pause() {
        try {
            audioTrack?.pause()
        } catch (_: Throwable) {}
    }

    fun resume() {
        if (!isAppInForeground) return
        if (musicEnabled) {
            startAudioEngine()
        }
    }

    // --- Sound Effects Queueing (Zero native allocation) ---

    fun playSwap() {
        if (!soundEnabled) return
        activeSfxGenerators.add(
            GlidingToneGenerator(startFreq = 350.0, endFreq = 550.0, duration = 0.08)
        )
        startAudioEngine()
    }

    fun playInvalidSwap() {
        if (!soundEnabled) return
        activeSfxGenerators.add(
            GlidingToneGenerator(startFreq = 150.0, endFreq = 90.0, duration = 0.12)
        )
        startAudioEngine()
    }

    fun playMatch(comboLevel: Int = 1) {
        if (!soundEnabled) return
        val freqs = listOf(523.25, 587.33, 659.25, 783.99, 880.00, 1046.50, 1174.66, 1318.51)
        val freq = freqs[(comboLevel - 1).coerceIn(0, freqs.size - 1)]
        activeSfxGenerators.add(
            BellToneGenerator(freq = freq, duration = 0.18)
        )
        startAudioEngine()
    }

    fun playRocketBlast() {
        if (!soundEnabled) return
        activeSfxGenerators.add(
            LaserWhooshGenerator(startFreq = 800.0, endFreq = 200.0, duration = 0.28)
        )
        startAudioEngine()
    }

    fun playBombExplosion() {
        if (!soundEnabled) return
        activeSfxGenerators.add(
            ExplosionGenerator(duration = 0.35)
        )
        startAudioEngine()
    }

    fun playRainbowBlast() {
        if (!soundEnabled) return
        activeSfxGenerators.add(
            SparkleArpeggioGenerator(duration = 0.45)
        )
        startAudioEngine()
    }

    fun playComboFanfare(comboLevel: Int) {
        if (!soundEnabled) return
        val baseFreq = when (comboLevel) {
            2 -> 659.25
            3 -> 783.99
            4 -> 880.00
            else -> 1046.50
        }
        activeSfxGenerators.add(
            BellToneGenerator(freq = baseFreq, duration = 0.22)
        )
        startAudioEngine()
    }

    fun playLevelWin() {
        if (!soundEnabled) return
        scope.launch {
            val notes = listOf(523.25, 659.25, 783.99, 1046.50, 1318.51)
            for (freq in notes) {
                activeSfxGenerators.add(BellToneGenerator(freq = freq, duration = 0.25))
                startAudioEngine()
                delay(120)
            }
        }
    }

    fun playLevelFail() {
        if (!soundEnabled) return
        scope.launch {
            val notes = listOf(440.0, 392.0, 349.23, 293.66)
            for (freq in notes) {
                activeSfxGenerators.add(BellToneGenerator(freq = freq, duration = 0.22))
                startAudioEngine()
                delay(140)
            }
        }
    }

    fun playButtonClick() {
        if (!soundEnabled) return
        activeSfxGenerators.add(
            ClickGenerator(freq = 880.0, duration = 0.04)
        )
        startAudioEngine()
    }

    fun playBooster() {
        if (!soundEnabled) return
        activeSfxGenerators.add(
            GlidingToneGenerator(startFreq = 400.0, endFreq = 1200.0, duration = 0.25)
        )
        startAudioEngine()
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
