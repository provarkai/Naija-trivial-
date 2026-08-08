package com.ai4biz.app.billing

import android.app.Activity
import android.content.Context
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Thin wrapper around Google Play Billing. [isPremium] is the single source
 * of truth the rest of the app (ads, usage limits) checks to decide whether
 * to treat the current user as a paying subscriber.
 *
 * This is a client-only check (no server-side receipt verification), which
 * is the standard trade-off for a scaffold: fine for legitimate users,
 * not resistant to a determined attacker patching the app. Add server-side
 * verification via the Play Developer API before this matters for revenue
 * at scale.
 */
class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _activePlan = MutableStateFlow<PlanId?>(null)
    val activePlan: StateFlow<PlanId?> = _activePlan.asStateFlow()

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
            handlePurchases(subs.purchasesList + inApp.purchasesList)
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        val active = purchases.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        if (active == null) {
            _isPremium.value = false
            _activePlan.value = null
            return
        }

        _isPremium.value = true
        _activePlan.value = PlanId.entries.find { plan -> active.products.contains(plan.productId) }

        if (!active.isAcknowledged) {
            scope.launch {
                billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(active.purchaseToken)
                        .build()
                )
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
