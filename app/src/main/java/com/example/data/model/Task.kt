package com.example.data.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class Priority(val label: String, val colorHex: Long) {
    HIGH("High", 0xFFE5484D),
    MEDIUM("Medium", 0xFFF5A524),
    LOW("Low", 0xFF2FA37A);

    companion object {
        fun fromString(value: String): Priority {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val dueAt: String, // ISO-8601 string, e.g. "2026-10-24T18:00:00"
    val repeat: String = "None", // "None", "Every day", "Weekly"
    val remindBefore: Int = 10, // 0, 5, 10, 30 minutes
    val priority: Priority = Priority.MEDIUM,
    val tag: String = "DSA",
    val done: Boolean = false,
    val notes: String = ""
) {
    fun parseDueDateTime(): LocalDateTime? {
        return try {
            LocalDateTime.parse(dueAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (e: Exception) {
            try {
                LocalDateTime.parse(dueAt)
            } catch (e2: Exception) {
                null
            }
        }
    }

    fun hasRepeat(): Boolean {
        return repeat.equals("Every day", ignoreCase = true) ||
               repeat.equals("Daily", ignoreCase = true) ||
               repeat.equals("Weekly", ignoreCase = true)
    }

    fun advanceToNextOccurrence(): Task {
        val currentDue = parseDueDateTime() ?: LocalDateTime.now()
        var nextDue = currentDue
        val now = LocalDateTime.now()
        val isDaily = repeat.equals("Every day", ignoreCase = true) || repeat.equals("Daily", ignoreCase = true)

        do {
            nextDue = if (isDaily) {
                nextDue.plusDays(1)
            } else {
                nextDue.plusWeeks(1)
            }
        } while (nextDue.isBefore(now))

        return this.copy(
            dueAt = nextDue.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            done = false
        )
    }

    fun isOverdue(): Boolean {
        if (done) return false
        val due = parseDueDateTime() ?: return false
        return due.isBefore(LocalDateTime.now())
    }

    fun isToday(): Boolean {
        val due = parseDueDateTime() ?: return false
        val now = LocalDateTime.now()
        return due.toLocalDate() == now.toLocalDate()
    }

    fun isUpcoming(): Boolean {
        if (done) return false
        val due = parseDueDateTime() ?: return false
        val now = LocalDateTime.now()
        return due.isAfter(now)
    }

    fun formattedTime(): String {
        val due = parseDueDateTime() ?: return ""
        val now = LocalDateTime.now()
        val timeStr = due.format(DateTimeFormatter.ofPattern("h:mm a"))
        return when {
            due.toLocalDate() == now.toLocalDate() -> timeStr
            due.toLocalDate() == now.toLocalDate().plusDays(1) -> "Tomorrow, $timeStr"
            due.toLocalDate() > now.toLocalDate() && due.toLocalDate() <= now.toLocalDate().plusDays(6) -> {
                val day = due.format(DateTimeFormatter.ofPattern("EEE"))
                "$day, $timeStr"
            }
            else -> {
                val dateStr = due.format(DateTimeFormatter.ofPattern("d MMM"))
                "$dateStr, $timeStr"
            }
        }
    }
}
