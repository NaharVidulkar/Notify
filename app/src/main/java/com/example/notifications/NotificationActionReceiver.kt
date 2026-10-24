package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.local.TaskRepository

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID) ?: return
        val repo = TaskRepository.getInstance(context)

        when (intent.action) {
            NotificationHelper.ACTION_DONE -> {
                val task = repo.getTaskById(taskId)
                if (task != null && task.hasRepeat()) {
                    val nextTask = task.advanceToNextOccurrence()
                    repo.updateTaskDirectly(nextTask)
                    ReminderAlarmScheduler.scheduleReminder(context, nextTask)
                    Toast.makeText(context, "Completed! Next: ${nextTask.formattedTime()}", Toast.LENGTH_SHORT).show()
                } else {
                    repo.toggleTask(taskId)
                    ReminderAlarmScheduler.cancelReminder(context, taskId)
                    Toast.makeText(context, "Marked as completed", Toast.LENGTH_SHORT).show()
                }
                NotificationHelper.cancelNotification(context, taskId)
            }
            NotificationHelper.ACTION_SNOOZE -> {
                val snoozeMinutes = repo.settings.value.defaultSnoozeMinutes
                repo.snoozeTask(taskId, snoozeMinutes)
                val snoozedTask = repo.getTaskById(taskId)
                if (snoozedTask != null) {
                    ReminderAlarmScheduler.scheduleReminder(context, snoozedTask)
                }
                NotificationHelper.cancelNotification(context, taskId)
                Toast.makeText(context, "Snoozed for $snoozeMinutes min", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
