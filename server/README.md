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

Any Node 18+ host works (`npm start` runs `index.js`). A few free/cheap
options: Render, Railway, Fly.io, a Cloudflare Worker port, or a small VPS.
Whichever you pick:

1. Set `OPENROUTER_API_KEY`, `APP_SHARED_SECRET`, and (optionally)
   `OPENROUTER_MODEL` as environment variables on the host -- don't bake
   them into the image/repo.
2. Point the Android app at the deployed URL (see the root README's
   "Wiring in a real AI backend" section) via `local.properties`.

## Endpoints

- `GET /health` -- liveness check, returns the configured model.
- `POST /api/generate` -- body `{ "toolId": string, "inputs": { [key: string]: string } }`,
  returns `{ "content": string, "tool": string, "model": string }`.
  Requires `Authorization: Bearer <APP_SHARED_SECRET>` if that env var is
  set (leaving it unset disables auth -- fine for local dev, not for a
  public deployment).
