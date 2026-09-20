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
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowNotificationManager
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = GentleNudgeApp::class)
class BugFixesRegressionTest {

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

    private fun makeCalendar(hour: Int, minute: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    @Test
    fun testBug1_RecurringTaskDoneFromNotification_PreservesNextOccurrenceAndAlarm() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<GentleNudgeApp>()
        val dao = app.database.nudgeTaskDao()

        val task = NudgeTask(
            id = 901L,
            title = "Daily Standup Meeting",
            dateLabel = "Today",
            timeLabel = "9:00 AM",
            category = "Work",
            repeat = "Every day",
            isDone = false
        )
        dao.insertTask(task)

        // 1. Alarm fires (ACTION_FIRE_NUDGE)
        val receiver = NudgeNotificationReceiver()
        val fireIntent = Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = NudgeNotificationHelper.ACTION_FIRE_NUDGE
            putExtra(NudgeNotificationHelper.EXTRA_TASK_ID, task.id)
            putExtra(NudgeNotificationHelper.EXTRA_TASK_TITLE, task.title)
        }
        receiver.onReceive(context, fireIntent)
        delay(300)

        // Verify task in DB was advanced to the next occurrence and is NOT done
        val dbTaskAfterFire = dao.getTaskById(task.id)
        assertNotNull("Task record must exist", dbTaskAfterFire)
        assertFalse("Tomorrow's occurrence must NOT be marked done", dbTaskAfterFire!!.isDone)
        val expectedNextDateLabel = NudgeNotificationReceiver.formatOccurrenceDateLabel(
            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        )
        assertEquals("Date label must be advanced to next occurrence date", expectedNextDateLabel, dbTaskAfterFire.dateLabel)

        // Verify alarm was scheduled for tomorrow
        val alarmsAfterFire = shadowAlarmManager.scheduledAlarms
        assertTrue("Alarm for tomorrow must be scheduled", alarmsAfterFire.isNotEmpty())

