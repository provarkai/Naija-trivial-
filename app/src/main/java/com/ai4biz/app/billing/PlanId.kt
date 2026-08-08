package com.ai4biz.app.billing

/**
 * Product IDs as configured in Play Console. MONTHLY/ANNUAL must be created
 * as Subscriptions; LIFETIME as a one-time In-app product. These exact
 * strings must match what's created there -- see docs/PLAY_STORE_RELEASE.md.
 */
enum class PlanId(val productId: String, val isSubscription: Boolean) {
    MONTHLY("ai4biz_monthly", isSubscription = true),
    ANNUAL("ai4biz_annual", isSubscription = true),
    LIFETIME("ai4biz_lifetime", isSubscription = false)
}
