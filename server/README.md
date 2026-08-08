# Ai4biz server

A small Express proxy that sits between the Android app and
[OpenRouter](https://openrouter.ai). It holds the OpenRouter API key; the
app only ever sees a lightweight shared secret for this endpoint, never the
real key.

```
Android app --(Bearer APP_SHARED_SECRET)--> this server --(OpenRouter key)--> OpenRouter --> model
```

## Run locally

```bash
cd server
npm install
cp .env.example .env
# edit .env: set OPENROUTER_API_KEY at minimum
npm start
```

Check it's up:

```bash
curl http://localhost:3000/health
```

Test a generation (adjust the fields to match a tool -- see `tools.js` for
the five tool ids: `business_plan`, `proposal`, `invoice_receipt`,
`social_media_content`, `whatsapp_reply`):

```bash
curl -X POST http://localhost:3000/api/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $APP_SHARED_SECRET" \
  -d '{
    "toolId": "whatsapp_reply",
    "inputs": {
      "customerMessage": "Do you deliver to Lekki?",
      "businessContext": "We deliver across Lagos, 2-3 business days",
      "tone": "warm and professional"
    }
  }'
```

## Deploying

Any Node 18+ host works (`npm start` runs `index.js`, and a `Dockerfile` is
included). A few free/cheap options: Render, Railway, Fly.io, a Cloudflare
Worker port, or a small VPS. Whichever you pick:

1. Set `OPENROUTER_API_KEY`, `APP_SHARED_SECRET`, and (optionally)
   `OPENROUTER_MODEL` / `OPENROUTER_MAX_TOKENS` / `GOOGLE_SERVICE_ACCOUNT_JSON_BASE64`
   as environment variables on the host -- don't bake them into the image/repo.
2. Point the Android app at the deployed URL (see the root README's
   "Real AI backend" section) via `local.properties`.

### Fly.io

A `fly.toml` is included (app name `ai4biz-server`, scales to zero when
idle). Currently deployed at **https://ai4biz-server.fly.dev**.

```bash
fly launch --no-deploy   # first time only, or reuse the included fly.toml
fly secrets set OPENROUTER_API_KEY=... APP_SHARED_SECRET=... OPENROUTER_MODEL=anthropic/claude-sonnet-5 OPENROUTER_MAX_TOKENS=2000
fly deploy
```

If `fly deploy`'s remote builder can't be reached from your network (e.g. a
gRPC-unfriendly proxy), build and push locally instead and point Fly at the
prebuilt image:

```bash
docker build -t registry.fly.io/ai4biz-server:latest .
fly auth docker
docker push registry.fly.io/ai4biz-server:latest
fly deploy --image registry.fly.io/ai4biz-server:latest
```

## Endpoints

- `GET /health` -- liveness check, returns the configured model.
- `POST /api/generate` -- body `{ "toolId": string, "inputs": { [key: string]: string } }`,
  returns `{ "content": string, "tool": string, "model": string }`.
  Requires `Authorization: Bearer <APP_SHARED_SECRET>` if that env var is
  set (leaving it unset disables auth -- fine for local dev, not for a
  public deployment).
- `POST /api/verify-purchase` -- body `{ "productId": string, "purchaseToken": string }`
  (`productId` must be one of `ai4biz_monthly`, `ai4biz_annual`,
  `ai4biz_lifetime`), returns `{ "valid": boolean, "expiryTimeMillis"?: number }`.
  Checks the purchase token against the Google Play Developer API --
  needs `GOOGLE_SERVICE_ACCOUNT_JSON_BASE64` set (see
  `docs/PLAY_STORE_RELEASE.md` "Set up server-side purchase verification"
  for how to create that service account). Without it, every call returns
  a 500 misconfiguration error and the Android app falls back to trusting
  Play Billing's client-side result alone.

Test it (needs a real purchase token, e.g. from a License Tester purchase):

```bash
curl -X POST http://localhost:3000/api/verify-purchase \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $APP_SHARED_SECRET" \
  -d '{"productId": "ai4biz_monthly", "purchaseToken": "<token from the app>"}'
```
