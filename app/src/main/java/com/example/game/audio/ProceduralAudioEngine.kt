package com.example.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.game.model.GameSettings
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class SoundEffect {
    FOOTSTEP,
    DOOR_SLIDE,
    SCANNER_PULSE,
    DECOY_PING,
    SHIELD_ACTIVATE,
    TERMINAL_BEEP,
    ALERT_SIREN,
    PUZZLE_SUCCESS,
    PUZZLE_FAIL,
    DAMAGE_HIT,
    ITEM_PICKUP,
    MISSION_COMPLETE,
    UI_CLICK
}

class ProceduralAudioEngine {

    private val sampleRate = 22050
    private val sfxBuffers = mutableMapOf<SoundEffect, ShortArray>()

    @Volatile
    private var masterVol = 0.85f
    @Volatile
    private var musicVol = 0.65f
    @Volatile
    private var sfxVol = 0.90f
    @Volatile
    private var voiceVol = 0.90f

    @Volatile
    private var ambienceRunning = false
    @Volatile
    private var ambienceMode = AmbienceMode.MENU
    @Volatile
    private var suspenseLevel = 0f

    private var ambienceThread: Thread? = null

    enum class AmbienceMode {
        MENU,
        EXPLORATION,
        SUSPENSE,
        PAUSED
    }

    init {
        buildBuffers()
    }

    fun updateSettings(settings: GameSettings) {
        masterVol = settings.masterVolume
        musicVol = settings.musicVolume
        sfxVol = settings.sfxVolume
        voiceVol = settings.voiceVolume
    }

    fun setAmbienceState(mode: AmbienceMode, suspense: Float = 0f) {
        ambienceMode = mode
        suspenseLevel = suspense.coerceIn(0f, 1f)
    }

    fun startAmbience() {
        if (ambienceRunning) return
        ambienceRunning = true
        ambienceThread = thread(start = true, isDaemon = true, name = "ShadowAmbienceAudio") {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            val track = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (_: Throwable) {
                ambienceRunning = false
                return@thread
            }

            try {
                track.play()
                val chunk = ShortArray(1024)
                var phase1 = 0.0
                var phase2 = 0.0
                var pulsePhase = 0.0

                while (ambienceRunning) {
                    val vol = (masterVol * musicVol * if (ambienceMode == AmbienceMode.PAUSED) 0.25f else 1f)
                        .coerceIn(0f, 1f)
                    val baseFreq = when (ambienceMode) {
                        AmbienceMode.MENU -> 55.0
                        AmbienceMode.EXPLORATION -> 48.0
                        AmbienceMode.SUSPENSE -> 62.0
                        AmbienceMode.PAUSED -> 42.0
                    }
                    val harmonicFreq = baseFreq * 1.498
                    val pulseRate = 1.1 + suspenseLevel * 2.8

                    for (i in chunk.indices) {
                        phase1 += 2.0 * PI * baseFreq / sampleRate
                        phase2 += 2.0 * PI * harmonicFreq / sampleRate
                        pulsePhase += 2.0 * PI * pulseRate / sampleRate
                        if (phase1 > 2.0 * PI) phase1 -= 2.0 * PI
                        if (phase2 > 2.0 * PI) phase2 -= 2.0 * PI
                        if (pulsePhase > 2.0 * PI) pulsePhase -= 2.0 * PI

                        val drone = sin(phase1) * 0.45 + sin(phase2) * 0.28 + sin(phase1 * 2.0) * 0.12
                        val lfo = 0.55 + 0.45 * sin(pulsePhase)
                        val tension = if (ambienceMode == AmbienceMode.SUSPENSE) {
                            sin(phase1 * 3.0) * 0.18 * suspenseLevel * lfo
                        } else 0.0
                        val sample = ((drone * lfo + tension) * vol * 5200.0)
                            .toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        chunk[i] = sample.toShort()
                    }
                    track.write(chunk, 0, chunk.size)
                }
            } catch (_: Throwable) {
            } finally {
                runCatching {
                    track.stop()
                    track.release()
                }
            }
        }
    }

    fun stopAmbience() {
        ambienceRunning = false
        ambienceThread?.interrupt()
        ambienceThread = null
    }

    fun playSfx(effect: SoundEffect, pitchScale: Float = 1.0f) {
        val data = sfxBuffers[effect] ?: return
        val vol = (masterVol * sfxVol).coerceIn(0f, 1f)
        if (vol <= 0.01f) return
        playShortBuffer(data, vol, pitchScale)
    }

    fun playRadioCue() {
        val vol = (masterVol * voiceVol).coerceIn(0f, 1f)
        if (vol <= 0.01f) return
        val len = (sampleRate * 0.24f).toInt()
        val buf = ShortArray(len)
        for (i in 0 until len) {
            val t = i.toFloat() / sampleRate
            val freq = if (t < 0.11f) 960f else 1280f
            val env = exp(-t * 10f)
            buf[i] = (sin(2.0 * PI * freq * t) * env * 14000.0).toInt().toShort()
        }
        playShortBuffer(buf, vol, 1.0f)
    }

