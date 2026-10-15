package com.maya.aiassistant

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var keyInput: EditText
    private lateinit var saveButton: Button
    private lateinit var clearHistoryButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        keyInput = findViewById(R.id.openaiKeyInput)
        saveButton = findViewById(R.id.saveSettingsButton)
        clearHistoryButton = findViewById(R.id.clearHistoryButton)

        val prefs = getSharedPreferences("maya_prefs", MODE_PRIVATE)
        keyInput.setText(prefs.getString("openai_api_key", ""))

        saveButton.setOnClickListener {
            val key = keyInput.text?.toString()?.trim().orEmpty()
            prefs.edit().putString("openai_api_key", key).apply()
            finish()
        }

        clearHistoryButton.setOnClickListener {
            prefs.edit().remove("history").apply()
        }
    }
}
