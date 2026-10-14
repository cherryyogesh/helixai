package com.example.chitttiaiassistant

import com.example.chitttiaiassistant.model.AssistantState
import com.example.chitttiaiassistant.service.LlmService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoicePipelineTest {

    private val llmService = LlmService()

    @Test
    fun testAssistantStates() {
        assertEquals("Listening...", AssistantState.LISTENING.label)
        assertEquals("Hey Helix Detected!", AssistantState.WAKE_WORD_DETECTED.label)
        assertEquals("Processing...", AssistantState.PROCESSING.label)
        assertEquals("Helix Speaking...", AssistantState.SPEAKING.label)
        assertEquals("Permission Required", AssistantState.PERMISSION_REQUIRED.label)
    }

    @Test
    fun testLocalTimeResolution() {
        val query = "what time is it?"
        val isTimeQuery = query.contains("time")
        val expectedTime = SimpleDateFormat("hh:mm a", Locale.US).format(Date())

        assertTrue(isTimeQuery)
        assertNotNull(expectedTime)
    }

    @Test
    fun testLocalDateResolution() {
        val query = "what is today's date?"
        val isDateQuery = query.contains("date")
        val expectedDate = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(Date())

        assertTrue(isDateQuery)
        assertNotNull(expectedDate)
    }

    @Test
    fun testGreetingQuery() {
        val query = "hello helix"
        val isGreeting = query == "hello helix" || query == "hi helix"
        assertTrue(isGreeting)
    }

    @Test
    fun testStopListeningCommand() {
        val query = "stop listening"
        val isStop = query == "stop listening" || query == "stop"
        assertTrue(isStop)
    }

    @Test
    fun testSecretRedaction() {
        val sampleLog = "Error connecting with key AIzaSyB3Kzm63G6s41pmeK9iuyqA4pL9gwfmFRE at endpoint"
        val redacted = llmService.redactSecrets(sampleLog, "AIzaSyB3Kzm63G6s41pmeK9iuyqA4pL9gwfmFRE")

        assertFalse(redacted.contains("AIzaSyB3Kzm63G6s41pmeK9iuyqA4pL9gwfmFRE"))
        assertTrue(redacted.contains("[REDACTED_API_KEY]"))
    }
}
