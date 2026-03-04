# Cyph

**Cyph** is a self-hosted, organization-agnostic **secret message sharing** tool. Users send encrypted messages that only the intended recipient can view once, with optional expiration. Designed for easy deployment and configuration in any organization.

---

## Features

- **End-to-end style encryption**: Each message is encrypted at rest (AES-256-GCM). Only the recipient can decrypt after authenticating.
- **Flexible authentication** (both can be enabled):
  1. **Single Sign-On (SSO)** — OAuth 2.0 / OpenID Connect. When SSO is configured, it is the **first priority** for sign-in.
  2. **Admin-managed users** — Admins add allowed emails; those users sign in with **Google OAuth** (or another configured provider). Works alongside SSO.
- **Recipient-only access**: Messages are tied to recipient email; only that user can open the link.
- **Configurable expiration**: Messages can expire after a set time (e.g. 24 hours); expired messages are cleaned up.
- **Config-driven**: No code changes for a new org. One config file (or env) drives SSO, allowed domains, SMTP, site URL, and feature flags.
- **SSO groups**: When users sign in via SSO, their **groups** are read from the IdP token (e.g. `groups` or `realm_access.roles`) and stored. **Same-group** users can send and read messages normally. **Cross-group** messages can be sent but are **locked**: the recipient sees "This message is locked" and cannot read the content (for auditing/compliance).
- **Audit log (no PII)**: Login, message sent (same-group vs cross-group), message viewed, and message deleted are logged. Logs contain only event type, timestamp, message id (UUID), and group names—**no emails, names, or message content**. Admins can view the log at **Log** in the app.

---

## Architecture

| Layer        | Technology                          |
|-------------|-------------------------------------|
| Frontend    | Angular, Tailwind CSS               |
| Backend     | Java (Spring Boot 3.x)               |
| Auth        | Spring Security, OAuth2/OIDC (SSO) + optional Google for manual users |
| Crypto      | AES-256-GCM (per-message key/nonce)  |
| Persistence | Configurable (e.g. PostgreSQL, MySQL, H2 for trials) |
| Deployment  | Docker, Docker Compose, Kubernetes, or plain VM |

---

## Quick Start

### Prerequisites

