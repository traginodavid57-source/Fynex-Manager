package org.fynex.manager.core.ai

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object AiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun chat(
        userMessage: String,
        contextText: String? = null,
        settings: AiSettings
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fullPrompt = if (contextText != null && contextText.isNotEmpty()) {
                "Conteúdo do arquivo / contexto:\n```\n${contextText.take(15000)}\n```\n\nPergunta ou solicitação do usuário:\n$userMessage"
            } else {
                userMessage
            }

            val responseText = when (settings.provider) {
                AiProvider.GEMINI -> callGemini(fullPrompt, settings)
                AiProvider.OPENAI, AiProvider.GROQ -> callOpenAiCompatible(fullPrompt, settings)
                AiProvider.CLAUDE -> callClaude(fullPrompt, settings)
                AiProvider.OLLAMA -> callOllama(fullPrompt, settings)
            }

            Result.success(responseText)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun callGemini(prompt: String, settings: AiSettings): String {
        if (settings.apiKey.isBlank()) throw IllegalArgumentException("Chave de API do Gemini não configurada! Vá nas configurações de IA e insira sua chave gratuita.")
        val model = if (settings.customModel.isNotBlank()) settings.customModel else settings.provider.defaultModel
        val url = "${settings.provider.defaultUrl}$model:generateContent?key=${settings.apiKey}"

        val bodyJson = JsonObject().apply {
            val contents = JsonArray().apply {
                val userContent = JsonObject().apply {
                    addProperty("role", "user")
                    val parts = JsonArray().apply {
                        val textPart = JsonObject().apply { addProperty("text", prompt) }
                        add(textPart)
                    }
                    add("parts", parts)
                }
                add(userContent)
            }
            add("contents", contents)

            val sysInstruction = JsonObject().apply {
                val parts = JsonArray().apply {
                    val p = JsonObject().apply { addProperty("text", settings.systemPrompt) }
                    add(p)
                }
                add("parts", parts)
            }
            add("systemInstruction", sysInstruction)
        }

        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toString().toRequestBody(jsonMedia))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string() ?: ""
                throw IllegalStateException("Erro Gemini (${response.code}): $err")
            }
            val resObj = gson.fromJson(response.body?.string(), JsonObject::class.java)
            val candidates = resObj.getAsJsonArray("candidates")
            if (candidates != null && candidates.size() > 0) {
                val content = candidates.get(0).asJsonObject.getAsJsonObject("content")
                val parts = content.getAsJsonArray("parts")
                if (parts != null && parts.size() > 0) {
                    return parts.get(0).asJsonObject.get("text").asString
                }
            }
            return "Sem resposta do Gemini."
        }
    }

    private fun callOpenAiCompatible(prompt: String, settings: AiSettings): String {
        val endpoint = if (settings.customEndpoint.isNotBlank()) settings.customEndpoint else settings.provider.defaultUrl
        val model = if (settings.customModel.isNotBlank()) settings.customModel else settings.provider.defaultModel

        val bodyJson = JsonObject().apply {
            addProperty("model", model)
            val messages = JsonArray().apply {
                val sysMsg = JsonObject().apply {
                    addProperty("role", "system")
                    addProperty("content", settings.systemPrompt)
                }
                add(sysMsg)

                val userMsg = JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", prompt)
                }
                add(userMsg)
            }
            add("messages", messages)
            addProperty("temperature", settings.temperature)
        }

        val reqBuilder = Request.Builder()
            .url(endpoint)
            .post(bodyJson.toString().toRequestBody(jsonMedia))

        if (settings.apiKey.isNotBlank()) {
            reqBuilder.addHeader("Authorization", "Bearer ${settings.apiKey}")
        }

        client.newCall(reqBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string() ?: ""
                throw IllegalStateException("Erro API (${response.code}): $err")
            }
            val resObj = gson.fromJson(response.body?.string(), JsonObject::class.java)
            val choices = resObj.getAsJsonArray("choices")
            if (choices != null && choices.size() > 0) {
                val message = choices.get(0).asJsonObject.getAsJsonObject("message")
                return message.get("content").asString
            }
            return "Sem resposta da API."
        }
    }

    private fun callClaude(prompt: String, settings: AiSettings): String {
        val model = if (settings.customModel.isNotBlank()) settings.customModel else settings.provider.defaultModel
        val bodyJson = JsonObject().apply {
            addProperty("model", model)
            addProperty("max_tokens", 4096)
            addProperty("system", settings.systemPrompt)
            val messages = JsonArray().apply {
                val userMsg = JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", prompt)
                }
                add(userMsg)
            }
            add("messages", messages)
        }

        val request = Request.Builder()
            .url(settings.provider.defaultUrl)
            .addHeader("x-api-key", settings.apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(bodyJson.toString().toRequestBody(jsonMedia))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string() ?: ""
                throw IllegalStateException("Erro Claude (${response.code}): $err")
            }
            val resObj = gson.fromJson(response.body?.string(), JsonObject::class.java)
            val content = resObj.getAsJsonArray("content")
            if (content != null && content.size() > 0) {
                return content.get(0).asJsonObject.get("text").asString
            }
            return "Sem resposta do Claude."
        }
    }

    private fun callOllama(prompt: String, settings: AiSettings): String {
        val endpoint = if (settings.customEndpoint.isNotBlank()) settings.customEndpoint else settings.provider.defaultUrl
        val model = if (settings.customModel.isNotBlank()) settings.customModel else settings.provider.defaultModel

        val bodyJson = JsonObject().apply {
            addProperty("model", model)
            addProperty("prompt", "${settings.systemPrompt}\n\n$prompt")
            addProperty("stream", false)
        }

        val request = Request.Builder()
            .url(endpoint)
            .post(bodyJson.toString().toRequestBody(jsonMedia))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string() ?: ""
                throw IllegalStateException("Erro Ollama (${response.code}): $err")
            }
            val resObj = gson.fromJson(response.body?.string(), JsonObject::class.java)
            return resObj.get("response")?.asString ?: "Sem resposta do Ollama."
        }
    }
}
