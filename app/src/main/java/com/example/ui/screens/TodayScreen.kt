package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val dateString = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEEE, d MMM"))
    }

    var selectedTaskForAction by remember { mutableStateOf<Task?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Date Label
        Text(
            text = dateString,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = NotifyTheme.colors.mutedText
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Greeting
        Text(
            text = "Jay Shree Mataji, ${settings.userName}",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(20.dp))

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
                        imageVector = Icons.Outlined.Edit,
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
                            imageVector = Icons.Outlined.NotificationsNone,
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

                    androidx.compose.material3.Divider(color = NotifyTheme.colors.border, thickness = 0.8.dp)

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
                            imageVector = Icons.Default.CheckCircle,
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
                            imageVector = Icons.Default.Schedule,
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
}
