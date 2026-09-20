package com.example.gentlenudge.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "time_goal_records",
    indices = [
        Index(value = ["goalId", "date"], unique = true),
        Index(value = ["date"])
    ]
)
data class TimeGoalRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goalId: Long,
    val date: String, // "YYYY-MM-DD" e.g. "2026-08-25"
    val actualMinutes: Int, // e.g. 90 = 1h 30m
    val note: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
