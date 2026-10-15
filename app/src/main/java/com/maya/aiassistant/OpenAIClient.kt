package com.maya.aiassistant

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.Executors

class OpenAIClient(private val apiKey: String) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    fun chatCompletion(prompt: String, history: List<String> = emptyList(), callback: (String) -> Unit) {
        executor.execute {
            try {
                val messages = mutableListOf<Map<String, String>>()
                messages.add(mapOf("role" to "system", "content" to "You are a helpful Android assistant named Maya. Respond concisely and when asked to perform device actions, return a JSON action tag in the format: [ACTION:type,data] but otherwise reply in natural language."))
                history.forEach { m -> messages.add(mapOf("role" to "user", "content" to m)) }
                messages.add(mapOf("role" to "user", "content" to prompt))

                val bodyObj = mapOf(
                    "model" to "gpt-3.5-turbo",
                    "messages" to messages,
                    "max_tokens" to 600,
                    "temperature" to 0.5
                )

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val bodyJson = gson.toJson(bodyObj)
                val request = Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(bodyJson.toRequestBody(mediaType))
                    .build()

                client.newCall(request).execute().use { resp ->
                    val respBody = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        handler.post { callback("(OpenAI error) ${resp.code}: $respBody") }
                        return@use
                    }

                    // Parse response
                    val tree = gson.fromJson(respBody, Map::class.java)
                    val choices = tree["choices"] as? List<*>
                    val first = choices?.firstOrNull() as? Map<*, *>
                    val message = (first?.get("message") as? Map<*, *>)?.get("content") as? String
                    val reply = message ?: "(no response)"
                    handler.post { callback(reply.trim()) }
                }
            } catch (e: Exception) {
                Log.e("OpenAIClient", "chatCompletion error", e)
                handler.post { callback("(error) ${e.message}") }
            }
        }
    }

    fun generateImage(prompt: String, callback: (String) -> Unit) {
        // Basic placeholder: call images endpoint (note: OpenAI images endpoint may vary)
        executor.execute {
            try {
                val bodyObj = mapOf(
                    "prompt" to prompt,
                    "n" to 1,
                    "size" to "1024x1024"
                )
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val bodyJson = gson.toJson(bodyObj)
                val request = Request.Builder()
                    .url("https://api.openai.com/v1/images/generations")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(bodyJson.toRequestBody(mediaType))
                    .build()

                client.newCall(request).execute().use { resp ->
                    val respBody = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        handler.post { callback("(Image error) ${resp.code}: $respBody") }
                        return@use
                    }
                    val tree = gson.fromJson(respBody, Map::class.java)
                    val data = tree["data"] as? List<*>
                    val url = ((data?.firstOrNull() as? Map<*, *>)?.get("url") as? String) ?: ""
                    handler.post { callback(url) }
                }
            } catch (e: Exception) {
                handler.post { callback("(error) ${e.message}") }
            }
        }
    }
}
