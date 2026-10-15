package com.example.chitttiaiassistant

import com.example.chitttiaiassistant.service.LlmProvider
import com.example.chitttiaiassistant.service.LlmService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmServiceTest {

    private val llmService = LlmService()

    @Test
    fun testProviderRecognitionHint() {
        assertEquals(LlmProvider.GEMINI, llmService.detectProviderHint("AIzaSyB3Kzm63G6s41pmeK9iuyqA4pL9gwfmFRE"))
        assertEquals(LlmProvider.CLAUDE, llmService.detectProviderHint("sk-ant-api03-samplekey"))
        assertEquals(LlmProvider.OPENAI, llmService.detectProviderHint("sk-proj-samplekey123456789"))
    }

    @Test
    fun testSha256KeyFingerprint() {
        val fp1 = llmService.computeKeyFingerprint("my-secret-key-1")
        val fp2 = llmService.computeKeyFingerprint("my-secret-key-1")
        val fp3 = llmService.computeKeyFingerprint("my-secret-key-2")

        assertEquals(fp1, fp2)
        assertTrue(fp1.isNotEmpty())
        assertFalse(fp1 == fp3)
    }

    @Test
    fun testFakeAndDemoKeyRejection() = runBlocking {
        val fakeResult = llmService.testConnection(LlmProvider.GEMINI, "demo", "")
        assertEquals(com.example.chitttiaiassistant.service.LlmConnectionStatus.INVALID_KEY, fakeResult.status)
        assertTrue(fakeResult.userMessage.contains("Demo and placeholder keys are not allowed"))
    }

    @Test
    fun testMissingKeyRejection() = runBlocking {
        val missingResult = llmService.testConnection(LlmProvider.OPENAI, "", "")
        assertEquals(com.example.chitttiaiassistant.service.LlmConnectionStatus.NOT_CONFIGURED, missingResult.status)
        assertEquals("Enter an API key.", missingResult.userMessage)
    }

    @Test
    fun testSecretRedaction() {
        val sampleText = "Request failed for key AIzaSyB3Kzm63G6s41pmeK9iuyqA4pL9gwfmFRE at endpoint."
        val redacted = llmService.redactSecrets(sampleText, "AIzaSyB3Kzm63G6s41pmeK9iuyqA4pL9gwfmFRE")

        assertFalse(redacted.contains("AIzaSyB3Kzm63G6s41pmeK9iuyqA4pL9gwfmFRE"))
        assertTrue(redacted.contains("[REDACTED_API_KEY]"))
    }
}
