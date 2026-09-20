package com.example.gentlenudge.data.repository

import com.example.gentlenudge.attachment.AttachmentManager
import com.example.gentlenudge.data.db.NudgeTaskDao
import com.example.gentlenudge.data.db.TimeGoalDao
import com.example.gentlenudge.data.model.NudgeTask
import com.example.gentlenudge.data.model.TaskAttachment
import com.example.gentlenudge.data.model.TimeGoal
import com.example.gentlenudge.data.model.TimeGoalMonthColor
import com.example.gentlenudge.data.model.TimeGoalRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class NudgeRepository(
    private val nudgeTaskDao: NudgeTaskDao,
    private val timeGoalDao: TimeGoalDao? = null
) {

    val allTasks: Flow<List<NudgeTask>> = nudgeTaskDao.getAllTasks()
    val todayTasks: Flow<List<NudgeTask>> = nudgeTaskDao.getTodayPendingTasks()
    val laterTasks: Flow<List<NudgeTask>> = nudgeTaskDao.getLaterPendingTasks()
    val completedTasks: Flow<List<NudgeTask>> = nudgeTaskDao.getCompletedTasks()
    val deletedTasks: Flow<List<NudgeTask>> = nudgeTaskDao.getDeletedTasks()
    val deletedTasksCount: Flow<Int> = nudgeTaskDao.getDeletedTasksCount()

    // Time Goals & Records
    val allTimeGoals: Flow<List<TimeGoal>> = timeGoalDao?.getAllGoals() ?: flowOf(emptyList())
    val activeTimeGoals: Flow<List<TimeGoal>> = timeGoalDao?.getActiveGoals() ?: flowOf(emptyList())
    val allTimeGoalRecords: Flow<List<TimeGoalRecord>> = timeGoalDao?.getAllRecordsFlow() ?: flowOf(emptyList())
    val allRecordedMonths: Flow<List<String>> = timeGoalDao?.getAllRecordedMonths() ?: flowOf(emptyList())
    val allMonthColors: Flow<List<TimeGoalMonthColor>> = timeGoalDao?.getAllMonthColorsFlow() ?: flowOf(emptyList())

    fun getRecordsForMonth(monthPrefix: String): Flow<List<TimeGoalRecord>> {
        return timeGoalDao?.getRecordsForMonth(monthPrefix) ?: flowOf(emptyList())
    }

    fun getRecordsForDate(date: String): Flow<List<TimeGoalRecord>> {
        return timeGoalDao?.getRecordsForDate(date) ?: flowOf(emptyList())
    }

    fun getMonthColorsForMonth(yearMonth: String): Flow<List<TimeGoalMonthColor>> {
        return timeGoalDao?.getMonthColorsForMonth(yearMonth) ?: flowOf(emptyList())
    }

    suspend fun getGoalById(id: Long): TimeGoal? = timeGoalDao?.getGoalById(id)

    suspend fun getAllTimeGoalsList(): List<TimeGoal> = timeGoalDao?.getAllGoalsList() ?: emptyList()

    suspend fun getRecordsListForMonth(monthPrefix: String): List<TimeGoalRecord> =
        timeGoalDao?.getRecordsListForMonth(monthPrefix) ?: emptyList()

    suspend fun getAllRecordsList(): List<TimeGoalRecord> = timeGoalDao?.getAllRecordsList() ?: emptyList()

    suspend fun getAllMonthColorsList(): List<TimeGoalMonthColor> = timeGoalDao?.getAllMonthColorsList() ?: emptyList()

    suspend fun insertTimeGoal(goal: TimeGoal): Long = timeGoalDao?.insertGoal(goal) ?: 0L

    suspend fun insertAllGoals(goals: List<TimeGoal>) = timeGoalDao?.insertAllGoals(goals)

    suspend fun clearAllGoals() = timeGoalDao?.clearAllGoals()

    suspend fun updateTimeGoal(goal: TimeGoal) = timeGoalDao?.updateGoal(goal)

    suspend fun insertOrUpdateMonthColor(monthColor: TimeGoalMonthColor): Long =
        timeGoalDao?.insertOrUpdateMonthColor(monthColor) ?: 0L

    suspend fun insertAllMonthColors(monthColors: List<TimeGoalMonthColor>) =
        timeGoalDao?.insertAllMonthColors(monthColors)

    suspend fun deleteTimeGoal(goal: TimeGoal) {
        timeGoalDao?.deleteRecordsForGoal(goal.id)
        timeGoalDao?.deleteMonthColorsForGoal(goal.id)
        timeGoalDao?.deleteGoal(goal)
    }

    suspend fun recordDailyTime(goalId: Long, date: String, minutes: Int, note: String? = null): Long {
        val existing = timeGoalDao?.getRecord(goalId, date)
        val record = existing?.copy(
            actualMinutes = minutes,
            note = note,
            updatedAt = System.currentTimeMillis()
        ) ?: TimeGoalRecord(
            goalId = goalId,
            date = date,
            actualMinutes = minutes,
            note = note,
            updatedAt = System.currentTimeMillis()
        )
        return timeGoalDao?.insertOrUpdateRecord(record) ?: 0L
    }

    suspend fun getAllTasksList(): List<NudgeTask> = nudgeTaskDao.getAllTasksList()

    suspend fun getTaskById(taskId: Long): NudgeTask? = nudgeTaskDao.getTaskById(taskId)

    suspend fun getDeletedTasksList(): List<NudgeTask> = nudgeTaskDao.getDeletedTasksList()

    suspend fun getAllTasksIncludingDeletedList(): List<NudgeTask> = nudgeTaskDao.getAllTasksIncludingDeletedList()

    suspend fun insert(task: NudgeTask): Long = nudgeTaskDao.insertTask(task)

    suspend fun insertAll(tasks: List<NudgeTask>) = nudgeTaskDao.insertAll(tasks)

    suspend fun update(task: NudgeTask) = nudgeTaskDao.updateTask(task)

    suspend fun toggleDone(task: NudgeTask) {
        val updated = task.copy(
            isDone = !task.isDone,
            completedAt = if (!task.isDone) System.currentTimeMillis() else null
        )
        nudgeTaskDao.updateTask(updated)
    }

    suspend fun snoozeTask(task: NudgeTask, newTimeLabel: String) {
        val updated = task.copy(
            timeLabel = newTimeLabel,
            section = "today"
        )
        nudgeTaskDao.updateTask(updated)
    }

    suspend fun softDelete(task: NudgeTask) {
        val updated = task.copy(
            isDeleted = true,
            deletedAt = System.currentTimeMillis()
        )
        nudgeTaskDao.updateTask(updated)
    }

    suspend fun restoreTask(task: NudgeTask) {
        val updated = task.copy(
            isDeleted = false,
            deletedAt = null
        )
        nudgeTaskDao.updateTask(updated)
    }

    suspend fun permanentlyDelete(task: NudgeTask) {
        // Remove associated attachments safely from local storage
        val attachments = TaskAttachment.listFromJson(task.attachmentsJson)
        for (att in attachments) {
            if (att.filePath.isNotBlank()) {
                AttachmentManager.deleteAttachmentFile(att.filePath)
            }
        }
        nudgeTaskDao.deleteTask(task)
    }

    suspend fun permanentlyDeleteAllRecentlyDeleted() {
        val deleted = nudgeTaskDao.getDeletedTasksList()
        for (task in deleted) {
            val attachments = TaskAttachment.listFromJson(task.attachmentsJson)
            for (att in attachments) {
                if (att.filePath.isNotBlank()) {
                    AttachmentManager.deleteAttachmentFile(att.filePath)
                }
            }
        }
        nudgeTaskDao.clearAllDeletedTasks()
    }

    suspend fun cleanupExpiredDeletedTasks(retentionDays: Int = 30): Int {
        val cutoff = System.currentTimeMillis() - (retentionDays.toLong() * 24 * 60 * 60 * 1000L)
        val expired = nudgeTaskDao.getExpiredDeletedTasks(cutoff)
        for (task in expired) {
            val attachments = TaskAttachment.listFromJson(task.attachmentsJson)
            for (att in attachments) {
                if (att.filePath.isNotBlank()) {
                    AttachmentManager.deleteAttachmentFile(att.filePath)
                }
            }
        }
        return nudgeTaskDao.deleteExpiredDeletedTasks(cutoff)
    }

    suspend fun delete(task: NudgeTask) = softDelete(task)

    suspend fun deleteById(id: Long) = nudgeTaskDao.softDeleteTaskById(id, System.currentTimeMillis())

    suspend fun clearAll() = nudgeTaskDao.clearAll()
}
