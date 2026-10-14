package com.example.chitttiaiassistant.repository

import android.content.Context
import com.example.chitttiaiassistant.model.AutonomyLevel
import com.example.chitttiaiassistant.model.UserMemoryItem
import com.example.chitttiaiassistant.service.LlmProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MemoryRepository(context: Context) {

    private val prefs = context.getSharedPreferences("helix_memory_prefs", Context.MODE_PRIVATE)

    private val _memories = MutableStateFlow<List<UserMemoryItem>>(emptyList())
    val memories: StateFlow<List<UserMemoryItem>> = _memories.asStateFlow()

    private val _selectedProvider = MutableStateFlow(getLlmProvider())
    val selectedProvider: StateFlow<LlmProvider> = _selectedProvider.asStateFlow()

    private val _apiKey = MutableStateFlow(getApiKey())
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _endpointUrl = MutableStateFlow(getEndpointUrl())
    val endpointUrl: StateFlow<String> = _endpointUrl.asStateFlow()

    private val _isLlmVerified = MutableStateFlow(isLlmVerified())
    val isLlmVerifiedFlow: StateFlow<Boolean> = _isLlmVerified.asStateFlow()

    private val _autonomyLevel = MutableStateFlow(getAutonomyLevel())
    val autonomyLevelFlow: StateFlow<AutonomyLevel> = _autonomyLevel.asStateFlow()

    init {
        loadInitialMemories()
    }

    private fun loadInitialMemories() {
        val initialList = listOf(
            UserMemoryItem(id = "1", key = "User Name", value = "User", category = "Profile"),
            UserMemoryItem(id = "2", key = "Nickname: Amma", value = "Mom (+1-800-555-0199)", category = "Contacts"),
            UserMemoryItem(id = "3", key = "Preferred Music App", value = "YouTube Music", category = "Preferences"),
            UserMemoryItem(id = "4", key = "Home Location", value = "Hyderabad", category = "Locations")
        )
        _memories.value = initialList
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

    fun isLlmVerified(): Boolean {
        return prefs.getBoolean("user_llm_verified", false)
    }

    fun setLlmVerified(verified: Boolean) {
        prefs.edit().putBoolean("user_llm_verified", verified).apply()
        _isLlmVerified.value = verified
    }

    fun getLlmProvider(): LlmProvider {
        val name = prefs.getString("selected_llm_provider", LlmProvider.GEMINI.name) ?: LlmProvider.GEMINI.name
        return try {
            LlmProvider.valueOf(name)
        } catch (e: Exception) {
            LlmProvider.GEMINI
        }
    }

    fun saveLlmProvider(provider: LlmProvider) {
        prefs.edit().putString("selected_llm_provider", provider.name).apply()
        _selectedProvider.value = provider
    }

    fun getApiKey(): String {
        return prefs.getString("user_gemini_api_key", "") ?: ""
    }

    fun saveApiKey(key: String) {
        val cleanKey = key.trim()
        prefs.edit().putString("user_gemini_api_key", cleanKey).apply()
        _apiKey.value = cleanKey
    }

    fun getEndpointUrl(): String {
        return prefs.getString("user_llm_endpoint_url", "http://10.0.2.2:11434") ?: "http://10.0.2.2:11434"
    }

    fun saveEndpointUrl(url: String) {
        val cleanUrl = url.trim()
        prefs.edit().putString("user_llm_endpoint_url", cleanUrl).apply()
        _endpointUrl.value = cleanUrl
    }

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