        // 2. User taps "Done" on today's notification (ACTION_MARK_DONE)
        val doneIntent = Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = NudgeNotificationHelper.ACTION_MARK_DONE
            putExtra(NudgeNotificationHelper.EXTRA_TASK_ID, task.id)
            putExtra(NudgeNotificationHelper.EXTRA_TASK_TITLE, task.title)
            putExtra(NudgeNotificationHelper.EXTRA_TASK_IS_REPEATING, true)
            putExtra(NudgeNotificationHelper.EXTRA_TASK_OCCURRENCE_DATE, "Today")
            putExtra(NudgeNotificationHelper.EXTRA_TASK_OCCURRENCE_TIME, "9:00 AM")
        }
        receiver.onReceive(context, doneIntent)
        delay(300)

        // Verify tomorrow's occurrence remains NOT done and pending
        val dbTaskAfterDone = dao.getTaskById(task.id)
        assertNotNull("Tomorrow's task must still exist in DB", dbTaskAfterDone)
        assertFalse("Tomorrow's occurrence must remain pending (isDone = false)", dbTaskAfterDone!!.isDone)
        assertEquals("Date label must remain advanced occurrence date", expectedNextDateLabel, dbTaskAfterDone.dateLabel)

        // Verify tomorrow's recurring alarm was NOT cancelled
        val alarmsAfterDone = shadowAlarmManager.scheduledAlarms
        assertTrue("Tomorrow's recurring alarm must NOT be cancelled", alarmsAfterDone.isNotEmpty())

        // Verify today's occurrence was saved as completed
        val allTasks = dao.getAllTasksList()
        assertTrue("Today's occurrence must be recorded as completed", allTasks.any {
            it.title == "Daily Standup Meeting" && it.isDone
        })

        dao.deleteTaskById(task.id)
    }

    @Test
    fun testBug1_RecurringTaskContinuesToFollowingOccurrenceNormally() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<GentleNudgeApp>()
        val dao = app.database.nudgeTaskDao()

        val task = NudgeTask(
            id = 902L,
            title = "Evening Workout",
            dateLabel = "Today",
            timeLabel = "6:00 PM",
            category = "Health",
            repeat = "Every day",
            isDone = false
        )
        dao.insertTask(task)

        val receiver = NudgeNotificationReceiver()

        // Occurrence 1: Alarm fires
        receiver.onReceive(context, Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = NudgeNotificationHelper.ACTION_FIRE_NUDGE
            putExtra(NudgeNotificationHelper.EXTRA_TASK_ID, task.id)
            putExtra(NudgeNotificationHelper.EXTRA_TASK_TITLE, task.title)
        })
        delay(300)

        // Occurrence 1: User marks Done
        receiver.onReceive(context, Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = NudgeNotificationHelper.ACTION_MARK_DONE
            putExtra(NudgeNotificationHelper.EXTRA_TASK_ID, task.id)
            putExtra(NudgeNotificationHelper.EXTRA_TASK_TITLE, task.title)
            putExtra(NudgeNotificationHelper.EXTRA_TASK_IS_REPEATING, true)
            putExtra(NudgeNotificationHelper.EXTRA_TASK_OCCURRENCE_DATE, "Today")
            putExtra(NudgeNotificationHelper.EXTRA_TASK_OCCURRENCE_TIME, "6:00 PM")
        })
        delay(300)

        // Occurrence 2 (Tomorrow): Alarm fires again for task.id
        receiver.onReceive(context, Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = NudgeNotificationHelper.ACTION_FIRE_NUDGE
            putExtra(NudgeNotificationHelper.EXTRA_TASK_ID, task.id)
            putExtra(NudgeNotificationHelper.EXTRA_TASK_TITLE, task.title)
        })
        delay(300)

        val dbTaskAfterSecondFire = dao.getTaskById(task.id)
        assertNotNull(dbTaskAfterSecondFire)
        assertFalse("Following occurrence must remain pending", dbTaskAfterSecondFire!!.isDone)

        // Occurrence 2: User marks Done
        receiver.onReceive(context, Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = NudgeNotificationHelper.ACTION_MARK_DONE
            putExtra(NudgeNotificationHelper.EXTRA_TASK_ID, task.id)
            putExtra(NudgeNotificationHelper.EXTRA_TASK_TITLE, task.title)
            putExtra(NudgeNotificationHelper.EXTRA_TASK_IS_REPEATING, true)
            putExtra(NudgeNotificationHelper.EXTRA_TASK_OCCURRENCE_DATE, "Tomorrow")
            putExtra(NudgeNotificationHelper.EXTRA_TASK_OCCURRENCE_TIME, "6:00 PM")
        })
        delay(300)

        // Task must continue to following occurrence
        val dbTaskAfterSecondDone = dao.getTaskById(task.id)
        assertNotNull(dbTaskAfterSecondDone)
        assertFalse("Task must continue normally with isDone = false", dbTaskAfterSecondDone!!.isDone)

        // Completed tasks should now include both occurrences
        val completed = dao.getAllTasksList().filter { it.title == "Evening Workout" && it.isDone }
        assertEquals("Both occurrences must be recorded as completed", 2, completed.size)

        dao.deleteTaskById(task.id)
    }

    @Test
    fun testBug2_TonightBefore830PM_PreservesExistingBehavior() {
        // 1. Before 8:00 PM (e.g. 7:00 PM): target is 8:00 PM today
        val cal7pm = makeCalendar(19, 0)
        val trigger7pm = NudgeAlarmScheduler.calculateTriggerMillis("Tonight", "Any time", cal7pm.timeInMillis)
        val expected8pm = makeCalendar(20, 0).timeInMillis
        assertEquals("Tonight created at 7:00 PM must trigger at 8:00 PM today", expected8pm, trigger7pm)

        // 2. Between 8:00 PM and 8:30 PM (e.g. 8:15 PM): target is 8:30 PM today
        val cal815pm = makeCalendar(20, 15)
        val trigger815pm = NudgeAlarmScheduler.calculateTriggerMillis("Tonight", "Any time", cal815pm.timeInMillis)
        val expected830pm = makeCalendar(20, 30).timeInMillis
        assertEquals("Tonight created at 8:15 PM must trigger at 8:30 PM today", expected830pm, trigger815pm)
    }

    @Test
    fun testBug2_TonightAfter830PM_ReceivesValidFutureAlarm() {
        // 1. At 8:35 PM (after 8:30 PM cutoff): target must be tomorrow night at 8:00 PM
        val cal835pm = makeCalendar(20, 35)
        val trigger835pm = NudgeAlarmScheduler.calculateTriggerMillis("Tonight", "Any time", cal835pm.timeInMillis)
        val expectedTomorrow8pm = makeCalendar(20, 0).apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        assertEquals("Tonight created at 8:35 PM must trigger tomorrow at 8:00 PM", expectedTomorrow8pm, trigger835pm)
        assertTrue("Trigger must be strictly in the future", trigger835pm > cal835pm.timeInMillis)

        // 2. At 9:00 PM: target must be tomorrow night at 8:00 PM
        val cal9pm = makeCalendar(21, 0)
        val trigger9pm = NudgeAlarmScheduler.calculateTriggerMillis("Tonight", "Any time", cal9pm.timeInMillis)
        assertEquals("Tonight created at 9:00 PM must trigger tomorrow at 8:00 PM", expectedTomorrow8pm, trigger9pm)
        assertTrue("Trigger must be strictly in the future", trigger9pm > cal9pm.timeInMillis)

        // 3. Specific time: Tonight at 8:00 PM created at 9:00 PM
        val triggerSpecific = NudgeAlarmScheduler.calculateTriggerMillis("Tonight", "8:00 PM", cal9pm.timeInMillis)
        assertEquals("Tonight at 8:00 PM created at 9:00 PM must trigger tomorrow at 8:00 PM", expectedTomorrow8pm, triggerSpecific)

        // 4. Verify scheduleTask schedules the alarm (does not skip)
        val task = NudgeTask(
            id = 903L,
            title = "Read a chapter tonight",
            dateLabel = "Tonight",
            timeLabel = "Any time",
            createdAt = cal9pm.timeInMillis
        )
        shadowAlarmManager.scheduledAlarms.clear()
        NudgeAlarmScheduler.scheduleTask(context, task, forceRecalculate = true)

        val scheduled = shadowAlarmManager.nextScheduledAlarm
        assertNotNull("Tonight task created after 8:30 PM must receive a scheduled alarm", scheduled)
        assertTrue("Scheduled alarm must be in the future", scheduled!!.triggerAtTime > System.currentTimeMillis())
    }

    @Test
    fun testBug2_UnrelatedDateTimeOptionsRemainUnchanged() {
        val cal9pm = makeCalendar(21, 0)

        // Today with no time specified defaults to 1 hour from now
        val triggerToday = NudgeAlarmScheduler.calculateTriggerMillis("Today", "Any time", cal9pm.timeInMillis)
        assertEquals("Today at 9 PM must default to 1 hour from now", cal9pm.timeInMillis + (60 * 60 * 1000L), triggerToday)

        // Tomorrow defaults to 9:00 AM tomorrow
        val triggerTomorrow = NudgeAlarmScheduler.calculateTriggerMillis("Tomorrow", "Any time", cal9pm.timeInMillis)
        val expectedTomorrow9am = makeCalendar(9, 0).apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        assertEquals("Tomorrow must trigger at 9:00 AM tomorrow", expectedTomorrow9am, triggerTomorrow)

        // Relative time: in 5 mins
        val triggerRelative = NudgeAlarmScheduler.calculateTriggerMillis("Today", "in 5 mins", cal9pm.timeInMillis)
        val expectedIn5Mins = cal9pm.timeInMillis + (5 * 60 * 1000L)
        assertEquals("Relative in 5 mins must be 5 minutes from now", expectedIn5Mins, triggerRelative)
    }
}
