package com.motorola.launcher.system

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.motorola.launcher.ui.model.CompanionStyle
import com.motorola.launcher.ui.model.MarvelHeroProtocol
import java.util.Locale

/**
 * Manages notification voice announcements for Marvel Superheroes and PixelFriend companions.
 * Uses hardware-accelerated Android native TextToSpeech with hero-specific vocal acoustics:
 * - Hulk: Deep booming bass timbre (pitch 0.55f, rate 0.82f)
 * - Thor: Heroic thunder resonance (pitch 0.68f, rate 0.90f)
 * - Spider-Man: Youthful, energetic cadence (pitch 1.25f, rate 1.15f)
 * - Iron Man: Crisp, synthesized AI assistant cadence (pitch 0.98f, rate 1.05f)
 * - Captain America: Commanding, clear tactical voice (pitch 0.88f, rate 0.98f)
 * - Black Panther: Regal, dignified Wakandan cadence (pitch 0.82f, rate 0.95f)
 * - Doctor Strange: Composed, measured mystic resonance (pitch 0.85f, rate 0.95f)
 * - Black Widow: Sharp, precise operative tone (pitch 1.08f, rate 1.02f)
 */
class CompanionVoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val result = engine.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isInitialized = true
                }
            }
        }
    }

    /**
     * Speaks the hero's hydration check notification when they arrive on screen.
     */
    fun speakHeroPrompt(hero: MarvelHeroProtocol, style: CompanionStyle) {
        if (!isInitialized) return
        configureHeroVoice(hero, style)
        val text = when {
            style != CompanionStyle.HERO_SUIT -> "Hydration check, bestie! Big ideas need a little water. Take a sip?"
            hero == MarvelHeroProtocol.HULK -> "Hulk reminder! Drink water now! Big muscles need water to smash!"
            hero == MarvelHeroProtocol.IRON_MAN -> "Coolant levels critical. Arc reactor needs fluid. Take a fresh sip of water."
            hero == MarvelHeroProtocol.CAPTAIN_AMERICA -> "Hydration check, soldier! Peak readiness requires regular water. Keep your discipline sharp!"
            hero == MarvelHeroProtocol.THOR -> "Hear the Thunderer! A warrior must never face battle parched! Drink of the water of life!"
            hero == MarvelHeroProtocol.BLACK_PANTHER -> "Wakanda reminder. Pure water sustains both the body and the warrior spirit. Drink now."
            hero == MarvelHeroProtocol.DOCTOR_STRANGE -> "Mystic equilibrium. Mental clarity across all dimensions begins with hydration. Drink water!"
            hero == MarvelHeroProtocol.SPIDER_MAN -> "Friendly neighborhood check! Web-shooters need fluid and so do you! Time for a quick water break!"
            hero == MarvelHeroProtocol.BLACK_WIDOW -> "Hydration protocol. Sharp reflexes start with staying hydrated. Take a sip, operative."
            else -> "Hydration check! Time to drink a glass of water!"
        }
        playArrivalChime()
        speak(text)
    }

    /**
     * Speaks celebratory dialogue when the user taps "Drank it ✓".
     */
    fun speakHeroAccepted(hero: MarvelHeroProtocol, style: CompanionStyle) {
        if (!isInitialized) return
        configureHeroVoice(hero, style)
        val text = when {
            style != CompanionStyle.HERO_SUIT -> "Heck yeah! One hundred percent friend energy!"
            hero == MarvelHeroProtocol.HULK -> "Hulk smash dehydration! Good job friend, Hulk happy! Muscles stay strong!"
            hero == MarvelHeroProtocol.THOR -> "Another! A mighty sip worthy of Valhalla!"
            hero == MarvelHeroProtocol.SPIDER_MAN -> "Web-shooters reloaded! Woohoo! One hundred percent friend energy! You rock!"
            hero == MarvelHeroProtocol.IRON_MAN -> "Optimal coolant restored. Power output at one hundred percent."
            hero == MarvelHeroProtocol.CAPTAIN_AMERICA -> "Outstanding work, soldier. Stay sharp and hydrated!"
            hero == MarvelHeroProtocol.BLACK_PANTHER -> "Wakanda salutes your discipline. Strength is replenished."
            hero == MarvelHeroProtocol.DOCTOR_STRANGE -> "Cosmic alignment restored. Your focus is renewed."
            hero == MarvelHeroProtocol.BLACK_WIDOW -> "Target neutralized. Reflexes back to maximum precision."
            else -> "Great job staying hydrated!"
        }
        speak(text)
    }

    /**
     * Speaks understanding reminder when the user taps "Later".
     */
    fun speakHeroRefused(hero: MarvelHeroProtocol, style: CompanionStyle) {
        if (!isInitialized) return
        configureHeroVoice(hero, style)
        val text = when {
            hero == MarvelHeroProtocol.HULK && style == CompanionStyle.HERO_SUIT -> "Hulk wait for you. Drink soon!"
            else -> "No worries. Take care of yourself soon, okay?"
        }
        speak(text)
    }

    private fun configureHeroVoice(hero: MarvelHeroProtocol, style: CompanionStyle) {
        val (pitch, rate) = when {
            style != CompanionStyle.HERO_SUIT -> Pair(1.05f, 1.05f)
            hero == MarvelHeroProtocol.HULK -> Pair(0.55f, 0.82f)           // Deep bass, gravelly powerful cadence
            hero == MarvelHeroProtocol.THOR -> Pair(0.68f, 0.90f)           // Booming heroic resonance
            hero == MarvelHeroProtocol.SPIDER_MAN -> Pair(1.25f, 1.15f)     // Youthful, energetic & upbeat
            hero == MarvelHeroProtocol.IRON_MAN -> Pair(0.98f, 1.05f)       // Crisp, confident tech assistant
            hero == MarvelHeroProtocol.CAPTAIN_AMERICA -> Pair(0.88f, 0.98f)// Commanding, clear tactical voice
            hero == MarvelHeroProtocol.BLACK_PANTHER -> Pair(0.82f, 0.95f)  // Dignified, smooth regal tone
            hero == MarvelHeroProtocol.DOCTOR_STRANGE -> Pair(0.85f, 0.95f) // Calm, measured mystic voice
            hero == MarvelHeroProtocol.BLACK_WIDOW -> Pair(1.08f, 1.02f)    // Sharp, composed operative voice
            else -> Pair(1.0f, 1.0f)
        }
        tts?.setPitch(pitch)
        tts?.setSpeechRate(rate)
    }

    private fun playArrivalChime() {
        try {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 55)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
        } catch (_: Throwable) {}
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "HERO_VOICE_${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}

/**
 * Remember and auto-cleanup CompanionVoiceManager in Compose.
 */
@Composable
fun rememberCompanionVoiceManager(): CompanionVoiceManager {
    val context = LocalContext.current
    val voiceManager = remember(context) { CompanionVoiceManager(context) }
    DisposableEffect(voiceManager) {
        onDispose {
            voiceManager.destroy()
        }
    }
    return voiceManager
}
