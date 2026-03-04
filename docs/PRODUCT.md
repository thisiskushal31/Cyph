# Cyph – Product Vision & Extension

Cyph is an **organization-deployable** security platform: **secret messaging** (send/view one-time messages) and a **full credential manager** (shared + personal). Organizations deploy their own instance; admins push shared credentials; users can also add and sync their own credentials. The Chrome extension is the password-manager client: all data is stored centrally in Cyph and fetched from there.

---

## 1. How It Works

### 1.1 Two Kinds of Credentials

All credentials live in Cyph and are available in the extension (and optionally the web app). There are two sources:

| Type | Who creates it | Who can see it | Example |
|------|----------------|----------------|---------|
| **Shared** | Admin | Users (or groups) the admin assigns | Grafana at `monitor.example.com` – shared “Engineering” login |
| **Personal** | User | Only that user | “My Grafana” at `monitor.example.com` – your own login |

**Same site, multiple entries** – For a single URL (e.g. `monitor.example.com` / Grafana) you can have:

1. **Shared credential** – e.g. “Grafana – Engineering” (admin-pushed, common team username/password). You select it from the extension like any other entry.
2. **Personal credential** – e.g. “My Grafana” (your own username/password). You add it yourself (in the extension or web app); it is stored in Cyph and synced so you can use it everywhere.

So: one deployment is both a **secret messenger** and a **centralized password/credential manager** – shared (admin-managed) and personal (user-managed), all fetched from the same backend.

### 1.2 Deployment (Organization)

- The organization deploys Cyph (backend + frontend + DB) at a URL of their choice (e.g. `https://cyph.company.com`).
- An admin configures that URL and manages users, groups, **shared credentials** (pushed to users/groups), and (via the web app) one-time secret messages.

### 1.3 Chrome Extension Flow

1. **Install** – User installs the Cyph Chrome extension (open source).
2. **Configure** – Admin provides the **Cyph URL**; optionally preconfigures it for the org.
3. **User setup** – In the extension, the user enters Cyph URL and their **username** and **password** (their Cyph login, e.g. the account the admin created for them).
4. **Authenticate** – Extension calls the backend extension-login endpoint; user receives a **token** for API access.
5. **Password manager** – Once authenticated, the user sees **all credentials they can access** (both shared and personal), e.g.:
   - **Shared** – “Grafana – Engineering”, “Test env – shared”, etc. (admin-pushed).
   - **Personal** – “My Grafana”, “My AWS”, etc. (created by the user; stored and synced in Cyph).
   - For a site like `monitor.example.com`, both the shared Grafana password and the user’s own Grafana password can appear; user picks the one they need (shared team login vs personal login).
6. **Reveal / copy** – User can reveal and copy any credential they’re allowed to see; optional future: autofill.
7. **Add personal** – User can add their own credentials (label, URL, username, secret); these are sent to Cyph and stored centrally, then appear in the extension (and stay in sync across devices/sessions).

---

## 2. Backend Additions (API for Extension)

The current API is session-cookie based (browser). The extension needs **token-based** auth and **credential** APIs.

### 2.1 Extension Auth (Token-Based)

- **POST** `/api/v1/auth/extension-login`  
  Body: `{ "username": string, "password": string }`  
  Headers: `X-Cyph-Base-Url` (optional, for multi-tenant or validation).  
  Success: `200` `{ "accessToken": string, "expiresIn": number }`  
  Error: `401` `{ "message": string }`  

- Tokens are **opaque** or JWTs, stored server-side or verified via signature; short-lived + refresh or long-lived with revocation list.  
- All extension endpoints use **Authorization: Bearer &lt;token&gt;**.

### 2.2 Shared Credentials (Admin-Pushed)

- **POST** `/api/v1/admin/credentials` (admin only)  
  Body: `{ "userEmail"?: string, "groupName"?: string, "label": string, "secret": string, "metadata"?: { "username"?: string, "url"?: string } }`  
  Assign to a user and/or group (e.g. “Grafana – Engineering” for group `engineering`).  
  Success: `201` `{ "id": string }`  
  Encrypt `secret` at rest; audit log.

- **GET** `/api/v1/admin/credentials` (admin only) – list all shared credentials (filter by user/group).  
- **DELETE** `/api/v1/admin/credentials/:id` (admin only) – revoke/remove.

