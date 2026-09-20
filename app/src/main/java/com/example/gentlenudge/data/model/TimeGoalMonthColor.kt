package com.example.gentlenudge.data.model

import androidx.room.Entity
import androidx.room.Index

/**
 * Persists month-by-month category accent colors.
 * Associates a specific pursuit/category (goalId) with a specific month/year (yearMonth: "yyyy-MM").
 */
@Entity(
    tableName = "time_goal_month_colors",
    primaryKeys = ["goalId", "yearMonth"],
    indices = [Index(value = ["yearMonth"])]
)
data class TimeGoalMonthColor(
    val goalId: Long,
    val yearMonth: String, // "YYYY-MM", e.g. "2026-08", "2026-09"
    val colorHex: String,
    val updatedAt: Long = System.currentTimeMillis()
)
