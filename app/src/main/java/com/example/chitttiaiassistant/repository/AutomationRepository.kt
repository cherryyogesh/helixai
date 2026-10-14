package com.example.chitttiaiassistant.repository

import com.example.chitttiaiassistant.model.AutomationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AutomationRepository {

    private val _automations = MutableStateFlow<List<AutomationItem>>(
        listOf(
            AutomationItem(
                id = "1",
                title = "Morning Briefing",
                description = "7:00 AM daily - Read weather forecast, calendar events & top news",
                scheduleTime = "7:00 AM Daily",
                isEnabled = true,
                type = "Time Routine"
            ),
            AutomationItem(
                id = "2",
                title = "Work Focus Mode",
                description = "9:30 AM weekdays - Enable Do Not Disturb & mute ring tone",
                scheduleTime = "9:30 AM Mon-Fri",
                isEnabled = false,
                type = "Focus Routine"
            ),
            AutomationItem(
                id = "3",
                title = "Home Arrival Music",
                description = "On reaching home - Play favorite relaxing YouTube Music playlist",
                scheduleTime = "Geofence Trigger",
                isEnabled = true,
                type = "Location Routine"
            ),
            AutomationItem(
                id = "4",
                title = "Night Sleep Routine",
                description = "10:30 PM - Set volume to 20%, turn off flashlight & pause music",
                scheduleTime = "10:30 PM Daily",
                isEnabled = true,
                type = "Night Routine"
            )
        )
    )
    val automations: StateFlow<List<AutomationItem>> = _automations.asStateFlow()

    fun toggleAutomation(id: String, enabled: Boolean) {
        _automations.value = _automations.value.map { item ->
            if (item.id == id) item.copy(isEnabled = enabled) else item
        }
    }
}