### 2.3 Personal Credentials (User-Owned)

User creates/updates/deletes their own credentials; stored in Cyph and synced to the extension (and web).

- **POST** `/api/v1/credentials` (Bearer token, or session)  
  Body: `{ "label": string, "secret": string, "url"?: string, "username"?: string }`  
  Success: `201` `{ "id": string }`  
  Encrypt at rest; audit (e.g. “user X added credential”).

- **GET** `/api/v1/credentials` (Bearer token, or session)  
  Response: list of current user’s personal credentials (metadata only; no secret).

- **PUT** `/api/v1/credentials/:id` (Bearer token, or session)  
  Body: `{ "label"?: string, "secret"?: string, "url"?: string, "username"?: string }`  
  Update; audit.

- **DELETE** `/api/v1/credentials/:id` (Bearer token, or session)  
  Remove; audit.

### 2.4 Extension: Unified List & Reveal (User)

- **GET** `/api/v1/extension/credentials` (Bearer token)  
  Response: list of **all** credentials the user can access (shared + personal), e.g.:  
  `{ "id": string, "label": string, "username"?: string, "url"?: string, "source": "shared" | "personal" }`  
  No secret in list.

- **POST** `/api/v1/extension/credentials/:id/reveal` (Bearer token)  
  Response: `200` `{ "secret": string }` (decrypted once; audit-logged).

- **POST** `/api/v1/extension/credentials` (Bearer token) – create **personal** credential (same body as §2.3).  
- **PUT** `/api/v1/extension/credentials/:id` (Bearer token) – update **personal** credential (user must own it).  
- **DELETE** `/api/v1/extension/credentials/:id` (Bearer token) – delete **personal** credential (user must own it).

So the extension can both **consume** (list/reveal shared and personal) and **update** (add/edit/delete personal); all data stays centralized in Cyph.

- Rate-limit and audit all access; short-lived tokens for extension recommended.

---

## 3. Chrome Extension (High-Level)

- **Manifest V3** (Chrome’s current standard).
- **Storage**: Cyph base URL in `chrome.storage.local`; **never** store the user’s password or plain-text secrets; token in `chrome.storage.session` or in-memory; avoid long-term storage of tokens.
- **Popup UI**:
  - Cyph URL + “Log in” with username + password.
  - After login: **unified list** of credentials (shared + personal) from GET `/extension/credentials`. Each item shows label, optional URL/username, and a **source** badge (e.g. “Shared” vs “Personal”). Same site (e.g. `monitor.example.com`) can show both “Grafana – Engineering” (shared) and “My Grafana” (personal); user chooses which to reveal/copy.
  - **Reveal** – copy secret to clipboard (or future: fill).
  - **Add personal** – form to add a new personal credential (label, URL, username, secret); POST to `/extension/credentials`; list refreshes and stays in sync with Cyph.
  - **Edit/delete personal** – only for items with `source: "personal"`; PUT/DELETE to sync back to backend.
- **CORS**: Backend must allow the extension origin for API calls (or use a proxy).
- **Security**: HTTPS only; no logging of secrets; token handling as above.

---

## 4. Deployment (Production)

