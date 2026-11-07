package com.motorola.launcher.ui

import com.motorola.launcher.ui.gesture.GestureWorldState
import com.motorola.launcher.ui.model.CustomizationSettings
import com.motorola.launcher.ui.model.MarvelHeroProtocol
import com.motorola.launcher.ui.model.ThemeColorPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureWorldEngineTest {

    @Test
    fun gestureWorldState_heroCyclingPinch_cyclesThroughMarvelHeroes() {
        val state = GestureWorldState(initialHero = MarvelHeroProtocol.IRON_MAN)
        assertEquals(MarvelHeroProtocol.IRON_MAN, state.currentHero)

        // Pinch in (scale < 0.88f) cycles forward
        var selectedHero: MarvelHeroProtocol? = null
        state.onPinch(0.80f) { selectedHero = it }
        assertEquals(MarvelHeroProtocol.SPIDER_MAN, state.currentHero)
        assertEquals(MarvelHeroProtocol.SPIDER_MAN, selectedHero)

        // Pinch out (scale > 1.12f) cycles backward
        state.onPinch(1.20f) { selectedHero = it }
        assertEquals(MarvelHeroProtocol.IRON_MAN, state.currentHero)
    }

    @Test
    fun gestureWorldState_twoFingerDrag_cyclesForwardAndBackward() {
        val state = GestureWorldState(initialHero = MarvelHeroProtocol.IRON_MAN)

        // Drag down (> +80f) cycles forward
        state.onTwoFingerDrag(100f)
        assertEquals(MarvelHeroProtocol.SPIDER_MAN, state.currentHero)

        // Drag up (< -80f) cycles backward
        state.onTwoFingerDrag(-100f)
        assertEquals(MarvelHeroProtocol.IRON_MAN, state.currentHero)
    }

    @Test
    fun gestureWorldState_edgeSwipe_cyclesColorPalette() {
        val state = GestureWorldState(initialColor = ThemeColorPreset.CYAN)
        assertEquals(ThemeColorPreset.CYAN, state.currentColor)

        var selectedColor: ThemeColorPreset? = null
        state.onEdgeSwipe(GestureWorldState.EdgeDirection.RIGHT) { selectedColor = it }
        assertEquals(ThemeColorPreset.PURPLE, state.currentColor)
        assertEquals(ThemeColorPreset.PURPLE, selectedColor)

        state.onEdgeSwipe(GestureWorldState.EdgeDirection.LEFT) { selectedColor = it }
        assertEquals(ThemeColorPreset.CYAN, state.currentColor)
    }

    @Test
    fun gestureWorldState_rotation_detectsOrientationChanges() {
        val state = GestureWorldState()
        assertFalse(state.isLandscape)

        state.onRotationChanged(true)
        assertTrue(state.isLandscape)

        state.onRotationChanged(false)
        assertFalse(state.isLandscape)
    }

    @Test
    fun customizationSettings_fivePillarsDefaults_verified() {
        val settings = CustomizationSettings()

        // Pillar 1: Gesture-driven world
        assertTrue(settings.gestureWorldEnabled)

        // Pillar 2: Autonomous context adaptation
        assertFalse(settings.autonomousContextEnabled) // opt-in toggle

        // Pillar 3: Daily family story mode
        assertTrue(settings.dailyStoryMode)

        // Pillar 5: Cinematic entrance
        assertTrue(settings.cinematicEntranceEnabled)
    }
}
