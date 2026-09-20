package com.example.gentlenudge.data.events

import androidx.compose.ui.graphics.Color

/**
 * Categories for calendar events and observances.
 */
enum class EventCategory(
    val displayName: String,
    val bgLight: Long,
    val textLight: Long,
    val badgeBgLight: Long,
    val badgeTextLight: Long,
    val bgDark: Long = 0xFF22252A,
    val textDark: Long = 0xFFE0E0E0,
    val badgeBgDark: Long = 0xFF373E47,
    val badgeTextDark: Long = 0xFFE0E0E0
) {
    HINDU_FESTIVAL(
        displayName = "Hindu Festival",
        bgLight = 0xFFFFF8EC, // Soft warm saffron / cream
        textLight = 0xFF4E2600, // Dark warm brown for high contrast
        badgeBgLight = 0xFFFFE4BF,
        badgeTextLight = 0xFF4E2600,
        bgDark = 0xFF2B2117, // Dark warm amber
        textDark = 0xFFFFD180, // Light warm amber
        badgeBgDark = 0xFF523D26, // Dark amber border
        badgeTextDark = 0xFFFFD180
    ),
    NATIONAL_DAY(
        displayName = "National Day",
        bgLight = 0xFFEFF4FC, // Soft light blue
        textLight = 0xFF0E2554, // Dark navy text
        badgeBgLight = 0xFFD7E4FB,
        badgeTextLight = 0xFF0E2554,
        bgDark = 0xFF142238, // Dark navy
        textDark = 0xFF90CAF9, // Light blue
        badgeBgDark = 0xFF203657,
        badgeTextDark = 0xFF90CAF9
    ),
    CULTURAL_EVENT(
        displayName = "Cultural Event",
        bgLight = 0xFFF1F8F3, // Soft light green
        textLight = 0xFF0E3F1F, // Dark forest green text
        badgeBgLight = 0xFFD4EED8,
        badgeTextLight = 0xFF0E3F1F,
        bgDark = 0xFF13281B, // Dark forest green
        textDark = 0xFFA5D6A7, // Light sage green
        badgeBgDark = 0xFF24442F,
        badgeTextDark = 0xFFA5D6A7
    ),
    INTERNATIONAL_DAY(
        displayName = "International Observance",
        bgLight = 0xFFF0F9FB, // Soft light cyan / blue
        textLight = 0xFF053F4D, // Dark teal / blue text
        badgeBgLight = 0xFFD2EFF5,
        badgeTextLight = 0xFF053F4D,
        bgDark = 0xFF13282D, // Dark cyan
        textDark = 0xFF80DEEA, // Soft cyan
        badgeBgDark = 0xFF234B54, // Dark teal border
        badgeTextDark = 0xFF80DEEA
    ),
    RELIGIOUS_FESTIVAL(
        displayName = "Religious Observance",
        bgLight = 0xFFF6F0FC, // Soft light purple / lavender
        textLight = 0xFF3B1266, // Dark purple text
        badgeBgLight = 0xFFE8DBFA,
        badgeTextLight = 0xFF3B1266,
        bgDark = 0xFF261D30, // Dark lavender
        textDark = 0xFFCE93D8, // Light lavender
        badgeBgDark = 0xFF443357,
        badgeTextDark = 0xFFCE93D8
    ),
    AWARENESS_DAY(
        displayName = "Awareness Day",
        bgLight = 0xFFFFF2F4, // Soft rose / coral
        textLight = 0xFF5C1021, // Dark rose text
        badgeBgLight = 0xFFFFDBE1,
        badgeTextLight = 0xFF5C1021,
        bgDark = 0xFF2C161C, // Dark rose
        textDark = 0xFFF48FB1, // Light rose
        badgeBgDark = 0xFF502732,
        badgeTextDark = 0xFFF48FB1
    ),
    PUBLIC_HOLIDAY(
        displayName = "Public Holiday",
        bgLight = 0xFFF4F6F8, // Soft slate / neutral gray
        textLight = 0xFF1F2937, // Dark slate text
        badgeBgLight = 0xFFE2E8F0,
        badgeTextLight = 0xFF1F2937,
        bgDark = 0xFF22252A, // Dark slate
        textDark = 0xFFCFD8DC, // Light slate
        badgeBgDark = 0xFF373E47,
        badgeTextDark = 0xFFCFD8DC
    );

    val lightBackgroundColor: Color get() = Color(bgLight)
    val lightTextColor: Color get() = Color(textLight)
    val badgeBackgroundColor: Color get() = Color(badgeBgLight)
    val badgeTextColor: Color get() = Color(badgeTextLight)

    val darkBackgroundColor: Color get() = Color(bgDark)
    val darkTextColor: Color get() = Color(textDark)
    val darkBadgeBackgroundColor: Color get() = Color(badgeBgDark)
    val darkBadgeTextColor: Color get() = Color(badgeTextDark)
}

enum class DateBasis(val displayName: String) {
    FIXED_GREGORIAN("Fixed Gregorian Date"),
    SOLAR_CALENDAR("Solar Calendar (Sankranti / Sauramana)"),
    HINDU_LUNAR("Hindu Lunisolar (Tithi / Chandramana)"),
    ISLAMIC_LUNAR("Islamic Lunar Calendar"),
    REGIONAL_CALENDAR("Regional Calendar"),
    ASTRONOMICAL_CALCULATED("Astronomical Calculation")
}

/**
 * Importance / prominence level for an event.
 */
enum class EventImportance(val displayName: String) {
    MAJOR("Major Festival / Holiday"),
    STANDARD("Important Observance"),
    REGIONAL("Regional / Cultural Observance")
}

/**
 * Immutable structured model for calendar events and observances.
 */
data class NudgeCalendarEvent(
    val id: String,
    val name: String,
    val month: Int, // 1-12 (1 = January, 12 = December)
    val day: Int, // 1-31
    val year: Int? = null, // null for annual recurring events, specific year for lunisolar/movable events
    val category: EventCategory,
    val description: String,
    val traditionOrRegion: String? = null,
    val importance: EventImportance = EventImportance.STANDARD,
    val dateBasis: DateBasis = if (year == null) DateBasis.FIXED_GREGORIAN else DateBasis.HINDU_LUNAR,
    val region: String? = traditionOrRegion,
    val religionOrTradition: String? = traditionOrRegion,
    val regionalVariationPossible: Boolean = false,
    val calculationBasisOrSource: String? = null
) {
    /**
     * Checks if this event matches the given year, month (1-12), and day (1-31).
     */
    fun matchesDate(targetYear: Int, targetMonth: Int, targetDay: Int): Boolean {
        if (this.month != targetMonth || this.day != targetDay) {
            return false
        }
        return this.year == null || this.year == targetYear
    }
}
