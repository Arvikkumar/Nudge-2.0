package com.example.gentlenudge

import com.example.gentlenudge.data.model.TimeGoal
import com.example.gentlenudge.data.model.TimeGoalCalculations
import com.example.gentlenudge.data.model.TimeGoalRecord
import com.example.gentlenudge.data.model.withMonthColor
import com.example.gentlenudge.data.model.withMonthColors
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeGoalStatusTest {

    @Test
    fun testFormatDurationWords_exactHours() {
        assertEquals("1 hour", TimeGoalCalculations.formatDurationWords(60))
        assertEquals("2 hours", TimeGoalCalculations.formatDurationWords(120))
        assertEquals("3 hours", TimeGoalCalculations.formatDurationWords(180))
    }

    @Test
    fun testFormatDurationWords_minutesOnly() {
        assertEquals("1 minute", TimeGoalCalculations.formatDurationWords(1))
        assertEquals("30 minutes", TimeGoalCalculations.formatDurationWords(30))
        assertEquals("45 minutes", TimeGoalCalculations.formatDurationWords(45))
        assertEquals("0 minutes", TimeGoalCalculations.formatDurationWords(0))
    }

    @Test
    fun testFormatDurationWords_compoundHoursAndMinutes() {
        assertEquals("1 hour 30 minutes", TimeGoalCalculations.formatDurationWords(90))
        assertEquals("2 hours 15 minutes", TimeGoalCalculations.formatDurationWords(135))
        assertEquals("1 hour 1 minute", TimeGoalCalculations.formatDurationWords(61))
    }

    @Test
    fun testStatusStates() {
        // State 1: Partial / Pending
        val actual1 = 60
        val target1 = 120
        val isDone1 = actual1 >= target1 && target1 > 0
        val rem1 = (target1 - actual1).coerceAtLeast(0)
        val status1 = when {
            isDone1 -> "✓ Complete"
            actual1 > 0 -> "Pending · ${TimeGoalCalculations.formatDurationWords(rem1)} remaining"
            else -> "Not Started · ${TimeGoalCalculations.formatDurationWords(target1)} remaining"
        }
        assertEquals("Pending · 1 hour remaining", status1)

        // State 2: Complete
        val actual2 = 120
        val target2 = 120
        val isDone2 = actual2 >= target2 && target2 > 0
        val rem2 = (target2 - actual2).coerceAtLeast(0)
        val status2 = when {
            isDone2 -> "✓ Complete"
            actual2 > 0 -> "Pending · ${TimeGoalCalculations.formatDurationWords(rem2)} remaining"
            else -> "Not Started · ${TimeGoalCalculations.formatDurationWords(target2)} remaining"
        }
        assertEquals("✓ Complete", status2)

        // State 3: Not Started
        val actual3 = 0
        val target3 = 120
        val isDone3 = actual3 >= target3 && target3 > 0
        val rem3 = (target3 - actual3).coerceAtLeast(0)
        val status3 = when {
            isDone3 -> "✓ Complete"
            actual3 > 0 -> "Pending · ${TimeGoalCalculations.formatDurationWords(rem3)} remaining"
            else -> "Not Started · ${TimeGoalCalculations.formatDurationWords(target3)} remaining"
        }
        assertEquals("Not Started · 2 hours remaining", status3)
    }

    @Test
    fun testReorder_moveFirstCardToLastPosition() {
        val list = mutableListOf("Reading", "Articulation", "Exercise", "Course", "Sleep")
        val fromIdx = 0
        val toIdx = TimeGoalCalculations.calculateDropIndex(
            fromIndex = fromIdx,
            dragOffsetY = 1000f,
            itemCount = list.size,
            defaultSlotHeightPx = 172f
        )
        assertEquals(4, toIdx)
        val item = list.removeAt(fromIdx)
        list.add(toIdx, item)
        assertEquals(listOf("Articulation", "Exercise", "Course", "Sleep", "Reading"), list)
    }

    @Test
    fun testReorder_moveLastCardToFirstPosition() {
        val list = mutableListOf("Reading", "Articulation", "Exercise", "Course", "Sleep")
        val fromIdx = 4
        val toIdx = TimeGoalCalculations.calculateDropIndex(
            fromIndex = fromIdx,
            dragOffsetY = -1000f,
            itemCount = list.size,
            defaultSlotHeightPx = 172f
        )
        assertEquals(0, toIdx)
        val item = list.removeAt(fromIdx)
        list.add(toIdx, item)
        assertEquals(listOf("Sleep", "Reading", "Articulation", "Exercise", "Course"), list)
    }

    @Test
    fun testReorder_moveArticulationAboveReading() {
        val list = mutableListOf("Reading", "Articulation", "Exercise", "Course", "Sleep")
        val fromIdx = 1
        val toIdx = TimeGoalCalculations.calculateDropIndex(
            fromIndex = fromIdx,
            dragOffsetY = -120f, // threshold is 86f
            itemCount = list.size,
            defaultSlotHeightPx = 172f
        )
        assertEquals(0, toIdx)
        val item = list.removeAt(fromIdx)
        list.add(toIdx, item)
        assertEquals(listOf("Articulation", "Reading", "Exercise", "Course", "Sleep"), list)
    }

    @Test
    fun testReorder_moveSleepBetweenReadingAndExercise() {
        val list = mutableListOf("Reading", "Articulation", "Exercise", "Course", "Sleep")
        // Drag Sleep (index 4) up to between Reading and Articulation/Exercise (index 1)
        val fromIdx = 4
        val toIdx = TimeGoalCalculations.calculateDropIndex(
            fromIndex = fromIdx,
            dragOffsetY = -520f, // moves past 3 slots up
            itemCount = list.size,
            defaultSlotHeightPx = 172f
        )
        assertEquals(1, toIdx)
        val item = list.removeAt(fromIdx)
        list.add(toIdx, item)
        assertEquals(listOf("Reading", "Sleep", "Articulation", "Exercise", "Course"), list)
    }

    @Test
    fun testReorder_moveMiddleCardOnePositionDown() {
        val list = mutableListOf("Reading", "Articulation", "Exercise", "Course", "Sleep")
        val fromIdx = 2 // Exercise
        val toIdx = TimeGoalCalculations.calculateDropIndex(
            fromIndex = fromIdx,
            dragOffsetY = 100f,
            itemCount = list.size,
            defaultSlotHeightPx = 172f
        )
        assertEquals(3, toIdx)
        val item = list.removeAt(fromIdx)
        list.add(toIdx, item)
        assertEquals(listOf("Reading", "Articulation", "Course", "Exercise", "Sleep"), list)
    }

    @Test
    fun testCategoryNotesMappingForDate() {
        val goals = listOf(
            com.example.gentlenudge.data.model.TimeGoal(id = 1, name = "Reading", dailyTargetMinutes = 60, colorHex = "#7C4DFF", startDate = "2026-08-01"),
            com.example.gentlenudge.data.model.TimeGoal(id = 2, name = "Sleep", dailyTargetMinutes = 480, colorHex = "#00897B", startDate = "2026-08-01"),
            com.example.gentlenudge.data.model.TimeGoal(id = 3, name = "Course", dailyTargetMinutes = 90, colorHex = "#1E88E5", startDate = "2026-08-01")
        )

        val records = listOf(
            com.example.gentlenudge.data.model.TimeGoalRecord(id = 101, goalId = 1, date = "2026-09-15", actualMinutes = 45, note = "Finished Chapter 4"),
            com.example.gentlenudge.data.model.TimeGoalRecord(id = 102, goalId = 2, date = "2026-09-15", actualMinutes = 480, note = "Deep restful sleep"),
            com.example.gentlenudge.data.model.TimeGoalRecord(id = 103, goalId = 3, date = "2026-09-15", actualMinutes = 60, note = ""),
            com.example.gentlenudge.data.model.TimeGoalRecord(id = 104, goalId = 1, date = "2026-09-16", actualMinutes = 60, note = "Started Chapter 5")
        )

        // Filter notes for 2026-09-15
        val dateStr15 = "2026-09-15"
        val notes15 = records.filter { it.date == dateStr15 && !it.note.isNullOrBlank() }
        assertEquals(2, notes15.size)

        val goalsWithNotes15 = notes15.mapNotNull { rec -> goals.firstOrNull { it.id == rec.goalId } }
        assertEquals(listOf("Reading", "Sleep"), goalsWithNotes15.map { it.name })
        assertEquals(listOf("#7C4DFF", "#00897B"), goalsWithNotes15.map { it.colorHex })

        // Filter notes for 2026-09-16
        val dateStr16 = "2026-09-16"
        val notes16 = records.filter { it.date == dateStr16 && !it.note.isNullOrBlank() }
        assertEquals(1, notes16.size)
        assertEquals("Reading", goals.firstOrNull { it.id == notes16[0].goalId }?.name)
    }

    @Test
    fun testMonthByMonthCategoryColorIsolation() {
        val readingGoal = com.example.gentlenudge.data.model.TimeGoal(
            id = 1,
            name = "Reading",
            dailyTargetMinutes = 60,
            colorHex = "#FF9800", // Default orange
            startDate = "2026-08-01"
        )

        // Month color overrides map: (goalId, yearMonth) -> colorHex
        val monthColorsMap = mutableMapOf<Pair<Long, String>, String>(
            Pair(1L, "2026-08") to "#FF9800", // August 2026: Orange
            Pair(1L, "2026-09") to "#1E88E5", // September 2026: Blue
            Pair(1L, "2026-10") to "#7C4DFF"  // October 2026: Purple
        )

        // Verify initial month colors
        val augGoal = readingGoal.withMonthColor("2026-08", monthColorsMap)
        val sepGoal = readingGoal.withMonthColor("2026-09", monthColorsMap)
        val octGoal = readingGoal.withMonthColor("2026-10", monthColorsMap)

        assertEquals("#FF9800", augGoal.colorHex) // Orange
        assertEquals("#1E88E5", sepGoal.colorHex) // Blue
        assertEquals("#7C4DFF", octGoal.colorHex) // Purple

        // Now user updates Reading's color in September 2026 to Green (#00897B)
        monthColorsMap[Pair(1L, "2026-09")] = "#00897B"

        val augGoalAfter = readingGoal.withMonthColor("2026-08", monthColorsMap)
        val sepGoalAfter = readingGoal.withMonthColor("2026-09", monthColorsMap)
        val octGoalAfter = readingGoal.withMonthColor("2026-10", monthColorsMap)

        // September 2026 updated to Green
        assertEquals("#00897B", sepGoalAfter.colorHex)

        // August 2026 MUST remain Orange
        assertEquals("#FF9800", augGoalAfter.colorHex)

        // October 2026 MUST remain Purple
        assertEquals("#7C4DFF", octGoalAfter.colorHex)

        // November 2026 (no override) falls back to base goal colorHex (#FF9800)
        val novGoal = readingGoal.withMonthColor("2026-11", monthColorsMap)
        assertEquals("#FF9800", novGoal.colorHex)
    }

    @Test
    fun testGoalListWithMonthColors_bulkMapping() {
        val goals = listOf(
            TimeGoal(id = 1, name = "Reading", dailyTargetMinutes = 60, colorHex = "#FF9800", startDate = "2026-08-01"),
            TimeGoal(id = 2, name = "Sleep", dailyTargetMinutes = 480, colorHex = "#00897B", startDate = "2026-08-01")
        )

        val monthColorsMap = mapOf(
            Pair(1L, "2026-09") to "#1E88E5" // Reading is blue in September
        )

        val resolvedForSep = goals.withMonthColors("2026-09", monthColorsMap)
        assertEquals("#1E88E5", resolvedForSep[0].colorHex) // Reading became blue
        assertEquals("#00897B", resolvedForSep[1].colorHex) // Sleep remained green

        val resolvedForAug = goals.withMonthColors("2026-08", monthColorsMap)
        assertEquals("#FF9800", resolvedForAug[0].colorHex) // Reading remained orange in Aug
        assertEquals("#00897B", resolvedForAug[1].colorHex) // Sleep remained green in Aug
    }

    @Test
    fun testReorder_todayCardsWithMeasuredHeights() {
        val list = mutableListOf("Articulation", "Exercise", "Course", "Reading", "Sleep")
        val itemHeights = mapOf(
            0 to 76f,
            1 to 76f,
            2 to 76f,
            3 to 76f,
            4 to 76f
        )

        // Drag "Exercise" (index 1) downward past Course and Reading
        val fromIdx = 1
        val toIdx = TimeGoalCalculations.calculateDropIndex(
            fromIndex = fromIdx,
            dragOffsetY = 160f,
            itemCount = list.size,
            itemHeights = itemHeights,
            defaultSlotHeightPx = 76f
        )
        assertEquals(3, toIdx) // target is index 3 (after Reading)
        val item = list.removeAt(fromIdx)
        list.add(toIdx, item)
        assertEquals(listOf("Articulation", "Course", "Reading", "Exercise", "Sleep"), list)

        // Drag "Sleep" (index 4) upward to the top
        val fromIdx2 = 4
        val toIdx2 = TimeGoalCalculations.calculateDropIndex(
            fromIndex = fromIdx2,
            dragOffsetY = -400f,
            itemCount = list.size,
            itemHeights = itemHeights,
            defaultSlotHeightPx = 76f
        )
        assertEquals(0, toIdx2)
        val item2 = list.removeAt(fromIdx2)
        list.add(toIdx2, item2)
        assertEquals(listOf("Sleep", "Articulation", "Course", "Reading", "Exercise"), list)
    }

    @Test
    fun testTimeEntryDurationPresets_exactDurationAndNoAccumulation() {
        // Presets available in DailyTimeEntrySheet
        val presets = listOf(
            120 to "Target (2h 00m)",
            15 to "+15m",
            30 to "+30m",
            45 to "+45m",
            60 to "1 hour",
            90 to "1h 30m",
            120 to "2 hours",
            180 to "3 hours",
            0 to "Clear (0m)"
        )

        var minutesLogged = 0

        // Tapping +15m should set duration to exactly 15 minutes
        val preset15 = presets.first { it.first == 15 }
        minutesLogged = preset15.first
        assertEquals(15, minutesLogged)
        assertEquals("15m", TimeGoalCalculations.formatMinutes(minutesLogged))

        // Repeatedly tapping +15m must NOT accumulate; must remain exactly 15 minutes
        minutesLogged = preset15.first
        assertEquals(15, minutesLogged)
        assertEquals("15m", TimeGoalCalculations.formatMinutes(minutesLogged))

        // Tapping +30m sets duration to exactly 30 minutes
        val preset30 = presets.first { it.first == 30 }
        minutesLogged = preset30.first
        assertEquals(30, minutesLogged)
        assertEquals("30m", TimeGoalCalculations.formatMinutes(minutesLogged))

        // Repeatedly tapping +30m remains 30 minutes
        minutesLogged = preset30.first
        assertEquals(30, minutesLogged)

        // Tapping +45m sets duration to exactly 45 minutes
        val preset45 = presets.first { it.first == 45 }
        minutesLogged = preset45.first
        assertEquals(45, minutesLogged)
        assertEquals("45m", TimeGoalCalculations.formatMinutes(minutesLogged))

        // Tapping 1 hour sets duration to exactly 60 minutes
        val preset60 = presets.first { it.first == 60 }
        minutesLogged = preset60.first
        assertEquals(60, minutesLogged)
        assertEquals("1h", TimeGoalCalculations.formatMinutes(minutesLogged))

        // Repeatedly tapping 1 hour remains 60 minutes
        minutesLogged = preset60.first
        assertEquals(60, minutesLogged)

        // Tapping Clear (0m) sets to 0 minutes
        val presetClear = presets.first { it.first == 0 }
        minutesLogged = presetClear.first
        assertEquals(0, minutesLogged)
        assertEquals("0m", TimeGoalCalculations.formatMinutes(minutesLogged))
    }
}
