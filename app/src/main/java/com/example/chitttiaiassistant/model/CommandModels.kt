package com.example.chitttiaiassistant.model

import java.util.UUID

enum class AutonomyLevel(val displayName: String, val description: String) {
    SUGGEST_ONLY("Suggest Only", "Ask for confirmation before executing any action."),
    ASK_BEFORE_EXECUTION("Ask Before Execution", "Ask before sensitive or high-impact actions, auto-run safe tasks."),
    EXECUTE_APPROVED("Execute Approved Workflows", "Automatically run verified skills, ask for new workflows."),
    FULL_AUTONOMY("Autonomous", "Execute tasks autonomously within safety limits.")
}

enum class CommandActionType {
    OPEN_APP,
    CLOSE_APP_GUIDANCE,
    GO_HOME,
    OPEN_RECENT_APPS,
    SET_FLASHLIGHT,
    SET_BRIGHTNESS,
    SET_VOLUME,
    TOGGLE_AUTO_ROTATE,
    OPEN_SETTINGS,
    GET_BATTERY_STATUS,
    GET_DEVICE_STATUS,
    LOCK_SCREEN,
    FIND_CONTACT,
    PLACE_CALL,
    OPEN_DIALER,
    REDIAL_LAST_NUMBER,
    OPEN_CALL_HISTORY,
    COMPOSE_SMS,
    SEND_SMS,
    READ_SMS,
    PLAY_MEDIA,
    PAUSE_MEDIA,
    RESUME_MEDIA,
    NEXT_TRACK,
    PREVIOUS_TRACK,
    SEARCH_SONG,
    OPEN_MUSIC_APP,
    STOP_MEDIA,
    GET_TIME,
    GET_DATE,
    CREATE_ALARM,
    CREATE_TIMER,
    CANCEL_TIMER,
    CREATE_REMINDER,
    LIST_REMINDERS,
    CREATE_CALENDAR_EVENT,
    READ_CALENDAR_EVENTS,
    OPEN_WIFI_SETTINGS,
    OPEN_BLUETOOTH_SETTINGS,
    OPEN_LOCATION_SETTINGS,
    OPEN_HOTSPOT_SETTINGS,
    OPEN_DISPLAY_SETTINGS,
    OPEN_SOUND_SETTINGS,
    OPEN_APP_SETTINGS,
    GET_NETWORK_STATUS,
    SET_AIRPLANE_MODE,
    ASK_AI,
    WEB_SEARCH,
    SUMMARIZE_TEXT,
    TRANSLATE_TEXT,
    EXPLAIN_TOPIC,
    CALCULATE,
    GET_WEATHER,
    GET_NEWS,
    REMEMBER_PREFERENCE,
    FORGET_PREFERENCE,
    OPEN_WHATSAPP,
    COMPOSE_WHATSAPP_MESSAGE,
    SHARE_TEXT,
    COMPOSE_EMAIL,
    SEND_EMAIL,
    ENABLE_WAKE_WORD,
    DISABLE_WAKE_WORD,
    SET_ASSISTANT_LANGUAGE,
    SET_ASSISTANT_VOICE,
    GET_PERMISSION_STATUS,
    OPEN_PERMISSION_SETTINGS,
    CLEAR_CONVERSATION_HISTORY,
    GET_ASSISTANT_STATUS,
    STOP_CURRENT_TASK,
    REPEAT_LAST_RESPONSE
}

enum class ExecutionStatus {
    SUCCESS,
    FAILED,
    NEEDS_CONFIRMATION,
    PERMISSION_DENIED,
    UNSUPPORTED,
    CANCELLED,
    PENDING
}

enum class RiskLevel {
    LOW,
    MODERATE,
    SENSITIVE,
    HIGH_IMPACT,
    RESTRICTED
}

data class CommandExecutionResult(
    val operationId: String = UUID.randomUUID().toString(),
    val action: CommandActionType,
    val status: ExecutionStatus,
    val message: String,
    val requiresConfirmation: Boolean = false,
    val errorCode: String? = null,
    val card: ActionResponseCard = ActionResponseCard.None
)

data class ParsedCommand(
    val action: CommandActionType,
    val parameters: Map<String, String> = emptyMap(),
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val requiresConfirmation: Boolean = false
)

sealed interface ActionResponseCard {
    data object None : ActionResponseCard

    data class WeatherCard(
        val city: String,
        val temp: String,
        val condition: String,
        val highTemp: String,
        val lowTemp: String,
        val hourlyForecast: List<HourlyTemp> = emptyList()
    ) : ActionResponseCard

    data class AppLaunchCard(
        val appName: String,
        val packageName: String,
        val success: Boolean,
        val message: String
    ) : ActionResponseCard

    data class CallCard(
        val contactName: String,
        val phoneNumber: String,
        val needsConfirmation: Boolean = true
    ) : ActionResponseCard

    data class SmsCard(
        val contactName: String,
        val messageText: String,
        val needsConfirmation: Boolean = true
    ) : ActionResponseCard

    data class MediaCard(
        val title: String,
        val artist: String,
        val appName: String,
        val isPlaying: Boolean
    ) : ActionResponseCard

    data class QaCard(
        val question: String,
        val answer: String,
        val source: String = "Helix Knowledge Engine"
    ) : ActionResponseCard

    data class TorchCard(
        val isOn: Boolean
    ) : ActionResponseCard

    data class VolumeCard(
        val streamName: String,
        val levelPercent: Int
    ) : ActionResponseCard

    data class MemoryCard(
        val key: String,
        val value: String,
        val action: String
    ) : ActionResponseCard

    data class SystemInfoCard(
        val title: String,
        val infoDetails: List<Pair<String, String>>
    ) : ActionResponseCard
}

data class HourlyTemp(
    val time: String,
    val temp: String,
    val icon: String
)

enum class Sender {
    USER,
    HELIX
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: Sender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val card: ActionResponseCard = ActionResponseCard.None
)

data class UserMemoryItem(
    val id: String = UUID.randomUUID().toString(),
    val key: String,
    val value: String,
    val category: String = "General"
)

data class AutomationItem(
    val id: String,
    val title: String,
    val description: String,
    val scheduleTime: String,
    val isEnabled: Boolean,
    val type: String
)
