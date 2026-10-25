package com.example.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.example.ui.components.NotifyLogoBadge
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.Task
import com.example.data.model.UserSettings
import com.example.ui.components.TaskCard
import com.example.ui.theme.NotifyTheme
import com.example.viewmodel.TaskFilter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(
    tasks: List<Task>,
    currentFilter: TaskFilter,
    settings: UserSettings,
    todayRemindersCount: Int,
    onFilterChange: (TaskFilter) -> Unit,
    onToggleTask: (String) -> Unit,
    onEditTask: (Task) -> Unit,
    onDeleteTask: (String) -> Unit,
    onSnoozeTask: (String) -> Unit,
    onEditFocus: () -> Unit,
    onShowNotificationPreview: (Task) -> Unit,
    onNameChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("notify_prefs", Context.MODE_PRIVATE) }

    val today = remember { LocalDate.now() }
    val dateString = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEEE, d MMM"))
    }

    var selectedTaskForAction by remember { mutableStateOf<Task?>(null) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember(settings.userName) { mutableStateOf(settings.userName) }

    // Notification Permission State (Android 13+)
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

    // Exact Alarm State (Android 12+)
    var canScheduleExactAlarms by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                alarmManager?.canScheduleExactAlarms() ?: true
            } else {
                true
            }
        )
    }

    // Full-screen Intent State (Android 14+)
    var canUseFullScreenIntent by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.canUseFullScreenIntent() ?: true
            } else {
                true
            }
        )
    }

    // Explanation dialog on first launch
    var showPermissionRationaleDialog by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                !prefs.getBoolean("has_prompted_notif_explanation", false)
            } else {
                false
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        prefs.edit().putBoolean("has_prompted_notif_explanation", true).apply()
    }

    // Refresh permission statuses on resume/recomposition
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            canScheduleExactAlarms = alarmManager?.canScheduleExactAlarms() ?: true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            canUseFullScreenIntent = notificationManager?.canUseFullScreenIntent() ?: true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // PART 1: Small Brand Row (above greeting)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            NotifyLogoBadge(size = 32.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notify",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "by Nahar",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = NotifyTheme.colors.mutedText
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Date Label
        Text(
            text = dateString,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = NotifyTheme.colors.mutedText
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Greeting: fixed 'Jay Shree Mataji 🙏, ' and editable accent name on one line
        val nameLength = settings.userName.length
        val greetingFontSize = when {
            nameLength > 16 -> 15.sp
            nameLength > 12 -> 17.sp
            nameLength > 8 -> 19.sp
            nameLength > 5 -> 21.sp
            else -> 23.sp
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("greeting_row")
        ) {
            Text(
                text = "Jay Shree Mataji 🙏, ",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = greetingFontSize,
                    letterSpacing = (-0.4).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                softWrap = false
            )

            val accentTextColor = NotifyTheme.colors.accentText
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        editNameInput = settings.userName
                        showEditNameDialog = true
                    }
                    .drawBehind {
                        val strokeWidth = 1.5.dp.toPx()
                        val y = size.height - 1.dp.toPx()
                        drawLine(
                            color = accentTextColor.copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeWidth
                        )
                    }
                    .padding(bottom = 2.dp)
                    .testTag("greeting_name_tap")
            ) {
                Text(
                    text = settings.userName,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = greetingFontSize,
                        letterSpacing = (-0.4).sp
                    ),
                    color = NotifyTheme.colors.accentText,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit name",
                    tint = NotifyTheme.colors.accentText.copy(alpha = 0.75f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Notification Permission Warning Banner (if denied on Android 13+)
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = NotifyTheme.colors.priorityHigh.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NotifyTheme.colors.priorityHigh.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = NotifyTheme.colors.priorityHigh,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notifications are off",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NotifyTheme.colors.priorityHigh
                        )
                        Text(
                            text = "Enable notifications to receive your reminders on time.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            }
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NotifyTheme.colors.priorityHigh,
                            contentColor = androidx.compose.ui.graphics.Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Enable", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Exact Alarm Permission Warning Banner (if restricted on Android 12+)
        if (!canScheduleExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = NotifyTheme.colors.accent.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NotifyTheme.colors.accent.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = NotifyIcons.Schedule,
                        contentDescription = null,
                        tint = NotifyTheme.colors.accentText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Exact alarms restricted",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NotifyTheme.colors.accentText
                        )
                        Text(
                            text = "Allow exact alarms so reminders ring at the exact minute.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback
                            }
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NotifyTheme.colors.accent,
                            contentColor = NotifyTheme.colors.onAccent
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Allow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Full-screen Intent Permission Banner (Android 14+)
        if (!canUseFullScreenIntent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = NotifyTheme.colors.accent.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NotifyTheme.colors.accent.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = NotifyTheme.colors.accentText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lock-screen alerts restricted",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Allow full-screen reminders so Notify can wake the screen.",
                            fontSize = 12.sp,
                            color = NotifyTheme.colors.mutedText
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(fallback)
                                } catch (_: Exception) {}
                            }
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NotifyTheme.colors.accent,
                            contentColor = NotifyTheme.colors.onAccent
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Allow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Today's focus card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = NotifyTheme.colors.cardBg,
            tonalElevation = if (NotifyTheme.colors.isDark) 0.dp else 1.dp,
            shadowElevation = if (NotifyTheme.colors.isDark) 0.dp else 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NotifyTheme.colors.border, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .clickable { onEditFocus() }
                .testTag("todays_focus_card")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(18.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Today's focus",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = NotifyTheme.colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = settings.todaysFocus,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$todayRemindersCount reminders today · Java track",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = NotifyTheme.colors.mutedText
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NotifyTheme.colors.accent.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit focus",
                        tint = NotifyTheme.colors.accentText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(TaskFilter.entries) { filter ->
                val isSelected = currentFilter == filter
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) NotifyTheme.colors.accent else NotifyTheme.colors.cardBg,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) NotifyTheme.colors.accent else NotifyTheme.colors.border
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onFilterChange(filter) }
                        .testTag("filter_chip_${filter.name}")
                ) {
                    Text(
                        text = filter.label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) NotifyTheme.colors.onAccent else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Task List or Empty State
        if (tasks.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(NotifyTheme.colors.accent.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = NotifyIcons.NotificationsNone,
                            contentDescription = null,
                            tint = NotifyTheme.colors.accentText,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No reminders here",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = when (currentFilter) {
                            TaskFilter.ALL -> "You're all caught up! Tap '+' below to create a reminder."
                            TaskFilter.TODAY -> "No reminders scheduled for today."
                            TaskFilter.UPCOMING -> "No upcoming reminders ahead."
                            TaskFilter.DONE -> "No completed tasks yet. Finish a task to see it here!"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = NotifyTheme.colors.mutedText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onToggle = { onToggleTask(task.id) },
                        onClick = { selectedTaskForAction = task },
                        onLongClick = { onEditTask(task) }
                    )
                }
            }
        }
    }

    // Explanation Dialog on first launch (Android 13+)
    if (showPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = {
                prefs.edit().putBoolean("has_prompted_notif_explanation", true).apply()
                showPermissionRationaleDialog = false
            },
            icon = {
                Icon(
                    imageVector = NotifyIcons.NotificationsActive,
                    contentDescription = null,
                    tint = NotifyTheme.colors.accentText,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Enable Reminders",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Notify needs notifications to remind you on time.",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationaleDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NotifyTheme.colors.accent,
                        contentColor = NotifyTheme.colors.onAccent
                    )
                ) {
                    Text("Allow Notifications", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        prefs.edit().putBoolean("has_prompted_notif_explanation", true).apply()
                        showPermissionRationaleDialog = false
                    }
                ) {
                    Text("Not Now", color = NotifyTheme.colors.mutedText)
                }
            },
            containerColor = NotifyTheme.colors.cardBg,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Task Action Dialog
    selectedTaskForAction?.let { task ->
        AlertDialog(
            onDismissRequest = { selectedTaskForAction = null },
            title = {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${task.formattedTime()} · ${task.tag} · ${task.priority.label} Priority",
                        color = NotifyTheme.colors.mutedText,
                        fontSize = 14.sp
                    )

                    Divider(color = NotifyTheme.colors.border, thickness = 0.8.dp)

                    // Mark Complete / Incomplete
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onToggleTask(task.id)
                                selectedTaskForAction = null
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = NotifyTheme.colors.accent
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (task.done) "Mark as pending" else "Mark as completed",
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Edit
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedTaskForAction = null
                                onEditTask(task)
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Edit reminder", fontWeight = FontWeight.Medium)
                    }

                    // Snooze 10 min
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSnoozeTask(task.id)
                                selectedTaskForAction = null
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = NotifyIcons.Schedule,
                            contentDescription = null,
                            tint = NotifyTheme.colors.accentText
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Snooze (${settings.defaultSnoozeMinutes} min)", fontWeight = FontWeight.Medium)
                    }

                    // Lockscreen Preview
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val t = task
                                selectedTaskForAction = null
                                onShowNotificationPreview(t)
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = NotifyTheme.colors.accent
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Preview lockscreen alert", fontWeight = FontWeight.Medium)
                    }

                    // Delete
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDeleteTask(task.id)
                                selectedTaskForAction = null
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = NotifyTheme.colors.priorityHigh
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Delete reminder", color = NotifyTheme.colors.priorityHigh, fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedTaskForAction = null }) {
                    Text("Close", color = NotifyTheme.colors.mutedText)
                }
            },
            containerColor = NotifyTheme.colors.cardBg,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Edit Name Dialog (Today Screen Greeting)
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(
                    text = "Edit Your Name",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Greeting displays: Jay Shree Mataji 🙏, [Name]",
                        style = MaterialTheme.typography.bodySmall,
                        color = NotifyTheme.colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = {
                            if (it.length <= 20) {
                                editNameInput = it
                            }
                        },
                        label = { Text("Your name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_greeting_name_input")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${editNameInput.trim().length}/20 characters (empty resets to Nahar)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = NotifyTheme.colors.mutedText
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sanitized = UserSettings.sanitizeUserName(editNameInput)
                        onNameChange(sanitized)
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NotifyTheme.colors.accent,
                        contentColor = NotifyTheme.colors.onAccent
                    ),
                    modifier = Modifier.testTag("save_greeting_name_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditNameDialog = false },
                    modifier = Modifier.testTag("cancel_greeting_name_button")
                ) {
                    Text("Cancel")
                }
            },
            containerColor = NotifyTheme.colors.cardBg,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

