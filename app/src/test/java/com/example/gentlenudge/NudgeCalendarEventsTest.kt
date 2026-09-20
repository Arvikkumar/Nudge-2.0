package com.example.gentlenudge

import com.example.gentlenudge.data.events.EventCategory
import com.example.gentlenudge.data.events.NudgeEventsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class NudgeCalendarEventsTest {

    @Test
    fun testIndianNationalHolidays() {
        // Republic Day (Jan 26)
        val repEvents = NudgeEventsRepository.getEventsForDate(2026, 1, 26)
        assertTrue(repEvents.any { it.name.contains("Republic Day") && it.category == EventCategory.NATIONAL_DAY })

        // Independence Day (Aug 15)
        val indEvents = NudgeEventsRepository.getEventsForDate(2026, 8, 15)
        assertTrue(indEvents.any { it.name.contains("Independence Day") && it.category == EventCategory.NATIONAL_DAY })

        // Gandhi Jayanti (Oct 2)
        val gandhiEvents = NudgeEventsRepository.getEventsForDate(2026, 10, 2)
        assertTrue(gandhiEvents.any { it.name.contains("Mahatma Gandhi") && it.category == EventCategory.NATIONAL_DAY })
    }

    @Test
    fun testIndianHinduFestivals() {
        // Diwali 2026 (Nov 8)
        val diwali2026 = NudgeEventsRepository.getEventsForDate(2026, 11, 8)
        assertTrue(diwali2026.any { it.name.contains("Diwali") && it.category == EventCategory.HINDU_FESTIVAL })

        // Holi 2026 (Mar 4)
        val holi2026 = NudgeEventsRepository.getEventsForDate(2026, 3, 4)
        assertTrue(holi2026.any { it.name.contains("Holi") && it.category == EventCategory.HINDU_FESTIVAL })

        // Maha Shivaratri 2026 (Feb 15)
        val shiva2026 = NudgeEventsRepository.getEventsForDate(2026, 2, 15)
        assertTrue(shiva2026.any { it.name.contains("Maha Shivaratri") && it.category == EventCategory.HINDU_FESTIVAL })

        // Ganesh Chaturthi 2026 (Sep 14)
        val ganesh2026 = NudgeEventsRepository.getEventsForDate(2026, 9, 14)
        assertTrue(ganesh2026.any { it.name.contains("Ganesh") && it.category == EventCategory.HINDU_FESTIVAL })

        // Raksha Bandhan 2026 (Aug 28)
        val rakhi2026 = NudgeEventsRepository.getEventsForDate(2026, 8, 28)
        assertTrue(rakhi2026.any { it.name.contains("Raksha Bandhan") && it.category == EventCategory.HINDU_FESTIVAL })
    }

    @Test
    fun testMajorCulturalEvents() {
        // Onam 2026 (Aug 29)
        val onam2026 = NudgeEventsRepository.getEventsForDate(2026, 8, 29)
        assertTrue(onam2026.any { it.name.contains("Onam") && it.category == EventCategory.CULTURAL_EVENT })

        // Vaisakhi (Apr 13)
        val vaisakhi = NudgeEventsRepository.getEventsForDate(2026, 4, 13)
        assertTrue(vaisakhi.any { it.name.contains("Vaisakhi") && it.category == EventCategory.CULTURAL_EVENT })

        // Lohri (Jan 13)
        val lohri = NudgeEventsRepository.getEventsForDate(2026, 1, 13)
        assertTrue(lohri.any { it.name.contains("Lohri") && it.category == EventCategory.CULTURAL_EVENT })
    }

    @Test
    fun testInternationalObservances() {
        // International Women's Day (Mar 8)
        val womensDay = NudgeEventsRepository.getEventsForDate(2026, 3, 8)
        assertTrue(womensDay.any { it.name.contains("International Women's Day") && it.category == EventCategory.INTERNATIONAL_DAY })

        // Earth Day (Apr 22)
        val earthDay = NudgeEventsRepository.getEventsForDate(2026, 4, 22)
        assertTrue(earthDay.any { it.name.contains("Earth Day") && it.category == EventCategory.INTERNATIONAL_DAY })

        // International Day of Yoga (Jun 21)
        val yogaDay = NudgeEventsRepository.getEventsForDate(2026, 6, 21)
        assertTrue(yogaDay.any { it.name.contains("Yoga") && it.category == EventCategory.INTERNATIONAL_DAY })

        // World Environment Day (Jun 5)
        val envDay = NudgeEventsRepository.getEventsForDate(2026, 6, 5)
        assertTrue(envDay.any { it.name.contains("Environment") && it.category == EventCategory.INTERNATIONAL_DAY })
    }

    @Test
    fun testDiverseReligiousObservances() {
        // Christmas (Dec 25)
        val christmas = NudgeEventsRepository.getEventsForDate(2026, 12, 25)
        assertTrue(christmas.any { it.name.contains("Christmas") && it.category == EventCategory.RELIGIOUS_FESTIVAL })

        // Good Friday 2026 (Apr 3)
        val goodFriday2026 = NudgeEventsRepository.getEventsForDate(2026, 4, 3)
        assertTrue(goodFriday2026.any { it.name.contains("Good Friday") && it.category == EventCategory.RELIGIOUS_FESTIVAL })

        // Easter Sunday 2026 (Apr 5)
        val easter2026 = NudgeEventsRepository.getEventsForDate(2026, 4, 5)
        assertTrue(easter2026.any { it.name.contains("Easter") && it.category == EventCategory.RELIGIOUS_FESTIVAL })

        // Eid al-Fitr 2026 (Mar 20)
        val eidFitr2026 = NudgeEventsRepository.getEventsForDate(2026, 3, 20)
        assertTrue(eidFitr2026.any { it.name.contains("Eid al-Fitr") && it.category == EventCategory.RELIGIOUS_FESTIVAL })

        // Eid al-Adha 2026 (May 27)
        val eidAdha2026 = NudgeEventsRepository.getEventsForDate(2026, 5, 27)
        assertTrue(eidAdha2026.any { it.name.contains("Eid al-Adha") && it.category == EventCategory.RELIGIOUS_FESTIVAL })

        // Buddha Purnima 2026 (May 2)
        val buddha2026 = NudgeEventsRepository.getEventsForDate(2026, 5, 2)
        assertTrue(buddha2026.any { it.name.contains("Buddha Purnima") && it.category == EventCategory.RELIGIOUS_FESTIVAL })

        // Mahavir Jayanti 2026 (Mar 31)
        val mahavir2026 = NudgeEventsRepository.getEventsForDate(2026, 3, 31)
        assertTrue(mahavir2026.any { it.name.contains("Mahavir") && it.category == EventCategory.RELIGIOUS_FESTIVAL })

        // Guru Nanak Gurpurab 2026 (Nov 24)
        val guruNanak2026 = NudgeEventsRepository.getEventsForDate(2026, 11, 24)
        assertTrue(guruNanak2026.any { it.name.contains("Guru Nanak") && it.category == EventCategory.RELIGIOUS_FESTIVAL })
    }

    @Test
    fun testImportantAwarenessDays() {
        // World Cancer Day (Feb 4)
        val cancerDay = NudgeEventsRepository.getEventsForDate(2026, 2, 4)
        assertTrue(cancerDay.any { it.name.contains("Cancer") && it.category == EventCategory.AWARENESS_DAY })

        // World Mental Health Day (Oct 10)
        val mentalHealth = NudgeEventsRepository.getEventsForDate(2026, 10, 10)
        assertTrue(mentalHealth.any { it.name.contains("Mental Health") && it.category == EventCategory.AWARENESS_DAY })

        // World Heart Day (Sep 29)
        val heartDay = NudgeEventsRepository.getEventsForDate(2026, 9, 29)
        assertTrue(heartDay.any { it.name.contains("Heart") && it.category == EventCategory.AWARENESS_DAY })
    }

    @Test
    fun testMultipleEventsOnSameDate() {
        // Oct 2: Mahatma Gandhi Jayanti + Lal Bahadur Shastri Jayanti + International Day of Non-Violence
        val oct2Events = NudgeEventsRepository.getEventsForDate(2026, 10, 2)
        assertTrue(oct2Events.size >= 2)
        assertTrue(oct2Events.any { it.name.contains("Mahatma Gandhi") })
        assertTrue(oct2Events.any { it.name.contains("Non-Violence") })

        // Dec 25: Christmas + Good Governance Day
        val dec25Events = NudgeEventsRepository.getEventsForDate(2026, 12, 25)
        assertTrue(dec25Events.size >= 2)
        assertTrue(dec25Events.any { it.name.contains("Christmas") })
        assertTrue(dec25Events.any { it.name.contains("Good Governance") })
    }

    @Test
    fun testHasEventsOnDateAndCalendarQuery() {
        assertTrue(NudgeEventsRepository.hasEventsOnDate(2026, 8, 15))
        assertFalse(NudgeEventsRepository.hasEventsOnDate(2026, 8, 3)) // Normal date with no events

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JANUARY) // 0-based
            set(Calendar.DAY_OF_MONTH, 26)
        }
        val calEvents = NudgeEventsRepository.getEventsForCalendar(cal)
        assertTrue(calEvents.isNotEmpty())
        assertEquals("Republic Day of India", calEvents.first { it.name.contains("Republic") }.name)
    }

    @Test
    fun testHinduCalendarCalculator() {
        // Test calculations for sample dates
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.AUGUST)
            set(Calendar.DAY_OF_MONTH, 15)
        }
        val info = com.example.gentlenudge.data.events.HinduCalendarCalculator.calculateForCalendar(cal)
        assertTrue(info.masaName.isNotEmpty())
        assertTrue(info.tithiName.isNotEmpty())
        assertTrue(info.paksha == "Shukla Paksha" || info.paksha == "Krishna Paksha")
        assertEquals(2083, info.vikramSamvat)
        assertTrue(info.rituName.isNotEmpty())
    }

    @Test
    fun testCategorySpecificEventQueries() {
        // Diwali 2026 (Nov 8)
        val hinduEvents = NudgeEventsRepository.getHinduEventsForDate(2026, 11, 8)
        assertTrue(hinduEvents.isNotEmpty())
        assertTrue(hinduEvents.all { it.category == EventCategory.HINDU_FESTIVAL })
        assertTrue(NudgeEventsRepository.hasHinduEventOnDate(2026, 11, 8))

        // Earth Day 2026 (Apr 22)
        val intlEvents = NudgeEventsRepository.getInternationalAndWorldEventsForDate(2026, 4, 22)
        assertTrue(intlEvents.isNotEmpty())
        assertTrue(intlEvents.all { it.category != EventCategory.HINDU_FESTIVAL })
        assertTrue(NudgeEventsRepository.hasInternationalEventOnDate(2026, 4, 22))

        // On an international day, hindu events query should not return international events
        val intlOnlyDateHinduEvents = NudgeEventsRepository.getHinduEventsForDate(2026, 4, 22)
        assertFalse(intlOnlyDateHinduEvents.any { it.name.contains("Earth Day") })
    }

    @Test
    fun testIndianEventsCoverageAcross202620272028() {
        // 2026 checks
        val diwali2026 = NudgeEventsRepository.getEventsForDate(2026, 11, 8)
        assertTrue(diwali2026.any { it.name.contains("Diwali") })
        val janmashtami2026 = NudgeEventsRepository.getEventsForDate(2026, 9, 4)
        assertTrue(janmashtami2026.any { it.name.contains("Janmashtami") })
        val rathYatra2026 = NudgeEventsRepository.getEventsForDate(2026, 7, 16)
        assertTrue(rathYatra2026.any { it.name.contains("Ratha Yatra") })
        val tulsiVivah2026 = NudgeEventsRepository.getEventsForDate(2026, 11, 20)
        assertTrue(tulsiVivah2026.any { it.name.contains("Tulsi Vivah") })
        val parsi2026 = NudgeEventsRepository.getEventsForDate(2026, 8, 15)
        assertTrue(parsi2026.any { it.name.contains("Parsi New Year") })

        // 2027 checks
        val diwali2027 = NudgeEventsRepository.getEventsForDate(2027, 10, 29)
        assertTrue(diwali2027.any { it.name.contains("Diwali") })
        val holi2027 = NudgeEventsRepository.getEventsForDate(2027, 3, 23)
        assertTrue(holi2027.any { it.name.contains("Holi") })
        val janmashtami2027 = NudgeEventsRepository.getEventsForDate(2027, 8, 25)
        assertTrue(janmashtami2027.any { it.name.contains("Janmashtami") })
        val eidFitr2027 = NudgeEventsRepository.getEventsForDate(2027, 3, 10)
        assertTrue(eidFitr2027.any { it.name.contains("Eid al-Fitr") })
        val guruNanak2027 = NudgeEventsRepository.getEventsForDate(2027, 11, 14)
        assertTrue(guruNanak2027.any { it.name.contains("Guru Nanak") })

        // 2028 checks
        val diwali2028 = NudgeEventsRepository.getEventsForDate(2028, 11, 17)
        assertTrue(diwali2028.any { it.name.contains("Diwali") })
        val holi2028 = NudgeEventsRepository.getEventsForDate(2028, 3, 12)
        assertTrue(holi2028.any { it.name.contains("Holi") })
        val janmashtami2028 = NudgeEventsRepository.getEventsForDate(2028, 8, 13)
        assertTrue(janmashtami2028.any { it.name.contains("Janmashtami") })
        val ganesh2028 = NudgeEventsRepository.getEventsForDate(2028, 8, 24)
        assertTrue(ganesh2028.any { it.name.contains("Ganesh Chaturthi") })
        val mahavir2028 = NudgeEventsRepository.getEventsForDate(2028, 4, 8)
        assertTrue(mahavir2028.any { it.name.contains("Mahavir Jayanti") })
        val buddha2028 = NudgeEventsRepository.getEventsForDate(2028, 5, 8)
        assertTrue(buddha2028.any { it.name.contains("Buddha Purnima") })

        // Fixed Indian events across multiple years
        for (year in listOf(2026, 2027, 2028)) {
            val lohri = NudgeEventsRepository.getEventsForDate(year, 1, 13)
            assertEquals(1, lohri.count { it.name.contains("Lohri") })

            val sankranti = NudgeEventsRepository.getEventsForDate(year, 1, 14)
            assertEquals(1, sankranti.count { it.name.contains("Sankranti") })

            val solarNY = NudgeEventsRepository.getEventsForDate(year, 4, 14)
            assertEquals(1, solarNY.count { it.name.contains("Puthandu") || it.name.contains("Vishu") })

            val navroz = NudgeEventsRepository.getEventsForDate(year, 3, 21)
            assertEquals(1, navroz.count { it.name.contains("Navroz") })

            val vishwakarma = NudgeEventsRepository.getEventsForDate(year, 9, 17)
            assertEquals(1, vishwakarma.count { it.name.contains("Vishwakarma") })
        }
    }

    @Test
    fun testDatesInspection() {
        // Specific user-requested dates verification
        // Jan 22, 2027: Pausha Purnima
        val jan22_2027 = NudgeEventsRepository.getHinduEventsForDate(2027, 1, 22)
        assertTrue("Expected Pausha Purnima on Jan 22, 2027", jan22_2027.any { it.name == "Pausha Purnima" })

        // Feb 20, 2027: Magha Purnima
        val feb20_2027 = NudgeEventsRepository.getHinduEventsForDate(2027, 2, 20)
        assertTrue("Expected Magha Purnima on Feb 20, 2027", feb20_2027.any { it.name == "Magha Purnima" })

        // Aug 4, 2027: Hariyali Teej
        val aug4_2027 = NudgeEventsRepository.getHinduEventsForDate(2027, 8, 4)
        assertTrue("Expected Hariyali Teej on Aug 4, 2027", aug4_2027.any { it.name == "Hariyali Teej" })

        // Sep 3, 2027: Hartalika Teej
        val sep3_2027 = NudgeEventsRepository.getHinduEventsForDate(2027, 9, 3)
        assertTrue("Expected Hartalika Teej on Sep 3, 2027", sep3_2027.any { it.name == "Hartalika Teej" })

        // Nov 2-5, 2027: 4-day Chhath Puja sequence
        val nov2_2027 = NudgeEventsRepository.getHinduEventsForDate(2027, 11, 2)
        assertTrue(nov2_2027.any { it.name == "Chhath Puja — Nahay Khay" })
        val nov3_2027 = NudgeEventsRepository.getHinduEventsForDate(2027, 11, 3)
        assertTrue(nov3_2027.any { it.name == "Chhath Puja — Kharna / Lohanda" })
        val nov4_2027 = NudgeEventsRepository.getHinduEventsForDate(2027, 11, 4)
        assertTrue(nov4_2027.any { it.name == "Chhath Puja — Sandhya Arghya" })
        val nov5_2027 = NudgeEventsRepository.getHinduEventsForDate(2027, 11, 5)
        assertTrue(nov5_2027.any { it.name == "Chhath Puja — Usha Arghya / Parana" })

        // Nov 13, 2027: Dev Deepawali
        val nov13_2027 = NudgeEventsRepository.getHinduEventsForDate(2027, 11, 13)
        assertTrue("Expected Dev Deepawali on Nov 13, 2027", nov13_2027.any { it.name == "Dev Deepawali" })

        // Nov 14, 2027: Kartik Purnima
        val nov14_2027 = NudgeEventsRepository.getHinduEventsForDate(2027, 11, 14)
        assertTrue("Expected Kartik Purnima on Nov 14, 2027", nov14_2027.any { it.name == "Kartik Purnima" })

        // 2026 Chhath sequence
        assertTrue(NudgeEventsRepository.getHinduEventsForDate(2026, 11, 13).any { it.name == "Chhath Puja — Nahay Khay" })
        assertTrue(NudgeEventsRepository.getHinduEventsForDate(2026, 11, 14).any { it.name == "Chhath Puja — Kharna / Lohanda" })
        assertTrue(NudgeEventsRepository.getHinduEventsForDate(2026, 11, 15).any { it.name == "Chhath Puja — Sandhya Arghya" })
        assertTrue(NudgeEventsRepository.getHinduEventsForDate(2026, 11, 16).any { it.name == "Chhath Puja — Usha Arghya / Parana" })

        // 2028 Chhath sequence
        assertTrue(NudgeEventsRepository.getHinduEventsForDate(2028, 11, 21).any { it.name == "Chhath Puja — Nahay Khay" })
        assertTrue(NudgeEventsRepository.getHinduEventsForDate(2028, 11, 22).any { it.name == "Chhath Puja — Kharna / Lohanda" })
        assertTrue(NudgeEventsRepository.getHinduEventsForDate(2028, 11, 23).any { it.name == "Chhath Puja — Sandhya Arghya" })
        assertTrue(NudgeEventsRepository.getHinduEventsForDate(2028, 11, 24).any { it.name == "Chhath Puja — Usha Arghya / Parana" })

        // Purnima & Amavasya additions across 2026-2028
        assertTrue(NudgeEventsRepository.getHinduEventsForDate(2026, 1, 3).any { it.name == "Pausha Purnima" })
        assertTrue(NudgeEventsRepository.getHinduEventsForDate(2026, 2, 1).any { it.name == "Magha Purnima" })
        assertTrue(NudgeEventsRepository.getHinduEventsForDate(2026, 8, 12).any { it.name == "Hariyali Amavasya" })
        assertTrue(NudgeEventsRepository.getHinduEventsForDate(2026, 12, 23).any { it.name == "Dattatreya Jayanti" })

        assertTrue(NudgeEventsRepository.getHinduEventsForDate(2027, 8, 2).any { it.name == "Hariyali Amavasya" })
        assertTrue(NudgeEventsRepository.getHinduEventsForDate(2027, 12, 13).any { it.name == "Dattatreya Jayanti" })

        assertTrue(NudgeEventsRepository.getHinduEventsForDate(2028, 1, 11).any { it.name == "Pausha Purnima" })
        assertTrue(NudgeEventsRepository.getHinduEventsForDate(2028, 2, 10).any { it.name == "Magha Purnima" })
        assertTrue(NudgeEventsRepository.getHinduEventsForDate(2028, 7, 22).any { it.name == "Hariyali Amavasya" })
        assertTrue(NudgeEventsRepository.getHinduEventsForDate(2028, 12, 31).any { it.name == "Dattatreya Jayanti" })
        // Bathukamma (Telangana state floral festival finale on Maha Ashtami)
        assertTrue(NudgeEventsRepository.getEventsForDate(2026, 10, 19).any { it.name == "Saddula Bathukamma" })
        assertTrue(NudgeEventsRepository.getEventsForDate(2027, 10, 8).any { it.name == "Saddula Bathukamma" })
        assertTrue(NudgeEventsRepository.getEventsForDate(2028, 10, 26).any { it.name == "Saddula Bathukamma" })
    }
}
