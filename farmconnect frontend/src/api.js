const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "/api";
const SESSION_KEY = "farmconnect_session";

export function getAuthSession() {
  try {
    return JSON.parse(sessionStorage.getItem(SESSION_KEY) || "null");
  } catch {
    return null;
  }
}

export function saveAuthSession(session) {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function clearAuthSession() {
  sessionStorage.removeItem(SESSION_KEY);
}

export async function apiRequest(path, options = {}) {
  const session = getAuthSession();
  const headers = { ...options.headers };
  if (options.body) headers["Content-Type"] = "application/json";
  if (session?.token) headers.Authorization = `Bearer ${session.token}`;
  const canRetry = !options.method || options.method.toUpperCase() === "GET";
  let response;
  for (let attempt = 0; attempt < (canRetry ? 3 : 1); attempt += 1) {
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), 12_000);
    try {
      response = await fetch(`${API_BASE}${path}`, { ...options, headers, signal: controller.signal });
    } catch {
      if (attempt === (canRetry ? 2 : 0)) {
        throw new Error(navigator.onLine === false
          ? "You appear to be offline. Check your connection and try again."
          : "FarmConnect could not connect to the server. Please try again in a moment.");
      }
    } finally {
      window.clearTimeout(timeout);
    }
    if (response && !(canRetry && [502, 503, 504].includes(response.status) && attempt < 2)) break;
    await new Promise((resolve) => window.setTimeout(resolve, 300 * (attempt + 1)));
  }

  const responseBody = await response.text();
  let payload;
  try {
    payload = responseBody ? JSON.parse(responseBody) : {};
  } catch {
    payload = { error: responseBody };
  }
  if ((response.status === 401 || response.status === 403) && session?.token) {
    clearAuthSession();
    window.dispatchEvent(new Event("farmconnect:unauthorized"));
  }
  if ([503, 504].includes(response.status)) {
    throw new Error("FarmConnect backend is unavailable. Start the app with `npm run dev` from the frontend folder.");
  }
  if (!response.ok) throw new Error(payload.error || payload.message || payload.detail || "The request could not be completed.");
  return payload;
}
