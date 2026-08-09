package com.ai4biz.app.ai

import com.ai4biz.app.BuildConfig
import com.ai4biz.app.model.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Calls the backend proxy's `/api/assistant/message` (see server/README.md)
 * -- same host/shared-secret as [RemoteAiGeneratorService], same
 * OkHttp+org.json pattern (this codebase deliberately has no Retrofit or
 * kotlinx-serialization anywhere).
 *
 * [AppContainer] only constructs this when a backend URL is configured;
 * otherwise it falls back to [MockAssistantService].
 */
class RemoteAssistantService(
    private val baseUrl: String = BuildConfig.AI4BIZ_BACKEND_URL,
    private val sharedSecret: String = BuildConfig.AI4BIZ_BACKEND_SECRET
) : AssistantService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun sendMessage(
        message: String,
        history: List<ConversationTurn>,
        businessContext: String?
    ): Result<AssistantResponse> = withContext(Dispatchers.IO) {
        try {
            val historyArray = JSONArray().apply {
                history.forEach { turn ->
                    put(
                        JSONObject().apply {
                            put("role", if (turn.role == MessageRole.USER) "user" else "assistant")
                            put("content", turn.content)
                        }
                    )
                }
            }
            val payload = JSONObject().apply {
                put("message", message)
                put("history", historyArray)
                if (!businessContext.isNullOrBlank()) put("businessContext", businessContext)
            }
            val body = payload.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val requestBuilder = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/api/assistant/message")
                .post(body)
            if (sharedSecret.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $sharedSecret")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errorMessage = runCatching { JSONObject(responseBody).optString("error") }
                        .getOrNull()
                        ?.takeIf { it.isNotBlank() }
                        ?: "Server error ${response.code}"
                    return@withContext Result.failure(IllegalStateException(errorMessage))
                }

                val json = JSONObject(responseBody)
                val reply = json.optString("reply")
                if (reply.isBlank()) {
                    return@withContext Result.failure(IllegalStateException("Empty response from assistant"))
                }
                val suggestedTools = json.optJSONArray("suggestedTools")?.let { array ->
                    (0 until array.length()).map { index ->
                        val tool = array.getJSONObject(index)
                        SuggestedTool(toolId = tool.optString("toolId"), reason = tool.optString("reason"))
                    }
                }.orEmpty()

                Result.success(AssistantResponse(reply, suggestedTools))
            }
        } catch (t: Exception) {
            Result.failure(t)
        }
    }
}
