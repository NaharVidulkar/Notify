package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Task

object NotificationHelper {
    const val CHANNEL_ID = "reminders_channel"
    const val CHANNEL_NAME = "Reminders"
    const val ACTION_DONE = "com.example.notify.ACTION_DONE"
    const val ACTION_SNOOZE = "com.example.notify.ACTION_SNOOZE"
    const val EXTRA_TASK_ID = "extra_task_id"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Task reminders and schedule alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setSound(defaultSoundUri, audioAttributes)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Schedules a reliable alarm using AlarmManager
     */
    fun scheduleReminder(context: Context, task: Task) {
        createNotificationChannel(context)
        ReminderAlarmScheduler.scheduleReminder(context, task)
    }

    /**
     * Displays the notification immediately (for triggered alarms, previews or tests)
     */
    fun showNotification(context: Context, task: Task) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            task.id.hashCode(),
            intent,
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

        // Snooze Action
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

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(task.title)
            .setContentText(contentText)
            .setSubText("Notify · $subText")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setContentIntent(contentPendingIntent)
            .addAction(R.mipmap.ic_launcher, "Done", donePendingIntent)
            .addAction(R.mipmap.ic_launcher, "Snooze 10 min", snoozePendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(task.id.hashCode(), notification)
    }

    fun cancelNotification(context: Context, taskId: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(taskId.hashCode())
    }
}
