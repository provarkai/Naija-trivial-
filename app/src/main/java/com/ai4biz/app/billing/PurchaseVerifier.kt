package com.ai4biz.app.billing

/**
 * Confirms a purchase is genuine and active, rather than trusting the
 * on-device Play Billing client alone (which a patched app could fake).
 * See [RemotePurchaseVerifier] (real check, via /server ->  Google Play
 * Developer API) and [ClientOnlyPurchaseVerifier] (fallback when no
 * backend is configured -- see AppContainer).
 */
interface PurchaseVerifier {
    suspend fun verify(productId: String, purchaseToken: String, isSubscription: Boolean): Result<Boolean>
}
