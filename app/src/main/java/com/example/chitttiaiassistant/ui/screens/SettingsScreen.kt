package com.example.chitttiaiassistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chitttiaiassistant.model.AutonomyLevel
import com.example.chitttiaiassistant.repository.PrivacyMode
import com.example.chitttiaiassistant.service.DiagnosticReport
import com.example.chitttiaiassistant.service.LlmConnectionStatus
import com.example.chitttiaiassistant.service.LlmProvider
import com.example.chitttiaiassistant.service.LlmValidationResult
import com.example.chitttiaiassistant.ui.theme.AccentError
import com.example.chitttiaiassistant.ui.theme.AccentGreen
import com.example.chitttiaiassistant.ui.theme.BrightCyan
import com.example.chitttiaiassistant.ui.theme.CardBackground
import com.example.chitttiaiassistant.ui.theme.DarkBackground
import com.example.chitttiaiassistant.ui.theme.ElectricBlue
import com.example.chitttiaiassistant.ui.theme.GlowingBorder
import com.example.chitttiaiassistant.ui.theme.NeonCyan
import com.example.chitttiaiassistant.ui.theme.TextPrimary
import com.example.chitttiaiassistant.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    currentProvider: LlmProvider,
    currentApiKey: String,
    currentEndpointUrl: String,
    isVerified: Boolean,
    currentAutonomyLevel: AutonomyLevel,
    selectedGeminiModel: String,
    currentPrivacyMode: PrivacyMode,
    isBgWakeWordEnabled: Boolean,
    diagnosticReport: DiagnosticReport?,
    onSaveAutonomyLevel: (AutonomyLevel) -> Unit,
    onSaveGeminiModel: (String) -> Unit,
    onSavePrivacyMode: (PrivacyMode) -> Unit,
    onSaveBgWakeWord: (Boolean) -> Unit,
    onRunDiagnostics: () -> Unit,
    onValidateLlmConfig: suspend (LlmProvider, String, String) -> LlmValidationResult,
    hasMicPermission: Boolean,
    hasNotificationPermission: Boolean,
    hasCallPermission: Boolean,
    hasContactsPermission: Boolean,
    onRequestPermissions: () -> Unit,
    onClearChatHistory: () -> Unit,
    onExitHelix: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var pitch by remember { mutableFloatStateOf(1.0f) }
    var speed by remember { mutableFloatStateOf(1.0f) }
    var selectedProvider by remember { mutableStateOf(currentProvider) }
    var selectedModel by remember { mutableStateOf(selectedGeminiModel) }
    var selectedAutonomy by remember { mutableStateOf(currentAutonomyLevel) }
    var selectedPrivacy by remember { mutableStateOf(currentPrivacyMode) }
    var bgWakeWordState by remember { mutableStateOf(isBgWakeWordEnabled) }

    var apiKeyInput by remember { mutableStateOf(currentApiKey) }
    var endpointInput by remember { mutableStateOf(currentEndpointUrl) }

    var isValidating by remember { mutableStateOf(false) }
    var validationResult by remember {
        mutableStateOf<LlmValidationResult?>(
            if (isVerified) LlmValidationResult(LlmConnectionStatus.CONNECTED, "Verified", currentProvider.displayName, selectedModel) else null
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SETTINGS & PRIVACY",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(text = "Voice privacy, Gemini models & system controls", color = TextSecondary, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(20.dp))

            // Profile Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlowingBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue.copy(alpha = 0.2f)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = BrightCyan, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "Helix User Profile", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Active LLM: ${selectedProvider.displayName} ($selectedModel)", color = BrightCyan, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Privacy & Microphone Mode Card
            Text(text = "MICROPHONE PRIVACY & WAKE WORD", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonCyan, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Microphone Activation Mode", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    PrivacyMode.entries.forEach { mode ->
                        val isSelected = selectedPrivacy == mode
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkBackground)
                                .border(1.dp, if (isSelected) NeonCyan else GlowingBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedPrivacy = mode
                                    onSavePrivacyMode(mode)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = mode.displayName, color = if (isSelected) NeonCyan else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    if (isSelected) Text(text = "✓ Active", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(text = mode.description, color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Background Wake-Word Detection", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Listen for 'Hey Helix' when app is minimized (Off by default)", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = bgWakeWordState,
                            onCheckedChange = {
                                bgWakeWordState = it
                                onSaveBgWakeWord(it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = NeonCyan)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // AI LLM Engine Configuration & Gemini Model Selector
            Text(text = "AI LLM CONFIGURATION & GEMINI MODELS", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        when (validationResult?.status) {
                            LlmConnectionStatus.CONNECTED -> AccentGreen
                            null -> NeonCyan
                            else -> AccentError
                        },
                        RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "LLM Provider & Model", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // Provider Selection Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LlmProvider.entries.forEach { provider ->
                            val isSelected = selectedProvider == provider
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkBackground)
                                    .border(1.dp, if (isSelected) NeonCyan else GlowingBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedProvider = provider
                                        validationResult = null
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = provider.displayName, color = if (isSelected) NeonCyan else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    if (isSelected) Text(text = "✓ Selected", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (selectedProvider == LlmProvider.GEMINI) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Gemini Model Selection:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        val geminiModels = listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash-exp")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            geminiModels.forEach { m ->
                                val isMSelected = selectedModel == m
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isMSelected) BrightCyan.copy(alpha = 0.2f) else DarkBackground)
                                        .border(1.dp, if (isMSelected) BrightCyan else GlowingBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedModel = m
                                            onSaveGeminiModel(m)
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = m.replace("gemini-", ""), color = if (isMSelected) BrightCyan else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedProvider == LlmProvider.OLLAMA || selectedProvider == LlmProvider.CUSTOM) {
                        OutlinedTextField(
                            value = endpointInput,
                            onValueChange = {
                                endpointInput = it
                                validationResult = null
                            },
                            label = { Text("${selectedProvider.displayName} Endpoint URL") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan, unfocusedBorderColor = GlowingBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    } else {
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = {
                                apiKeyInput = it
                                validationResult = null
                            },
                            label = { Text("${selectedProvider.displayName} API Key") },
                            placeholder = { Text("Paste your API key here...") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan, unfocusedBorderColor = GlowingBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    validationResult?.let { res ->
                        if (res.status != LlmConnectionStatus.CONNECTED) {
                            Text(
                                text = "⚠️ ${res.userMessage}",
                                color = AccentError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isValidating = true
                                validationResult = null
                                val res = onValidateLlmConfig(selectedProvider, apiKeyInput, endpointInput)
                                isValidating = false
                                validationResult = res
                            }
                        },
                        enabled = !isValidating,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (validationResult?.status) {
                                LlmConnectionStatus.CONNECTED -> AccentGreen
                                null -> NeonCyan
                                else -> AccentError
                            }
                        )
                    ) {
                        if (isValidating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DarkBackground, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connecting...", color = DarkBackground, fontWeight = FontWeight.Bold)
                        } else {
                            Text(
                                text = when (validationResult?.status) {
                                    LlmConnectionStatus.CONNECTED -> "✓ Connected & Saved"
                                    null -> "Test Gemini Connection ➔"
                                    else -> "Retry LLM Connection"
                                },
                                color = DarkBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // System Voice & Pipeline Diagnostics
            Text(text = "VOICE & SYSTEM DIAGNOSTICS", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlowingBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = onRunDiagnostics,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Run Voice & System Diagnostics 🧪", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }

                    diagnosticReport?.let { report ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (report.isOverallPassed) "✓ All 7 Diagnostic Checks Passed" else "⚠️ Some Diagnostic Checks Failed",
                            color = if (report.isOverallPassed) AccentGreen else AccentError,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        report.stepResults.forEach { step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "${step.stepNumber}. ${step.stepName}", color = TextPrimary, fontSize = 12.sp)
                                Text(
                                    text = if (step.isPassed) "PASS" else "FAIL",
                                    color = if (step.isPassed) AccentGreen else AccentError,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Exit Helix Action
            Text(text = "SYSTEM CONTROLS & EXIT", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onExitHelix,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentError),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("🛑 Exit Helix & Terminate Background Service", color = TextPrimary, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
