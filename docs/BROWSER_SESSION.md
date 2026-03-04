# Browser session and 403 on admin actions

If you get **403 Forbidden** when deleting users (or other admin actions) even after logging in with the super-admin account, the backend may not be receiving your session cookie. Use these steps to verify and fix.

## 1. Use the same origin everywhere

- Open the app at **`http://localhost:4200`** (not `http://127.0.0.1:4200`).
- Cookies are scoped by host; mixing `localhost` and `127.0.0.1` can prevent the cookie from being sent.

## 2. Check what the backend sees

After logging in, open this URL in the **same tab** (so the cookie is sent):

```
http://localhost:4200/api/v1/auth/session-info
```

- **If you see JSON** like `{"authenticated":true,"principal":"admin@localhost","isAdmin":true}`  
  → The backend sees you and considers you admin. If the delete still returns 403, the issue is elsewhere (e.g. different request path or CORS).

- **If you see** `{"authenticated":false,"message":"No session or not logged in."}` or **401**  
  → The backend is **not** getting your session. The cookie is either not set or not sent.

## 3. Verify the cookie in DevTools

1. Open DevTools (F12) → **Application** (Chrome) or **Storage** (Firefox).
2. Under **Cookies** → **http://localhost:4200**, check for **`JSESSIONID`**.
   - If it’s missing after login, the cookie was never set or was rejected (e.g. SameSite/Secure).
3. In the **Network** tab, trigger the failing request (e.g. delete user).
4. Select that request and check **Request Headers**.
   - There should be a header: **`Cookie: JSESSIONID=...`**.
   - If `Cookie` is missing, the browser is not sending the session for that request.

## 4. Reset and try again

1. **Clear site data** for `http://localhost:4200`:  
   DevTools → Application → **Storage** → **Clear site data** (or clear cookies for localhost).
2. Close any other tabs for the app.
3. Open a **single** tab: `http://localhost:4200`.
4. Log in with the super-admin credentials (e.g. `admin@localhost` / `admin`).
5. Without refreshing, open `http://localhost:4200/api/v1/auth/session-info` in the same tab (or a new tab to the same origin).
6. Confirm `session-info` shows `"isAdmin": true`, then try the admin action again.

## 5. Try incognito / private window

Extensions or existing cookies can interfere. Open `http://localhost:4200` in an **incognito/private** window, log in, check `/api/v1/auth/session-info`, then try the delete.

## 6. Docker and proxy

With Docker Compose, the frontend at 4200 proxies `/api` to the backend. The proxy rewrites `Set-Cookie` so the cookie applies to `localhost:4200`. Backend session cookie is set with `Path=/` and `SameSite=Lax`. If you’re not using Docker, ensure the Angular dev server proxy is running so `/api/*` and the login endpoint are proxied to the backend.

---

**Summary:** Use `http://localhost:4200` only, clear cookies for that origin, log in once, then open `/api/v1/auth/session-info` to confirm the backend sees you as admin. If `session-info` shows `isAdmin: true` but delete still returns 403, say so and we can debug the admin check or the delete request next.
