package com.example.gentlenudge

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.gentlenudge.data.model.NudgeTask
import com.example.gentlenudge.notification.NudgeAlarmScheduler
import com.example.gentlenudge.notification.NudgeNotificationHelper
import com.example.gentlenudge.notification.NudgeNotificationReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowNotificationManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = GentleNudgeApp::class)
class NudgeAlarmDeliveryTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var shadowNotificationManager: ShadowNotificationManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = shadowOf(alarmManager)
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = shadowOf(notificationManager)
        NudgeNotificationHelper.createNotificationChannels(context)
    }

    @Test
    fun testOneMinuteLaterRelativeTimeCalculation() {
        val now = System.currentTimeMillis()
        val trigger = NudgeAlarmScheduler.calculateTriggerMillis("Today", "1 minute later")
        val diff = trigger - now
        // Should be approximately 60,000 ms (allowing 5000 ms delta)
        assertTrue("Expected ~60s in future, but got $diff ms", diff in 55000..65000)
    }

    @Test
    fun testTwoMinutesLaterRelativeTimeCalculation() {
        val now = System.currentTimeMillis()
        val trigger = NudgeAlarmScheduler.calculateTriggerMillis("Today", "2 minutes later")
        val diff = trigger - now
        assertTrue("Expected ~120s in future, but got $diff ms", diff in 115000..125000)
    }

    @Test
    fun testInXMinutesPhraseCalculation() {
        val now = System.currentTimeMillis()
        val trigger1 = NudgeAlarmScheduler.calculateTriggerMillis("Today", "in 1 minute")
        val diff1 = trigger1 - now
        assertTrue("Expected in 1 minute to be ~60s, got $diff1", diff1 in 55000..65000)

        val trigger2 = NudgeAlarmScheduler.calculateTriggerMillis("Today", "in 5 mins")
        val diff2 = trigger2 - now
        assertTrue("Expected in 5 mins to be ~300s, got $diff2", diff2 in 295000..305000)
    }

    @Test
    fun testScheduleTaskSetsExactAlarmForOneMinuteDelay() {
        val task = NudgeTask(
            id = 42L,
            title = "loca",
            dateLabel = "Today",
            timeLabel = "1 minute later",
            category = "Personal"
        )

        NudgeAlarmScheduler.scheduleTask(context, task)

        val nextAlarm = shadowAlarmManager.nextScheduledAlarm
        assertNotNull("An alarm should be scheduled in AlarmManager", nextAlarm)
        val alarm = checkNotNull(nextAlarm)
        assertEquals(AlarmManager.RTC_WAKEUP, alarm.type)
        val now = System.currentTimeMillis()
        assertTrue("Alarm trigger time should be in the future", alarm.triggerAtTime > now)
        assertTrue("Alarm trigger time should be ~60s in the future", (alarm.triggerAtTime - now) in 55000..65000)
    }

    @Test
    fun testScheduleTaskSetsExactAlarmForTwoMinuteDelay() {
        val task = NudgeTask(
            id = 43L,
            title = "Quick reminder",
            dateLabel = "Today",
            timeLabel = "2 minutes later",
            category = "Work"
        )

        NudgeAlarmScheduler.scheduleTask(context, task)

        val nextAlarm = shadowAlarmManager.nextScheduledAlarm
        assertNotNull("An alarm should be scheduled in AlarmManager", nextAlarm)
        val alarm = checkNotNull(nextAlarm)
        assertEquals(AlarmManager.RTC_WAKEUP, alarm.type)
        val now = System.currentTimeMillis()
        assertTrue("Alarm trigger time should be ~120s in the future", (alarm.triggerAtTime - now) in 115000..125000)
    }

    @Test
    fun testScheduleTaskSpecificTimeFormat() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, 5)
        val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
        val formattedTime = timeFormat.format(cal.time)

        val task = NudgeTask(
            id = 44L,
            title = "Specific time task",
            dateLabel = "Today",
            timeLabel = formattedTime,
            category = "Personal"
        )

        NudgeAlarmScheduler.scheduleTask(context, task)

        val nextAlarm = shadowAlarmManager.nextScheduledAlarm
        assertNotNull("An alarm should be scheduled for specific time", nextAlarm)
        val alarm = checkNotNull(nextAlarm)
        assertEquals(AlarmManager.RTC_WAKEUP, alarm.type)
    }

    @Test
    fun testCancelTaskRemovesAlarmAndNotification() {
        val task = NudgeTask(
            id = 45L,
            title = "Cancel me",
            dateLabel = "Today",
            timeLabel = "1 minute later"
        )

        NudgeAlarmScheduler.scheduleTask(context, task)
        assertNotNull("Alarm scheduled", shadowAlarmManager.nextScheduledAlarm)

        NudgeAlarmScheduler.cancelTask(context, task.id)
        val remainingAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue("No alarm should remain for task 45", remainingAlarms.none { it.operation.toString().contains("45") })
    }

    @Test
    fun testNotificationDeliveryViaReceiver() {
        val task = NudgeTask(
            id = 99L,
            title = "loca",
            dateLabel = "Today",
            timeLabel = "8:24 PM",
            category = "Personal"
        )

        // Directly verify notification helper builds and posts notification
        NudgeNotificationHelper.showNudgeNotification(context, task)

        val notifications = shadowNotificationManager.allNotifications
        assertTrue("Notification should be posted", notifications.isNotEmpty())
        val posted = notifications.find { it.extras.getString(android.app.Notification.EXTRA_TEXT) == "loca" }
        assertNotNull("Notification with title 'loca' should be posted", posted)
    }

    @Test
    fun testRecurringDailyAlarmPassedTodaySchedulesTomorrow() {
        // Suppose current time is 7:07 AM today
        val calNow = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 7)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMillis = calNow.timeInMillis

        // Recurring 4:00 AM daily created when time is 7:07 AM
        val targetMillis = NudgeAlarmScheduler.calculateNextOccurrenceMillis(
            dateLabel = "Today",
            timeLabel = "4:00 AM",
            repeatRule = "Every day",
            now = nowMillis
        )

        // Expected next occurrence: 4:00 AM tomorrow
        val expectedCal = (calNow.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 0)
        }

        assertEquals(expectedCal.timeInMillis, targetMillis)
        assertTrue("Target millis must be strictly in future", targetMillis > nowMillis)
        val diffHours = (targetMillis - nowMillis) / (1000 * 60 * 60)
        assertEquals(20L, diffHours) // 7:07 AM today to 4:00 AM tomorrow is ~20 hours and 53 mins
    }

    @Test
    fun testNonRepeatingTodayPassedTimeDoesNotRollOverToTomorrow() {
        // Today at 4:00 AM, but current time is 7:07 AM
        val calNow = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 7)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMillis = calNow.timeInMillis

        val targetMillis = NudgeAlarmScheduler.calculateNextOccurrenceMillis(
            dateLabel = "Today",
            timeLabel = "4:00 AM",
            repeatRule = "Does not repeat",
            now = nowMillis
        )

        val expectedCal = (calNow.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 0)
        }

        assertEquals(expectedCal.timeInMillis, targetMillis)
        assertTrue("Target millis must remain today at 4:00 AM and not roll over", targetMillis <= nowMillis)
    }

    @Test
    fun testFutureTimeOnSameDayRemainsToday() {
        // Suppose current time is 3:00 AM today
        val calNow = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 3)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMillis = calNow.timeInMillis

        val targetMillis = NudgeAlarmScheduler.calculateNextOccurrenceMillis(
            dateLabel = "Today",
            timeLabel = "4:00 AM",
            repeatRule = "Every day",
            now = nowMillis
        )

        val expectedCal = (calNow.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 0)
        }

        assertEquals(expectedCal.timeInMillis, targetMillis)
        val diffHours = (targetMillis - nowMillis) / (1000 * 60 * 60)
        assertEquals(1L, diffHours) // 1 hour away
    }

    @Test
    fun testIsTaskRepeatingDetection() {
        val taskOnce = NudgeTask(id = 1L, title = "Take screenshot", repeat = "Once")
        val taskDoesNotRepeat = NudgeTask(id = 2L, title = "Dentist", repeat = "Does not repeat")
        val taskBlank = NudgeTask(id = 3L, title = "Walk", repeat = "")
        val taskEveryDay = NudgeTask(id = 4L, title = "Drink water", repeat = "Every day")
        val taskCustom = NudgeTask(id = 5L, title = "Yoga", repeat = "Custom (Mon, Wed)")

        assertFalse("Once should not be repeating", NudgeAlarmScheduler.isTaskRepeating(taskOnce))
        assertFalse("Does not repeat should not be repeating", NudgeAlarmScheduler.isTaskRepeating(taskDoesNotRepeat))
        assertFalse("Blank should not be repeating", NudgeAlarmScheduler.isTaskRepeating(taskBlank))
        assertTrue("Every day should be repeating", NudgeAlarmScheduler.isTaskRepeating(taskEveryDay))
        assertTrue("Custom should be repeating", NudgeAlarmScheduler.isTaskRepeating(taskCustom))
    }

    @Test
    fun testOneTimeReminderDeliveryClaimAndDeduplication() {
        val taskId = 101L
        val scheduledMillis = 1780000000000L
        val key = NudgeAlarmScheduler.getTaskOccurrenceKey(taskId, scheduledMillis)

        // Clear any previous state
        val prefs = context.getSharedPreferences("gentle_nudge_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove(NudgeAlarmScheduler.KEY_DELIVERED_TASK_REMINDERS).commit()

        assertFalse("Initially should not be marked delivered", NudgeAlarmScheduler.isTaskReminderDelivered(context, key))

        // First attempt to claim should succeed
        val firstClaim = NudgeAlarmScheduler.tryClaimTaskReminderDelivery(context, key)
        assertTrue("First claim should return true", firstClaim)
        assertTrue("Should now be marked delivered", NudgeAlarmScheduler.isTaskReminderDelivered(context, key))

        // Second attempt to claim should fail (race condition protection)
        val secondClaim = NudgeAlarmScheduler.tryClaimTaskReminderDelivery(context, key)
        assertFalse("Second claim must return false to prevent duplicate delivery", secondClaim)
    }

    @Test
    fun testOneTimeReminderOnceDeliveredDoesNotScheduleAlarm() {
        val taskId = 102L
        val now = System.currentTimeMillis()
        val scheduledMillis = now + 600000L // 10 minutes in future
        val key = NudgeAlarmScheduler.getTaskOccurrenceKey(taskId, scheduledMillis)

        // Persist the scheduled trigger
        NudgeAlarmScheduler.saveScheduledTriggerMillis(context, taskId, scheduledMillis)

        // Mark as already delivered
        NudgeAlarmScheduler.markTaskReminderDelivered(context, key)

        val task = NudgeTask(
            id = taskId,
            title = "Status screenshot",
            repeat = "Once",
            dateLabel = "Today",
            timeLabel = "in 10 minutes"
        )

        // Attempting to schedule an already delivered task must do nothing
        NudgeAlarmScheduler.scheduleTask(context, task)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue(
            "Delivered one-time reminder must not be scheduled in AlarmManager",
            scheduledAlarms.none { it.operation.toString().contains(taskId.toString()) }
        )
    }

    @Test
    fun testBootReceiverDeliversMissedOneTimeReminderImmediately() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<com.example.gentlenudge.GentleNudgeApp>()
        val dao = app.database.nudgeTaskDao()

        // Create a one-time task that was scheduled in the past (e.g. 1 hour ago)
        val now = System.currentTimeMillis()
        val missedTime = now - 3600000L // 1 hour ago
        val task = NudgeTask(
            id = 201L,
            title = "Missed WhatsApp status check",
            timeLabel = "1:00 PM",
            dateLabel = "Today",
            repeat = "Once",
            isDone = false,
            createdAt = missedTime - 3600000L
        )
        dao.insertTask(task)

        // Save its scheduled time in the past
        NudgeAlarmScheduler.saveScheduledTriggerMillis(context, task.id, missedTime)

        // Verify not delivered yet
        val key = NudgeAlarmScheduler.getTaskOccurrenceKey(task.id, missedTime)
        assertFalse(NudgeAlarmScheduler.isTaskReminderDelivered(context, key))

        // Clear any notifications
        notificationManager.cancelAll()

        // Trigger BootReceiver
        val bootReceiver = com.example.gentlenudge.notification.BootReceiver()
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        bootReceiver.onReceive(context, intent)

        // Give background coroutine in BootReceiver time to finish
        kotlinx.coroutines.delay(500)

        // Verify missed notification was immediately delivered
        val notifications = shadowNotificationManager.allNotifications
        assertTrue("Missed notification should be delivered on boot", notifications.isNotEmpty())
        val found = notifications.find { it.extras.getString(android.app.Notification.EXTRA_TEXT) == "Missed WhatsApp status check" }
        assertNotNull("Notification for missed task should be posted", found)

        // Verify occurrence is now marked delivered in persistent storage
        assertTrue("Occurrence must be marked delivered", NudgeAlarmScheduler.isTaskReminderDelivered(context, key))

        // Verify it was NOT rescheduled for the next day
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue(
            "Missed one-time reminder must NOT be rescheduled as an alarm for tomorrow",
            scheduledAlarms.none { it.operation.toString().contains(task.id.toString()) }
        )

        // Second boot: must NOT deliver again!
        notificationManager.cancelAll()
        bootReceiver.onReceive(context, intent)
        kotlinx.coroutines.delay(500)
        val secondNotifications = shadowNotificationManager.allNotifications
        assertTrue("Should not deliver again on subsequent boot", secondNotifications.none { it.extras.getString(android.app.Notification.EXTRA_TEXT) == "Missed WhatsApp status check" })

        // Clean up
        dao.deleteTaskById(task.id)
    }

    @Test
    fun testBootReceiverSchedulesFutureOneTimeReminderWithoutDeliveringImmediately() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<com.example.gentlenudge.GentleNudgeApp>()
        val dao = app.database.nudgeTaskDao()

        // Create a one-time task scheduled in the future (e.g. 2 hours from now)
        val now = System.currentTimeMillis()
        val futureTime = now + 7200000L // 2 hours in future
        val task = NudgeTask(
            id = 202L,
            title = "Future status screenshot",
            timeLabel = "4:00 PM",
            dateLabel = "Tomorrow",
            repeat = "Once",
            isDone = false,
            createdAt = now
        )
        dao.insertTask(task)
        NudgeAlarmScheduler.saveScheduledTriggerMillis(context, task.id, futureTime)

        notificationManager.cancelAll()

        // Trigger BootReceiver
        val bootReceiver = com.example.gentlenudge.notification.BootReceiver()
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        bootReceiver.onReceive(context, intent)
        kotlinx.coroutines.delay(500)

        // Verify NO immediate notification was posted
        val notifications = shadowNotificationManager.allNotifications
        assertTrue(
            "Future one-time reminder must NOT be delivered immediately on boot",
            notifications.none { it.extras.getString(android.app.Notification.EXTRA_TEXT) == "Future status screenshot" }
        )

        // Clean up
        dao.deleteTaskById(task.id)
    }

    @Test
    fun test1_OnceReminderCreatedYesterdayForTomorrowCrossingMidnight() {
        // Created yesterday at 3:00 PM with date="Tomorrow", time="4:00 AM", repeat="Once"
        val calYesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val createdAtYesterday = calYesterday.timeInMillis

        // At creation time yesterday:
        val task = NudgeTask(
            id = 501L,
            title = "Test 1 Early Morning",
            dateLabel = "Tomorrow",
            timeLabel = "4:00 AM",
            repeat = "Once",
            isDone = false,
            createdAt = createdAtYesterday
        )

        // Scheduling at creation time yesterday sets the trigger to today at 4:00 AM
        val triggerCalculatedYesterday = NudgeAlarmScheduler.calculateTriggerMillis(task.dateLabel, task.timeLabel, createdAtYesterday)
        NudgeAlarmScheduler.saveScheduledTriggerMillis(context, task.id, triggerCalculatedYesterday)
        val initialTrigger = NudgeAlarmScheduler.getScheduledTriggerMillis(context, task)

        val calToday4am = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals("Initial trigger must be today at 4:00 AM", calToday4am.timeInMillis, initialTrigger)

        // After midnight, say at 2:00 AM today (before 4:00 AM):
        val calAfterMidnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 2)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val triggerAfterMidnight = NudgeAlarmScheduler.getScheduledTriggerMillis(context, task, calAfterMidnight.timeInMillis)
        assertEquals("Trigger after midnight must STILL be today at 4:00 AM, not rolled to tomorrow", calToday4am.timeInMillis, triggerAfterMidnight)

        // Re-scheduling after midnight must not roll to tomorrow
        NudgeAlarmScheduler.scheduleTask(context, task, forceRecalculate = false)
        val persistedTrigger = NudgeAlarmScheduler.getScheduledTriggerMillis(context, task)
        assertEquals("Persisted trigger must not change after midnight", calToday4am.timeInMillis, persistedTrigger)
    }

    @Test
    fun test2_OnceReminderFiresAtScheduledTimeAndMarksCompleted() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<com.example.gentlenudge.GentleNudgeApp>()
        val dao = app.database.nudgeTaskDao()

        val calToday4am = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val scheduledMillis = calToday4am.timeInMillis

        val task = NudgeTask(
            id = 502L,
            title = "Test 2 One Time Reminder",
            dateLabel = "Today",
            timeLabel = "4:00 AM",
            repeat = "Once",
            isDone = false,
            createdAt = scheduledMillis - 3600000L
        )
        dao.insertTask(task)
        NudgeAlarmScheduler.saveScheduledTriggerMillis(context, task.id, scheduledMillis)

        notificationManager.cancelAll()

        // Simulate alarm triggering
        val receiver = NudgeNotificationReceiver()
        val intent = Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = NudgeNotificationHelper.ACTION_FIRE_NUDGE
            putExtra(NudgeNotificationHelper.EXTRA_TASK_ID, task.id)
            putExtra(NudgeNotificationHelper.EXTRA_TASK_TITLE, task.title)
            putExtra(NudgeNotificationHelper.EXTRA_TASK_SCHEDULED_MILLIS, scheduledMillis)
        }
        receiver.onReceive(context, intent)
        kotlinx.coroutines.delay(600)

        // Verify notification delivered
        val notifications = shadowNotificationManager.allNotifications
        assertTrue(
            "Notification must be delivered",
            notifications.any { it.extras.getString(android.app.Notification.EXTRA_TEXT) == "Test 2 One Time Reminder" }
        )

        // Verify task in DB remains pending (Fix #2: do NOT auto-complete upon delivery)
        val updatedTask = dao.getTaskById(task.id)
        assertNotNull(updatedTask)
        assertFalse("Task must NOT be marked isDone = true upon delivery", updatedTask!!.isDone)
        assertNull("completedAt must NOT be populated upon delivery", updatedTask.completedAt)

        // Subsequent schedule call must still be a no-op because occurrence was delivered and is in past
        val initialAlarmCount = shadowAlarmManager.scheduledAlarms.size
        NudgeAlarmScheduler.scheduleTask(context, updatedTask)
        assertEquals("No new alarm must be scheduled for already delivered past occurrence", initialAlarmCount, shadowAlarmManager.scheduledAlarms.size)

        dao.deleteTaskById(task.id)
    }

    @Test
    fun test3_OpenAppAfterOneTimeReminderFiredRemainsCompletedWithoutNewAlarm() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<com.example.gentlenudge.GentleNudgeApp>()
        val dao = app.database.nudgeTaskDao()

        val task = NudgeTask(
            id = 503L,
            title = "Test 3 Already Fired",
            dateLabel = "Today",
            timeLabel = "4:00 AM",
            repeat = "Once",
            isDone = true,
            completedAt = System.currentTimeMillis() - 3600000L
        )
        dao.insertTask(task)

        val beforeCount = shadowAlarmManager.scheduledAlarms.size
        // Simulate app reopen / ViewModel init
        NudgeAlarmScheduler.scheduleTask(context, task, forceRecalculate = false)
        assertEquals("Completed task must not schedule any alarm", beforeCount, shadowAlarmManager.scheduledAlarms.size)

        val inDb = dao.getTaskById(task.id)
        assertTrue("Task must remain completed", inDb!!.isDone)

        dao.deleteTaskById(task.id)
    }

    @Test
    fun test4_DeviceRebootBeforeOneTimeReminderRestoresSameAbsoluteTrigger() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<com.example.gentlenudge.GentleNudgeApp>()
        val dao = app.database.nudgeTaskDao()

        val now = System.currentTimeMillis()
        val futureTrigger = now + 14400000L // 4 hours in future

        val task = NudgeTask(
            id = 504L,
            title = "Test 4 Reboot Upcoming",
            dateLabel = "Today",
            timeLabel = "8:00 PM",
            repeat = "Once",
            isDone = false,
            createdAt = now
        )
        dao.insertTask(task)
        NudgeAlarmScheduler.saveScheduledTriggerMillis(context, task.id, futureTrigger)

        // Clear all shadow alarms to simulate device reboot
        shadowAlarmManager.scheduledAlarms.clear()

        // Trigger BootReceiver
        val bootReceiver = com.example.gentlenudge.notification.BootReceiver()
        bootReceiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        kotlinx.coroutines.delay(500)

        // Verify alarm was restored with the exact same trigger timestamp
        val restoredAlarm = shadowAlarmManager.scheduledAlarms.find {
            val op = it.operation
            val shadowOp = Shadows.shadowOf(op)
            shadowOp.savedIntent.getLongExtra(NudgeNotificationHelper.EXTRA_TASK_ID, -1L) == task.id
        }
        assertNotNull("Alarm must be restored after reboot", restoredAlarm)
        assertEquals("Restored trigger must match original trigger", futureTrigger, restoredAlarm!!.triggerAtTime)

        dao.deleteTaskById(task.id)
    }

    @Test
    fun test5_ActionDateChangedAtMidnightDoesNotAlterScheduledTrigger() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<com.example.gentlenudge.GentleNudgeApp>()
        val dao = app.database.nudgeTaskDao()

        val calToday4am = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val scheduledMillis = calToday4am.timeInMillis

        val task = NudgeTask(
            id = 505L,
            title = "Test 5 Midnight Date Change",
            dateLabel = "Today",
            timeLabel = "4:00 AM",
            repeat = "Once",
            isDone = false,
            createdAt = scheduledMillis - 7200000L
        )
        dao.insertTask(task)
        NudgeAlarmScheduler.saveScheduledTriggerMillis(context, task.id, scheduledMillis)

        // Broadcast ACTION_DATE_CHANGED
        val bootReceiver = com.example.gentlenudge.notification.BootReceiver()
        bootReceiver.onReceive(context, Intent(Intent.ACTION_DATE_CHANGED))
        kotlinx.coroutines.delay(500)

        val persisted = NudgeAlarmScheduler.getScheduledTriggerMillis(context, task)
        assertEquals("Scheduled trigger millis must remain unchanged after ACTION_DATE_CHANGED", scheduledMillis, persisted)

        dao.deleteTaskById(task.id)
    }

    @Test
    fun test6_RepeatingRemindersRecurrenceBehaviorUnchanged() {
        val calNow = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMillis = calNow.timeInMillis

        // Recurring daily alarm at 4:00 AM evaluated at 7:00 AM must advance to tomorrow
        val targetMillis = NudgeAlarmScheduler.calculateNextOccurrenceMillis(
            dateLabel = "Today",
            timeLabel = "4:00 AM",
            repeatRule = "Every day",
            now = nowMillis
        )

        val calExpected = (calNow.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 0)
        }
        assertEquals("Repeating daily alarm must advance to tomorrow", calExpected.timeInMillis, targetMillis)
    }

    @Test
    fun test7_TodayFutureTimeOnceRemainsOnToday() {
        val calNow = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMillis = calNow.timeInMillis

        val task = NudgeTask(
            id = 507L,
            title = "Test 7 Today Afternoon",
            dateLabel = "Today",
            timeLabel = "2:00 PM",
            repeat = "Once",
            isDone = false,
            createdAt = nowMillis
        )

        NudgeAlarmScheduler.scheduleTask(context, task, forceRecalculate = true)
        val scheduled = NudgeAlarmScheduler.getScheduledTriggerMillis(context, task)

        val calExpected = (calNow.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals("Task scheduled for today 2:00 PM must remain on today", calExpected.timeInMillis, scheduled)

        // Opening later at 11:00 AM must not move it to tomorrow
        val calLater = (calNow.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 11)
        }
        NudgeAlarmScheduler.scheduleTask(context, task, forceRecalculate = false)
        val scheduledLater = NudgeAlarmScheduler.getScheduledTriggerMillis(context, task, calLater.timeInMillis)
        assertEquals("Task must remain on today at 2:00 PM", calExpected.timeInMillis, scheduledLater)
    }

    @Test
    fun test8_TomorrowFutureTimeOnceResolvedAtCreationAndDoesNotShiftAtMidnight() {
        val calYesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val task = NudgeTask(
            id = 508L,
            title = "Test 8 Tomorrow Once",
            dateLabel = "Tomorrow",
            timeLabel = "10:00 AM",
            repeat = "Once",
            isDone = false,
            createdAt = calYesterday.timeInMillis
        )

        // Evaluated and established once at creation:
        val calToday10am = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val trigger = NudgeAlarmScheduler.getScheduledTriggerMillis(context, task, calYesterday.timeInMillis)
        assertEquals("Tomorrow evaluated yesterday must resolve to today at 10:00 AM", calToday10am.timeInMillis, trigger)

        // After midnight:
        val calToday1am = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 1)
        }
        val triggerAfterMidnight = NudgeAlarmScheduler.getScheduledTriggerMillis(context, task, calToday1am.timeInMillis)
        assertEquals("Meaning must not shift after midnight", calToday10am.timeInMillis, triggerAfterMidnight)
    }

    @Test
    fun test9_SnoozeAfterNotificationFiresSchedulesNewAlarm() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<com.example.gentlenudge.GentleNudgeApp>()
        val dao = app.database.nudgeTaskDao()

        val task = NudgeTask(
            id = 509L,
            title = "Test 9 Snooze",
            dateLabel = "Today",
            timeLabel = "5:00 AM",
            repeat = "Does not repeat",
            isDone = false,
            createdAt = System.currentTimeMillis() - 3600000L
        )
        dao.insertTask(task)

        // Simulate firing notification
        val receiver = NudgeNotificationReceiver()
        val fireIntent = Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = NudgeNotificationHelper.ACTION_FIRE_NUDGE
            putExtra(NudgeNotificationHelper.EXTRA_TASK_ID, task.id)
            putExtra(NudgeNotificationHelper.EXTRA_TASK_TITLE, task.title)
        }
        receiver.onReceive(context, fireIntent)
        kotlinx.coroutines.delay(300)

        // Task must remain pending after firing
        val pendingTask = dao.getTaskById(task.id)
        assertNotNull(pendingTask)
        assertFalse("Task must not be done", pendingTask!!.isDone)

        // Now user taps Snooze
        val snoozeIntent = Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = NudgeNotificationHelper.ACTION_SNOOZE_NUDGE
            putExtra(NudgeNotificationHelper.EXTRA_TASK_ID, task.id)
        }
        receiver.onReceive(context, snoozeIntent)
        kotlinx.coroutines.delay(300)

        // Verify task updated with new snooze time, still pending
        val snoozedTask = dao.getTaskById(task.id)
        assertNotNull(snoozedTask)
        assertFalse("Snoozed task must not be done", snoozedTask!!.isDone)
        assertEquals("Snoozed task section must be today", "today", snoozedTask.section)

        // Verify alarm is scheduled in the future for snoozed task
        val snoozedAlarm = shadowAlarmManager.scheduledAlarms.find {
            val op = it.operation
            val shadowOp = Shadows.shadowOf(op)
            shadowOp.savedIntent.getLongExtra(NudgeNotificationHelper.EXTRA_TASK_ID, -1L) == task.id
        }
        assertNotNull("Alarm must be scheduled for snoozed task", snoozedAlarm)
        assertTrue("Snoozed alarm must be in the future", snoozedAlarm!!.triggerAtTime > System.currentTimeMillis())

        dao.deleteTaskById(task.id)
    }

    @Test
    fun test10_EditingTitleAfterMidnightDoesNotShiftIntendedOccurrence() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<com.example.gentlenudge.GentleNudgeApp>()
        val dao = app.database.nudgeTaskDao()

        val calYesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calToday10am = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val originalTask = NudgeTask(
            id = 510L,
            title = "Original Title",
            dateLabel = "Tomorrow",
            timeLabel = "10:00 AM",
            repeat = "Does not repeat",
            isDone = false,
            createdAt = calYesterday.timeInMillis
        )
        dao.insertTask(originalTask)
        NudgeAlarmScheduler.saveScheduledTriggerMillis(context, originalTask.id, calToday10am.timeInMillis)

        // Today after midnight, user edits only the title:
        val editedTask = originalTask.copy(title = "Updated Title Only")
        dao.updateTask(editedTask)

        // When scheduleTask is called with forceRecalculate = false (as NudgeViewModel does when schedule info hasn't changed):
        NudgeAlarmScheduler.scheduleTask(context, editedTask, forceRecalculate = false)

        val triggerAfterEdit = NudgeAlarmScheduler.getScheduledTriggerMillis(context, editedTask)
        assertEquals("Scheduled occurrence must remain Today at 10:00 AM after title edit", calToday10am.timeInMillis, triggerAfterEdit)

        // TaskOccurrenceResolver must also resolve to today at 10:00 AM
        val resolvedOccurrence = com.example.gentlenudge.ui.components.TaskOccurrenceResolver.resolveTargetOccurrenceMillis(editedTask, context)
        assertEquals("TaskOccurrenceResolver must preserve Today 10:00 AM", calToday10am.timeInMillis, resolvedOccurrence)

        dao.deleteTaskById(originalTask.id)
    }
}