- **Docker** and **Docker Compose** (for local dev and testing)
- For production: container images from `deploy/docker/` or Kubernetes/VM (see [Deployment options](#deployment-options))

### 1. Configuration

Optional: copy the example config and adjust for your organization. Defaults in `application.yml` work for local Docker (form login `admin@localhost` / `admin`, Postgres in container).

```bash
cp backend/src/main/resources/application.example.yml backend/src/main/resources/application.yml
# Edit application.yml: SSO, allowed users, SMTP, site URL, etc.
```

Or use environment variables; see [Configuration](#configuration) below.

### 2. Run locally (one command)

From the repo root, start the app with a **persistent Postgres** and live code updates (edit code and see changes without rebuilding):

```bash
docker compose up
```

- **App**: http://localhost:4200  
- **Backend API**: http://localhost:8080  
- **Database**: Postgres in Docker (data in volume `pgdata`). Login: `admin@localhost` / `admin`.

Code is mounted into the containers: backend recompiles on change (Gradle continuous build), frontend hot-reloads. Single terminal; stop with `Ctrl+C`.

### 3. Production (containerized or VM)

- **Containerized**: Build images with `deploy/docker/Dockerfile.backend` and `Dockerfile.frontend`; run with a compose file that uses Postgres and mounts config (see `deploy/cyph-config.example.yml`).
- **Kubernetes**: `kubectl apply -f deploy/kubernetes/` (adjust ConfigMap/Secrets).
- **VM**: Run the backend JAR and a reverse proxy in front of the built frontend; same config or env.

---

## Configuration

All organization-specific behavior is driven by configuration (file or environment). No code changes required.

| Key area | What you configure |
|----------|--------------------|
| **SSO** | OIDC issuer URI, client id/secret, scopes. When set, SSO is used first for sign-in. |
| **Manual users** | Admin panel: add/remove allowed emails. Those users sign in via Google (or configured social provider). |
| **Allowed domains** | Optional email domain restriction (e.g. `@yourcompany.com`). |
| **SSO groups claim** | JWT claim for group names (e.g. `groups`). Synced on login for same-group / cross-group messaging. |
| **SMTP** | Host, port, user, password, from-address for “you have a secret message” emails. |
| **Site URL** | Public base URL used in email links (e.g. `https://cyph.yourcompany.com`). |
| **Message TTL** | Default message lifetime (e.g. 24 hours). |
| **Database** | JDBC URL, username, password (or use embedded H2 for quick trials). |

Example structure (YAML):

```yaml
# application.yml (or application-{profile}.yml)
cyph:
  site-url: https://cyph.yourcompany.com
  message:
    default-ttl-hours: 24
  auth:
    sso:
      enabled: true
      issuer-uri: https://idp.yourcompany.com
      client-id: ${OIDC_CLIENT_ID}
      client-secret: ${OIDC_CLIENT_SECRET}
    allowed-domains: ["yourcompany.com"]  # optional
  mail:
    host: ${SMTP_HOST}
    port: ${SMTP_PORT}
    username: ${SMTP_USER}
    password: ${SMTP_PASSWORD}
    from: noreply@yourcompany.com
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
```

Secrets (client secrets, DB password, SMTP password) should be injected via env or a secret manager, not committed.

---

## Authentication behavior

- If **SSO is enabled**: Login page uses your OIDC provider first. Users sign in with org SSO.
- If **admin-managed users** are enabled: Admins add allowed emails. Those users can sign in via Google (or configured provider). They do not need to be in SSO.
- If **both** are enabled: SSO is offered first; users not in SSO can still use “Sign in with Google” (or similar) if their email is in the allowed list. So: **SSO first, then manual list**. All users (SSO or admin-added) appear in **Admin → Users** with email, source (SSO vs ADMIN_ADDED), external user ID, and last login; admins can add/remove users and toggle admin role.

---

## Local testing (SSO + Admin)

1. **Backend** `application.yml`: set `cyph.auth.admin-emails: [your@email.com]` so you can open the Admin panel after sign-in. For local, leave `require-allowed-user-list: false` so any Google sign-in is allowed (or use allowed-domains).
2. **Google OAuth (local)**  
   - Create a project in Google Cloud Console, add OAuth 2.0 credentials (Web application), set redirect URI to `http://localhost:8080/login/oauth2/code/google`.  
   - In `application.yml` add under `spring.security.oauth2.client.registration`: a `google` entry with `client-id` and `client-secret`.  
   - Start backend and frontend; open `http://localhost:4200`, sign in with Google. You'll be created in the allowed_user table. Your email in `admin-emails` gets Admin access.
3. **SSO (e.g. Keycloak)**  
   - **With Docker**: `docker compose up` starts Keycloak on port **8180**. The backend is preconfigured (registration id: `keycloak`). Open http://localhost:4200 and use “Sign in with SSO”; test user `sso-user` / `sso-user`. See [backend README → Keycloak (local SSO)](backend/README.md#keycloak-local-sso).  
   - **Without Docker**: Run Keycloak (or another OIDC IdP) locally; create a realm and client, set redirect URI to `http://localhost:8080/login/oauth2/code/sso`. Set `cyph.auth.sso.enabled: true` and register the client with `registrationId: sso` and `issuer-uri`. First SSO login creates the user in DB; add your email to `admin-emails` to access Admin.

## Redundancy: message cleanup and job failure

- **Two cleanup paths**: (1) When a recipient **views** a message (or an expired one), the backend deletes it (best-effort: if delete fails, the response is still returned and the scheduled job will remove the row later). (2) A **scheduled job** runs every 15 minutes (configurable via `cyph.cleanup.cron`) and deletes all expired messages.
- **If a delete fails**: The scheduled job and the view-path both perform deletes. If one delete fails (e.g. DB blip), the other path or the next run will clean up; deletes are **idempotent**.
- **If the scheduled job fails**: The job uses **Spring Retry** (3 attempts with backoff). If all retries fail, the exception is logged and the next **cron run** (e.g. 15 minutes later) will run again. So a failing job does not need a "scheduler for the scheduler"—the next run is the redundancy.
- **Config**: `cyph.cleanup.cron` (default `0 */15 * * * *`).

## Extension: organization-managed password manager

Cyph can act as an **organization-managed password manager** via a Chrome extension:

- **Admins** push credentials (e.g. service passwords, API keys) to users from the Cyph web app.
- **Users** install the [Cyph Chrome extension](cyph-extension/), enter their Cyph URL and login (username/password), and then see and reveal only the credentials pushed to them.

The extension is in **cyph-extension/**; the backend APIs for extension auth and pushed credentials are specified in **[docs/PRODUCT.md](docs/PRODUCT.md)**. That doc also covers deployment and a **SOC 2 / ISO 27001** compatibility roadmap so any organization can deploy Cyph and aim for compliance.

---

## Project layout

```
Cyph/
├── README.md                 # This file
├── docker-compose.yml        # Local dev: docker compose up (DB + backend + frontend, live code)
├── docs/
│   ├── PRODUCT.md            # Product vision, extension API, SOC 2 / ISO 27001
│   └── DEPLOYMENT.md         # Production deployment
├── cyph-extension/           # Chrome extension (org-managed password manager client)
├── backend/                  # Java Spring Boot API
│   ├── Dockerfile.dev        # Dev image (mount source, Gradle continuous build)
│   ├── src/main/java/...     # Packages: config, security, message, api, persistence
│   ├── src/main/resources/
│   │   ├── application.example.yml
│   │   └── application.yml   # Your config (git-ignored)
│   └── build.gradle.kts
├── frontend/                 # Angular + Tailwind
│   ├── Dockerfile.dev        # Dev image (mount source, proxy to backend)
│   ├── proxy.conf.docker.json
│   ├── src/app/
│   ├── tailwind.config.js
│   └── package.json
├── config/
│   └── application.example.yml  # Copy into backend or mount in containers
└── deploy/
    ├── docker/
    │   ├── Dockerfile.backend
    │   ├── Dockerfile.frontend
    │   └── nginx.conf
    ├── cyph-config.example.yml   # Copy to cyph-config.yml for Docker
    └── kubernetes/           # Optional K8s manifests
```

---

## Deployment options

- **Docker**: Local dev uses `docker compose up` (Postgres + backend + frontend with mounted code). For production, use images from `deploy/docker/` with your config.
- **Kubernetes**: Use provided manifests (or Helm chart if added) and set config via ConfigMap/Secret.
- **VM**: Run the JAR and a reverse proxy (e.g. nginx) in front of Angular build + API; same config file or env.

In all cases, point the app at the same config (file or env) so any organization can “plug and play” without forking the code.

---

## Development

- **Backend**: Clean structure (config, security, domain, application service, API). Crypto and message lifecycle in dedicated services; auth behind interfaces so SSO vs manual users are pluggable.
- **Frontend**: Angular with Tailwind; components for login, send secret, view secret, and (if enabled) admin panel. API client and auth flow aligned with backend.
- **Config**: All org-specific and environment-specific values live in config; no hardcoded domains or credentials.

---

## License

See repository license file.
