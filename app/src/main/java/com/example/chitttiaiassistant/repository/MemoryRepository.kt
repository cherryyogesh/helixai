package com.example.chitttiaiassistant.repository

import android.content.Context
import com.example.chitttiaiassistant.model.AutonomyLevel
import com.example.chitttiaiassistant.model.UserMemoryItem
import com.example.chitttiaiassistant.service.LlmProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PrivacyMode(val displayName: String, val description: String) {
    PUSH_TO_TALK("Push-to-Talk Mode (Default)", "Microphone stays completely OFF. Activates only when you tap the Mic button."),
    APP_OPEN_WAKE_WORD("App-Open Wake-Word Mode", "Listens for 'Hey Helix' locally only while Helix is open on screen.")
}

class MemoryRepository(context: Context) {

    private val prefs = context.getSharedPreferences("helix_memory_prefs", Context.MODE_PRIVATE)

    private val _memories = MutableStateFlow<List<UserMemoryItem>>(emptyList())
    val memories: StateFlow<List<UserMemoryItem>> = _memories.asStateFlow()

    private val _serverEndpointUrl = MutableStateFlow(getServerEndpointUrl())
    val serverEndpointUrlFlow: StateFlow<String> = _serverEndpointUrl.asStateFlow()

    private val _privacyMode = MutableStateFlow(getPrivacyMode())
    val privacyModeFlow: StateFlow<PrivacyMode> = _privacyMode.asStateFlow()

    private val _isBackgroundWakeWordEnabled = MutableStateFlow(isBackgroundWakeWordEnabled())
    val isBackgroundWakeWordEnabledFlow: StateFlow<Boolean> = _isBackgroundWakeWordEnabled.asStateFlow()

    private val _isLlmVerified = MutableStateFlow(true)
    val isLlmVerifiedFlow: StateFlow<Boolean> = _isLlmVerified.asStateFlow()

    private val _autonomyLevel = MutableStateFlow(getAutonomyLevel())
    val autonomyLevelFlow: StateFlow<AutonomyLevel> = _autonomyLevel.asStateFlow()

    init {
        loadInitialMemories()
    }

    private fun loadInitialMemories() {
        val initialList = listOf(
            UserMemoryItem(id = "1", key = "Creator & Primary User", value = "Yogesh", category = "Profile"),
            UserMemoryItem(id = "2", key = "Honorific Title for Yogesh", value = "Boss", category = "Profile"),
            UserMemoryItem(id = "3", key = "Nickname: Amma", value = "Mom (+1-800-555-0199)", category = "Contacts"),
            UserMemoryItem(id = "4", key = "Preferred Music App", value = "YouTube Music", category = "Preferences"),
            UserMemoryItem(id = "5", key = "Home Location", value = "Hyderabad", category = "Locations")
        )
        _memories.value = initialList
    }

    fun getPrivacyMode(): PrivacyMode {
        val name = prefs.getString("user_privacy_mode", PrivacyMode.PUSH_TO_TALK.name) ?: PrivacyMode.PUSH_TO_TALK.name
        return try {
            PrivacyMode.valueOf(name)
        } catch (e: Exception) {
            PrivacyMode.PUSH_TO_TALK
        }
    }

    fun savePrivacyMode(mode: PrivacyMode) {
        prefs.edit().putString("user_privacy_mode", mode.name).apply()
        _privacyMode.value = mode
    }

    fun isBackgroundWakeWordEnabled(): Boolean {
        return prefs.getBoolean("bg_wake_word_enabled", false)
    }

    fun saveBackgroundWakeWordEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("bg_wake_word_enabled", enabled).apply()
        _isBackgroundWakeWordEnabled.value = enabled
    }

    fun getServerEndpointUrl(): String {
        return prefs.getString("helix_server_url", "http://10.0.2.2:8080") ?: "http://10.0.2.2:8080"
    }

    fun saveServerEndpointUrl(url: String) {
        val cleanUrl = url.trim()
        prefs.edit().putString("helix_server_url", cleanUrl).apply()
        _serverEndpointUrl.value = cleanUrl
    }

    fun getAutonomyLevel(): AutonomyLevel {
        val name = prefs.getString("user_autonomy_level", AutonomyLevel.ASK_BEFORE_EXECUTION.name) ?: AutonomyLevel.ASK_BEFORE_EXECUTION.name
        return try {
            AutonomyLevel.valueOf(name)
        } catch (e: Exception) {
            AutonomyLevel.ASK_BEFORE_EXECUTION
        }
    }

    fun saveAutonomyLevel(level: AutonomyLevel) {
        prefs.edit().putString("user_autonomy_level", level.name).apply()
        _autonomyLevel.value = level
    }

    fun isLlmVerified(): Boolean = true

    fun getLlmProvider(): LlmProvider = LlmProvider.GEMINI

    fun getApiKey(): String = ""

    fun getEndpointUrl(): String = getServerEndpointUrl()

    fun addMemory(key: String, value: String, category: String = "General") {
        val newItem = UserMemoryItem(key = key, value = value, category = category)
        _memories.value = _memories.value + newItem
    }

    fun deleteMemory(id: String) {
        _memories.value = _memories.value.filter { it.id != id }
    }

    fun clearAllMemories() {
        _memories.value = emptyList()
    }

    fun findMemory(queryKey: String): UserMemoryItem? {
        val q = queryKey.lowercase()
        return _memories.value.find { it.key.lowercase().contains(q) || q.contains(it.key.lowercase()) }
    }
}
