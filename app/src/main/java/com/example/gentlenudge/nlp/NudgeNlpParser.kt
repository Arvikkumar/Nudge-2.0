package com.example.gentlenudge.nlp

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ParsedNudge(
    val cleanTitle: String,
    val extractedDate: String,
    val extractedTime: String,
    val extractedCategory: String,
    val extractedPriority: String, // "Normal" or "Important"
    val extractedRepeat: String, // "Does not repeat", "Every day", "Every week", "Every month"
    val confidence: Float = 1.0f,
    val hasExplicitDateTime: Boolean = false,
    val originalText: String = "",
    val isAmbiguousAmPm: Boolean = false,
    val alternativeTime: String? = null,
    val confirmationPrompt: String? = null
) {
    // Convenient aliases
    val title: String get() = cleanTitle
    val dateLabel: String get() = extractedDate
    val timeLabel: String get() = extractedTime
    val category: String get() = extractedCategory
    val priority: String get() = extractedPriority
    val repeat: String get() = extractedRepeat
    val hasExplicitDateOrTime: Boolean get() = hasExplicitDateTime
}

object NudgeNlpParser {

    private val timeFormatter = SimpleDateFormat("h:mm a", Locale.US)

    private val wordToDigitMap = mapOf(
        "zero" to "0", "one" to "1", "two" to "2", "three" to "3", "four" to "4",
        "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9",
        "ten" to "10", "eleven" to "11", "twelve" to "12", "thirteen" to "13",
        "fourteen" to "14", "fifteen" to "15", "sixteen" to "16", "seventeen" to "17",
        "eighteen" to "18", "nineteen" to "19", "twenty" to "20", "twenty-five" to "25",
        "twenty five" to "25", "thirty" to "30", "thirty-five" to "35", "thirty five" to "35",
        "forty" to "40", "forty-five" to "45", "forty five" to "45", "fifty" to "50",
        "fifty-five" to "55", "fifty five" to "55"
    )

    private val hourWordToNumber = mapOf(
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6,
        "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12
    )

    private val minuteWordToNumber = mapOf(
        "o'clock" to 0, "oclock" to 0, "o clock" to 0,
        "fifteen" to 15, "twenty" to 20, "twenty-five" to 25, "twenty five" to 25,
        "thirty" to 30, "thirty-five" to 35, "thirty five" to 35, "forty" to 40,
        "forty-five" to 45, "forty five" to 45, "fifty" to 50, "fifty-five" to 55, "fifty five" to 55
    )

