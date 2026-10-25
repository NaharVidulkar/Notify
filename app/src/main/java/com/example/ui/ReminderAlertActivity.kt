package com.example.ui

import android.app.KeyguardManager
import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import com.example.ui.components.NotifyIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.data.local.TaskRepository
import com.example.data.model.Priority
import com.example.data.model.ThemeMode
import com.example.notifications.NotificationHelper
import com.example.notifications.ReminderAlarmScheduler
import com.example.ui.components.NotifyLogoBadge
import com.example.ui.theme.NotifyAppTheme
import com.example.ui.theme.NotifyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReminderAlertActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ReminderAlertActivity"
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Wake screen and show on top of lock screen
        setupLockscreenFlags()

        // 2. Take a short wake lock while the alert shows (max 60 seconds)
        acquireWakeLock()

        val repo = TaskRepository.getInstance(this)
        val settings = repo.settings.value

        val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID) ?: ""
        val taskTitle = intent.getStringExtra("task_title") ?: "Reminder Alert"
        val taskDue = intent.getStringExtra("task_due") ?: ""
        val taskTag = intent.getStringExtra("task_tag") ?: "DSA"
        val priorityName = intent.getStringExtra("task_priority") ?: Priority.HIGH.name
        val priority = try { Priority.valueOf(priorityName) } catch (_: Exception) { Priority.HIGH }

        val canRingAndAlert = NotificationHelper.shouldRingAndAlert(settings, priority)

        // 3. Play ringtone and start vibration pattern according to user settings
        startAlertEffects(settings, canRingAndAlert)

        // Auto-silence after 60 seconds
        lifecycleScope.launch {
            delay(60_000)
            stopAlertEffects()
        }

        setContent {
            val liveSettings by repo.settings.collectAsState()
            val isDark = when (liveSettings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            NotifyAppTheme(
                darkTheme = isDark,
                accentColor = Color(liveSettings.accentColor.colorHex),
                onAccentColor = Color(liveSettings.accentColor.onAccentHex)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ReminderAlertContent(
                        title = taskTitle,
                        dueAt = taskDue,
                        tag = taskTag,
                        priority = priority,
                        snoozeMinutes = liveSettings.defaultSnoozeMinutes,
                        onDone = {
                            if (taskId.isNotBlank()) {
                                val task = repo.getTaskById(taskId)
                                if (task != null && task.hasRepeat()) {
                                    val nextTask = task.advanceToNextOccurrence()
                                    repo.updateTaskDirectly(nextTask)
                                    ReminderAlarmScheduler.scheduleReminder(this@ReminderAlertActivity, nextTask)
                                } else {
                                    if (task != null) {
                                        repo.updateTaskDirectly(task.copy(done = true))
                                    } else {
                                        repo.toggleTask(taskId)
                                    }
                                    ReminderAlarmScheduler.cancelReminder(this@ReminderAlertActivity, taskId)
                                }
                                NotificationHelper.cancelNotification(this@ReminderAlertActivity, taskId)
                            }
                            stopAlertEffects()
                            finishAndRemoveTask()
                        },
                        onSnooze = {
                            if (taskId.isNotBlank()) {
                                repo.snoozeTask(taskId, liveSettings.defaultSnoozeMinutes)
                                val snoozed = repo.getTaskById(taskId)
                                if (snoozed != null) {
                                    ReminderAlarmScheduler.scheduleReminder(this@ReminderAlertActivity, snoozed)
                                }
                                NotificationHelper.cancelNotification(this@ReminderAlertActivity, taskId)
                            }
                            stopAlertEffects()
                            finishAndRemoveTask()
                        },
                        onDismiss = {
                            if (taskId.isNotBlank()) {
                                NotificationHelper.cancelNotification(this@ReminderAlertActivity, taskId)
                            }
                            stopAlertEffects()
                            finishAndRemoveTask()
                        }
                    )
                }
            }
        }
    }

    private fun setupLockscreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            @Suppress("DEPRECATION")
            wakeLock = powerManager?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "Notify:ReminderAlertWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(60_000)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire wake lock: ${e.message}")
        }
    }

    private fun startAlertEffects(settings: com.example.data.model.UserSettings, canRingAndAlert: Boolean) {
        if (!canRingAndAlert) return

        if (settings.soundEnabled) {
            try {
                val soundUri: Uri = settings.ringtoneUri?.let { Uri.parse(it) }
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                ringtone = RingtoneManager.getRingtone(applicationContext, soundUri)?.apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        isLooping = settings.repeatRingtone
                    }
                    play()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error playing ringtone: ${e.message}")
            }
        }

        if (settings.vibrate) {
            try {
                vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

                val pattern = settings.vibrationPattern.pattern
                val repeatIndex = if (settings.repeatRingtone) 0 else -1

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, repeatIndex))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, repeatIndex)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error initiating vibration: ${e.message}")
            }
        }
    }

    private fun stopAlertEffects() {
        try {
            if (ringtone?.isPlaying == true) {
                ringtone?.stop()
            }
            vibrator?.cancel()
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping alert effects", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlertEffects()
    }
}

@Composable
fun ReminderAlertContent(
    title: String,
    dueAt: String,
    tag: String,
    priority: Priority,
    snoozeMinutes: Int,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Brand Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            NotifyLogoBadge(size = 32.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
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

        // Center Alert Card
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Pulsing Alarm Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(90.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(NotifyTheme.colors.accent.copy(alpha = 0.18f))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(NotifyTheme.colors.accent)
                ) {
                    Icon(
                        imageVector = NotifyIcons.Alarm,
                        contentDescription = "Alert",
                        tint = NotifyTheme.colors.onAccent,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tag & Priority Pills
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = NotifyTheme.colors.cardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NotifyTheme.colors.border)
                ) {
                    Text(
                        text = "#$tag",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NotifyTheme.colors.accentText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                val priorityColor = when (priority) {
                    Priority.HIGH -> NotifyTheme.colors.priorityHigh
                    Priority.MEDIUM -> NotifyTheme.colors.priorityMedium
                    Priority.LOW -> NotifyTheme.colors.priorityLow
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = priorityColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, priorityColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(priorityColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${priority.label} Priority",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = priorityColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Task Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    lineHeight = 32.sp
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Time / Due label
            Text(
                text = "Reminder triggered now",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = NotifyTheme.colors.mutedText
            )
        }

        // Bottom Action Buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Big "Done" Button
            Button(
                onClick = onDone,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NotifyTheme.colors.accent,
                    contentColor = NotifyTheme.colors.onAccent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("alert_done_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Done",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Big "Snooze 10 min" Button
            OutlinedButton(
                onClick = onSnooze,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, NotifyTheme.colors.border),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = NotifyTheme.colors.cardBg,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("alert_snooze_button")
            ) {
                Icon(
                    imageVector = NotifyIcons.Schedule,
                    contentDescription = null,
                    tint = NotifyTheme.colors.accentText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Snooze $snoozeMinutes min",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dismiss Button
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("alert_dismiss_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = NotifyTheme.colors.mutedText,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Dismiss",
                    color = NotifyTheme.colors.mutedText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
