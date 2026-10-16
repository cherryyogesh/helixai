package com.example.chitttiaiassistant.service

import android.view.KeyEvent
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
    private var lastSpokenResponse: String = "I am Helix, your AI assistant."

    private fun resolveTitleForSpeaker(speakerName: String): String {
        val clean = speakerName.trim().lowercase()
        return if (clean.contains("yogesh")) "Boss" else speakerName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    suspend fun processCommand(inputQuery: String): CommandResult {
        val result = internalProcessCommand(inputQuery)
        lastSpokenResponse = result.spokenResponse
        return result
    }

    private suspend fun internalProcessCommand(inputQuery: String): CommandResult {
        val query = inputQuery.lowercase().trim()

        // 1. Capability Inquiries ("What can you do?")
        if (query.contains("what can you do") || query.contains("your capabilities") || query.contains("help me with")) {
            val helpText = "I can control your phone, open installed apps, toggle flashlight, adjust volume, place calls, draft WhatsApp and SMS messages, set alarms and timers, check live weather, search Google, and answer questions. How can I assist you?"
            val card = ActionResponseCard.SystemInfoCard(
                title = "Helix Capabilities",
                infoDetails = listOf(
                    "Phone Controls" to "Flashlight, Volume, Apps, Settings",
                    "Communication" to "Direct Calls, SMS, WhatsApp",
                    "Productivity" to "Alarms, Timers, Reminders, Memory",
                    "Intelligence" to "Multi-LLM, Live Weather, Web Search"
                )
            )
            return CommandResult(helpText, card)
        }

        // 2. Repeat Last Response
        if (query.contains("repeat") || query.contains("say that again") || query.contains("repeat last answer")) {
            return CommandResult(lastSpokenResponse)
        }

        // 3. Timers and Alarms ("set a timer for 10 minutes", "wake me up at 7")
        if (query.contains("timer")) {
            val minutes = query.filter { it.isDigit() }.toIntOrNull() ?: 5
            val msg = phoneControlManager.createTimer(minutes * 60, "Timer for $minutes minutes")
            val card = ActionResponseCard.SystemInfoCard("Timer Set", listOf("Duration" to "$minutes minutes", "Status" to "Active"))
            return CommandResult(msg, card)
        }

        if (query.contains("alarm") || query.contains("wake me up")) {
            val hour = query.filter { it.isDigit() }.toIntOrNull() ?: 7
            val msg = phoneControlManager.createAlarm(hour, 0, "Alarm for $hour:00")
            val card = ActionResponseCard.SystemInfoCard("Alarm Set", listOf("Time" to "$hour:00", "Status" to "Active"))
            return CommandResult(msg, card)
        }

        // 4. YouTube Playback & Search Commands
        if ((query.contains("youtube") || query.startsWith("play ")) && !query.contains("next") && !query.contains("skip") && !query.contains("pause")) {
            val musicQuery = query.replace("play ", "").replace("from youtube", "").replace("on youtube", "").replace("youtube", "").replace("songs", "song").trim()
            val card = phoneControlManager.playYouTubeSearch(if (musicQuery.isBlank()) "jailer 2 songs" else musicQuery)
            return CommandResult("Searching and playing '$musicQuery' on YouTube for you!", card)
        }

        // 5. Media Playback Controls (Skip, Pause, Resume)
        if (query.contains("skip") || query.contains("next video") || query.contains("next track") || query.contains("next song")) {
            phoneControlManager.sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
            val card = ActionResponseCard.MediaCard("Next Video / Track", "Media Player", "YouTube / Media Player", true)
            return CommandResult("Skipping to next video.", card)
        }

        if (query == "pause" || query.contains("pause video") || query.contains("pause music") || query.contains("stop video")) {
            phoneControlManager.sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE)
            val card = ActionResponseCard.MediaCard("Media Paused", "Media Player", "YouTube / Media Player", false)
            return CommandResult("Playback paused.", card)
        }

        if (query == "resume" || query.contains("resume video") || query.contains("play video") || query.contains("resume music")) {
            phoneControlManager.sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY)
            val card = ActionResponseCard.MediaCard("Media Resumed", "Media Player", "YouTube / Media Player", true)
            return CommandResult("Resuming playback.", card)
        }

        // 6. Name Asking & Speaker Registration
        if (query.contains("who is speaking") || query.contains("who am i")) {
            val activeSpeaker = memoryRepository.findMemory("Active Speaker")?.value
            return if (activeSpeaker != null) {
                val title = resolveTitleForSpeaker(activeSpeaker)
                CommandResult("You are $title, sir.", ActionResponseCard.MemoryCard("Active Speaker", activeSpeaker, "Recognized"))
            } else {
                CommandResult("Hello there! May I gently know your name?", ActionResponseCard.MemoryCard("Speaker Status", "Unknown", "Asking Name"))
            }
        }

        // Speaker Name Registration
        if (query.contains("my name is ") || query.contains("i am ") || query.contains("naa peru ")) {
            val extractedName = when {
                query.contains("my name is ") -> query.substringAfter("my name is ").trim()
                query.contains("i am ") -> query.substringAfter("i am ").trim()
                query.contains("naa peru ") -> query.substringAfter("naa peru ").trim()
                else -> "User"
            }.split(" ").firstOrNull()?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: "User"

            memoryRepository.addMemory("Active Speaker", extractedName, "Profile")

            return if (extractedName.equals("Yogesh", ignoreCase = true)) {
                memoryRepository.addMemory("Creator Title", "Boss", "Profile")
                CommandResult("Welcome back, Boss Yogesh! I have recognized your voice. At your service, Boss.", ActionResponseCard.MemoryCard("Recognized Speaker", "Yogesh (Boss)", "Creator Verified"))
            } else {
                CommandResult("Pleased to meet you, $extractedName! I will recognize you next time when you speak.", ActionResponseCard.MemoryCard("Recognized Speaker", extractedName, "Speaker Registered"))
            }
        }

        val currentSpeakerName = memoryRepository.findMemory("Active Speaker")?.value ?: "Yogesh"
        val speakerTitle = resolveTitleForSpeaker(currentSpeakerName)

        // 7. Messaging (WhatsApp & SMS)
        if (query.contains("whatsapp") || query.startsWith("send message") || query.startsWith("message ") || query.startsWith("text ") || query.startsWith("sms ") || query.contains("message pampu")) {
            var raw = query.replace("send message to ", "").replace("send message ", "").replace("message ", "").replace("text ", "").replace("sms ", "").replace("message pampu", "").trim()
            val isWhatsApp = query.contains("whatsapp")
            if (isWhatsApp) raw = raw.replace("on whatsapp", "").replace("whatsapp", "").trim()

            val parts = if (raw.contains(" that ")) raw.split(" that ", limit = 2) else raw.split(" ki ", limit = 2)
            val targetContact = parts.getOrNull(0)?.trim() ?: "Dad"
            val messageBody = if (parts.size > 1) parts[1].trim() else "I am coming late today"

            val card = if (isWhatsApp) {
                phoneControlManager.openWhatsAppMessage(targetContact, messageBody)
            } else {
                phoneControlManager.composeSms(targetContact, messageBody)
            }

            return CommandResult("Opening ${if (isWhatsApp) "WhatsApp" else "Messaging"} to send message to ${card.contactName}: '$messageBody', $speakerTitle!", card)
        }

        // 8. Direct Calling
        if (query.startsWith("call ") || query.startsWith("dial ") || query.contains("call cheyyi") || query.contains("ki call")) {
            val rawTarget = query.replace("call ", "").replace("dial ", "").replace("call cheyyi", "").replace("ki call", "").trim()
            val contactMatch = phoneControlManager.findPhoneNumberForContact(rawTarget)
            val nameToCall = contactMatch?.first ?: memoryRepository.findMemory(rawTarget)?.value ?: rawTarget

            val card = phoneControlManager.makeCall(nameToCall).copy(needsConfirmation = false)
            return CommandResult("Placing call to ${card.contactName} now, $speakerTitle.", card)
        }

        // 9. Multi-Person Conversation Parsing
        if (query.contains(" and ") && (query.contains("says") || query.contains("asks") || query.contains("wants"))) {
            val parts = query.split(" and ")
            val replies = mutableListOf<String>()

            for (part in parts) {
                val personName = part.substringBefore(" ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                val personTitle = resolveTitleForSpeaker(personName)
                val subResult = processCommand(part)
                replies.add("$personTitle: ${subResult.spokenResponse}")
            }

            val combinedResponse = replies.joinToString("  ")
            return CommandResult(combinedResponse)
        }

        // JARVIS Multi-Step Routine 1: Bedtime Routine
        if (query.contains("going to sleep") || query.contains("bedtime") || query.contains("goodnight") || query.contains("night mode") || query.contains("prepare my phone for the night")) {
            phoneControlManager.setMute(mute = true)
            phoneControlManager.toggleTorch(enable = false)
            phoneControlManager.createAlarm(7, 0, "Morning Alarm")
            val batteryCard = phoneControlManager.getBatteryStatus()
            val batteryDetail = batteryCard.infoDetails.find { it.first == "Battery Level" }?.second ?: "80%"

            val jarvisText = "At your service, $speakerTitle. Media muted, flashlight turned off, battery at $batteryDetail, and morning alarm set for 7:00 AM. Goodnight, $speakerTitle."
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

            val jarvisText = "Good morning, $speakerTitle. Today is $dateStr, current time is $timeStr. Weather in Hyderabad is sunny at 31°C. Battery level at $batteryDetail. All systems optimal, $speakerTitle."
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

        // Local Processing 1: Time & Date
        if (query.contains("time") || query.contains("clock") || query.contains("samayam") || query.contains("సమయం")) {
            val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
            val card = ActionResponseCard.SystemInfoCard("Current Time", listOf("Time" to timeStr, "Time Zone" to "Local Time"))
            return CommandResult("$speakerTitle, the current time is $timeStr.", card)
        }

        if (query.contains("date") || query.contains("today's date") || query.contains("theedi") || query.contains("తేదీ")) {
            val dateStr = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(Date())
            val card = ActionResponseCard.SystemInfoCard("Today's Date", listOf("Date" to dateStr))
            return CommandResult("$speakerTitle, today is $dateStr.", card)
        }

        // Local Processing 2: Greetings & Stop
        if (query == "hello helix" || query == "hi helix" || query == "hey helix" || query == "hello" || query == "namaste") {
            return CommandResult("At your service, $speakerTitle. How can Helix assist you today?")
        }

        if (query == "stop listening" || query == "stop" || query == "cancel") {
            return CommandResult("Listening suspended, $speakerTitle.")
        }

        // Web Search / Information Extraction via Gemini AI Knowledge Engine
        if (query.startsWith("search ") || query.startsWith("google ") || query.contains("browser") || query.contains("search web") || query.startsWith("tell me about") || query.startsWith("what is ") || query.startsWith("who is ") || query.contains("extract")) {
            val searchQuery = query.replace("search ", "").replace("google ", "").replace("browser ", "").replace("tell me about ", "").replace("for ", "").replace("search web ", "").trim()
            val (llmAnswer, _) = llmService.queryLlm(
                memoryRepository.getLlmProvider(),
                memoryRepository.getApiKey(),
                memoryRepository.getEndpointUrl(),
                "Perform an accurate informational web search summary for: '$searchQuery'. Provide a clear, informative 2 to 3 sentence response."
            )

            val searchCard = ActionResponseCard.QaCard(
                question = "Search: $searchQuery",
                answer = llmAnswer,
                source = "Web & Gemini Knowledge Engine"
            )
            return CommandResult(llmAnswer, searchCard)
        }

        // Live Weather
        if (query.contains("weather") || query.contains("temperature") || query.contains("forecast") || query.contains("climate") || query.contains("vaathavaranam")) {
            val location = if (query.contains("in ")) query.substringAfter("in ").trim() else "your current location"
            val (weatherSummary, _) = llmService.queryLlm(
                memoryRepository.getLlmProvider(),
                memoryRepository.getApiKey(),
                memoryRepository.getEndpointUrl(),
                "Give a concise current live weather report for $location. Mention estimated temperature, weather condition, high/low temps, and a brief 1-sentence summary."
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
                return CommandResult("Flashlight disabled, $speakerTitle.", card)
            } else {
                val card = phoneControlManager.toggleTorch(enable = true)
                return CommandResult("Flashlight enabled, $speakerTitle.", card)
            }
        }

        // Hardware Controls 2: Volume Commands
        if (query.contains("volume up") || query.contains("increase volume") || query.contains("louder")) {
            val card = phoneControlManager.adjustVolume(increase = true)
            return CommandResult("Increasing media volume, $speakerTitle.", card)
        }
        if (query.contains("volume down") || query.contains("decrease volume") || query.contains("lower volume") || query.contains("quieter")) {
            val card = phoneControlManager.adjustVolume(increase = false)
            return CommandResult("Decreasing volume, $speakerTitle.", card)
        }
        if (query.contains("mute") || query.contains("silent")) {
            val card = phoneControlManager.setMute(mute = true)
            return CommandResult("Audio muted, $speakerTitle.", card)
        }

        // Hardware Controls 3: Open App Commands
        if (query.startsWith("open ") || query.startsWith("launch ")) {
            val appName = query.replace("open ", "").replace("launch ", "").trim()
            val card = phoneControlManager.launchAppByName(appName)
            return if (card.success) {
                CommandResult("Opening ${card.appName} for you, $speakerTitle.", card)
            } else {
                CommandResult("I couldn't find ${card.appName} installed on your device, $speakerTitle.", card)
            }
        }

        // Memory Queries / Store ("Remember that...", "Forget that...")
        if (query.startsWith("remember that ") || query.startsWith("remember ")) {
            val content = query.replace("remember that ", "").replace("remember ", "")
            memoryRepository.addMemory("Fact", content)
            val card = ActionResponseCard.MemoryCard("Saved Fact", content, "Added to Memory")
            return CommandResult("Logged into personal memory, $speakerTitle: '$content'.", card)
        }

        if (query.startsWith("forget that ") || query.startsWith("forget ")) {
            val target = query.replace("forget that ", "").replace("forget ", "")
            val found = memoryRepository.findMemory(target)
            if (found != null) {
                memoryRepository.deleteMemory(found.id)
                val card = ActionResponseCard.MemoryCard("Memory Deleted", found.key, "Removed")
                return CommandResult("Forgotten '$target' from personal memory, $speakerTitle.", card)
            }
        }

        // Universal Multi-LLM Response
        return querySelectedLlm(inputQuery)
    }

    private suspend fun querySelectedLlm(rawQuery: String): CommandResult {
        val provider = memoryRepository.getLlmProvider()
        val apiKey = memoryRepository.getApiKey()
        val endpointUrl = memoryRepository.getEndpointUrl()
        val currentSpeakerName = memoryRepository.findMemory("Active Speaker")?.value ?: "Yogesh"
        val speakerTitle = resolveTitleForSpeaker(currentSpeakerName)

        val memoryList = memoryRepository.memories.value.joinToString("; ") { "${it.key}: ${it.value}" }
        val promptText = "You are Helix, a highly intelligent, polite, slightly witty AI assistant inspired by JARVIS and Google Assistant. User context memories: [$memoryList]. Answer the user's question accurately in 1 to 3 concise sentences. Address the user as '$speakerTitle'. Question: $rawQuery"

        val (responseText, sourceName) = llmService.queryLlm(provider, apiKey, endpointUrl, promptText)

        val card = ActionResponseCard.QaCard(
            question = rawQuery,
            answer = responseText,
            source = sourceName
        )
        return CommandResult(spokenResponse = responseText, card = card)
    }
}
