package com.example.chitttiaiassistant.service

import com.example.chitttiaiassistant.model.ActionResponseCard
import com.example.chitttiaiassistant.model.HourlyTemp
import com.example.chitttiaiassistant.repository.MemoryRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CommandResult(
    val spokenResponse: String,
    val card: ActionResponseCard = ActionResponseCard.None
)

class CommandRouter(
    private val phoneControlManager: PhoneControlManager,
    private val memoryRepository: MemoryRepository
) {

    private val llmService = LlmService()

    suspend fun processCommand(inputQuery: String): CommandResult {
        val query = inputQuery.lowercase().trim()

        // Local Processing 1: Time & Date (Processed locally without requiring LLM)
        if (query.contains("time") || query.contains("clock") || query.contains("సమయం")) {
            val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
            val card = ActionResponseCard.SystemInfoCard("Current Time", listOf("Time" to timeStr, "Time Zone" to "Local Time"))
            return CommandResult("It is $timeStr.", card)
        }

        if (query.contains("date") || query.contains("today's date") || query.contains("తేదీ")) {
            val dateStr = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(Date())
            val card = ActionResponseCard.SystemInfoCard("Today's Date", listOf("Date" to dateStr))
            return CommandResult("Today is $dateStr.", card)
        }

        // Local Processing 2: Greetings & Stop
        if (query == "hello helix" || query == "hi helix" || query == "hey helix" || query == "hello") {
            return CommandResult("Hello! I'm Helix, your personal AI voice assistant. How can I help you today?")
        }

        if (query == "stop listening" || query == "stop" || query == "cancel") {
            return CommandResult("Listening stopped.")
        }

        // Hardware Controls 1: Flashlight / Torch
        if (query.contains("flashlight") || query.contains("torch") || query.contains("light")) {
            if (query.contains("off") || query.contains("disable") || query.contains("stop")) {
                val card = phoneControlManager.toggleTorch(enable = false)
                return CommandResult("Done, torch is turned off.", card)
            } else {
                val card = phoneControlManager.toggleTorch(enable = true)
                return CommandResult("Done, torch is on.", card)
            }
        }

        // Hardware Controls 2: Volume Commands
        if (query.contains("volume up") || query.contains("increase volume") || query.contains("louder")) {
            val card = phoneControlManager.adjustVolume(increase = true)
            return CommandResult("Volume turned up.", card)
        }
        if (query.contains("volume down") || query.contains("decrease volume") || query.contains("lower volume") || query.contains("quieter")) {
            val card = phoneControlManager.adjustVolume(increase = false)
            return CommandResult("Volume turned down.", card)
        }
        if (query.contains("mute") || query.contains("silent")) {
            val card = phoneControlManager.setMute(mute = true)
            return CommandResult("Media volume muted.", card)
        }

        // Hardware Controls 3: Open App Commands
        if (query.startsWith("open ") || query.startsWith("launch ")) {
            val appName = query.replace("open ", "").replace("launch ", "").trim()
            val card = phoneControlManager.launchAppByName(appName)
            return if (card.success) {
                CommandResult("Opening ${card.appName} for you.", card)
            } else {
                CommandResult("I couldn't find ${card.appName} installed on your phone.", card)
            }
        }

        // Hardware Controls 4: Battery & Device Status
        if (query.contains("battery") || query.contains("charging")) {
            val card = phoneControlManager.getBatteryStatus()
            return CommandResult("Checking battery status for you.", card)
        }
        if (query.contains("device info") || query.contains("phone info")) {
            val card = phoneControlManager.getDeviceStatus()
            return CommandResult("Here is your device information.", card)
        }

        // Hardware Controls 5: Calls
        if (query.startsWith("call ") || query.startsWith("dial ")) {
            val contactName = query.replace("call ", "").replace("dial ", "").trim()
            val memory = memoryRepository.findMemory(contactName)
            val nameToCall = memory?.value ?: contactName
            val card = phoneControlManager.makeCall(nameToCall)
            return CommandResult("Placing a call to $contactName.", card)
        }

        // Hardware Controls 6: Messages / SMS / WhatsApp
        if (query.contains("whatsapp")) {
            val raw = query.replace("whatsapp", "").replace("message", "").replace("send", "").trim()
            val parts = raw.split(" that ", limit = 2)
            val contact = parts.getOrNull(0) ?: "Contact"
            val textMsg = if (parts.size > 1) parts[1] else "Hello from Helix!"
            val card = phoneControlManager.openWhatsAppMessage(contact, textMsg)
            return CommandResult("Opening WhatsApp with your message to $contact: '$textMsg'.", card)
        }
        if (query.startsWith("message ") || query.startsWith("text ") || query.startsWith("sms ")) {
            val raw = query.replace("message ", "").replace("text ", "").replace("sms ", "").trim()
            val parts = raw.split(" that ", limit = 2)
            val contact = parts.getOrNull(0) ?: "Contact"
            val textMsg = if (parts.size > 1) parts[1] else "Hello from Helix!"
            val card = phoneControlManager.composeSms(contact, textMsg)
            return CommandResult("Drafted message to $contact: '$textMsg'. Should I send it?", card)
        }

        // Hardware Controls 7: Settings Panel
        if (query.contains("wifi") || query.contains("wi-fi")) {
            phoneControlManager.openSettingsPanel("wifi")
            return CommandResult("Opening Wi-Fi settings.")
        }
        if (query.contains("bluetooth")) {
            phoneControlManager.openSettingsPanel("bluetooth")
            return CommandResult("Opening Bluetooth settings.")
        }

        // Weather Command
        if (query.contains("weather") || query.contains("temperature") || query.contains("forecast")) {
            val weatherCard = ActionResponseCard.WeatherCard(
                city = "Hyderabad",
                temp = "31°C",
                condition = "Mostly Sunny",
                highTemp = "34°C",
                lowTemp = "22°C",
                hourlyForecast = listOf(
                    HourlyTemp("12 PM", "31°C", "☀️"),
                    HourlyTemp("3 PM", "34°C", "☀️"),
                    HourlyTemp("6 PM", "29°C", "⛅"),
                    HourlyTemp("9 PM", "25°C", "🌙")
                )
            )
            return CommandResult("Looks sunny in Hyderabad today, around 31 degrees with clear skies.", weatherCard)
        }

        // Memory Queries / Store
        if (query.startsWith("remember that ") || query.startsWith("remember ")) {
            val content = query.replace("remember that ", "").replace("remember ", "")
            memoryRepository.addMemory("Fact", content)
            val card = ActionResponseCard.MemoryCard("Saved Fact", content, "Added to Memory")
            return CommandResult("Got it! I will remember '$content'.", card)
        }
        if (query.contains("what do you remember") || query.contains("my memories")) {
            val list = memoryRepository.memories.value
            val summary = if (list.isEmpty()) "I don't have any saved memories yet." else "I remember ${list.size} key facts about you, including your name and nicknames."
            val card = ActionResponseCard.MemoryCard("Memory Count", "${list.size} items", "Viewed Memory")
            return CommandResult(summary, card)
        }

        // Multi-LLM AI Response
        if (!memoryRepository.isLlmVerified()) {
            return CommandResult("LLM connection not verified. Please test and save your API key in Settings.")
        }

        return querySelectedLlm(inputQuery)
    }

    private suspend fun querySelectedLlm(rawQuery: String): CommandResult {
        val provider = memoryRepository.getLlmProvider()
        val apiKey = memoryRepository.getApiKey()
        val endpointUrl = memoryRepository.getEndpointUrl()

        val memoryList = memoryRepository.memories.value.joinToString("; ") { "${it.key}: ${it.value}" }
        val promptText = "You are Helix, a warm, friendly, humanized AI personal voice assistant. User context memories: [$memoryList]. Answer the user's question accurately in 1 to 3 short sentences so it sounds natural when spoken. Question: $rawQuery"

        val (responseText, sourceName) = llmService.queryLlm(provider, apiKey, endpointUrl, promptText)

        val card = ActionResponseCard.QaCard(
            question = rawQuery,
            answer = responseText,
            source = sourceName
        )
        return CommandResult(spokenResponse = responseText, card = card)
    }
}
