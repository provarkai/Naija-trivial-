#!/usr/bin/env node
// One-time admin script: creates the ai4biz_monthly / ai4biz_annual
// subscriptions and the ai4biz_lifetime managed product in Play Console,
// via the Android Publisher API. Safe to re-run -- an already-existing
// product is skipped rather than duplicated.
//
// Usage:
//   GOOGLE_SERVICE_ACCOUNT_JSON_BASE64=<...> node scripts/setup-billing-products.js
// or
//   GOOGLE_SERVICE_ACCOUNT_JSON_PATH=path/to/key.json node scripts/setup-billing-products.js
//
// Requires the service account to have "Manage store presence" (or
// broader) permission granted in Play Console -- see
// docs/PLAY_STORE_RELEASE.md "Set up billing products".
//
// If this fails with an "invalid regions version" style error, the error
// message names the current correct value -- rerun with
// REGIONS_VERSION=<that value> node scripts/setup-billing-products.js

import { google } from "googleapis";
import fs from "node:fs";

const PACKAGE_NAME = process.env.ANDROID_PACKAGE_NAME || "com.ai4biz.app";
// See https://support.google.com/googleplay/android-developer/answer/10532353
// -- bump via REGIONS_VERSION env var if Google's API rejects this value.
const REGIONS_VERSION = process.env.REGIONS_VERSION || "2022/02";

function loadCredentials() {
  const base64 = process.env.GOOGLE_SERVICE_ACCOUNT_JSON_BASE64;
  const path = process.env.GOOGLE_SERVICE_ACCOUNT_JSON_PATH;
  if (base64) return JSON.parse(Buffer.from(base64, "base64").toString("utf8"));
  if (path) return JSON.parse(fs.readFileSync(path, "utf8"));
  throw new Error(
    "Set GOOGLE_SERVICE_ACCOUNT_JSON_BASE64 or GOOGLE_SERVICE_ACCOUNT_JSON_PATH"
  );
}

async function getAuthClient() {
  const credentials = loadCredentials();
  const auth = new google.auth.GoogleAuth({
    credentials,
    scopes: ["https://www.googleapis.com/auth/androidpublisher"]
  });
  return auth.getClient();
}

const androidpublisher = google.androidpublisher("v3");

async function createSubscription(
  auth,
  { productId, basePlanId, title, description, benefits, billingPeriodDuration, priceUnits }
) {
  const subscription = {
    packageName: PACKAGE_NAME,
    productId,
    listings: [{ languageCode: "en-US", title, description, benefits }],
    basePlans: [
      {
        basePlanId,
        autoRenewingBasePlanType: {
          billingPeriodDuration,
          gracePeriodDuration: "P3D"
        },
        regionalConfigs: [
          {
            regionCode: "NG",
            newSubscriberAvailability: true,
            price: { currencyCode: "NGN", units: String(priceUnits), nanos: 0 }
          }
        ]
        // No otherRegionsConfig: launching NG-only by design (the app's
        // prices are Naira-denominated). Add it later with real
        // USD/EUR pricing to expand availability to new Play regions.
      }
    ]
  };

  try {
    await androidpublisher.monetization.subscriptions.create({
      auth,
      packageName: PACKAGE_NAME,
      productId,
      "regionsVersion.version": REGIONS_VERSION,
      requestBody: subscription
    });
    console.log(`Created subscription ${productId}`);
  } catch (err) {
    if (err?.code === 409) {
      console.log(`Subscription ${productId} already exists, skipping create`);
    } else {
      throw err;
    }
  }

  await androidpublisher.monetization.subscriptions.basePlans.activate({
    auth,
    packageName: PACKAGE_NAME,
    productId,
    basePlanId,
    requestBody: {}
  });
  console.log(`Activated base plan ${basePlanId} for ${productId}`);
}

async function createLifetimeProduct(auth) {
  const product = {
    packageName: PACKAGE_NAME,
    sku: "ai4biz_lifetime",
    status: "active",
    purchaseType: "managedUser",
    defaultLanguage: "en-US",
    defaultPrice: { priceMicros: "180000000000", currency: "NGN" },
    prices: { NG: { priceMicros: "180000000000", currency: "NGN" } },
    listings: {
      "en-US": {
        title: "Lifetime",
        description: "One-time payment, all future MVP tool updates included."
      }
    }
  };

  try {
    await androidpublisher.inappproducts.insert({
      auth,
      packageName: PACKAGE_NAME,
      requestBody: product
    });
    console.log("Created in-app product ai4biz_lifetime");
  } catch (err) {
    if (err?.code === 409) {
      console.log("ai4biz_lifetime already exists, skipping");
    } else {
      throw err;
    }
  }
}

async function main() {
  const auth = await getAuthClient();

  await createSubscription(auth, {
    productId: "ai4biz_monthly",
    basePlanId: "monthly",
    title: "Monthly",
    description: "Unlimited AI generations across all tools, no ads.",
    benefits: ["Unlimited generations", "No ads"],
    billingPeriodDuration: "P1M",
    priceUnits: 4500
  });

  await createSubscription(auth, {
    productId: "ai4biz_annual",
    basePlanId: "annual",
    title: "Annual",
    description: "Same as Monthly, priced for a full year.",
    benefits: ["Unlimited generations", "No ads", "2 months free vs. monthly"],
    billingPeriodDuration: "P1Y",
    priceUnits: 42000
  });

  await createLifetimeProduct(auth);

  console.log("Done. Products may take a few minutes to show as Active in Play Console.");
}

main().catch((err) => {
  console.error("Setup failed:");
  console.error(err?.response?.data ? JSON.stringify(err.response.data, null, 2) : err);
  process.exit(1);
});
