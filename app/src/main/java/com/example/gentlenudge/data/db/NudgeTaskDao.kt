package com.example.gentlenudge.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.gentlenudge.data.model.NudgeTask
import kotlinx.coroutines.flow.Flow

@Dao
interface NudgeTaskDao {
    @Query("SELECT * FROM nudge_tasks WHERE isDeleted = 0 ORDER BY isDone ASC, id DESC")
    fun getAllTasks(): Flow<List<NudgeTask>>

    @Query("SELECT * FROM nudge_tasks WHERE section = 'today' AND isDone = 0 AND isDeleted = 0 ORDER BY id ASC")
    fun getTodayPendingTasks(): Flow<List<NudgeTask>>

    @Query("SELECT * FROM nudge_tasks WHERE section = 'later' AND isDone = 0 AND isDeleted = 0 ORDER BY id ASC")
    fun getLaterPendingTasks(): Flow<List<NudgeTask>>

    @Query("SELECT * FROM nudge_tasks WHERE isDone = 1 AND isDeleted = 0 ORDER BY completedAt DESC, id DESC")
    fun getCompletedTasks(): Flow<List<NudgeTask>>

    @Query("SELECT * FROM nudge_tasks WHERE isDeleted = 1 ORDER BY deletedAt DESC, id DESC")
    fun getDeletedTasks(): Flow<List<NudgeTask>>

    @Query("SELECT COUNT(*) FROM nudge_tasks WHERE isDeleted = 1")
    fun getDeletedTasksCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: NudgeTask): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<NudgeTask>)

    @Update
    suspend fun updateTask(task: NudgeTask)

    @Delete
    suspend fun deleteTask(task: NudgeTask)

    @Query("SELECT * FROM nudge_tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): NudgeTask?

    @Query("SELECT * FROM nudge_tasks WHERE isDone = 0 AND isDeleted = 0")
    suspend fun getAllPendingTasksSync(): List<NudgeTask>

    @Query("SELECT * FROM nudge_tasks WHERE isDeleted = 0 ORDER BY isDone ASC, createdAt DESC, id DESC")
    suspend fun getAllTasksList(): List<NudgeTask>

    @Query("SELECT * FROM nudge_tasks WHERE isDeleted = 1 ORDER BY deletedAt DESC, id DESC")
    suspend fun getDeletedTasksList(): List<NudgeTask>

    @Query("SELECT * FROM nudge_tasks ORDER BY id ASC")
    suspend fun getAllTasksIncludingDeletedList(): List<NudgeTask>

    @Query("UPDATE nudge_tasks SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteTaskById(id: Long, deletedAt: Long)

    @Query("UPDATE nudge_tasks SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreTaskById(id: Long)

    @Query("SELECT * FROM nudge_tasks WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt < :cutoffTimestamp")
    suspend fun getExpiredDeletedTasks(cutoffTimestamp: Long): List<NudgeTask>

    @Query("DELETE FROM nudge_tasks WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt < :cutoffTimestamp")
    suspend fun deleteExpiredDeletedTasks(cutoffTimestamp: Long): Int

    @Query("DELETE FROM nudge_tasks WHERE isDeleted = 1")
    suspend fun clearAllDeletedTasks()

    @Query("DELETE FROM nudge_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM nudge_tasks")
    suspend fun clearAll()
}
