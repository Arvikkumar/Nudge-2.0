package com.example.gentlenudge.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.gentlenudge.GentleNudgeApp
import com.example.gentlenudge.backup.NudgeBackupScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED ||
            action == "android.intent.action.TIME_SET"
        ) {
            Log.d("BootReceiver", "Received action $action. Rescheduling active alarms and backups...")
            val app = context.applicationContext as? GentleNudgeApp ?: return

            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "GentleNudge:BootReceiverWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(60_000L)
            }

            val pendingResult = goAsync()

            // Reschedule backup reminders & automatic backups
            try {
                NudgeBackupScheduler.rescheduleAll(context)
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to reschedule backups on boot: ${e.message}", e)
            }

            // Reschedule global event notifications
            try {
                NudgeEventNotificationScheduler.rescheduleIfEnabled(context)
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to reschedule event notifications on boot: ${e.message}", e)
            }

            // Reschedule active Deep Dive session if any
            try {
                com.example.gentlenudge.deepdive.DeepDiveManager.rescheduleOnBoot(context)
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to reschedule Deep Dive on boot: ${e.message}", e)
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val pendingTasks = app.database.nudgeTaskDao().getAllPendingTasksSync()
                    val prefs = context.getSharedPreferences("gentle_nudge_prefs", Context.MODE_PRIVATE)
                    val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
                    val now = System.currentTimeMillis()

                    for (task in pendingTasks) {
                        val isRepeating = NudgeAlarmScheduler.isTaskRepeating(task)
                        val scheduledMillis = NudgeAlarmScheduler.getScheduledTriggerMillis(context, task, now)
                        val occurrenceKey = NudgeAlarmScheduler.getTaskOccurrenceKey(task.id, scheduledMillis)

                        if (scheduledMillis <= now) {
                            // Scheduled time passed while device was off or unhandled
                            if (!NudgeAlarmScheduler.isTaskReminderDelivered(context, occurrenceKey)) {
                                if (notificationsEnabled) {
                                    if (NudgeAlarmScheduler.tryClaimTaskReminderDelivery(context, occurrenceKey)) {
                                        Log.d("BootReceiver", "Delivering missed reminder for task ${task.id} ('${task.title}') scheduled at $scheduledMillis")
                                        val posted = NudgeNotificationHelper.showNudgeNotification(context, task)
                                        if (posted) {
                                            NudgeAlarmScheduler.markTaskReminderDelivered(context, occurrenceKey)
                                            if (isRepeating) {
                                                val nextTask = NudgeNotificationReceiver.calculateNextOccurrenceTask(task, scheduledMillis)
                                                if (nextTask != null) {
                                                    app.database.nudgeTaskDao().updateTask(nextTask)
                                                    val baseCal = Calendar.getInstance().apply { timeInMillis = scheduledMillis }
                                                    val advancedCal = NudgeNotificationReceiver.calculateNextCalendar(baseCal, task.repeat.trim())
                                                    if (advancedCal != null) {
                                                        NudgeAlarmScheduler.saveScheduledTriggerMillis(context, nextTask.id, advancedCal.timeInMillis)
                                                    }
                                                    NudgeAlarmScheduler.scheduleTask(context, nextTask, forceRecalculate = false)
                                                }
                                            }
                                        } else {
                                            NudgeAlarmScheduler.unclaimTaskReminderDelivery(context, occurrenceKey)
                                        }
                                    }
                                } else {
                                    Log.d("BootReceiver", "Missed reminder for task ${task.id} not shown as notifications are disabled.")
                                }
                            } else {
                                Log.d("BootReceiver", "Task ${task.id} occurrence $occurrenceKey already delivered. Skipping delivery.")
                                if (isRepeating && notificationsEnabled) {
                                    NudgeAlarmScheduler.scheduleTask(context, task, forceRecalculate = false)
                                }
                            }
                        } else {
                            // Scheduled time is in the future: restore the exact same persisted trigger
                            if (notificationsEnabled) {
                                NudgeAlarmScheduler.scheduleTask(context, task, forceRecalculate = false)
                            }
                        }
                    }
                    Log.d("BootReceiver", "Successfully processed ${pendingTasks.size} tasks on boot/time change.")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to reschedule tasks: ${e.message}", e)
                } finally {
                    try {
                        if (wakeLock?.isHeld == true) {
                            wakeLock.release()
                        }
                    } catch (e: Exception) {
                        Log.e("BootReceiver", "Error releasing wake lock: ${e.message}", e)
                    }
                    pendingResult.finish()
                }
            }
        }
    }
}
