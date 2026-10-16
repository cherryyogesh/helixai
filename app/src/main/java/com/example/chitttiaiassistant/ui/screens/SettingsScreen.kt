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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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

    var selectedPrivacy by remember { mutableStateOf(currentPrivacyMode) }
    var bgWakeWordState by remember { mutableStateOf(isBgWakeWordEnabled) }

    var isValidating by remember { mutableStateOf(false) }
    var validationResult by remember {
        mutableStateOf<LlmValidationResult?>(
            LlmValidationResult(LlmConnectionStatus.CONNECTED, "Connected to Helix server", "Helix Server Managed", "Automatic")
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
            Text(text = "Voice privacy, AI server connection & system controls", color = TextSecondary, fontSize = 13.sp)

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
                        Text(text = "AI Connection: Server Managed", color = BrightCyan, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Server-Managed AI Connection Section (Zero API Key in App UI)
            Text(text = "AI CONNECTION", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AccentGreen, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "AI Server Status", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkBackground)
                            .border(1.dp, AccentGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Text(text = "Server Status: Connected to Helix server", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Provider Selection: Automatic", color = BrightCyan, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "AI Status: Ready", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your API keys are managed securely by the server. No API key is required in the app.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isValidating = true
                                val res = onValidateLlmConfig(currentProvider, "", currentEndpointUrl)
                                isValidating = false
                                validationResult = res
                            }
                        },
                        enabled = !isValidating,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isValidating) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DarkBackground, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reconnecting...", color = DarkBackground, fontWeight = FontWeight.Bold)
                        } else {
                            Text("Reconnect to AI Server 🔄", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
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