    /**
     * Normalizes spoken text variations like "p.m.", "a.m.", "half past five",
     * "quarter to six", "seven thirty", and number words before time units into canonical forms.
     */
    fun normalizeSpokenText(input: String): String {
        return try {
            var text = input

            // 1. Normalize "p.m.", "a.m.", "p. m.", "a. m.", "P.M.", "A.M."
            text = text.replace(Regex("(?i)\\b([ap])\\s*\\.\\s*m\\.?"), "$1m")
            text = text.replace(Regex("(?i)\\b([ap])\\.m\\b"), "$1m")

            // 2. Normalize "o'clock" and "o clock"
            text = text.replace(Regex("(?i)\\bo'?\\s*clock\\b"), "o'clock")

            // 3. Common spoken duration idioms
            text = text.replace(Regex("(?i)\\bhalf\\s+an\\s+hour\\b"), "30 minutes")
            text = text.replace(Regex("(?i)\\ban\\s+hour\\b"), "1 hour")
            text = text.replace(Regex("(?i)\\ba\\s+hour\\b"), "1 hour")
            text = text.replace(Regex("(?i)\\ba\\s+minute\\b"), "1 minute")
            text = text.replace(Regex("(?i)\\bcouple\\s+(?:of\\s+)?hours?\\b"), "2 hours")
            text = text.replace(Regex("(?i)\\bcouple\\s+(?:of\\s+)?minutes?\\b"), "2 minutes")

            // 4. Handle "half past [hour]", "quarter past [hour]", "quarter to [hour]"
            // e.g. "half past five" -> "5:30", "half past 5" -> "5:30"
            val halfPastRegex = Regex("(?i)\\bhalf\\s+past\\s+(\\w+|\\d+)\\b")
            text = text.replace(halfPastRegex) { match ->
                val hourStr = match.groupValues[1].lowercase()
                val hourNum = hourWordToNumber[hourStr] ?: hourStr.toIntOrNull()
                if (hourNum != null && hourNum in 1..12) {
                    "$hourNum:30"
                } else {
                    match.value
                }
            }

            // "quarter past [hour]" -> "[hour]:15"
            val quarterPastRegex = Regex("(?i)\\bquarter\\s+past\\s+(\\w+|\\d+)\\b")
            text = text.replace(quarterPastRegex) { match ->
                val hourStr = match.groupValues[1].lowercase()
                val hourNum = hourWordToNumber[hourStr] ?: hourStr.toIntOrNull()
                if (hourNum != null && hourNum in 1..12) {
                    "$hourNum:15"
                } else {
                    match.value
                }
            }

            // "quarter to [hour]" -> "[hour - 1]:45" (e.g. quarter to six -> 5:45)
            val quarterToRegex = Regex("(?i)\\bquarter\\s+to\\s+(\\w+|\\d+)\\b")
            text = text.replace(quarterToRegex) { match ->
                val hourStr = match.groupValues[1].lowercase()
                val hourNum = hourWordToNumber[hourStr] ?: hourStr.toIntOrNull()
                if (hourNum != null && hourNum in 1..12) {
                    val prevHour = if (hourNum == 1) 12 else hourNum - 1
                    "$prevHour:45"
                } else {
                    match.value
                }
            }

            // 5. Spoken hour + minute compound phrases (e.g., "seven thirty" -> "7:30", "five fifteen" -> "5:15", "eight forty five" -> "8:45")
            for ((hWord, hNum) in hourWordToNumber) {
                for ((mWord, mNum) in minuteWordToNumber) {
                    if (mNum > 0) {
                        val phraseRegex = Regex("(?i)\\b$hWord\\s+$mWord\\b")
                        val minutePad = if (mNum < 10) "0$mNum" else "$mNum"
                        text = text.replace(phraseRegex, "$hNum:$minutePad")
                    }
                }
            }

            // Also handle digit hour + minute word (e.g., "7 thirty" -> "7:30")
            for ((mWord, mNum) in minuteWordToNumber) {
                if (mNum > 0) {
                    val phraseRegex = Regex("(?i)\\b(\\d{1,2})\\s+$mWord\\b")
                    val minutePad = if (mNum < 10) "0$mNum" else "$mNum"
                    text = text.replace(phraseRegex, "$1:$minutePad")
                }
            }

            // 6. Word numbers before time units or after prepositions (without variable length lookbehinds)
            for ((word, digit) in wordToDigitMap) {
                val prepRegex = Regex("(?i)\\b(in|after|at|every|for|by)\\s+($word)(?=\\s+(hours?|hrs?|minutes?|mins?|days?|weeks?|months?|o'clock|am|pm|in the|at|\\b))")
                text = text.replace(prepRegex) { match ->
                    "${match.groupValues[1]} $digit"
                }
                text = text.replace(Regex("(?i)\\b$word(?=\\s+(hours?|hrs?|minutes?|mins?|o'clock|am|pm)\\b)"), digit)
            }

            // Convert word hours after "at" to digits: "at five" -> "at 5", "at six in the evening" -> "at 6 in the evening"
            for ((word, digit) in hourWordToNumber) {
                val atRegex = Regex("(?i)\\b(at)\\s+($word)(?=\\s+(in the|at|o'clock|am|pm|tonight|tomorrow|today|\\b))")
                text = text.replace(atRegex) { match ->
                    "${match.groupValues[1]} $digit"
                }
            }

            // 7. Contextual inline transformations:
            // Clock time with period words
            text = text.replace(Regex("(?i)\\b(\\d{1,2}(?::\\d{2})?)\\s+in the evening\\b"), "$1 pm")
            text = text.replace(Regex("(?i)\\b(\\d{1,2}(?::\\d{2})?)\\s+in the afternoon\\b"), "$1 pm")
            text = text.replace(Regex("(?i)\\b(\\d{1,2}(?::\\d{2})?)\\s+in the morning\\b"), "$1 am")
            text = text.replace(Regex("(?i)\\b(\\d{1,2}(?::\\d{2})?)\\s+(?:at\\s+night|in the night)\\b"), "$1 pm")

            // "9 tonight" -> "9 pm tonight", "7:30 tonight" -> "7:30 pm tonight"
            text = text.replace(Regex("(?i)\\b(\\d{1,2}(?::\\d{2})?)\\s+tonight\\b"), "$1 pm tonight")

            // "6 tomorrow morning" -> "6 am tomorrow", "6:30 tomorrow morning" -> "6:30 am tomorrow"
            text = text.replace(Regex("(?i)\\b(\\d{1,2}(?::\\d{2})?)\\s+tomorrow\\s+morning\\b"), "$1 am tomorrow")
            // "7:30 tomorrow evening" -> "7:30 pm tomorrow"
            text = text.replace(Regex("(?i)\\b(\\d{1,2}(?::\\d{2})?)\\s+tomorrow\\s+evening\\b"), "$1 pm tomorrow")
            // "3 tomorrow afternoon" -> "3 pm tomorrow"
            text = text.replace(Regex("(?i)\\b(\\d{1,2}(?::\\d{2})?)\\s+tomorrow\\s+afternoon\\b"), "$1 pm tomorrow")
            // "9 tomorrow night" -> "9 pm tomorrow"
            text = text.replace(Regex("(?i)\\b(\\d{1,2}(?::\\d{2})?)\\s+tomorrow\\s+night\\b"), "$1 pm tomorrow")

            text
        } catch (_: Throwable) {
            input
        }
    }

