package com.maya.aiassistant

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var transcriptView: TextView
    private lateinit var inputField: EditText
    private lateinit var speakButton: Button
    private lateinit var sendButton: Button
    private lateinit var demoActionButton: Button
    private lateinit var tts: TextToSpeech
    private lateinit var speechLauncher: ActivityResultLauncher<Intent>

    private val assistant = AssistantEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        transcriptView = findViewById(R.id.transcriptText)
        inputField = findViewById(R.id.queryInput)
        speakButton = findViewById(R.id.speakButton)
        sendButton = findViewById(R.id.sendButton)
        demoActionButton = findViewById(R.id.demoActionButton)

        transcriptView.movementMethod = ScrollingMovementMethod()
        tts = TextToSpeech(this, this)

        speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.firstOrNull()?.orEmpty()
                if (spoken.isNotBlank()) {
                    inputField.setText(spoken)
                    handleUserInput(spoken)
                }
            }
        }

        speakButton.setOnClickListener {
            launchVoiceInput()
        }

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

        appendBubble("Maya", "Hello! I am Maya AI. Tap the mic or type a task like: \"Call 9876543210\", \"Send SMS\", or \"Open YouTube\".")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                appendBubble("System", "TTS language is not available; English fallback will still be used if supported.")
            }
        }
    }

    private fun launchVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your command")
        }

        try {
            speechLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            appendBubble("System", "Voice input is not available on this device. Please type your command.")
        }
    }

    private fun handleUserInput(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            appendBubble("System", "Please enter a valid query.")
            return
        }

        inputField.setText("")
        appendBubble("You", cleanQuery)

        val response = assistant.handle(cleanQuery, this)
        appendBubble("Maya", response)
        speak(response)
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