- **Docker Compose (prod)** – Use production-grade images: no volume mounts for code; env from secrets; HTTPS at a reverse proxy (e.g. Nginx, Traefik).  
- **Environment** – `CYPH_SITE_URL`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`, DB URL, OAuth client secrets, etc., from a secret manager.  
- **Database** – PostgreSQL; backups; encrypted at rest (cloud or FS encryption).  
- **Session** – Secure, HTTP-only cookies; same-site; CSRF for web app.  
- **Extension** – Distributed via Chrome Web Store (or sideloaded for enterprise); org points users to their Cyph URL.

A separate **DEPLOYMENT.md** can detail exact compose files, env vars, and reverse-proxy config.

---

## 5. SOC 2 & ISO 27001 (Compatibility Roadmap)

Cyph can be designed and operated so that an organization can use it in a **SOC 2** and **ISO 27001** (information security) environment. Compliance is achieved by the **organization** (policies, procedures, evidence), not by the software alone; the product should support controls.

### 5.1 SOC 2 (Trust Services Criteria)

- **Security**  
  - Access control: auth (form + OAuth), admin vs user, extension token auth.  
  - Encryption: TLS in transit; secrets encrypted at rest (e.g. AES-GCM); passwords hashed (e.g. bcrypt).  
  - Audit: existing audit log (who did what, when); extend to credential push/reveal and extension logins.  
  - Change management: versioned releases, release notes.

- **Availability**  
  - Deployment and monitoring (health checks, logging); optional redundancy/backups.

- **Confidentiality**  
  - Secrets and credentials encrypted; minimal retention; secure disposal (e.g. delete credential = secure delete or overwrite).

- **Processing integrity**  
  - Validation on all inputs; idempotency where needed; error handling and logging.

- **Privacy**  
  - If PII is stored: retention, purpose, and access aligned with privacy policy; support for deletion/anonymization.

**Deliverables**: Security and availability design doc; audit log specification; encryption and key management doc; access control matrix; runbook for deployment and secrets.

### 5.2 ISO 27001 (Information Security)

- **A.9 – Access control**  
  - User and admin roles; MFA readiness (future); extension token lifecycle and revocation.

- **A.10 – Cryptography**  
  - Encryption at rest and in transit; key management (e.g. per-tenant or per-secret keys); no plaintext storage of secrets.

- **A.12 – Operations security**  
  - Logging and monitoring; change management; backup and restore; secure development (e.g. dependency checks, secure SDLC).

- **A.14 – System acquisition, development, maintenance**  
  - Secure development lifecycle; testing (e.g. auth, authz, encryption); supply chain (dependencies, container images).

- **A.16 – Information security incident management**  
  - Audit log and alerts; procedure for handling incidents (e.g. credential leak, token compromise).

**Deliverables**: Risk assessment; security policy and procedures; encryption and key management; incident response procedure; and evidence for audits (logs, configs, change records).

### 5.3 ISO 17001

**ISO 17001** does not exist as a standard. You may mean:

- **ISO/IEC 17025** – Quality for testing/calibration labs (not directly about software security).  
- **ISO 27001** – Information security management (most relevant here).

If your target is **laboratory** compliance, specify which standard (e.g. 17025) and we can align documentation accordingly.

---

## 6. Open Source

- Cyph (backend + frontend) and the Chrome extension can be **open source** (license TBD: MIT, Apache 2.0, etc.).  
- Deployment and operation (keys, env, infra) remain the organization’s responsibility; SOC 2/ISO 27001 are achieved by the organization using Cyph as a component within their control environment.

---

## 7. Example: monitor.example.com (Grafana)

- **Shared** – Admin creates “Grafana – Engineering” for URL `https://monitor.example.com`, assigns to group `engineering`. Everyone in that group sees it in the extension and can reveal the shared username/password.
- **Personal** – You add “My Grafana” for the same URL with your own username/password. It’s stored in Cyph and appears in your extension next to the shared one.
- In the extension you see both; you pick “Grafana – Engineering” when using the team account, or “My Grafana” when using your own. All data is fetched from the centralized Cyph backend.

---

## 8. Summary

| Component        | Purpose |
|-----------------|--------|
| **Cyph (web)**  | Deploy by org; secret messaging (send/view); admin: users, groups, **shared credentials**; user: **personal credentials** (add/edit/delete). |
| **Chrome extension** | Log in with Cyph URL + username/password; **unified list** (shared + personal); reveal/copy; **add/edit/delete personal** (synced to Cyph). |
| **Credentials** | **Shared** = admin-pushed, assigned to users/groups; **Personal** = user-created, stored in Cyph, synced to extension. Same site can have both. |
| **Backend** | Extension auth (token); admin CRUD shared credentials; user CRUD personal credentials; extension unified list + reveal + personal CRUD; encryption at rest; audit. |
| **Deployment** | Production Docker, HTTPS, secrets from vault, DB backups. |
| **SOC 2 / ISO 27001** | Design for access control, encryption, audit, availability; org implements policies and evidence. |

Next steps: implement extension auth; shared credentials (admin push, list by user/group); personal credentials (user CRUD); extension APIs (unified list, reveal, personal CRUD); extension UI (list with source badge, add/edit personal, reveal); then deployment and compliance documentation.
