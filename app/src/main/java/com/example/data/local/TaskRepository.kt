package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.AccentColor
import com.example.data.model.Priority
import com.example.data.model.Task
import com.example.data.model.ThemeMode
import com.example.data.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

class TaskRepository(private val context: Context) {
    companion object {
        private const val TAG = "TaskRepository"
        private const val PREFS_NAME = "notify_prefs"
        private const val KEY_SAVED_TASKS = "saved_tasks"
        private const val KEY_SAVED_TASKS_BACKUP = "saved_tasks_backup"

        @Volatile
        private var instance: TaskRepository? = null

        fun getInstance(context: Context): TaskRepository {
            return instance ?: synchronized(this) {
                instance ?: TaskRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Thread-safe in-memory cache for ultra-fast, zero-latency reads & background receiver lookups
    private val taskCache = ConcurrentHashMap<String, Task>()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _settings = MutableStateFlow(UserSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private val _customTags = MutableStateFlow<List<String>>(listOf("DSA", "YCCE", "React", "Project", "GitHub"))
    val customTags: StateFlow<List<String>> = _customTags.asStateFlow()

    init {
        loadSettings()
        loadTags()
        loadTasksWithCache()
    }

    private fun loadSettings() {
        val themeModeStr = prefs.getString("theme_mode", ThemeMode.LIGHT.name) ?: ThemeMode.LIGHT.name
        val accentColorStr = prefs.getString("accent_color", AccentColor.AMBER.name) ?: AccentColor.AMBER.name
        val snooze = prefs.getInt("default_snooze", 10)
        val dailySummary = prefs.getString("daily_summary", "8:00 AM") ?: "8:00 AM"
        val sound = prefs.getString("sound", "Chime") ?: "Chime"
        val userName = prefs.getString("user_name", "Nahar") ?: "Nahar"
        val userSub = prefs.getString("user_sub", "YCCE · Nagpur") ?: "YCCE · Nagpur"
        val focus = prefs.getString("todays_focus", "DSA – Graphs & DP") ?: "DSA – Graphs & DP"
        val focusSub = prefs.getString("focus_subtext", "5 reminders today · Java track") ?: "5 reminders today · Java track"

        _settings.value = UserSettings(
            themeMode = try { ThemeMode.valueOf(themeModeStr) } catch (e: Exception) { ThemeMode.LIGHT },
            accentColor = AccentColor.fromName(accentColorStr),
            defaultSnoozeMinutes = snooze,
            dailySummaryTime = dailySummary,
            sound = sound,
            userName = userName,
            userSub = userSub,
            todaysFocus = focus,
            focusSubtext = focusSub
        )
    }

    fun updateSettings(newSettings: UserSettings) {
        _settings.value = newSettings
        prefs.edit()
            .putString("theme_mode", newSettings.themeMode.name)
            .putString("accent_color", newSettings.accentColor.name)
            .putInt("default_snooze", newSettings.defaultSnoozeMinutes)
            .putString("daily_summary", newSettings.dailySummaryTime)
            .putString("sound", newSettings.sound)
            .putString("user_name", newSettings.userName)
            .putString("user_sub", newSettings.userSub)
            .putString("todays_focus", newSettings.todaysFocus)
            .putString("focus_subtext", newSettings.focusSubtext)
            .apply()
    }

    private fun loadTags() {
        val raw = prefs.getString("custom_tags", null)
        if (raw.isNullOrEmpty()) {
            val defaults = listOf("DSA", "YCCE", "React", "Project", "GitHub")
            _customTags.value = defaults
            saveTags(defaults)
        } else {
            try {
                val array = JSONArray(raw)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                _customTags.value = list
            } catch (e: Exception) {
                _customTags.value = listOf("DSA", "YCCE", "React", "Project", "GitHub")
            }
        }
    }

    fun addTag(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isNotEmpty() && !_customTags.value.contains(trimmed)) {
            val updated = _customTags.value + trimmed
            _customTags.value = updated
            saveTags(updated)
        }
    }

    private fun saveTags(tags: List<String>) {
        val array = JSONArray()
        tags.forEach { array.put(it) }
        prefs.edit().putString("custom_tags", array.toString()).apply()
    }

    @Synchronized
    private fun loadTasksWithCache() {
        var raw = prefs.getString(KEY_SAVED_TASKS, null)
        if (raw.isNullOrEmpty()) {
            // Check fallback backup key
            raw = prefs.getString(KEY_SAVED_TASKS_BACKUP, null)
        }

        if (raw.isNullOrEmpty()) {
            val initial = getSeedTasks()
            taskCache.clear()
            initial.forEach { taskCache[it.id] = it }
            _tasks.value = initial
            saveTasksInternal(initial)
        } else {
            try {
                val list = parseJsonTasks(raw)
                taskCache.clear()
                list.forEach { taskCache[it.id] = it }
                _tasks.value = list
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse tasks from primary cache, attempting backup recovery: ${e.message}")
                val backup = prefs.getString(KEY_SAVED_TASKS_BACKUP, null)
                val list = if (!backup.isNullOrEmpty()) {
                    try { parseJsonTasks(backup) } catch (e2: Exception) { getSeedTasks() }
                } else {
                    getSeedTasks()
                }
                taskCache.clear()
                list.forEach { taskCache[it.id] = it }
                _tasks.value = list
                saveTasksInternal(list)
            }
        }
    }

    private fun parseJsonTasks(raw: String): List<Task> {
        val array = JSONArray(raw)
        val list = mutableListOf<Task>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                Task(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    dueAt = obj.getString("dueAt"),
                    repeat = obj.optString("repeat", "None"),
                    remindBefore = obj.optInt("remindBefore", 10),
                    priority = Priority.fromString(obj.optString("priority", "Medium")),
                    tag = obj.optString("tag", "DSA"),
                    done = obj.optBoolean("done", false),
                    notes = obj.optString("notes", "")
                )
            )
        }
        return list
    }

    @Synchronized
    private fun saveTasksInternal(taskList: List<Task>) {
        // Update in-memory cache
        taskCache.clear()
        taskList.forEach { taskCache[it.id] = it }
        _tasks.value = taskList

        val array = JSONArray()
        for (task in taskList) {
            val obj = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("dueAt", task.dueAt)
                put("repeat", task.repeat)
                put("remindBefore", task.remindBefore)
                put("priority", task.priority.name)
                put("tag", task.tag)
                put("done", task.done)
                put("notes", task.notes)
            }
            array.put(obj)
        }
        val serialized = array.toString()
        prefs.edit()
            .putString(KEY_SAVED_TASKS, serialized)
            .putString(KEY_SAVED_TASKS_BACKUP, serialized)
            .apply()
    }

