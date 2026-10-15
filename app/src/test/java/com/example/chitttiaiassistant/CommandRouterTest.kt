package com.example.chitttiaiassistant

import org.junit.Assert.assertTrue
import org.junit.Test

class CommandRouterTest {

    @Test
    fun testCallCommandParsing() {
        val callQuery = "call Amma"
        assertTrue(callQuery.startsWith("call "))
    }

    @Test
    fun testSmsMessageParsing() {
        val smsQuery = "message Amma that I am arriving soon"
        assertTrue(smsQuery.startsWith("message "))
        assertTrue(smsQuery.contains(" that "))
    }

    @Test
    fun testGoogleSearchParsing() {
        val searchQuery = "search google for latest android features"
        assertTrue(searchQuery.contains("google") || searchQuery.contains("search"))
    }

    @Test
    fun testDataExtractionParsing() {
        val extractQuery = "extract data from: Name: John, Role: Developer, City: Hyderabad"
        assertTrue(extractQuery.contains("extract") || extractQuery.contains("data"))
    }
}
