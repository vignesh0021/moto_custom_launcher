package com.motorola.launcher.theme

import com.motorola.launcher.ui.model.CoreGeometry
import com.motorola.launcher.ui.model.MarvelHeroProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying the centralized Marvel Theme Engine:
 * - Theme registry completeness and immutability
 * - Safe fallback mechanisms for null, empty, and unknown IDs
 * - Presence of required superhero themes (Iron Man, Captain America, Thor, Black Panther, Spider-Man, Doctor Strange, Hulk, Black Widow, Avengers Unified)
 * - Completeness and sanity of visual tokens (colors, HUD, wallpaper, icons, dock, drawer, dashboard, animations, sound/haptic)
 * - Safe default sound and haptic configurations (disabled by default)
 * - Dynamic registry extensibility
 */
class MarvelThemeEngineTest {

    @Test
    fun testRegistryContainsAllRequiredMarvelThemes() {
        val themes = MarvelThemeRegistry.getAllThemes()
        assertTrue("Theme registry must contain at least 9 themes", themes.size >= 9)

        val themeIds = themes.map { it.id }.toSet()
        val requiredIds = setOf(
            "iron_man",
            "captain_america",
            "thor",
            "black_panther",
            "spider_man",
            "doctor_strange",
            "hulk",
            "black_widow",
            "avengers_unified"
        )

        for (requiredId in requiredIds) {
            assertTrue("Registry must contain theme ID: $requiredId", themeIds.contains(requiredId))
        }
    }

    @Test
    fun testGetThemeByIdReturnsExpectedInstance() {
        val ironMan = MarvelThemeRegistry.getTheme("iron_man")
        assertEquals("iron_man", ironMan.id)
        assertEquals("Iron Man", ironMan.name)
        assertEquals(MarvelHeroProtocol.IRON_MAN, ironMan.heroProtocol)
        assertEquals(CoreGeometry.ARC_REACTOR, ironMan.coreGeometry)

        val captainAmerica = MarvelThemeRegistry.getTheme("captain_america")
        assertEquals("captain_america", captainAmerica.id)
        assertEquals("Captain America", captainAmerica.name)
        assertEquals(MarvelHeroProtocol.CAPTAIN_AMERICA, captainAmerica.heroProtocol)
        assertEquals(CoreGeometry.STAR_SHIELD, captainAmerica.coreGeometry)

        val blackPanther = MarvelThemeRegistry.getTheme("black_panther")
        assertEquals("black_panther", blackPanther.id)
        assertEquals("Black Panther", blackPanther.name)
        assertEquals(MarvelHeroProtocol.BLACK_PANTHER, blackPanther.heroProtocol)
        assertEquals(CoreGeometry.VIBRANIUM_WEAVE, blackPanther.coreGeometry)

        val thor = MarvelThemeRegistry.getTheme("thor")
        assertEquals("thor", thor.id)
        assertEquals("Thor", thor.name)
        assertEquals(MarvelHeroProtocol.THOR, thor.heroProtocol)
        assertEquals(CoreGeometry.MJOLNIR_RUNIC, thor.coreGeometry)

        val spiderMan = MarvelThemeRegistry.getTheme("spider_man")
        assertEquals("spider_man", spiderMan.id)
        assertEquals("Spider-Man", spiderMan.name)
        assertEquals(MarvelHeroProtocol.SPIDER_MAN, spiderMan.heroProtocol)
        assertEquals(CoreGeometry.SPIDER_MATRIX, spiderMan.coreGeometry)

        val doctorStrange = MarvelThemeRegistry.getTheme("doctor_strange")
        assertEquals("doctor_strange", doctorStrange.id)
        assertEquals("Doctor Strange", doctorStrange.name)
        assertEquals(MarvelHeroProtocol.DOCTOR_STRANGE, doctorStrange.heroProtocol)
        assertEquals(CoreGeometry.EYE_OF_AGAMOTTO, doctorStrange.coreGeometry)

        val hulk = MarvelThemeRegistry.getTheme("hulk")
        assertEquals("hulk", hulk.id)
        assertEquals("Hulk", hulk.name)
        assertEquals(MarvelHeroProtocol.HULK, hulk.heroProtocol)
        assertEquals(CoreGeometry.GAMMA_FIST, hulk.coreGeometry)

        val blackWidow = MarvelThemeRegistry.getTheme("black_widow")
        assertEquals("black_widow", blackWidow.id)
        assertEquals("Black Widow", blackWidow.name)
        assertEquals(MarvelHeroProtocol.BLACK_WIDOW, blackWidow.heroProtocol)
        assertEquals(CoreGeometry.BLACK_WIDOW_HOURGLASS, blackWidow.coreGeometry)

        val avengersUnified = MarvelThemeRegistry.getTheme("avengers_unified")
        assertEquals("avengers_unified", avengersUnified.id)
        assertEquals("Avengers Unified", avengersUnified.name)
        assertEquals(MarvelHeroProtocol.AVENGERS_UNIFIED, avengersUnified.heroProtocol)
        assertEquals(CoreGeometry.AVENGERS_CHEVRON, avengersUnified.coreGeometry)
    }

