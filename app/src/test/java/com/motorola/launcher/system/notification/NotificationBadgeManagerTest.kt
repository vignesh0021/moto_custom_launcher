package com.motorola.launcher.system.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for NotificationBadgeManager state flow and connection lifecycle.
 */
class NotificationBadgeManagerTest {

    @Test
    fun testServiceConnectedStateFlow() {
        NotificationBadgeManager.setServiceConnected(true)
        assertTrue(NotificationBadgeManager.isServiceConnected.value)

        NotificationBadgeManager.setServiceConnected(false)
        assertFalse(NotificationBadgeManager.isServiceConnected.value)
        assertEquals(0, NotificationBadgeManager.badgeCounts.value.size)
    }

    @Test
    fun testEmptyNotificationSnapshotClearsBadges() {
        NotificationBadgeManager.setServiceConnected(true)
        NotificationBadgeManager.updateFromNotifications(null)
        assertTrue(NotificationBadgeManager.badgeCounts.value.isEmpty())

        NotificationBadgeManager.updateFromNotifications(emptyArray())
        assertTrue(NotificationBadgeManager.badgeCounts.value.isEmpty())
    }
}
