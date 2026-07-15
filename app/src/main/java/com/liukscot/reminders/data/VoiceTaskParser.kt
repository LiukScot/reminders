package com.liukscot.reminders.data

import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

// One interface, two providers (see architecture.md → Voice/AI). Takes the on-device transcript and
// returns a draft the user confirms. Throws on transport/HTTP/parse failure so the caller shows an
// error rather than a silent no-op.
interface VoiceTaskParser {
    suspend fun parse(transcript: String, today: LocalDate, lists: List<String>): VoiceTaskDraft
}

// NOTE: request shapes and model ids follow each provider's documented format but haven't yet been
// exercised against the live APIs (no key on hand). Isolated here so verifying with a real key later
// is a localized change; model ids are constants for the same reason.
private const val MISTRAL_URL = "https://api.mistral.ai/v1/chat/completions"
private const val MISTRAL_MODEL = "mistral-small-latest"
private const val GEMINI_MODEL = "gemini-2.5-flash"
private const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent"

class VoiceTaskParsers(
    private val keyStore: SecureKeyStore,
    private val http: OkHttpClient = defaultVoiceHttpClient(),
) {
    // null when the chosen provider has no key stored yet — the caller prompts the user to add one.
    fun forProvider(provider: AiProvider): VoiceTaskParser? {
        val key = keyStore.apiKey(provider) ?: return null
        return when (provider) {
            AiProvider.MISTRAL -> MistralVoiceParser(key, http)
            AiProvider.GOOGLE -> GeminiVoiceParser(key, http)
        }
    }
}

// Mistral chat completions: system instruction + the transcript as the user turn, JSON-object mode.
private class MistralVoiceParser(
    private val apiKey: String,
    private val http: OkHttpClient,
) : VoiceTaskParser {
    override suspend fun parse(transcript: String, today: LocalDate, lists: List<String>): VoiceTaskDraft {
        val body = JSONObject().apply {
            put("model", MISTRAL_MODEL)
            put("temperature", 0)
            put("response_format", JSONObject().put("type", "json_object"))
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", voiceTaskInstruction(today, lists)))
                put(JSONObject().put("role", "user").put("content", transcript))
            })
        }
        val response = http.postJson(MISTRAL_URL, body, mapOf("Authorization" to "Bearer $apiKey"))
        val content = JSONObject(response).getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").getString("content")
        return parseVoiceTaskDraft(content)
    }
}

// Gemini generateContent: instruction + transcript as text parts, JSON forced with response_mime_type.
private class GeminiVoiceParser(
    private val apiKey: String,
    private val http: OkHttpClient,
) : VoiceTaskParser {
    override suspend fun parse(transcript: String, today: LocalDate, lists: List<String>): VoiceTaskDraft {
        val body = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray()
                            .put(JSONObject().put("text", voiceTaskInstruction(today, lists)))
                            .put(JSONObject().put("text", transcript)),
                    ),
                ),
            )
            put("generationConfig", JSONObject().put("response_mime_type", "application/json"))
        }
        val response = http.postJson(GEMINI_URL, body, mapOf("x-goog-api-key" to apiKey))
        val content = JSONObject(response).getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        return parseVoiceTaskDraft(content)
    }
}

private fun defaultVoiceHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .callTimeout(60, TimeUnit.SECONDS)
    .build()

private val JSON_MEDIA_TYPE = "application/json".toMediaType()

private suspend fun OkHttpClient.postJson(url: String, body: JSONObject, headers: Map<String, String>): String =
    withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .apply { headers.forEach { (name, value) -> addHeader(name, value) } }
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            check(response.isSuccessful) { "AI request failed (${response.code}): ${text.take(300)}" }
            text
        }
    }
