package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TaskRepository
import com.example.data.model.AccentColor
import com.example.data.model.Priority
import com.example.data.model.Task
import com.example.data.model.ThemeMode
import com.example.data.model.UserSettings
import com.example.notifications.NotificationHelper
import com.example.notifications.ReminderAlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

enum class TaskFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    DONE("Done")
}

enum class BottomTab(val label: String) {
    TODAY("Today"),
    CALENDAR("Calendar"),
    SETTINGS("Settings")
}

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TaskRepository.getInstance(application)

    val allTasks: StateFlow<List<Task>> = repository.tasks
    val settings: StateFlow<UserSettings> = repository.settings
    val customTags: StateFlow<List<String>> = repository.customTags

    private val _currentFilter = MutableStateFlow(TaskFilter.ALL)
    val currentFilter: StateFlow<TaskFilter> = _currentFilter.asStateFlow()

    private val _currentTab = MutableStateFlow(BottomTab.TODAY)
    val currentTab: StateFlow<BottomTab> = _currentTab.asStateFlow()

    private val _selectedCalendarDate = MutableStateFlow(LocalDate.now())
    val selectedCalendarDate: StateFlow<LocalDate> = _selectedCalendarDate.asStateFlow()

    private val _isNewReminderOpen = MutableStateFlow(false)
    val isNewReminderOpen: StateFlow<Boolean> = _isNewReminderOpen.asStateFlow()

    private val _editingTask = MutableStateFlow<Task?>(null)
    val editingTask: StateFlow<Task?> = _editingTask.asStateFlow()

    private val _isEditFocusOpen = MutableStateFlow(false)
    val isEditFocusOpen: StateFlow<Boolean> = _isEditFocusOpen.asStateFlow()

    private val _isNotificationPreviewOpen = MutableStateFlow(false)
    val isNotificationPreviewOpen: StateFlow<Boolean> = _isNotificationPreviewOpen.asStateFlow()

    private val _previewTask = MutableStateFlow<Task?>(null)
    val previewTask: StateFlow<Task?> = _previewTask.asStateFlow()

    val filteredTasks: StateFlow<List<Task>> = combine(allTasks, _currentFilter) { list, filter ->
        when (filter) {
            TaskFilter.ALL -> list
            TaskFilter.TODAY -> list.filter { it.isToday() }
            TaskFilter.UPCOMING -> list.filter { it.isUpcoming() }
            TaskFilter.DONE -> list.filter { it.done }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayRemindersCount: StateFlow<Int> = allTasks.combine(_currentFilter) { list, _ ->
        list.count { it.isToday() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    init {
        // Reschedule pending exact alarms on app startup
        ReminderAlarmScheduler.rescheduleAllPendingReminders(application)
    }

    fun setFilter(filter: TaskFilter) {
        _currentFilter.value = filter
    }

    fun setTab(tab: BottomTab) {
        _currentTab.value = tab
    }

    fun selectCalendarDate(date: LocalDate) {
        _selectedCalendarDate.value = date
    }

    fun openNewReminder(defaultDate: LocalDate? = null) {
        _editingTask.value = null
        _isNewReminderOpen.value = true
    }

    fun openEditReminder(task: Task) {
        _editingTask.value = task
        _isNewReminderOpen.value = true
    }

    fun closeReminderSheet() {
        _isNewReminderOpen.value = false
        _editingTask.value = null
    }

    fun openEditFocus() {
        _isEditFocusOpen.value = true
    }

    fun closeEditFocus() {
        _isEditFocusOpen.value = false
    }

    fun saveTask(task: Task) {
        // Cancel prior alarm if exists
        ReminderAlarmScheduler.cancelReminder(getApplication(), task.id)

        if (_editingTask.value != null) {
            repository.updateTask(task)
        } else {
            repository.addTask(task)
        }

        // Schedule exact alarm for the reminder if active
        if (!task.done) {
            ReminderAlarmScheduler.scheduleReminder(getApplication(), task)
        }
        closeReminderSheet()
    }

    fun toggleTask(taskId: String) {
        val task = repository.getTaskById(taskId)
        if (task != null) {
            if (task.done) {
                // Re-activating task:
                val uncompleted = task.copy(done = false)
                repository.updateTaskDirectly(uncompleted)
                ReminderAlarmScheduler.scheduleReminder(getApplication(), uncompleted)
            } else {
                // Completing task:
                if (task.hasRepeat()) {
                    // Reschedule for next occurrence
                    val nextTask = task.advanceToNextOccurrence()
                    repository.updateTaskDirectly(nextTask)
                    ReminderAlarmScheduler.scheduleReminder(getApplication(), nextTask)
                } else {
                    val completed = task.copy(done = true)
                    repository.updateTaskDirectly(completed)
                    ReminderAlarmScheduler.cancelReminder(getApplication(), taskId)
                }
            }
        } else {
            repository.toggleTask(taskId)
        }
    }

    fun deleteTask(taskId: String) {
        ReminderAlarmScheduler.cancelReminder(getApplication(), taskId)
        repository.deleteTask(taskId)
    }

    fun snoozeTask(taskId: String, minutes: Int = settings.value.defaultSnoozeMinutes) {
        ReminderAlarmScheduler.cancelReminder(getApplication(), taskId)
        repository.snoozeTask(taskId, minutes)
        val snoozed = repository.getTaskById(taskId)
        if (snoozed != null) {
            ReminderAlarmScheduler.scheduleReminder(getApplication(), snoozed)
        }
    }

    fun addCustomTag(tag: String) {
        repository.addTag(tag)
    }

    fun updateThemeMode(mode: ThemeMode) {
        repository.updateSettings(settings.value.copy(themeMode = mode))
    }

    fun updateAccentColor(accent: AccentColor) {
        repository.updateSettings(settings.value.copy(accentColor = accent))
    }

    fun updateDefaultSnooze(minutes: Int) {
        repository.updateSettings(settings.value.copy(defaultSnoozeMinutes = minutes))
    }

    fun updateDailySummary(time: String) {
        repository.updateSettings(settings.value.copy(dailySummaryTime = time))
    }

    fun updateSound(sound: String) {
        repository.updateSettings(settings.value.copy(sound = sound))
    }

    fun updateProfile(name: String, sub: String) {
        repository.updateSettings(settings.value.copy(userName = name, userSub = sub))
    }

    fun updateFocus(title: String, subtitle: String) {
        repository.updateSettings(settings.value.copy(todaysFocus = title, focusSubtext = subtitle))
        closeEditFocus()
    }

    fun resetData() {
        repository.resetToDefaultSeed()
        ReminderAlarmScheduler.rescheduleAllPendingReminders(getApplication())
    }

    fun showNotificationPreview(task: Task? = null) {
        val target = task ?: allTasks.value.firstOrNull()
        _previewTask.value = target
        _isNotificationPreviewOpen.value = true
    }

    fun closeNotificationPreview() {
        _isNotificationPreviewOpen.value = false
    }

    fun triggerSystemNotification(task: Task? = null) {
        val target = task ?: allTasks.value.firstOrNull() ?: Task(
            title = "DSA: Graphs – BFS & DFS practice",
            dueAt = java.time.LocalDateTime.now().plusMinutes(10).toString(),
            priority = Priority.HIGH,
            tag = "DSA",
            notes = "45 min session. Keep the streak going."
        )
        NotificationHelper.showNotification(getApplication(), target)
    }
}
