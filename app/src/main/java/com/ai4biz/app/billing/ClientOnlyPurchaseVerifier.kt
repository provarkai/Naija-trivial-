package com.ai4biz.app.billing

/**
 * No backend configured, so there's nothing to verify against -- trusts
 * whatever Google Play Billing reported client-side. This is the same
 * "works with zero setup" fallback the AI generator and PDF export follow;
 * it is not anti-tampering, only [RemotePurchaseVerifier] is.
 */
class ClientOnlyPurchaseVerifier : PurchaseVerifier {
    override suspend fun verify(productId: String, purchaseToken: String, isSubscription: Boolean): Result<Boolean> =
        Result.success(true)
}
