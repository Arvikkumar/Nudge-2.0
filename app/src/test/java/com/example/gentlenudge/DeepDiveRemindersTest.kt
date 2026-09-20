package com.example.gentlenudge

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.gentlenudge.deepdive.DeepDiveManager
import com.example.gentlenudge.notification.NudgeNotificationHelper
import com.example.gentlenudge.notification.NudgeNotificationReceiver
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = GentleNudgeApp::class)
class DeepDiveRemindersTest {

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
        DeepDiveManager.init(context)
        DeepDiveManager.endSession(context)
    }

    @Test
    fun testStartSessionWithReminders() {
        val now = System.currentTimeMillis()
        val endTime = now + 3600_000L // 1 hour
        val reminder1 = DeepDiveManager.ReminderPoint(now + 900_000L, "15 min")
        val reminder2 = DeepDiveManager.ReminderPoint(now + 1800_000L, "30 min")

        DeepDiveManager.startSession(context, endTime, "One Shot", listOf(reminder1, reminder2))

        val state = DeepDiveManager.state.value
        assertTrue("Session should be active", state.isActive)
        assertEquals(endTime, state.endTimeMillis)
        assertEquals(2, state.reminderPoints.size)
        assertEquals("15 min", state.reminderPoints[0].label)
        assertEquals("30 min", state.reminderPoints[1].label)

        // Verify alarm scheduled
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue("Should have scheduled alarms", scheduledAlarms.isNotEmpty())
    }

    @Test
    fun testIntermediateReminderKeepsSessionActive() {
        val now = System.currentTimeMillis()
        val endTime = now + 3600_000L
        val reminder1Time = now + 900_000L
        val reminder1 = DeepDiveManager.ReminderPoint(reminder1Time, "15 min")

        DeepDiveManager.startSession(context, endTime, "One Shot", listOf(reminder1))

        // Fire the intermediate reminder
        val reminderIntent = Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = DeepDiveManager.ACTION_FIRE_DEEP_DIVE
            putExtra(DeepDiveManager.EXTRA_IS_REMINDER, true)
            putExtra(DeepDiveManager.EXTRA_REMINDER_TIME_MILLIS, reminder1Time)
            putExtra(DeepDiveManager.EXTRA_REMINDER_LABEL, "15 min")
            putExtra("deep_dive_notification_style", "One Shot")
        }
        val receiver = NudgeNotificationReceiver()
        receiver.onReceive(context, reminderIntent)

        val state = DeepDiveManager.state.value
        // Critical: Session MUST still be active and end time preserved
        assertTrue("Session must remain active after intermediate reminder", state.isActive)
        assertEquals(endTime, state.endTimeMillis)
        // The fired reminder should have been removed from the remaining list
        assertTrue("Fired reminder should be cleared from list", state.reminderPoints.isEmpty())

        // Notification should be shown
        val reminderNotif = shadowNotificationManager.allNotifications.find {
            it.extras.getString(android.app.Notification.EXTRA_TITLE) == "Deep Dive Reminder"
        }
        assertNotNull("Reminder notification should be posted", reminderNotif)
    }

    @Test
    fun testFinalAlarmEndsSession() {
        val now = System.currentTimeMillis()
        val endTime = now + 3600_000L
        val reminder1 = DeepDiveManager.ReminderPoint(now + 900_000L, "15 min")

        DeepDiveManager.startSession(context, endTime, "One Shot", listOf(reminder1))

        // Fire final alarm (EXTRA_IS_REMINDER is false)
        val finalIntent = Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = DeepDiveManager.ACTION_FIRE_DEEP_DIVE
            putExtra(DeepDiveManager.EXTRA_IS_REMINDER, false)
            putExtra("deep_dive_notification_style", "One Shot")
        }
        val receiver = NudgeNotificationReceiver()
        receiver.onReceive(context, finalIntent)

        val state = DeepDiveManager.state.value
        assertFalse("Session must NOT be active after final alarm", state.isActive)
        assertEquals(0L, state.endTimeMillis)
        assertTrue("Reminder points must be cleared", state.reminderPoints.isEmpty())
    }

    @Test
    fun testEndSessionCancelsEverything() {
        val now = System.currentTimeMillis()
        val endTime = now + 3600_000L
        val reminder1 = DeepDiveManager.ReminderPoint(now + 900_000L, "15 min")

        DeepDiveManager.startSession(context, endTime, "One Shot", listOf(reminder1))
        assertTrue(DeepDiveManager.state.value.isActive)

        DeepDiveManager.endSession(context)
        val state = DeepDiveManager.state.value
        assertFalse(state.isActive)
        assertEquals(0L, state.endTimeMillis)
        assertTrue(state.reminderPoints.isEmpty())
    }

    @Test
    fun testCaseA_NormalValidCustomReminder() {
        // Current: 7:28 PM, End: 7:58 PM, Select: 7:45 PM
        val nowCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 19)
            set(java.util.Calendar.MINUTE, 28)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val now = nowCal.timeInMillis
        val end = now + 30 * 60 * 1000L // 7:58 PM

        val result = DeepDiveManager.resolveCustomReminderMillis(
            hourOfDay = 19,
            minute = 45,
            nowMillis = now,
            targetEndMillis = end
        )

        assertNotNull("Valid custom reminder should be resolved", result)
        val expectedCal = java.util.Calendar.getInstance().apply {
            timeInMillis = now
            set(java.util.Calendar.HOUR_OF_DAY, 19)
            set(java.util.Calendar.MINUTE, 45)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        assertEquals(expectedCal.timeInMillis, result)
        assertTrue("Reminder must be after now", result!! > now)
        assertTrue("Reminder must be strictly before end", result < end)
    }

    @Test
    fun testCaseB_CustomReminderExactlyAtEnd_Rejected() {
        // Current: 7:28 PM, End: 7:58 PM, Select: 7:58 PM
        val nowCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 19)
            set(java.util.Calendar.MINUTE, 28)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val now = nowCal.timeInMillis
        val end = now + 30 * 60 * 1000L // 7:58 PM

        val result = DeepDiveManager.resolveCustomReminderMillis(
            hourOfDay = 19,
            minute = 58,
            nowMillis = now,
            targetEndMillis = end
        )

        org.junit.Assert.assertNull("Custom reminder exactly at end time must be rejected", result)
    }

    @Test
    fun testCaseC_CustomReminderAfterEnd_Rejected() {
        // Current: 7:28 PM, End: 7:58 PM, Select: 8:21 PM
        val nowCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 19)
            set(java.util.Calendar.MINUTE, 28)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val now = nowCal.timeInMillis
        val end = now + 30 * 60 * 1000L // 7:58 PM

        val result = DeepDiveManager.resolveCustomReminderMillis(
            hourOfDay = 20,
            minute = 21,
            nowMillis = now,
            targetEndMillis = end
        )

        org.junit.Assert.assertNull("Custom reminder after end time must be rejected", result)
    }

    @Test
    fun testCaseD_ExistingPresetAndCustomCoexist() {
        val now = System.currentTimeMillis()
        val endTime = now + 3600_000L // 1 hr (e.g. now=7:00, end=8:00)

        val preset15 = DeepDiveManager.ReminderPoint(now + 900_000L, "15 min") // 7:15
        val custom = DeepDiveManager.ReminderPoint(now + 2400_000L, "Custom") // 7:40

        DeepDiveManager.startSession(context, endTime, "One Shot", listOf(preset15, custom))

        val state = DeepDiveManager.state.value
        assertEquals(2, state.reminderPoints.size)
        assertEquals("15 min", state.reminderPoints[0].label)
        assertEquals("Custom", state.reminderPoints[1].label)
    }

    @Test
    fun testCaseE_DuplicateReminderPrevention() {
        val now = System.currentTimeMillis()
        val endTime = now + 3600_000L
        val presetTime = now + 900_000L
        val existingReminder = DeepDiveManager.ReminderPoint(presetTime, "15 min")

        // Attempting to add a custom reminder at the exact same minute
        val isDuplicate = listOf(existingReminder).any { Math.abs(it.triggerTimeMillis - presetTime) < 60_000L }
        assertTrue("Duplicate time within 1 minute should be detected", isDuplicate)
    }

    @Test
    fun testCaseF_AmPmDistinction() {
        // Current: 7:28 PM (19:28), End: 7:58 PM (19:58).
        // User accidentally chooses 7:45 AM (07:45) instead of 7:45 PM (19:45).
        val nowCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 19)
            set(java.util.Calendar.MINUTE, 28)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val now = nowCal.timeInMillis
        val end = now + 30 * 60 * 1000L // 7:58 PM

        val resultAm = DeepDiveManager.resolveCustomReminderMillis(
            hourOfDay = 7,
            minute = 45,
            nowMillis = now,
            targetEndMillis = end
        )
        org.junit.Assert.assertNull("7:45 AM is not within 7:28 PM to 7:58 PM session and must be rejected", resultAm)

        val resultPm = DeepDiveManager.resolveCustomReminderMillis(
            hourOfDay = 19,
            minute = 45,
            nowMillis = now,
            targetEndMillis = end
        )
        assertNotNull("7:45 PM is within 7:28 PM to 7:58 PM session and must be accepted", resultPm)
    }

    @Test
    fun testCaseG_MidnightCrossing() {
        // Current: 11:50 PM (23:50), End: 12:20 AM tomorrow (00:20 + 1 day).
        // User selects 12:05 AM (00:05).
        val nowCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 50)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val now = nowCal.timeInMillis
        val end = now + 30 * 60 * 1000L // 12:20 AM tomorrow

        // Pick 12:05 AM
        val validMidnightReminder = DeepDiveManager.resolveCustomReminderMillis(
            hourOfDay = 0,
            minute = 5,
            nowMillis = now,
            targetEndMillis = end
        )
        assertNotNull("12:05 AM across midnight must be accepted", validMidnightReminder)
        assertTrue("Must be after 11:50 PM", validMidnightReminder!! > now)
        assertTrue("Must be before 12:20 AM", validMidnightReminder < end)

        // Pick 12:25 AM (after session end)
        val invalidMidnightReminder = DeepDiveManager.resolveCustomReminderMillis(
            hourOfDay = 0,
            minute = 25,
            nowMillis = now,
            targetEndMillis = end
        )
        org.junit.Assert.assertNull("12:25 AM is after 12:20 AM session end and must be rejected", invalidMidnightReminder)
    }

    @Test
    fun testCaseH_ShortRemainingDuration_FailsGracefully() {
        val now = System.currentTimeMillis()
        val end = now + 30_000L // only 30 seconds left

        val result = DeepDiveManager.resolveCustomReminderMillis(
            hourOfDay = 12,
            minute = 0,
            nowMillis = now,
            targetEndMillis = end
        )
        org.junit.Assert.assertNull("No reminder can be scheduled when remaining session time is <= 0 or too short", result)

        val endedResult = DeepDiveManager.resolveCustomReminderMillis(
            hourOfDay = 12,
            minute = 0,
            nowMillis = now,
            targetEndMillis = now - 1000L
        )
        org.junit.Assert.assertNull("Target end in the past must return null", endedResult)
    }
}