    fun parse(input: String): ParsedNudge {
        val original = input.trim()
        if (original.isBlank()) {
            return ParsedNudge(
                cleanTitle = "",
                extractedDate = "Today",
                extractedTime = "Any time",
                extractedCategory = "Personal",
                extractedPriority = "Normal",
                extractedRepeat = "Does not repeat",
                confidence = 0f,
                hasExplicitDateTime = false,
                originalText = original
            )
        }

        return try {
            var text = normalizeSpokenText(original)

            var detectedDate: String? = null
            var detectedTime: String? = null
            var detectedRepeat = "Does not repeat"
            var detectedPriority = "Normal"
            var hasExplicitDateTime = false
            var isAmbiguousAmPm = false
            var alternativeTime: String? = null
            var contextTimeOfDay: String? = null // "morning", "afternoon", "evening", "night"

        // 1. Priority Keywords
        val urgentRegex = Regex("(?i)\\b(urgently|urgent|important|asap|high priority|critical|must do|priority)\\b")
        if (urgentRegex.containsMatchIn(text)) {
            detectedPriority = "Important"
            text = text.replace(urgentRegex, " ")
        }

        // 2. Recurrence Keywords
        val dailyRegex = Regex("(?i)\\b(every day|daily|everyday|each day)\\b")
        val weeklyRegex = Regex("(?i)\\b(every week|weekly|each week|every (monday|tuesday|wednesday|thursday|friday|saturday|sunday))\\b")
        val monthlyRegex = Regex("(?i)\\b(every month|monthly|each month)\\b")

        if (dailyRegex.containsMatchIn(text)) {
            detectedRepeat = "Every day"
            text = text.replace(dailyRegex, " ")
            hasExplicitDateTime = true
        } else if (weeklyRegex.containsMatchIn(text)) {
            detectedRepeat = "Every week"
            text = text.replace(weeklyRegex, " ")
            hasExplicitDateTime = true
        } else if (monthlyRegex.containsMatchIn(text)) {
            detectedRepeat = "Every month"
            text = text.replace(monthlyRegex, " ")
            hasExplicitDateTime = true
        }

        // 3. Relative Minutes / Hours ("in 2 hours", "after 30 minutes", "in 30 mins", "in 45 minutes", "in 1 hour", "after 1 hour")
        val relativeTimeRegex = Regex("(?i)\\b(in|after)\\s+(\\d+)\\s*(minutes|minute|mins|min|m|hours|hour|hrs|hr|h)\\b")
        val relMatch = relativeTimeRegex.find(text)
        if (relMatch != null) {
            val amount = relMatch.groupValues[2].toIntOrNull() ?: 30
            val unit = relMatch.groupValues[3].lowercase()
            val cal = Calendar.getInstance()
            val startDay = cal.get(Calendar.DAY_OF_YEAR)
            if (unit.startsWith("h")) {
                cal.add(Calendar.HOUR_OF_DAY, amount)
            } else {
                cal.add(Calendar.MINUTE, amount)
            }
            detectedTime = timeFormatter.format(cal.time)
            detectedDate = if (cal.get(Calendar.DAY_OF_YEAR) != startDay) "Tomorrow" else "Today"
            hasExplicitDateTime = true
            text = text.replace(relMatch.value, " ")
        }

        // 4. Extract Date Phrases & Contextual Time-of-Day (e.g. "tomorrow evening", "tonight", "this morning")
        if (detectedTime == null) {
            // "today night", "tonight", "this night"
            val todayNightRegex = Regex("(?i)\\b(today\\s+night|tonight|this\\s+night)\\b")
            if (todayNightRegex.containsMatchIn(text)) {
                detectedDate = "Today"
                contextTimeOfDay = "night"
                hasExplicitDateTime = true
                text = text.replace(todayNightRegex, " ")
            }

            // "tomorrow morning", "tomorrow afternoon", "tomorrow evening", "tomorrow night"
            val tomorrowTimeRegex = Regex("(?i)\\btomorrow\\s+(morning|afternoon|evening|night)\\b")
            val tomTimeMatch = tomorrowTimeRegex.find(text)
            if (tomTimeMatch != null) {
                detectedDate = "Tomorrow"
                contextTimeOfDay = tomTimeMatch.groupValues[1].lowercase()
                hasExplicitDateTime = true
                text = text.replace(tomTimeMatch.value, " ")
            }

            // "today morning", "today afternoon", "today evening", "this morning", "this afternoon", "this evening"
            val todayTimeRegex = Regex("(?i)\\b(today|this)\\s+(morning|afternoon|evening)\\b")
            val todayTimeMatch = todayTimeRegex.find(text)
            if (todayTimeMatch != null) {
                detectedDate = "Today"
                contextTimeOfDay = todayTimeMatch.groupValues[2].lowercase()
                hasExplicitDateTime = true
                text = text.replace(todayTimeMatch.value, " ")
            }
        }

        // Check general "tomorrow", "tmrw"
        val tomorrowRegex = Regex("(?i)\\b(tomorrow|tmrw)\\b")
        if (tomorrowRegex.containsMatchIn(text)) {
            if (detectedDate == null) detectedDate = "Tomorrow"
            hasExplicitDateTime = true
            text = text.replace(tomorrowRegex, " ")
        }

        // Check general "today"
        val todayRegex = Regex("(?i)\\b(today)\\b")
        if (todayRegex.containsMatchIn(text)) {
            if (detectedDate == null) detectedDate = "Today"
            hasExplicitDateTime = true
            text = text.replace(todayRegex, " ")
        }

        // "this weekend" / "next weekend" / "weekend"
        val weekendRegex = Regex("(?i)\\b((this|next)\\s+weekend|weekend)\\b")
        if (weekendRegex.containsMatchIn(text)) {
            if (detectedDate == null) detectedDate = "This weekend"
            hasExplicitDateTime = true
            text = text.replace(weekendRegex, " ")
        }

        // "next week" / "this week" / "next month"
        val nextWeekRegex = Regex("(?i)\\b(next week)\\b")
        if (nextWeekRegex.containsMatchIn(text)) {
            if (detectedDate == null) detectedDate = "Next week"
            hasExplicitDateTime = true
            text = text.replace(nextWeekRegex, " ")
        }
        val nextMonthRegex = Regex("(?i)\\b(next month)\\b")
        if (nextMonthRegex.containsMatchIn(text)) {
            if (detectedDate == null) detectedDate = "Next month"
            hasExplicitDateTime = true
            text = text.replace(nextMonthRegex, " ")
        }

        // Days of week: Monday..Sunday, with optional "this" or "next" or "on"
        val daysRegex = Regex("(?i)\\b(?:on\\s+)?(?:(this|next)\\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)(?:\\s+(morning|afternoon|evening|night))?\\b")
        val dayMatch = daysRegex.find(text)
        if (dayMatch != null && detectedDate == null) {
            val dayName = dayMatch.groupValues[2].lowercase().replaceFirstChar { it.uppercase() }
            val modifier = dayMatch.groupValues[1]
            val dayTimeOfDay = dayMatch.groupValues[3]
            if (dayTimeOfDay.isNotBlank() && contextTimeOfDay == null) {
                contextTimeOfDay = dayTimeOfDay.lowercase()
            }
            detectedDate = if (modifier.isNotBlank()) "${modifier.lowercase().replaceFirstChar { it.uppercase() }} $dayName" else dayName
            hasExplicitDateTime = true
            text = text.replace(dayMatch.value, " ")
        }

        // Calendar dates: "August 30th", "30th of August", "Aug 30", "30 August"
        val monthNames = "jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?"
        val dateMonthRegex = Regex("(?i)\\b(?:on\\s+)?($monthNames)\\s+(\\d{1,2})(?:st|nd|rd|th)?\\b")
        val dayMonthRegex = Regex("(?i)\\b(?:on\\s+)?(\\d{1,2})(?:st|nd|rd|th)?(?:\\s+of)?\\s+($monthNames)\\b")

        val monthShortMap = mapOf(
            "january" to "Jan", "jan" to "Jan",
            "february" to "Feb", "feb" to "Feb",
            "march" to "Mar", "mar" to "Mar",
            "april" to "Apr", "apr" to "Apr",
            "may" to "May",
            "june" to "Jun", "jun" to "Jun",
            "july" to "Jul", "jul" to "Jul",
            "august" to "Aug", "aug" to "Aug",
            "september" to "Sep", "sep" to "Sep",
            "october" to "Oct", "oct" to "Oct",
            "november" to "Nov", "nov" to "Nov",
            "december" to "Dec", "dec" to "Dec"
        )

        val dmMatch = dateMonthRegex.find(text) ?: dayMonthRegex.find(text)
        if (dmMatch != null && detectedDate == null) {
            val isDateMonth = dateMonthRegex.matches(dmMatch.value)
            val monthRaw = if (isDateMonth) dmMatch.groupValues[1] else dmMatch.groupValues[2]
            val dayRaw = if (isDateMonth) dmMatch.groupValues[2] else dmMatch.groupValues[1]
            val shortMonth = monthShortMap[monthRaw.lowercase()] ?: monthRaw.replaceFirstChar { it.uppercase() }
            detectedDate = "$shortMonth $dayRaw"
            hasExplicitDateTime = true
            text = text.replace(dmMatch.value, " ")
        }

        // 5. Clock Time Extraction with AM/PM or Contextual Modifiers
        if (detectedTime == null) {
            // Pattern A: Clock with explicit AM / PM (e.g. "at 5 pm", "5:30 pm", "6:00 am", "6 am", "12 pm", "12 am", "at 5 PM")
            val clockWithAmPmRegex = Regex("(?i)\\b(?:at\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(?:o'?clock)?\\s*(am|pm)\\b")
            val amPmMatch = clockWithAmPmRegex.find(text)
            if (amPmMatch != null) {
                var hour = amPmMatch.groupValues[1].toIntOrNull() ?: 12
                val minute = amPmMatch.groupValues[2].ifEmpty { "00" }.toIntOrNull() ?: 0
                val ampm = amPmMatch.groupValues[3].uppercase(Locale.US)

                if (ampm == "PM" && hour < 12) hour += 12
                if (ampm == "AM" && hour == 12) hour = 0

                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                detectedTime = timeFormatter.format(cal.time)
                hasExplicitDateTime = true
                isAmbiguousAmPm = false
                text = text.replace(amPmMatch.value, " ")
            }
        }

        if (detectedTime == null) {
            // Pattern B: Clock with inline context (e.g. "at 6 in the evening", "6 in the morning", "6 at night", "6 in the afternoon", "at 6 o'clock in the morning")
            val clockWithContextRegex = Regex("(?i)\\b(?:at\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(?:o'?clock)?\\s*(in the morning|in the afternoon|in the evening|at night|in the night)\\b")
            val contextMatch = clockWithContextRegex.find(text)
            if (contextMatch != null) {
                var hour = contextMatch.groupValues[1].toIntOrNull() ?: 12
                val minute = contextMatch.groupValues[2].ifEmpty { "00" }.toIntOrNull() ?: 0
                val inlineContext = contextMatch.groupValues[3].lowercase()

                hour = when {
                    inlineContext.contains("morning") -> if (hour == 12) 0 else hour
                    inlineContext.contains("afternoon") -> if (hour < 12) hour + 12 else hour
                    inlineContext.contains("evening") -> if (hour < 12) hour + 12 else hour
                    inlineContext.contains("night") -> if (hour == 12) 0 else if (hour < 12) hour + 12 else hour
                    else -> hour
                }

                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                detectedTime = timeFormatter.format(cal.time)
                hasExplicitDateTime = true
                isAmbiguousAmPm = false
                text = text.replace(contextMatch.value, " ")
            }
        }

        if (detectedTime == null && contextTimeOfDay != null) {
            // Pattern C: Time with previously captured context (e.g. "tomorrow evening at 6", "tomorrow morning at 6:30", "tonight at 8")
            val clockWithPrevContextRegex = Regex("(?i)\\b(?:at\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(?:o'?clock)?\\b")
            val prevContextMatch = clockWithPrevContextRegex.find(text)
            if (prevContextMatch != null) {
                var hour = prevContextMatch.groupValues[1].toIntOrNull() ?: 12
                val minute = prevContextMatch.groupValues[2].ifEmpty { "00" }.toIntOrNull() ?: 0

                if (hour in 1..12) {
                    hour = when (contextTimeOfDay) {
                        "morning" -> if (hour == 12) 0 else hour
                        "afternoon" -> if (hour < 12) hour + 12 else hour
                        "evening" -> if (hour < 12) hour + 12 else hour
                        "night" -> if (hour == 12) 0 else if (hour < 12) hour + 12 else hour
                        else -> hour
                    }

                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    detectedTime = timeFormatter.format(cal.time)
                    hasExplicitDateTime = true
                    isAmbiguousAmPm = false
                    text = text.replace(prevContextMatch.value, " ")
                }
            }
        }

        if (detectedTime == null) {
            // Pattern D: 24-hour time or explicit colon time ("at 14:00", "18:30", "at 6:30", "at 6")
            val clockPlainRegex = Regex("(?i)\\b(?:at\\s+)?(\\d{1,2})(?::(\\d{2}))(?:\\s*o'?clock)?\\b")
            val plainColonMatch = clockPlainRegex.find(text)
            if (plainColonMatch != null) {
                val rawHour = plainColonMatch.groupValues[1].toIntOrNull() ?: 12
                val minute = plainColonMatch.groupValues[2].toIntOrNull() ?: 0

                if (rawHour in 13..23) {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, rawHour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    detectedTime = timeFormatter.format(cal.time)
                    hasExplicitDateTime = true
                    isAmbiguousAmPm = false
                    text = text.replace(plainColonMatch.value, " ")
                } else if (rawHour in 1..12) {
                    // Default to PM for typical waking afternoon/evening hours (1..6), AM for (7..11), 12 PM for 12
                    val isDefaultPm = rawHour in 1..6 || rawHour == 12
                    val calMain = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, if (isDefaultPm && rawHour < 12) rawHour + 12 else rawHour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val calAlt = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, if (isDefaultPm) rawHour else if (rawHour < 12) rawHour + 12 else 0)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    detectedTime = timeFormatter.format(calMain.time)
                    alternativeTime = timeFormatter.format(calAlt.time)
                    isAmbiguousAmPm = true
                    hasExplicitDateTime = true
                    text = text.replace(plainColonMatch.value, " ")
                }
            } else {
                // Number after "at" or "o'clock" e.g. "at 6", "6 o'clock"
                val clockAtRegex = Regex("(?i)\\b(?:at\\s+(\\d{1,2})|(\\d{1,2})\\s*o'?clock)\\b")
                val atMatch = clockAtRegex.find(text)
                if (atMatch != null) {
                    val rawHour = (atMatch.groupValues[1].ifEmpty { atMatch.groupValues[2] }).toIntOrNull() ?: 12
                    val minute = 0

                    if (rawHour in 13..23) {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, rawHour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        detectedTime = timeFormatter.format(cal.time)
                        hasExplicitDateTime = true
                        isAmbiguousAmPm = false
                        text = text.replace(atMatch.value, " ")
                    } else if (rawHour in 1..12) {
                        val isDefaultPm = rawHour in 1..6 || rawHour == 12
                        val calMain = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, if (isDefaultPm && rawHour < 12) rawHour + 12 else rawHour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val calAlt = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, if (isDefaultPm) rawHour else if (rawHour < 12) rawHour + 12 else 0)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        detectedTime = timeFormatter.format(calMain.time)
                        alternativeTime = timeFormatter.format(calAlt.time)
                        isAmbiguousAmPm = true
                        hasExplicitDateTime = true
                        text = text.replace(atMatch.value, " ")
                    }
                }
            }
        }

