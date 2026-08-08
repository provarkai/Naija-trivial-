package com.ai4biz.app.ai

import com.ai4biz.app.BuildConfig
import com.ai4biz.app.model.ToolType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Calls the backend proxy in /server, which in turn calls OpenRouter. The
 * OpenRouter API key never reaches the client -- only the lightweight
 * per-app [BuildConfig.AI4BIZ_BACKEND_SECRET] does, and only to stop the
 * proxy being an open relay (see server/README.md).
 *
 * [AppContainer] only constructs this when a backend URL is configured;
 * otherwise it falls back to [MockAiGeneratorService].
 */
class RemoteAiGeneratorService(
    private val baseUrl: String = BuildConfig.AI4BIZ_BACKEND_URL,
    private val sharedSecret: String = BuildConfig.AI4BIZ_BACKEND_SECRET
) : AiGeneratorService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun generate(toolType: ToolType, inputs: Map<String, String>): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("toolId", toolType.id)
                    put("inputs", JSONObject(inputs))
                }
                val body = payload.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                val requestBuilder = Request.Builder()
                    .url("${baseUrl.trimEnd('/')}/api/generate")
                    .post(body)
                if (sharedSecret.isNotBlank()) {
                    requestBuilder.addHeader("Authorization", "Bearer $sharedSecret")
                }

                client.newCall(requestBuilder.build()).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val message = runCatching { JSONObject(responseBody).optString("error") }
                            .getOrNull()
                            ?.takeIf { it.isNotBlank() }
                            ?: "Server error ${response.code}"
                        return@withContext Result.failure(IllegalStateException(message))
                    }
                    val content = JSONObject(responseBody).optString("content")
                    if (content.isBlank()) {
                        Result.failure(IllegalStateException("Empty response from AI backend"))
                    } else {
                        Result.success(content)
                    }
                }
            } catch (t: Exception) {
                Result.failure(t)
            }
        }
}
