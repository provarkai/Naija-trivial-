import { google } from "googleapis";

const PACKAGE_NAME = process.env.ANDROID_PACKAGE_NAME || "com.ai4biz.app";

let cachedAuthClient = null;

function loadCredentials() {
  const base64 = process.env.GOOGLE_SERVICE_ACCOUNT_JSON_BASE64;
  const raw = process.env.GOOGLE_SERVICE_ACCOUNT_JSON;
  const json = base64 ? Buffer.from(base64, "base64").toString("utf8") : raw;
  if (!json) {
    throw new Error(
      "Set GOOGLE_SERVICE_ACCOUNT_JSON_BASE64 (preferred) or GOOGLE_SERVICE_ACCOUNT_JSON " +
        "-- see docs/PLAY_STORE_RELEASE.md for how to create this service account."
    );
  }
  return JSON.parse(json);
}

async function getAuthClient() {
  if (cachedAuthClient) return cachedAuthClient;
  const credentials = loadCredentials();
  const auth = new google.auth.GoogleAuth({
    credentials,
    scopes: ["https://www.googleapis.com/auth/androidpublisher"]
  });
  cachedAuthClient = await auth.getClient();
  return cachedAuthClient;
}

const androidpublisher = google.androidpublisher("v3");

// Google removed `purchases.subscriptions.get` (the old v3 verification
// call) from current client libraries -- purchases.subscriptionsv2.get is
// the replacement and has a materially different shape (subscriptionState
// instead of paymentState, no subscriptionId in the request at all since
// one token can now cover multiple line items). Confirmed against the live
// discovery doc, not assumed from memory, since this is easy to get wrong
// with a stale mental model of the API.
const ACTIVE_SUBSCRIPTION_STATES = ["SUBSCRIPTION_STATE_ACTIVE", "SUBSCRIPTION_STATE_IN_GRACE_PERIOD"];

/**
 * Verifies a subscription purchase token against the Google Play Developer
 * API -- this is the real anti-tampering check; a client (even a patched
 * one) can't fabricate a token that passes this.
 */
export async function verifySubscription(subscriptionId, purchaseToken) {
  const auth = await getAuthClient();
  const { data } = await androidpublisher.purchases.subscriptionsv2.get({
    auth,
    packageName: PACKAGE_NAME,
    token: purchaseToken
  });

  const valid = ACTIVE_SUBSCRIPTION_STATES.includes(data.subscriptionState);
  const lineItem =
    data.lineItems?.find((item) => item.productId === subscriptionId) ?? data.lineItems?.[0];
  const expiryTimeMillis = lineItem?.expiryTime ? Date.parse(lineItem.expiryTime) : null;

  return { valid, expiryTimeMillis, subscriptionState: data.subscriptionState };
}

/**
 * Verifies a one-time (in-app) product purchase token.
 * purchaseState: 0 = purchased, 1 = canceled, 2 = pending.
 */
export async function verifyProduct(productId, purchaseToken) {
  const auth = await getAuthClient();
  const { data } = await androidpublisher.purchases.products.get({
    auth,
    packageName: PACKAGE_NAME,
    productId,
    token: purchaseToken
  });
  return { valid: data.purchaseState === 0 };
}
