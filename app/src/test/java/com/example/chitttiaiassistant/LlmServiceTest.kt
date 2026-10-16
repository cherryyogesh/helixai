package com.example.chitttiaiassistant

import com.example.chitttiaiassistant.service.LlmConnectionStatus
import com.example.chitttiaiassistant.service.LlmProvider
import com.example.chitttiaiassistant.service.LlmService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmServiceTest {

    private val llmService = LlmService()

    @Test
    fun testServerManagedAiConnection() = runBlocking {
        val serverResult = llmService.testServerConnection("http://10.0.2.2:8080")
        assertEquals(LlmConnectionStatus.CONNECTED, serverResult.status)
        assertTrue(serverResult.userMessage.contains("Helix"))
    }

    @Test
    fun testZeroKeyFrontendRequirement() = runBlocking {
        // App query operates through backend server abstraction without needing a frontend API key
        val (response, source) = llmService.queryLlm(LlmProvider.GEMINI, "", "", "What is the capital of France?")
        assertTrue(response.isNotEmpty())
        assertTrue(source.contains("Helix Server AI"))
    }

    @Test
    fun testSecretRedaction() {
        val sampleText = "Request failed for key AIzaSyB3Kzm63G6s41pmeK9iuyqA4pL9gwfmFRE at endpoint."
        val redacted = llmService.redactSecrets(sampleText, "AIzaSyB3Kzm63G6s41pmeK9iuyqA4pL9gwfmFRE")

        assertFalse(redacted.contains("AIzaSyB3Kzm63G6s41pmeK9iuyqA4pL9gwfmFRE"))
        assertTrue(redacted.contains("[REDACTED_API_KEY]"))
    }
}
