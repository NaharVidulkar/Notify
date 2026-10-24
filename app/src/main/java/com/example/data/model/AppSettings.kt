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

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val accentColor: AccentColor = AccentColor.AMBER,
    val defaultSnoozeMinutes: Int = 10,
    val dailySummaryTime: String = "8:00 AM",
    val sound: String = "Chime",
    val userName: String = "Nahar",
    val userSub: String = "YCCE · Nagpur",
    val todaysFocus: String = "DSA – Graphs & DP",
    val focusSubtext: String = "5 reminders today · Java track"
)
