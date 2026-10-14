package com.example.chitttiaiassistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.chitttiaiassistant.model.AssistantState
import com.example.chitttiaiassistant.service.LlmConnectionStatus
import com.example.chitttiaiassistant.service.LlmProvider
import com.example.chitttiaiassistant.service.LlmValidationResult
import com.example.chitttiaiassistant.ui.components.HudOrb
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
fun OnboardingScreen(
    currentProvider: LlmProvider,
    currentApiKey: String,
    currentEndpointUrl: String,
    onValidateConfig: suspend (LlmProvider, String, String) -> LlmValidationResult,
    onRequestPermissionsAndEnterDashboard: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var onboardingStep by remember { mutableIntStateOf(1) } // 1: LLM Validation, 2: Permissions Setup

    var selectedProvider by remember { mutableStateOf(currentProvider) }
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }
    var endpointInput by remember { mutableStateOf(currentEndpointUrl) }

    var isValidating by remember { mutableStateOf(false) }
    var validationResult by remember { mutableStateOf<LlmValidationResult?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        HudOrb(state = AssistantState.IDLE, size = 110.dp)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Welcome to Helix",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (onboardingStep == 1) "Step 1 of 2: Connect Your AI LLM" else "Step 2 of 2: Mobile & Location Access",
            color = BrightCyan,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        if (onboardingStep == 1) {
            // STEP 1: Multi-LLM Provider Setup & Real Validation
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
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
                        Text(text = "Connect Your AI LLM Service", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Paste your API key to connect to your LLM service. Keys are stored securely on device.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Provider Selection Chips
                    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                        LlmProvider.entries.forEach { provider ->
                            val isSelected = selectedProvider == provider
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkBackground)
                                    .border(1.dp, if (isSelected) NeonCyan else GlowingBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedProvider = provider
                                        validationResult = null
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = provider.displayName, color = if (isSelected) NeonCyan else TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    if (isSelected) {
                                        Text(text = "✓ Selected", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
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
                                fontSize = 13.sp,
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
                                val result = onValidateConfig(selectedProvider, apiKeyInput, endpointInput)
                                isValidating = false
                                validationResult = result
                                if (result.status == LlmConnectionStatus.CONNECTED) {
                                    onboardingStep = 2
                                }
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
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isValidating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DarkBackground, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connecting to ${selectedProvider.displayName}...", color = DarkBackground, fontWeight = FontWeight.Bold)
                        } else {
                            Text(
                                text = when (validationResult?.status) {
                                    LlmConnectionStatus.CONNECTED -> "✓ Connected! Proceed to Step 2 ➔"
                                    null -> "Test Connection & Connect ➔"
                                    else -> "Retry LLM Connection"
                                },
                                color = DarkBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            // STEP 2: Mobile & Location Permissions Setup
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(1.dp, NeonCyan, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Enable Mobile & Location Access",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "To allow Helix to open apps, make calls, and provide live local weather, grant the required access below.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    PermissionItem("🎙️ Voice & Microphone", "To listen and respond hands-free")
                    PermissionItem("📱 Mobile Phone & App Control", "To place calls, send SMS, and open installed apps")
                    PermissionItem("📍 Live Location Access", "For accurate local weather forecasts and routines")

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onRequestPermissionsAndEnterDashboard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Grant Access & Open Dashboard 🚀",
                            color = DarkBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onRequestPermissionsAndEnterDashboard) {
            Text(text = "Skip to Dashboard", color = TextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun PermissionItem(title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(ElectricBlue.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = BrightCyan, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = desc, color = TextSecondary, fontSize = 11.sp)
        }
    }
}
