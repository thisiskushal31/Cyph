# Cyph Backend (Spring Boot)

Java 17, Spring Boot 3.x. REST API for secret messages, auth (form login, OAuth2 SSO, Google), admin user management, and audit logging.

## Package layout (`com.cyph`)

- **`api/`** – REST controllers. `SecretMessageController` (send/view), `AdminController` (users, audit log), `MeController` (/api/me), `LoginController` (GET /login redirect), `AuthMethodsController` (GET /api/public/auth-methods), `RootController` (GET / redirect to frontend).
- **`config/`** – `CyphProperties` (all `cyph.*` config), `WebConfig`, `CyphConfig`, `SchedulingConfig`.
- **`domain/`** – JPA entities: `SecretMessage`, `AllowedUser`, `Group`, `AuditLog`.
- **`repository/`** – Spring Data JPA repositories.
- **`service/`** – Business logic: `SecretMessageService`, `AllowedUserService`, `AuditService`, `EncryptionService` (AES-256-GCM), `MailSenderService`, etc.
- **`security/`** – `SecurityConfig` (form + OAuth2, permit /api/public, /login, /oauth2), `FormLoginUserDetailsService`, `FormLoginSuccessHandler`, `CyphOAuth2LoginSuccessHandler`.
- **`scheduler/`** – Expired message cleanup job (cron).

## Auth

- **Form login**: Admin username/password from `cyph.auth.form-login`; POST /login, session cookie.
- **OAuth2**: SSO and/or Google via `spring.security.oauth2.client.registration`; buttons on login page when configured and listed in `cyph.auth.oauth2-registration-ids`.
- **Session**: Cookie-based; frontend uses same origin and sends credentials.

## Run

**Local dev (recommended):** From repo root run `docker compose up`; backend runs in a container with Postgres and live code reload.

**Without Docker:** `cd backend && ./gradlew bootRun`. Uses `src/main/resources/application.yml`. Default: H2 in-memory, form login (admin@localhost / admin). Add OAuth2 in `application.yml` for SSO/Google (see main [README](../README.md)).