    private fun playShortBuffer(data: ShortArray, volume: Float, pitchScale: Float) {
        thread(start = true, isDaemon = true) {
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate((sampleRate * pitchScale).toInt().coerceIn(8000, 48000))
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .build()
                    )
                    .setBufferSizeInBytes(data.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.setVolume(volume)
                track.write(data, 0, data.size)
                track.play()
                val durationMs = (data.size * 1000L / sampleRate) + 60L
                Thread.sleep(durationMs)
                track.stop()
                track.release()
            } catch (_: Throwable) {
            }
        }
    }

    private fun buildBuffers() {
        sfxBuffers[SoundEffect.FOOTSTEP] = synthFootstep()
        sfxBuffers[SoundEffect.DOOR_SLIDE] = synthSweep(210f, 95f, 0.36f, 0.35f)
        sfxBuffers[SoundEffect.SCANNER_PULSE] = synthSweep(380f, 1450f, 0.48f, 0.55f)
        sfxBuffers[SoundEffect.DECOY_PING] = synthDoublePing(1650f, 2100f)
        sfxBuffers[SoundEffect.SHIELD_ACTIVATE] = synthSweep(240f, 760f, 0.42f, 0.5f)
        sfxBuffers[SoundEffect.TERMINAL_BEEP] = synthTone(1120f, 0.08f, 0.45f)
        sfxBuffers[SoundEffect.ALERT_SIREN] = synthAlert()
        sfxBuffers[SoundEffect.PUZZLE_SUCCESS] = synthChord(listOf(523.25f, 659.25f, 783.99f), 0.38f)
        sfxBuffers[SoundEffect.PUZZLE_FAIL] = synthSweep(260f, 110f, 0.30f, 0.5f)
        sfxBuffers[SoundEffect.DAMAGE_HIT] = synthNoiseImpact(0.22f)
        sfxBuffers[SoundEffect.ITEM_PICKUP] = synthDoublePing(740f, 1110f)
        sfxBuffers[SoundEffect.MISSION_COMPLETE] = synthChord(listOf(440f, 554.37f, 659.25f, 880f), 0.65f)
        sfxBuffers[SoundEffect.UI_CLICK] = synthTone(880f, 0.045f, 0.35f)
    }

    private fun synthFootstep(): ShortArray {
        val len = (sampleRate * 0.09f).toInt()
        val out = ShortArray(len)
        val rng = Random(42)
        for (i in 0 until len) {
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 36f)
            val thud = sin(2.0 * PI * 92.0 * t) * 0.65
            val grit = (rng.nextFloat() * 2f - 1f) * 0.35
            out[i] = ((thud + grit) * env * 13000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthTone(freq: Float, durSec: Float, amp: Float): ShortArray {
        val len = (sampleRate * durSec).toInt()
        return ShortArray(len) { i ->
            val t = i.toFloat() / sampleRate
            val env = exp(-t * (5f / durSec))
            (sin(2.0 * PI * freq * t) * env * amp * 24000.0).toInt().toShort()
        }
    }

    private fun synthSweep(f0: Float, f1: Float, durSec: Float, amp: Float): ShortArray {
        val len = (sampleRate * durSec).toInt()
        var phase = 0.0
        return ShortArray(len) { i ->
            val frac = i.toFloat() / len
            val f = f0 + (f1 - f0) * frac
            phase += 2.0 * PI * f / sampleRate
            val env = sin(PI * frac)
            (sin(phase) * env * amp * 24000.0).toInt().toShort()
        }
    }

    private fun synthDoublePing(f1: Float, f2: Float): ShortArray {
        val len = (sampleRate * 0.24f).toInt()
        return ShortArray(len) { i ->
            val t = i.toFloat() / sampleRate
            val f = if (t < 0.1f) f1 else f2
            val localT = if (t < 0.1f) t else (t - 0.1f)
            val env = exp(-localT * 18f)
            (sin(2.0 * PI * f * t) * env * 16000.0).toInt().toShort()
        }
    }

    private fun synthAlert(): ShortArray {
        val len = (sampleRate * 0.45f).toInt()
        var phase = 0.0
        return ShortArray(len) { i ->
            val t = i.toFloat() / sampleRate
            val f = if ((t * 8f).toInt() % 2 == 0) 980f else 680f
            phase += 2.0 * PI * f / sampleRate
            val env = 1f - (t / 0.45f) * 0.4f
            (sin(phase) * env * 18000.0).toInt().toShort()
        }
    }

    private fun synthChord(freqs: List<Float>, durSec: Float): ShortArray {
        val len = (sampleRate * durSec).toInt()
        return ShortArray(len) { i ->
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 3.5f)
            var sum = 0.0
            for (f in freqs) sum += sin(2.0 * PI * f * t)
            ((sum / freqs.size) * env * 18000.0).toInt().toShort()
        }
    }

    private fun synthNoiseImpact(durSec: Float): ShortArray {
        val len = (sampleRate * durSec).toInt()
        val rng = Random(99)
        return ShortArray(len) { i ->
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 16f)
            val wave = sin(2.0 * PI * 75.0 * t) * 0.5 + (rng.nextFloat() * 2f - 1f) * 0.5
            (wave * env * 22000.0).toInt().toShort()
        }
    }
}
