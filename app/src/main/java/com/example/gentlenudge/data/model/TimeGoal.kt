package com.example.gentlenudge.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_goals")
data class TimeGoal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dailyTargetMinutes: Int, // e.g. 120 = 2 hours
    val colorHex: String = "#7C4DFF", // Category accent color
    val iconName: String = "book", // Icon identifier
    val startDate: String, // "YYYY-MM-DD" e.g. "2026-08-01"
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val orderIndex: Int = 0
) {
    fun parseColor(): Color {
        return try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (_: Exception) {
            Color(0xFF7C4DFF)
        }
    }

    fun getIcon(): ImageVector {
        return when (iconName.lowercase()) {
            "book", "reading" -> Icons.Outlined.MenuBook
            "code", "coding" -> Icons.Outlined.Code
            "school", "course", "study" -> Icons.Outlined.School
            "fitness", "exercise", "workout" -> Icons.Outlined.FitnessCenter
            "meditation", "self_improvement", "mind" -> Icons.Outlined.SelfImprovement
            "language", "english" -> Icons.Outlined.Language
            "music", "instrument" -> Icons.Outlined.MusicNote
            "work", "project" -> Icons.Outlined.WorkOutline
            "art", "brush", "creative" -> Icons.Outlined.Brush
            "story", "writing" -> Icons.Outlined.AutoStories
            "run", "running" -> Icons.Outlined.DirectionsRun
            "walk", "walking" -> Icons.Outlined.DirectionsWalk
            "bike", "cycling" -> Icons.Outlined.DirectionsBike
            "swim", "swimming" -> Icons.Outlined.Pool
            "spa", "wellness" -> Icons.Outlined.Spa
            "psychology", "brain", "deep_thinking" -> Icons.Outlined.Psychology
            "lightbulb", "idea" -> Icons.Outlined.Lightbulb
            "journal", "notes", "writing_notes" -> Icons.Outlined.EditNote
            "terminal", "dev" -> Icons.Outlined.Terminal
            "math", "calculate" -> Icons.Outlined.Calculate
            "translate" -> Icons.Outlined.Translate
            "headphones", "podcast" -> Icons.Outlined.Headphones
            "mic", "speech" -> Icons.Outlined.Mic
            "camera", "photo" -> Icons.Outlined.CameraAlt
            "video", "film" -> Icons.Outlined.Videocam
            "palette", "design" -> Icons.Outlined.Palette
            "finance", "savings" -> Icons.Outlined.Savings
            "timer", "pomodoro" -> Icons.Outlined.Timer
            "heart", "health" -> Icons.Outlined.Favorite
            "community", "social" -> Icons.Outlined.Groups
            "nature", "outdoor" -> Icons.Outlined.Park
            "coffee", "morning" -> Icons.Outlined.LocalCafe
            "sleep", "rest" -> Icons.Outlined.Bedtime
            "home", "clean" -> Icons.Outlined.CleaningServices
            "trophy", "milestone" -> Icons.Outlined.EmojiEvents
            "flag", "target" -> Icons.Outlined.Flag
            "rocket", "launch" -> Icons.Outlined.RocketLaunch
            else -> Icons.Outlined.Star
        }
    }

    companion object {
        val AVAILABLE_ICONS = listOf(
            "book" to "Reading",
            "code" to "Coding",
            "school" to "Study",
            "fitness" to "Exercise",
            "meditation" to "Meditation",
            "language" to "Languages",
            "music" to "Music",
            "work" to "Focus",
            "art" to "Creative",
            "star" to "General"
        )

        val ALL_MORE_ICONS = listOf(
            "Health & Fitness" to listOf(
                "fitness" to "Workout",
                "run" to "Running",
                "walk" to "Walking",
                "bike" to "Cycling",
                "swim" to "Swimming",
                "heart" to "Health",
                "sleep" to "Sleep & Rest"
            ),
            "Mind & Focus" to listOf(
                "meditation" to "Meditation",
                "spa" to "Wellness",
                "psychology" to "Deep Thinking",
                "timer" to "Timer & Focus",
                "coffee" to "Morning Routine"
            ),
            "Learning & Craft" to listOf(
                "book" to "Reading",
                "school" to "Study & Courses",
                "code" to "Coding & Dev",
                "terminal" to "Tech & Terminal",
                "language" to "Languages",
                "translate" to "Translation",
                "journal" to "Journaling",
                "math" to "Math & Logic"
            ),
            "Creative & Arts" to listOf(
                "art" to "Art & Painting",
                "palette" to "Design & Color",
                "music" to "Music & Sound",
                "headphones" to "Audio & Podcasts",
                "camera" to "Photography",
                "video" to "Film & Video",
                "mic" to "Speaking & Voice",
                "story" to "Creative Writing"
            ),
            "Work & Life" to listOf(
                "work" to "Deep Work",
                "rocket" to "Projects & Launch",
                "flag" to "Milestones",
                "trophy" to "Achievements",
                "lightbulb" to "Ideas & Innovation",
                "finance" to "Finance & Budget",
                "nature" to "Outdoors & Nature",
                "community" to "Family & Social",
                "home" to "Home & Organization",
                "star" to "Essential Pursuit"
            )
        )
    }
}

/**
 * Returns a copy of the TimeGoal with the color configured for the specific year-month ("yyyy-MM"),
 * falling back to the goal's base colorHex if no month-specific override exists.
 */
fun TimeGoal.withMonthColor(
    yearMonthStr: String,
    monthColorsMap: Map<Pair<Long, String>, String>
): TimeGoal {
    val ym = if (yearMonthStr.length >= 7) yearMonthStr.substring(0, 7) else yearMonthStr
    val monthColor = monthColorsMap[Pair(id, ym)] ?: this.colorHex
    return if (this.colorHex == monthColor) this else this.copy(colorHex = monthColor)
}

fun TimeGoal.withMonthColor(
    yearMonth: java.time.YearMonth,
    monthColorsMap: Map<Pair<Long, String>, String>
): TimeGoal {
    return withMonthColor(yearMonth.toString(), monthColorsMap)
}

/**
 * Returns a list of TimeGoals with colors mapped to the specified year-month.
 */
fun List<TimeGoal>.withMonthColors(
    yearMonth: java.time.YearMonth,
    monthColorsMap: Map<Pair<Long, String>, String>
): List<TimeGoal> {
    val ymStr = yearMonth.toString()
    return map { it.withMonthColor(ymStr, monthColorsMap) }
}

fun List<TimeGoal>.withMonthColors(
    yearMonthStr: String,
    monthColorsMap: Map<Pair<Long, String>, String>
): List<TimeGoal> {
    return map { it.withMonthColor(yearMonthStr, monthColorsMap) }
}

