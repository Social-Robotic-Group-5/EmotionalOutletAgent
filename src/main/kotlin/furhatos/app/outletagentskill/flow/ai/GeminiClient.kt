package furhatos.app.outletagentskill.flow.ai

import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    fun getCompletion(
        userMessage: String,
        conversationHistory: List<Pair<String, String>> = emptyList(),
        systemPrompt: String = "You are Furhat, a friendly social robot. Keep responses brief (1-2 sentences) and conversational.",
        temperature: Double = 0.7,
        maxTokens: Int = 150 // Keep low for free tier efficiency
    ): String? {
        val contents = mutableListOf<Content>()

        // Add system prompt as first user message (Gemini doesn't have separate system role)
        contents.add(Content(
            parts = listOf(Part("$systemPrompt\n\nNow respond to the following conversation:")),
            role = "user"
        ))
        contents.add(Content(
            parts = listOf(Part("Understood. I'll act as Furhat and keep responses brief.")),
            role = "model"
        ))

        // Add conversation history
        conversationHistory.forEach { (role, text) ->
            contents.add(Content(
                parts = listOf(Part(text)),
                role = if (role == "user") "user" else "model"
            ))
        }

        // Add current user message
        contents.add(Content(
            parts = listOf(Part(userMessage)),
            role = "user"
        ))

        val requestBody = GeminiRequest(
            contents = contents,
            generationConfig = GenerationConfig(
                temperature = temperature,
                maxOutputTokens = maxTokens
            )
        )

        val json = gson.toJson(requestBody)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("$GEMINI_API_URL?key=$GEMINI_API_KEY")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val geminiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)

                // Log token usage (useful for monitoring free tier limits)
                geminiResponse.usageMetadata?.let { usage ->
                    println("Token usage - Prompt: ${usage.promptTokenCount}, Response: ${usage.candidatesTokenCount}, Total: ${usage.totalTokenCount}")
                }

                geminiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            } else {
                println("Gemini API error: ${response.code} - $responseBody")
                null
            }
        } catch (e: IOException) {
            println("Network error calling Gemini: ${e.message}")
            null
        } catch (e: Exception) {
            println("Unexpected error: ${e.message}")
            null
        }
    }
}