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
    onSaveAutonomyLevel: (AutonomyLevel) -> Unit,
    onValidateLlmConfig: suspend (LlmProvider, String, String) -> LlmValidationResult,
    hasMicPermission: Boolean,
    hasNotificationPermission: Boolean,
    hasCallPermission: Boolean,
    hasContactsPermission: Boolean,
    onRequestPermissions: () -> Unit,
    onClearChatHistory: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var pitch by remember { mutableFloatStateOf(1.0f) }
    var speed by remember { mutableFloatStateOf(1.0f) }
    var selectedProvider by remember { mutableStateOf(currentProvider) }
    var selectedAutonomy by remember { mutableStateOf(currentAutonomyLevel) }
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }
    var endpointInput by remember { mutableStateOf(currentEndpointUrl) }

    var isValidating by remember { mutableStateOf(false) }
    var validationResult by remember {
        mutableStateOf<LlmValidationResult?>(
            if (isVerified) LlmValidationResult(LlmConnectionStatus.CONNECTED, "Verified", currentProvider.displayName, "Active Model") else null
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
            Text(text = "Voice preferences, Multi-LLM AI models & security", color = TextSecondary, fontSize = 13.sp)

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
                        Text(text = "Active LLM: ${selectedProvider.displayName}", color = BrightCyan, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Autonomy Level Selector Card
            Text(text = "ASSISTANT AUTONOMY LEVEL", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlowingBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Autonomy Mode", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    AutonomyLevel.entries.forEach { level ->
                        val isSelected = selectedAutonomy == level
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkBackground)
                                .border(1.dp, if (isSelected) NeonCyan else GlowingBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedAutonomy = level
                                    onSaveAutonomyLevel(level)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = level.displayName, color = if (isSelected) NeonCyan else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    if (isSelected) Text(text = "✓ Active", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(text = level.description, color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Multi-LLM Engine Configuration Card
            Text(text = "AI LLM ENGINE CONFIGURATION", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
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
                        Text(text = "Select LLM Provider", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // Provider Chips
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
                                    LlmConnectionStatus.CONNECTED -> "✓ LLM Connected & Saved"
                                    null -> "Test & Connect ${selectedProvider.displayName}"
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

            // Voice & Speech Customization Card
            Text(text = "VOICE & SPEECH SYNTHESIS", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlowingBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Voice Pitch: ${String.format(java.util.Locale.US, "%.1f", pitch)}x", color = TextPrimary, fontSize = 14.sp)
                    Slider(
                        value = pitch,
                        onValueChange = { pitch = it },
                        valueRange = 0.5f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = BrightCyan)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Speech Rate: ${String.format(java.util.Locale.US, "%.1f", speed)}x", color = TextPrimary, fontSize = 14.sp)
                    Slider(
                        value = speed,
                        onValueChange = { speed = it },
                        valueRange = 0.5f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = BrightCyan)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Permissions Health Check Card
            Text(text = "PERMISSIONS HEALTH CHECK", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlowingBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PermissionRow("Microphone Access", hasMicPermission)
                    PermissionRow("Notifications Indicator", hasNotificationPermission)
                    PermissionRow("Phone Calling Permission", hasCallPermission)
                    PermissionRow("Contacts Reading", hasContactsPermission)

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Grant / Fix Permissions", color = TextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Privacy & Logs
            Text(text = "PRIVACY & HISTORY CONTROLS", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onClearChatHistory,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentError),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentError)
            ) {
                Text("Clear Conversation History")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PermissionRow(title: String, isGranted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = TextPrimary, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isGranted) Icons.Default.Check else Icons.Default.Security,
                contentDescription = null,
                tint = if (isGranted) AccentGreen else AccentError,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isGranted) "Granted" else "Missing",
                color = if (isGranted) AccentGreen else AccentError,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
