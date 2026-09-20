package com.example.gentlenudge

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.gentlenudge.data.model.NudgeTask
import com.example.gentlenudge.notification.BootReceiver
import com.example.gentlenudge.notification.NudgeAlarmScheduler
import com.example.gentlenudge.notification.NudgeNotificationHelper
import com.example.gentlenudge.notification.NudgeNotificationReceiver
import com.example.gentlenudge.ui.components.RecurringReminderUiHelper
import com.example.gentlenudge.ui.components.TaskOccurrenceResolver
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowNotificationManager
import java.time.LocalDateTime
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = GentleNudgeApp::class)
class RecurringReminderLifecycleTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var shadowNotificationManager: ShadowNotificationManager

    @Before
    fun setup() {
        runBlocking {
            context = ApplicationProvider.getApplicationContext()
            val app = ApplicationProvider.getApplicationContext<GentleNudgeApp>()
            app.database.nudgeTaskDao().clearAll()
        }

        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = shadowOf(alarmManager)
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = shadowOf(notificationManager)
        NudgeNotificationHelper.createNotificationChannels(context)

        // Clear shared preferences for clean test state
        val prefs = context.getSharedPreferences("gentle_nudge_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    /**
     * SCENARIO A: Evening creation at 7:30 PM for Today + 4:00 AM + Every day.
     * The first occurrence must be Monday 4:00 AM, scheduled for Monday 4:00 AM,
     * and displayed as Tomorrow from Sunday evening's perspective.
     */
    @Test
    fun testScenarioA_EveningCreation_CalculatesMondayFourAm() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<GentleNudgeApp>()
        val dao = app.database.nudgeTaskDao()

        // Sunday Sep 13, 2026 at 7:30 PM (19:30)
        val sundayEvening = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 13, 19, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val sundayEveningMillis = sundayEvening.timeInMillis

        val task = NudgeTask(
            id = 1001L,
            title = "Morning stretch",
            dateLabel = "Today",
            timeLabel = "4:00 AM",
            repeat = "Every day",
            createdAt = sundayEveningMillis,
            isDone = false
        )
        dao.insertTask(task)

        // Calculate occurrence from evening creation perspective
        val scheduledMillis = NudgeAlarmScheduler.calculateNextOccurrenceMillis(
            dateLabel = task.dateLabel,
            timeLabel = task.timeLabel,
            repeatRule = task.repeat,
            now = sundayEveningMillis,
            baseRef = sundayEveningMillis
        )

        val mondayFourAm = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 14, 4, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals("Occurrence must be Monday 4:00 AM", mondayFourAm.timeInMillis, scheduledMillis)

        // Schedule task using the scheduler
        NudgeAlarmScheduler.saveScheduledTriggerMillis(context, task.id, scheduledMillis)
        NudgeAlarmScheduler.scheduleTask(context, task, forceRecalculate = false)

        val resolved = TaskOccurrenceResolver.resolveTargetOccurrenceMillis(task, context, sundayEveningMillis)
        assertEquals("Authoritative occurrence must be Monday 4:00 AM", mondayFourAm.timeInMillis, resolved)

        // Display string from Sunday evening perspective
        val sundayEveningLdt = LocalDateTime.of(2026, 9, 13, 19, 30, 0)
        val display = RecurringReminderUiHelper.getRecurringReminderDisplay(
            task = task,
            context = context,
            now = sundayEveningLdt,
            nowMillis = sundayEveningMillis
        )
        assertNotNull("Display info must not be null", display)
        assertEquals("Tomorrow", display!!.relativeDateLabel)
        assertEquals("8h 30m", display.countdown)
        assertEquals("Tomorrow, 8h 30m", display.displayString)
    }

    /**
     * SCENARIO B: Pre-alarm state on Monday at 3:30 AM for a 4:00 AM reminder.
     * Must remain Monday 4:00 AM, displaying "Today, 30m".
     */
    @Test
    fun testScenarioB_PreAlarmStateAtThreeThirtyAm_DisplaysTodayThirtyMinutes() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<GentleNudgeApp>()
        val dao = app.database.nudgeTaskDao()

        val mondayFourAm = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 14, 4, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val mondayThreeThirtyAm = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 14, 3, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val task = NudgeTask(
            id = 1002L,
            title = "Early jog",
            dateLabel = "Today",
            timeLabel = "4:00 AM",
            repeat = "Every day",
            createdAt = mondayFourAm.timeInMillis - (12 * 3600 * 1000L),
            isDone = false
        )
        dao.insertTask(task)
        NudgeAlarmScheduler.saveScheduledTriggerMillis(context, task.id, mondayFourAm.timeInMillis)

        val resolved = TaskOccurrenceResolver.resolveTargetOccurrenceMillis(task, context, mondayThreeThirtyAm.timeInMillis)
        assertEquals("Target occurrence must be Monday 4:00 AM", mondayFourAm.timeInMillis, resolved)

        val mondayThreeThirtyLdt = LocalDateTime.of(2026, 9, 14, 3, 30, 0)
        val display = RecurringReminderUiHelper.getRecurringReminderDisplay(
            task = task,
            context = context,
            now = mondayThreeThirtyLdt,
            nowMillis = mondayThreeThirtyAm.timeInMillis
        )
        assertNotNull(display)
        assertEquals("Today", display!!.relativeDateLabel)
        assertEquals("30m", display.countdown)
        assertEquals("Today, 30m", display.displayString)
    }

    /**
     * SCENARIO C: Alarm firing with successful notification delivery.
     * Notification is posted, occurrence marked delivered, task advances to Tuesday 4:00 AM.
     */
    @Test
    fun testScenarioC_AlarmFiresWithSuccessfulDelivery_AdvancesToNextOccurrence() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<GentleNudgeApp>()
        val dao = app.database.nudgeTaskDao()

        val mondayFourAm = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 14, 4, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val task = NudgeTask(
            id = 1003L,
            title = "Meditation",
            dateLabel = "Today",
            timeLabel = "4:00 AM",
            repeat = "Every day",
            isDone = false
        )
        dao.insertTask(task)
        NudgeAlarmScheduler.saveScheduledTriggerMillis(context, task.id, mondayFourAm.timeInMillis)

        val receiver = NudgeNotificationReceiver()
        val fireIntent = Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = NudgeNotificationHelper.ACTION_FIRE_NUDGE
            putExtra(NudgeNotificationHelper.EXTRA_TASK_ID, task.id)
            putExtra(NudgeNotificationHelper.EXTRA_TASK_SCHEDULED_MILLIS, mondayFourAm.timeInMillis)
        }
        receiver.onReceive(context, fireIntent)
        delay(300)

        // 1. Notification must be posted
        val activeNotifications = shadowNotificationManager.allNotifications
        assertTrue("Notification must be posted", activeNotifications.isNotEmpty())

        // 2. Occurrence must be marked delivered
        val occurrenceKey = NudgeAlarmScheduler.getTaskOccurrenceKey(task.id, mondayFourAm.timeInMillis)
        assertTrue("Monday occurrence must be marked delivered", NudgeAlarmScheduler.isTaskReminderDelivered(context, occurrenceKey))

        // 3. Task in DB must be advanced to Tuesday and not marked done
        val updatedDbTask = dao.getTaskById(task.id)
        assertNotNull(updatedDbTask)
        assertFalse("Task must remain pending for next cycle", updatedDbTask!!.isDone)
        val tuesdayFourAm = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 15, 4, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val expectedTuesdayLabel = NudgeNotificationReceiver.formatOccurrenceDateLabel(tuesdayFourAm)
        assertEquals("Date label must be advanced to Tuesday", expectedTuesdayLabel, updatedDbTask.dateLabel)

        // 4. Next scheduled trigger must be Tuesday 4:00 AM
        val nextTrigger = NudgeAlarmScheduler.getScheduledTriggerMillis(context, updatedDbTask)
        assertEquals("Next scheduled trigger must be Tuesday 4:00 AM", tuesdayFourAm.timeInMillis, nextTrigger)
    }

    /**
     * SCENARIO D: Undelivered occurrence past scheduled time (e.g. at 4:01 AM or 5:30 AM).
     * Without notification delivery, occurrence MUST NOT advance to Tuesday!
     * It remains Monday 4:00 AM, displays "Today, now", and is included in Today's Nudges.
     */
    @Test
    fun testScenarioD_UndeliveredOccurrencePastScheduledTime_DoesNotAdvance() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<GentleNudgeApp>()
        val dao = app.database.nudgeTaskDao()

        val mondayFourAm = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 14, 4, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val mondayFiveThirtyAm = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 14, 5, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val task = NudgeTask(
            id = 1004L,
            title = "Morning hydration",
            dateLabel = "Today",
            timeLabel = "4:00 AM",
            repeat = "Every day",
            isDone = false
        )
        dao.insertTask(task)
        NudgeAlarmScheduler.saveScheduledTriggerMillis(context, task.id, mondayFourAm.timeInMillis)

        // Occurrence has NOT been delivered
        val occurrenceKey = NudgeAlarmScheduler.getTaskOccurrenceKey(task.id, mondayFourAm.timeInMillis)
        assertFalse("Occurrence is not delivered", NudgeAlarmScheduler.isTaskReminderDelivered(context, occurrenceKey))

        // Authoritative occurrence resolution at 5:30 AM must STILL return Monday 4:00 AM!
        val resolvedOccurrence = TaskOccurrenceResolver.resolveTargetOccurrenceMillis(
            task,
            context,
            mondayFiveThirtyAm.timeInMillis
        )
        assertEquals("Undelivered occurrence must not advance to tomorrow", mondayFourAm.timeInMillis, resolvedOccurrence)

        // Today or past check must return true so it remains in Today's Nudges list!
        val isTodayOrPast = TaskOccurrenceResolver.isOccurrenceTodayOrPast(resolvedOccurrence, mondayFiveThirtyAm.timeInMillis)
        assertTrue("Undelivered occurrence must remain in Today's nudges", isTodayOrPast)

        // UI display at 5:30 AM must show Today, now (not Tomorrow!)
        val mondayFiveThirtyLdt = LocalDateTime.of(2026, 9, 14, 5, 30, 0)
        val display = RecurringReminderUiHelper.getRecurringReminderDisplay(
            task = task,
            context = context,
            now = mondayFiveThirtyLdt,
            nowMillis = mondayFiveThirtyAm.timeInMillis
        )
        assertNotNull(display)
        assertEquals("Today", display!!.relativeDateLabel)
        assertEquals("now", display.countdown)
        assertEquals("Today, now", display.displayString)
    }

    /**
     * SCENARIO E: Subsequent delivery of the delayed occurrence.
     * When the delayed alarm finally delivers, it advances to Tuesday.
     */
    @Test
    fun testScenarioE_DelayedOccurrenceDelivered_AdvancesToFollowingCycle() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<GentleNudgeApp>()
        val dao = app.database.nudgeTaskDao()

        val mondayFourAm = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 14, 4, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val task = NudgeTask(
            id = 1005L,
            title = "Review goals",
            dateLabel = "Today",
            timeLabel = "4:00 AM",
            repeat = "Every day",
            isDone = false
        )
        dao.insertTask(task)
        NudgeAlarmScheduler.saveScheduledTriggerMillis(context, task.id, mondayFourAm.timeInMillis)

        // Fire delayed alarm
        val receiver = NudgeNotificationReceiver()
        val fireIntent = Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = NudgeNotificationHelper.ACTION_FIRE_NUDGE
            putExtra(NudgeNotificationHelper.EXTRA_TASK_ID, task.id)
            putExtra(NudgeNotificationHelper.EXTRA_TASK_SCHEDULED_MILLIS, mondayFourAm.timeInMillis)
        }
        receiver.onReceive(context, fireIntent)
        delay(300)

        // After successful delivery, it must now be delivered and advanced
        val occurrenceKey = NudgeAlarmScheduler.getTaskOccurrenceKey(task.id, mondayFourAm.timeInMillis)
        assertTrue("Occurrence is now marked delivered", NudgeAlarmScheduler.isTaskReminderDelivered(context, occurrenceKey))

        val updatedTask = dao.getTaskById(task.id)
        assertNotNull(updatedTask)
        val tuesdayFourAm = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 15, 4, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nextTrigger = NudgeAlarmScheduler.getScheduledTriggerMillis(context, updatedTask!!)
        assertEquals("Now advanced to Tuesday 4:00 AM", tuesdayFourAm.timeInMillis, nextTrigger)
    }

    /**
     * SCENARIO F: Boot recovery of undelivered recurring occurrence.
     * Device was off at 4:00 AM, boots at 7:00 AM.
     * BootReceiver detects undelivered Monday occurrence, delivers it, and advances to Tuesday.
     */
    @Test
    fun testScenarioF_BootRecoveryOfUndeliveredRecurringOccurrence() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<GentleNudgeApp>()
        val dao = app.database.nudgeTaskDao()

        val mondayFourAm = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 14, 4, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val task = NudgeTask(
            id = 1006L,
            title = "Morning vitamins",
            dateLabel = "Today",
            timeLabel = "4:00 AM",
            repeat = "Every day",
            isDone = false
        )
        dao.insertTask(task)
        NudgeAlarmScheduler.saveScheduledTriggerMillis(context, task.id, mondayFourAm.timeInMillis)

        // Device boots at Monday 7:00 AM
        val bootReceiver = BootReceiver()
        val bootIntent = Intent(Intent.ACTION_BOOT_COMPLETED)
        bootReceiver.onReceive(context, bootIntent)
        delay(300)

        // Notification must have been delivered on boot
        val notifications = shadowNotificationManager.allNotifications
        assertTrue("Missed recurring reminder notification must be posted on boot", notifications.isNotEmpty())

        // Monday occurrence must be marked delivered
        val occurrenceKey = NudgeAlarmScheduler.getTaskOccurrenceKey(task.id, mondayFourAm.timeInMillis)
        assertTrue("Monday occurrence must be marked delivered", NudgeAlarmScheduler.isTaskReminderDelivered(context, occurrenceKey))

        // Task must be advanced to Tuesday
        val updatedTask = dao.getTaskById(task.id)
        assertNotNull(updatedTask)
        val tuesdayFourAm = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 15, 4, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nextTrigger = NudgeAlarmScheduler.getScheduledTriggerMillis(context, updatedTask!!)
        assertEquals("Advanced to Tuesday 4:00 AM trigger", tuesdayFourAm.timeInMillis, nextTrigger)
    }

    /**
     * SCENARIO G: Boot recovery when occurrence was already delivered before reboot.
     * Must NOT repost duplicate notification.
     */
    @Test
    fun testScenarioG_BootRecoveryWhenAlreadyDelivered_DoesNotRepostDuplicate() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<GentleNudgeApp>()
        val dao = app.database.nudgeTaskDao()

        val mondayFourAm = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 14, 4, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val task = NudgeTask(
            id = 1007L,
            title = "Water plants",
            dateLabel = "Today",
            timeLabel = "4:00 AM",
            repeat = "Every day",
            isDone = false
        )
        dao.insertTask(task)
        NudgeAlarmScheduler.saveScheduledTriggerMillis(context, task.id, mondayFourAm.timeInMillis)

        // Mark already delivered before reboot
        val occurrenceKey = NudgeAlarmScheduler.getTaskOccurrenceKey(task.id, mondayFourAm.timeInMillis)
        NudgeAlarmScheduler.markTaskReminderDelivered(context, occurrenceKey)

        val countBefore = shadowNotificationManager.allNotifications.size

        // Device boots
        val bootReceiver = BootReceiver()
        val bootIntent = Intent(Intent.ACTION_BOOT_COMPLETED)
        bootReceiver.onReceive(context, bootIntent)
        delay(300)

        // No new notifications should be posted
        assertEquals("No duplicate notification should be posted on boot", countBefore, shadowNotificationManager.allNotifications.size)
    }

    /**
     * SCENARIO H: Notification disabled on boot.
     * Does NOT post notification and does NOT advance occurrence.
     */
    @Test
    fun testScenarioH_NotificationsDisabledOnBoot_DoesNotAdvanceOccurrence() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<GentleNudgeApp>()
        val dao = app.database.nudgeTaskDao()

        val prefs = context.getSharedPreferences("gentle_nudge_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("notifications_enabled", false).commit()

        val mondayFourAm = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 14, 4, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val task = NudgeTask(
            id = 1008L,
            title = "Read a chapter",
            dateLabel = "Today",
            timeLabel = "4:00 AM",
            repeat = "Every day",
            isDone = false
        )
        dao.insertTask(task)
        NudgeAlarmScheduler.saveScheduledTriggerMillis(context, task.id, mondayFourAm.timeInMillis)

        val bootReceiver = BootReceiver()
        val bootIntent = Intent(Intent.ACTION_BOOT_COMPLETED)
        bootReceiver.onReceive(context, bootIntent)
        delay(300)

        // Notifications disabled: nothing posted
        val notifications = shadowNotificationManager.allNotifications
        assertTrue("No notification posted when disabled", notifications.isEmpty())

        // Must NOT be marked delivered
        val occurrenceKey = NudgeAlarmScheduler.getTaskOccurrenceKey(task.id, mondayFourAm.timeInMillis)
        assertFalse("Must not be marked delivered", NudgeAlarmScheduler.isTaskReminderDelivered(context, occurrenceKey))

        // Authoritative occurrence must remain Monday 4:00 AM
        val resolved = TaskOccurrenceResolver.resolveTargetOccurrenceMillis(task, context, System.currentTimeMillis())
        assertEquals("Must remain Monday 4:00 AM occurrence", mondayFourAm.timeInMillis, resolved)
    }

    /**
     * SCENARIO I: Snooze on recurring reminder notification.
     * Reschedules for 30 minutes in future, updates DB and alarm, and dismisses notification.
     */
    @Test
    fun testScenarioI_SnoozeRecurringReminder_ReschedulesCorrectly() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<GentleNudgeApp>()
        val dao = app.database.nudgeTaskDao()

        val task = NudgeTask(
            id = 1009L,
            title = "Take morning medicine",
            dateLabel = "Today",
            timeLabel = "4:00 AM",
            repeat = "Every day",
            isDone = false
        )
        dao.insertTask(task)

        val receiver = NudgeNotificationReceiver()
        val snoozeIntent = Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = NudgeNotificationHelper.ACTION_SNOOZE_NUDGE
            putExtra(NudgeNotificationHelper.EXTRA_TASK_ID, task.id)
        }
        receiver.onReceive(context, snoozeIntent)
        delay(300)

        val updatedTask = dao.getTaskById(task.id)
        assertNotNull(updatedTask)
        assertFalse("Snoozed task must not be done", updatedTask!!.isDone)
        assertEquals("Today", updatedTask.dateLabel)

        // Alarm must be scheduled for snoozed time
        val alarms = shadowAlarmManager.scheduledAlarms
        assertTrue("Alarm must be scheduled for snooze", alarms.isNotEmpty())
    }

    /**
     * SCENARIO J: One-time reminder non-regression.
     * Delivery claim prevents duplicate delivery, does NOT mark done in DB, and does not convert to recurring.
     */
    @Test
    fun testScenarioJ_OneTimeReminder_DoesNotMarkDoneAndNoDuplicate() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<GentleNudgeApp>()
        val dao = app.database.nudgeTaskDao()

        val task = NudgeTask(
            id = 1010L,
            title = "Call doctor",
            dateLabel = "Today",
            timeLabel = "2:00 PM",
            repeat = "Once",
            isDone = false
        )
        dao.insertTask(task)

        val scheduledMillis = System.currentTimeMillis()
        NudgeAlarmScheduler.saveScheduledTriggerMillis(context, task.id, scheduledMillis)

        val receiver = NudgeNotificationReceiver()
        val fireIntent = Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = NudgeNotificationHelper.ACTION_FIRE_NUDGE
            putExtra(NudgeNotificationHelper.EXTRA_TASK_ID, task.id)
            putExtra(NudgeNotificationHelper.EXTRA_TASK_SCHEDULED_MILLIS, scheduledMillis)
        }

        // First firing
        receiver.onReceive(context, fireIntent)
        var wait = 0
        while (shadowNotificationManager.allNotifications.isEmpty() && wait < 20) {
            delay(50)
            wait++
        }

        val occurrenceKey = NudgeAlarmScheduler.getTaskOccurrenceKey(task.id, scheduledMillis)
        assertTrue("One-time occurrence is delivered", NudgeAlarmScheduler.isTaskReminderDelivered(context, occurrenceKey))

        val dbTaskAfterFire = dao.getTaskById(task.id)
        assertNotNull(dbTaskAfterFire)
        assertFalse("One-time task must NOT be automatically marked done", dbTaskAfterFire!!.isDone)
        assertEquals("Once", dbTaskAfterFire.repeat)

        // Second firing (duplicate alarm / broadcast race condition)
        val countBeforeDuplicate = shadowNotificationManager.allNotifications.size
        receiver.onReceive(context, fireIntent)
        delay(300)

        assertEquals("Duplicate fire must not re-deliver notification", countBeforeDuplicate, shadowNotificationManager.allNotifications.size)
    }
}
