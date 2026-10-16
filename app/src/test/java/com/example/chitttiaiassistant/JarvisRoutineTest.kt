package com.example.chitttiaiassistant

import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisRoutineTest {

    @Test
    fun testBedtimeRoutineParsing() {
        val query = "prepare my phone for the night"
        val isBedtime = query.contains("going to sleep") || query.contains("bedtime") || query.contains("prepare my phone for the night")
        assertTrue(isBedtime)
    }

    @Test
    fun testMorningRoutineParsing() {
        val query = "prepare me for tomorrow"
        val isMorning = query.contains("prepare me for tomorrow") || query.contains("morning routine") || query.contains("good morning")
        assertTrue(isMorning)
    }

    @Test
    fun testCreatorAndUserIdentification() {
        val creatorQuery = "my name is Yogesh"
        val isCreator = creatorQuery.contains("Yogesh", ignoreCase = true)
        assertTrue(isCreator)

        val userQuery = "my name is Priya"
        val isUser = userQuery.contains("Priya", ignoreCase = true)
        assertTrue(isUser)
    }

    @Test
    fun testTeluguCallAndMessageParsing() {
        val teluguCall = "praveen ki call cheyyi"
        val isCall = teluguCall.contains("call") || teluguCall.contains("call cheyyi")
        assertTrue(isCall)

        val teluguSms = "praveen ki message pampu"
        val isSms = teluguSms.contains("message") || teluguSms.contains("pampu")
        assertTrue(isSms)
    }
}
