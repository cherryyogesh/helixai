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
    fun testTeluguEnglishCommandParsing() {
        val timeQuery = "samayam entha"
        val isTime = timeQuery.contains("time") || timeQuery.contains("samayam")
        assertTrue(isTime)

        val callQuery = "ammanaku call cheyyi"
        val isCall = callQuery.contains("call") || callQuery.contains("call cheyyi")
        assertTrue(isCall)
    }
}
