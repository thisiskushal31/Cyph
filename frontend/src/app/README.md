# Cyph Frontend (Angular)

This folder contains the Angular application for Cyph: send secret messages, sign in (admin / SSO / Google), and admin panel.

## Folder structure

- **`app.component.ts`** – Root component: header (nav: Send, Admin, Log, Sign in/out) and main outlet for routes.
- **`app.routes.ts`** – Route definitions. `/login` is public; `/send`, `/view/:token`, `/admin`, `/log` are protected by guards.
- **`core/`** – Shared logic used across the app.
  - **`guards/`** – `authGuard` (requires session), `adminGuard` (requires admin role from `/api/me`).
  - **`services/`** – `ApiService`: all HTTP calls to the backend (`/api/...`), with credentials for cookies.
- **`pages/`** – One folder per main screen.
  - **`login/`** – Sign-in page with three options: username/password, SSO, Google. Template and styles in separate files.
  - **`send/`** – Send a secret message: recipient email, message text, submit.
  - **`view/`** – View a secret by token (from link); shows message or “locked” if cross-group.
  - **`admin/`** – Admin-only: list/add/remove users, toggle admin; SSO/Google config is in backend.
  - **`log/`** – Admin-only: audit log (login, send, view events; no message content).

## Auth flow

1. User opens app → if not logged in, guard redirects to `/login`.
2. Login page shows three options; user picks one. Form login POSTs to `/login`; SSO/Google use `/oauth2/authorization/{id}` (proxied to backend).
3. After login, backend sets session cookie; user is redirected to `/send` (or requested redirect).
4. Nav shows Send (+ Admin, Log if admin). Sign out goes to `/logout`.

## Build and serve

- `npm start` – Dev server with proxy to backend (see `src/proxy.conf.json`: `/api`, `/login`, `/logout`, `/oauth2` → backend).
- `npm run build` – Production build in `dist/`.

All API calls use relative URLs and `withCredentials: true` so the session cookie is sent.
