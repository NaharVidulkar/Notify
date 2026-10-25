package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.TaskRepository
import com.example.data.model.LockScreenPrivacy
import com.example.data.model.Priority
import com.example.data.model.Task
import com.example.data.model.UserSettings
import com.example.data.model.VibrationPattern
import com.example.notifications.BootReceiver
import com.example.notifications.NotificationActionReceiver
import com.example.notifications.NotificationHelper
import com.example.notifications.ReminderAlarmScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.LocalTime

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

  @Test
  fun `verify dynamic channel generation from settings and obsolete channel cleanup`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

    // Initial settings with standard vibration
    val settingsA = UserSettings(
      soundEnabled = true,
      vibrate = true,
      vibrationPattern = VibrationPattern.STANDARD
    )
    val channelIdA = NotificationHelper.ensureNotificationChannel(context, settingsA)
    assertTrue(channelIdA.startsWith("reminders_"))
    assertNotNull(nm.getNotificationChannel(channelIdA))

    // New settings with Pulse vibration and no sound
    val settingsB = UserSettings(
      soundEnabled = false,
      vibrate = true,
      vibrationPattern = VibrationPattern.PULSE
    )
    val channelIdB = NotificationHelper.ensureNotificationChannel(context, settingsB)
    assertTrue(channelIdB.startsWith("reminders_"))
    assertNotEquals(channelIdA, channelIdB)

    // Channel B exists
    assertNotNull(nm.getNotificationChannel(channelIdB))
    // Old channel A should have been deleted because notification channels are immutable
    assertNull(nm.getNotificationChannel(channelIdA))

    // Silent channel test
    val silentId = NotificationHelper.ensureNotificationChannel(context, settingsB, isSilent = true)
    assertEquals("reminders_silent", silentId)
    assertNotNull(nm.getNotificationChannel(silentId))
  }

  @Test
  fun `verify quiet hours logic across day and midnight boundaries`() {
    // Disabled quiet hours
    val disabledSettings = UserSettings(quietHoursEnabled = false, quietHoursStart = "22:00", quietHoursEnd = "07:00")
    assertFalse(NotificationHelper.isQuietHours(disabledSettings, LocalTime.of(23, 30)))
    assertFalse(NotificationHelper.isQuietHours(disabledSettings, LocalTime.of(12, 0)))

    // Enabled quiet hours across midnight: 22:00 to 07:00
    val overnightSettings = UserSettings(quietHoursEnabled = true, quietHoursStart = "22:00", quietHoursEnd = "07:00")
    assertTrue("23:30 should be in quiet hours", NotificationHelper.isQuietHours(overnightSettings, LocalTime.of(23, 30)))
    assertTrue("02:15 should be in quiet hours", NotificationHelper.isQuietHours(overnightSettings, LocalTime.of(2, 15)))
    assertTrue("22:00 exact start should be in quiet hours", NotificationHelper.isQuietHours(overnightSettings, LocalTime.of(22, 0)))
    assertFalse("07:00 end boundary should not be in quiet hours", NotificationHelper.isQuietHours(overnightSettings, LocalTime.of(7, 0)))
    assertFalse("14:30 daytime should not be in quiet hours", NotificationHelper.isQuietHours(overnightSettings, LocalTime.of(14, 30)))

    // Same-day quiet hours: 13:00 to 15:00
    val daytimeSettings = UserSettings(quietHoursEnabled = true, quietHoursStart = "13:00", quietHoursEnd = "15:00")
    assertTrue("14:00 should be in quiet hours", NotificationHelper.isQuietHours(daytimeSettings, LocalTime.of(14, 0)))
    assertFalse("16:00 should not be in quiet hours", NotificationHelper.isQuietHours(daytimeSettings, LocalTime.of(16, 0)))
  }

  @Test
  fun `verify high priority override during quiet hours`() {
    val quietSettingsWithOverride = UserSettings(
      quietHoursEnabled = true,
      quietHoursStart = "22:00",
      quietHoursEnd = "07:00",
      quietHoursHighPriorityCanRing = true
    )
    val nightTime = LocalTime.of(23, 45)

    // High priority can ring
    assertTrue(NotificationHelper.shouldRingAndAlert(quietSettingsWithOverride, Priority.HIGH, nightTime))
    // Medium priority cannot ring
    assertFalse(NotificationHelper.shouldRingAndAlert(quietSettingsWithOverride, Priority.MEDIUM, nightTime))
    // Low priority cannot ring
    assertFalse(NotificationHelper.shouldRingAndAlert(quietSettingsWithOverride, Priority.LOW, nightTime))

    // High priority override disabled
    val quietSettingsWithoutOverride = quietSettingsWithOverride.copy(quietHoursHighPriorityCanRing = false)
    assertFalse(NotificationHelper.shouldRingAndAlert(quietSettingsWithoutOverride, Priority.HIGH, nightTime))
  }

  @Test
  fun `verify notification settings persistence across restart`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = TaskRepository(context)

    val customSettings = UserSettings(
      vibrate = false,
      vibrationPattern = VibrationPattern.LONG,
      soundEnabled = false,
      wakeScreenOverLock = false,
      lockScreenPrivacy = LockScreenPrivacy.HIDE_DETAILS,
      quietHoursEnabled = true,
      quietHoursStart = "21:00",
      quietHoursEnd = "06:30",
      quietHoursHighPriorityCanRing = false,
      defaultSnoozeMinutes = 30
    )
    repo.updateSettings(customSettings)

    // Simulate app restart by constructing a new repository instance
    val restartedRepo = TaskRepository(context)
    val loaded = restartedRepo.settings.value

    assertEquals(false, loaded.vibrate)
    assertEquals(VibrationPattern.LONG, loaded.vibrationPattern)
    assertEquals(false, loaded.soundEnabled)
    assertEquals(false, loaded.wakeScreenOverLock)
    assertEquals(LockScreenPrivacy.HIDE_DETAILS, loaded.lockScreenPrivacy)
    assertEquals(true, loaded.quietHoursEnabled)
    assertEquals("21:00", loaded.quietHoursStart)
    assertEquals("06:30", loaded.quietHoursEnd)
    assertEquals(false, loaded.quietHoursHighPriorityCanRing)
    assertEquals(30, loaded.defaultSnoozeMinutes)
  }

  @Test
  fun `verify editable greeting name default fallback and persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // 1. Default name validation
    assertEquals("Nahar", UserSettings.DEFAULT_USER_NAME)
    assertEquals("Nahar", UserSettings().userName)

    // 2. Fallback logic: empty or whitespace-only inputs fallback to Nahar
    assertEquals("Nahar", UserSettings.sanitizeUserName(""))
    assertEquals("Nahar", UserSettings.sanitizeUserName("   "))
    assertEquals("Nahar", UserSettings.sanitizeUserName(null))

    // Trimming and 20 character maximum constraint
    assertEquals("Rahul", UserSettings.sanitizeUserName("  Rahul  "))
    val longName = "ThisIsAVeryLongNameExceedingTwentyCharacters"
    val sanitizedLong = UserSettings.sanitizeUserName(longName)
    assertEquals(20, sanitizedLong.length)
    assertEquals("ThisIsAVeryLongNameE", sanitizedLong)

    // 3. Persistence through repository and across app restarts
    val repo = TaskRepository(context)
    repo.updateUserName("Rahul")
    assertEquals("Rahul", repo.settings.value.userName)

    // Simulate restart with a new repository instance
    val restartedRepo = TaskRepository(context)
    assertEquals("Rahul", restartedRepo.settings.value.userName)

    // Empty input fallback when updating
    restartedRepo.updateUserName("   ")
    assertEquals("Nahar", restartedRepo.settings.value.userName)

    // Verify persisted after another restart
    val reloadedRepo = TaskRepository(context)
    assertEquals("Nahar", reloadedRepo.settings.value.userName)
  }
}
