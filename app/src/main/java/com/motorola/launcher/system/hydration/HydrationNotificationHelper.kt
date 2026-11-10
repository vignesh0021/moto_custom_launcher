package com.motorola.launcher.system.hydration

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.motorola.launcher.MainActivity
import com.motorola.launcher.R
import com.motorola.launcher.system.companion.CompanionDialogue
import java.util.UUID

/**
 * Helper that creates and posts rich interactive Android notifications
 * for hydration reminders with Tamil text and quick action buttons.
 */
object HydrationNotificationHelper {

    const val CHANNEL_ID = "v2h_hydration_reminders_v2"
    const val CHANNEL_NAME = "Smart Water Reminders (Lock Screen)"
    const val NOTIFICATION_ID = 8801

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily hydration notifications from Mithra and Amma (works on lock screen)"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(
        context: Context,
        dialogue: CompanionDialogue,
        consumedMl: Int,
        targetMl: Int
    ) {
        createNotificationChannel(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val uniqueToken = UUID.randomUUID().toString()

        // 1. Content Intent (Open App)
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_HYDRATION_DASHBOARD, true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            1001,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Action 1: Drink 200 ml
        val drink200Intent = Intent(context, HydrationActionReceiver::class.java).apply {
            action = HydrationActionReceiver.ACTION_DRINK_200
            putExtra(HydrationActionReceiver.EXTRA_AMOUNT_ML, 200)
            putExtra(HydrationActionReceiver.EXTRA_ACTION_TOKEN, uniqueToken)
        }
        val drink200PendingIntent = PendingIntent.getBroadcast(
            context,
            2001,
            drink200Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Action 2: Drink 250 ml
        val drink250Intent = Intent(context, HydrationActionReceiver::class.java).apply {
            action = HydrationActionReceiver.ACTION_DRINK_250
            putExtra(HydrationActionReceiver.EXTRA_AMOUNT_ML, 250)
            putExtra(HydrationActionReceiver.EXTRA_ACTION_TOKEN, uniqueToken)
        }
        val drink250PendingIntent = PendingIntent.getBroadcast(
            context,
            2002,
            drink250Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Action 3: Remind me later (Snooze)
        val snoozeIntent = Intent(context, HydrationActionReceiver::class.java).apply {
            action = HydrationActionReceiver.ACTION_SNOOZE
            putExtra(HydrationActionReceiver.EXTRA_ACTION_TOKEN, uniqueToken)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            2003,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 5. Action 4: Open Hydration Dashboard
        val dashboardIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_HYDRATION_DASHBOARD, true)
        }
        val dashboardPendingIntent = PendingIntent.getActivity(
            context,
            2004,
            dashboardIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val progressText = "இன்று: $consumedMl / $targetMl ml"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("💧 ${dialogue.title}")
            .setContentText(dialogue.tamilText)
            .setSubText(progressText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${dialogue.tamilText}\n\n$progressText")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(contentPendingIntent, true)
            .setAutoCancel(true)
            // 4 Required Actions
            .addAction(0, "200 ml குடிச்சேன் ✓", drink200PendingIntent)
            .addAction(0, "250 ml குடிச்சேன் ✓", drink250PendingIntent)
            .addAction(0, "அப்புறம் சொல்லுங்க", snoozePendingIntent)
            .addAction(0, "டேஷ்போர்டு", dashboardPendingIntent)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Android 13+ POST_NOTIFICATIONS permission not granted
        }
    }

    fun dismissNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(NOTIFICATION_ID)
    }
}
