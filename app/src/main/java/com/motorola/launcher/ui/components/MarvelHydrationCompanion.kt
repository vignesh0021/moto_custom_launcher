package com.motorola.launcher.ui.components

import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.R
import com.motorola.launcher.data.companion.CharacterCompanionSprites
import com.motorola.launcher.data.companion.CharacterExpression
import com.motorola.launcher.data.companion.CharacterPose
import com.motorola.launcher.data.companion.CharacterSheetEngine
import com.motorola.launcher.system.rememberCompanionVoiceManager
import com.motorola.launcher.ui.model.CompanionStyle
import com.motorola.launcher.ui.model.CustomizationSettings
import com.motorola.launcher.ui.model.MarvelHeroProtocol
import com.motorola.launcher.ui.theme.LocalCustomization
import com.motorola.launcher.ui.theme.StarkEmerald
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * State of user interaction with the Marvel Superhero or Character Companion.
 */
enum class HydrationState {
    PROMPT,     // Companion arrives asking user to drink water
    ACCEPTED,   // User tapped "Drank it ✓" (celebrates with signature action & dialogue)
    REFUSED     // User tapped "Later" (nods understandingly / warning reminder)
}

/**
 * Maps each Marvel Hero to its 3D Front (dialogue/celebration) and Side (marching walk) drawables.
 */
private fun getHeroDrawables(hero: MarvelHeroProtocol): Pair<Int, Int> {
    return when (hero) {
        MarvelHeroProtocol.IRON_MAN -> Pair(R.drawable.hero_iron_man_front, R.drawable.hero_iron_man_side)
        MarvelHeroProtocol.CAPTAIN_AMERICA -> Pair(R.drawable.hero_captain_america_front, R.drawable.hero_captain_america_side)
        MarvelHeroProtocol.THOR -> Pair(R.drawable.hero_thor_front, R.drawable.hero_thor_side)
        MarvelHeroProtocol.BLACK_PANTHER -> Pair(R.drawable.hero_black_panther_front, R.drawable.hero_black_panther_side)
        MarvelHeroProtocol.HULK -> Pair(R.drawable.hero_hulk_front, R.drawable.hero_hulk_side)
        MarvelHeroProtocol.DOCTOR_STRANGE -> Pair(R.drawable.hero_doctor_strange_front, R.drawable.hero_doctor_strange_side)
        MarvelHeroProtocol.SPIDER_MAN -> Pair(R.drawable.hero_spider_man_front, R.drawable.hero_spider_man_side)
        MarvelHeroProtocol.BLACK_WIDOW -> Pair(R.drawable.hero_black_widow_front, R.drawable.hero_black_widow_side)
        MarvelHeroProtocol.AVENGERS_UNIFIED -> Pair(R.drawable.hero_iron_man_front, R.drawable.hero_iron_man_side)
    }
}

/**
 * Real Multi-Frame Animated Character & Superhero Companion.
 * Walks horizontally across the full width of the screen above the bottom dock.
 *
 * Real Walk Gait Cycle:
 * - Multi-frame animation: cycles between SIDE stride and 3/4 turn stride during walk phases.
 * - Dynamic stepping bob and stride contact ground shadow.
 * - Pauses near screen center and transitions to FRONT turnaround or dialogue facial expression.
 * - Expressive responses with Tamil & English editable speech bubble and acoustic voice feedback.
 * - Resumes walking sequence across to the right edge and exits off-screen.
 */