    @Test
    fun testSafeFallbackOnNullOrEmptyId() {
        val defaultTheme = MarvelThemeRegistry.defaultTheme

        val themeForNull = MarvelThemeRegistry.getTheme(null)
        assertEquals("Null ID must safely fallback to defaultTheme", defaultTheme.id, themeForNull.id)

        val themeForEmpty = MarvelThemeRegistry.getTheme("")
        assertEquals("Empty ID must safely fallback to defaultTheme", defaultTheme.id, themeForEmpty.id)

        val themeForBlank = MarvelThemeRegistry.getTheme("   ")
        assertEquals("Blank ID must safely fallback to defaultTheme", defaultTheme.id, themeForBlank.id)
    }

    @Test
    fun testSafeFallbackOnUnknownOrCorruptedId() {
        val defaultTheme = MarvelThemeRegistry.defaultTheme

        val themeUnknown = MarvelThemeRegistry.getTheme("non_existent_hero_xyz_99")
        assertEquals("Unknown theme ID must safely fallback to defaultTheme", defaultTheme.id, themeUnknown.id)

        val themeSpecialChars = MarvelThemeRegistry.getTheme("!@#$$%^&*()")
        assertEquals("Corrupted theme ID must safely fallback to defaultTheme", defaultTheme.id, themeSpecialChars.id)
    }