        // 6. Generic Time of Day Fallbacks if no specific clock time was found
        val morningRegex = Regex("(?i)\\b(in the morning|morning)\\b")
        val afternoonRegex = Regex("(?i)\\b(in the afternoon|afternoon)\\b")
        val eveningRegex = Regex("(?i)\\b(in the evening|evening)\\b")
        val nightRegex = Regex("(?i)\\b(at night|in the night|night)\\b")
        val noonRegex = Regex("(?i)\\b(at noon|midday|noon)\\b")
        val midnightRegex = Regex("(?i)\\b(at midnight|midnight)\\b")

        if (detectedTime == null) {
            when {
                contextTimeOfDay == "morning" || morningRegex.containsMatchIn(text) -> {
                    detectedTime = "9:00 AM"
                    hasExplicitDateTime = true
                }
                contextTimeOfDay == "afternoon" || afternoonRegex.containsMatchIn(text) -> {
                    detectedTime = "2:00 PM"
                    hasExplicitDateTime = true
                }
                contextTimeOfDay == "evening" || eveningRegex.containsMatchIn(text) -> {
                    detectedTime = "6:00 PM"
                    hasExplicitDateTime = true
                }
                contextTimeOfDay == "night" || nightRegex.containsMatchIn(text) -> {
                    detectedTime = "8:00 PM"
                    hasExplicitDateTime = true
                }
                noonRegex.containsMatchIn(text) -> {
                    detectedTime = "12:00 PM"
                    hasExplicitDateTime = true
                }
                midnightRegex.containsMatchIn(text) -> {
                    detectedTime = "12:00 AM"
                    hasExplicitDateTime = true
                }
            }
        }

