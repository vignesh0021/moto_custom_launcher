package com.motorola.launcher.ui.components

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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.R
import com.motorola.launcher.system.rememberCompanionVoiceManager
import com.motorola.launcher.ui.model.CompanionStyle
import com.motorola.launcher.ui.model.MarvelHeroProtocol
import com.motorola.launcher.ui.theme.LocalCustomization
import com.motorola.launcher.ui.theme.StarkEmerald
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextSecondary
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * State of user interaction with the Marvel Superhero Screen Companion.
 */
enum class HydrationState {
    PROMPT,     // Hero arrives asking user to drink water
    ACCEPTED,   // User tapped "Drank it ✓" (Hero celebrates with signature action)
    REFUSED     // User tapped "Later" (Hero nods understandingly)
}

/**
 * Maps each Marvel Hero to its real 3D Front (dialogue/celebration) and Side (marching walk) drawables.
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
    }
}

/**
 * Real 3D Animated Marvel Superhero Screen Companion.
 * Walks all the way across the full width of the screen from left to right.
 *
 * Real Walking Gait Cycle:
 * - Traverses horizontally across the screen above the bottom dock.
 * - Side-profile stride with real stepping bob, stride rocking tilt, and expanding ground shadow.
 * - Pauses near screen center, smoothly transitions to the Front-facing 3D hero with floating speech HUD.
 * - Interactive responses: "Drank it ✓" (celebration jump, quote & sip count increment) or "Later".
 * - Resumes side-profile walk across to the right edge and exits off-screen.
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

    val view = LocalView.current
    val customization = LocalCustomization.current
    val companionStyle = customization.companionStyle
    val hero = customization.heroProtocol
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val voiceManager = rememberCompanionVoiceManager()

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
            voiceManager.speakHeroPrompt(hero, companionStyle)
        }
    }

    // Footstep haptics (Hulk ground thud)
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
            // 1. Floating Superhero Speech Bubble (Appears when paused at screen center)
            AnimatedVisibility(
                visible = !isWalking,
                enter = fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.85f),
                exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.85f)
            ) {
                HeroSpeechBubble(
                    hero = hero,
                    style = companionStyle,
                    state = state,
                    glassesCount = glassesCount,
                    accentColor = hero.primary,
                    onAccept = {
                        if (customization.hapticFeedback) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        }
                        state = HydrationState.ACCEPTED
                        onAcceptWater()
                        if (customization.companionVoiceEnabled) {
                            voiceManager.speakHeroAccepted(hero, companionStyle)
                        }

                        coroutineScope.launch {
                            // Signature celebration bounce / flex
                            launch {
                                celebrationBounce.animateTo(-24f, tween(160, easing = FastOutSlowInEasing))
                                celebrationBounce.animateTo(0f, tween(160, easing = FastOutSlowInEasing))
                                celebrationBounce.animateTo(-12f, tween(130, easing = FastOutSlowInEasing))
                                celebrationBounce.animateTo(0f, tween(130, easing = FastOutSlowInEasing))
                            }
                            delay(2000)
                            // Resume walking all the way across to the right edge and exit
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
                            voiceManager.speakHeroRefused(hero, companionStyle)
                        }

                        coroutineScope.launch {
                            delay(1200)
                            exitWalkOff(walkXDp, walkPhase, exitX, onDismiss)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Real 3D Character Avatar with Speech Pointer Tail
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
                                celebrationBounce.animateTo(-16f, tween(120))
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

                // Character Sprite Presentation
                val (frontRes, sideRes) = getHeroDrawables(hero)
                val isHulk = hero == MarvelHeroProtocol.HULK
                val spriteW = if (isHulk) 80.dp else 70.dp
                val spriteH = if (isHulk) 124.dp else 115.dp

                Box(
                    modifier = Modifier.size(width = spriteW, height = spriteH),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Soft Ground Contact Shadow (expands & contracts with stride)
                    val shadowScale = if (isWalking) 1f + 0.14f * abs(sin(walkPhase.value)) else 1f
                    Canvas(
                        modifier = Modifier
                            .size(width = if (isHulk) 56.dp else 46.dp, height = 8.dp)
                            .graphicsLayer { scaleX = shadowScale }
                    ) {
                        drawOval(
                            color = Color(0x66000000),
                            topLeft = Offset.Zero,
                            size = size
                        )
                    }

                    // Render Real 3D Model Image if HERO_SUIT, else fallback to procedural
                    if (companionStyle == CompanionStyle.HERO_SUIT) {
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
                        // Alternate Tailored Jacket / South Indian style
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
 * Superhero Speech Bubble with character-authentic reminder dialogues.
 */