    @Test
    fun testAllThemesHaveCompleteVisualTokens() {
        for (theme in MarvelThemeRegistry.getAllThemes()) {
            assertNotNull("Theme ID must not be null", theme.id)
            assertTrue("Theme ID must not be blank", theme.id.isNotBlank())
            assertTrue("Theme Name must not be blank", theme.name.isNotBlank())
            assertTrue("Protocol title must not be blank", theme.protocolTitle.isNotBlank())
            assertTrue("Description must not be blank", theme.description.isNotBlank())

            // Colors token completeness
            assertNotNull(theme.colors.primary)
            assertNotNull(theme.colors.secondary)
            assertNotNull(theme.colors.bright)
            assertNotNull(theme.colors.glow)
            assertNotNull(theme.colors.subtle)
            assertNotNull(theme.colors.surface)
            assertNotNull(theme.colors.surfaceDark)
            assertNotNull(theme.colors.surfaceCard)
            assertNotNull(theme.colors.hudBorder)

            // HUD token completeness
            assertTrue("HUD header prefix must not be blank", theme.hudConfig.headerPrefix.isNotBlank())
            assertTrue("HUD status badge must not be blank", theme.hudConfig.statusBadge.isNotBlank())
            assertTrue("HUD search hint must not be blank", theme.hudConfig.searchHint.isNotBlank())
            assertTrue("HUD dashboard title must not be blank", theme.hudConfig.dashboardTitle.isNotBlank())

            // Wallpaper config
            assertNotNull(theme.wallpaperConfig.defaultPreset)
            assertNotNull(theme.wallpaperConfig.topColor)
            assertNotNull(theme.wallpaperConfig.bottomColor)
            assertNotNull(theme.wallpaperConfig.ambientEdgeGlow)

            // Icon treatment
            assertNotNull(theme.iconTreatment.frameShape)
            assertNotNull(theme.iconTreatment.insigniaInCorner)
            assertNotNull(theme.iconTreatment.borderBrush)

            // Dock style
            assertNotNull(theme.dockStyle.backgroundBrush)
            assertNotNull(theme.dockStyle.borderColor)
            assertTrue("Dock pill label must not be blank", theme.dockStyle.dockPillLabel.isNotBlank())

            // Drawer style
            assertNotNull(theme.drawerStyle.headerAccent)
            assertNotNull(theme.drawerStyle.tabIndicatorColor)

            // Dashboard style
            assertNotNull(theme.dashboardStyle.accentGlow)
            assertTrue("Dashboard header must not be blank", theme.dashboardStyle.blueprintLabel.isNotBlank())

            // Animation config
            assertTrue("Transition duration must be positive", theme.animationConfig.transitionDurationMs > 0)
            assertTrue("Particle density must be >= 0", theme.animationConfig.particleDensityMultiplier >= 0f)

            // Sound and Haptic config
            assertNotNull(theme.soundHapticConfig)
        }
    }

    @Test
    fun testSoundOptionsAreDisabledByDefault() {
        // Acceptance criterion: Sound options disabled by default where appropriate
        for (theme in MarvelThemeRegistry.getAllThemes()) {
            assertFalse(
                "Sound effects should be disabled by default for theme ${theme.id}",
                theme.soundHapticConfig.soundEffectsEnabled
            )
        }
    }

    @Test
    fun testCustomThemeRegistration() {
        val customTheme = MarvelThemeRegistry.defaultTheme.copy(
            id = "custom_test_hero",
            name = "Test Hero",
            protocolTitle = "TEST // PROTOCOL"
        )

        MarvelThemeRegistry.registerTheme(customTheme)

        val retrieved = MarvelThemeRegistry.getTheme("custom_test_hero")
        assertEquals("custom_test_hero", retrieved.id)
        assertEquals("Test Hero", retrieved.name)
        assertEquals("TEST // PROTOCOL", retrieved.protocolTitle)
    }

    @Test
    fun testHeroProtocolGeometryConsistency() {
        val protocolMap = mapOf(
            MarvelHeroProtocol.IRON_MAN to CoreGeometry.ARC_REACTOR,
            MarvelHeroProtocol.BLACK_PANTHER to CoreGeometry.VIBRANIUM_WEAVE,
            MarvelHeroProtocol.CAPTAIN_AMERICA to CoreGeometry.STAR_SHIELD,
            MarvelHeroProtocol.THOR to CoreGeometry.MJOLNIR_RUNIC,
            MarvelHeroProtocol.HULK to CoreGeometry.GAMMA_FIST,
            MarvelHeroProtocol.DOCTOR_STRANGE to CoreGeometry.EYE_OF_AGAMOTTO,
            MarvelHeroProtocol.SPIDER_MAN to CoreGeometry.SPIDER_MATRIX,
            MarvelHeroProtocol.BLACK_WIDOW to CoreGeometry.BLACK_WIDOW_HOURGLASS,
            MarvelHeroProtocol.AVENGERS_UNIFIED to CoreGeometry.AVENGERS_CHEVRON
        )

        for ((protocol, expectedGeometry) in protocolMap) {
            assertEquals("Protocol $protocol must match geometry $expectedGeometry", expectedGeometry, protocol.coreGeometry)
            assertNotNull(protocol.primary)
            assertNotNull(protocol.secondary)
            assertNotNull(protocol.bright)
            assertTrue(protocol.displayName.isNotBlank())
        }
    }
}
