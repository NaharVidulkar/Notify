package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.TaskRepository
import com.example.data.model.Priority
import com.example.data.model.Task
import com.example.notifications.BootReceiver
import com.example.notifications.NotificationActionReceiver
import com.example.notifications.NotificationHelper
import com.example.notifications.ReminderAlarmScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Notify", appName)
  }

  @Test
  fun `verify task repository seed and operations with cache`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = TaskRepository(context)

    val tasks = repo.getAllTasks()
    assertTrue("Seed tasks should not be empty", tasks.isNotEmpty())
    assertEquals(5, tasks.size)

    val firstTask = tasks.first()
    assertEquals("DSA: Graphs – BFS & DFS", firstTask.title)
    assertEquals(Priority.HIGH, firstTask.priority)

    // Lookup by ID through cache
    val cached = repo.getTaskById(firstTask.id)
    assertNotNull(cached)
    assertEquals(firstTask.title, cached?.title)

    // Test toggle
    repo.toggleTask(firstTask.id)
    val toggled = repo.getTaskById(firstTask.id)
    assertTrue(toggled?.done == true)
  }

  @Test
  fun `verify task overdue and today logic`() {
    val overdueTask = Task(
      title = "Old task",
      dueAt = LocalDateTime.now().minusHours(2).toString(),
      done = false
    )
    assertTrue(overdueTask.isOverdue())

    val doneTask = overdueTask.copy(done = true)
    assertFalse(doneTask.isOverdue())

    val todayTask = Task(
      title = "Today task",
      dueAt = java.time.LocalDate.now().atTime(12, 0).toString(),
      done = false
    )
    assertTrue(todayTask.isToday())
  }

  @Test
  fun `verify recurring task advancement`() {
    val dailyTask = Task(
      title = "Daily practice",
      dueAt = LocalDateTime.now().minusHours(1).toString(),
      repeat = "Every day"
    )
    assertTrue(dailyTask.hasRepeat())

    val nextDayTask = dailyTask.advanceToNextOccurrence()
    assertFalse(nextDayTask.done)
    val nextDue = nextDayTask.parseDueDateTime()
    assertNotNull(nextDue)
    assertTrue("Next occurrence must be in the future", nextDue!!.isAfter(LocalDateTime.now()))

    val weeklyTask = Task(
      title = "Weekly review",
      dueAt = LocalDateTime.now().minusDays(2).toString(),
      repeat = "Weekly"
    )
    assertTrue(weeklyTask.hasRepeat())
    val nextWeekTask = weeklyTask.advanceToNextOccurrence()
    assertTrue(nextWeekTask.parseDueDateTime()!!.isAfter(LocalDateTime.now()))
  }

  @Test
  fun `verify notification action receiver handles Done and Snooze`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = TaskRepository.getInstance(context)

    val task = Task(
      id = "test-action-task",
      title = "Action Test",
      dueAt = LocalDateTime.now().plusHours(2).toString(),
      repeat = "None",
      done = false
    )
    repo.addTask(task)

    val receiver = NotificationActionReceiver()

    // Test Done Action
    val doneIntent = Intent(NotificationHelper.ACTION_DONE).apply {
      putExtra(NotificationHelper.EXTRA_TASK_ID, task.id)
    }
    receiver.onReceive(context, doneIntent)

    val updatedDone = repo.getTaskById(task.id)
    assertTrue(updatedDone?.done == true)

    // Test Snooze Action
    val snoozeIntent = Intent(NotificationHelper.ACTION_SNOOZE).apply {
      putExtra(NotificationHelper.EXTRA_TASK_ID, task.id)
    }
    receiver.onReceive(context, snoozeIntent)

    val updatedSnoozed = repo.getTaskById(task.id)
    assertFalse(updatedSnoozed?.done == true)
  }

  @Test
  fun `verify alarm scheduler and boot receiver execution`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val task = Task(
      id = "test-alarm-task",
      title = "Alarm Test",
      dueAt = LocalDateTime.now().plusHours(1).toString(),
      remindBefore = 10,
      done = false
    )

    // Scheduling and cancellation shouldn't throw exceptions
    ReminderAlarmScheduler.scheduleReminder(context, task)
    ReminderAlarmScheduler.cancelReminder(context, task.id)

    // Boot Receiver shouldn't throw exceptions
    val bootReceiver = BootReceiver()
    val bootIntent = Intent(Intent.ACTION_BOOT_COMPLETED)
    bootReceiver.onReceive(context, bootIntent)
  }
}
