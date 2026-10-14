package com.example.chitttiaiassistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chitttiaiassistant.brain.mcp.LearningBrainMcpTools
import com.example.chitttiaiassistant.model.ActionResponseCard
import com.example.chitttiaiassistant.model.AssistantState
import com.example.chitttiaiassistant.model.AutomationItem
import com.example.chitttiaiassistant.model.AutonomyLevel
import com.example.chitttiaiassistant.model.ChatMessage
import com.example.chitttiaiassistant.model.Sender
import com.example.chitttiaiassistant.model.UserMemoryItem
import com.example.chitttiaiassistant.repository.AutomationRepository
import com.example.chitttiaiassistant.repository.MemoryRepository
import com.example.chitttiaiassistant.service.CommandRouter
import com.example.chitttiaiassistant.service.DiagnosticReport
import com.example.chitttiaiassistant.service.HelixVoiceAssistant
import com.example.chitttiaiassistant.service.InstalledAppInfo
import com.example.chitttiaiassistant.service.LlmConnectionStatus
import com.example.chitttiaiassistant.service.LlmProvider
import com.example.chitttiaiassistant.service.LlmService
import com.example.chitttiaiassistant.service.LlmValidationResult
import com.example.chitttiaiassistant.service.PhoneControlManager
import com.example.chitttiaiassistant.service.VoiceDiagnostics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    val phoneControlManager = PhoneControlManager(context)
    val memoryRepository = MemoryRepository(context)
    val automationRepository = AutomationRepository()
    val learningBrainMcp = LearningBrainMcpTools(context)

    private val commandRouter = CommandRouter(phoneControlManager, memoryRepository)
    private val llmService = LlmService()
    private val voiceDiagnostics = VoiceDiagnostics(context, memoryRepository)

    private val voiceAssistant = HelixVoiceAssistant(context) { recognizedText ->
        processUserCommand(recognizedText)
    }

    // Navigation & Screen Visibility States
    private val _isSplashVisible = MutableStateFlow(true)
    val isSplashVisible: StateFlow<Boolean> = _isSplashVisible.asStateFlow()

    private val _isOnboardingVisible = MutableStateFlow(false)
    val isOnboardingVisible: StateFlow<Boolean> = _isOnboardingVisible.asStateFlow()

    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    private val _isOverlayVisible = MutableStateFlow(false)
    val isOverlayVisible: StateFlow<Boolean> = _isOverlayVisible.asStateFlow()

    // Assistant States
    val assistantState: StateFlow<AssistantState> = voiceAssistant.assistantState
    val liveTranscript: StateFlow<String> = voiceAssistant.liveTranscript
    val audioLevel: StateFlow<Float> = voiceAssistant.audioLevel

    private val _currentCard = MutableStateFlow<ActionResponseCard>(ActionResponseCard.None)
    val currentCard: StateFlow<ActionResponseCard> = _currentCard.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _diagnosticReport = MutableStateFlow<DiagnosticReport?>(null)
    val diagnosticReport: StateFlow<DiagnosticReport?> = _diagnosticReport.asStateFlow()

    // Repository States
    val memories: StateFlow<List<UserMemoryItem>> = memoryRepository.memories
    val automations: StateFlow<List<AutomationItem>> = automationRepository.automations
    val selectedLlmProvider: StateFlow<LlmProvider> = memoryRepository.selectedProvider
    val apiKey: StateFlow<String> = memoryRepository.apiKey
    val llmEndpointUrl: StateFlow<String> = memoryRepository.endpointUrl
    val isLlmVerified: StateFlow<Boolean> = memoryRepository.isLlmVerifiedFlow
    val autonomyLevel: StateFlow<AutonomyLevel> = memoryRepository.autonomyLevelFlow

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _isTorchActive = MutableStateFlow(false)
    val isTorchActive: StateFlow<Boolean> = _isTorchActive.asStateFlow()

    init {
        loadInstalledApps()
    }

    fun onSplashFinished() {
        _isSplashVisible.value = false
        _isOnboardingVisible.value = true
    }

    fun onGetStarted() {
        _isOnboardingVisible.value = false
    }

    fun runVoiceDiagnostics() {
        viewModelScope.launch {
            _diagnosticReport.value = voiceDiagnostics.runFullDiagnostics()
        }
    }

    fun saveAutonomyLevel(level: AutonomyLevel) {
        memoryRepository.saveAutonomyLevel(level)
    }

    suspend fun validateLlmConfig(provider: LlmProvider, key: String, endpointUrl: String): LlmValidationResult {
        val result = llmService.testConnection(provider, key, endpointUrl)
        val isSuccess = result.status == LlmConnectionStatus.CONNECTED
        memoryRepository.setLlmVerified(isSuccess)
        if (isSuccess) {
            memoryRepository.saveLlmProvider(provider)
            memoryRepository.saveApiKey(key)
            memoryRepository.saveEndpointUrl(endpointUrl)
        }
        return result
    }

    fun saveLlmConfig(provider: LlmProvider, key: String, endpointUrl: String) {
        memoryRepository.saveLlmProvider(provider)
        if (key.isNotBlank()) memoryRepository.saveApiKey(key)
        if (endpointUrl.isNotBlank()) memoryRepository.saveEndpointUrl(endpointUrl)
    }

    fun selectTab(index: Int) {
        _activeTab.value = index
    }

    fun triggerVoiceAssistant() {
        _isOverlayVisible.value = true
        _currentCard.value = ActionResponseCard.None
        voiceAssistant.startListening()
    }

    fun processUserCommand(query: String) {
        if (query.isBlank()) return

        _isOverlayVisible.value = true

        val userMsg = ChatMessage(sender = Sender.USER, text = query)
        _chatHistory.value = listOf(userMsg) + _chatHistory.value

        viewModelScope.launch {
            val result = commandRouter.processCommand(query)
            _currentCard.value = result.card

            val helixMsg = ChatMessage(sender = Sender.HELIX, text = result.spokenResponse, card = result.card)
            _chatHistory.value = listOf(helixMsg) + _chatHistory.value

            learningBrainMcp.recordTask(
                title = query,
                context = "User voice/text command",
                actionsTaken = listOf(result.card.javaClass.simpleName),
                resultSummary = result.spokenResponse,
                outcome = "SUCCESS"
            )

            voiceAssistant.speak(result.spokenResponse)
        }
    }

    fun dismissOverlay() {
        voiceAssistant.stopListening()
        _isOverlayVisible.value = false
    }

    fun toggleTorch() {
        val card = phoneControlManager.toggleTorch()
        _isTorchActive.value = card.isOn
    }

    fun adjustVolume(increase: Boolean) {
        val card = phoneControlManager.adjustVolume(increase = increase)
        _currentCard.value = card
    }

    fun openApp(appName: String) {
        processUserCommand("open $appName")
    }

    fun makeCall(contact: String) {
        processUserCommand("call $contact")
    }

    fun openSettings(action: String) {
        phoneControlManager.openSettingsPanel(action)
    }

    fun addMemory(key: String, value: String, category: String) {
        memoryRepository.addMemory(key, value, category)
    }

    fun deleteMemory(id: String) {
        memoryRepository.deleteMemory(id)
    }

    fun clearAllMemories() {
        memoryRepository.clearAllMemories()
    }

    fun toggleAutomation(id: String, enabled: Boolean) {
        automationRepository.toggleAutomation(id, enabled)
    }

    fun clearChatHistory() {
        _chatHistory.value = emptyList()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _installedApps.value = phoneControlManager.getInstalledAppsList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceAssistant.destroy()
    }
}