@Composable
fun MarvelHydrationCompanion(
    visible: Boolean,
    glassesCount: Int,
    onAcceptWater: () -> Unit,
    onRefuseWater: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val context = LocalContext.current
    val view = LocalView.current
    val customization = LocalCustomization.current
    val companionStyle = customization.companionStyle
    val hero = customization.heroProtocol
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val voiceManager = rememberCompanionVoiceManager()

    val sheetEngine = remember(context) { CharacterSheetEngine.getInstance(context) }
    val isCustomSheetCompanion = companionStyle == CompanionStyle.MITHRA ||
            companionStyle == CompanionStyle.MITHRA_MOM ||
            companionStyle == CompanionStyle.CUSTOM_SHEET

    val companionSprites by produceState<CharacterCompanionSprites?>(
        initialValue = null,
        companionStyle,
        customization.customSheetUri
    ) {
        if (isCustomSheetCompanion) {
            value = withContext(Dispatchers.IO) {
                sheetEngine.getSprites(companionStyle, customization.customSheetUri)
            }
        } else {
            value = null
        }
    }

    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val startX = -110f
    val pauseX = (screenWidthDp * 0.38f)
    val exitX = screenWidthDp + 130f

    var state by remember { mutableStateOf(HydrationState.PROMPT) }

    // Horizontal position across screen (in dp)
    val walkXDp = remember { Animatable(startX) }
    // Walking stride phase (0 to 2π)
    val walkPhase = remember { Animatable(0f) }
    // Vertical celebration jump
    val celebrationBounce = remember { Animatable(0f) }
    var eyeBlink by remember { mutableStateOf(false) }

    // Periodic eye-blinking loop
    LaunchedEffect(visible) {
        while (visible) {
            delay(2800)
            eyeBlink = true
            delay(150)
            eyeBlink = false
        }
    }

    // Full-screen walk across sequence (Left to Right)
    LaunchedEffect(visible) {
        state = HydrationState.PROMPT
        walkXDp.snapTo(startX)
        celebrationBounce.snapTo(0f)
        walkPhase.snapTo(0f)

        // 1. Gait cycle while walking across
        val gaitJob = launch {
            while (isActive) {
                walkPhase.animateTo(
                    targetValue = walkPhase.value + (PI.toFloat() * 2f),
                    animationSpec = tween(durationMillis = 360, easing = LinearEasing)
                )
            }
        }

        // 2. Walk smoothly from left off-screen to screen center
        walkXDp.animateTo(
            targetValue = pauseX,
            animationSpec = tween(durationMillis = 2800, easing = LinearEasing)
        )
        gaitJob.cancel()
        walkPhase.snapTo(0f)

        // 3. Auto-dismiss after 25 seconds if untouched to keep screen clean
        delay(25000)
        if (state == HydrationState.PROMPT) {
            onRefuseWater()
            exitWalkOff(walkXDp, walkPhase, exitX, onDismiss)
        }
    }

    val isWalking = abs(walkXDp.value - pauseX) > 12f
    val stepBob = if (isWalking) abs(sin(walkPhase.value)) * 6f else 0f
    val strideTilt = if (isWalking) sin(walkPhase.value) * 4.5f else 0f

    // Voice announcement on arrival at screen center
    LaunchedEffect(isWalking) {
        if (!isWalking && state == HydrationState.PROMPT && customization.companionVoiceEnabled) {
            voiceManager.speakPrompt(customization)
        }
    }

    // Footstep haptics
    LaunchedEffect(walkPhase.value) {
        if (isWalking && hero == MarvelHeroProtocol.HULK && customization.hapticFeedback) {
            if (cos(walkPhase.value) > 0.92f) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        }
    }

    // Floating companion container anchored above the dock
    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomStart
    ) {
        val currentX = walkXDp.value.dp

        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .offset { IntOffset(currentX.roundToPx(), 0) }
                .padding(bottom = 98.dp)
        ) {
            // 1. Floating Speech Bubble (Appears when paused at screen center)
            AnimatedVisibility(
                visible = !isWalking,
                enter = fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.85f),
                exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.85f)
            ) {
                HeroSpeechBubble(
                    customization = customization,
                    state = state,
                    glassesCount = glassesCount,
                    accentColor = if (isCustomSheetCompanion) Color(0xFF00E5FF) else hero.primary,
                    onAccept = {
                        if (customization.hapticFeedback) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        }
                        state = HydrationState.ACCEPTED
                        onAcceptWater()
                        if (customization.companionVoiceEnabled) {
                            voiceManager.speakAccepted(customization)
                        }

                        coroutineScope.launch {
                            // Celebration bounce
                            launch {
                                celebrationBounce.animateTo(-26f, tween(160, easing = FastOutSlowInEasing))
                                celebrationBounce.animateTo(0f, tween(160, easing = FastOutSlowInEasing))
                                celebrationBounce.animateTo(-14f, tween(130, easing = FastOutSlowInEasing))
                                celebrationBounce.animateTo(0f, tween(130, easing = FastOutSlowInEasing))
                            }
                            delay(2200)
                            exitWalkOff(walkXDp, walkPhase, exitX, onDismiss)
                        }
                    },
                    onLater = {
                        if (customization.hapticFeedback) {
                            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                        }
                        state = HydrationState.REFUSED
                        onRefuseWater()
                        if (customization.companionVoiceEnabled) {
                            voiceManager.speakRefused(customization)
                        }

                        coroutineScope.launch {
                            delay(1400)
                            exitWalkOff(walkXDp, walkPhase, exitX, onDismiss)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Real Character Avatar presentation
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .offset { IntOffset(0, (celebrationBounce.value - stepBob).toInt()) }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (customization.hapticFeedback) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                            coroutineScope.launch {
                                celebrationBounce.animateTo(-18f, tween(120))
                                celebrationBounce.animateTo(0f, tween(120))
                            }
                        }
                    )
            ) {
                // Speech Pointer Arrow
                AnimatedVisibility(
                    visible = !isWalking,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Canvas(modifier = Modifier.size(width = 16.dp, height = 8.dp)) {
                        val path = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width, 0f)
                            lineTo(size.width / 2f, size.height)
                            close()
                        }
                        drawPath(path, color = Color(0xF209101E))
                    }
                }

                // Sprite Presentation Dimensions
                // Character Dimensions (Real Video for Mithra & Mom, 3D Hero models, Sprites for Custom Sheet)
                val isMithra = companionStyle == CompanionStyle.MITHRA
                val isMom = companionStyle == CompanionStyle.MITHRA_MOM
                val isVideoCompanion = isMithra || isMom
                val isHulk = hero == MarvelHeroProtocol.HULK && companionStyle == CompanionStyle.HERO_SUIT

                val spriteW = when {
                    isMithra -> 108.dp
                    isMom -> 114.dp
                    isHulk -> 82.dp
                    else -> 70.dp
                }
                val spriteH = when {
                    isMithra -> 176.dp
                    isMom -> 184.dp
                    isHulk -> 124.dp
                    else -> 115.dp
                }

                Box(
                    modifier = Modifier.size(width = spriteW, height = spriteH),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Soft Ground Contact Shadow (expands & contracts with walk stride)
                    val shadowScale = if (isWalking) 1f + 0.15f * abs(sin(walkPhase.value)) else 1f
                    Canvas(
                        modifier = Modifier
                            .size(width = if (isVideoCompanion) 72.dp else if (isHulk) 56.dp else 46.dp, height = 8.dp)
                            .graphicsLayer { scaleX = shadowScale }
                    ) {
                        drawOval(
                            color = Color(0x66000000),
                            topLeft = Offset.Zero,
                            size = size
                        )
                    }

                    // Render Character (Real Hardware-Accelerated Video for Mithra & Mom, 3D model for Heroes, Sprites for Custom Sheet)
                    if (isVideoCompanion) {
                        val rawId = if (isMithra) R.raw.video_mithra else R.raw.video_mithra_mom
                        val videoBorder = if (isMithra) Color(0xFF00E5FF) else Color(0xFFFFD54F)
                        VideoCompanionPlayer(
                            rawResId = rawId,
                            cornerRadius = 16.dp,
                            borderColor = videoBorder.copy(alpha = 0.85f),
                            isMuted = !customization.companionVoiceEnabled,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (isCustomSheetCompanion && companionSprites != null) {
                        // Multi-Frame Walk Cycle & Dynamic Facial Expression Engine
                        val activeBitmap: Bitmap? = if (isWalking) {
                            sheetEngine.getWalkFrame(companionSprites, walkPhase.value)
                        } else {
                            when (state) {
                                HydrationState.PROMPT -> {
                                    companionSprites.poses[CharacterPose.FRONT]
                                        ?: companionSprites.expressions[CharacterExpression.CURIOUS]
                                }
                                HydrationState.ACCEPTED -> {
                                    companionSprites.expressions[CharacterExpression.HAPPY]
                                        ?: companionSprites.poses[CharacterPose.FRONT]
                                }
                                HydrationState.REFUSED -> {
                                    companionSprites.expressions[CharacterExpression.UPSET]
                                        ?: companionSprites.poses[CharacterPose.FRONT]
                                }
                            }
                        }

                        if (activeBitmap != null) {
                            Image(
                                bitmap = activeBitmap.asImageBitmap(),
                                contentDescription = companionStyle.displayName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        if (isWalking) {
                                            rotationZ = sin(walkPhase.value) * 3f
                                        }
                                    }
                            )
                        } else {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawMarvel3DCharacter(
                                    hero = hero,
                                    style = companionStyle,
                                    walkPhase = walkPhase.value,
                                    isBlinking = eyeBlink,
                                    isCelebrating = state == HydrationState.ACCEPTED,
                                    isDisappointed = state == HydrationState.REFUSED
                                )
                            }
                        }
                    } else if (companionStyle == CompanionStyle.HERO_SUIT) {
                        // Real 3D Marvel Hero Model
                        val (frontRes, sideRes) = getHeroDrawables(hero)
                        val activeDrawable = if (isWalking) sideRes else frontRes
                        Image(
                            painter = painterResource(id = activeDrawable),
                            contentDescription = hero.heroName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    rotationZ = strideTilt
                                }
                        )
                    } else {
                        // Procedural fallback
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawMarvel3DCharacter(
                                hero = hero,
                                style = companionStyle,
                                walkPhase = walkPhase.value,
                                isBlinking = eyeBlink,
                                isCelebrating = state == HydrationState.ACCEPTED,
                                isDisappointed = state == HydrationState.REFUSED
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Handles smooth walking exit off-screen to the right.
 */
private suspend fun exitWalkOff(
    walkXDp: Animatable<Float, *>,
    walkPhase: Animatable<Float, *>,
    exitX: Float,
    onDismiss: () -> Unit
) = coroutineScope {
    val gaitJob = launch {
        while (isActive) {
            walkPhase.animateTo(
                targetValue = walkPhase.value + (PI.toFloat() * 2f),
                animationSpec = tween(durationMillis = 340, easing = LinearEasing)
            )
        }
    }

    walkXDp.animateTo(
        targetValue = exitX,
        animationSpec = tween(durationMillis = 2400, easing = LinearEasing)
    )
    gaitJob.cancel()
    walkPhase.snapTo(0f)
    onDismiss()
}

/**
 * Companion Speech Bubble with Tamil and English customizable dialogues.
 */
@Composable
private fun HeroSpeechBubble(
    customization: CustomizationSettings,
    state: HydrationState,
    glassesCount: Int,
    accentColor: Color,
    onAccept: () -> Unit,
    onLater: () -> Unit
) {
    val hero = customization.heroProtocol
    val style = customization.companionStyle
    val bubbleShape = RoundedCornerShape(18.dp)
    val stateBorderColor = when (state) {
        HydrationState.PROMPT -> accentColor.copy(alpha = 0.9f)
        HydrationState.ACCEPTED -> StarkEmerald
        HydrationState.REFUSED -> Color(0x99FFAA00)
    }

    Box(
        modifier = Modifier
            .widthIn(min = 230.dp, max = 295.dp)
            .shadow(16.dp, bubbleShape)
            .clip(bubbleShape)
            .background(Color(0xF509101E))
            .border(1.5.dp, stateBorderColor, bubbleShape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            // Header: Companion Identifier & Daily Sips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (state == HydrationState.ACCEPTED) StarkEmerald else accentColor)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    val headerText = when (style) {
                        CompanionStyle.MITHRA -> "MITHRA // மலர் பாப்பா"
                        CompanionStyle.MITHRA_MOM -> "MOM // மனைவி"
                        CompanionStyle.CUSTOM_SHEET -> "CUSTOM COMPANION"
                        CompanionStyle.HERO_SUIT -> hero.heroName.uppercase()
                        else -> "PIXEL FRIEND"
                    }
                    Text(
                        text = headerText,
                        color = accentColor,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    )
                }

                Text(
                    text = "💧 $glassesCount SIPS",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dynamic editable dialogue text
            val (titleText, bodyText) = when (state) {
                HydrationState.PROMPT -> when (style) {
                    CompanionStyle.MITHRA -> Pair(
                        "மலர் பாப்பா (Mithra) 🌸",
                        customization.mithraDialoguePrompt
                    )
                    CompanionStyle.MITHRA_MOM -> Pair(
                        "மனைவி (Mithra's Mom) 💛",
                        customization.momDialoguePrompt
                    )
                    CompanionStyle.CUSTOM_SHEET -> Pair(
                        "Hydration Check 💧",
                        customization.customDialoguePrompt
                    )
                    CompanionStyle.HERO_SUIT -> when (hero) {
                        MarvelHeroProtocol.HULK -> Pair(
                            "HULK HYDRATION CHECK! 💧",
                            "Hulk say: Drink water now! Big muscles need water to smash!"
                        )
                        MarvelHeroProtocol.IRON_MAN -> Pair(
                            "Coolant levels critical 💧",
                            "Arc reactor needs regular fluid. Take a fresh sip of water!"
                        )
                        MarvelHeroProtocol.CAPTAIN_AMERICA -> Pair(
                            "Hydration check, soldier! 💧",
                            "Peak readiness requires regular water. Keep your discipline sharp!"
                        )
                        MarvelHeroProtocol.THOR -> Pair(
                            "Hear the Thunderer! ⚡💧",
                            "A warrior must never face battle parched! Drink of the water of life!"
                        )
                        MarvelHeroProtocol.BLACK_PANTHER -> Pair(
                            "Wakanda reminder 💧",
                            "Pure water sustains both the body and the warrior spirit. Drink now."
                        )
                        MarvelHeroProtocol.DOCTOR_STRANGE -> Pair(
                            "Mystic equilibrium 💧",
                            "Mental clarity across all dimensions begins with hydration. Drink water!"
                        )
                        MarvelHeroProtocol.SPIDER_MAN -> Pair(
                            "Friendly neighborhood check! 💧",
                            "Web-shooters need fluid and so do you! Time for a quick water break!"
                        )
                        MarvelHeroProtocol.BLACK_WIDOW -> Pair(
                            "Hydration protocol 💧",
                            "Sharp reflexes start with staying hydrated. Take a sip, operative."
                        )
                        else -> Pair("Hydration check! 💧", "Time to drink a glass of water!")
                    }
                    else -> Pair("Hydration check, bestie 💧", "Big ideas need a little water. Take a sip?")
                }
                HydrationState.ACCEPTED -> when (style) {
                    CompanionStyle.MITHRA -> Pair(
                        "அப்பா சூப்பர்! 🎉",
                        customization.mithraDialogueAccepted
                    )
                    CompanionStyle.MITHRA_MOM -> Pair(
                        "ரொம்ப நல்லதுங்க! ✨",
                        customization.momDialogueAccepted
                    )
                    CompanionStyle.CUSTOM_SHEET -> Pair(
                        "Awesome! 🌟",
                        customization.customDialogueAccepted
                    )
                    CompanionStyle.HERO_SUIT -> when (hero) {
                        MarvelHeroProtocol.HULK -> Pair(
                            "HULK SMASH DEHYDRATION! 💚",
                            "Good job friend! Hulk happy! Muscles stay strong!"
                        )
                        MarvelHeroProtocol.THOR -> Pair(
                            "ANOTHER! ⚡",
                            "A mighty sip worthy of the golden halls of Valhalla!"
                        )
                        MarvelHeroProtocol.SPIDER_MAN -> Pair(
                            "Web-shooters reloaded! 🕷️",
                            "Woohoo! 100% friend energy! You rock!"
                        )
                        else -> Pair(
                            "Heck yeah! 🎉",
                            "100% friend energy. Vitals operating at peak efficiency!"
                        )
                    }
                    else -> Pair(
                        "Heck yeah! 🎉",
                        "100% friend energy. Vitals operating at peak efficiency!"
                    )
                }
                HydrationState.REFUSED -> when (style) {
                    CompanionStyle.MITHRA -> Pair(
                        "அப்புறம் குடிங்க 🥺",
                        customization.mithraDialogueRefused
                    )
                    CompanionStyle.MITHRA_MOM -> Pair(
                        "மறந்துடாதீங்க 😤",
                        customization.momDialogueRefused
                    )
                    CompanionStyle.CUSTOM_SHEET -> Pair(
                        "Remember later 🫡",
                        customization.customDialogueRefused
                    )
                    CompanionStyle.HERO_SUIT -> when (hero) {
                        MarvelHeroProtocol.HULK -> Pair(
                            "Hulk wait for you! 😤",
                            "Hulk will be back soon! Drink when ready!"
                        )
                        else -> Pair(
                            "No worries! 🫡",
                            "Take care of yourself soon, okay?"
                        )
                    }
                    else -> Pair(
                        "No worries! 🫡",
                        "Take care of yourself soon, okay?"
                    )
                }
            }

            Text(
                text = titleText,
                color = TextPrimary,
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = bodyText,
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                lineHeight = 15.sp
            )

            // Interactive Buttons (Only in PROMPT state)
            if (state == HydrationState.PROMPT) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val acceptText = when (style) {
                        CompanionStyle.MITHRA, CompanionStyle.MITHRA_MOM -> "குடிச்சிட்டேன் ✓"
                        else -> "Drank it ✓"
                    }
                    val laterText = when (style) {
                        CompanionStyle.MITHRA, CompanionStyle.MITHRA_MOM -> "அப்புறம்"
                        else -> "Later"
                    }

                    // "Drank it ✓" Button
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(StarkEmerald.copy(alpha = 0.22f))
                            .border(1.dp, StarkEmerald, RoundedCornerShape(10.dp))
                            .clickable(onClick = onAccept)
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = acceptText,
                            color = StarkEmerald,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // "Later" Button
                    Box(
                        modifier = Modifier
                            .weight(0.9f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x18FFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                            .clickable(onClick = onLater)
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = laterText,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Procedural fallback renderer for tailored / alternate outfits.
 */
private fun DrawScope.drawMarvel3DCharacter(
    hero: MarvelHeroProtocol,
    style: CompanionStyle,
    walkPhase: Float,
    isBlinking: Boolean,
    isCelebrating: Boolean,
    isDisappointed: Boolean
) {
    val cx = size.width / 2f
    val isHulk = hero == MarvelHeroProtocol.HULK && style == CompanionStyle.HERO_SUIT

    val headRadius = if (isHulk) 16.5f else 15f
    val headY = if (isHulk) 21f else 20f
    val bodyTop = if (isHulk) 36f else 35f
    val bodyBottom = if (isHulk) 65f else 64f
    val footY = size.height - 4f

    // Ambient Ground Shadow
    val shadowW = if (isHulk) 48f else 40f
    drawOval(
        color = Color(0x4D000000),
        topLeft = Offset(cx - shadowW / 2f, size.height - 7f),
        size = Size(shadowW, 6f)
    )

    // Walking Gait Calculations
    val leftLegSwing = sin(walkPhase) * 10f
    val rightLegSwing = -sin(walkPhase) * 10f
    val leftFootLift = (cos(walkPhase).coerceAtLeast(0f)) * 3f
    val rightFootLift = ((-cos(walkPhase)).coerceAtLeast(0f)) * 3f

    val leftFootX = cx - (if (isHulk) 12f else 9f) + leftLegSwing
    val rightFootX = cx + (if (isHulk) 12f else 9f) + rightLegSwing

    // Cape (Thor / Doctor Strange)
    if (style == CompanionStyle.HERO_SUIT && (hero == MarvelHeroProtocol.THOR || hero == MarvelHeroProtocol.DOCTOR_STRANGE)) {
        val capeColor = if (hero == MarvelHeroProtocol.THOR) Color(0xFFB91C1C) else Color(0xFF991B1B)
        val capePath = Path().apply {
            moveTo(cx - 15f, bodyTop + 2f)
            lineTo(cx - 24f, size.height - 12f)
            lineTo(cx + 24f, size.height - 12f)
            lineTo(cx + 15f, bodyTop + 2f)
            close()
        }
        drawPath(capePath, color = capeColor)
        if (hero == MarvelHeroProtocol.DOCTOR_STRANGE) {
            drawLine(Color(0xFFFBBF24), Offset(cx - 24f, size.height - 12f), Offset(cx + 24f, size.height - 12f), strokeWidth = 2.5f)
        }
    }

    // Legs
    val trouserColor = Color(0xFF1E293B)
    drawLine(trouserColor, Offset(cx - 6f, bodyBottom), Offset(leftFootX, footY - 3f - leftFootLift), strokeWidth = 6.5f)
    drawLine(trouserColor, Offset(cx + 6f, bodyBottom), Offset(rightFootX, footY - 3f - rightFootLift), strokeWidth = 6.5f)

    // Shoes
    val shoeColor = Color(0xFF0F172A)
    drawRoundRect(shoeColor, topLeft = Offset(leftFootX - 5.5f, footY - 5f - leftFootLift), size = Size(11f, 5.5f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f))
    drawRoundRect(shoeColor, topLeft = Offset(rightFootX - 5.5f, footY - 5f - rightFootLift), size = Size(11f, 5.5f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f))

    // Body
    val bodyW = 28f
    val bodyH = bodyBottom - bodyTop
    val jacketColor = Color(0xFF854D0E)
    drawRoundRect(jacketColor, topLeft = Offset(cx - bodyW / 2f, bodyTop), size = Size(bodyW, bodyH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f))

    // Arms
    val leftArmSwing = -leftLegSwing
    val rightArmSwing = -rightLegSwing
    drawLine(jacketColor, Offset(cx - bodyW / 2f + 1f, bodyTop + 4f), Offset(cx - bodyW / 2f - 4f - leftArmSwing, bodyTop + 20f), strokeWidth = 5.5f)
    drawLine(jacketColor, Offset(cx + bodyW / 2f - 1f, bodyTop + 4f), Offset(cx + bodyW / 2f + 4f - rightArmSwing, bodyTop + 20f), strokeWidth = 5.5f)

    // Head
    val skin = Color(0xFFFFD1A4)
    drawCircle(skin, radius = headRadius, center = Offset(cx, headY))

    // Hair
    val hairColor = Color(0xFF1F1D1B)
    val hairPath = Path().apply {
        moveTo(cx - headRadius, headY - 1f)
        cubicTo(cx - headRadius, headY - headRadius - 6f, cx + headRadius, headY - headRadius - 6f, cx + headRadius, headY - 1f)
        lineTo(cx + headRadius - 3f, headY - 5f)
        lineTo(cx - headRadius + 3f, headY - 5f)
        close()
    }
    drawPath(hairPath, color = hairColor)

    // Eyes
    val eyeColor = Color(0xFF1E293B)
    if (isBlinking) {
        drawLine(eyeColor, Offset(cx - 6f, headY), Offset(cx - 2f, headY), strokeWidth = 1.8f)
        drawLine(eyeColor, Offset(cx + 2f, headY), Offset(cx + 6f, headY), strokeWidth = 1.8f)
    } else {
        drawCircle(eyeColor, radius = 2.2f, center = Offset(cx - 4.5f, headY))
        drawCircle(Color.White, radius = 0.8f, center = Offset(cx - 5f, headY - 0.7f))
        drawCircle(eyeColor, radius = 2.2f, center = Offset(cx + 4.5f, headY))
        drawCircle(Color.White, radius = 0.8f, center = Offset(cx + 4f, headY - 0.7f))
    }

    // Smile
    val smilePath = Path().apply {
        moveTo(cx - 3.5f, headY + 5.5f)
        quadraticTo(cx, headY + 8f, cx + 3.5f, headY + 5.5f)
    }
    drawPath(smilePath, color = Color(0xFF78350F), style = Stroke(width = 1.5f))
}
