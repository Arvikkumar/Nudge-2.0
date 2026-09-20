package com.example.gentlenudge.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.gentlenudge.data.model.TimeGoal
import com.example.gentlenudge.data.model.TimeGoalMonthColor
import com.example.gentlenudge.data.model.TimeGoalRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeGoalDao {

    @Query("SELECT * FROM time_goals ORDER BY isArchived ASC, orderIndex ASC, id ASC")
    fun getAllGoals(): Flow<List<TimeGoal>>

    @Query("SELECT * FROM time_goals WHERE isArchived = 0 ORDER BY orderIndex ASC, id ASC")
    fun getActiveGoals(): Flow<List<TimeGoal>>

    @Query("SELECT * FROM time_goals WHERE id = :id LIMIT 1")
    suspend fun getGoalById(id: Long): TimeGoal?

    @Query("SELECT * FROM time_goals ORDER BY isArchived ASC, orderIndex ASC, id ASC")
    suspend fun getAllGoalsList(): List<TimeGoal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: TimeGoal): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllGoals(goals: List<TimeGoal>)

    @Update
    suspend fun updateGoal(goal: TimeGoal)

    @Delete
    suspend fun deleteGoal(goal: TimeGoal)

    @Query("DELETE FROM time_goals WHERE id = :id")
    suspend fun deleteGoalById(id: Long)

    // Records
    @Query("SELECT * FROM time_goal_records WHERE date LIKE :monthPrefix || '%' ORDER BY date ASC")
    fun getRecordsForMonth(monthPrefix: String): Flow<List<TimeGoalRecord>>

    @Query("SELECT * FROM time_goal_records WHERE goalId = :goalId ORDER BY date ASC")
    fun getRecordsForGoal(goalId: Long): Flow<List<TimeGoalRecord>>

    @Query("SELECT * FROM time_goal_records WHERE goalId = :goalId AND date = :date LIMIT 1")
    suspend fun getRecord(goalId: Long, date: String): TimeGoalRecord?

    @Query("SELECT * FROM time_goal_records WHERE date = :date")
    fun getRecordsForDate(date: String): Flow<List<TimeGoalRecord>>

    @Query("SELECT * FROM time_goal_records WHERE date LIKE :monthPrefix || '%'")
    suspend fun getRecordsListForMonth(monthPrefix: String): List<TimeGoalRecord>

    @Query("SELECT * FROM time_goal_records ORDER BY date DESC")
    fun getAllRecordsFlow(): Flow<List<TimeGoalRecord>>

    @Query("SELECT DISTINCT substr(date, 1, 7) FROM time_goal_records WHERE date IS NOT NULL AND date != '' ORDER BY date DESC")
    fun getAllRecordedMonths(): Flow<List<String>>

    @Query("SELECT * FROM time_goal_records")
    suspend fun getAllRecordsList(): List<TimeGoalRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRecord(record: TimeGoalRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRecords(records: List<TimeGoalRecord>)

    @Delete
    suspend fun deleteRecord(record: TimeGoalRecord)

    @Query("DELETE FROM time_goal_records WHERE goalId = :goalId")
    suspend fun deleteRecordsForGoal(goalId: Long)

    @Query("DELETE FROM time_goals")
    suspend fun clearAllGoals()

    @Query("DELETE FROM time_goal_records")
    suspend fun clearAllRecords()

    // Month-by-month Category Colors
    @Query("SELECT * FROM time_goal_month_colors")
    fun getAllMonthColorsFlow(): Flow<List<TimeGoalMonthColor>>

    @Query("SELECT * FROM time_goal_month_colors WHERE yearMonth = :yearMonth")
    fun getMonthColorsForMonth(yearMonth: String): Flow<List<TimeGoalMonthColor>>

    @Query("SELECT * FROM time_goal_month_colors WHERE goalId = :goalId AND yearMonth = :yearMonth LIMIT 1")
    suspend fun getMonthColor(goalId: Long, yearMonth: String): TimeGoalMonthColor?

    @Query("SELECT * FROM time_goal_month_colors")
    suspend fun getAllMonthColorsList(): List<TimeGoalMonthColor>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMonthColor(monthColor: TimeGoalMonthColor): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMonthColors(monthColors: List<TimeGoalMonthColor>)

    @Query("DELETE FROM time_goal_month_colors WHERE goalId = :goalId")
    suspend fun deleteMonthColorsForGoal(goalId: Long)

    @Query("DELETE FROM time_goal_month_colors")
    suspend fun clearAllMonthColors()
}
