# Cyph – Production Deployment

Deploy Cyph so any organization can run it at their own URL. Use this with **PRODUCT.md** for the full product and extension story.

## Prerequisites

- Docker and Docker Compose (or Kubernetes)
- Domain and TLS (e.g. Let’s Encrypt via Traefik/Caddy/Nginx)
- PostgreSQL (or use the compose DB)
- Secret manager or env for secrets (no defaults in prod)

## Production Checklist

1. **Secrets from env / secret manager**
   - `ADMIN_USERNAME`, `ADMIN_PASSWORD` (super-admin)
   - `POSTGRES_PASSWORD` / DB URL
   - OAuth client secrets (if using SSO/Google)
   - `CYPH_EXTENSION_JWT_SECRET` – secret for signing extension JWTs (min 32 bytes for HS256). Use a strong random value in production.
   - Any other key material for encrypting pushed credentials (handled by app encryption)

2. **URLs**
   - `CYPH_SITE_URL` = public URL of the frontend (e.g. `https://cyph.company.com`)
   - Backend and frontend must agree on this (cookies, CORS, redirects).
   - **Extension:** Users configure the extension with this same URL. The extension calls `https://cyph.company.com/api/v1/auth/extension-login` and `https://cyph.company.com/api/v1/extension/credentials` (and related endpoints). Ensure the backend is reachable at the same origin (or CORS allows the extension if applicable).

3. **TLS**
   - Terminate HTTPS at reverse proxy; backend can be HTTP behind proxy.

4. **Session**
   - `SERVER_SERVLET_SESSION_COOKIE_SECURE=true`
   - `SERVER_SERVLET_SESSION_COOKIE_SAME-SITE=Lax` (or Strict)
   - Session timeout and persistence (e.g. DB-backed sessions if scaling to multiple backend instances)

5. **Database**
   - PostgreSQL with backups; encrypted volume or cloud encryption at rest.
   - No `DDL_AUTO=create-drop`; use `update` or migrations.

6. **Extension**
   - Backend CORS: allow the extension origin when using token auth (or run API calls from a backend-for-frontend if you prefer not to expose CORS to the extension).
   - Extension distributed via Chrome Web Store or enterprise policy; org sets Cyph URL to this deployment.

## Example: Compose Behind a Reverse Proxy

- Run `backend`, `frontend`, `db` (and optional Keycloak) with production images (no dev volume mounts).
- Put Nginx/Traefik/Caddy in front: `https://cyph.company.com` → frontend, `/api` → backend.
- Set `CYPH_SITE_URL=https://cyph.company.com` and pass through `Host` and `X-Forwarded-*` headers so redirects and cookies work.

## Docker Compose (Prod)

A production compose would:

- Use `Dockerfile` (not `Dockerfile.dev`) for backend and frontend (built artifacts).
- Remove `volumes` that mount source code.
- Set env from file or secret manager (e.g. `env_file: .env.prod`).
- Expose only the reverse proxy (e.g. 443); backend/frontend not exposed to the internet.
- Optional: separate compose or stack for Keycloak if using SSO.

See `docker-compose.yml` in the repo root for the current dev setup; duplicate and adapt for prod (images, env, no dev mounts, single public entrypoint).

## SOC 2 / ISO 27001

For SOC 2 and ISO 27001, document:

- How secrets and credentials are stored and encrypted (at rest and in transit).
- Who can access what (admin vs user; extension token scope).
- Audit log retention and who can access it.
- Backup, restore, and incident response.

Details are in **PRODUCT.md** (§5).