@Composable
private fun HeroSpeechBubble(
    hero: MarvelHeroProtocol,
    style: CompanionStyle,
    state: HydrationState,
    glassesCount: Int,
    accentColor: Color,
    onAccept: () -> Unit,
    onLater: () -> Unit
) {
    val bubbleShape = RoundedCornerShape(18.dp)
    val stateBorderColor = when (state) {
        HydrationState.PROMPT -> accentColor.copy(alpha = 0.9f)
        HydrationState.ACCEPTED -> StarkEmerald
        HydrationState.REFUSED -> Color(0x99FFAA00)
    }

    Box(
        modifier = Modifier
            .widthIn(min = 230.dp, max = 285.dp)
            .shadow(16.dp, bubbleShape)
            .clip(bubbleShape)
            .background(Color(0xF509101E))
            .border(1.5.dp, stateBorderColor, bubbleShape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            // Header: Hero Identifier & Daily Sips
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
                    Text(
                        text = if (style == CompanionStyle.HERO_SUIT) hero.heroName.uppercase() else "PIXEL FRIEND",
                        color = accentColor,
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
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

            // Character dialogue from sheet personalities
            val (titleText, bodyText) = when (state) {
                HydrationState.PROMPT -> when {
                    style != CompanionStyle.HERO_SUIT -> Pair(
                        "Hydration check, bestie 💧",
                        "Big ideas need a little water. Take a sip?"
                    )
                    hero == MarvelHeroProtocol.HULK -> Pair(
                        "HULK HYDRATION CHECK! 💧",
                        "Hulk say: Drink water now! Big muscles need water to smash!"
                    )
                    hero == MarvelHeroProtocol.IRON_MAN -> Pair(
                        "Coolant levels critical 💧",
                        "Arc reactor needs regular fluid. Take a fresh sip of water!"
                    )
                    hero == MarvelHeroProtocol.CAPTAIN_AMERICA -> Pair(
                        "Hydration check, soldier! 💧",
                        "Peak readiness requires regular water. Keep your discipline sharp!"
                    )
                    hero == MarvelHeroProtocol.THOR -> Pair(
                        "Hear the Thunderer! ⚡💧",
                        "A warrior must never face battle parched! Drink of the water of life!"
                    )
                    hero == MarvelHeroProtocol.BLACK_PANTHER -> Pair(
                        "Wakanda reminder 💧",
                        "Pure water sustains both the body and the warrior spirit. Drink now."
                    )
                    hero == MarvelHeroProtocol.DOCTOR_STRANGE -> Pair(
                        "Mystic equilibrium 💧",
                        "Mental clarity across all dimensions begins with hydration. Drink water!"
                    )
                    hero == MarvelHeroProtocol.SPIDER_MAN -> Pair(
                        "Friendly neighborhood check! 💧",
                        "Web-shooters need fluid and so do you! Time for a quick water break!"
                    )
                    hero == MarvelHeroProtocol.BLACK_WIDOW -> Pair(
                        "Hydration protocol 💧",
                        "Sharp reflexes start with staying hydrated. Take a sip, operative."
                    )
                    else -> Pair("Hydration check! 💧", "Time to drink a glass of water!")
                }
                HydrationState.ACCEPTED -> when {
                    hero == MarvelHeroProtocol.HULK && style == CompanionStyle.HERO_SUIT -> Pair(
                        "HULK SMASH DEHYDRATION! 💚",
                        "Good job friend! Hulk happy! Muscles stay strong!"
                    )
                    hero == MarvelHeroProtocol.THOR && style == CompanionStyle.HERO_SUIT -> Pair(
                        "ANOTHER! ⚡",
                        "A mighty sip worthy of the golden halls of Valhalla!"
                    )
                    hero == MarvelHeroProtocol.SPIDER_MAN && style == CompanionStyle.HERO_SUIT -> Pair(
                        "Web-shooters reloaded! 🕷️",
                        "Woohoo! 100% friend energy! You rock!"
                    )
                    else -> Pair(
                        "Heck yeah! 🎉",
                        "100% friend energy. Vitals operating at peak efficiency!"
                    )
                }
                HydrationState.REFUSED -> when {
                    hero == MarvelHeroProtocol.HULK && style == CompanionStyle.HERO_SUIT -> Pair(
                        "Hulk wait for you! 😤",
                        "Hulk will be back soon! Drink when ready!"
                    )
                    else -> Pair(
                        "No worries! 🫡",
                        "Take care of yourself soon, okay?"
                    )
                }
            }

            Text(
                text = titleText,
                color = TextPrimary,
                fontSize = 13.5.sp,
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
                            text = "Drank it ✓",
                            color = StarkEmerald,
                            fontSize = 11.5.sp,
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
                            text = "Later",
                            color = TextSecondary,
                            fontSize = 11.5.sp,
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
