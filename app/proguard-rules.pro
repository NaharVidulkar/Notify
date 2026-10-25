# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Notify keep rules
# Preserve all alarm, boot, and notification-action broadcast receivers
-keep class com.example.notifications.ReminderAlarmReceiver { *; }
-keep class com.example.notifications.BootReceiver { *; }
-keep class com.example.notifications.NotificationActionReceiver { *; }
-keep class com.example.notifications.ReminderAlarmScheduler { *; }
-keep class com.example.notifications.NotificationHelper { *; }

# Preserve Activities
-keep class com.example.MainActivity { *; }
-keep class com.example.ui.ReminderAlertActivity { *; }

# Preserve data models, settings classes and enums
-keep class com.example.data.model.** { *; }
-keep class com.example.data.local.TaskRepository { *; }
-keep class com.example.viewmodel.TaskViewModel { *; }
-keepclassmembers enum * { *; }
