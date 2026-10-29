package com.motorola.launcher.system

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.motorola.launcher.ui.model.CompanionStyle
import com.motorola.launcher.ui.model.MarvelHeroProtocol
import java.util.Locale

/**
 * Manages notification voice announcements for Marvel Superheroes and PixelFriend companions.
 * Uses hardware-accelerated Android native TextToSpeech with hero-specific vocal acoustics,
 * dynamic voice selection (gender/timbre matching), and varied character-authentic dialogue pools:
 * - Hulk: Deep booming bass timbre (pitch 0.50f, rate 0.74f)
 * - Thor: Heroic thunder resonance (pitch 0.65f, rate 0.86f)
 * - Spider-Man: Youthful, energetic cadence (pitch 1.34f, rate 1.22f)
 * - Iron Man: Crisp, witty tech billionaire tone (pitch 1.06f, rate 1.10f)
 * - Captain America: Commanding, clear tactical voice (pitch 0.85f, rate 0.96f)
 * - Black Panther: Regal, dignified Wakandan cadence (pitch 0.78f, rate 0.88f)
 * - Doctor Strange: Composed, measured mystic resonance (pitch 0.90f, rate 0.92f)
 * - Black Widow: Sharp, precise operative tone (pitch 1.22f, rate 1.04f, female voice preferred)
 * - Avengers Unified: Tactical initiative alert tone (pitch 1.00f, rate 1.00f)
 */
class CompanionVoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val promptIndexMap = mutableMapOf<MarvelHeroProtocol, Int>()
    private val acceptedIndexMap = mutableMapOf<MarvelHeroProtocol, Int>()
    private val refusedIndexMap = mutableMapOf<MarvelHeroProtocol, Int>()

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
     * Selects from a varied pool of character-authentic dialogue so the voice never repeats identically.
     */
    fun speakHeroPrompt(hero: MarvelHeroProtocol, style: CompanionStyle) {
        if (!isInitialized) return
        configureHeroVoice(hero, style)

        val lines = if (style != CompanionStyle.HERO_SUIT) {
            listOf(
                "Hydration check, bestie! Big ideas need a little water. Take a sip?",
                "Hey friend! Time for a quick splash of water. Keep that energy high!",
                "Friendly reminder! A glass of water now makes the whole day brighter!"
            )
        } else {
            when (hero) {
                MarvelHeroProtocol.HULK -> listOf(
                    "Hulk reminder! Drink water now! Big muscles need water to smash!",
                    "Hulk thirsty! You thirsty too! Drink cold water, make body strong!",
                    "Dehydration make Hulk angry! You drink fresh water, keep Hulk happy!",
                    "Water good! Drink big glass now! Hulk watch you drink!"
                )
                MarvelHeroProtocol.IRON_MAN -> listOf(
                    "Coolant levels critical, Tony. Arc reactor temperature rising. Drink some water before we overheat.",
                    "Jarvis telemetry alert: Cellular hydration below optimal efficiency. Grab a glass of water, boss.",
                    "Even a genius billionaire philanthropist needs fluids. Drink some water, let's keep that brain firing.",
                    "Hydration protocol active. Powering down thrusters for a quick refuel. Drink up, let's get back in the suit."
                )
                MarvelHeroProtocol.CAPTAIN_AMERICA -> listOf(
                    "Hydration check, soldier! Peak readiness requires regular water. Keep your discipline sharp!",
                    "Attention! A good soldier never marches dehydrated. Drink a fresh glass of water, on the double!",
                    "We can do this all day, but only if we stay hydrated. Time for a water break, soldier.",
                    "Discipline starts with the basics. Hydrate now, stay ready for whatever battle comes next."
                )
                MarvelHeroProtocol.THOR -> listOf(
                    "Hear the Thunderer! A warrior must never face battle parched! Drink of the water of life!",
                    "By Odin's beard! Even the mighty gods of Asgard replenish their strength! Quench your thirst, warrior!",
                    "A storm approaches, friend! Drink deep from your vessel and let the lightning surge within you!",
                    "A toast to victory! But first, a goblet of pure cold water to fuel your fighting spirit!"
                )
                MarvelHeroProtocol.BLACK_PANTHER -> listOf(
                    "Wakanda reminder. Pure water sustains both the body and the warrior spirit. Drink now.",
                    "The ancestors teach us to protect the vessel of life. Take a moment, replenish yourself with water.",
                    "Vibranium shields require focus, and focus requires hydration. Honor your health, my friend.",
                    "Wakanda forever, but health for today. Drink your water and stand strong with the Panther."
                )
                MarvelHeroProtocol.SPIDER_MAN -> listOf(
                    "Friendly neighborhood check! Web-shooters need fluid and so do you! Time for a quick water break!",
                    "Hey there! My Spider-Sense is tingling... and it says you haven't had water in a while! Go drink some!",
                    "Swinging between skyscrapers gets thirsty! Don't let your webs get dry, grab some water!",
                    "Quick reminder from your pal Spidey: hydrate now so you can keep being awesome today!"
                )
                MarvelHeroProtocol.DOCTOR_STRANGE -> listOf(
                    "Mystic equilibrium. Mental clarity across all dimensions begins with hydration. Drink water!",
                    "I have peeked into fourteen million futures, and in every good one, you drink a glass of water right now.",
                    "The Sanctum Sanctorum demands balance of the elements. Replenish your internal water element.",
                    "A clouded mind cannot wield the mystic arts. Hydrate your vessel, restore your cosmic focus."
                )
                MarvelHeroProtocol.BLACK_WIDOW -> listOf(
                    "Hydration protocol. Sharp reflexes start with staying hydrated. Take a sip, operative.",
                    "In the field, dehydration compromises mission success. Don't slip up. Drink water now.",
                    "Target acquired: a clean glass of water. Refuel your senses, operative.",
                    "Stay light on your feet and sharp in the mind. Hydration check completed, now take a drink."
                )
                MarvelHeroProtocol.AVENGERS_UNIFIED -> listOf(
                    "Avengers Initiative priority alert: Hydration reserves low. Stand down for immediate replenishment.",
                    "Command to all active operatives: Maintain optimal physiological readiness. Drink water now.",
                    "Mission parameters updated: Hydration check in progress. Please consume one glass of water."
                )
            }
        }

        val nextIndex = ((promptIndexMap[hero] ?: 0) + 1) % lines.size
        promptIndexMap[hero] = nextIndex
        val text = lines[nextIndex]

        playArrivalChime()
        speak(text)
    }

    /**
     * Speaks celebratory dialogue when the user taps "Drank it ✓".
     */
    fun speakHeroAccepted(hero: MarvelHeroProtocol, style: CompanionStyle) {
        if (!isInitialized) return
        configureHeroVoice(hero, style)

        val lines = if (style != CompanionStyle.HERO_SUIT) {
            listOf(
                "Heck yeah! One hundred percent friend energy!",
                "Great job! Body hydrated, spirits high!",
                "Awesome! You are crushing your healthy habits today!"
            )
        } else {
            when (hero) {
                MarvelHeroProtocol.HULK -> listOf(
                    "Hulk smash dehydration! Good job friend, Hulk happy! Muscles stay strong!",
                    "Yes! Hulk like seeing you drink! Now we strong together!",
                    "Hulk roar with joy! You did it! Keep smashing the day!"
                )
                MarvelHeroProtocol.THOR -> listOf(
                    "Another! A mighty sip worthy of Valhalla!",
                    "Magnificent! The thunder sings of your triumph over thirst!",
                    "By the Bifrost, you drink like a true Asgardian champion!"
                )
                MarvelHeroProtocol.SPIDER_MAN -> listOf(
                    "Web-shooters reloaded! Woohoo! You rock! Keep being awesome!",
                    "Boom! Dehydration defeated by your friendly neighborhood hero! Nice job!",
                    "That's what I'm talking about! Hydrated and ready to save the city!"
                )
                MarvelHeroProtocol.IRON_MAN -> listOf(
                    "Optimal coolant restored. Power output at one hundred percent.",
                    "Diagnostics green across the board. Good work, Tony would be proud.",
                    "Power levels stabilized. Systems operating at peak efficiency."
                )
                MarvelHeroProtocol.CAPTAIN_AMERICA -> listOf(
                    "Outstanding work, soldier. Stay sharp and hydrated!",
                    "Mission accomplished. Discipline like that wins battles.",
                    "Good work, soldier. That's how we keep moving forward."
                )
                MarvelHeroProtocol.BLACK_PANTHER -> listOf(
                    "Wakanda salutes your discipline. Strength is replenished.",
                    "The Panther smiles upon your health. Walk with pride.",
                    "A warrior honors their vessel. Well done, my friend."
                )
                MarvelHeroProtocol.DOCTOR_STRANGE -> listOf(
                    "Cosmic alignment restored. Your focus is renewed.",
                    "The elements are in harmony once again. Excellent.",
                    "Clarity returns to the astral plane. Carry on, seeker."
                )
                MarvelHeroProtocol.BLACK_WIDOW -> listOf(
                    "Target neutralized. Reflexes back to maximum precision.",
                    "Clean execution, operative. Keep your head on a swivel.",
                    "Objective complete. You are ready for anything."
                )
                MarvelHeroProtocol.AVENGERS_UNIFIED -> listOf(
                    "Hydration target confirmed. All Avengers systems nominal.",
                    "Operative status restored to one hundred percent operational readiness."
                )
            }
        }

        val nextIndex = ((acceptedIndexMap[hero] ?: 0) + 1) % lines.size
        acceptedIndexMap[hero] = nextIndex
        val text = lines[nextIndex]

        speak(text)
    }

    /**
     * Speaks understanding reminder when the user taps "Later".
     */
    fun speakHeroRefused(hero: MarvelHeroProtocol, style: CompanionStyle) {
        if (!isInitialized) return
        configureHeroVoice(hero, style)

        val lines = when {
            hero == MarvelHeroProtocol.HULK && style == CompanionStyle.HERO_SUIT -> listOf(
                "Hulk wait for you. Drink soon!",
                "Hulk sad, but Hulk be patient. Drink later, okay?"
            )
            hero == MarvelHeroProtocol.IRON_MAN && style == CompanionStyle.HERO_SUIT -> listOf(
                "Snoozing telemetry reminder. Don't push your reactor too hard, boss.",
                "Standing by on standby power. Drink soon, Jarvis out."
            )
            hero == MarvelHeroProtocol.SPIDER_MAN && style == CompanionStyle.HERO_SUIT -> listOf(
                "No worries! But promise me you'll drink some water later, deal?",
                "Catch ya on the flip side! Don't forget that water!"
            )
            else -> listOf(
                "No worries. Take care of yourself soon, okay?",
                "Standing down for now. Remember to hydrate when you can."
            )
        }

        val nextIndex = ((refusedIndexMap[hero] ?: 0) + 1) % lines.size
        refusedIndexMap[hero] = nextIndex
        val text = lines[nextIndex]

        speak(text)
    }

    private fun configureHeroVoice(hero: MarvelHeroProtocol, style: CompanionStyle) {
        val engine = tts ?: return

        // 1. Dynamic Android native TTS voice selection (gender & locale matching where available)
        try {
            val availableVoices = engine.voices
            if (!availableVoices.isNullOrEmpty()) {
                val targetVoice: Voice? = when (hero) {
                    MarvelHeroProtocol.BLACK_WIDOW -> {
                        // Black Widow: Prefer natural English female voice
                        availableVoices.firstOrNull { voice ->
                            val n = voice.name.lowercase()
                            (n.contains("female") || voice.features.any { it.contains("female") }) &&
                            voice.locale.language == Locale.ENGLISH.language
                        } ?: availableVoices.firstOrNull { it.locale.language == Locale.ENGLISH.language }
                    }
                    MarvelHeroProtocol.IRON_MAN, MarvelHeroProtocol.DOCTOR_STRANGE -> {
                        // Iron Man / Jarvis & Doctor Strange: British English male voice for cultured/AI tone
                        availableVoices.firstOrNull { voice ->
                            voice.locale == Locale.UK && voice.name.lowercase().contains("male")
                        } ?: availableVoices.firstOrNull { voice ->
                            voice.name.lowercase().contains("male") && voice.locale.language == Locale.ENGLISH.language
                        }
                    }
                    else -> {
                        // Other heroes: Distinct male English voice
                        availableVoices.firstOrNull { voice ->
                            val n = voice.name.lowercase()
                            (n.contains("male") && !n.contains("female")) &&
                            voice.locale.language == Locale.ENGLISH.language
                        }
                    }
                }
                targetVoice?.let { engine.voice = it }
            }
        } catch (_: Throwable) {
            // Safe fallback if voice enumeration is restricted
        }

        // 2. Character-specific acoustic pitch and speech cadence tuning
        val (pitch, rate) = when {
            style != CompanionStyle.HERO_SUIT -> Pair(1.05f, 1.05f)
            hero == MarvelHeroProtocol.HULK -> Pair(0.50f, 0.74f)          // Deep booming gravelly titan
            hero == MarvelHeroProtocol.THOR -> Pair(0.65f, 0.86f)          // Booming heroic Asgardian thunder
            hero == MarvelHeroProtocol.SPIDER_MAN -> Pair(1.34f, 1.22f)    // Fast, youthful, upbeat Peter Parker
            hero == MarvelHeroProtocol.IRON_MAN -> Pair(1.06f, 1.10f)      // Crisp, witty tech billionaire
            hero == MarvelHeroProtocol.CAPTAIN_AMERICA -> Pair(0.85f, 0.96f) // Clear commanding tactical leader
            hero == MarvelHeroProtocol.BLACK_PANTHER -> Pair(0.78f, 0.88f) // Deep, smooth dignified Wakandan king
            hero == MarvelHeroProtocol.DOCTOR_STRANGE -> Pair(0.90f, 0.92f)// Calm, mystical Sorcerer Supreme
            hero == MarvelHeroProtocol.BLACK_WIDOW -> Pair(1.22f, 1.04f)   // Sharp, sleek operative cadence
            hero == MarvelHeroProtocol.AVENGERS_UNIFIED -> Pair(1.00f, 1.00f) // Unified command dispatch
            else -> Pair(1.0f, 1.0f)
        }
        engine.setPitch(pitch)
        engine.setSpeechRate(rate)
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
