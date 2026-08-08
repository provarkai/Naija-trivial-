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

/**
 * Verifies a subscription purchase token against the Google Play Developer
 * API -- this is the real anti-tampering check; a client (even a patched
 * one) can't fabricate a token that passes this.
 *
 * paymentState: 0 = pending, 1 = received, 2 = free trial,
 * 3 = pending deferred upgrade/downgrade.
 */
export async function verifySubscription(subscriptionId, purchaseToken) {
  const auth = await getAuthClient();
  const { data } = await androidpublisher.purchases.subscriptions.get({
    auth,
    packageName: PACKAGE_NAME,
    subscriptionId,
    token: purchaseToken
  });
  const expiryTimeMillis = data.expiryTimeMillis ? Number(data.expiryTimeMillis) : null;
  const notExpired = expiryTimeMillis === null || expiryTimeMillis > Date.now();
  const paymentOk = data.paymentState === 1 || data.paymentState === 2;
  return { valid: Boolean(notExpired && paymentOk), expiryTimeMillis };
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
