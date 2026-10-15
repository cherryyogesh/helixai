package com.example.chitttiaiassistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.example.chitttiaiassistant.MainActivity
import com.example.chitttiaiassistant.R
import com.example.chitttiaiassistant.repository.MemoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class ForegroundListeningService : Service(), TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private lateinit var phoneControlManager: PhoneControlManager
    private lateinit var memoryRepository: MemoryRepository
    private lateinit var commandRouter: CommandRouter

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        phoneControlManager = PhoneControlManager(applicationContext)
        memoryRepository = MemoryRepository(applicationContext)
        commandRouter = CommandRouter(phoneControlManager, memoryRepository)

        textToSpeech = TextToSpeech(applicationContext, this)
        initBackgroundSpeechRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_EXIT_HELIX) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Only start background audio capture if explicitly enabled by user
        if (memoryRepository.isBackgroundWakeWordEnabled()) {
            startListeningLoop()
        }
        return START_STICKY
    }

    private fun initBackgroundSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        if (memoryRepository.isBackgroundWakeWordEnabled()) {
                            mainHandler.postDelayed({ startListeningLoop() }, 3000)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            handleBackgroundVoiceCommand(text)
                        } else if (memoryRepository.isBackgroundWakeWordEnabled()) {
                            mainHandler.postDelayed({ startListeningLoop() }, 1500)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    private fun startListeningLoop() {
        if (!memoryRepository.isBackgroundWakeWordEnabled()) return
        try {
            if (SpeechRecognizer.isRecognitionAvailable(this)) {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                }
                speechRecognizer?.startListening(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleBackgroundVoiceCommand(rawText: String) {
        scope.launch {
            val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("EXTRA_START_VOICE", true)
                putExtra("EXTRA_INITIAL_QUERY", rawText)
            }
            startActivity(launchIntent)

            val result = commandRouter.processCommand(rawText)
            if (isTtsReady && result.spokenResponse.isNotEmpty()) {
                textToSpeech?.speak(result.spokenResponse, TextToSpeech.QUEUE_FLUSH, null, "BG_HELIX_UTTERANCE")
            }

            if (memoryRepository.isBackgroundWakeWordEnabled()) {
                mainHandler.postDelayed({ startListeningLoop() }, 4000)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.US
            isTtsReady = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Helix System-Wide Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs Helix over any app or home screen hands-free"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("EXTRA_START_VOICE", true)
        }
        val tapPendingIntent = PendingIntent.getActivity(
            this,
            0,
            tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val exitIntent = Intent(this, ForegroundListeningService::class.java).apply {
            action = ACTION_EXIT_HELIX
        }
        val exitPendingIntent = PendingIntent.getService(
            this,
            1,
            exitIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Helix System Voice Assistant")
            .setContentText("Helix service active • Tap to open Voice Assistant")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(tapPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "🎙️ Open HUD", tapPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "🛑 Exit Helix", exitPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "helix_listening_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_EXIT_HELIX = "com.example.chitttiaiassistant.ACTION_EXIT_HELIX"

        fun startService(context: Context) {
            val intent = Intent(context, ForegroundListeningService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ForegroundListeningService::class.java).apply {
                action = ACTION_EXIT_HELIX
            }
            context.startService(intent)
        }
    }
}
