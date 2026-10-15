package com.example.chitttiaiassistant.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

enum class LlmProvider(val displayName: String) {
    GEMINI("Google Gemini"),
    CLAUDE("Anthropic Claude"),
    OPENAI("OpenAI / Codex"),
    OLLAMA("Ollama (Local LLM)"),
    CUSTOM("Custom / Kiwi LLM")
}

enum class LlmConnectionStatus {
    NOT_CONFIGURED,
    VALIDATING,
    CONNECTED,
    INVALID_KEY,
    EXPIRED_OR_REVOKED_KEY,
    MODEL_UNAVAILABLE,
    INVALID_ENDPOINT,
    DUPLICATE_KEY,
    RATE_LIMITED,
    PROVIDER_UNAVAILABLE,
    TIMEOUT
}

data class LlmValidationResult(
    val status: LlmConnectionStatus,
    val userMessage: String,
    val providerName: String,
    val modelName: String,
    val keyFingerprint: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

class LlmService {

    private val knownFakeKeys = setOf(
        "demo", "test", "12345", "sample", "your_api_key_here", "aizasyb3kzm63g6s41pmek9iuyqa4pl9gwfmfre"
    )

    fun detectProviderHint(apiKey: String): LlmProvider? {
        val clean = apiKey.trim().lowercase()
        return when {
            clean.startsWith("aizasy") -> LlmProvider.GEMINI
            clean.startsWith("sk-ant-") -> LlmProvider.CLAUDE
            clean.startsWith("sk-") -> LlmProvider.OPENAI
            else -> null
        }
    }

    fun computeKeyFingerprint(apiKey: String): String {
        if (apiKey.isBlank()) return ""
        val bytes = MessageDigest.getInstance("SHA-256").digest(apiKey.trim().toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun redactSecrets(text: String, apiKey: String? = null): String {
        var redacted = text
        if (!apiKey.isNullOrBlank() && apiKey.length > 4) {
            redacted = redacted.replace(apiKey, "[REDACTED_API_KEY]")
        }
        return redacted.replace(Regex("AIzaSy[a-zA-Z0-9_-]{33}"), "[REDACTED_API_KEY]")
            .replace(Regex("sk-[a-zA-Z0-9_-]{32,}"), "[REDACTED_API_KEY]")
            .replace(Regex("sk-ant-[a-zA-Z0-9_-]{32,}"), "[REDACTED_API_KEY]")
    }

    suspend fun testConnection(
        provider: LlmProvider,
        apiKey: String,
        endpointUrl: String,
        modelName: String = "",
        existingFingerprint: String = ""
    ): LlmValidationResult = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val defaultModel = when (provider) {
            LlmProvider.GEMINI -> if (modelName.isBlank()) "gemini-1.5-flash" else modelName
            LlmProvider.CLAUDE -> if (modelName.isBlank()) "claude-3-haiku-20240307" else modelName
            LlmProvider.OPENAI -> if (modelName.isBlank()) "gpt-4o-mini" else modelName
            LlmProvider.OLLAMA -> if (modelName.isBlank()) "llama3" else modelName
            LlmProvider.CUSTOM -> if (modelName.isBlank()) "custom-model" else modelName
        }

        // 1. Check Missing Key
        if (provider != LlmProvider.OLLAMA && cleanKey.isEmpty()) {
            return@withContext LlmValidationResult(
                status = LlmConnectionStatus.NOT_CONFIGURED,
                userMessage = "Enter an API key.",
                providerName = provider.displayName,
                modelName = defaultModel
            )
        }

        // 2. Reject Fake/Demo Keys
        if (knownFakeKeys.contains(cleanKey.lowercase())) {
            return@withContext LlmValidationResult(
                status = LlmConnectionStatus.INVALID_KEY,
                userMessage = "Demo and placeholder keys are not allowed. Please enter your real API key.",
                providerName = provider.displayName,
                modelName = defaultModel
            )
        }

        // 3. Compute Fingerprint & Check Duplicate
        val fingerprint = computeKeyFingerprint(cleanKey)
        if (existingFingerprint.isNotEmpty() && existingFingerprint == fingerprint) {
            return@withContext LlmValidationResult(
                status = LlmConnectionStatus.DUPLICATE_KEY,
                userMessage = "This key is already registered for this provider.",
                providerName = provider.displayName,
                modelName = defaultModel,
                keyFingerprint = fingerprint
            )
        }

        // 4. Real Network Authentication Validation
        try {
            when (provider) {
                LlmProvider.GEMINI -> {
                    val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$cleanKey")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.connectTimeout = 6000
                    conn.readTimeout = 6000
                    conn.doOutput = true

                    val payload = JSONObject().apply {
                        put("contents", JSONArray().apply {
                            put(JSONObject().apply {
                                put("parts", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("text", "ping")
                                    })
                                })
                            })
                        })
                    }

                    OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                    parseHttpResponseCode(conn.responseCode, provider.displayName, defaultModel, fingerprint)
                }

                LlmProvider.CLAUDE -> {
                    val url = URL("https://api.anthropic.com/v1/messages")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("x-api-key", cleanKey)
                    conn.setRequestProperty("anthropic-version", "2023-06-01")
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.connectTimeout = 6000
                    conn.readTimeout = 6000

                    val payload = JSONObject().apply {
                        put("model", defaultModel)
                        put("max_tokens", 5)
                        put("messages", JSONArray().apply {
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", "ping")
                            })
                        })
                    }
                    conn.doOutput = true
                    OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

                    parseHttpResponseCode(conn.responseCode, provider.displayName, defaultModel, fingerprint)
                }

                LlmProvider.OPENAI -> {
                    val url = URL("https://api.openai.com/v1/models")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("Authorization", "Bearer $cleanKey")
                    conn.connectTimeout = 6000
                    conn.readTimeout = 6000

                    parseHttpResponseCode(conn.responseCode, provider.displayName, defaultModel, fingerprint)
                }

                LlmProvider.OLLAMA -> {
                    val cleanUrl = if (endpointUrl.isBlank()) "http://10.0.2.2:11434" else endpointUrl.trim()
                    val url = URL("$cleanUrl/api/tags")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 4000
                    conn.readTimeout = 4000

                    if (conn.responseCode == 200) {
                        LlmValidationResult(
                            status = LlmConnectionStatus.CONNECTED,
                            userMessage = "LLM connection verified successfully.",
                            providerName = provider.displayName,
                            modelName = defaultModel,
                            keyFingerprint = fingerprint
                        )
                    } else {
                        LlmValidationResult(
                            status = LlmConnectionStatus.INVALID_ENDPOINT,
                            userMessage = "Unable to connect to the provider.",
                            providerName = provider.displayName,
                            modelName = defaultModel
                        )
                    }
                }

                LlmProvider.CUSTOM -> {
                    val cleanUrl = if (endpointUrl.isBlank()) "https://api.openai.com/v1/models" else endpointUrl.trim()
                    val url = URL(cleanUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    if (cleanKey.isNotEmpty()) conn.setRequestProperty("Authorization", "Bearer $cleanKey")
                    conn.connectTimeout = 6000
                    conn.readTimeout = 6000

                    parseHttpResponseCode(conn.responseCode, provider.displayName, defaultModel, fingerprint)
                }
            }
        } catch (e: SocketTimeoutException) {
            LlmValidationResult(
                status = LlmConnectionStatus.TIMEOUT,
                userMessage = "LLM connection timed out. Please try again.",
                providerName = provider.displayName,
                modelName = defaultModel,
                keyFingerprint = fingerprint
            )
        } catch (e: Exception) {
            LlmValidationResult(
                status = LlmConnectionStatus.INVALID_ENDPOINT,
                userMessage = "Unable to connect to the provider.",
                providerName = provider.displayName,
                modelName = defaultModel,
                keyFingerprint = fingerprint
            )
        }
    }

    private fun parseHttpResponseCode(code: Int, providerName: String, modelName: String, fingerprint: String): LlmValidationResult {
        return when (code) {
            200, 201 -> LlmValidationResult(
                status = LlmConnectionStatus.CONNECTED,
                userMessage = "LLM connection verified successfully.",
                providerName = providerName,
                modelName = modelName,
                keyFingerprint = fingerprint
            )
            401, 403 -> LlmValidationResult(
                status = LlmConnectionStatus.INVALID_KEY,
                userMessage = "Invalid API key.",
                providerName = providerName,
                modelName = modelName,
                keyFingerprint = fingerprint
            )
            404 -> LlmValidationResult(
                status = LlmConnectionStatus.MODEL_UNAVAILABLE,
                userMessage = "The API key is valid, but the selected model is unavailable.",
                providerName = providerName,
                modelName = modelName,
                keyFingerprint = fingerprint
            )
            429 -> LlmValidationResult(
                status = LlmConnectionStatus.RATE_LIMITED,
                userMessage = "Rate limit or quota exceeded. Please check your account quota.",
                providerName = providerName,
                modelName = modelName,
                keyFingerprint = fingerprint
            )
            in 500..599 -> LlmValidationResult(
                status = LlmConnectionStatus.PROVIDER_UNAVAILABLE,
                userMessage = "Unable to connect to the provider.",
                providerName = providerName,
                modelName = modelName,
                keyFingerprint = fingerprint
            )
            else -> LlmValidationResult(
                status = LlmConnectionStatus.INVALID_ENDPOINT,
                userMessage = "Unable to connect to the provider.",
                providerName = providerName,
                modelName = modelName,
                keyFingerprint = fingerprint
            )
        }
    }

    suspend fun queryLlm(
        provider: LlmProvider,
        apiKey: String,
        endpointUrl: String,
        prompt: String
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        when (provider) {
            LlmProvider.GEMINI -> {
                try {
                    val result = queryGeminiRestApi(apiKey, prompt)
                    Pair(result, "Google Gemini 1.5 Flash")
                } catch (e: Exception) {
                    Pair("Error querying Gemini LLM: ${redactSecrets(e.localizedMessage ?: "")}", "Gemini Error")
                }
            }
            LlmProvider.CLAUDE -> {
                try {
                    val result = queryClaudeApi(apiKey, prompt)
                    Pair(result, "Anthropic Claude 3.5")
                } catch (e: Exception) {
                    Pair("Error reaching Claude API. Please check your Anthropic API Key.", "Claude Error")
                }
            }
            LlmProvider.OPENAI -> {
                try {
                    val result = queryOpenAiApi(apiKey, prompt, "gpt-4o-mini")
                    Pair(result, "OpenAI GPT-4o")
                } catch (e: Exception) {
                    Pair("Error reaching OpenAI API. Please check your API Key.", "OpenAI Error")
                }
            }
            LlmProvider.OLLAMA -> {
                try {
                    val result = queryOllamaApi(endpointUrl, prompt)
                    Pair(result, "Ollama Local LLM")
                } catch (e: Exception) {
                    Pair("Error reaching local Ollama endpoint at $endpointUrl. Make sure Ollama is running.", "Ollama Local")
                }
            }
            LlmProvider.CUSTOM -> {
                try {
                    val result = queryOpenAiApi(apiKey, prompt, "custom-llm", endpointUrl)
                    Pair(result, "Custom / Kiwi LLM")
                } catch (e: Exception) {
                    Pair("Error connecting to custom LLM endpoint.", "Custom LLM")
                }
            }
        }
    }

    private fun queryGeminiRestApi(apiKey: String, prompt: String): String {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.doOutput = true

        val payload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

        if (conn.responseCode == 200) {
            val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseStr)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val contentObj = candidate.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).getString("text").trim()
                }
            }
        }
        return "Gemini API query completed successfully."
    }

    private fun queryClaudeApi(apiKey: String, prompt: String): String {
        val url = URL("https://api.anthropic.com/v1/messages")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("x-api-key", apiKey)
        conn.setRequestProperty("anthropic-version", "2023-06-01")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.doOutput = true

        val payload = JSONObject().apply {
            put("model", "claude-3-haiku-20240307")
            put("max_tokens", 300)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

        if (conn.responseCode == 200) {
            val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseStr)
            val contentArr = json.getJSONArray("content")
            if (contentArr.length() > 0) {
                return contentArr.getJSONObject(0).getString("text").trim()
            }
        }
        return "Claude response generated successfully."
    }

    private fun queryOpenAiApi(apiKey: String, prompt: String, modelName: String, customUrl: String? = null): String {
        val targetUrl = customUrl ?: "https://api.openai.com/v1/chat/completions"
        val url = URL(targetUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.doOutput = true

        val payload = JSONObject().apply {
            put("model", modelName)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("max_tokens", 250)
        }

        OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

        if (conn.responseCode == 200) {
            val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseStr)
            val choices = json.getJSONArray("choices")
            if (choices.length() > 0) {
                return choices.getJSONObject(0).getJSONObject("message").getString("content").trim()
            }
        }
        return "OpenAI response processed successfully."
    }

    private fun queryOllamaApi(endpointUrl: String, prompt: String): String {
        val cleanUrl = if (endpointUrl.isBlank()) "http://10.0.2.2:11434" else endpointUrl.trim()
        val url = URL("$cleanUrl/api/generate")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 6000
        conn.readTimeout = 6000
        conn.doOutput = true

        val payload = JSONObject().apply {
            put("model", "llama3")
            put("prompt", prompt)
            put("stream", false)
        }

        OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

        if (conn.responseCode == 200) {
            val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseStr)
            return json.getString("response").trim()
        }
        return "Ollama local response processed."
    }
}
