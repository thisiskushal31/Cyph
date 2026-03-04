# Cyph Backend (Spring Boot)

Java 17, Spring Boot 3.x. REST API for secret messages, auth (form login, OAuth2 SSO, Google), admin user management, and audit logging.

---

## [**→ API documentation (v1)**](docs/API.md)

All endpoints are versioned under `/api/v1`. The API doc lists every endpoint, request/response shapes, and error handling. Use it when integrating with the backend or debugging.

---

## Package layout (`com.cyph`)

- **`api/`** – REST controllers under **`/api/v1`**. `SecretMessageController` (send/view), `AdminController` (users, groups, permissions, audit log), `MeController` (/api/v1/me), `AuthLoginController` (/api/v1/auth/login), `AuthMethodsController` (/api/v1/public/auth-methods), `LoginController` (GET /login redirect), `RootController` (GET / redirect to frontend).
- **`config/`** – `CyphProperties` (all `cyph.*` config), `WebConfig`, `CyphConfig`, `SchedulingConfig`.
- **`domain/`** – JPA entities: `SecretMessage`, `AllowedUser`, `Group`, `AuditLog`.
- **`repository/`** – Spring Data JPA repositories.
- **`service/`** – Business logic: `SecretMessageService`, `AllowedUserService`, `AuditService`, `EncryptionService` (AES-256-GCM), `MailSenderService`, etc.
- **`security/`** – `SecurityConfig` (form + OAuth2, permit /api/v1/public, /api/v1/auth/login, /login, /oauth2), `FormLoginUserDetailsService`, `FormLoginSuccessHandler`, `CyphOAuth2LoginSuccessHandler`.
- **`scheduler/`** – Expired message cleanup job (cron).

## Auth

- **Form login**: Admin username/password from `cyph.auth.form-login`; POST /login, session cookie.
- **OAuth2**: SSO and/or Google via `spring.security.oauth2.client.registration`; buttons on login page when configured and listed in `cyph.auth.oauth2-registration-ids`.
- **Session**: Cookie-based; frontend uses same origin and sends credentials.

## Run

**Local dev (recommended):** From repo root run `docker compose up`; backend runs in a container with Postgres and live code reload.

**Without Docker:** `cd backend && ./gradlew bootRun`. Uses `src/main/resources/application.yml`. Default: H2 in-memory, form login (admin@localhost / admin). Add OAuth2 in `application.yml` for SSO/Google (see main [README](../README.md)).

---

## Keycloak (local SSO)

When you run `docker compose up`, **Keycloak** is started for local SSO testing (port **8180**). The backend is configured via env to use it as the OAuth2 provider (registration id: `keycloak`). The SSO provider can be **any** IdP—Keycloak, Google, or another OIDC/OAuth2 provider; you configure one (or more) in `oauth2-registration-ids`.

- **Keycloak admin**: http://localhost:8180 — login with `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` (see `docker-compose.yml`; default `admin` / `admin`).
- **Realm**: `cyph`, imported from `config/keycloak/cyph-realm.json`.
- **Test user**: `sso-user` / `sso-user` (create more users in Keycloak if needed).
- **Frontend**: At http://localhost:4200 you’ll see “Sign in with SSO”; it uses Keycloak when that’s the only OAuth2 provider.
- **Allowed list**: If `cyph.auth.require-allowed-user-list` is true, add the SSO user’s email (e.g. `sso-user@...` or whatever you set in Keycloak) in Admin → Users so they can sign in.
- **Linux**: If the browser can’t reach `host.docker.internal`, add `host.docker.internal` → `127.0.0.1` to `/etc/hosts` so the OAuth redirect works.

---

## Email (SMTP)

Notification emails (“you have a secret message” with the view link) are sent by **MailSenderService** using Spring’s **JavaMailSender** (SMTP). They are sent only when **SMTP is configured**: set `cyph.mail.host` (and optionally port, username, password, from) in config or env.

- **Local dev without SMTP**: If `cyph.mail.host` is not set, the service does **not** send email. Messages are still stored and view links are generated; you can copy the link from logs or use the API. This is typical for local runs.
- **Production**: Configure SMTP (e.g. your org’s relay or a provider like SendGrid) and `cyph.site-url` so links in emails point to the correct frontend.
