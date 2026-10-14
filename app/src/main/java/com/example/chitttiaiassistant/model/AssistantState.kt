package com.example.chitttiaiassistant.model

enum class AssistantState(val label: String) {
    IDLE("Ready"),
    DISABLED("Disabled"),
    PERMISSION_REQUIRED("Permission Required"),
    LISTENING("Listening..."),
    WAKE_WORD_DETECTED("Hey Helix Detected!"),
    PROCESSING("Processing..."),
    THINKING("Thinking..."),
    SPEAKING("Helix Speaking..."),
    ERROR("Error")
}
