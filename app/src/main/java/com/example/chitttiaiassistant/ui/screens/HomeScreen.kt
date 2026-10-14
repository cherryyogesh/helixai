package com.example.chitttiaiassistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chitttiaiassistant.model.AssistantState
import com.example.chitttiaiassistant.model.ChatMessage
import com.example.chitttiaiassistant.model.Sender
import com.example.chitttiaiassistant.ui.components.HudOrb
import com.example.chitttiaiassistant.ui.theme.AccentGreen
import com.example.chitttiaiassistant.ui.theme.BrightCyan
import com.example.chitttiaiassistant.ui.theme.CardBackground
import com.example.chitttiaiassistant.ui.theme.DarkBackground
import com.example.chitttiaiassistant.ui.theme.ElectricBlue
import com.example.chitttiaiassistant.ui.theme.GlowingBorder
import com.example.chitttiaiassistant.ui.theme.NeonCyan
import com.example.chitttiaiassistant.ui.theme.SurfaceDark
import com.example.chitttiaiassistant.ui.theme.TextPrimary
import com.example.chitttiaiassistant.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    state: AssistantState,
    audioLevel: Float,
    chatHistory: List<ChatMessage>,
    hasMicPermission: Boolean,
    onMicClick: () -> Unit,
    onCommandClick: (String) -> Unit,
    onRequestPermissions: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HELIX",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(text = "PERSONAL AI", color = NeonCyan, fontSize = 11.sp, letterSpacing = 2.sp)
                }

                // Permission Health Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (hasMicPermission) AccentGreen.copy(alpha = 0.15f) else ElectricBlue.copy(alpha = 0.2f))
                        .border(1.dp, if (hasMicPermission) AccentGreen else GlowingBorder, RoundedCornerShape(20.dp))
                        .clickable { if (!hasMicPermission) onRequestPermissions() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = if (hasMicPermission) AccentGreen else BrightCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasMicPermission) "Active" else "Permissions Check",
                        color = if (hasMicPermission) AccentGreen else BrightCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Hero HUD Orb Avatar
            HudOrb(
                state = state,
                audioLevel = audioLevel,
                size = 180.dp,
                onClick = onMicClick
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Listening status
            val statusLabel = when (state) {
                AssistantState.IDLE -> "Ready, say \"Hey Helix\""
                AssistantState.DISABLED -> "Microphone Disabled"
                AssistantState.PERMISSION_REQUIRED -> "Microphone Permission Required"
                AssistantState.LISTENING -> "Yes, I'm listening..."
                AssistantState.WAKE_WORD_DETECTED -> "Hey Helix Detected!"
                AssistantState.PROCESSING, AssistantState.THINKING -> "Thinking..."
                AssistantState.SPEAKING -> "Helix speaking..."
                AssistantState.ERROR -> "Tap mic to try again"
            }
            Text(
                text = statusLabel,
                color = BrightCyan,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Giant Mic Button
            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(if (state == AssistantState.LISTENING) BrightCyan else NeonCyan)
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Activate Voice Assistant",
                    tint = DarkBackground,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Quick Action Chips Header
            Text(
                text = "QUICK ACTIONS",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Actions Flow Row
            val quickActions = listOf(
                "💡 Flashlight On",
                "🔊 Volume Up",
                "📞 Call Amma",
                "▶️ Play Music",
                "🌤️ Weather Today",
                "💬 Open WhatsApp"
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickActions.forEach { action ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardBackground)
                            .border(1.dp, GlowingBorder, RoundedCornerShape(20.dp))
                            .clickable { onCommandClick(action.substring(2).trim()) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(text = action, color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "RECENT CONVERSATIONS",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Recent History Items
        if (chatHistory.isEmpty()) {
            item {
                Text(
                    text = "No recent voice commands yet. Tap the mic above!",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        } else {
            items(chatHistory.take(5)) { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onCommandClick(msg.text) },
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (msg.sender == Sender.USER) "👤" else "🤖",
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (msg.sender == Sender.USER) "You" else "Helix",
                                color = if (msg.sender == Sender.USER) BrightCyan else NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = msg.text, color = TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
