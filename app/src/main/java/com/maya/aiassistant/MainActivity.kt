package com.maya.aiassistant

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var transcriptView: TextView
    private lateinit var inputField: EditText
    private lateinit var speakButton: Button
    private lateinit var sendButton: Button
    private lateinit var demoActionButton: Button
    private lateinit var settingsButton: Button
    private lateinit var tts: TextToSpeech
    private lateinit var speechLauncher: ActivityResultLauncher<Intent>

    private lateinit var assistant: AssistantEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        transcriptView = findViewById(R.id.transcriptText)
        inputField = findViewById(R.id.queryInput)
        speakButton = findViewById(R.id.speakButton)
        sendButton = findViewById(R.id.sendButton)
        demoActionButton = findViewById(R.id.demoActionButton)
        settingsButton = findViewById(R.id.settingsButton)

        transcriptView.movementMethod = ScrollingMovementMethod()
        tts = TextToSpeech(this, this)

        assistant = AssistantEngine(this)

        speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.firstOrNull()?.orEmpty()
                if (spoken.isNotBlank()) {
                    inputField.setText(spoken)
                    handleUserInput(spoken)
                }
            }
        }

        speakButton.setOnClickListener { launchVoiceInput() }

        sendButton.setOnClickListener {
            val question = inputField.text?.toString()?.trim().orEmpty()
            if (question.isNotBlank()) {
                handleUserInput(question)
            }
        }

        demoActionButton.setOnClickListener {
            val demo = "Open YouTube and say hello to Maya AI"
            inputField.setText(demo)
            handleUserInput(demo)
        }

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        appendBubble("Maya", "Hello! I am Maya AI. Tap the mic or type a task like: \"Call 9876543210\", \"Send SMS\", or \"Open YouTube\".")
    }

    private fun launchVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your command")
        }

        try { speechLauncher.launch(intent) } catch (e: Exception) { appendBubble("System", "Voice input is not available on this device.") }
    }

    private fun handleUserInput(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            appendBubble("System", "Please enter a valid query.")
            return
        }

        inputField.setText("")
        appendBubble("You", cleanQuery)

        assistant.handleAsync(cleanQuery, this) { response ->
            runOnUiThread {
                appendBubble("Maya", response)
                speak(response)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = java.util.Locale.US
        }
    }

    private fun speak(text: String) {
        if (text.isBlank()) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "maya_ai_response")
    }

    private fun appendBubble(sender: String, text: String) {
        val current = transcriptView.text?.toString().orEmpty()
        val next = if (current.isBlank()) {
            "$sender: $text"
        } else {
            "$current\n\n$sender: $text"
        }
        transcriptView.text = next
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
