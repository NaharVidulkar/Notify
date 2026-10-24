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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Priority
import com.example.data.model.Task
import com.example.ui.components.TaskCard
import com.example.ui.theme.NotifyTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    tasks: List<Task>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    onToggleTask: (String) -> Unit,
    onEditTask: (Task) -> Unit,
    onAddTaskForDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val today = remember { LocalDate.now() }

    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value % 7 // 0 for Sunday
    val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7

    val tasksForSelectedDate = remember(tasks, selectedDate) {
        tasks.filter { task ->
            val due = task.parseDueDateTime()
            due?.toLocalDate() == selectedDate
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Month Navigation Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Row {
                IconButton(
                    onClick = { currentMonth = currentMonth.minusMonths(1) },
                    modifier = Modifier
                        .size(38.dp)
                        .background(NotifyTheme.colors.cardBg, CircleShape)
                        .border(1.dp, NotifyTheme.colors.border, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Month",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { currentMonth = currentMonth.plusMonths(1) },
                    modifier = Modifier
                        .size(38.dp)
                        .background(NotifyTheme.colors.cardBg, CircleShape)
                        .border(1.dp, NotifyTheme.colors.border, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Month",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Calendar Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NotifyTheme.colors.cardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, NotifyTheme.colors.border),
            shadowElevation = if (NotifyTheme.colors.isDark) 0.dp else 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Day of Week Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { d ->
                        Text(
                            text = d,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NotifyTheme.colors.mutedText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Days Grid
                val rows = totalCells / 7
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val dayNum = cellIndex - firstDayOfWeek + 1

                            if (dayNum in 1..daysInMonth) {
                                val cellDate = currentMonth.atDay(dayNum)
                                val isSelected = cellDate == selectedDate
                                val isCurrentDay = cellDate == today

                                // Find tasks for this day
                                val dayTasks = tasks.filter {
                                    it.parseDueDateTime()?.toLocalDate() == cellDate
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> NotifyTheme.colors.accent
                                                isCurrentDay -> NotifyTheme.colors.accent.copy(alpha = 0.2f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = if (isCurrentDay && !isSelected) 1.5.dp else 0.dp,
                                            color = if (isCurrentDay && !isSelected) NotifyTheme.colors.accent else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { onSelectDate(cellDate) }
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected || isCurrentDay) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> NotifyTheme.colors.onAccent
                                                isCurrentDay -> NotifyTheme.colors.accentText
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )

                                        if (dayTasks.isNotEmpty()) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                dayTasks.take(3).forEach { t ->
                                                    val dotColor = when (t.priority) {
                                                        Priority.HIGH -> NotifyTheme.colors.priorityHigh
                                                        Priority.MEDIUM -> NotifyTheme.colors.priorityMedium
                                                        Priority.LOW -> NotifyTheme.colors.priorityLow
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) NotifyTheme.colors.onAccent else dotColor)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Selected Date Header and List
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${tasksForSelectedDate.size} reminders scheduled",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = NotifyTheme.colors.mutedText
                )
            }

            Button(
                onClick = { onAddTaskForDate(selectedDate) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NotifyTheme.colors.accent,
                    contentColor = NotifyTheme.colors.onAccent
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Add", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (tasksForSelectedDate.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(20.dp)
            ) {
                Text(
                    text = "No reminders on this date.\nTap 'Add' to schedule one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NotifyTheme.colors.mutedText,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(tasksForSelectedDate, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onToggle = { onToggleTask(task.id) },
                        onClick = { onEditTask(task) },
                        onLongClick = { onEditTask(task) }
                    )
                }
            }
        }
    }
}
