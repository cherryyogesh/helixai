package com.example.chitttiaiassistant.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Settings
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.example.chitttiaiassistant.model.ActionResponseCard

data class InstalledAppInfo(
    val name: String,
    val packageName: String
)

class PhoneControlManager(private val context: Context) {

    private var isTorchOn = false

    fun sendMediaKeyEvent(keyCode: Int) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            audioManager.dispatchMediaKeyEvent(eventDown)
            audioManager.dispatchMediaKeyEvent(eventUp)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playYouTubeSearch(searchQuery: String): ActionResponseCard.MediaCard {
        val intent = Intent(Intent.ACTION_SEARCH).apply {
            setPackage("com.google.android.youtube")
            putExtra("query", searchQuery)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(searchQuery)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try { context.startActivity(webIntent) } catch (ex: Exception) { ex.printStackTrace() }
        }
        return ActionResponseCard.MediaCard(
            title = searchQuery,
            artist = "YouTube",
            appName = "YouTube",
            isPlaying = true
        )
    }

    fun findPhoneNumberForContact(queryName: String): Pair<String, String>? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        try {
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$queryName%"),
                null
            )
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val name = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    val number = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    return Pair(name, number)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun toggleTorch(enable: Boolean? = null): ActionResponseCard.TorchCard {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            val newState = enable ?: !isTorchOn
            cameraManager.setTorchMode(cameraId, newState)
            isTorchOn = newState
            ActionResponseCard.TorchCard(isOn = newState)
        } catch (e: Exception) {
            e.printStackTrace()
            ActionResponseCard.TorchCard(isOn = isTorchOn)
        }
    }

    fun isTorchActive(): Boolean = isTorchOn

    fun adjustVolume(streamType: Int = AudioManager.STREAM_MUSIC, increase: Boolean): ActionResponseCard.VolumeCard {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val stream = when (streamType) {
            AudioManager.STREAM_RING -> AudioManager.STREAM_RING
            AudioManager.STREAM_ALARM -> AudioManager.STREAM_ALARM
            else -> AudioManager.STREAM_MUSIC
        }
        val streamName = when (streamType) {
            AudioManager.STREAM_RING -> "Ringtone Volume"
            AudioManager.STREAM_ALARM -> "Alarm Volume"
            else -> "Media Volume"
        }

        val maxVol = audioManager.getStreamMaxVolume(stream)
        val currentVol = audioManager.getStreamVolume(stream)

        val newVol = if (increase) {
            (currentVol + 2).coerceAtMost(maxVol)
        } else {
            (currentVol - 2).coerceAtLeast(0)
        }

        audioManager.setStreamVolume(stream, newVol, AudioManager.FLAG_SHOW_UI)
        val percent = (newVol * 100) / maxVol
        return ActionResponseCard.VolumeCard(streamName = streamName, levelPercent = percent)
    }

    fun setVolumePercent(streamType: Int = AudioManager.STREAM_MUSIC, percent: Int): ActionResponseCard.VolumeCard {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val stream = when (streamType) {
            AudioManager.STREAM_RING -> AudioManager.STREAM_RING
            AudioManager.STREAM_ALARM -> AudioManager.STREAM_ALARM
            else -> AudioManager.STREAM_MUSIC
        }
        val streamName = when (streamType) {
            AudioManager.STREAM_RING -> "Ringtone Volume"
            AudioManager.STREAM_ALARM -> "Alarm Volume"
            else -> "Media Volume"
        }

        val maxVol = audioManager.getStreamMaxVolume(stream)
        val targetVol = (percent.coerceIn(0, 100) * maxVol) / 100

        audioManager.setStreamVolume(stream, targetVol, AudioManager.FLAG_SHOW_UI)
        return ActionResponseCard.VolumeCard(streamName = streamName, levelPercent = percent.coerceIn(0, 100))
    }

    fun setMute(mute: Boolean): ActionResponseCard.VolumeCard {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val stream = AudioManager.STREAM_MUSIC
        val maxVol = audioManager.getStreamMaxVolume(stream)

        if (mute) {
            audioManager.setStreamVolume(stream, 0, AudioManager.FLAG_SHOW_UI)
            return ActionResponseCard.VolumeCard(streamName = "Media Volume", levelPercent = 0)
        } else {
            val defaultVol = maxVol / 2
            audioManager.setStreamVolume(stream, defaultVol, AudioManager.FLAG_SHOW_UI)
            return ActionResponseCard.VolumeCard(streamName = "Media Volume", levelPercent = 50)
        }
    }

    fun launchAppByName(appNameQuery: String): ActionResponseCard.AppLaunchCard {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        val queryClean = appNameQuery.lowercase().trim()

        var matchedApp: ApplicationInfo? = null
        for (app in packages) {
            val appLabel = pm.getApplicationLabel(app).toString().lowercase()
            if (appLabel == queryClean || appLabel.contains(queryClean) || queryClean.contains(appLabel)) {
                if (pm.getLaunchIntentForPackage(app.packageName) != null) {
                    matchedApp = app
                    break
                }
            }
        }

        if (matchedApp != null) {
            val launchIntent = pm.getLaunchIntentForPackage(matchedApp.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                val label = pm.getApplicationLabel(matchedApp).toString()
                return ActionResponseCard.AppLaunchCard(
                    appName = label,
                    packageName = matchedApp.packageName,
                    success = true,
                    message = "Opening $label for you!"
                )
            }
        }

        return ActionResponseCard.AppLaunchCard(
            appName = appNameQuery,
            packageName = "",
            success = false,
            message = "App '$appNameQuery' not found on your device."
        )
    }

    fun getInstalledAppsList(): List<InstalledAppInfo> {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val result = mutableListOf<InstalledAppInfo>()

        for (app in packages) {
            val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
            if (launchIntent != null) {
                val label = pm.getApplicationLabel(app).toString()
                result.add(InstalledAppInfo(name = label, packageName = app.packageName))
            }
        }
        return result.sortedBy { it.name }
    }

    fun getBatteryStatus(): ActionResponseCard.SystemInfoCard {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val details = listOf(
            "Battery Level" to "$batteryPct%",
            "Charging State" to if (isCharging) "Charging ⚡" else "Discharging 🔋",
            "Power Saver" to "Off"
        )

        return ActionResponseCard.SystemInfoCard("Battery Status", details)
    }

    fun getDeviceStatus(): ActionResponseCard.SystemInfoCard {
        val details = listOf(
            "Device Model" to Build.MODEL,
            "Manufacturer" to Build.MANUFACTURER,
            "Android Version" to "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "Assistant Status" to "Helix Active & Ready"
        )
        return ActionResponseCard.SystemInfoCard("Device Information", details)
    }

    fun createAlarm(hour: Int, minute: Int, message: String): String {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            return "Setting alarm for ${String.format(java.util.Locale.US, "%02d:%02d", hour, minute)}."
        } catch (e: Exception) {
            e.printStackTrace()
            return "Unable to set alarm."
        }
    }

    fun createTimer(seconds: Int, message: String): String {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            val mins = seconds / 60
            return "Starting a $mins minute timer."
        } catch (e: Exception) {
            e.printStackTrace()
            return "Unable to start timer."
        }
    }

    fun makeCall(contactNameOrNumber: String): ActionResponseCard.CallCard {
        val contactMatch = findPhoneNumberForContact(contactNameOrNumber)
        val resolvedName = contactMatch?.first ?: contactNameOrNumber
        val rawNumber = contactMatch?.second ?: if (contactNameOrNumber.matches(Regex("^[0-9+ -]+$"))) contactNameOrNumber else "+18005550199"
        val cleanNumber = rawNumber.replace(" ", "").replace("-", "")

        val callIntent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$cleanNumber")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(callIntent)
        } catch (e: SecurityException) {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try { context.startActivity(dialIntent) } catch (ex: Exception) { ex.printStackTrace() }
        } catch (e: Exception) {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try { context.startActivity(dialIntent) } catch (ex: Exception) { ex.printStackTrace() }
        }

        return ActionResponseCard.CallCard(
            contactName = resolvedName,
            phoneNumber = cleanNumber,
            needsConfirmation = false
        )
    }

    fun composeSms(contactName: String, text: String): ActionResponseCard.SmsCard {
        val contactMatch = findPhoneNumberForContact(contactName)
        val resolvedName = contactMatch?.first ?: contactName
        val resolvedNumber = contactMatch?.second ?: ""

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse(if (resolvedNumber.isNotEmpty()) "smsto:$resolvedNumber" else "smsto:")
            putExtra("sms_body", text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ActionResponseCard.SmsCard(
            contactName = resolvedName,
            messageText = text,
            needsConfirmation = false
        )
    }

    fun openWhatsAppMessage(contactName: String, text: String): ActionResponseCard.SmsCard {
        val contactMatch = findPhoneNumberForContact(contactName)
        val resolvedName = contactMatch?.first ?: contactName

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(text)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ActionResponseCard.SmsCard(
            contactName = "$resolvedName (WhatsApp)",
            messageText = text,
            needsConfirmation = false
        )
    }

    fun openSettingsPanel(action: String) {
        val settingIntent = when (action.lowercase()) {
            "wifi", "wi-fi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
            "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            "location" -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            "sound", "volume" -> Intent(Settings.ACTION_SOUND_SETTINGS)
            "display", "brightness" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            "application", "apps" -> Intent(Settings.ACTION_APPLICATION_SETTINGS)
            else -> Intent(Settings.ACTION_SETTINGS)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(settingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
