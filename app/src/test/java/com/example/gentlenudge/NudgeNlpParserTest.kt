package com.example.gentlenudge

import com.example.gentlenudge.nlp.NudgeNlpParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class NudgeNlpParserTest {

    @Test
    fun testUserCase1_RemindMeTomorrowAt5PMToMeetHim() {
        val result = NudgeNlpParser.parse("Remind me tomorrow at 5 PM to meet him")
        assertEquals("Meet him", result.cleanTitle)
        assertEquals("Tomorrow", result.extractedDate)
        assertEquals("5:00 PM", result.extractedTime)
        assertFalse(result.isAmbiguousAmPm)
        assertTrue(result.hasExplicitDateTime)
    }

    @Test
    fun testUserCase2_CallTheDoctorTomorrowAt3PM() {
        val result = NudgeNlpParser.parse("Call the doctor tomorrow at 3 PM")
        assertEquals("Call the doctor", result.cleanTitle)
        assertEquals("Tomorrow", result.extractedDate)
        assertEquals("3:00 PM", result.extractedTime)
        assertFalse(result.isAmbiguousAmPm)
    }

    @Test
    fun testUserCase3_ReadTonightAt9PM() {
        val result = NudgeNlpParser.parse("Read tonight at 9 PM")
        assertEquals("Read", result.cleanTitle)
        assertEquals("Today", result.extractedDate)
        assertEquals("9:00 PM", result.extractedTime)
        assertFalse(result.isAmbiguousAmPm)
    }

    @Test
    fun testUserCase4_BuyGroceriesAt730TomorrowEvening() {
        val result = NudgeNlpParser.parse("Buy groceries at 7:30 tomorrow evening")
        assertEquals("Buy groceries", result.cleanTitle)
        assertEquals("Tomorrow", result.extractedDate)
        assertEquals("7:30 PM", result.extractedTime)
        assertEquals("Shopping", result.extractedCategory)
    }

    @Test
    fun testUserCase5_BuyGroceriesTomorrowEvening() {
        val result = NudgeNlpParser.parse("Buy groceries tomorrow evening")
        assertEquals("Buy groceries", result.cleanTitle)
        assertEquals("Tomorrow", result.extractedDate)
        assertEquals("6:00 PM", result.extractedTime)
        assertEquals("Shopping", result.extractedCategory)
    }

    @Test
    fun testUserCase6_WakeMeUpAt6AMTomorrow() {
        val result = NudgeNlpParser.parse("Wake me up at 6 AM tomorrow")
        assertEquals("Wake me up", result.cleanTitle)
        assertEquals("Tomorrow", result.extractedDate)
        assertEquals("6:00 AM", result.extractedTime)
        assertFalse(result.isAmbiguousAmPm)
    }

    @Test
    fun testUserCase7_RemindMeIn30MinutesToCheckTheMilk() {
        val result = NudgeNlpParser.parse("Remind me in 30 minutes to check the milk")
        assertEquals("Check the milk", result.cleanTitle)
        assertEquals("Today", result.extractedDate)
        assertTrue(result.hasExplicitDateTime)
        assertNotNull(result.extractedTime)
    }

    @Test
    fun testUserCase8_RemindMeAfter30MinutesToCheckTheMilk() {
        val result = NudgeNlpParser.parse("Remind me after 30 minutes to check the milk")
        assertEquals("Check the milk", result.cleanTitle)
        assertEquals("Today", result.extractedDate)
        assertTrue(result.hasExplicitDateTime)
        assertNotNull(result.extractedTime)
    }

    @Test
    fun testUserCase9_RemindMeNextMondayAt10AMToCallHim() {
        val result = NudgeNlpParser.parse("Remind me next Monday at 10 AM to call him")
        assertEquals("Call him", result.cleanTitle)
        assertEquals("Next Monday", result.extractedDate)
        assertEquals("10:00 AM", result.extractedTime)
        assertFalse(result.isAmbiguousAmPm)
    }

    @Test
    fun testUserCase10_WorkWithAINextWeekAt5PM() {
        val result = NudgeNlpParser.parse("Work with AI next week at 5 PM")
        assertEquals("Work with AI", result.cleanTitle)
        assertEquals("Next week", result.extractedDate)
        assertEquals("5:00 PM", result.extractedTime)
        assertEquals("Work", result.extractedCategory)
    }

    @Test
    fun testSpokenWords_FiveInTheEvening() {
        val result = NudgeNlpParser.parse("Remind me tomorrow at five in the evening to meet him")
        assertEquals("Meet him", result.cleanTitle)
        assertEquals("Tomorrow", result.extractedDate)
        assertEquals("5:00 PM", result.extractedTime)
    }

    @Test
    fun testSpokenWords_HalfPastFive() {
        val result = NudgeNlpParser.parse("Call doctor tomorrow at half past five in the evening")
        assertEquals("Call doctor", result.cleanTitle)
        assertEquals("Tomorrow", result.extractedDate)
        assertEquals("5:30 PM", result.extractedTime)
    }

    @Test
    fun testSpokenWords_InThirtyMinutes() {
        val result = NudgeNlpParser.parse("Remind me in thirty minutes to check the oven")
        assertEquals("Check the oven", result.cleanTitle)
        assertEquals("Today", result.extractedDate)
        assertTrue(result.hasExplicitDateTime)
    }

    @Test
    fun testSpokenWords_InHalfAnHour() {
        val result = NudgeNlpParser.parse("Remind me in half an hour to take a break")
        assertEquals("Take a break", result.cleanTitle)
        assertEquals("Today", result.extractedDate)
        assertTrue(result.hasExplicitDateTime)
    }

    @Test
    fun testExample_AmbiguousAmPm() {
        val result = NudgeNlpParser.parse("Remind me tomorrow at 6")
        assertEquals("Tomorrow", result.extractedDate)
        assertEquals("6:00 PM", result.extractedTime)
        assertEquals("6:00 AM", result.alternativeTime)
        assertTrue(result.isAmbiguousAmPm)
        assertEquals("Did you mean Tomorrow at 6:00 PM?", result.confirmationPrompt)
    }

    @Test
    fun testDatesAndTimesEverywhereInSentence() {
        val res1 = NudgeNlpParser.parse("Buy groceries on August 30th at 4:30 PM")
        assertEquals("Aug 30", res1.extractedDate)
        assertEquals("4:30 PM", res1.extractedTime)
        assertEquals("Shopping", res1.extractedCategory)
        assertEquals("Buy groceries", res1.cleanTitle)

        val res2 = NudgeNlpParser.parse("Tonight at 8:00 call Rahul")
        assertEquals("Today", res2.extractedDate)
        assertEquals("8:00 PM", res2.extractedTime)
        assertEquals("Call Rahul", res2.cleanTitle)

        val res3 = NudgeNlpParser.parse("Meeting Friday at 10 a.m.")
        assertEquals("Friday", res3.extractedDate)
        assertEquals("10:00 AM", res3.extractedTime)
        assertEquals("Work", res3.extractedCategory)
    }

    @Test
    fun testTypingTomorrowSingleWord() {
        val result = NudgeNlpParser.parse("tomorrow")
        assertNotNull(result)
        assertEquals("Tomorrow", result.extractedDate)
        assertTrue(result.cleanTitle.isNotBlank())
    }

    @Test
    fun testCallDentistTomorrowAt3PM() {
        val result = NudgeNlpParser.parse("Call dentist tomorrow at 3 PM")
        assertNotNull(result)
        assertEquals("Call dentist", result.cleanTitle)
        assertEquals("Tomorrow", result.extractedDate)
        assertEquals("3:00 PM", result.extractedTime)
    }
}
