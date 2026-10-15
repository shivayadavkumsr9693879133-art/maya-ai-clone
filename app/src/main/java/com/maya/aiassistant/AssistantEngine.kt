package com.maya.aiassistant

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings
import android.util.Log

class AssistantEngine(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("maya_prefs", Context.MODE_PRIVATE)

    fun handleAsync(query: String, activity: Activity, callback: (String) -> Unit) {
        val clean = query.trim()
        if (clean.isBlank()) {
            callback("Please say or type something so I can help.")
            return
        }

        // Simple local intents first
        val lower = clean.lowercase()
        if (lower.contains("call")) {
            val number = Regex("\\d{10,13}").find(clean)?.value
            if (number != null) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                activity.startActivity(intent)
                callback("Dialing $number now.")
                return
            }
        }

        if (lower.contains("sms") || lower.contains("message") || lower.contains("text")) {
            val number = Regex("\\d{10,13}").find(clean)?.value
            val message = extractMessage(clean)
            if (number != null) {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:$number")
                    putExtra("sms_body", message)
                }
                activity.startActivity(intent)
                callback("Opening SMS to $number.")
                return
            }
        }

        if (lower.contains("open") || lower.contains("launch")) {
            val appName = when {
                lower.contains("youtube") -> "com.google.android.youtube"
                lower.contains("whatsapp") -> "com.whatsapp"
                lower.contains("chrome") -> "com.android.chrome"
                lower.contains("settings") -> "android.settings.SETTINGS"
                else -> null
            }

            if (appName != null) {
                val intent = if (appName == "android.settings.SETTINGS") {
                    Intent(Settings.ACTION_SETTINGS)
                } else {
                    activity.packageManager.getLaunchIntentForPackage(appName)
                        ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appName"))
                }
                activity.startActivity(intent)
                callback("Opening the app now.")
                return
            }
        }

        // If an OpenAI API key is configured, use it for a smarter response
        val apiKey = prefs.getString("openai_api_key", null)
        if (!apiKey.isNullOrBlank()) {
            try {
                val client = OpenAIClient(apiKey)
                // Load last few history items
                val history = loadHistory().takeLast(6)
                client.chatCompletion(clean, history) { reply ->
                    saveToHistory("User: $clean")
                    saveToHistory("Maya: $reply")

                    // Check for image intent
                    if (lower.contains("generate image") || lower.contains("create image") || lower.contains("image of")) {
                        val imgPrompt = clean
                        client.generateImage(imgPrompt) { imgUrl ->
                            val final = if (imgUrl.startsWith("http")) "Image generated: $imgUrl" else "Image generation result: $imgUrl"
                            callback(final)
                        }
                    } else {
                        callback(reply)
                    }
                }
            } catch (e: Exception) {
                Log.e("AssistantEngine", "OpenAI call failed", e)
                callback("(AI error) ${e.message}")
            }
            return
        }

        // Fallback local responses
        val local = localFallback(clean)
        saveToHistory("User: $clean")
        saveToHistory("Maya: $local")
        callback(local)
    }

    private fun extractMessage(clean: String): String {
        val lower = clean.lowercase()
        val keywordIndex = listOf("saying", "message", "text", "sms").mapNotNull { keyword ->
            val idx = lower.indexOf(keyword)
            if (idx >= 0) idx else null
        }.minOrNull() ?: clean.length

        val result = clean.substring(maxOf(keywordIndex, 0) + 1).trim()
        return result.ifBlank { "Hello from Maya AI" }
    }

    private fun localFallback(clean: String): String {
        val lower = clean.lowercase()
        return when {
            lower.contains("hello") || lower.contains("hi") -> "Hello! I am Maya AI. I can help with calls, SMS, app launch, and quick smart assistant actions."
            lower.contains("weather") -> "Live weather needs an API key. I can integrate OpenWeather on request."
            lower.contains("what") && lower.contains("you") -> "I am a Maya-inspired AI assistant for Android. I can process voice commands, place calls, send SMS, open apps, and give quick replies."
            else -> "I understood: \"${if (clean.length>140) clean.take(120)+"..." else clean}\". I’m ready to help with calls, messages, app launch, and simple AI-style guidance."
        }
    }

    private fun saveToHistory(entry: String) {
        val h = loadHistory().toMutableList()
        h.add(entry)
        val joined = h.joinToString("\n")
        prefs.edit().putString("history", joined).apply()
    }

    private fun loadHistory(): List<String> {
        val s = prefs.getString("history", "") ?: ""
        if (s.isBlank()) return emptyList()
        return s.split("\n").map { it }
    }
}
