package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Priority
import com.example.data.model.Task
import com.example.ui.theme.NotifyTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewEditReminderSheet(
    initialTask: Task?,
    availableTags: List<String>,
    onSave: (Task) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit,
    onAddTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isEdit = initialTask != null

    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var notes by remember { mutableStateOf(initialTask?.notes ?: "") }

    val initialDue = remember {
        initialTask?.parseDueDateTime() ?: LocalDateTime.now().plusHours(1).withMinute(0)
    }

    var selectedDate by remember { mutableStateOf(initialDue.toLocalDate()) }
    var selectedTime by remember { mutableStateOf(initialDue.toLocalTime()) }
    var repeatOption by remember { mutableStateOf(initialTask?.repeat ?: "None") }
    var remindBeforeMinutes by remember { mutableStateOf(initialTask?.remindBefore ?: 10) }
    var selectedPriority by remember { mutableStateOf(initialTask?.priority ?: Priority.HIGH) }
    var selectedTag by remember { mutableStateOf(initialTask?.tag ?: "DSA") }

    var showRepeatDialog by remember { mutableStateOf(false) }
    var showRemindBeforeDialog by remember { mutableStateOf(false) }
    var showNewTagDialog by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }

    val formattedDate = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
    }
    val formattedTime = remember(selectedTime) {
        selectedTime.format(DateTimeFormatter.ofPattern("h:mm a"))
    }

    // DatePicker
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        )
    }

    // TimePicker
    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                selectedTime = LocalTime.of(hourOfDay, minute)
            },
            selectedTime.hour,
            selectedTime.minute,
            false
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 40.dp)
        ) {
            // Header: Close pill and Screen title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onClose,
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NotifyTheme.colors.border),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = NotifyTheme.colors.cardBg,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.testTag("close_reminder_button")
                ) {
                    Text(
                        text = "Close",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = if (isEdit) "Edit reminder" else "New reminder",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.weight(1.3f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Task Name Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = NotifyTheme.colors.cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NotifyTheme.colors.border, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "Task",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = NotifyTheme.colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("e.g. DSA: Graphs – BFS & DFS practice", color = NotifyTheme.colors.mutedText) },
                        textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_title_input")
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("Notes or session description (optional)", color = NotifyTheme.colors.mutedText.copy(alpha = 0.7f)) },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Details Card (Date, Time, Repeat, Remind me before)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = NotifyTheme.colors.cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NotifyTheme.colors.border, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                    // Date Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerDialog.show() }
                            .padding(vertical = 14.dp)
                    ) {
                        Text(
                            text = "Date",
                            style = MaterialTheme.typography.bodyLarge,
                            color = NotifyTheme.colors.mutedText
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Divider(color = NotifyTheme.colors.border, thickness = 0.8.dp)

                    // Time Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { timePickerDialog.show() }
                            .padding(vertical = 14.dp)
                    ) {
                        Text(
                            text = "Time",
                            style = MaterialTheme.typography.bodyLarge,
                            color = NotifyTheme.colors.mutedText
                        )
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = NotifyTheme.colors.accentText
                        )
                    }

                    Divider(color = NotifyTheme.colors.border, thickness = 0.8.dp)

                    // Repeat Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRepeatDialog = true }
                            .padding(vertical = 14.dp)
                    ) {
                        Text(
                            text = "Repeat",
                            style = MaterialTheme.typography.bodyLarge,
                            color = NotifyTheme.colors.mutedText
                        )
                        Text(
                            text = repeatOption,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Divider(color = NotifyTheme.colors.border, thickness = 0.8.dp)

                    // Remind me before Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRemindBeforeDialog = true }
                            .padding(vertical = 14.dp)
                    ) {
                        Text(
                            text = "Remind me before",
                            style = MaterialTheme.typography.bodyLarge,
                            color = NotifyTheme.colors.mutedText
                        )
                        Text(
                            text = if (remindBeforeMinutes > 0) "$remindBeforeMinutes min" else "None",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Priority Selector
            Text(
                text = "Priority",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Priority.entries.forEach { priority ->
                    val isSelected = selectedPriority == priority
                    val priorityColor = when (priority) {
                        Priority.HIGH -> NotifyTheme.colors.priorityHigh
                        Priority.MEDIUM -> NotifyTheme.colors.priorityMedium
                        Priority.LOW -> NotifyTheme.colors.priorityLow
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) NotifyTheme.colors.accent else NotifyTheme.colors.cardBg,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) NotifyTheme.colors.accent else NotifyTheme.colors.border
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { selectedPriority = priority }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(priorityColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = priority.label,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) NotifyTheme.colors.onAccent else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Tag Selector
            Text(
                text = "Tag",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                availableTags.forEach { tag ->
                    val isSelected = selectedTag == tag
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) NotifyTheme.colors.accent else NotifyTheme.colors.cardBg,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) NotifyTheme.colors.accent else NotifyTheme.colors.border
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { selectedTag = tag }
                    ) {
                        Text(
                            text = tag,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) NotifyTheme.colors.onAccent else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }

                // Add Custom Tag Chip
                Surface(
                    shape = RoundedCornerShape(50),
                    color = NotifyTheme.colors.cardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NotifyTheme.colors.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { showNewTagDialog = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add tag",
                            tint = NotifyTheme.colors.mutedText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add",
                            fontSize = 13.sp,
                            color = NotifyTheme.colors.mutedText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Save reminder Button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val dueDateTime = LocalDateTime.of(selectedDate, selectedTime)
                        val task = Task(
                            id = initialTask?.id ?: UUID.randomUUID().toString(),
                            title = title.trim(),
                            dueAt = dueDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            repeat = repeatOption,
                            remindBefore = remindBeforeMinutes,
                            priority = selectedPriority,
                            tag = selectedTag,
                            done = initialTask?.done ?: false,
                            notes = notes.trim()
                        )
                        onSave(task)
                    }
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NotifyTheme.colors.accent,
                    contentColor = NotifyTheme.colors.onAccent,
                    disabledContainerColor = NotifyTheme.colors.accent.copy(alpha = 0.4f),
                    disabledContentColor = NotifyTheme.colors.onAccent.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_reminder_button")
            ) {
                Text(
                    text = if (isEdit) "Update reminder" else "Save reminder",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isEdit && initialTask != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onDelete(initialTask.id) },
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NotifyTheme.colors.priorityHigh.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NotifyTheme.colors.priorityHigh
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("delete_reminder_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Delete reminder", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Repeat Dialog
    if (showRepeatDialog) {
        AlertDialog(
            onDismissRequest = { showRepeatDialog = false },
            title = { Text("Repeat") },
            text = {
                Column {
                    listOf("None", "Every day", "Weekly").forEach { opt ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repeatOption = opt
                                    showRepeatDialog = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = opt,
                                fontSize = 16.sp,
                                fontWeight = if (repeatOption == opt) FontWeight.Bold else FontWeight.Normal,
                                color = if (repeatOption == opt) NotifyTheme.colors.accentText else MaterialTheme.colorScheme.onSurface
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

    // Remind me before Dialog
    if (showRemindBeforeDialog) {
        AlertDialog(
            onDismissRequest = { showRemindBeforeDialog = false },
            title = { Text("Remind me before") },
            text = {
                Column {
                    listOf(0 to "None", 5 to "5 min", 10 to "10 min", 30 to "30 min").forEach { (min, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    remindBeforeMinutes = min
                                    showRemindBeforeDialog = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 16.sp,
                                fontWeight = if (remindBeforeMinutes == min) FontWeight.Bold else FontWeight.Normal,
                                color = if (remindBeforeMinutes == min) NotifyTheme.colors.accentText else MaterialTheme.colorScheme.onSurface
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

    // Add Tag Dialog
    if (showNewTagDialog) {
        AlertDialog(
            onDismissRequest = { showNewTagDialog = false },
            title = { Text("Add Custom Tag") },
            text = {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    placeholder = { Text("e.g. Study, Gym, Work") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTagInput.isNotBlank()) {
                            onAddTag(newTagInput.trim())
                            selectedTag = newTagInput.trim()
                            newTagInput = ""
                            showNewTagDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NotifyTheme.colors.accent,
                        contentColor = NotifyTheme.colors.onAccent
                    )
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewTagDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = NotifyTheme.colors.cardBg,
            shape = RoundedCornerShape(18.dp)
        )
    }
}
