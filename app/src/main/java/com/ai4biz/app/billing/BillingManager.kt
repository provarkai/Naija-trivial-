package com.ai4biz.app.billing

import android.app.Activity
import android.content.Context
import com.ai4biz.app.data.repository.EntitlementRepository
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Thin wrapper around Google Play Billing. [isPremium] is the single source
 * of truth the rest of the app (ads, usage limits) checks -- but it's
 * backed by [EntitlementRepository], not Play Billing's own in-memory
 * purchase list directly. The distinction matters: Play Billing tells us
 * "does this device currently see a purchase"; [PurchaseVerifier] confirms
 * server-side that the purchase is real (not spoofed by a patched app) and
 * still active, and only that confirmed result is persisted and trusted.
 *
 * A transient verification failure (network blip, server hiccup) leaves
 * the last-known-good entitlement in place rather than revoking access;
 * only an explicit "not valid" from the verifier, or Play Billing itself
 * reporting no purchase, clears it.
 */
class BillingManager(
    private val context: Context,
    private val purchaseVerifier: PurchaseVerifier,
    private val entitlementRepository: EntitlementRepository
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(Dispatchers.Main)

    val isPremium: Flow<Boolean> = entitlementRepository.isPremium
    val activePlan: Flow<PlanId?> = entitlementRepository.planId.map { id -> PlanId.entries.find { it.productId == id } }

    private var productDetailsMap: Map<String, ProductDetails> = emptyMap()

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        queryProductDetails()
                        refreshPurchases()
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                // Play Billing recommends reconnecting with backoff before
                // the next purchase-related action; kept simple here since
                // startConnection() is safe to call again on demand.
            }
        })
    }

    private suspend fun queryProductDetails() {
        val subsProducts = listOf(PlanId.MONTHLY, PlanId.ANNUAL).map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it.productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val inAppProducts = listOf(PlanId.LIFETIME).map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it.productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val subsResult = billingClient.queryProductDetails(
            QueryProductDetailsParams.newBuilder().setProductList(subsProducts).build()
        )
        val inAppResult = billingClient.queryProductDetails(
            QueryProductDetailsParams.newBuilder().setProductList(inAppProducts).build()
        )

        productDetailsMap = (subsResult.productDetailsList.orEmpty() + inAppResult.productDetailsList.orEmpty())
            .associateBy { it.productId }
    }

    fun refreshPurchases() {
        scope.launch {
            val subs = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
            )
            val inApp = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
            )
            // Only act on a query that actually succeeded -- a failed query
            // is not evidence of "no purchase", so leave entitlement as-is.
            if (subs.billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                inApp.billingResult.responseCode == BillingClient.BillingResponseCode.OK
            ) {
                handlePurchases(subs.purchasesList + inApp.purchasesList)
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        val active = purchases.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        if (active == null) {
            scope.launch { entitlementRepository.setVerified(isPremium = false, planId = null) }
            return
        }

        val plan = PlanId.entries.find { p -> active.products.contains(p.productId) }

        if (!active.isAcknowledged) {
            scope.launch {
                billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(active.purchaseToken)
                        .build()
                )
            }
        }

        if (plan == null) return // Unrecognized product; nothing to verify against.

        scope.launch {
            purchaseVerifier.verify(plan.productId, active.purchaseToken, plan.isSubscription)
                .onSuccess { valid ->
                    entitlementRepository.setVerified(isPremium = valid, planId = if (valid) plan.productId else null)
                }
                .onFailure {
                    // Network/server error -- keep whatever was last
                    // verified rather than revoking on a transient failure.
                }
        }
    }

    fun launchPurchase(activity: Activity, plan: PlanId) {
        val details = productDetailsMap[plan.productId] ?: return

        val paramsList = if (plan.isSubscription) {
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .setOfferToken(offerToken)
                    .build()
            )
        } else {
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .build()
            )
        }

        billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder().setProductDetailsParamsList(paramsList).build()
        )
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        }
    }

    /** Formatted price string from Play Console, or null if not loaded yet (e.g. product doesn't exist there yet). */
    fun priceFor(plan: PlanId): String? {
        val details = productDetailsMap[plan.productId] ?: return null
        return if (plan.isSubscription) {
            details.subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
        } else {
            details.oneTimePurchaseOfferDetails?.formattedPrice
        }
    }
}
