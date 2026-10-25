package com.example.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import com.example.ui.components.NotifyIcons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.TaskRepository
import com.example.data.model.AccentColor
import com.example.data.model.LockScreenPrivacy
import com.example.data.model.ThemeMode
import com.example.data.model.UserSettings
import com.example.data.model.VibrationPattern
import com.example.notifications.NotificationHelper
import com.example.notifications.ReminderAlarmScheduler
import com.example.ui.components.NotifyLogoBadge
import com.example.ui.theme.NotifyTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
fun SettingsScreen(
    settings: UserSettings,
    onThemeChange: (ThemeMode) -> Unit,
    onAccentChange: (AccentColor) -> Unit,
    onSnoozeChange: (Int) -> Unit,
    onDailySummaryChange: (String) -> Unit,
    onSoundChange: (String) -> Unit,
    onRingtoneChange: (String?, String) -> Unit = { _, _ -> },
    onRepeatRingtoneChange: (Boolean) -> Unit = {},
    onVibrateChange: (Boolean) -> Unit = {},
    onVibrationPatternChange: (VibrationPattern) -> Unit = {},
    onSoundEnabledChange: (Boolean) -> Unit = {},
    onWakeScreenOverLockChange: (Boolean) -> Unit = {},
    onLockScreenPrivacyChange: (LockScreenPrivacy) -> Unit = {},
    onQuietHoursChange: (Boolean, String, String, Boolean) -> Unit = { _, _, _, _ -> },
    onProfileChange: (String, String) -> Unit,
    onResetData: () -> Unit,
    onShowPreview: () -> Unit,
    onSendRealNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    // System Ringtone Picker Launcher
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            val title = if (uri != null) {
                try {
                    val ringtone = RingtoneManager.getRingtone(context, uri)
                    ringtone?.getTitle(context) ?: "Custom sound"
                } catch (_: Exception) {
                    "Custom sound"
                }
            } else {
                "Default notification sound"
            }
            onRingtoneChange(uri?.toString(), title)
        }
    }

    // System Status: Battery, Alarms, DND, Channel
    var isIgnoringBatteryOptimizations by remember { mutableStateOf(false) }
    var canScheduleExactAlarms by remember { mutableStateOf(true) }
    var canUseFullScreenIntent by remember { mutableStateOf(true) }
    var isDndActive by remember { mutableStateOf(false) }
    var isChannelMuted by remember { mutableStateOf(false) }

    fun checkPermissions() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        isIgnoringBatteryOptimizations = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            canScheduleExactAlarms = alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            canScheduleExactAlarms = true
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            canUseFullScreenIntent = notificationManager?.canUseFullScreenIntent() ?: true
        } else {
            canUseFullScreenIntent = true
        }

        // Check DND status
        isDndActive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager != null) {
            val filter = notificationManager.currentInterruptionFilter
            filter != NotificationManager.INTERRUPTION_FILTER_ALL && filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        } else false

        // Check channel muted status
        isChannelMuted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            val activeId = NotificationHelper.getCurrentChannelId(context)
            val channel = notificationManager.getNotificationChannel(activeId)
            channel?.importance == NotificationManager.IMPORTANCE_NONE
        } else false
    }

    LaunchedEffect(Unit) {
        checkPermissions()
    }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(settings.userName) }
    var editSub by remember { mutableStateOf(settings.userSub) }

    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showVibrationPatternDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    // TimePicker for Daily Summary
    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val time = LocalTime.of(hourOfDay, minute)
                val formatted = time.format(DateTimeFormatter.ofPattern("h:mm a"))
                onDailySummaryChange(formatted)
            },
            8,
            0,
            false
        )
    }

    // TimePicker for Quiet Hours Start
    val quietStartPicker = remember {
        val initial = try { LocalTime.parse(settings.quietHoursStart) } catch (_: Exception) { LocalTime.of(22, 0) }
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val formatted = String.format("%02d:%02d", hourOfDay, minute)
                onQuietHoursChange(settings.quietHoursEnabled, formatted, settings.quietHoursEnd, settings.quietHoursHighPriorityCanRing)
            },
            initial.hour,
            initial.minute,
            true
        )
    }

    // TimePicker for Quiet Hours End
    val quietEndPicker = remember {
        val initial = try { LocalTime.parse(settings.quietHoursEnd) } catch (_: Exception) { LocalTime.of(7, 0) }
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val formatted = String.format("%02d:%02d", hourOfDay, minute)
                onQuietHoursChange(settings.quietHoursEnabled, settings.quietHoursStart, formatted, settings.quietHoursHighPriorityCanRing)
            },
            initial.hour,
            initial.minute,
            true
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 100.dp)
    ) {
        // Title
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Profile Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = NotifyTheme.colors.cardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, NotifyTheme.colors.border),
            shadowElevation = if (NotifyTheme.colors.isDark) 0.dp else 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable {
                    editName = settings.userName
                    editSub = settings.userSub
                    showEditProfileDialog = true
                }
                .testTag("profile_card")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(18.dp)
            ) {
                // Avatar with the first letter of the name
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(NotifyTheme.colors.accent.copy(alpha = 0.2f))
                        .testTag("profile_avatar"),
                    contentAlignment = Alignment.Center
                ) {
                    val initialLetter = (settings.userName.trim().firstOrNull() ?: 'N').uppercaseChar().toString()
                    Text(
                        text = initialLetter,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = NotifyTheme.colors.accentText
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = settings.userName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${settings.userName} · YCCE, Nagpur",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NotifyTheme.colors.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("profile_sub_label")
                    )
                }

                Text(
                    text = "Edit",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = NotifyTheme.colors.accentText
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // DND or Channel Muted Warning Banner
        if (isDndActive || isChannelMuted) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = NotifyTheme.colors.priorityHigh.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NotifyTheme.colors.priorityHigh.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .testTag("dnd_warning_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = NotifyIcons.DoNotDisturbOn,
                        contentDescription = null,
                        tint = NotifyTheme.colors.priorityHigh,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isDndActive) "Do Not Disturb is Active" else "Notification Channel Muted",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NotifyTheme.colors.priorityHigh
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Alarms and reminders may be silenced by system settings.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NotifyTheme.colors.priorityHigh,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Open Notification Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // PART 1: NOTIFICATIONS SECTION (All 9 Functional Options)
        // -------------------------------------------------------------
        Text(
            text = "Notifications",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            color = NotifyTheme.colors.mutedText
        )

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = NotifyTheme.colors.cardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, NotifyTheme.colors.border),
            shadowElevation = if (NotifyTheme.colors.isDark) 0.dp else 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("notifications_settings_section")
        ) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {

                // 1. Vibrate switch (default on)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Vibrate",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Vibrate on reminder delivery",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = NotifyTheme.colors.mutedText
                        )
                    }
                    Switch(
                        checked = settings.vibrate,
                        onCheckedChange = { onVibrateChange(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NotifyTheme.colors.onAccent,
                            checkedTrackColor = NotifyTheme.colors.accent
                        ),
                        modifier = Modifier.testTag("switch_vibrate")
                    )
                }

                Divider(color = NotifyTheme.colors.border, thickness = 0.8.dp)

                // 2. Vibration pattern: Short, Standard, Long, Pulse (only enabled when Vibrate is on)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = settings.vibrate) { showVibrationPatternDialog = true }
                        .padding(vertical = 14.dp)
                        .testTag("row_vibration_pattern")
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Vibration pattern",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (settings.vibrate) MaterialTheme.colorScheme.onSurface else NotifyTheme.colors.mutedText.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (settings.vibrate) settings.vibrationPattern.label else "Disabled (turn on Vibrate)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = if (settings.vibrate) NotifyTheme.colors.accentText else NotifyTheme.colors.mutedText.copy(alpha = 0.5f)
                        )
                    }
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (settings.vibrate) NotifyTheme.colors.accentText else NotifyTheme.colors.mutedText.copy(alpha = 0.3f)
                    )
                }

                Divider(color = NotifyTheme.colors.border, thickness = 0.8.dp)

                // 3. Sound switch (default on) and Reminder ringtone
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sound",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Play sound when reminders fire",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = NotifyTheme.colors.mutedText
                        )
                    }
                    Switch(
                        checked = settings.soundEnabled,
                        onCheckedChange = { onSoundEnabledChange(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NotifyTheme.colors.onAccent,
                            checkedTrackColor = NotifyTheme.colors.accent
                        ),
                        modifier = Modifier.testTag("switch_sound")
                    )
                }

                if (settings.soundEnabled) {
                    Divider(color = NotifyTheme.colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)

                    // Reminder ringtone picker
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Reminder Ringtone")
                                        val existing = settings.ringtoneUri?.let { Uri.parse(it) }
                                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    }
                                    ringtonePickerLauncher.launch(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Cannot open ringtone picker", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 12.dp)
                            .testTag("row_reminder_ringtone")
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reminder ringtone",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = settings.ringtoneTitle,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = NotifyTheme.colors.mutedText
                            )
                        }
                        Text(
                            text = "Choose",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = NotifyTheme.colors.accentText
                        )
                    }
                }

                Divider(color = NotifyTheme.colors.border, thickness = 0.8.dp)

                // 4. Repeat ringtone until dismissed (switch, max 60 seconds)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Repeat ringtone until dismissed",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Alarms ring & vibrate continuously (up to 60s)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = NotifyTheme.colors.mutedText
                        )
                    }
                    Switch(
                        checked = settings.repeatRingtone,
                        onCheckedChange = { onRepeatRingtoneChange(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NotifyTheme.colors.onAccent,
                            checkedTrackColor = NotifyTheme.colors.accent
                        ),
                        modifier = Modifier.testTag("switch_repeat_ringtone")
                    )
                }

                Divider(color = NotifyTheme.colors.border, thickness = 0.8.dp)

                // 5. Wake screen and show over lock screen (switch, default on)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Wake screen and show over lock screen",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (settings.wakeScreenOverLock) "Full-screen alert on lock screen" else "Only standard notification banner shown",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = NotifyTheme.colors.mutedText
                        )
                    }
                    Switch(
                        checked = settings.wakeScreenOverLock,
                        onCheckedChange = { onWakeScreenOverLockChange(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NotifyTheme.colors.onAccent,
                            checkedTrackColor = NotifyTheme.colors.accent
                        ),
                        modifier = Modifier.testTag("switch_wake_screen")
                    )
                }

                Divider(color = NotifyTheme.colors.border, thickness = 0.8.dp)

                // 6. Lock screen privacy: "Show task title" or "Hide details"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPrivacyDialog = true }
                        .padding(vertical = 14.dp)
                        .testTag("row_lockscreen_privacy")
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lock screen privacy",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = settings.lockScreenPrivacy.label,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = NotifyTheme.colors.accentText
                        )
                    }
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = NotifyTheme.colors.accentText
                    )
                }

                Divider(color = NotifyTheme.colors.border, thickness = 0.8.dp)

                // 7. Quiet hours (switch plus start and end time + high priority option)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Quiet hours",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (settings.quietHoursEnabled) "${settings.quietHoursStart} – ${settings.quietHoursEnd} (Silent mode)" else "Disabled",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = NotifyTheme.colors.mutedText
                        )
                    }
                    Switch(
                        checked = settings.quietHoursEnabled,
                        onCheckedChange = {
                            onQuietHoursChange(it, settings.quietHoursStart, settings.quietHoursEnd, settings.quietHoursHighPriorityCanRing)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NotifyTheme.colors.onAccent,
                            checkedTrackColor = NotifyTheme.colors.accent
                        ),
                        modifier = Modifier.testTag("switch_quiet_hours")
                    )
                }

                if (settings.quietHoursEnabled) {
                    // Start and End time selectors
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { quietStartPicker.show() },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NotifyTheme.colors.border),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text("From: ${settings.quietHoursStart}", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { quietEndPicker.show() },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NotifyTheme.colors.border),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text("To: ${settings.quietHoursEnd}", fontSize = 13.sp)
                        }
                    }

                    // High priority override switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "High priority tasks can still ring",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Urgent tasks override quiet hours silence",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = NotifyTheme.colors.mutedText
                            )
                        }
                        Switch(
                            checked = settings.quietHoursHighPriorityCanRing,
                            onCheckedChange = {
                                onQuietHoursChange(settings.quietHoursEnabled, settings.quietHoursStart, settings.quietHoursEnd, it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NotifyTheme.colors.onAccent,
                                checkedTrackColor = NotifyTheme.colors.accent
                            ),
                            modifier = Modifier.testTag("switch_quiet_hours_high_priority")
                        )
                    }
                }

                Divider(color = NotifyTheme.colors.border, thickness = 0.8.dp)

                // 8. Default snooze length: 5, 10, 15 or 30 min
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSnoozeDialog = true }
                        .padding(vertical = 14.dp)
                        .testTag("row_default_snooze")
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Default snooze length",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${settings.defaultSnoozeMinutes} min (Notification snooze button)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = NotifyTheme.colors.accentText
                        )
                    }
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = NotifyTheme.colors.accentText
                    )
                }

                Divider(color = NotifyTheme.colors.border, thickness = 0.8.dp)

                // Daily summary time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { timePickerDialog.show() }
                        .padding(vertical = 14.dp)
                ) {
                    Text(
                        text = "Daily morning briefing",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = settings.dailySummaryTime,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = NotifyTheme.colors.accentText
                    )
                }

                Divider(color = NotifyTheme.colors.border, thickness = 0.8.dp)

                Spacer(modifier = Modifier.height(14.dp))

                // 9. "Test with current settings" button
                Button(
                    onClick = {
                        val testTask = com.example.data.model.Task(
                            id = UUID.randomUUID().toString(),
                            title = "Test Reminder (Current Settings)",
                            dueAt = java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            priority = com.example.data.model.Priority.HIGH,
                            tag = "Test",
                            notes = "Testing active vibrate, sound, ringtone, and privacy settings!"
                        )
                        NotificationHelper.showNotification(context, testTask)
                        Toast.makeText(context, "Notification sent with current settings!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NotifyTheme.colors.accent,
                        contentColor = NotifyTheme.colors.onAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("test_with_current_settings_button")
                ) {
                    Icon(
                        imageVector = NotifyIcons.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Test with current settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Appearance Section
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            color = NotifyTheme.colors.mutedText
        )

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = NotifyTheme.colors.cardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, NotifyTheme.colors.border),
            shadowElevation = if (NotifyTheme.colors.isDark) 0.dp else 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Theme Segmented Control: Light / Dark / System
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (NotifyTheme.colors.isDark) Color(0xFF14171E) else Color(0xFFEFF2F7),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val isSelected = settings.themeMode == mode
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isSelected) NotifyTheme.colors.accent else Color.Transparent)
                                    .clickable { onThemeChange(mode) }
                                    .testTag("theme_mode_${mode.name}")
                            ) {
                                Text(
                                    text = mode.label,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) NotifyTheme.colors.onAccent else NotifyTheme.colors.mutedText
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Accent Color Label
                Text(
                    text = "Accent color",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    color = NotifyTheme.colors.mutedText
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Accent Color Circles
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AccentColor.entries.forEach { accent ->
                        val isSelected = settings.accentColor == accent
                        val circleColor = Color(accent.colorHex)

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(circleColor)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onAccentChange(accent) }
                                .testTag("accent_color_${accent.name}")
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(accent.onAccentHex),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Background Delivery & Battery Section
        Text(
            text = "Reliable Delivery & System Tests",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            color = NotifyTheme.colors.mutedText
        )

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = NotifyTheme.colors.cardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, NotifyTheme.colors.border),
            shadowElevation = if (NotifyTheme.colors.isDark) 0.dp else 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Battery Optimization
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(fallback)
                                } catch (e2: Exception) {
                                    Toast.makeText(context, "Open Settings to adjust battery optimization", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isIgnoringBatteryOptimizations)
                                    NotifyTheme.colors.priorityLow.copy(alpha = 0.15f)
                                else
                                    NotifyTheme.colors.priorityMedium.copy(alpha = 0.15f)
                            )
                    ) {
                        Icon(
                            imageVector = if (isIgnoringBatteryOptimizations) NotifyIcons.BatteryChargingFull else NotifyIcons.BatteryAlert,
                            contentDescription = null,
                            tint = if (isIgnoringBatteryOptimizations) NotifyTheme.colors.priorityLow else NotifyTheme.colors.priorityMedium,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Battery: allow unrestricted",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isIgnoringBatteryOptimizations) "Status: Unrestricted (Optimal)" else "Status: Optimized (May delay background alarms)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = if (isIgnoringBatteryOptimizations) NotifyTheme.colors.priorityLow else NotifyTheme.colors.priorityMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = NotifyTheme.colors.border, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Schedule test in 1 minute button
                Button(
                    onClick = {
                        val due = java.time.LocalDateTime.now().plusMinutes(1)
                        val testTask = com.example.data.model.Task(
                            id = UUID.randomUUID().toString(),
                            title = "Test Reminder (1 min)",
                            dueAt = due.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            repeat = "None",
                            remindBefore = 0,
                            priority = com.example.data.model.Priority.HIGH,
                            tag = "Test",
                            notes = "Scheduled exact alarm test fired!"
                        )
                        val repo = TaskRepository.getInstance(context)
                        repo.addTask(testTask)
                        ReminderAlarmScheduler.scheduleReminder(context, testTask)
                        Toast.makeText(
                            context,
                            "Reminder scheduled in 1 minute! Lock your phone or close app to test.",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (NotifyTheme.colors.isDark) Color(0xFF262C38) else Color(0xFFE4E9F2),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("schedule_test_1min_button")
                ) {
                    Icon(
                        imageVector = NotifyIcons.Schedule,
                        contentDescription = null,
                        tint = NotifyTheme.colors.accentText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Schedule test in 1 minute",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Test locked-screen alert (fires in 10 seconds)
                Button(
                    onClick = {
                        val due = java.time.LocalDateTime.now().plusSeconds(10)
                        val testTask = com.example.data.model.Task(
                            id = UUID.randomUUID().toString(),
                            title = "Test Lock-Screen Alert",
                            dueAt = due.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            repeat = "None",
                            remindBefore = 0,
                            priority = com.example.data.model.Priority.HIGH,
                            tag = "Alarm",
                            notes = "Screen was woken and alert sounded on top of lock screen!"
                        )
                        val repo = TaskRepository.getInstance(context)
                        repo.addTask(testTask)
                        ReminderAlarmScheduler.scheduleReminder(context, testTask)
                        Toast.makeText(
                            context,
                            "Lock your screen NOW! Alert will wake screen and ring in 10 seconds.",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NotifyTheme.colors.accent,
                        contentColor = NotifyTheme.colors.onAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("test_lockscreen_alert_10s_button")
                ) {
                    Icon(
                        imageVector = NotifyIcons.Alarm,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Test locked-screen alert (fires in 10 seconds)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // About Section (Bell with N badge, "Notify", and "by Nahar" below it)
        Text(
            text = "About",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            color = NotifyTheme.colors.mutedText
        )

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = NotifyTheme.colors.cardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, NotifyTheme.colors.border),
            shadowElevation = if (NotifyTheme.colors.isDark) 0.dp else 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                NotifyLogoBadge(size = 32.dp)

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Notify",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Notify · by Nahar",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    color = NotifyTheme.colors.mutedText
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Smart, reliable reminders · YCCE Nagpur",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = NotifyTheme.colors.mutedText
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = NotifyTheme.colors.mutedText.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Reset Sample Data
        OutlinedButton(
            onClick = onResetData,
            shape = RoundedCornerShape(50),
            border = androidx.compose.foundation.BorderStroke(1.dp, NotifyTheme.colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = NotifyTheme.colors.mutedText,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Reset 5 Sample Reminders",
                color = NotifyTheme.colors.mutedText,
                fontSize = 13.sp
            )
        }
    }

    // Vibration Pattern Picker Dialog
    if (showVibrationPatternDialog) {
        AlertDialog(
            onDismissRequest = { showVibrationPatternDialog = false },
            title = { Text("Vibration Pattern", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    VibrationPattern.entries.forEach { pattern ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onVibrationPatternChange(pattern)
                                    showVibrationPatternDialog = false
                                }
                                .padding(vertical = 12.dp)
                                .testTag("vibration_pattern_${pattern.name}")
                        ) {
                            Text(
                                text = pattern.label,
                                fontSize = 16.sp,
                                fontWeight = if (settings.vibrationPattern == pattern) FontWeight.Bold else FontWeight.Normal,
                                color = if (settings.vibrationPattern == pattern) NotifyTheme.colors.accentText else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (settings.vibrationPattern == pattern) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = NotifyTheme.colors.accentText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = NotifyTheme.colors.cardBg,
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Lock Screen Privacy Picker Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Lock Screen Privacy", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    LockScreenPrivacy.entries.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLockScreenPrivacyChange(option)
                                    showPrivacyDialog = false
                                }
                                .padding(vertical = 12.dp)
                                .testTag("privacy_${option.name}")
                        ) {
                            Text(
                                text = option.label,
                                fontSize = 16.sp,
                                fontWeight = if (settings.lockScreenPrivacy == option) FontWeight.Bold else FontWeight.Normal,
                                color = if (settings.lockScreenPrivacy == option) NotifyTheme.colors.accentText else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (settings.lockScreenPrivacy == option) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = NotifyTheme.colors.accentText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = NotifyTheme.colors.cardBg,
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Snooze Picker Dialog
    if (showSnoozeDialog) {
        AlertDialog(
            onDismissRequest = { showSnoozeDialog = false },
            title = { Text("Default Snooze Time", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf(5, 10, 15, 30).forEach { min ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSnoozeChange(min)
                                    showSnoozeDialog = false
                                }
                                .padding(vertical = 12.dp)
                                .testTag("snooze_min_$min")
                        ) {
                            Text(
                                text = "$min min",
                                fontSize = 16.sp,
                                fontWeight = if (settings.defaultSnoozeMinutes == min) FontWeight.Bold else FontWeight.Normal,
                                color = if (settings.defaultSnoozeMinutes == min) NotifyTheme.colors.accentText else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (settings.defaultSnoozeMinutes == min) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = NotifyTheme.colors.accentText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = NotifyTheme.colors.cardBg,
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = {
                            if (it.length <= 20) {
                                editName = it
                            }
                        },
                        label = { Text("Your name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_name_input")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${editName.trim().length}/20 characters (empty resets to Nahar)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = NotifyTheme.colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "College / Department",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "YCCE, Nagpur",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sanitized = UserSettings.sanitizeUserName(editName)
                        onProfileChange(sanitized, "YCCE, Nagpur")
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NotifyTheme.colors.accent,
                        contentColor = NotifyTheme.colors.onAccent
                    ),
                    modifier = Modifier.testTag("save_profile_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditProfileDialog = false },
                    modifier = Modifier.testTag("cancel_profile_button")
                ) {
                    Text("Cancel")
                }
            },
            containerColor = NotifyTheme.colors.cardBg,
            shape = RoundedCornerShape(18.dp)
        )
    }
}
