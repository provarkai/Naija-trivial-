package com.ai4biz.app.billing

import com.ai4biz.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Calls /server's /api/verify-purchase, which checks the purchase token
 * against the Google Play Developer API server-side -- the real
 * anti-tampering check (see server/billing.js). Reuses the same backend
 * URL/shared secret as [com.ai4biz.app.ai.RemoteAiGeneratorService].
 */
class RemotePurchaseVerifier(
    private val baseUrl: String = BuildConfig.AI4BIZ_BACKEND_URL,
    private val sharedSecret: String = BuildConfig.AI4BIZ_BACKEND_SECRET
) : PurchaseVerifier {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    override suspend fun verify(productId: String, purchaseToken: String, isSubscription: Boolean): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("productId", productId)
                    put("purchaseToken", purchaseToken)
                    put("isSubscription", isSubscription)
                }
                val body = payload.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                val requestBuilder = Request.Builder()
                    .url("${baseUrl.trimEnd('/')}/api/verify-purchase")
                    .post(body)
                if (sharedSecret.isNotBlank()) {
                    requestBuilder.addHeader("Authorization", "Bearer $sharedSecret")
                }

                client.newCall(requestBuilder.build()).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(IllegalStateException("Server error ${response.code}"))
                    }
                    val valid = JSONObject(responseBody).optBoolean("valid", false)
                    Result.success(valid)
                }
            } catch (t: Exception) {
                Result.failure(t)
            }
        }
}
