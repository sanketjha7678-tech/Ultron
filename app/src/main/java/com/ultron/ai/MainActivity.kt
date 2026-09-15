package com.ultron.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.ultron.ai.actions.UltronActionExecutor
import com.ultron.ai.domain.*
import com.ultron.ai.service.UltronVoiceService
import com.ultron.ai.ui.UltronHudScreen
import com.ultron.ai.voice.UltronSpeechRecognizer
import com.ultron.ai.voice.UltronTtsManager

class MainActivity : ComponentActivity() {

    private lateinit var ttsManager: UltronTtsManager
    private lateinit var speechRecognizer: UltronSpeechRecognizer
    private lateinit var router: UltronCommandRouter

    private var ultronState by mutableStateOf(UltronState.STANDBY)
    private var hudMessages = mutableStateListOf<HudMessage>()
    private var rmsLevel by mutableFloatStateOf(0f)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (audioGranted) {
            startVoiceService()
            listenForVoice()
        } else {
            speakAndDisplay("Microphone permission is required for voice commands.", UltronState.ERROR)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate()

        val executor = UltronActionExecutor(this)
        router = UltronCommandRouter(executor)

        ttsManager = UltronTtsManager(this) { isSpeaking ->
            ultronState = if (isSpeaking) UltronState.SPEAKING else UltronState.STANDBY
        }

        speechRecognizer = UltronSpeechRecognizer(
            context = this,
            onResult = { query -> processInput(query) },
            onErrorState = { _ -> 
                ultronState = UltronState.STANDBY 
            },
            onRmsChanged = { rms -> rmsLevel = rms }
        )

        setContent {
            UltronHudScreen(
                currentState = ultronState,
                hudMessages = hudMessages,
                rmsLevel = rmsLevel,
                onMicClick = { checkPermissionsAndListen() },
                onVisionClick = { speakAndDisplay("Vision mode ready. Select or capture an image.", UltronState.EXECUTING) }
            )
        }

        checkPermissionsAndListen()
    }

    private fun checkPermissionsAndListen() {
        val requiredPermissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            startVoiceService()
            listenForVoice()
        }
    }

    private fun listenForVoice() {
        ultronState = UltronState.LISTENING
        speechRecognizer.startListening()
    }

    private fun processInput(rawInput: String) {
        val cleanInput = rawInput.trim()
        hudMessages.add(HudMessage("USER", cleanInput, isUser = true))
        
        val inputToProcess = if (cleanInput.lowercase().startsWith("hello ultron")) {
            cleanInput.substringAfter("hello ultron").trim()
        } else cleanInput

        if (inputToProcess.isEmpty()) {
            speakAndDisplay("Listening, sir.", UltronState.SPEAKING)
            return
        }

        ultronState = UltronState.THINKING
        val result = router.processCommand(inputToProcess)

        when (result) {
            is CommandResult.Success -> {
                speakAndDisplay(result.responseMessage, UltronState.EXECUTING)
            }
            is CommandResult.Failure -> {
                speakAndDisplay(result.errorMessage, UltronState.ERROR)
            }
            is CommandResult.AmbiguousContact -> {
                val names = result.matches.joinToString(", ") { it.displayName }
                speakAndDisplay("Multiple contacts found: $names. Please specify.", UltronState.SPEAKING)
            }
        }
    }

    private fun speakAndDisplay(text: String, state: UltronState) {
        ultronState = state
        hudMessages.add(HudMessage("ULTRON", text, isUser = false))
        ttsManager.speak(text)
    }

    private fun startVoiceService() {
        val serviceIntent = Intent(this, UltronVoiceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        ttsManager.shutdown()
    }
}
