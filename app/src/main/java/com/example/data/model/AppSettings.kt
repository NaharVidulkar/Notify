package com.example.data.model

enum class ThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System")
}

enum class AccentColor(val label: String, val colorHex: Long, val onAccentHex: Long) {
    AMBER("Amber", 0xFFFFB547, 0xFF1A1206),
    BLUE("Blue", 0xFF4C7DFF, 0xFFFFFFFF),
    GREEN("Green", 0xFF2FA37A, 0xFFFFFFFF),
    VIOLET("Violet", 0xFF8B5CF6, 0xFFFFFFFF);

    companion object {
        fun fromName(name: String): AccentColor {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: AMBER
        }
    }
}

enum class VibrationPattern(val label: String, val pattern: LongArray) {
    SHORT("Short", longArrayOf(0, 150, 100, 150)),
    STANDARD("Standard", longArrayOf(0, 300, 200, 300)),
    LONG("Long", longArrayOf(0, 600, 250, 600)),
    PULSE("Pulse", longArrayOf(0, 100, 100, 100, 100, 100, 100, 300));

    companion object {
        fun fromName(name: String?): VibrationPattern {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: STANDARD
        }
    }
}

enum class LockScreenPrivacy(val label: String) {
    SHOW_TITLE("Show task title"),
    HIDE_DETAILS("Hide details");

    companion object {
        fun fromName(name: String?): LockScreenPrivacy {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: SHOW_TITLE
        }
    }
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val accentColor: AccentColor = AccentColor.AMBER,
    val defaultSnoozeMinutes: Int = 10,
    val dailySummaryTime: String = "8:00 AM",
    val sound: String = "Chime",
    val userName: String = "Nahar",
    val userSub: String = "YCCE · Nagpur",
    val todaysFocus: String = "DSA – Graphs & DP",
    val focusSubtext: String = "5 reminders today · Java track",
    val ringtoneUri: String? = null,
    val ringtoneTitle: String = "Default notification sound",
    val repeatRingtone: Boolean = true,
    // Functional Notification Settings
    val vibrate: Boolean = true,
    val vibrationPattern: VibrationPattern = VibrationPattern.STANDARD,
    val soundEnabled: Boolean = true,
    val wakeScreenOverLock: Boolean = true,
    val lockScreenPrivacy: LockScreenPrivacy = LockScreenPrivacy.SHOW_TITLE,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00",
    val quietHoursHighPriorityCanRing: Boolean = true
) {
    companion object {
        const val DEFAULT_USER_NAME = "Nahar"

        fun sanitizeUserName(name: String?): String {
            val trimmed = name?.trim() ?: ""
            if (trimmed.isEmpty()) return DEFAULT_USER_NAME
            return trimmed.take(20)
        }
    }
}

