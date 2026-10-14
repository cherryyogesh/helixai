# Helix AI Assistant 🚀

Helix is a native Android AI Voice Assistant inspired by Google Assistant. Built with Kotlin, Jetpack Compose, Coroutines, StateFlow, Android SpeechRecognizer, TextToSpeech, and Multi-LLM provider support (Google Gemini, Anthropic Claude, OpenAI/Codex, Ollama Local LLM, and Custom LLM endpoints).

![Helix Banner](app/src/main/res/drawable/ic_launcher_foreground.xml)

---

## 🌟 Key Features

- **Hands-Free Voice Activation & Background Service**:
  - Wake phrase detection for `"Hey Helix"`.
  - Android System-Wide Digital Assistant role support (`android.intent.action.ASSIST`).
  - Persistent microphone background service (`ForegroundListeningService`) with Quick Settings Tile (`HelixTileService`).
  - System overlay window HUD that appears over any open application or mobile home screen.

- **Multi-LLM Engine Integration**:
  - Supports **Google Gemini** (`gemini-1.5-flash`), **Anthropic Claude 3.5**, **OpenAI / Codex** (`gpt-4o-mini`), **Ollama Local LLM**, and **Custom HTTP LLM Endpoints**.
  - Real-time API key validation and connection verification.
  - Zero hardcoded secrets – API keys are masked in the UI and stored securely on device.

- **On-Device Phone Controls**:
  - **App Launcher**: Exact/fuzzy matching to open installed apps (YouTube, WhatsApp, Camera, Chrome, Instagram, Spotify, etc.).
  - **Hardware Controls**: Flashlight/Torch toggle, Media/Ring/Alarm volume controls, Mute toggle, Battery status reporting, Device information.
  - **Calls & Messages**: Contact name resolution ("Amma = Mom"), dialer invocation, SMS drafting, and WhatsApp message pre-filling via deep link.
  - **System Settings**: Direct shortcuts to Wi-Fi, Bluetooth, Sound, Display, and Location settings.

- **Helix Learning Brain MCP**:
  - **Durable Task Memory**: Stores task execution histories, contexts, and results in an on-device SQLite database (`helix_learning_brain.db`).
  - **Feedback & Reusable Skills**: Evaluates task outcomes, calculates confidence scores (0.0–1.0), and converts successful patterns into versioned reusable skills.
  - **Sandboxed Practice Mode**: Isolated execution environment for safely testing tasks before production execution.
  - **Human Approval Safeguards**: Autonomy levels (`Suggest Only`, `Ask Before Execution`, `Execute Approved Workflows`, `Autonomous`) with emergency stop and audit logging.

- **Voice Pipeline Diagnostics & Testing**:
  - Complete 7-step voice diagnostics suite: Microphone Input, Wake-Word Detection, Speech-to-Text, Command Processing, LLM Connectivity, Text-to-Speech, and Audio Playback Output.

---

## 📱 Screenshots & UI Layout

1. **Onboarding & Multi-LLM Setup**: LLM provider selection and API key validation.
2. **Home Dashboard**: Animated glowing HUD Orb Avatar, live audio visualizer, quick action chips, and recent conversation cards.
3. **Phone Tools Dashboard**: Hardware toggles, volume controls, quick settings, app launcher with search.
4. **Helix Memory**: User facts, nicknames, and preference management.
5. **Settings & Privacy**: Voice pitch/rate customization, autonomy level selector, permissions health check, and clear history controls.

---

## 🛠️ Project Structure

```
app/src/main/java/com/example/chitttiaiassistant/
├── MainActivity.kt               # Entry activity with Assist intents & bottom navigation
├── MainViewModel.kt              # Main UI state coordinator
├── model/                        # Data models (AssistantState, CommandModels, AutonomyLevel)
├── repository/                   # Memory & Preferences persistence (MemoryRepository, AutomationRepository)
├── service/                      # Core background services
│   ├── HelixVoiceAssistant.kt    # SpeechRecognizer & TextToSpeech engine
│   ├── ForegroundListeningService.kt # Background voice listener
│   ├── HelixTileService.kt       # Quick Settings Tile
│   ├── CommandRouter.kt          # Intent parser & Gemini/LLM router
│   ├── PhoneControlManager.kt   # System API integrations (Camera, AudioManager, PackageManager)
│   ├── LlmService.kt             # Multi-LLM API client & key validator
│   └── VoiceDiagnostics.kt       # 7-step voice diagnostics suite
├── brain/                        # Learning Brain MCP Engine
│   ├── db/                       # SQLite entities & DB helper (LearningBrainDbHelper)
│   ├── sandbox/                  # PracticeSandbox
│   ├── evaluator/                # SkillEvaluator
│   ├── safeguards/               # ApprovalGuard
│   └── mcp/                      # LearningBrainMcpTools (14 MCP tools)
└── ui/                           # Jetpack Compose UI
    ├── components/               # HudOrb, AssistantOverlay, ActionCards
    ├── screens/                  # SplashScreen, OnboardingScreen, HomeScreen, ToolsScreen, MemoryScreen, AutomationsScreen, SettingsScreen
    └── theme/                    # Color, Theme, Type
```

---

## ⚙️ Building & Running

1. **Clone Repository**:
   ```bash
   git clone https://github.com/cherryyogesh/helixai.git
   cd helixai
   ```
2. **Open in Android Studio**:
   Open the project in Android Studio Ladybug (or newer).
3. **Build APK**:
   ```bash
   ./gradlew :app:assembleDebug
   ```
4. **Run Unit Tests**:
   ```bash
   ./gradlew :app:testDebugUnitTest
   ```

---

## 📄 License & Author

Developed by **cherryyogesh** as part of the Helix AI Assistant project.
