package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.TaskRepository

class ReminderAlarmReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_FIRE_ALARM = "com.example.notify.ACTION_FIRE_ALARM"
        const val EXTRA_TASK_ID = "extra_task_id"
        private const val TAG = "ReminderAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        Log.d(TAG, "Alarm fired for task ID: $taskId")

        val repo = TaskRepository.getInstance(context)
        val task = repo.getTaskById(taskId) ?: return

        if (!task.done) {
            // Post high-importance notification
            NotificationHelper.showNotification(context, task)

            // If task repeats, schedule the next occurrence
            if (task.hasRepeat()) {
                val nextTask = task.advanceToNextOccurrence()
                repo.updateTaskDirectly(nextTask)
                ReminderAlarmScheduler.scheduleReminder(context, nextTask)
                Log.d(TAG, "Advanced repeating task ${task.title} to next occurrence: ${nextTask.dueAt}")
            }
        }
    }
}
