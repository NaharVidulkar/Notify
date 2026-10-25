package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import com.example.ui.components.NotifyLogoBadge
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import com.example.ui.components.NotifyIcons
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ThemeMode
import com.example.notifications.NotificationHelper
import com.example.ui.components.EditFocusDialog
import com.example.ui.components.NotificationPreviewDialog
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.NewEditReminderSheet
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.NotifyAppTheme
import com.example.ui.theme.NotifyTheme
import com.example.viewmodel.BottomTab
import com.example.viewmodel.TaskViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)
        com.example.notifications.ReminderAlarmScheduler.rescheduleAllPendingReminders(this)

        setContent {
            val settings by viewModel.settings.collectAsState()
            val isSystemDark = isSystemInDarkTheme()

            val isDark = when (settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemDark
            }

            val accentColor = Color(settings.accentColor.colorHex)
            val onAccentColor = Color(settings.accentColor.onAccentHex)

            var isSplashVisible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }

            androidx.compose.runtime.LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1200)
                isSplashVisible = false
            }

            NotifyAppTheme(
                darkTheme = isDark,
                accentColor = accentColor,
                onAccentColor = onAccentColor
            ) {
                androidx.compose.animation.AnimatedContent(
                    targetState = isSplashVisible,
                    label = "splashTransition"
                ) { splash ->
                    if (splash) {
                        SplashScreen()
                    } else {
                        NotifyApp(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            NotifyLogoBadge(size = 32.dp)
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Notify",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "by Nahar",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = NotifyTheme.colors.mutedText
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Smart, reliable reminders",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp
                ),
                color = NotifyTheme.colors.mutedText.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun NotifyApp(viewModel: TaskViewModel) {
    val tasks by viewModel.filteredTasks.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val customTags by viewModel.customTags.collectAsState()
    val selectedCalendarDate by viewModel.selectedCalendarDate.collectAsState()
    val todayRemindersCount by viewModel.todayRemindersCount.collectAsState()

    val isNewReminderOpen by viewModel.isNewReminderOpen.collectAsState()
    val editingTask by viewModel.editingTask.collectAsState()
    val isEditFocusOpen by viewModel.isEditFocusOpen.collectAsState()
    val isNotificationPreviewOpen by viewModel.isNotificationPreviewOpen.collectAsState()
    val previewTask by viewModel.previewTask.collectAsState()

    // Back handling
    BackHandler(enabled = isNewReminderOpen) {
        viewModel.closeReminderSheet()
    }
    BackHandler(enabled = !isNewReminderOpen && currentTab != BottomTab.TODAY) {
        viewModel.setTab(BottomTab.TODAY)
    }

    // Outer Container centering app on wide displays (max width 430dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (NotifyTheme.colors.isDark) Color(0xFF0A0C0E) else Color(0xFFEAEDF3)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 430.dp)
                .fillMaxWidth()
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                bottomBar = {
                    NotifyBottomBar(
                        currentTab = currentTab,
                        onTabSelected = { viewModel.setTab(it) }
                    )
                },
                floatingActionButton = {
                    if (currentTab != BottomTab.SETTINGS && !isNewReminderOpen) {
                        FloatingActionButton(
                            onClick = { viewModel.openNewReminder() },
                            containerColor = NotifyTheme.colors.accent,
                            contentColor = NotifyTheme.colors.onAccent,
                            shape = CircleShape,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                            modifier = Modifier
                                .padding(end = 6.dp, bottom = 12.dp)
                                .size(56.dp)
                                .testTag("fab_add_reminder")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add reminder",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        BottomTab.TODAY -> {
                            TodayScreen(
                                tasks = tasks,
                                currentFilter = currentFilter,
                                settings = settings,
                                todayRemindersCount = todayRemindersCount,
                                onFilterChange = { viewModel.setFilter(it) },
                                onToggleTask = { viewModel.toggleTask(it) },
                                onEditTask = { viewModel.openEditReminder(it) },
                                onDeleteTask = { viewModel.deleteTask(it) },
                                onSnoozeTask = { viewModel.snoozeTask(it) },
                                onEditFocus = { viewModel.openEditFocus() },
                                onShowNotificationPreview = { viewModel.showNotificationPreview(it) },
                                onNameChange = { viewModel.updateUserName(it) }
                            )
                        }
                        BottomTab.CALENDAR -> {
                            CalendarScreen(
                                tasks = allTasks,
                                selectedDate = selectedCalendarDate,
                                onSelectDate = { viewModel.selectCalendarDate(it) },
                                onToggleTask = { viewModel.toggleTask(it) },
                                onEditTask = { viewModel.openEditReminder(it) },
                                onAddTaskForDate = { date ->
                                    viewModel.selectCalendarDate(date)
                                    viewModel.openNewReminder(date)
                                }
                            )
                        }
                        BottomTab.SETTINGS -> {
                            SettingsScreen(
                                settings = settings,
                                onThemeChange = { viewModel.updateThemeMode(it) },
                                onAccentChange = { viewModel.updateAccentColor(it) },
                                onSnoozeChange = { viewModel.updateDefaultSnooze(it) },
                                onDailySummaryChange = { viewModel.updateDailySummary(it) },
                                onSoundChange = { viewModel.updateSound(it) },
                                onRingtoneChange = { uri, title -> viewModel.updateRingtone(uri, title) },
                                onRepeatRingtoneChange = { viewModel.updateRepeatRingtone(it) },
                                onVibrateChange = { viewModel.updateVibrate(it) },
                                onVibrationPatternChange = { viewModel.updateVibrationPattern(it) },
                                onSoundEnabledChange = { viewModel.updateSoundEnabled(it) },
                                onWakeScreenOverLockChange = { viewModel.updateWakeScreenOverLock(it) },
                                onLockScreenPrivacyChange = { viewModel.updateLockScreenPrivacy(it) },
                                onQuietHoursChange = { enabled, start, end, highPriority ->
                                    viewModel.updateQuietHours(enabled, start, end, highPriority)
                                },
                                onProfileChange = { name, sub -> viewModel.updateProfile(name, sub) },
                                onResetData = { viewModel.resetData() },
                                onShowPreview = { viewModel.showNotificationPreview() },
                                onSendRealNotification = { viewModel.triggerSystemNotification() }
                            )
                        }
                    }

                    // New/Edit Reminder Modal Screen
                    AnimatedVisibility(
                        visible = isNewReminderOpen,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        NewEditReminderSheet(
                            initialTask = editingTask,
                            availableTags = customTags,
                            onSave = { viewModel.saveTask(it) },
                            onDelete = {
                                viewModel.deleteTask(it)
                                viewModel.closeReminderSheet()
                            },
                            onClose = { viewModel.closeReminderSheet() },
                            onAddTag = { viewModel.addCustomTag(it) }
                        )
                    }
                }
            }
        }
    }

    // Today's Focus Dialog
    if (isEditFocusOpen) {
        EditFocusDialog(
            currentFocus = settings.todaysFocus,
            currentSubtext = settings.focusSubtext,
            onSave = { focus, sub -> viewModel.updateFocus(focus, sub) },
            onDismiss = { viewModel.closeEditFocus() }
        )
    }

    // Notification Preview Dialog
    if (isNotificationPreviewOpen && previewTask != null) {
        NotificationPreviewDialog(
            task = previewTask!!,
            onDone = { task ->
                viewModel.toggleTask(task.id)
            },
            onSnooze = { task ->
                viewModel.snoozeTask(task.id)
            },
            onDismiss = { viewModel.closeNotificationPreview() },
            onPostRealNotification = { viewModel.triggerSystemNotification(previewTask) }
        )
    }
}

