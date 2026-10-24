package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Task
import com.example.ui.theme.NotifyTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun NotificationPreviewDialog(
    task: Task,
    onDone: (Task) -> Unit,
    onSnooze: (Task) -> Unit,
    onDismiss: () -> Unit,
    onPostRealNotification: () -> Unit
) {
    val now = LocalDateTime.now()
    val timeFormatted = now.format(DateTimeFormatter.ofPattern("h:mm"))
    val dateFormatted = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFDDE3EC)) // Slate lockscreen tint matching reference image
                .padding(20.dp)
        ) {
            // Close button at top-right
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Color.White.copy(alpha = 0.6f), CircleShape)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close preview",
                    tint = Color(0xFF14171F),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp)
            ) {
                // Large Clock Display
                Text(
                    text = timeFormatted,
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF181E2A),
                    letterSpacing = (-2).sp
                )

                Text(
                    text = dateFormatted,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4C5568)
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Primary Active Notification Card
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notification_preview_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        // Header: Bell Icon + "Notify · in 10 min"
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NotifyTheme.colors.accent)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notify",
                                    tint = NotifyTheme.colors.onAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notify · in ${if (task.remindBefore > 0) task.remindBefore else 10} min",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF5B6474)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Title
                        Text(
                            text = task.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF14171F)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Body notes
                        Text(
                            text = if (task.notes.isNotBlank()) task.notes else "Scheduled session. Keep the streak going.",
                            fontSize = 14.sp,
                            color = Color(0xFF5B6474)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons: Done & Snooze 10 min
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    onDone(task)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NotifyTheme.colors.accent,
                                    contentColor = NotifyTheme.colors.onAccent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("preview_done_button")
                            ) {
                                Text(
                                    text = "Done",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    onSnooze(task)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(50),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD6DBE5)),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(48.dp)
                                    .testTag("preview_snooze_button")
                            ) {
                                Text(
                                    text = "Snooze 10 min",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF14171F)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Secondary subtle notification card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "Notify · 8:30 PM",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF5B6474)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "FEWD revision – MDM1ADS201",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF14171F)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Option to trigger system notification
                Button(
                    onClick = onPostRealNotification,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E2430),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Send to Android Status Bar")
                }
            }
        }
    }
}
