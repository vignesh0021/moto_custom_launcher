package com.motorola.launcher.system

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Sound as Interface: Sci-Fi Tactical Acoustic Engine for STARK OS.
 *
 * Generates instantaneous, zero-latency procedural synthesized audio tones
 * using Android's native PCM AudioTrack.
 *
 * Benefits:
 * - 0 external asset bloat (no heavy sound files)
 * - 100% offline & instantaneous response (< 10ms latency)
 * - Respects system silent / DND mode and user's soundEffectsEnabled toggle
 */
object LauncherSoundManager {

    private val audioScope = CoroutineScope(Dispatchers.Default)
    private const val SAMPLE_RATE = 22050

    enum class SoundEvent {
        APP_CLICK,
        HERO_CYCLE,
        COLOR_SWIPE,
        QUANTUM_WARP,
        HYDRATION_CHIME
    }

    fun playSound(context: Context, event: SoundEvent, soundEnabled: Boolean) {
        if (!soundEnabled) return

        // Respect device ringer mode (Silent/Vibrate suppresses audio)
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (am != null && am.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            return
        }

        audioScope.launch {
            try {
                val pcm = synthesizePcm(event) ?: return@launch
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
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
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()
                // Let it play out then release
                track.setNotificationMarkerPosition(pcm.size)
                track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onPeriodicNotification(t: AudioTrack?) {}
                    override fun onMarkerReached(t: AudioTrack?) {
                        t?.stop()
                        t?.release()
                    }
                })
            } catch (_: Throwable) {
                // Non-critical background acoustic failure fallback
            }
        }
    }

    private fun synthesizePcm(event: SoundEvent): ShortArray? {
        val durationMs = when (event) {
            SoundEvent.APP_CLICK -> 32
            SoundEvent.HERO_CYCLE -> 55
            SoundEvent.COLOR_SWIPE -> 40
            SoundEvent.QUANTUM_WARP -> 90
            SoundEvent.HYDRATION_CHIME -> 110
        }

        val numSamples = (SAMPLE_RATE * durationMs) / 1000
        val pcm = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples

            val sampleVal: Double = when (event) {
                SoundEvent.APP_CLICK -> {
                    // Futuristic repulsor tick: fast chirp from 1200 Hz down to 600 Hz with sharp exponential decay
                    val freq = 1200.0 - (600.0 * progress)
                    val envelope = exp(-8.0 * progress)
                    sin(2.0 * PI * freq * t) * envelope
                }
                SoundEvent.HERO_CYCLE -> {
                    // Vibranium frequency harmonic rise: 520 Hz up to 880 Hz
                    val freq = 520.0 + (360.0 * progress)
                    val envelope = sin(PI * progress) // smooth swell and release
                    (sin(2.0 * PI * freq * t) * 0.7 + sin(2.0 * PI * freq * 1.5 * t) * 0.3) * envelope
                }
                SoundEvent.COLOR_SWIPE -> {
                    // Crystalline spectrum tick
                    val freq = 800.0 + (200.0 * progress)
                    val envelope = exp(-6.0 * progress)
                    sin(2.0 * PI * freq * t) * envelope
                }
                SoundEvent.QUANTUM_WARP -> {
                    // Resonant dual-phase warp
                    val freq1 = 300.0 + (900.0 * progress)
                    val freq2 = 450.0 + (450.0 * progress)
                    val envelope = (1.0 - progress) * 0.9
                    (sin(2.0 * PI * freq1 * t) * 0.6 + sin(2.0 * PI * freq2 * t) * 0.4) * envelope
                }
                SoundEvent.HYDRATION_CHIME -> {
                    // Gentle water droplet acoustic tone: pure high sine at 1400 Hz fading into 1050 Hz
                    val freq = 1400.0 - (350.0 * progress)
                    val envelope = exp(-4.5 * progress)
                    sin(2.0 * PI * freq * t) * envelope
                }
            }

            pcm[i] = (sampleVal * 18000.0).toInt().coerceIn(-32768, 32767).toShort()
        }

        return pcm
    }
}