        // Strip remaining time of day words from text
        text = text.replace(morningRegex, " ")
            .replace(afternoonRegex, " ")
            .replace(eveningRegex, " ")
            .replace(nightRegex, " ")
            .replace(noonRegex, " ")
            .replace(midnightRegex, " ")

        // 7. Strip Action & Prefix Phrases from Task Title (anywhere in sentence or at beginning)
        // Matches "remind me to", "remind me", "remember to", "set a reminder to", "set reminder to", "nudge me to", etc.
        val prefixRegex = Regex("(?i)\\b(please\\s+)?(remind me to|remind me|remember to|don't forget to|dont forget to|nudge me to|nudge me|schedule to|schedule|set a reminder to|set a reminder for|set reminder to|set reminder for|i have to|i need to|i must|i've got to|i gotta|i will go to|i will go|i am going to|i am going|need to|have to|gotta)\\b")
        text = text.replace(prefixRegex, " ")

        // 8. Clean trailing & leading prepositions, conjunctions, punctuation & extra spaces
        text = text.trim()
        // Strip leading particles like "to ", "about ", "for ", "that ", "on ", "at ", "by ", "in "
        text = text.replace(Regex("(?i)^(?:to|about|for|that|of|on|at|by|in|with)\\s+(?!(?:ai|him|her|them|it)\\b)"), "")
        // Handle repeated leading "to " if remaining
        text = text.replace(Regex("(?i)^to\\s+"), "")
        // Strip trailing prepositions/particles
        text = text.replace(Regex("(?i)\\b(at|on|for|by|in|to|with|about)\\s*$"), "")
            .replace(Regex("[.,!?;:]+$"), "")
            .replace(Regex("^[.,!?;:]+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        val finalTitle = if (text.isNotBlank()) {
            text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        } else {
            "Reminder"
        }

        // 9. Semantic Categorization
        val detectedCategory = inferCategory(finalTitle)

        val finalDate = detectedDate ?: "Today"
        val finalTime = detectedTime ?: if (finalDate == "Today") "Any time" else "9:00 AM"

        val prompt = if (isAmbiguousAmPm && detectedTime != null) {
            "Did you mean $finalDate at $finalTime?"
        } else null

        ParsedNudge(
            cleanTitle = finalTitle,
            extractedDate = finalDate,
            extractedTime = finalTime,
            extractedCategory = detectedCategory,
            extractedPriority = detectedPriority,
            extractedRepeat = detectedRepeat,
            confidence = if (hasExplicitDateTime && !isAmbiguousAmPm) 0.98f else if (hasExplicitDateTime) 0.85f else 0.7f,
            hasExplicitDateTime = hasExplicitDateTime,
            originalText = original,
            isAmbiguousAmPm = isAmbiguousAmPm,
            alternativeTime = alternativeTime,
            confirmationPrompt = prompt
        )
    } catch (_: Throwable) {
        ParsedNudge(
            cleanTitle = original.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            extractedDate = "Today",
            extractedTime = "Any time",
            extractedCategory = "Personal",
            extractedPriority = "Normal",
            extractedRepeat = "Does not repeat",
            confidence = 0.5f,
            hasExplicitDateTime = false,
            originalText = original
        )
    }
}

    private fun inferCategory(text: String): String {
        val lower = text.lowercase()

        // Shopping
        if (lower.contains(Regex("\\b(buy|groceries|grocery|milk|bread|toothpaste|market|supermarket|store|shopping|order|purchase|amazon|pharmacy|cart|eggs|coffee|fruit|veggies|shampoo|soap)\\b"))) {
            return "Shopping"
        }

        // Work
        if (lower.contains(Regex("\\b(work|meeting|meet with|client|boss|colleague|email|send email|report|presentation|documents|project|zoom|deadline|review|contract|interview|office|spreadsheet|deck|invoice|jira|slack|ai)\\b"))) {
            return "Work"
        }

        // Home
        if (lower.contains(Regex("\\b(home|clean|laundry|dishes|oven|water plants|plants|fix|repair|cook|dinner|kitchen|garage|trash|house|garden|vacuum|plumber|mow|ac|fridge|microwave|bedroom)\\b"))) {
            return "Home"
        }

        // Personal
        if (lower.contains(Regex("\\b(call|phone|mom|dad|friend|gym|workout|doctor|dentist|medicine|pill|meditation|read|walk|relax|birthday|party|anniversary|rahul|meera|cyrus|book|run|yoga|vet|pet|dog|cat|sleep|wake|wake up|meet him|meet her)\\b"))) {
            return "Personal"
        }

        return "Personal"
    }
}
