# Cyph API (v1)

All REST endpoints are versioned under **`/api/v1`**. Use a session cookie for authenticated requests; send credentials with every request (`withCredentials: true`).

**Base URL (relative):** `/api/v1`

---

## Conventions

- **Auth:** Session-based (cookie). Public endpoints are listed below; all others require an authenticated session.
- **Mutations:** Use **POST** with a JSON body so the session cookie is sent reliably.
- **Errors:** 4xx/5xx responses include a JSON body when applicable: `{ "message": "..." }`.
- **Content-Type:** Request: `application/json` where a body is required. Response: `application/json`.

---

## Public (no auth)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/public/auth-methods` | Which login methods are available (SSO, Google, form login). |

**Response:** `{ "oauth2RegistrationIds": string[], "formLogin": boolean }`

---

## Auth

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/login` | Form login (username + password). Creates session. |
| GET | `/api/v1/auth/session-info` | Current session: principal and isAdmin (for debugging). |

**POST /api/v1/auth/login**  
Body: `{ "username": string, "password": string, "redirectUrl"?: string }`  
Success: `200` `{ "redirectUrl": string }`  
Error: `401` `{ "message": string }`

**GET /api/v1/auth/session-info**  
Success: `200` `{ "authenticated": true, "principal": string, "isAdmin": boolean }`  
Not logged in: `401` `{ "authenticated": false, "message": string }`

---

## Me (authenticated)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/me` | Current user email, name, and admin flag. |

**Response:** `{ "email": string, "name": string, "admin": boolean }`  
Unauthenticated: `401`

---

## Recipients (authenticated)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/recipients` | List users that can be selected as message recipients (e.g. for Send page dropdown). |

**Response:** Array of `{ "email": string, "source": string, "displayName"?: string }`  
Unauthenticated: `401` `{ "message": string }`

---

## Send message (authenticated)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/send` | Send a secret message to a recipient. |

**Body:** `{ "recipientEmail": string, "message": string }`  
**Response:** `200` `{ "accessToken": string }` (use in view link)  
Errors: `401` `{ "message": string }`, `400` validation

---

## View message (authenticated)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/view` | Retrieve and decrypt a secret message (one-time read). |

**Body:** `{ "accessToken": string }`  
**Success:** `200` `{ "message": string }`  
**Locked (cross-group):** `403` `{ "locked": true, "message": string }`  
**Not found / expired:** `404` `{ "message": string }`  
**Unauthenticated:** `401` `{ "message": string }`

---

## Admin – Users

All admin endpoints require an authenticated user with admin role. Otherwise: `401` or `403` `{ "message": string }`.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/admin/users` | List all users (admin only). |
| POST | `/api/v1/admin/users` | Add a user (admin only). |
| POST | `/api/v1/admin/users/remove` | Remove a user (admin only). |
| POST | `/api/v1/admin/users/set-admin` | Set a user’s admin flag (admin only). |

**GET /api/v1/admin/users**  
Response: Array of user objects (email, displayName, source, externalId, admin, createdAt, lastLoginAt, addedBy).

**POST /api/v1/admin/users**  
Body: `{ "email": string, "username"?: string, "password"?: string, "group"?: string }`  
Success: `200` `{ "email": string, "source": string }` or `400` `{ "message": string }` (e.g. "User already exists", "Group not found").

**POST /api/v1/admin/users/remove**  
Body: `{ "email": string }`  
Success: `204`  
Errors: `400` `{ "message": string }` (e.g. "Cannot remove yourself", "Cannot remove the super-admin user", "Cannot remove an admin user"), `404` `{ "message": "User not found" }`.

**POST /api/v1/admin/users/set-admin**  
Body: `{ "email": string, "admin": boolean }`  
Success: `200` `{ "email": string, "admin": boolean }`  
Errors: `400` `{ "message": string }` (e.g. "Cannot demote the super-admin user").

---

## Admin – Groups

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/admin/groups` | List groups (admin only). |
| POST | `/api/v1/admin/groups` | Create a group (admin only). |

**POST /api/v1/admin/groups**  
Body: `{ "name": string }`  
Success: `201` `{ "id": number, "name": string }`  
Error: `400` `{ "message": string }`

---

## Admin – Group permissions

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/admin/group-permissions` | List group send permissions (admin only). |
| POST | `/api/v1/admin/group-permissions` | Add a permission: fromGroup → toGroup (admin only). |
| POST | `/api/v1/admin/group-permissions/remove` | Remove a group permission (admin only). |

**POST /api/v1/admin/group-permissions**  
Body: `{ "fromGroupName": string, "toGroupName": string }`  
Success: `201` `{ "fromGroupId", "fromGroupName", "toGroupId", "toGroupName" }`  
Error: `400` `{ "message": string }`

**POST /api/v1/admin/group-permissions/remove**  
Body: `{ "fromGroupId": number, "toGroupId": number }`  
Success: `204`

---

## Admin – Audit log

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/admin/audit-log` | Paginated audit log (admin only). |

**Query:** `?page=0&size=50` (Spring Pageable)  
**Response:** `{ "content": AuditLogEntry[], "totalElements": number, "totalPages": number, "number": number }`

**Audit log events:** All server-side actions are logged: `LOGIN`, `MESSAGE_SENT_SAME_GROUP`, `MESSAGE_SENT_CROSS_GROUP`, `MESSAGE_VIEWED`, `MESSAGE_DELETED`, `USER_CREATED`, `USER_DELETED`, `USER_ADMIN_CHANGED`, `GROUP_CREATED`, `GROUP_PERMISSION_ADDED`, `GROUP_PERMISSION_REMOVED`. Each entry may include `actorIdentifier`, `targetIdentifier`, and `details`.

---

## Error handling summary

| Status | When | Body |
|--------|------|------|
| 400 | Validation / business rule (e.g. "Email is required", "Cannot remove super-admin") | `{ "message": string }` |
| 401 | Not authenticated | `{ "message": string }` |
| 403 | Authenticated but not allowed (e.g. admin required, or message locked) | `{ "message": string }` or `{ "locked": true, "message": string }` |
| 404 | Resource not found (e.g. user, message) | `{ "message": string }` |

Clients should read `message` from the response body for user-facing error text.
