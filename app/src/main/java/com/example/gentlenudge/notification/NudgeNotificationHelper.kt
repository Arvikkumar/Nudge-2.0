package com.example.gentlenudge.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.gentlenudge.MainActivity
import com.example.gentlenudge.data.model.NudgeTask
import java.util.Calendar

object NudgeNotificationHelper {

    const val GENTLE_CHANNEL_ID = "gentle_nudge_channel"
    const val GENTLE_CHANNEL_NAME = "Nudges"
    const val GENTLE_CHANNEL_DESC = "Quiet, peaceful reminders for the things you want to remember"

    const val FULL_RINGTONE_CHANNEL_ID = "full_ringtone_nudge_channel"
    const val FULL_RINGTONE_CHANNEL_NAME = "Full Ringtone Reminders"
    const val FULL_RINGTONE_CHANNEL_DESC = "Prominent alarm-style reminders with full ringtone"

    const val ACTION_FIRE_NUDGE = "com.example.gentlenudge.ACTION_FIRE_NUDGE"
    const val ACTION_MARK_DONE = "com.example.gentlenudge.ACTION_MARK_DONE"
    const val ACTION_SNOOZE_NUDGE = "com.example.gentlenudge.ACTION_SNOOZE_NUDGE"
    const val ACTION_FIRE_EVENT_NOTIFICATION = "com.example.gentlenudge.ACTION_FIRE_EVENT_NOTIFICATION"

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_TASK_CATEGORY = "extra_task_category"
    const val EXTRA_TASK_SCHEDULED_MILLIS = "extra_task_scheduled_millis"
    const val EXTRA_TASK_IS_REPEATING = "extra_task_is_repeating"
    const val EXTRA_TASK_OCCURRENCE_DATE = "extra_task_occurrence_date"
    const val EXTRA_TASK_OCCURRENCE_TIME = "extra_task_occurrence_time"

    const val EXTRA_EVENT_ID = "extra_event_id"
    const val EXTRA_EVENT_NAME = "extra_event_name"
    const val EXTRA_EVENT_CATEGORY = "extra_event_category"
    const val EXTRA_EVENT_DESCRIPTION = "extra_event_description"
    const val EXTRA_EVENT_DATE_LABEL = "extra_event_date_label"
    const val EXTRA_DAYS_BEFORE = "extra_days_before"
    const val EXTRA_EVENT_YEAR = "extra_event_year"
    const val EXTRA_EVENT_MONTH = "extra_event_month"
    const val EXTRA_EVENT_DAY = "extra_event_day"
    const val EXTRA_REMINDER_KEY = "extra_reminder_key"

