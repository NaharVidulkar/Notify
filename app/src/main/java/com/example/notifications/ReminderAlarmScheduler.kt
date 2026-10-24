package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.TaskRepository
import com.example.data.model.Task
import java.time.LocalDateTime
import java.time.ZoneId

object ReminderAlarmScheduler {
    private const val TAG = "ReminderAlarmScheduler"

    fun scheduleReminder(context: Context, task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        if (task.done) {
            cancelReminder(context, task.id)
            return
        }

        val dueDateTime = task.parseDueDateTime()
        if (dueDateTime == null) {
            Log.w(TAG, "Cannot parse due date for task: ${task.id}")
            return
        }

        val triggerDateTime = dueDateTime.minusMinutes(task.remindBefore.toLong())
        var triggerMillis = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val nowMillis = System.currentTimeMillis()

        var currentTask = task

        // If the scheduled time has already passed
        if (triggerMillis <= nowMillis) {
            if (task.hasRepeat()) {
                // Advance repeating task to next future occurrence
                currentTask = task.advanceToNextOccurrence()
                TaskRepository.getInstance(context).updateTaskDirectly(currentTask)
                val newDue = currentTask.parseDueDateTime() ?: return
                val newTrigger = newDue.minusMinutes(currentTask.remindBefore.toLong())
                triggerMillis = newTrigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } else {
                // If past due by more than 1 minute and not repeating, don't set an alarm
                if (triggerMillis < nowMillis - 60_000) {
                    Log.d(TAG, "Task ${task.title} is in the past, skipping alarm.")
                    return
                } else {
                    // Fire in 1 second if due right now
                    triggerMillis = nowMillis + 1_000
                }
            }
        }

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_FIRE_ALARM
            putExtra(ReminderAlarmReceiver.EXTRA_TASK_ID, currentTask.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            currentTask.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

            if (canScheduleExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
                Log.d(TAG, "Exact alarm set for ${currentTask.title} at $triggerMillis")
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
                Log.d(TAG, "Inexact allowWhileIdle alarm set for ${currentTask.title} at $triggerMillis")
            }
        } catch (se: SecurityException) {
            Log.w(TAG, "SecurityException setting exact alarm, falling back: ${se.message}")
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule alarm: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error scheduling alarm: ${e.message}")
        }
    }

    fun cancelReminder(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_FIRE_ALARM
            putExtra(ReminderAlarmReceiver.EXTRA_TASK_ID, taskId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for task $taskId")
        }
    }

    fun rescheduleAllPendingReminders(context: Context) {
        Log.d(TAG, "Rescheduling all pending reminders...")
        val repo = TaskRepository.getInstance(context)
        val tasks = repo.getAllTasks()
        for (task in tasks) {
            if (!task.done) {
                scheduleReminder(context, task)
            } else {
                cancelReminder(context, task.id)
            }
        }
    }
}
