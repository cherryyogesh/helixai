package com.example.chitttiaiassistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chitttiaiassistant.model.ActionResponseCard
import com.example.chitttiaiassistant.ui.theme.AccentGreen
import com.example.chitttiaiassistant.ui.theme.BrightCyan
import com.example.chitttiaiassistant.ui.theme.CardBackground
import com.example.chitttiaiassistant.ui.theme.ElectricBlue
import com.example.chitttiaiassistant.ui.theme.GlowingBorder
import com.example.chitttiaiassistant.ui.theme.NeonCyan
import com.example.chitttiaiassistant.ui.theme.TextPrimary
import com.example.chitttiaiassistant.ui.theme.TextSecondary

@Composable
fun RenderActionCard(card: ActionResponseCard) {
    when (card) {
        is ActionResponseCard.None -> {}
        is ActionResponseCard.WeatherCard -> WeatherCardView(card)
        is ActionResponseCard.AppLaunchCard -> AppLaunchCardView(card)
        is ActionResponseCard.CallCard -> CallCardView(card)
        is ActionResponseCard.SmsCard -> SmsCardView(card)
        is ActionResponseCard.MediaCard -> MediaCardView(card)
        is ActionResponseCard.QaCard -> QaCardView(card)
        is ActionResponseCard.TorchCard -> TorchCardView(card)
        is ActionResponseCard.VolumeCard -> VolumeCardView(card)
        is ActionResponseCard.MemoryCard -> MemoryCardView(card)
        is ActionResponseCard.SystemInfoCard -> SystemInfoCardView(card)
    }
}

@Composable
fun WeatherCardView(card: ActionResponseCard.WeatherCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, GlowingBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = card.city, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = card.condition, color = BrightCyan, fontSize = 14.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "☀️", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = card.temp, color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                card.hourlyForecast.forEach { item ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(Color(0xFF1E2738), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(text = item.time, color = TextSecondary, fontSize = 12.sp)
                        Text(text = item.icon, fontSize = 18.sp)
                        Text(text = item.temp, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AppLaunchCardView(card: ActionResponseCard.AppLaunchCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, GlowingBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BoxIcon(Icons.Default.Info, BrightCyan)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "App Control", color = TextSecondary, fontSize = 12.sp)
                Text(text = card.message, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun CallCardView(card: ActionResponseCard.CallCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, ElectricBlue, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BoxIcon(Icons.Default.Call, AccentGreen)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "Calling ${card.contactName}", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(text = card.phoneNumber, color = TextSecondary, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = {}, colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)) {
                    Text("Confirm Call")
                }
            }
        }
    }
}

@Composable
fun SmsCardView(card: ActionResponseCard.SmsCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, GlowingBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BoxIcon(Icons.Default.Send, NeonCyan)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Message to ${card.contactName}", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\"${card.messageText}\"",
                color = BrightCyan,
                fontSize = 15.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E2738), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            )
        }
    }
}

@Composable
fun MediaCardView(card: ActionResponseCard.MediaCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, GlowingBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BoxIcon(Icons.Default.PlayArrow, ElectricBlue)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = card.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = "${card.artist} • ${card.appName}", color = BrightCyan, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun QaCardView(card: ActionResponseCard.QaCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, GlowingBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = card.question, color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = card.answer, color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Source: ${card.source}", color = BrightCyan, fontSize = 11.sp)
        }
    }
}

@Composable
fun TorchCardView(card: ActionResponseCard.TorchCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, if (card.isOn) NeonCyan else GlowingBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = if (card.isOn) "💡" else "🔌", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (card.isOn) "Flashlight is ON" else "Flashlight is OFF",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun VolumeCardView(card: ActionResponseCard.VolumeCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, GlowingBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🔊", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = card.streamName, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = "Level: ${card.levelPercent}%", color = BrightCyan, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun MemoryCardView(card: ActionResponseCard.MemoryCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, GlowingBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🧠", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = "${card.key}: ${card.value}", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(text = card.action, color = BrightCyan, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SystemInfoCardView(card: ActionResponseCard.SystemInfoCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, NeonCyan, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = card.title, color = BrightCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            card.infoDetails.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = label, color = TextSecondary, fontSize = 13.sp)
                    Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BoxIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Column(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
    }
}
