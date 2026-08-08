import "dotenv/config";
import express from "express";
import cors from "cors";
import { TOOLS } from "./tools.js";
import { verifySubscription, verifyProduct } from "./billing.js";

// Must match app/src/main/java/com/ai4biz/app/billing/PlanId.kt exactly.
const KNOWN_PRODUCTS = {
  ai4biz_monthly: { isSubscription: true },
  ai4biz_annual: { isSubscription: true },
  ai4biz_lifetime: { isSubscription: false }
};

const PORT = process.env.PORT || 3000;
const OPENROUTER_API_KEY = process.env.OPENROUTER_API_KEY;
const OPENROUTER_MODEL = process.env.OPENROUTER_MODEL || "anthropic/claude-sonnet-5";
const OPENROUTER_MAX_TOKENS = Number(process.env.OPENROUTER_MAX_TOKENS) || 2000;
const APP_SHARED_SECRET = process.env.APP_SHARED_SECRET || "";
const OPENROUTER_SITE_URL = process.env.OPENROUTER_SITE_URL || "";
const OPENROUTER_SITE_NAME = process.env.OPENROUTER_SITE_NAME || "Business Edge AI";

const MAX_FIELD_LENGTH = 4000;
const MAX_FIELDS = 20;

const app = express();
app.use(cors());
app.use(express.json({ limit: "256kb" }));

// Public liveness check -- must stay reachable without auth so host
// platforms (Render/Railway/Fly/...) can health-check this service.
app.get("/health", (_req, res) => {
  res.json({ status: "ok", model: OPENROUTER_MODEL });
});

// Lightweight app-level auth: NOT the OpenRouter key, just a shared secret
// so this proxy isn't an open relay burning your OpenRouter credits. Swap
// for real user auth (e.g. tied to accounts/subscriptions) before shipping.
app.use((req, res, next) => {
  if (!APP_SHARED_SECRET) return next(); // auth disabled if unset (dev convenience)
  const header = req.get("authorization") || "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : "";
  if (token !== APP_SHARED_SECRET) {
    return res.status(401).json({ error: "Unauthorized" });
  }
  next();
});

app.post("/api/generate", async (req, res) => {
  try {
    if (!OPENROUTER_API_KEY) {
      return res.status(500).json({ error: "Server misconfigured: OPENROUTER_API_KEY is not set." });
    }

    const { toolId, inputs } = req.body ?? {};
    const tool = TOOLS[toolId];
    if (!tool) {
      return res.status(400).json({ error: `Unknown toolId: ${toolId}` });
    }
    if (!inputs || typeof inputs !== "object" || Array.isArray(inputs)) {
      return res.status(400).json({ error: "inputs must be an object of field -> text" });
    }

    const entries = Object.entries(inputs).slice(0, MAX_FIELDS);
    const formattedInputs = entries
      .map(([key, value]) => `${key}: ${String(value ?? "").slice(0, MAX_FIELD_LENGTH)}`)
      .join("\n");

    const systemPrompt =
      "You are Business Edge AI, an assistant that generates polished, " +
      "ready-to-use business documents for entrepreneurs, freelancers, and " +
      "SMEs. Be concise and professional. Output only the document itself " +
      "-- no preamble, no \"Here's your document\", no closing commentary.";

    const userPrompt = `${tool.instructions}\n\nDetails provided by the user:\n${formattedInputs}`;

    const upstream = await fetch("https://openrouter.ai/api/v1/chat/completions", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${OPENROUTER_API_KEY}`,
        ...(OPENROUTER_SITE_URL ? { "HTTP-Referer": OPENROUTER_SITE_URL } : {}),
        "X-Title": OPENROUTER_SITE_NAME
      },
      body: JSON.stringify({
        model: OPENROUTER_MODEL,
        max_tokens: OPENROUTER_MAX_TOKENS,
        messages: [
          { role: "system", content: systemPrompt },
          { role: "user", content: userPrompt }
        ]
      })
    });

    if (!upstream.ok) {
      const errorBody = await upstream.text();
      console.error("OpenRouter error", upstream.status, errorBody);
      return res.status(502).json({ error: `AI provider error (${upstream.status})` });
    }

    const data = await upstream.json();
    const content = data?.choices?.[0]?.message?.content?.trim();
    if (!content) {
      return res.status(502).json({ error: "AI provider returned an empty response" });
    }

    res.json({ content, tool: tool.title, model: OPENROUTER_MODEL });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Internal server error" });
  }
});

app.post("/api/verify-purchase", async (req, res) => {
  try {
    const { productId, purchaseToken } = req.body ?? {};
    const product = KNOWN_PRODUCTS[productId];

    if (!product) {
      return res.status(400).json({ error: `Unknown productId: ${productId}` });
    }
    if (!purchaseToken || typeof purchaseToken !== "string") {
      return res.status(400).json({ error: "purchaseToken is required" });
    }

    const result = product.isSubscription
      ? await verifySubscription(productId, purchaseToken)
      : await verifyProduct(productId, purchaseToken);

    res.json(result);
  } catch (err) {
    // A malformed/replayed/fake token surfaces here as a Google API error
    // (typically 400/404) -- that's a real "not valid" signal, not a
    // server bug, so it's reported as valid:false rather than a 5xx.
    console.error("verify-purchase error", err?.response?.data || err.message);
    if (err.message?.includes("GOOGLE_SERVICE_ACCOUNT_JSON")) {
      return res.status(500).json({ error: "Server misconfigured: " + err.message });
    }
    res.json({ valid: false });
  }
});

app.listen(PORT, () => {
  console.log(`Ai4biz server listening on port ${PORT} (model: ${OPENROUTER_MODEL})`);
});