    fun getAllTasks(): List<Task> {
        return _tasks.value.ifEmpty {
            taskCache.values.toList().ifEmpty {
                loadTasksWithCache()
                _tasks.value
            }
        }
    }

    fun getTaskById(taskId: String): Task? {
        // Check cache first
        return taskCache[taskId] ?: run {
            loadTasksWithCache()
            taskCache[taskId]
        }
    }

    @Synchronized
    fun addTask(task: Task) {
        val updated = listOf(task) + _tasks.value
        saveTasksInternal(updated)
    }

    @Synchronized
    fun updateTask(task: Task) {
        val updated = _tasks.value.map { if (it.id == task.id) task else it }
        saveTasksInternal(updated)
    }

    @Synchronized
    fun updateTaskDirectly(task: Task) {
        taskCache[task.id] = task
        val updated = _tasks.value.map { if (it.id == task.id) task else it }
        saveTasksInternal(updated)
    }

    @Synchronized
    fun toggleTask(taskId: String) {
        val updated = _tasks.value.map {
            if (it.id == taskId) it.copy(done = !it.done) else it
        }
        saveTasksInternal(updated)
    }

    @Synchronized
    fun deleteTask(taskId: String) {
        taskCache.remove(taskId)
        val updated = _tasks.value.filterNot { it.id == taskId }
        saveTasksInternal(updated)
    }

    @Synchronized
    fun snoozeTask(taskId: String, minutes: Int) {
        val updated = _tasks.value.map { task ->
            if (task.id == taskId) {
                val currentDue = task.parseDueDateTime() ?: LocalDateTime.now()
                val newDue = currentDue.plusMinutes(minutes.toLong())
                task.copy(
                    dueAt = newDue.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    done = false
                )
            } else {
                task
            }
        }
        saveTasksInternal(updated)
    }

    fun resetToDefaultSeed() {
        val initial = getSeedTasks()
        saveTasksInternal(initial)
    }

    private fun getSeedTasks(): List<Task> {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val sun = today.plusDays((7 - today.dayOfWeek.value % 7).toLong())

        val t1Due = LocalDateTime.of(today, LocalTime.of(18, 0)).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val t2Due = LocalDateTime.of(today, LocalTime.of(20, 30)).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val t3Due = LocalDateTime.of(today, LocalTime.of(21, 15)).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val t4Due = LocalDateTime.of(tomorrow, LocalTime.of(7, 0)).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val t5Due = LocalDateTime.of(sun, LocalTime.of(10, 0)).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        return listOf(
            Task(
                id = "seed-1",
                title = "DSA: Graphs – BFS & DFS",
                dueAt = t1Due,
                repeat = "Every day",
                remindBefore = 10,
                priority = Priority.HIGH,
                tag = "DSA",
                done = false,
                notes = "45 min session. Keep the streak going."
            ),
            Task(
                id = "seed-2",
                title = "FEWD revision – MDM1ADS201",
                dueAt = t2Due,
                repeat = "None",
                remindBefore = 10,
                priority = Priority.HIGH,
                tag = "YCCE",
                done = false,
                notes = "YCCE exam prep"
            ),
            Task(
                id = "seed-3",
                title = "SkillProof – polish & README",
                dueAt = t3Due,
                repeat = "None",
                remindBefore = 10,
                priority = Priority.MEDIUM,
                tag = "Project",
                done = false,
                notes = "Clean up UI and documentation"
            ),
            Task(
                id = "seed-4",
                title = "Namaste React – lecture 5",
                dueAt = t4Due,
                repeat = "None",
                remindBefore = 10,
                priority = Priority.MEDIUM,
                tag = "React",
                done = false,
                notes = "Hooks deep dive"
            ),
            Task(
                id = "seed-5",
                title = "Update focused-dsa repo",
                dueAt = t5Due,
                repeat = "Weekly",
                remindBefore = 10,
                priority = Priority.LOW,
                tag = "GitHub",
                done = false,
                notes = "Push solved LeetCode solutions"
            )
        )
    }
}
