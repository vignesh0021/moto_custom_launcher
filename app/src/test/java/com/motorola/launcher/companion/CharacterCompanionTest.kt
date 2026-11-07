package com.motorola.launcher.companion

import com.motorola.launcher.data.companion.CharacterExpression
import com.motorola.launcher.data.companion.CharacterPose
import com.motorola.launcher.ui.model.CompanionStyle
import com.motorola.launcher.ui.model.CustomizationSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterCompanionTest {

    @Test
    fun companionStyle_fromId_resolvesAccurately() {
        assertEquals(CompanionStyle.MITHRA, CompanionStyle.fromId("mithra"))
        assertEquals(CompanionStyle.MITHRA_MOM, CompanionStyle.fromId("mithra_mom"))
        assertEquals(CompanionStyle.CUSTOM_SHEET, CompanionStyle.fromId("custom_sheet"))
        assertEquals(CompanionStyle.HERO_SUIT, CompanionStyle.fromId("hero_suit"))
        assertEquals(CompanionStyle.HERO_SUIT, CompanionStyle.fromId("unknown_style_fallback"))
    }

    @Test
    fun customizationSettings_containsTamilDefaultDialogues() {
        val settings = CustomizationSettings()

        // Mithra (Daughter)
        assertTrue(settings.mithraDialoguePrompt.contains("தண்ணி"))
        assertTrue(settings.mithraDialogueAccepted.contains("சூப்பர்"))
        assertTrue(settings.mithraDialogueRefused.contains("அப்புறம்"))

        // Mithra's Mom (Wife)
        assertTrue(settings.momDialoguePrompt.contains("தண்ணி"))
        assertTrue(settings.momDialogueAccepted.contains("நல்லது"))
        assertTrue(settings.momDialogueRefused.contains("மறந்துடாதீங்க"))
    }

    @Test
    fun characterSheetEnums_complete() {
        val poses = CharacterPose.entries
        assertTrue(poses.contains(CharacterPose.FRONT))
        assertTrue(poses.contains(CharacterPose.QUARTER))
        assertTrue(poses.contains(CharacterPose.SIDE))
        assertTrue(poses.contains(CharacterPose.PORTRAIT))

        val expressions = CharacterExpression.entries
        assertEquals(6, expressions.size)
        assertTrue(expressions.contains(CharacterExpression.HAPPY))
        assertTrue(expressions.contains(CharacterExpression.CURIOUS))
        assertTrue(expressions.contains(CharacterExpression.SURPRISED))
        assertTrue(expressions.contains(CharacterExpression.MISCHIEVOUS))
        assertTrue(expressions.contains(CharacterExpression.CONFUSED))
        assertTrue(expressions.contains(CharacterExpression.UPSET))
    }

    @Test
    fun tamilUnicodeDetection_identifiesTamilScript() {
        val tamilText = "அப்பா தண்ணி குடிங்க"
        val englishText = "Daddy, drink water please!"

        val hasTamil1 = tamilText.any { it in '\u0B80'..'\u0BFF' }
        val hasTamil2 = englishText.any { it in '\u0B80'..'\u0BFF' }

        assertTrue(hasTamil1)
        assertTrue(!hasTamil2)
    }
}