@Composable
fun NotifyBottomBar(
    currentTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit
) {
    Surface(
        color = NotifyTheme.colors.cardBg,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.8.dp,
                color = NotifyTheme.colors.border,
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Today Tab
            BottomNavItem(
                label = "Today",
                selected = currentTab == BottomTab.TODAY,
                selectedIcon = Icons.Filled.Notifications,
                unselectedIcon = NotifyIcons.NotificationsNone,
                onClick = { onTabSelected(BottomTab.TODAY) },
                testTag = "nav_today"
            )

            // Calendar Tab
            BottomNavItem(
                label = "Calendar",
                selected = currentTab == BottomTab.CALENDAR,
                selectedIcon = NotifyIcons.CalendarMonth,
                unselectedIcon = NotifyIcons.CalendarMonth,
                onClick = { onTabSelected(BottomTab.CALENDAR) },
                testTag = "nav_calendar"
            )

            // Settings Tab
            BottomNavItem(
                label = "Settings",
                selected = currentTab == BottomTab.SETTINGS,
                selectedIcon = NotifyIcons.Tune,
                unselectedIcon = NotifyIcons.Tune,
                onClick = { onTabSelected(BottomTab.SETTINGS) },
                testTag = "nav_settings"
            )
        }
    }
}

@Composable
fun BottomNavItem(
    label: String,
    selected: Boolean,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    val tint = if (selected) NotifyTheme.colors.accentText else NotifyTheme.colors.mutedText

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tint
        )
    }
}
