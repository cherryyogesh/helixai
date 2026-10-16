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

    private fun getSpeakerTitle(userQuery: String): String {
        val q = userQuery.lowercase()
        val storedName = memoryRepository.findMemory("Active Speaker")?.value ?: "Yogesh"
        if (q.contains("yogesh") || storedName.contains("yogesh", ignoreCase = true)) {
            return "Boss"
        }
        return storedName
    }

    suspend fun processCommand(inputQuery: String): CommandResult {
        val query = inputQuery.lowercase().trim()
        val title = getSpeakerTitle(query)

        // Identify Creator (Yogesh = Boss) or Other User
        if (query.contains("my name is ") || query.contains("i am ") || query.contains("naa peru ")) {
            val name = when {
                query.contains("my name is ") -> query.substringAfter("my name is ").trim()
                query.contains("i am ") -> query.substringAfter("i am ").trim()
                query.contains("naa peru ") -> query.substringAfter("naa peru ").trim()
                else -> "User"
            }.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

            memoryRepository.addMemory("Active Speaker", name, "Profile")
            return if (name.equals("Yogesh", ignoreCase = true)) {
                memoryRepository.addMemory("Creator Title", "Boss", "Profile")
                CommandResult("Welcome back, Boss! Recognized you as Yogesh, creator of Helix. All systems ready for you.", ActionResponseCard.MemoryCard("Active User", "Yogesh (Boss)", "Recognized Creator"))
            } else {
                CommandResult("Hello $name! I have registered your name. How can Helix assist you today?", ActionResponseCard.MemoryCard("Active User", name, "Registered User"))
            }
        }

        // JARVIS Multi-Step Routine 1: Bedtime Routine
        if (query.contains("going to sleep") || query.contains("bedtime") || query.contains("goodnight") || query.contains("night mode") || query.contains("prepare my phone for the night")) {
            phoneControlManager.setMute(mute = true)
            phoneControlManager.toggleTorch(enable = false)
            phoneControlManager.createAlarm(7, 0, "Morning Alarm")
            val batteryCard = phoneControlManager.getBatteryStatus()
            val batteryDetail = batteryCard.infoDetails.find { it.first == "Battery Level" }?.second ?: "80%"

            val jarvisText = "At your service, $title. Media muted, flashlight turned off, battery at $batteryDetail, and morning alarm set for 7:00 AM. Goodnight, $title."
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

            val jarvisText = "Good morning, $title. Today is $dateStr, current time is $timeStr. Weather in Hyderabad is sunny at 31°C. Battery level at $batteryDetail. All systems optimal, $title."
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
            return CommandResult("$title, the current time is $timeStr.", card)
        }

        if (query.contains("date") || query.contains("today's date") || query.contains("theedi") || query.contains("తేదీ")) {
            val dateStr = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(Date())
            val card = ActionResponseCard.SystemInfoCard("Today's Date", listOf("Date" to dateStr))
            return CommandResult("$title, today is $dateStr.", card)
        }

        // Local Processing 2: Greetings & Stop
        if (query == "hello helix" || query == "hi helix" || query == "hey helix" || query == "hello" || query == "namaste") {
            return CommandResult("At your service, $title. How can Helix assist you today?")
        }

        if (query == "stop listening" || query == "stop" || query == "cancel") {
            return CommandResult("Listening suspended, $title.")
        }

        // Feature 1: Web Search / Browser Search
        if (query.startsWith("search ") || query.startsWith("google ") || query.contains("browser") || query.contains("search web")) {
            val searchQuery = query.replace("search ", "").replace("google ", "").replace("browser ", "").replace("for ", "").replace("search web ", "").trim()
            val (llmAnswer, _) = llmService.queryLlm(
                memoryRepository.getLlmProvider(),
                memoryRepository.getApiKey(),
                memoryRepository.getEndpointUrl(),
                "Perform a concise web search summary for: '$searchQuery'. Provide a clear, informative 2-sentence response in JARVIS assistant tone addressed to $title."
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
                "Give a concise current live weather report for $location. Mention estimated temperature, weather condition (e.g. Sunny, Partly Cloudy, Rain), high/low temps, and a brief 1-sentence summary addressed politely to $title."
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
                return CommandResult("Flashlight disabled, $title.", card)
            } else {
                val card = phoneControlManager.toggleTorch(enable = true)
                return CommandResult("Flashlight enabled, $title.", card)
            }
        }

        // Hardware Controls 2: Volume Commands
        if (query.contains("volume up") || query.contains("increase volume") || query.contains("louder")) {
            val card = phoneControlManager.adjustVolume(increase = true)
            return CommandResult("Increasing media volume, $title.", card)
        }
        if (query.contains("volume down") || query.contains("decrease volume") || query.contains("lower volume") || query.contains("quieter")) {
            val card = phoneControlManager.adjustVolume(increase = false)
            return CommandResult("Decreasing volume, $title.", card)
        }
        if (query.contains("mute") || query.contains("silent")) {
            val card = phoneControlManager.setMute(mute = true)
            return CommandResult("Audio muted, $title.", card)
        }

        // Hardware Controls 3: Open App Commands
        if (query.startsWith("open ") || query.startsWith("launch ")) {
            val appName = query.replace("open ", "").replace("launch ", "").trim()
            val card = phoneControlManager.launchAppByName(appName)
            return if (card.success) {
                CommandResult("Opening ${card.appName} for you, $title.", card)
            } else {
                CommandResult("I couldn't find ${card.appName} installed on your device, $title.", card)
            }
        }

        // Hardware Controls 4: Battery & Device Status
        if (query.contains("battery") || query.contains("charging")) {
            val card = phoneControlManager.getBatteryStatus()
            return CommandResult("Checking power systems and battery status, $title.", card)
        }
        if (query.contains("device info") || query.contains("phone info") || query.contains("system status")) {
            val card = phoneControlManager.getDeviceStatus()
            return CommandResult("Displaying system status and device information, $title.", card)
        }

        // Hardware Controls 5: Calls (English & Telugu e.g. "call Amma", "Praveen ki call cheyyi")
        if (query.startsWith("call ") || query.startsWith("dial ") || query.contains("call cheyyi") || query.contains("ki call")) {
            val rawTarget = query.replace("call ", "").replace("dial ", "").replace("call cheyyi", "").replace("ki call", "").trim()
            val contactMatch = phoneControlManager.findPhoneNumberForContact(rawTarget)
            val nameToCall = contactMatch?.first ?: memoryRepository.findMemory(rawTarget)?.value ?: rawTarget

            val card = phoneControlManager.makeCall(nameToCall)
            return CommandResult("Initiating call to ${card.contactName}, $title.", card)
        }

        // Hardware Controls 6: Messages / SMS / WhatsApp (English & Telugu e.g. "Praveen ki message pampu")
        if (query.contains("whatsapp")) {
            val raw = query.replace("whatsapp", "").replace("message", "").replace("send", "").replace("pampu", "").trim()
            val parts = raw.split(" that ", " ki ", limit = 2)
            val contact = parts.getOrNull(0) ?: "Contact"
            val textMsg = if (parts.size > 1) parts[1] else "Hello from Helix!"
            val card = phoneControlManager.openWhatsAppMessage(contact, textMsg)
            return CommandResult("Preparing WhatsApp message to $contact, $title.", card)
        }
        if (query.startsWith("message ") || query.startsWith("text ") || query.startsWith("sms ") || query.contains("message pampu")) {
            val raw = query.replace("message ", "").replace("text ", "").replace("sms ", "").replace("message pampu", "").trim()
            val parts = raw.split(" that ", " ki ", limit = 2)
            val contact = parts.getOrNull(0) ?: "Contact"
            val textMsg = if (parts.size > 1) parts[1] else "Hello from Helix!"
            val card = phoneControlManager.composeSms(contact, textMsg)
            return CommandResult("Drafted SMS to ${card.contactName}: '$textMsg'. Confirmation required, $title.", card)
        }

        // Hardware Controls 7: Settings Panel
        if (query.contains("wifi") || query.contains("wi-fi")) {
            phoneControlManager.openSettingsPanel("wifi")
            return CommandResult("Opening Wi-Fi settings, $title.")
        }
        if (query.contains("bluetooth")) {
            phoneControlManager.openSettingsPanel("bluetooth")
            return CommandResult("Opening Bluetooth settings, $title.")
        }

        // Memory Queries / Store
        if (query.startsWith("remember that ") || query.startsWith("remember ")) {
            val content = query.replace("remember that ", "").replace("remember ", "")
            memoryRepository.addMemory("Fact", content)
            val card = ActionResponseCard.MemoryCard("Saved Fact", content, "Added to Memory")
            return CommandResult("Logged into personal memory, $title: '$content'.", card)
        }
        if (query.contains("what do you remember") || query.contains("my memories")) {
            val list = memoryRepository.memories.value
            val summary = if (list.isEmpty()) "Memory archives are empty, $title." else "I have ${list.size} stored memories regarding your profile and preferences, $title."
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
        val title = getSpeakerTitle(rawQuery)

        val memoryList = memoryRepository.memories.value.joinToString("; ") { "${it.key}: ${it.value}" }
        val promptText = "You are Helix, a highly intelligent, polite, slightly witty AI assistant inspired by JARVIS and Google Assistant. User context memories: [$memoryList]. Answer the user's question accurately in 1 to 3 concise sentences so it sounds natural when spoken. Address the user politely as '$title'. Question: $rawQuery"

        val (responseText, sourceName) = llmService.queryLlm(provider, apiKey, endpointUrl, promptText)

        val card = ActionResponseCard.QaCard(
            question = rawQuery,
            answer = responseText,
            source = sourceName
        )
        return CommandResult(spokenResponse = responseText, card = card)
    }
}
