package com.motorola.launcher.system

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.motorola.launcher.ui.model.CompanionStyle
import com.motorola.launcher.ui.model.CustomizationSettings
import com.motorola.launcher.ui.model.MarvelHeroProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Visual mouth shapes for real-time lip-synchronization with TextToSpeech audio.
 */
enum class VisemeShape {
    REST,      // Neutral closed smile
    OPEN_A,    // Open mouth for 'a', 'ah', 'aa'
    ROUND_O,   // Rounded mouth for 'o', 'oo', 'u'
    WIDE_E,    // Wide smile mouth for 'e', 'ee', 'i'
    CLOSED_M   // Closed pressed lips for 'm', 'p', 'b'
}

/**
 * Manages notification voice announcements for Marvel Superheroes and Character Companions.
 * Uses hardware-accelerated Android native TextToSpeech with hero-specific vocal acoustics,
 * dynamic voice selection (gender/timbre matching), Tamil TTS with phonetic fallback,
 * and varied character-authentic dialogue pools so the voice never repeats identically:
 * - Mithra: Sweet toddler/daughter voice (pitch 1.55f, rate 1.08f, Tamil/English)
 * - Mithra's Mom: Warm, caring wife voice (pitch 1.18f, rate 0.98f, female voice, Tamil/English)
 * - Hulk: Deep booming bass timbre (pitch 0.48f, rate 0.72f)
 * - Thor: Heroic thunder resonance (pitch 0.65f, rate 0.86f)
 * - Spider-Man: Youthful, energetic cadence (pitch 1.34f, rate 1.22f)
 * - Iron Man: Crisp, witty tech billionaire tone (pitch 1.06f, rate 1.10f)
 * - Captain America: Commanding, clear tactical voice (pitch 0.85f, rate 0.96f)
 * - Black Panther: Regal, dignified Wakandan cadence (pitch 0.78f, rate 0.88f)
 * - Doctor Strange: Composed, measured mystic resonance (pitch 0.90f, rate 0.92f)
 * - Black Widow: Sharp, precise operative tone (pitch 1.22f, rate 1.04f, female voice)
 * - Avengers Unified: Tactical initiative alert tone (pitch 1.00f, rate 1.00f)
 */
class CompanionVoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val promptIndexMap = mutableMapOf<String, Int>()
    private val acceptedIndexMap = mutableMapOf<String, Int>()
    private val refusedIndexMap = mutableMapOf<String, Int>()

    var isSpeaking by mutableStateOf(false)
        private set
    var currentViseme by mutableStateOf(VisemeShape.REST)
        private set

    private var visemeJob: Job? = null

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
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                        startVisemeCadence()
                    }

                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                        visemeJob?.cancel()
                        currentViseme = VisemeShape.REST
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                        visemeJob?.cancel()
                        currentViseme = VisemeShape.REST
                    }
                })
            }
        }
    }

    private fun startVisemeCadence() {
        visemeJob?.cancel()
        visemeJob = CoroutineScope(Dispatchers.Main).launch {
            val visemes = listOf(
                VisemeShape.OPEN_A,
                VisemeShape.WIDE_E,
                VisemeShape.ROUND_O,
                VisemeShape.OPEN_A,
                VisemeShape.CLOSED_M,
                VisemeShape.WIDE_E
            )
            var index = 0
            while (isSpeaking) {
                currentViseme = visemes[index % visemes.size]
                index++
                delay(140)
            }
            currentViseme = VisemeShape.REST
        }
    }

    /**
     * Speaks the companion's hydration check notification when they arrive on screen.
     */
    fun speakPrompt(customization: CustomizationSettings) {
        if (!isInitialized) return
        val style = customization.companionStyle
        val hero = customization.heroProtocol

        configureCompanionVoice(customization)
        playArrivalChime()

        when (style) {
            CompanionStyle.MITHRA -> {
                val text = customization.mithraDialoguePrompt.ifBlank { "அப்பா தண்ணி குடிங்க" }
                val phonetic = "Appa, thanni kudinga!"
                speakWithLocaleCheck(text, phonetic, customization)
            }
            CompanionStyle.MITHRA_MOM -> {
                val text = customization.momDialoguePrompt.ifBlank { "ஏங்க தண்ணி குடிங்க" }
                val phonetic = "Yenga, thanni kudinga!"
                speakWithLocaleCheck(text, phonetic, customization)
            }
            CompanionStyle.CUSTOM_SHEET -> {
                val text = customization.customDialoguePrompt.ifBlank { "Hydration check! Drink some water!" }
                speakWithLocaleCheck(text, text, customization)
            }
            CompanionStyle.HERO_SUIT -> {
                speakHeroPromptInternal(hero, customization)
            }
            else -> {
                val lines = listOf(
                    "Hydration check, bestie! Big ideas need a little water. Take a sip?",
                    "Hey friend! Time for a quick splash of water. Keep that energy high!",
                    "Friendly reminder! A glass of water now makes the whole day brighter!"
                )
                val key = "other_prompt"
                val nextIdx = ((promptIndexMap[key] ?: 0) + 1) % lines.size
                promptIndexMap[key] = nextIdx
                applyAcoustics(customization)
                speak(lines[nextIdx])
            }
        }
    }

    /**
     * Speaks celebratory dialogue when the user taps "Drank it ✓".
     */
    fun speakAccepted(customization: CustomizationSettings) {
        if (!isInitialized) return
        val style = customization.companionStyle
        val hero = customization.heroProtocol

        configureCompanionVoice(customization)

        when (style) {
            CompanionStyle.MITHRA -> {
                val text = customization.mithraDialogueAccepted.ifBlank { "அப்பா சூப்பர்! நல்லா தண்ணி குடிச்சிட்டீங்க!" }
                val phonetic = "Appa super! Nalla thanni kudichitteenga!"
                speakWithLocaleCheck(text, phonetic, customization)
            }
            CompanionStyle.MITHRA_MOM -> {
                val text = customization.momDialogueAccepted.ifBlank { "ரொம்ப நல்லதுங்க, உடம்பை நல்லா பார்த்துக்கோங்க!" }
                val phonetic = "Romba nalladhunga, udamba paarthukkoanga!"
                speakWithLocaleCheck(text, phonetic, customization)
            }
            CompanionStyle.CUSTOM_SHEET -> {
                val text = customization.customDialogueAccepted.ifBlank { "Awesome! Stay hydrated and energized!" }
                speakWithLocaleCheck(text, text, customization)
            }
            CompanionStyle.HERO_SUIT -> {
                speakHeroAcceptedInternal(hero, customization)
            }
            else -> {
                val lines = listOf(
                    "Heck yeah! One hundred percent friend energy!",
                    "Great job! Body hydrated, spirits high!",
                    "Awesome! You are crushing your healthy habits today!"
                )
                val key = "other_accepted"
                val nextIdx = ((acceptedIndexMap[key] ?: 0) + 1) % lines.size
                acceptedIndexMap[key] = nextIdx
                applyAcoustics(customization)
                speak(lines[nextIdx])
            }
        }
    }

    /**
     * Speaks understanding reminder when the user taps "Later".
     */
    fun speakRefused(customization: CustomizationSettings) {
        if (!isInitialized) return
        val style = customization.companionStyle
        val hero = customization.heroProtocol

        configureCompanionVoice(customization)

        when (style) {
            CompanionStyle.MITHRA -> {
                val text = customization.mithraDialogueRefused.ifBlank { "அப்பா அப்புறம் மறக்காம தண்ணி குடிங்க!" }
                val phonetic = "Appa, appuram marakkama thanni kudinga!"
                speakWithLocaleCheck(text, phonetic, customization)
            }
            CompanionStyle.MITHRA_MOM -> {
                val text = customization.momDialogueRefused.ifBlank { "வேலை முக்கியம் தான், ஆனா தண்ணி குடிக்க மறந்துடாதீங்க!" }
                val phonetic = "Velai mukkiyam thaan, aana thanni kudikka marandhidaadheenga!"
                speakWithLocaleCheck(text, phonetic, customization)
            }
            CompanionStyle.CUSTOM_SHEET -> {
                val text = customization.customDialogueRefused.ifBlank { "Take care of yourself, remember to drink later!" }
                speakWithLocaleCheck(text, text, customization)
            }
            CompanionStyle.HERO_SUIT -> {
                speakHeroRefusedInternal(hero, customization)
            }
            else -> {
                val lines = listOf(
                    "No worries. Take care of yourself soon, okay?",
                    "Standing down for now. Remember to hydrate when you can."
                )
                val key = "other_refused"
                val nextIdx = ((refusedIndexMap[key] ?: 0) + 1) % lines.size
                refusedIndexMap[key] = nextIdx
                applyAcoustics(customization)
                speak(lines[nextIdx])
            }
        }
    }

    // Backwards-compatible callers for older references
    fun speakHeroPrompt(hero: MarvelHeroProtocol, style: CompanionStyle) {
        speakPrompt(CustomizationSettings(heroProtocol = hero, companionStyle = style))
    }

    fun speakHeroAccepted(hero: MarvelHeroProtocol, style: CompanionStyle) {
        speakAccepted(CustomizationSettings(heroProtocol = hero, companionStyle = style))
    }

    fun speakHeroRefused(hero: MarvelHeroProtocol, style: CompanionStyle) {
        speakRefused(CustomizationSettings(heroProtocol = hero, companionStyle = style))
    }

    private fun speakHeroPromptInternal(hero: MarvelHeroProtocol, customization: CustomizationSettings) {
        val lines = when (hero) {
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

        val key = hero.id
        val nextIndex = ((promptIndexMap[key] ?: 0) + 1) % lines.size
        promptIndexMap[key] = nextIndex
        applyAcoustics(customization)
        speak(lines[nextIndex])
    }

    private fun speakHeroAcceptedInternal(hero: MarvelHeroProtocol, customization: CustomizationSettings) {
        val lines = when (hero) {
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

        val key = hero.id
        val nextIndex = ((acceptedIndexMap[key] ?: 0) + 1) % lines.size
        acceptedIndexMap[key] = nextIndex
        applyAcoustics(customization)
        speak(lines[nextIndex])
    }

    private fun speakHeroRefusedInternal(hero: MarvelHeroProtocol, customization: CustomizationSettings) {
        val lines = when (hero) {
            MarvelHeroProtocol.HULK -> listOf(
                "Hulk wait for you. Drink soon!",
                "Hulk sad, but Hulk be patient. Drink later, okay?"
            )
            MarvelHeroProtocol.IRON_MAN -> listOf(
                "Snoozing telemetry reminder. Don't push your reactor too hard, boss.",
                "Standing by on standby power. Drink soon, Jarvis out."
            )
            MarvelHeroProtocol.SPIDER_MAN -> listOf(
                "No worries! But promise me you'll drink some water later, deal?",
                "Catch ya on the flip side! Don't forget that water!"
            )
            else -> listOf(
                "No worries. Take care of yourself soon, okay?",
                "Standing down for now. Remember to hydrate when you can."
            )
        }

        val key = hero.id
        val nextIndex = ((refusedIndexMap[key] ?: 0) + 1) % lines.size
        refusedIndexMap[key] = nextIndex
        applyAcoustics(customization)
        speak(lines[nextIndex])
    }

    private fun configureCompanionVoice(customization: CustomizationSettings) {
        val engine = tts ?: return
        val style = customization.companionStyle
        val hero = customization.heroProtocol

        // 1. Dynamic TTS Voice selection (gender & language matching)
        try {
            val availableVoices = engine.voices
            if (!availableVoices.isNullOrEmpty()) {
                val femaleVoices = availableVoices.filter { voice ->
                    val n = voice.name.lowercase()
                    (n.contains("female") || voice.features.any { it.contains("female") })
                }

                val targetVoice: Voice? = when (style) {
                    CompanionStyle.MITHRA -> {
                        // For Mithra (young daughter), pick the first female voice or child-like timbre
                        femaleVoices.firstOrNull() ?: availableVoices.firstOrNull()
                    }
                    CompanionStyle.MITHRA_MOM -> {
                        // For Mom (mother/wife), pick second female voice if available to differentiate
                        if (femaleVoices.size > 1) femaleVoices[1] else femaleVoices.firstOrNull()
                    }
                    CompanionStyle.HERO_SUIT -> {
                        when (hero) {
                            MarvelHeroProtocol.BLACK_WIDOW -> {
                                femaleVoices.firstOrNull { it.locale.language == Locale.ENGLISH.language }
                                    ?: femaleVoices.firstOrNull()
                            }
                            MarvelHeroProtocol.IRON_MAN, MarvelHeroProtocol.DOCTOR_STRANGE -> {
                                availableVoices.firstOrNull { voice ->
                                    voice.locale == Locale.UK && voice.name.lowercase().contains("male")
                                } ?: availableVoices.firstOrNull { voice ->
                                    voice.name.lowercase().contains("male") && voice.locale.language == Locale.ENGLISH.language
                                }
                            }
                            else -> {
                                availableVoices.firstOrNull { voice ->
                                    val n = voice.name.lowercase()
                                    (n.contains("male") && !n.contains("female")) &&
                                    voice.locale.language == Locale.ENGLISH.language
                                }
                            }
                        }
                    }
                    else -> null
                }
                targetVoice?.let { engine.voice = it }
            }
        } catch (_: Throwable) {
            // Safe fallback
        }

        applyAcoustics(customization)
    }

    /**
     * Sets distinct character acoustic parameters (pitch and speech rate).
     * Called both during voice configuration and immediately after any language switch,
     * preventing Android TTS from resetting to default timbre.
     */
    private fun applyAcoustics(customization: CustomizationSettings) {
        val engine = tts ?: return
        val style = customization.companionStyle
        val hero = customization.heroProtocol

        val (pitch, rate) = when (style) {
            // Mithra: Playful, sweet toddler/daughter voice (high pitch, quick cadence)
            CompanionStyle.MITHRA -> Pair(1.72f, 1.08f)
            // Mithra's Mom: Warm, caring adult mother/wife voice (calm, natural maternal pitch)
            CompanionStyle.MITHRA_MOM -> Pair(1.06f, 0.94f)
            CompanionStyle.CUSTOM_SHEET -> Pair(1.05f, 1.02f)
            CompanionStyle.HERO_SUIT -> when (hero) {
                MarvelHeroProtocol.HULK -> Pair(0.48f, 0.72f)          // Deep booming titan
                MarvelHeroProtocol.THOR -> Pair(0.65f, 0.86f)          // Booming heroic Asgardian thunder
                MarvelHeroProtocol.SPIDER_MAN -> Pair(1.34f, 1.22f)    // Fast, youthful Peter Parker
                MarvelHeroProtocol.IRON_MAN -> Pair(1.06f, 1.10f)      // Crisp, witty tech billionaire
                MarvelHeroProtocol.CAPTAIN_AMERICA -> Pair(0.85f, 0.96f) // Clear commanding leader
                MarvelHeroProtocol.BLACK_PANTHER -> Pair(0.78f, 0.88f) // Deep, smooth Wakandan king
                MarvelHeroProtocol.DOCTOR_STRANGE -> Pair(0.90f, 0.92f)// Calm, mystical Sorcerer Supreme
                MarvelHeroProtocol.BLACK_WIDOW -> Pair(1.22f, 1.04f)   // Sharp, sleek operative
                MarvelHeroProtocol.AVENGERS_UNIFIED -> Pair(1.00f, 1.00f)
            }
            else -> Pair(1.05f, 1.05f)
        }
        engine.setPitch(pitch)
        engine.setSpeechRate(rate)
    }

    private fun speakWithLocaleCheck(text: String, phoneticFallback: String, customization: CustomizationSettings) {
        val engine = tts ?: return
        val isTamil = text.any { it in '\u0B80'..'\u0BFF' }

        if (isTamil) {
            val taLocale = Locale("ta", "IN")
            val taAvailable = try {
                engine.isLanguageAvailable(taLocale)
            } catch (_: Throwable) {
                TextToSpeech.LANG_NOT_SUPPORTED
            }

            if (taAvailable >= TextToSpeech.LANG_AVAILABLE) {
                try {
                    engine.language = taLocale
                    // Critical: reapply acoustics AFTER setting language, because setLanguage resets pitch & rate
                    applyAcoustics(customization)
                    speak(text)
                    return
                } catch (_: Throwable) {}
            }
            // Fallback to English phonetic pronunciation so the speaker still sounds clear
            try {
                engine.language = Locale.US
                applyAcoustics(customization)
            } catch (_: Throwable) {}
            speak(phoneticFallback)
        } else {
            try {
                engine.language = Locale.US
                applyAcoustics(customization)
            } catch (_: Throwable) {}
            speak(text)
        }
    }

    private fun playArrivalChime() {
        try {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 55)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
        } catch (_: Throwable) {}
    }

    companion object {
        /**
         * Sanitizes dialogue text before sending to TextToSpeech engine.
         * Completely strips emojis, symbols, and UI glyphs so the TTS engine never
         * speaks emoji names (e.g. "water droplet", "flexed biceps", "checkmark").
         */
        fun sanitizeForSpeech(text: String): String {
            // Strip unicode emoji symbols, dingbats, surrogate pairs, and pictographs
            val emojiRegex = Regex("[\\p{So}\\p{Sk}\\p{Cn}\\uD83C-\\uDBFF\\uDC00-\\uDFFF\\u2600-\\u27BF\\uFE00-\\uFE0F]+")
            val withoutEmojis = text.replace(emojiRegex, " ")
            // Strip non-spoken UI symbols like ✓, ✕, ⚡, ✦, ⚙, etc.
            val withoutSymbols = withoutEmojis.replace(Regex("[✓✕⚡✦⚙▶◀▲▼•·|/\\[\\]{}()#*~`_=+\\-<>]"), " ")
            return withoutSymbols.replace(Regex("\\s+"), " ").trim()
        }
    }

    private fun speak(text: String) {
        val sanitized = sanitizeForSpeech(text)
        if (sanitized.isBlank()) return
        tts?.speak(sanitized, TextToSpeech.QUEUE_FLUSH, null, "COMPANION_VOICE_${System.currentTimeMillis()}")
    }

    fun stop() {
        visemeJob?.cancel()
        isSpeaking = false
        currentViseme = VisemeShape.REST
        tts?.stop()
    }

    fun destroy() {
        visemeJob?.cancel()
        isSpeaking = false
        currentViseme = VisemeShape.REST
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
