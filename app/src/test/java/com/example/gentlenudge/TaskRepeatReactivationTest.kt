package com.example.gentlenudge

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.gentlenudge.data.db.AppDatabase
import com.example.gentlenudge.data.model.NudgeTask
import com.example.gentlenudge.data.repository.NudgeRepository
import com.example.gentlenudge.notification.NudgeNotificationReceiver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskRepeatReactivationTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: NudgeRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NudgeRepository(database.nudgeTaskDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun isRepeatingRule(repeat: String?): Boolean {
        if (repeat.isNullOrBlank()) return false
        val trimmed = repeat.trim()
        return !trimmed.equals("Does not repeat", ignoreCase = true) && !trimmed.equals("Once", ignoreCase = true)
    }

    @Test
    fun testTest1_EveryDayRecurrenceCompletion_LeavesActiveAndAppearsInCompletedWithoutDuplicate() = runBlocking {
        // Step 1: Create active recurring nudge
        val id = repository.insert(
            NudgeTask(
                title = "alo",
                dateLabel = "Today",
                timeLabel = "7:32 pm",
                repeat = "Every day",
                category = "Personal",
                priority = "Important",
                soundType = "Small nudge",
                isDone = false,
                section = "today"
            )
        )
        val initialActive = repository.allTasks.first().filter { !it.isDone }
        assertEquals("Should have exactly 1 active task", 1, initialActive.size)
        val initialTask = initialActive.first()
        assertEquals("alo", initialTask.title)
        assertEquals("Every day", initialTask.repeat)

        // Step 2: User taps completion checkbox
        repository.toggleDone(initialTask)

        // Step 3: Verify current occurrence leaves ACTIVE
        val activeTasksAfter = repository.allTasks.first().filter { !it.isDone }
        val todayTasksAfter = repository.todayTasks.first()
        assertTrue("ACTIVE list must be empty (current occurrence left ACTIVE)", activeTasksAfter.isEmpty())
        assertTrue("Today tasks list must be empty", todayTasksAfter.isEmpty())

        // Step 4: Verify it appears once in COMPLETED
        val completedTasksAfter = repository.completedTasks.first()
        assertEquals("COMPLETED list must contain exactly 1 task", 1, completedTasksAfter.size)
        val completedTask = completedTasksAfter.first()
        assertEquals("alo", completedTask.title)
        assertEquals("7:32 pm", completedTask.timeLabel)
        assertEquals("Today", completedTask.dateLabel)
        assertEquals("Every day", completedTask.repeat)
        assertEquals("Personal", completedTask.category)
        assertEquals("Important", completedTask.priority)
        assertTrue("isDone must be true", completedTask.isDone)
        assertTrue("completedAt must be set", completedTask.completedAt != null)

        // Step 5: Verify total tasks count is exactly 1 (no duplicate row created)
        val allTasksTotal = repository.allTasks.first()
        assertEquals("Total tasks must be exactly 1", 1, allTasksTotal.size)
    }

    @Test
    fun testTest2_NonRecurringOnceCompletion_LeavesActiveAndAppearsInCompleted() = runBlocking {
        val id = repository.insert(
            NudgeTask(
                title = "Call doctor",
                dateLabel = "Today",
                timeLabel = "2:00 PM",
                repeat = "Does not repeat",
                category = "Health",
                priority = "Normal",
                isDone = false,
                section = "today"
            )
        )
        val task = database.nudgeTaskDao().getTaskById(id)!!
        assertFalse(task.isDone)

        repository.toggleDone(task)

        val activeTasks = repository.allTasks.first().filter { !it.isDone }
        val completedTasks = repository.completedTasks.first()
        assertTrue("Active tasks must be empty", activeTasks.isEmpty())
        assertEquals("Completed tasks must have 1 task", 1, completedTasks.size)
        assertEquals("Call doctor", completedTasks.first().title)
        assertEquals("Does not repeat", completedTasks.first().repeat)
    }

    @Test
    fun testTest3_OtherRecurrenceCompletion_WeeklyPreservedWithoutDuplicate() = runBlocking {
        val id = repository.insert(
            NudgeTask(
                title = "Weekly review",
                dateLabel = "Today",
                timeLabel = "9:00 AM",
                repeat = "Every week",
                category = "Work",
                priority = "Normal",
                isDone = false,
                section = "today"
            )
        )
        val task = database.nudgeTaskDao().getTaskById(id)!!
        assertFalse(task.isDone)

        repository.toggleDone(task)

        val activeTasks = repository.allTasks.first().filter { !it.isDone }
        val completedTasks = repository.completedTasks.first()
        assertTrue("Active tasks must be empty", activeTasks.isEmpty())
        assertEquals("Completed tasks must have 1 task", 1, completedTasks.size)
        assertEquals("Weekly review", completedTasks.first().title)
        assertEquals("Every week", completedTasks.first().repeat)
    }

    @Test
    fun testTest4_ExistingReactivationFix_ThenCompleteMovesToCompletedWithoutDuplicate() = runBlocking {
        // Step 1: Completed task
        val id = repository.insert(
            NudgeTask(
                title = "Meditate",
                dateLabel = "Today",
                timeLabel = "7:00 AM",
                repeat = "Does not repeat",
                isDone = true,
                completedAt = 1000L,
                section = "today"
            )
        )
        val saved = database.nudgeTaskDao().getTaskById(id)!!
        assertTrue(saved.isDone)

        // Step 2: Edit and change repeat to "Every day"
        val isRepeatingOption = isRepeatingRule("Every day")
        val wasDoneAndNowRepeating = saved.isDone && isRepeatingOption
        val newIsDone = if (wasDoneAndNowRepeating) false else saved.isDone
        val newCompletedAt = if (!newIsDone) null else saved.completedAt

        val updatedTask = saved.copy(
            repeat = "Every day",
            isDone = newIsDone,
            completedAt = newCompletedAt
        )
        repository.update(updatedTask)

        // Verify it is active
        val reactivated = database.nudgeTaskDao().getTaskById(id)!!
        assertFalse("Reactivated task must be active", reactivated.isDone)
        assertEquals("Every day", reactivated.repeat)

        // Step 3: Complete it with checkbox
        repository.toggleDone(reactivated)

        // Verify it moves to COMPLETED and does not remain in ACTIVE
        val activeTasks = repository.allTasks.first().filter { !it.isDone }
        val completedTasks = repository.completedTasks.first()
        assertTrue("ACTIVE list must be empty", activeTasks.isEmpty())
        assertEquals("COMPLETED list must have 1 item", 1, completedTasks.size)
        assertEquals("Meditate", completedTasks.first().title)
        assertEquals("Every day", completedTasks.first().repeat)
        assertTrue(completedTasks.first().isDone)
    }

    @Test
    fun testKeepCompletedWhenRepeatNotChangedToRepeating() = runBlocking {
        // 1. Create a completed task
        val completedTask = NudgeTask(
            id = 2,
            title = "Buy groceries",
            dateLabel = "Today",
            timeLabel = "5:00 PM",
            repeat = "Does not repeat",
            isDone = true,
            completedAt = 123456789L,
            section = "today"
        )
        repository.insert(completedTask)

        val saved = repository.allTasks.first().first { it.id == 2L }
        assertTrue(saved.isDone)

        // 2. User edits title only, repeat remains "Does not repeat"
        val isRepeatingOption = isRepeatingRule("Does not repeat")
        val wasDoneAndNowRepeating = saved.isDone && isRepeatingOption
        val newIsDone = if (wasDoneAndNowRepeating) false else saved.isDone
        val newCompletedAt = if (!newIsDone) null else saved.completedAt

        val updatedTask = saved.copy(
            title = "Buy groceries and fruit",
            repeat = "Does not repeat",
            isDone = newIsDone,
            completedAt = newCompletedAt
        )
        repository.update(updatedTask)

        // 3. Verify task remains completed
        val afterSaved = repository.allTasks.first().first { it.id == 2L }
        assertTrue("Task should remain completed", afterSaved.isDone)
        assertEquals(123456789L, afterSaved.completedAt)
        assertEquals("Buy groceries and fruit", afterSaved.title)
    }

    @Test
    fun testCustomRecurrenceEvery2DaysNextOccurrenceCalculation() {
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 10, 14, 0, 0)
        }
        val nextCal = NudgeNotificationReceiver.calculateNextCalendar(baseCal, "Every 2 days")
        assertNotNull(nextCal)
        assertEquals(2026, nextCal!!.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, nextCal.get(Calendar.MONTH))
        assertEquals(12, nextCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(14, nextCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, nextCal.get(Calendar.MINUTE))
    }

    @Test
    fun testCustomRecurrenceEvery3WeeksNextOccurrenceCalculation() {
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 1, 9, 30, 0)
        }
        val nextCal = NudgeNotificationReceiver.calculateNextCalendar(baseCal, "Every 3 weeks")
        assertNotNull(nextCal)
        assertEquals(2026, nextCal!!.get(Calendar.YEAR))
        assertEquals(Calendar.MAY, nextCal.get(Calendar.MONTH))
        assertEquals(22, nextCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testCustomRecurrenceMonthBoundaryAndLeapYear() {
        // Leap year: Feb 28, 2028 + 2 days -> Mar 1, 2028
        val leapCal = Calendar.getInstance().apply {
            set(2028, Calendar.FEBRUARY, 28, 10, 0, 0)
        }
        val nextLeap = NudgeNotificationReceiver.calculateNextCalendar(leapCal, "Every 2 days")
        assertNotNull(nextLeap)
        assertEquals(2028, nextLeap!!.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, nextLeap.get(Calendar.MONTH))
        assertEquals(1, nextLeap.get(Calendar.DAY_OF_MONTH))

        // Non-leap year: Feb 28, 2027 + 2 days -> Mar 2, 2027
        val nonLeapCal = Calendar.getInstance().apply {
            set(2027, Calendar.FEBRUARY, 28, 10, 0, 0)
        }
        val nextNonLeap = NudgeNotificationReceiver.calculateNextCalendar(nonLeapCal, "Every 2 days")
        assertNotNull(nextNonLeap)
        assertEquals(2027, nextNonLeap!!.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, nextNonLeap.get(Calendar.MONTH))
        assertEquals(2, nextNonLeap.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testCustomRecurrenceYearBoundary() {
        val endOfYearCal = Calendar.getInstance().apply {
            set(2026, Calendar.DECEMBER, 30, 8, 0, 0)
        }
        val nextCal = NudgeNotificationReceiver.calculateNextCalendar(endOfYearCal, "Every 5 days")
        assertNotNull(nextCal)
        assertEquals(2027, nextCal!!.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, nextCal.get(Calendar.MONTH))
        assertEquals(4, nextCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testCalculateNextOccurrenceTaskAdvancesCustomIntervalAndPersistsCorrectly() = runBlocking {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val testYear = 2026
        val expectedLabel = if (testYear == currentYear) "Aug 13" else "Aug 13, 2026"

        val task = NudgeTask(
            id = 50L,
            title = "Water the plants",
            dateLabel = "Aug 10, $testYear",
            timeLabel = "10:00 AM",
            repeat = "Every 3 days",
            category = "Personal",
            section = "later"
        )
        repository.insert(task)

        val nextTask = NudgeNotificationReceiver.calculateNextOccurrenceTask(task)
        assertNotNull("Next occurrence task should be computed", nextTask)
        assertEquals("Water the plants", nextTask!!.title)
        assertEquals("Every 3 days", nextTask.repeat)
        assertEquals("10:00 AM", nextTask.timeLabel)
        assertEquals(expectedLabel, nextTask.dateLabel)
        assertEquals("later", nextTask.section)
        assertFalse(nextTask.isDone)

        // Persist the next occurrence in repository (as done in NudgeNotificationReceiver)
        repository.update(nextTask)

        val savedInDb = repository.allTasks.first().first { it.id == 50L }
        assertEquals(expectedLabel, savedInDb.dateLabel)
        assertEquals("Every 3 days", savedInDb.repeat)
        assertFalse(savedInDb.isDone)
    }
}
