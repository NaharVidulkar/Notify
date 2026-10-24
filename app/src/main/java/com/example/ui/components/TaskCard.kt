package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Priority
import com.example.data.model.Task
import com.example.ui.theme.NotifyTheme

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TaskCard(
    task: Task,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOverdue = task.isOverdue()
    val priorityColor = when (task.priority) {
        Priority.HIGH -> NotifyTheme.colors.priorityHigh
        Priority.MEDIUM -> NotifyTheme.colors.priorityMedium
        Priority.LOW -> NotifyTheme.colors.priorityLow
    }

    val checkboxBg by animateColorAsState(
        targetValue = if (task.done) NotifyTheme.colors.accent else Color.Transparent,
        label = "checkboxBg"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = NotifyTheme.colors.cardBg,
        tonalElevation = if (NotifyTheme.colors.isDark) 0.dp else 1.dp,
        shadowElevation = if (NotifyTheme.colors.isDark) 0.dp else 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, NotifyTheme.colors.border, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("task_card_${task.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth()
        ) {
            // Circular Checkbox
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(checkboxBg)
                    .border(
                        width = 1.5.dp,
                        color = if (task.done) NotifyTheme.colors.accent else NotifyTheme.colors.mutedText.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .clickable { onToggle() }
                    .testTag("checkbox_${task.id}")
            ) {
                if (task.done) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = NotifyTheme.colors.onAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Task Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = when {
                        task.done -> NotifyTheme.colors.mutedText
                        isOverdue -> NotifyTheme.colors.priorityHigh
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.formattedTime(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = if (isOverdue) NotifyTheme.colors.priorityHigh else NotifyTheme.colors.mutedText
                    )
                    Text(
                        text = " · ",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = NotifyTheme.colors.mutedText
                    )
                    Text(
                        text = task.tag,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        color = NotifyTheme.colors.mutedText
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Priority Dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(priorityColor)
            )
        }
    }
}
