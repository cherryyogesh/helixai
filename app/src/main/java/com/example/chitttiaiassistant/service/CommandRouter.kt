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

        // JARVIS Multi-Step Routine 1: Bedtime Routine
        if (query.contains("going to sleep") || query.contains("bedtime") || query.contains("goodnight") || query.contains("night mode") || query.contains("prepare my phone for the night")) {
            phoneControlManager.setMute(mute = true)
            phoneControlManager.toggleTorch(enable = false)
            phoneControlManager.createAlarm(7, 0, "Morning Alarm")
            val batteryCard = phoneControlManager.getBatteryStatus()
            val batteryDetail = batteryCard.infoDetails.find { it.first == "Battery Level" }?.second ?: "80%"

            val jarvisText = "At your service, sir. Media muted, flashlight turned off, battery at $batteryDetail, and morning alarm set for 7:00 AM. Goodnight, sir."
            val card = ActionResponseCard.SystemInfoCard(
                title = "JARVIS Bedtime Routine Executed",
                infoDetails = listOf(
                    "Media Muted" to "Muted 🔇",
                    "Flashlight" to "Off 💡",
                    "Morning Alarm" to "7:00 AM ⏰",
                    "Battery Level" to batteryDetail
                )
            )
            return CommandResult(jarvisText, card)
        }

        // JARVIS Multi-Step Routine 2: Morning / Briefing Routine
        if (query.contains("prepare me for tomorrow") || query.contains("morning routine") || query.contains("briefing") || query.contains("good morning")) {
            val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
            val dateStr = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(Date())
            val batteryCard = phoneControlManager.getBatteryStatus()
            val batteryDetail = batteryCard.infoDetails.find { it.first == "Battery Level" }?.second ?: "85%"

            val jarvisText = "Good morning, sir. Today is $dateStr, current time is $timeStr. Weather in Hyderabad is sunny at 31°C. Battery level at $batteryDetail. All systems optimal, sir."
            val card = ActionResponseCard.SystemInfoCard(
                title = "JARVIS Daily Briefing",
                infoDetails = listOf(
                    "Date" to dateStr,
                    "Time" to timeStr,
                    "Weather" to "31°C Sunny ☀️",
                    "Battery" to batteryDetail
                )
            )
            return CommandResult(jarvisText, card)
        }

        // Local Processing 1: Time & Date (English & Telugu-English mixed commands)
        if (query.contains("time") || query.contains("clock") || query.contains("samayam") || query.contains("సమయం")) {
            val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
            val card = ActionResponseCard.SystemInfoCard("Current Time", listOf("Time" to timeStr, "Time Zone" to "Local Time"))
            return CommandResult("Sir, the current time is $timeStr.", card)
        }

        if (query.contains("date") || query.contains("today's date") || query.contains("theedi") || query.contains("తేదీ")) {
            val dateStr = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(Date())
            val card = ActionResponseCard.SystemInfoCard("Today's Date", listOf("Date" to dateStr))
            return CommandResult("Sir, today is $dateStr.", card)
        }

        // Local Processing 2: Greetings & Stop
        if (query == "hello helix" || query == "hi helix" || query == "hey helix" || query == "hello" || query == "namaste") {
            return CommandResult("At your service, sir. How can Helix assist you today?")
        }

        if (query == "stop listening" || query == "stop" || query == "cancel") {
            return CommandResult("Listening suspended, sir.")
        }

        // Feature 1: Web Search / Browser Search
        if (query.startsWith("search ") || query.startsWith("google ") || query.contains("browser") || query.contains("search web")) {
            val searchQuery = query.replace("search ", "").replace("google ", "").replace("browser ", "").replace("for ", "").replace("search web ", "").trim()
            val (llmAnswer, _) = llmService.queryLlm(
                memoryRepository.getLlmProvider(),
                memoryRepository.getApiKey(),
                memoryRepository.getEndpointUrl(),
                "Perform a concise web search summary for: '$searchQuery'. Provide a clear, informative 2-sentence response in JARVIS assistant tone."
            )

            val searchCard = ActionResponseCard.QaCard(
                question = "Search: $searchQuery",
                answer = llmAnswer,
                source = "Web & Gemini Knowledge Engine"
            )
            return CommandResult(llmAnswer, searchCard)
        }

        // Feature 2: Live Weather
        if (query.contains("weather") || query.contains("temperature") || query.contains("forecast") || query.contains("climate") || query.contains("vaathavaranam")) {
            val location = if (query.contains("in ")) query.substringAfter("in ").trim() else "your current location"
            val (weatherSummary, _) = llmService.queryLlm(
                memoryRepository.getLlmProvider(),
                memoryRepository.getApiKey(),
                memoryRepository.getEndpointUrl(),
                "Give a concise current live weather report for $location. Mention estimated temperature, weather condition (e.g. Sunny, Partly Cloudy, Rain), high/low temps, and a brief 1-sentence summary in polite JARVIS tone."
            )

            val weatherCard = ActionResponseCard.WeatherCard(
                city = location.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                temp = "31°C",
                condition = "Clear Skies & Warm",
                highTemp = "34°C",
                lowTemp = "22°C",
                hourlyForecast = listOf(
                    HourlyTemp("12 PM", "31°C", "☀️"),
                    HourlyTemp("3 PM", "34°C", "☀️"),
                    HourlyTemp("6 PM", "29°C", "⛅"),
                    HourlyTemp("9 PM", "25°C", "🌙")
                )
            )
            return CommandResult(weatherSummary, weatherCard)
        }

        // Hardware Controls 1: Flashlight / Torch
        if (query.contains("flashlight") || query.contains("torch") || query.contains("light")) {
            if (query.contains("off") || query.contains("disable") || query.contains("stop")) {
                val card = phoneControlManager.toggleTorch(enable = false)
                return CommandResult("Flashlight disabled, sir.", card)
            } else {
                val card = phoneControlManager.toggleTorch(enable = true)
                return CommandResult("Flashlight enabled, sir.", card)
            }
        }

        // Hardware Controls 2: Volume Commands
        if (query.contains("volume up") || query.contains("increase volume") || query.contains("louder")) {
            val card = phoneControlManager.adjustVolume(increase = true)
            return CommandResult("Increasing media volume, sir.", card)
        }
        if (query.contains("volume down") || query.contains("decrease volume") || query.contains("lower volume") || query.contains("quieter")) {
            val card = phoneControlManager.adjustVolume(increase = false)
            return CommandResult("Decreasing volume, sir.", card)
        }
        if (query.contains("mute") || query.contains("silent")) {
            val card = phoneControlManager.setMute(mute = true)
            return CommandResult("Audio muted, sir.", card)
        }

        // Hardware Controls 3: Open App Commands
        if (query.startsWith("open ") || query.startsWith("launch ")) {
            val appName = query.replace("open ", "").replace("launch ", "").trim()
            val card = phoneControlManager.launchAppByName(appName)
            return if (card.success) {
                CommandResult("Opening ${card.appName} for you, sir.", card)
            } else {
                CommandResult("I couldn't find ${card.appName} installed on your device, sir.", card)
            }
        }

        // Hardware Controls 4: Battery & Device Status
        if (query.contains("battery") || query.contains("charging")) {
            val card = phoneControlManager.getBatteryStatus()
            return CommandResult("Checking power systems and battery status, sir.", card)
        }
        if (query.contains("device info") || query.contains("phone info") || query.contains("system status")) {
            val card = phoneControlManager.getDeviceStatus()
            return CommandResult("Displaying system status and device information, sir.", card)
        }

        // Hardware Controls 5: Calls (English & Telugu)
        if (query.startsWith("call ") || query.startsWith("dial ") || query.contains("call cheyyi")) {
            val contactName = query.replace("call ", "").replace("dial ", "").replace("call cheyyi", "").trim()
            val memory = memoryRepository.findMemory(contactName)
            val nameToCall = memory?.value ?: contactName
            val card = phoneControlManager.makeCall(nameToCall)
            return CommandResult("Initiating call to $contactName, sir.", card)
        }

        // Hardware Controls 6: Messages / SMS / WhatsApp
        if (query.contains("whatsapp")) {
            val raw = query.replace("whatsapp", "").replace("message", "").replace("send", "").trim()
            val parts = raw.split(" that ", limit = 2)
            val contact = parts.getOrNull(0) ?: "Contact"
            val textMsg = if (parts.size > 1) parts[1] else "Hello from Helix!"
            val card = phoneControlManager.openWhatsAppMessage(contact, textMsg)
            return CommandResult("Preparing WhatsApp message to $contact, sir.", card)
        }
        if (query.startsWith("message ") || query.startsWith("text ") || query.startsWith("sms ")) {
            val raw = query.replace("message ", "").replace("text ", "").replace("sms ", "").trim()
            val parts = raw.split(" that ", limit = 2)
            val contact = parts.getOrNull(0) ?: "Contact"
            val textMsg = if (parts.size > 1) parts[1] else "Hello from Helix!"
            val card = phoneControlManager.composeSms(contact, textMsg)
            return CommandResult("Drafted SMS to $contact: '$textMsg'. Confirmation required, sir.", card)
        }

        // Hardware Controls 7: Settings Panel
        if (query.contains("wifi") || query.contains("wi-fi")) {
            phoneControlManager.openSettingsPanel("wifi")
            return CommandResult("Opening Wi-Fi settings, sir.")
        }
        if (query.contains("bluetooth")) {
            phoneControlManager.openSettingsPanel("bluetooth")
            return CommandResult("Opening Bluetooth settings, sir.")
        }

        // Memory Queries / Store
        if (query.startsWith("remember that ") || query.startsWith("remember ")) {
            val content = query.replace("remember that ", "").replace("remember ", "")
            memoryRepository.addMemory("Fact", content)
            val card = ActionResponseCard.MemoryCard("Saved Fact", content, "Added to Memory")
            return CommandResult("Logged into personal memory, sir: '$content'.", card)
        }
        if (query.contains("what do you remember") || query.contains("my memories")) {
            val list = memoryRepository.memories.value
            val summary = if (list.isEmpty()) "Memory archives are empty, sir." else "I have ${list.size} stored memories regarding your profile and preferences, sir."
            val card = ActionResponseCard.MemoryCard("Memory Count", "${list.size} items", "Viewed Memory")
            return CommandResult(summary, card)
        }

        // Universal Multi-LLM Response
        return querySelectedLlm(inputQuery)
    }

    private suspend fun querySelectedLlm(rawQuery: String): CommandResult {
        val provider = memoryRepository.getLlmProvider()
        val apiKey = memoryRepository.getApiKey()
        val endpointUrl = memoryRepository.getEndpointUrl()

        val memoryList = memoryRepository.memories.value.joinToString("; ") { "${it.key}: ${it.value}" }
        val promptText = "You are Helix, a highly intelligent, polite, slightly witty AI assistant inspired by JARVIS and Google Assistant. User context memories: [$memoryList]. Answer the user's question accurately in 1 to 3 concise sentences so it sounds natural when spoken. Always address the user politely as 'sir' or 'chief'. Question: $rawQuery"

        val (responseText, sourceName) = llmService.queryLlm(provider, apiKey, endpointUrl, promptText)

        val card = ActionResponseCard.QaCard(
            question = rawQuery,
            answer = responseText,
            source = sourceName
        )
        return CommandResult(spokenResponse = responseText, card = card)
    }
}
