# Backend database setup

The backend stores users, products, and cart items in MongoDB collections (`users`, `products`, and `cart_items`).

Set `MONGODB_URI` and `JWT_SECRET` before starting the backend. Never commit live credentials or share private JWT signing keys. For MongoDB Atlas, use the connection string from Atlas and include the database name, for example:

```powershell
$env:MONGODB_URI = "mongodb+srv://<username>:<password>@<cluster-host>/farmconnect?retryWrites=true&w=majority"
$env:JWT_SECRET = "replace-with-a-long-random-secret"
.\mvnw.cmd spring-boot:run
```

For a local MongoDB server, the default is `mongodb://localhost:27017/farmconnect`. Keep credentials out of source control and URL-encode any reserved characters in the username or password.

## OpenAI AI

The backend uses OpenAI for chat at `/api/ai/chat` and recipe suggestions at `/api/recipes/match`. Create an API key in your OpenAI account, then set it in `application-secrets.properties` as `OPENAI_API_KEY=your-key` (or set the `OPENAI_API_KEY` environment variable). Never commit or share the key.

The model defaults to `gpt-4.1-mini`. Override it with `OPENAI_MODEL`; use `OPENAI_BASE_URL` only for an OpenAI-compatible API endpoint. Without a key, recipe suggestions use the built-in fallback and chat reports that setup is required. If OpenAI reports exhausted API credits, recipe requests show a catalog-based fallback with an explanatory note; live LLM recipes require available OpenAI API credits.

## Deploying with AI enabled

The local `application-secrets.properties` file is for development and is not required in production. In your hosting provider's **backend environment variables / secrets** settings, add:

```text
OPENAI_API_KEY=<new OpenAI API key>
CORS_ALLOWED_ORIGINS=https://<your-frontend-domain>
MONGODB_URI=<your production MongoDB connection string>
JWT_SECRET=<a long random secret>
```

Set `OPENAI_API_KEY` only on the backend. Do not use a `VITE_` prefix or put the key in frontend files; Vite exposes `VITE_` variables to the browser. The backend also accepts the platform's `PORT` variable. Redeploy or restart the backend after changing its variables.

For the frontend build, set `VITE_API_BASE_URL=https://<your-backend-domain>/api` in the hosting provider's **frontend build environment variables**, then rebuild and redeploy the frontend. If the frontend and backend share an origin behind a reverse proxy, set `VITE_API_BASE_URL=/api` instead. Keep the frontend origin in `CORS_ALLOWED_ORIGINS` (include the scheme, with no trailing slash).

After deployment, open `https://<your-backend-domain>/api/ai/status`. It should report `configured: true` and `provider: OpenAI`. Chat and recipe requests then use the deployed backend's OpenAI key; the key never needs to be sent to the browser.

If your frontend host sets its own Content Security Policy, allow the backend origin in its `connect-src` directive as well.
## Razorpay Route payouts

The checkout uses Razorpay Orders with Route transfers. Farmers submit their bank details to Razorpay for linked-account verification; FarmConnect stores only the Razorpay account ID, payout status, and masked account ending. Route transfers are created with the order and settle after the buyer's payment is captured. FarmConnect commission is currently 0%; gateway costs are absorbed by the platform.

Set the platform's Razorpay credentials in the backend environment. Do not commit live credentials or place them in frontend code:

```powershell
$env:RAZORPAY_KEY_ID = "rzp_test_..."
$env:RAZORPAY_KEY_SECRET = "..."
$env:RAZORPAY_WEBHOOK_SECRET = "..."
```

Configure a Razorpay webhook to `https://<public-backend-host>/api/payments/razorpay/webhook` with the same webhook secret. Subscribe to `payment.captured`, `transfer.processed`, and `transfer.failed`. Use test-mode credentials and test linked accounts before enabling live payments. Razorpay Route activation, linked-account KYC, bank verification, and sufficient platform balance are required for live transfers.
