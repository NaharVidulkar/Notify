package com.example.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AccentColor
import com.example.data.model.ThemeMode
import com.example.data.model.UserSettings
import com.example.ui.theme.NotifyTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    settings: UserSettings,
    onThemeChange: (ThemeMode) -> Unit,
    onAccentChange: (AccentColor) -> Unit,
    onSnoozeChange: (Int) -> Unit,
    onDailySummaryChange: (String) -> Unit,
    onSoundChange: (String) -> Unit,
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

    // Battery optimization state
    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(false)
    }
    var canScheduleExactAlarms by remember {
        mutableStateOf(true)
    }

    fun checkPermissions() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        isIgnoringBatteryOptimizations = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            canScheduleExactAlarms = alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            canScheduleExactAlarms = true
        }
    }

    LaunchedEffect(Unit) {
        checkPermissions()
    }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(settings.userName) }
    var editSub by remember { mutableStateOf(settings.userSub) }

    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showSoundDialog by remember { mutableStateOf(false) }

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
                // Circle Avatar "N"
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(NotifyTheme.colors.accent)
                ) {
                    Text(
                        text = settings.userName.take(1).uppercase(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = NotifyTheme.colors.onAccent
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = settings.userName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = settings.userSub,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NotifyTheme.colors.mutedText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Appearance Section
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
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

        // Reminders Section
        Text(
            text = "Reminders",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
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
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                // Default snooze
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSnoozeDialog = true }
                        .padding(vertical = 16.dp)
                ) {
                    Text(
                        text = "Default snooze",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${settings.defaultSnoozeMinutes} min",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Divider(color = NotifyTheme.colors.border, thickness = 0.8.dp)

                // Daily summary
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { timePickerDialog.show() }
                        .padding(vertical = 16.dp)
                ) {
                    Text(
                        text = "Daily summary",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = settings.dailySummaryTime,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Divider(color = NotifyTheme.colors.border, thickness = 0.8.dp)

                // Sound
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSoundDialog = true }
                        .padding(vertical = 16.dp)
                ) {
                    Text(
                        text = "Sound",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = settings.sound,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Background Delivery & Battery Optimization Section
        Text(
            text = "Reliable Delivery & Battery",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
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
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
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
                            imageVector = if (isIgnoringBatteryOptimizations) Icons.Default.BatteryChargingFull else Icons.Default.BatteryAlert,
                            contentDescription = null,
                            tint = if (isIgnoringBatteryOptimizations) NotifyTheme.colors.priorityLow else NotifyTheme.colors.priorityMedium,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Battery Optimization",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (isIgnoringBatteryOptimizations) "Unrestricted (Reminders deliver reliably)" else "Optimized (Reminders may be delayed during sleep)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = if (isIgnoringBatteryOptimizations) NotifyTheme.colors.priorityLow else NotifyTheme.colors.priorityMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Android power-saving modes can postpone alarms. To guarantee your reminders ring on the exact minute, disable battery optimization for Notify.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                            color = NotifyTheme.colors.mutedText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
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
                                Toast.makeText(context, "Open Settings > Apps > Notify to adjust battery optimization", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isIgnoringBatteryOptimizations) NotifyTheme.colors.cardBg else NotifyTheme.colors.accent,
                        contentColor = if (isIgnoringBatteryOptimizations) MaterialTheme.colorScheme.onSurface else NotifyTheme.colors.onAccent
                    ),
                    border = if (isIgnoringBatteryOptimizations) androidx.compose.foundation.BorderStroke(1.dp, NotifyTheme.colors.border) else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isIgnoringBatteryOptimizations) "Adjust Battery Optimization Settings" else "Disable Battery Optimization",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                // Exact Alarms permission on Android 12+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = NotifyTheme.colors.border, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canScheduleExactAlarms)
                                        NotifyTheme.colors.priorityLow.copy(alpha = 0.15f)
                                    else
                                        NotifyTheme.colors.priorityHigh.copy(alpha = 0.15f)
                                )
                        ) {
                            Icon(
                                imageVector = if (canScheduleExactAlarms) Icons.Default.Check else Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (canScheduleExactAlarms) NotifyTheme.colors.priorityLow else NotifyTheme.colors.priorityHigh,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exact Alarms (Android 12+)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (canScheduleExactAlarms) "Exact alarms allowed" else "Exact alarms restricted",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = if (canScheduleExactAlarms) NotifyTheme.colors.priorityLow else NotifyTheme.colors.priorityHigh
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Required for AlarmManager.setExactAndAllowWhileIdle to wake the device right on time.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = NotifyTheme.colors.mutedText
                            )
                        }
                    }

                    if (!canScheduleExactAlarms) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Open Settings to allow exact alarms", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NotifyTheme.colors.accent,
                                contentColor = NotifyTheme.colors.onAccent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Exact Alarm Permission", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Notification Actions & Previews
        Text(
            text = "Notifications & Actions",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
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
                // Permission Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Channel: Reminders",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (hasNotificationPermission) "High Importance · Sound & Vibrate enabled" else "Permission required to show alerts",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = if (hasNotificationPermission) NotifyTheme.colors.priorityLow else NotifyTheme.colors.priorityHigh
                        )
                    }

                    if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NotifyTheme.colors.accent,
                                contentColor = NotifyTheme.colors.onAccent
                            )
                        ) {
                            Text("Enable")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Preview Lockscreen Alert Button (Mockup test)
                Button(
                    onClick = onShowPreview,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NotifyTheme.colors.accent,
                        contentColor = NotifyTheme.colors.onAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("preview_lockscreen_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Preview Lockscreen Notification Banner",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Real notification trigger
                OutlinedButton(
                    onClick = onSendRealNotification,
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NotifyTheme.colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Test Send Notification to Android Tray",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                }
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

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editSub,
                        onValueChange = { editSub = it },
                        label = { Text("College / Department") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            onProfileChange(editName.trim(), editSub.trim())
                            showEditProfileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NotifyTheme.colors.accent,
                        contentColor = NotifyTheme.colors.onAccent
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = NotifyTheme.colors.cardBg,
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Snooze Picker Dialog
    if (showSnoozeDialog) {
        AlertDialog(
            onDismissRequest = { showSnoozeDialog = false },
            title = { Text("Default Snooze Time") },
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
                        ) {
                            Text(
                                text = "$min min",
                                fontSize = 16.sp,
                                fontWeight = if (settings.defaultSnoozeMinutes == min) FontWeight.Bold else FontWeight.Normal,
                                color = if (settings.defaultSnoozeMinutes == min) NotifyTheme.colors.accentText else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = NotifyTheme.colors.cardBg,
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Sound Picker Dialog
    if (showSoundDialog) {
        AlertDialog(
            onDismissRequest = { showSoundDialog = false },
            title = { Text("Notification Sound") },
            text = {
                Column {
                    listOf("Chime", "Radar", "Bell", "Subtle").forEach { s ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSoundChange(s)
                                    showSoundDialog = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = s,
                                fontSize = 16.sp,
                                fontWeight = if (settings.sound == s) FontWeight.Bold else FontWeight.Normal,
                                color = if (settings.sound == s) NotifyTheme.colors.accentText else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = NotifyTheme.colors.cardBg,
            shape = RoundedCornerShape(18.dp)
        )
    }
}