    fun createNotificationChannel(context: Context) = createNotificationChannels(context)

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Gentle channel (subtle, standard notification)
            val gentleChannel = NotificationChannel(
                GENTLE_CHANNEL_ID,
                GENTLE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = GENTLE_CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(gentleChannel)

            // Full ringtone channel (alarm / prominent tone)
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val ringtoneChannel = NotificationChannel(
                FULL_RINGTONE_CHANNEL_ID,
                FULL_RINGTONE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = FULL_RINGTONE_CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 700, 300, 700, 300, 700)
                setSound(alarmSound, audioAttributes)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(ringtoneChannel)
        }
    }

    /**
     * Checks if a sound URI can actually be opened and read by the current process/audio system.
     * Returns true if accessible, false if invalid, revoked, deleted, or unreadable.
     */
    private fun isSoundUriAccessible(context: Context, uri: Uri): Boolean {
        return try {
            when (uri.scheme) {
                "content" -> {
                    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
                }
                "android.resource", "file" -> {
                    context.contentResolver.openInputStream(uri)?.use { true } ?: false
                }
                else -> true
            }
        } catch (e: SecurityException) {
            Log.w("NudgeNotification", "SecurityException checking ringtone URI $uri: ${e.message}")
            false
        } catch (e: Exception) {
            Log.w("NudgeNotification", "Cannot access ringtone URI $uri: ${e.message}")
            false
        }
    }

    private fun getRingtoneChannelId(context: Context, soundUri: Uri?, ringtoneTitle: String?): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && soundUri != null) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "nudge_ringtone_${Math.abs(soundUri.toString().hashCode())}"
            val channelName = if (!ringtoneTitle.isNullOrBlank()) "Ringtone: $ringtoneTitle" else FULL_RINGTONE_CHANNEL_NAME

            val existingChannel = notificationManager.getNotificationChannel(channelId)
            if (existingChannel == null) {
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()

                val customChannel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminder sound: $channelName"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 700, 300, 700, 300, 700)
                    setSound(soundUri, audioAttributes)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(customChannel)
            }
            return channelId
        }
        return FULL_RINGTONE_CHANNEL_ID
    }

    fun showNudgeNotification(context: Context, task: NudgeTask): Boolean {
        createNotificationChannels(context)

        val isFullRingtone = task.soundType.equals("Full ringtone", ignoreCase = true)
        val defaultAlarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val customSoundUri: Uri? = if (isFullRingtone) {
            if (!task.ringtoneUri.isNullOrBlank()) {
                val parsedUri = try {
                    Uri.parse(task.ringtoneUri)
                } catch (e: Exception) {
                    Log.w("NudgeNotification", "Failed to parse ringtoneUri '${task.ringtoneUri}': ${e.message}")
                    null
                }

                if (parsedUri != null && isSoundUriAccessible(context, parsedUri)) {
                    parsedUri
                } else {
                    Log.w("NudgeNotification", "Ringtone URI '$parsedUri' inaccessible or invalid. Falling back to default alarm sound.")
                    defaultAlarmSound
                }
            } else {
                defaultAlarmSound
            }
        } else {
            null
        }

        val channelId = if (isFullRingtone) {
            if (customSoundUri != null && customSoundUri != defaultAlarmSound) {
                getRingtoneChannelId(context, customSoundUri, task.ringtoneTitle)
            } else {
                FULL_RINGTONE_CHANNEL_ID
            }
        } else {
            GENTLE_CHANNEL_ID
        }

        // Intent to open app
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Mark as Done
        val isRepeating = NudgeAlarmScheduler.isTaskRepeating(task)
        val doneIntent = Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_TITLE, task.title)
            putExtra(EXTRA_TASK_IS_REPEATING, isRepeating)
            putExtra(EXTRA_TASK_OCCURRENCE_DATE, task.dateLabel)
            putExtra(EXTRA_TASK_OCCURRENCE_TIME, task.timeLabel)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (task.id * 10 + 1).toInt(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze
        val snoozeIntent = Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = ACTION_SNOOZE_NUDGE
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_TITLE, task.title)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (task.id * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bigTextMessage = if (task.timeLabel.isNotBlank()) {
            "“${task.title}”\nScheduled for ${task.timeLabel}"
        } else {
            "“${task.title}”"
        }

        val title = if (isFullRingtone) "Reminder: ${task.title}" else "Nudge"
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(task.title)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(bigTextMessage)
            )
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Done", donePendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Snooze", snoozePendingIntent)

        if (isFullRingtone && customSoundUri != null) {
            builder.setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(customSoundUri)
                .setVibrate(longArrayOf(0, 700, 300, 700, 300, 700))
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
        }

        return try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) {
                Log.e("NudgeNotification", "Notifications blocked at system/app level for task ${task.id}")
                return false
            }
            notificationManager.notify(task.id.toInt(), builder.build())
            Log.d("NudgeNotification", "Notification successfully posted for task ${task.id} ('${task.title}')")
            true
        } catch (e: SecurityException) {
            Log.e("NudgeNotification", "SecurityException posting notification: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e("NudgeNotification", "Failed to post notification: ${e.message}", e)
            false
        }
    }

    fun dismissNotification(context: Context, taskId: Long) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(taskId.toInt())
    }

    fun showEventNotification(
        context: Context,
        eventId: String,
        eventName: String,
        category: String,
        description: String,
        dateLabel: String,
        daysBefore: Int,
        eventYear: Int = 0,
        eventMonth: Int = 0,
        eventDay: Int = 0
    ): Boolean {
        createNotificationChannels(context)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val notifId = (Math.abs(eventId.hashCode()) % 400000) + 500000
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate dynamic days remaining if exact event date is provided
        val daysRemaining: Int = if (eventYear > 0 && eventMonth > 0 && eventDay > 0) {
            val nowCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val evCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, eventYear)
                set(Calendar.MONTH, eventMonth - 1)
                set(Calendar.DAY_OF_MONTH, eventDay)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val diffMillis = evCal.timeInMillis - nowCal.timeInMillis
            (diffMillis / (1000L * 60 * 60 * 24)).toInt()
        } else {
            daysBefore
        }

        val title = when (daysRemaining) {
            0 -> "Today: $eventName"
            1 -> "Upcoming Tomorrow: $eventName"
            2 -> "Upcoming in 2 Days: $eventName"
            else -> if (daysRemaining > 0) "Upcoming in $daysRemaining Days: $eventName" else "Event Reminder: $eventName"
        }

        val subtitle = if (category.isNotBlank()) "$category • $dateLabel" else dateLabel
        val bigText = if (description.isNotBlank()) {
            "“$eventName”\n$subtitle\n$description"
        } else {
            "“$eventName”\n$subtitle"
        }

        val builder = NotificationCompat.Builder(context, GENTLE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(if (description.isNotBlank()) description else subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w("NudgeNotification", "POST_NOTIFICATIONS permission not granted; cannot display event notification for $eventName")
                    return false
                }
            }
            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) {
                Log.w("NudgeNotification", "Notifications disabled in system settings; cannot display event notification for $eventName")
                return false
            }
            notificationManager.notify(notifId, builder.build())
            return true
        } catch (e: SecurityException) {
            Log.w("NudgeNotification", "SecurityException posting event notification: ${e.message}")
            return false
        } catch (e: Exception) {
            Log.e("NudgeNotification", "Error posting event notification: ${e.message}", e)
            return false
        }
    }
}
