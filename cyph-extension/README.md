# Cyph Chrome Extension

Full credential manager client for Cyph: **shared** (admin-pushed) and **personal** (user-created) credentials, all stored in Cyph and fetched from here.

## Two kinds of credentials

- **Shared** – Admins push credentials to users or groups (e.g. “Grafana – Engineering” for `monitor.example.com`). You see them in the extension and can reveal/copy.
- **Personal** – You add your own (e.g. “My Grafana” for the same site). You create/edit/delete them in the extension (or web app); they are stored in Cyph and stay in sync.

For a site like `monitor.example.com` you might see both the shared team login and your personal login; you pick which one to use. All data is centralized in Cyph.

## How it works

1. **Admin** deploys Cyph and pushes **shared** credentials to users/groups (web app; backend API in progress).
2. **User** installs this extension, enters **Cyph URL** and **username** / **password** (their Cyph login).
3. Extension calls `POST /api/v1/auth/extension-login` and receives a token.
4. User sees a **unified list** (shared + personal) from `GET /api/v1/extension/credentials` (each item has a `source`: shared or personal).
5. **Reveal** copies the secret to the clipboard (`POST /api/v1/extension/credentials/:id/reveal`).
6. **Add personal** – User can add a new credential (label, URL, username, secret); `POST /api/v1/extension/credentials`; list refreshes and syncs to Cyph.
7. **Edit/delete personal** – Only for `source: "personal"`; PUT/DELETE to sync back.

See **docs/PRODUCT.md** in the main repo for the full product vision and API contract.

## Load in Chrome (development)

1. Open `chrome://extensions/`.
2. Enable **Developer mode**.
3. Click **Load unpacked** and select this folder (`cyph-extension`).
4. Optional: add icons (16, 48, 128 px PNG) as `icons/icon16.png`, `icons/icon48.png`, `icons/icon128.png` and add them to `manifest.json` under `action.default_icon` and `icons`.

## Backend requirements

The Cyph backend must expose (see **docs/PRODUCT.md**):

- **Auth:** `POST /api/v1/auth/extension-login` – body `{ username, password }`, response `{ accessToken }`.
- **Unified list:** `GET /api/v1/extension/credentials` – response array of `{ id, label, username?, url?, source: "shared" | "personal" }`.
- **Reveal:** `POST /api/v1/extension/credentials/:id/reveal` – response `{ secret }`.
- **Personal CRUD:** `POST /api/v1/extension/credentials`, `PUT /api/v1/extension/credentials/:id`, `DELETE /api/v1/extension/credentials/:id` (user must own the credential).

CORS must allow the extension origin (or use a proxy). Token should be short-lived or revocable.

## License

Same as the Cyph project (open source).
