package com.example.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.local.TaskRepository
import com.example.data.model.LockScreenPrivacy
import com.example.data.model.Priority
import com.example.data.model.Task
import com.example.data.model.UserSettings
import com.example.ui.ReminderAlertActivity
import java.time.LocalTime
import java.util.Objects

object NotificationHelper {
    const val DEFAULT_CHANNEL_ID = "reminders_v2"
    const val CHANNEL_NAME = "Reminders"
    const val ACTION_DONE = "com.example.notify.ACTION_DONE"
    const val ACTION_SNOOZE = "com.example.notify.ACTION_SNOOZE"
    const val EXTRA_TASK_ID = "extra_task_id"

    /**
     * Checks if current time is within Quiet Hours.
     */
    fun isQuietHours(settings: UserSettings, currentTime: LocalTime = LocalTime.now()): Boolean {
        if (!settings.quietHoursEnabled) return false
        return try {
            val start = LocalTime.parse(settings.quietHoursStart)
            val end = LocalTime.parse(settings.quietHoursEnd)
            if (start == end) return false
            if (start.isBefore(end)) {
                !currentTime.isBefore(start) && currentTime.isBefore(end)
            } else {
                // Wraps around midnight (e.g. 22:00 to 07:00)
                !currentTime.isBefore(start) || currentTime.isBefore(end)
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Determines whether sound, vibration, and full-screen alert can fire.
     */
    fun shouldRingAndAlert(settings: UserSettings, priority: Priority, currentTime: LocalTime = LocalTime.now()): Boolean {
        if (!isQuietHours(settings, currentTime)) {
            return true
        }
        // In quiet hours, check if high priority tasks can override
        return settings.quietHoursHighPriorityCanRing && priority == Priority.HIGH
    }

    /**
     * Android notification channels are immutable for sound & vibration.
     * We dynamically generate the channel ID from sound, vibration, and pattern settings.
     */
    fun computeChannelId(settings: UserSettings, isSilent: Boolean): String {
        if (isSilent) {
            return "reminders_silent"
        }
        val soundKey = if (settings.soundEnabled) (settings.ringtoneUri ?: "default") else "none"
        val vibKey = if (settings.vibrate) settings.vibrationPattern.name else "none"
        val hash = Math.abs(Objects.hash(soundKey, vibKey))
        return "reminders_${Integer.toHexString(hash)}"
    }

    fun getCurrentChannelId(context: Context): String {
        val repo = TaskRepository.getInstance(context)
        return computeChannelId(repo.settings.value, false)
    }

    /**
     * Ensures the dynamic channel exists, and cleans up obsolete channels.
     */
    fun ensureNotificationChannel(
        context: Context,
        settings: UserSettings,
        isSilent: Boolean = false
    ): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val targetChannelId = computeChannelId(settings, isSilent)

            // Clean up obsolete channels starting with "reminders"
            try {
                val channels = manager.notificationChannels
                for (ch in channels) {
                    val id = ch.id
                    if (id.startsWith("reminders") && id != targetChannelId && id != "reminders_silent") {
                        manager.deleteNotificationChannel(id)
                    }
                }
            } catch (_: Exception) {}

            val existing = manager.getNotificationChannel(targetChannelId)
            if (existing == null) {
                val channel = if (isSilent) {
                    NotificationChannel(
                        targetChannelId,
                        "Reminders (Silent)",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = "Silent reminders during quiet hours"
                        enableVibration(false)
                        setSound(null, null)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    }
                } else {
                    val soundUri: Uri? = if (settings.soundEnabled) {
                        settings.ringtoneUri?.let { Uri.parse(it) }
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    } else null

                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()

                    NotificationChannel(
                        targetChannelId,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Time-sensitive alerts and reminder alarms"
                        enableVibration(settings.vibrate)
                        if (settings.vibrate) {
                            vibrationPattern = settings.vibrationPattern.pattern
                        }
                        enableLights(true)
                        setShowBadge(true)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                        setSound(soundUri, audioAttributes)
                    }
                }
                manager.createNotificationChannel(channel)
            }
            return targetChannelId
        }
        return DEFAULT_CHANNEL_ID
    }

    fun createNotificationChannel(context: Context) {
        val repo = TaskRepository.getInstance(context)
        ensureNotificationChannel(context, repo.settings.value, false)
    }

    /**
     * Schedules a reliable alarm using AlarmManager
     */
    fun scheduleReminder(context: Context, task: Task) {
        val repo = TaskRepository.getInstance(context)
        ensureNotificationChannel(context, repo.settings.value, false)
        ReminderAlarmScheduler.scheduleReminder(context, task)
    }

    /**
     * Displays the notification immediately using active runtime settings.
     */
    fun showNotification(context: Context, task: Task) {
        val repo = TaskRepository.getInstance(context)
        val settings = repo.settings.value

        // Check quiet hours and priority
        val canRingAndAlert = shouldRingAndAlert(settings, task.priority)
        val channelId = ensureNotificationChannel(context, settings, isSilent = !canRingAndAlert)

        val soundUri: Uri? = if (canRingAndAlert && settings.soundEnabled) {
            settings.ringtoneUri?.let { Uri.parse(it) }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        } else null

        // Full-screen Intent to wake screen & show on lock screen
        val fullScreenIntent = Intent(context, ReminderAlertActivity::class.java).apply {
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra("task_title", task.title)
            putExtra("task_due", task.dueAt)
            putExtra("task_tag", task.tag)
            putExtra("task_priority", task.priority.name)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            task.id.hashCode() + 1000,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Content intent
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            task.id.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Done Action
        val doneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_DONE
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (task.id + "_done").hashCode(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze Action (uses user-configured default snooze length)
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (task.id + "_snooze").hashCode(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subText = if (task.remindBefore > 0) "in ${task.remindBefore} min" else task.formattedTime()
        val contentText = if (task.notes.isNotBlank()) task.notes else "${task.tag} · ${task.priority.label} Priority"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(task.title)
            .setContentText(contentText)
            .setSubText("Notify · $subText")
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(R.mipmap.ic_launcher, "Done", donePendingIntent)
            .addAction(R.mipmap.ic_launcher, "Snooze ${settings.defaultSnoozeMinutes} min", snoozePendingIntent)

        if (canRingAndAlert) {
            builder.setPriority(NotificationCompat.PRIORITY_MAX)
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
            if (settings.soundEnabled && soundUri != null) {
                builder.setSound(soundUri)
            }
            if (settings.vibrate) {
                builder.setVibrate(settings.vibrationPattern.pattern)
            }
            // Full screen intent only when wakeScreenOverLock is enabled
            if (settings.wakeScreenOverLock) {
                builder.setFullScreenIntent(fullScreenPendingIntent, true)
            }
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_LOW)
            builder.setCategory(NotificationCompat.CATEGORY_REMINDER)
            builder.setSilent(true)
        }

        // Lock screen privacy handling
        if (settings.lockScreenPrivacy == LockScreenPrivacy.HIDE_DETAILS) {
            builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            val publicNotification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Reminder")
                .setContentText("You have a scheduled reminder")
                .build()
            builder.setPublicVersion(publicNotification)
        } else {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(task.id.hashCode(), builder.build())
    }

    fun cancelNotification(context: Context, taskId: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(taskId.hashCode())
    }
}
