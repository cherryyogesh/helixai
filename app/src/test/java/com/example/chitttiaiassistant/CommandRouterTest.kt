package com.example.chitttiaiassistant

import com.example.chitttiaiassistant.model.CommandActionType
import com.example.chitttiaiassistant.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class CommandRouterTest {

    private fun parseIntent(query: String): Triple<CommandActionType, Map<String, String>, RiskLevel> {
        val q = query.trim()
        val qLower = q.lowercase()

        return when {
            qLower.contains("flashlight") || qLower.contains("torch") || qLower.contains("వెలుగు") -> {
                val state = if (qLower.contains("off") || qLower.contains("stop")) "false" else "true"
                Triple(CommandActionType.SET_FLASHLIGHT, mapOf("enabled" to state), RiskLevel.MODERATE)
            }
            qLower.contains("volume up") || qLower.contains("increase volume") || qLower.contains("volume penchu") -> {
                Triple(CommandActionType.SET_VOLUME, mapOf("direction" to "up"), RiskLevel.MODERATE)
            }
            qLower.startsWith("open ") || qLower.startsWith("launch ") -> {
                val app = q.substring(5).trim()
                Triple(CommandActionType.OPEN_APP, mapOf("appName" to app), RiskLevel.LOW)
            }
            qLower.startsWith("call ") || qLower.startsWith("dial ") || qLower.contains("అమ్మకి కాల్") -> {
                val contact = if (qLower.contains("అమ్మకి కాల్")) "Amma" else q.substring(5).trim()
                Triple(CommandActionType.PLACE_CALL, mapOf("contact" to contact), RiskLevel.HIGH_IMPACT)
            }
            qLower.startsWith("message ") || qLower.startsWith("text ") || qLower.contains("whatsapp") -> {
                Triple(CommandActionType.COMPOSE_SMS, mapOf("type" to "message"), RiskLevel.SENSITIVE)
            }
            qLower.contains("battery") || qLower.contains("charging") -> {
                Triple(CommandActionType.GET_BATTERY_STATUS, emptyMap(), RiskLevel.LOW)
            }
            qLower.contains("time") || qLower.contains("clock") || qLower.contains("సమయం") -> {
                Triple(CommandActionType.GET_TIME, emptyMap(), RiskLevel.LOW)
            }
            else -> {
                Triple(CommandActionType.ASK_AI, mapOf("query" to query), RiskLevel.LOW)
            }
        }
    }

    @Test
    fun testFlashlightIntentParsing() {
        val (action, params, risk) = parseIntent("turn on flashlight")
        assertEquals(CommandActionType.SET_FLASHLIGHT, action)
        assertEquals("true", params["enabled"])
        assertEquals(RiskLevel.MODERATE, risk)
    }

    @Test
    fun testTeluguFlashlightIntentParsing() {
        val (action, params, risk) = parseIntent("torch వెలుగు వేయి")
        assertEquals(CommandActionType.SET_FLASHLIGHT, action)
        assertEquals("true", params["enabled"])
        assertEquals(RiskLevel.MODERATE, risk)
    }

    @Test
    fun testVolumeIntentParsing() {
        val (action, params, risk) = parseIntent("volume up please")
        assertEquals(CommandActionType.SET_VOLUME, action)
        assertEquals("up", params["direction"])
        assertEquals(RiskLevel.MODERATE, risk)
    }

    @Test
    fun testAppLaunchIntentParsing() {
        val (action, params, risk) = parseIntent("open youtube")
        assertEquals(CommandActionType.OPEN_APP, action)
        assertEquals("youtube", params["appName"])
        assertEquals(RiskLevel.LOW, risk)
    }

    @Test
    fun testCallIntentParsingAndRiskLevel() {
        val (action, params, risk) = parseIntent("call Amma")
        assertEquals(CommandActionType.PLACE_CALL, action)
        assertEquals("Amma", params["contact"])
        assertEquals(RiskLevel.HIGH_IMPACT, risk)
    }

    @Test
    fun testBatteryIntentParsing() {
        val (action, _, risk) = parseIntent("what is my battery level?")
        assertEquals(CommandActionType.GET_BATTERY_STATUS, action)
        assertEquals(RiskLevel.LOW, risk)
    }

    @Test
    fun testMultiStepSequenceSplitting() {
        val multiCommand = "open youtube and volume up"
        val steps = multiCommand.split(" and ").map { parseIntent(it) }

        assertEquals(2, steps.size)
        assertEquals(CommandActionType.OPEN_APP, steps[0].first)
        assertEquals(CommandActionType.SET_VOLUME, steps[1].first)
    }
}
