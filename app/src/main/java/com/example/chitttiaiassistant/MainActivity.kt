package com.example.chitttiaiassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chitttiaiassistant.service.ForegroundListeningService
import com.example.chitttiaiassistant.ui.components.AssistantOverlay
import com.example.chitttiaiassistant.ui.screens.AutomationsScreen
import com.example.chitttiaiassistant.ui.screens.HomeScreen
import com.example.chitttiaiassistant.ui.screens.MemoryScreen
import com.example.chitttiaiassistant.ui.screens.OnboardingScreen
import com.example.chitttiaiassistant.ui.screens.SettingsScreen
import com.example.chitttiaiassistant.ui.screens.SplashScreen
import com.example.chitttiaiassistant.ui.screens.ToolsScreen
import com.example.chitttiaiassistant.ui.theme.BrightCyan
import com.example.chitttiaiassistant.ui.theme.ChitttiAiAssistantTheme
import com.example.chitttiaiassistant.ui.theme.DarkBackground
import com.example.chitttiaiassistant.ui.theme.NeonCyan
import com.example.chitttiaiassistant.ui.theme.SurfaceDark
import com.example.chitttiaiassistant.ui.theme.TextPrimary
import com.example.chitttiaiassistant.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    private var initialQueryToProcess: String? = null
    private var shouldStartVoiceOnInit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        // Start Persistent Background Voice Assistant Service
        try {
            ForegroundListeningService.startService(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            ChitttiAiAssistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChittiAppMain(
                        startVoiceOnInit = shouldStartVoiceOnInit,
                        initialQuery = initialQueryToProcess
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val startVoiceExtra = intent.getBooleanExtra("EXTRA_START_VOICE", false)
        val initialQuery = intent.getStringExtra("EXTRA_INITIAL_QUERY")

        if (!initialQuery.isNullOrBlank()) {
            initialQueryToProcess = initialQuery
            shouldStartVoiceOnInit = true
        } else if (startVoiceExtra || Intent.ACTION_ASSIST == action || Intent.ACTION_VOICE_COMMAND == action) {
            shouldStartVoiceOnInit = true
        } else {
            shouldStartVoiceOnInit = false
        }
    }
}

data class NavigationTab(
    val title: String,
    val icon: ImageVector
)

@Composable
fun ChittiAppMain(
    startVoiceOnInit: Boolean = false,
    initialQuery: String? = null,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current

    val isSplashVisible by viewModel.isSplashVisible.collectAsState()
    val isOnboardingVisible by viewModel.isOnboardingVisible.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val isOverlayVisible by viewModel.isOverlayVisible.collectAsState()

    val assistantState by viewModel.assistantState.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()
    val audioLevel by viewModel.audioLevel.collectAsState()
    val currentCard by viewModel.currentCard.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()

    val memories by viewModel.memories.collectAsState()
    val automations by viewModel.automations.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val isTorchActive by viewModel.isTorchActive.collectAsState()

    val selectedLlmProvider by viewModel.selectedLlmProvider.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val llmEndpointUrl by viewModel.llmEndpointUrl.collectAsState()
    val isLlmVerified by viewModel.isLlmVerified.collectAsState()
    val autonomyLevel by viewModel.autonomyLevel.collectAsState()

    // Permission Checking States
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    var hasCallPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasMicPermission = results[Manifest.permission.RECORD_AUDIO] ?: hasMicPermission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = results[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotificationPermission
        }
        hasCallPermission = results[Manifest.permission.CALL_PHONE] ?: hasCallPermission
        hasContactsPermission = results[Manifest.permission.READ_CONTACTS] ?: hasContactsPermission
    }

    fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    LaunchedEffect(startVoiceOnInit, initialQuery) {
        if (!initialQuery.isNullOrBlank()) {
            viewModel.processUserCommand(initialQuery)
        } else if (startVoiceOnInit && !isSplashVisible && !isOnboardingVisible && hasMicPermission) {
            viewModel.triggerVoiceAssistant()
        }
    }

    if (isSplashVisible) {
        SplashScreen(onSplashFinished = { viewModel.onSplashFinished() })
        return
    }

    if (isOnboardingVisible) {
        OnboardingScreen(
            currentProvider = selectedLlmProvider,
            currentApiKey = apiKey,
            currentEndpointUrl = llmEndpointUrl,
            onValidateConfig = { provider, key, endpoint ->
                viewModel.validateLlmConfig(provider, key, endpoint)
            },
            onRequestPermissionsAndEnterDashboard = {
                requestPermissions()
                viewModel.onGetStarted()
                if (!initialQuery.isNullOrBlank()) {
                    viewModel.processUserCommand(initialQuery)
                }
            }
        )
        return
    }

    val tabs = listOf(
        NavigationTab("Home", Icons.Default.Home),
        NavigationTab("Tools", Icons.Default.Build),
        NavigationTab("Memory", Icons.Default.Memory),
        NavigationTab("Automations", Icons.Default.Schedule),
        NavigationTab("Settings", Icons.Default.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = activeTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(index) },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) NeonCyan else TextSecondary
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                color = if (isSelected) BrightCyan else TextSecondary,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = DarkBackground
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = DarkBackground
        ) {
            when (activeTab) {
                0 -> HomeScreen(
                    state = assistantState,
                    audioLevel = audioLevel,
                    chatHistory = chatHistory,
                    hasMicPermission = hasMicPermission,
                    onMicClick = {
                        if (hasMicPermission) {
                            viewModel.triggerVoiceAssistant()
                        } else {
                            requestPermissions()
                        }
                    },
                    onCommandClick = { query -> viewModel.processUserCommand(query) },
                    onRequestPermissions = { requestPermissions() }
                )
                1 -> ToolsScreen(
                    isTorchOn = isTorchActive,
                    onToggleTorch = { viewModel.toggleTorch() },
                    onAdjustVolume = { increase -> viewModel.adjustVolume(increase) },
                    onOpenApp = { app -> viewModel.openApp(app) },
                    onMakeCall = { contact -> viewModel.makeCall(contact) },
                    onOpenSettings = { setting -> viewModel.openSettings(setting) },
                    installedApps = installedApps
                )
                2 -> MemoryScreen(
                    memories = memories,
                    onAddMemory = { k, v, c -> viewModel.addMemory(k, v, c) },
                    onDeleteMemory = { id -> viewModel.deleteMemory(id) },
                    onClearAll = { viewModel.clearAllMemories() }
                )
                3 -> AutomationsScreen(
                    automations = automations,
                    onToggleAutomation = { id, en -> viewModel.toggleAutomation(id, en) }
                )
                4 -> SettingsScreen(
                    currentProvider = selectedLlmProvider,
                    currentApiKey = apiKey,
                    currentEndpointUrl = llmEndpointUrl,
                    isVerified = isLlmVerified,
                    currentAutonomyLevel = autonomyLevel,
                    onSaveAutonomyLevel = { level -> viewModel.saveAutonomyLevel(level) },
                    onValidateLlmConfig = { provider, key, endpoint ->
                        viewModel.validateLlmConfig(provider, key, endpoint)
                    },
                    hasMicPermission = hasMicPermission,
                    hasNotificationPermission = hasNotificationPermission,
                    hasCallPermission = hasCallPermission,
                    hasContactsPermission = hasContactsPermission,
                    onRequestPermissions = { requestPermissions() },
                    onClearChatHistory = { viewModel.clearChatHistory() }
                )
            }

            // Floating Assistant HUD Bottom Sheet Overlay
            AssistantOverlay(
                isVisible = isOverlayVisible,
                state = assistantState,
                transcript = liveTranscript,
                audioLevel = audioLevel,
                card = currentCard,
                onDismiss = { viewModel.dismissOverlay() },
                onMicClick = { viewModel.triggerVoiceAssistant() },
                onTextSubmit = { text -> viewModel.processUserCommand(text) }
            )
        }
    }
}
