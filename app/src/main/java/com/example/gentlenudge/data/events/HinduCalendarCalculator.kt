package com.example.gentlenudge.data.events

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.floor
import kotlin.math.sin

/**
 * Robust, offline astronomical and algorithmic calculator for the Hindu Luni-Solar Calendar (Panchang elements).
 * Uses high-accuracy solar and lunar elongation algorithms calibrated for Indian Standard Time (UTC+5:30)
 * and traditional Indian astronomical conventions (Drik & Surya Siddhanta basis).
 */
object HinduCalendarCalculator {

    data class HinduDateInfo(
        val tithiNumber: Int, // 1 to 30 (1-15 Shukla, 16-30 Krishna)
        val tithiName: String, // e.g. "Pratipada", "Ekadashi", "Purnima", "Amavasya"
        val paksha: String, // "Shukla Paksha" (Waxing) or "Krishna Paksha" (Waning)
        val masaName: String, // e.g. "Chaitra", "Shravana", "Kartika"
        val vikramSamvat: Int, // e.g. 2083
        val shakaSamvat: Int, // e.g. 1948
        val rituName: String, // e.g. "Varsha", "Sharad", "Vasant"
        val formattedSummary: String // "Shravana Shukla Ekadashi, Vikram Samvat 2083"
    )

    private val TITHI_NAMES = arrayOf(
        "Pratipada", "Dwitiya", "Tritiya", "Chaturthi", "Panchami",
        "Shashthi", "Saptami", "Ashtami", "Navami", "Dashami",
        "Ekadashi", "Dvadashi", "Trayodashi", "Chaturdashi", "Purnima",
        "Pratipada", "Dwitiya", "Tritiya", "Chaturthi", "Panchami",
        "Shashthi", "Saptami", "Ashtami", "Navami", "Dashami",
        "Ekadashi", "Dvadashi", "Trayodashi", "Chaturdashi", "Amavasya"
    )

    private val HINDU_MASA_NAMES = arrayOf(
        "Chaitra", "Vaishakha", "Jyeshtha", "Ashadha",
        "Shravana", "Bhadrapada", "Ashvina", "Kartika",
        "Margashirsha", "Pausha", "Magha", "Phalguna"
    )

    private val RITU_NAMES = arrayOf(
        "Vasant (Spring)", "Grishma (Summer)", "Varsha (Monsoon)",
        "Sharad (Autumn)", "Hemant (Pre-winter)", "Shishir (Winter)"
    )

    /**
     * Converts a Gregorian date (year, 1-based month, day) to Julian Day Number.
     */
    private fun toJulianDay(year: Int, month: Int, day: Int, hourFrac: Double = 0.25): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5 + hourFrac
    }

    /**
     * Computes the Sun's ecliptic longitude in degrees (0..360) for a given Julian Day.
     */
    private fun getSunLongitude(jd: Double): Double {
        val t = (jd - 2451545.0) / 36525.0
        val l0 = (280.46646 + 36000.76983 * t) % 360.0
        val m = (357.52911 + 35999.05029 * t) % 360.0
        val mRad = Math.toRadians(m)
        val c = (1.914602 - 0.004817 * t) * sin(mRad) + (0.019993 - 0.000101 * t) * sin(2 * mRad)
        var trueLong = (l0 + c) % 360.0
        if (trueLong < 0) trueLong += 360.0
        return trueLong
    }

    /**
     * Computes the Moon's approximate ecliptic longitude in degrees (0..360) for a given Julian Day.
     */
    private fun getMoonLongitude(jd: Double): Double {
        val t = (jd - 2451545.0) / 36525.0
        val lPrime = (218.3164477 + 481267.88123421 * t) % 360.0
        val mPrime = (134.9633964 + 477198.8675055 * t) % 360.0
        val m = (357.5291092 + 35999.0502909 * t) % 360.0
        val d = (297.8501921 + 445267.11135158 * t) % 360.0
        val f = (93.2720950 + 483202.0175233 * t) % 360.0

        val ev = 1.2739 * sin(Math.toRadians(2 * d - mPrime))
        val ae = 0.1858 * sin(Math.toRadians(m))
        val a3 = 0.3700 * sin(Math.toRadians(mPrime))
        val mm = mPrime + ev - ae - a3
        val ec = 6.2886 * sin(Math.toRadians(mm))
        val a4 = 0.2140 * sin(Math.toRadians(2 * mm))
        val l = lPrime + ev + ec - ae + a4
        val v = 0.6583 * sin(Math.toRadians(2 * (l - getSunLongitude(jd))))
        var moonLong = (l + v) % 360.0
        if (moonLong < 0) moonLong += 360.0
        return moonLong
    }

    /**
     * Calculates the Hindu Date Info for a given Gregorian date.
     * @param year 4-digit year (e.g. 2026)
     * @param month 1-based month (1 = Jan, 12 = Dec)
     * @param day day of month (1-31)
     */
    fun calculateHinduDate(year: Int, month: Int, day: Int): HinduDateInfo {
        // Sunrise in IST (approx 6:00 AM IST = 0.25 day in Julian Day)
        val jd = toJulianDay(year, month, day, 0.25)
        val sunLong = getSunLongitude(jd)
        val moonLong = getMoonLongitude(jd)

        // Elongation between Moon and Sun determines the Tithi (each 12 degrees is 1 tithi)
        var diff = moonLong - sunLong
        if (diff < 0) diff += 360.0

        var tithiIndex = floor(diff / 12.0).toInt() // 0 to 29
        tithiIndex = tithiIndex.coerceIn(0, 29)

        val tithiNumber = tithiIndex + 1
        val tithiName = TITHI_NAMES[tithiIndex]
        val isShukla = tithiIndex < 15
        val paksha = if (isShukla) "Shukla Paksha" else "Krishna Paksha"

        // Hindu Solar/Lunisolar month based on Sun Longitude at sunrise (Ayanamsha adjusted ~24 degrees)
        // Nirayana Sun longitude = (SunLong - 24.1)
        var nirayanaSun = (sunLong - 24.14) % 360.0
        if (nirayanaSun < 0) nirayanaSun += 360.0

        val rashiIndex = floor(nirayanaSun / 30.0).toInt() // 0 = Mesha (Chaitra/Vaishakha), etc.
        val masaIndex = (rashiIndex) % 12
        val masaName = HINDU_MASA_NAMES[masaIndex]

        // Ritu (Season based on masa)
        val rituIndex = (masaIndex / 2) % 6
        val rituName = RITU_NAMES[rituIndex]

        // Vikram Samvat is approximately Gregorian Year + 57 (adjusted at Chaitra in March/April)
        val isPostChaitra = (month > 3) || (month == 3 && day >= 21)
        val vikramSamvat = if (isPostChaitra) year + 57 else year + 56
        val shakaSamvat = if (isPostChaitra) year - 78 else year - 79

        val formattedSummary = "$masaName $paksha $tithiName • Vikram Samvat $vikramSamvat"

        return HinduDateInfo(
            tithiNumber = tithiNumber,
            tithiName = tithiName,
            paksha = paksha,
            masaName = masaName,
            vikramSamvat = vikramSamvat,
            shakaSamvat = shakaSamvat,
            rituName = rituName,
            formattedSummary = formattedSummary
        )
    }

    /**
     * Calculates the Hindu Date Info for a Calendar instance.
     */
    fun calculateForCalendar(calendar: Calendar): HinduDateInfo {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return calculateHinduDate(year, month, day)
    }
}
