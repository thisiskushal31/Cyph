/**
 * Cyph extension popup: login with Cyph URL + username/password, then list/reveal credentials.
 * Backend must expose: POST /api/v1/auth/extension-login, GET /api/v1/extension/credentials,
 * POST /api/v1/extension/credentials/:id/reveal (see docs/PRODUCT.md).
 */

const STORAGE_KEYS = { baseUrl: 'cyphBaseUrl', token: 'cyphToken' };

function $(id) {
  return document.getElementById(id);
}

function showPanel(panelId) {
  document.querySelectorAll('.panel').forEach((p) => p.classList.add('hidden'));
  const panel = $(panelId);
  if (panel) panel.classList.remove('hidden');
}

function setError(el, message) {
  if (!el) return;
  el.textContent = message || '';
  el.classList.toggle('hidden', !message);
}

async function getBaseUrl() {
  const { baseUrl } = await chrome.storage.local.get(STORAGE_KEYS.baseUrl);
  return baseUrl && baseUrl.replace(/\/$/, '');
}

async function getStoredToken() {
  const { token } = await chrome.storage.session.get(STORAGE_KEYS.token);
  return token;
}

async function setToken(token) {
  if (token) {
    await chrome.storage.session.set({ [STORAGE_KEYS.token]: token });
  } else {
    await chrome.storage.session.remove(STORAGE_KEYS.token);
  }
}

async function login(baseUrl, username, password) {
  const res = await fetch(`${baseUrl}/api/v1/auth/extension-login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.message || `Login failed (${res.status})`);
  }
  const data = await res.json();
  return data.accessToken;
}

async function fetchCredentials(baseUrl, token) {
  const res = await fetch(`${baseUrl}/api/v1/extension/credentials`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) {
    if (res.status === 401) return null;
    throw new Error(`Failed to load credentials (${res.status})`);
  }
  return res.json();
}

async function revealCredential(baseUrl, token, credentialId) {
  const res = await fetch(`${baseUrl}/api/v1/extension/credentials/${credentialId}/reveal`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.message || `Failed to reveal (${res.status})`);
  }
  const data = await res.json();
  return data.secret;
}

function renderCredentialsList(items) {
  const list = $('credentials-list');
  const noCreds = $('no-credentials');
  const errEl = $('credentials-error');
  setError(errEl, '');
  list.innerHTML = '';
  if (!items || items.length === 0) {
    noCreds.classList.remove('hidden');
    return;
  }
  noCreds.classList.add('hidden');
  items.forEach((item) => {
    const div = document.createElement('div');
    div.className = 'credential-item';
    div.innerHTML = `
      <div>
        <div class="label">${escapeHtml(item.label || 'Unnamed')}</div>
        ${item.username ? `<div class="meta">${escapeHtml(item.username)}</div>` : ''}
        ${item.url ? `<div class="meta">${escapeHtml(item.url)}</div>` : ''}
      </div>
      <button type="button" class="reveal-btn" data-id="${escapeHtml(item.id)}">Reveal</button>
    `;
    div.querySelector('.reveal-btn').addEventListener('click', () => handleReveal(item.id));
    list.appendChild(div);
  });
}

function escapeHtml(s) {
  const el = document.createElement('span');
  el.textContent = s;
  return el.innerHTML;
}

async function handleReveal(credentialId) {
  const baseUrl = await getBaseUrl();
  const token = await getStoredToken();
  if (!baseUrl || !token) {
    showPanel('login-section');
    return;
  }
  try {
    const secret = await revealCredential(baseUrl, token, credentialId);
    await navigator.clipboard.writeText(secret);
    const btn = document.querySelector(`[data-id="${credentialId}"]`);
    if (btn) {
      const orig = btn.textContent;
      btn.textContent = 'Copied!';
      setTimeout(() => { btn.textContent = orig; }, 1500);
    }
  } catch (e) {
    setError($('credentials-error'), e.message || 'Failed to reveal');
  }
}

async function loadCredentials() {
  const baseUrl = await getBaseUrl();
  const token = await getStoredToken();
  if (!baseUrl || !token) {
    showPanel('login-section');
    if (baseUrl) $('cyph-url').value = baseUrl;
    return;
  }
  $('cyph-url').value = baseUrl;
  showPanel('credentials-section');
  setError($('credentials-error'), '');
  try {
    const items = await fetchCredentials(baseUrl, token);
    if (items === null) {
      await setToken(null);
      showPanel('login-section');
      return;
    }
    renderCredentialsList(Array.isArray(items) ? items : (items.items || []));
  } catch (e) {
    renderCredentialsList([]);
    setError($('credentials-error'), e.message || 'Could not load credentials');
  }
}

$('login-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const baseUrl = ($('cyph-url').value || '').replace(/\/$/, '');
  const username = $('username').value.trim();
  const password = $('password').value;
  if (!baseUrl || !username || !password) return;
  setError($('login-error'), '');
  $('login-btn').disabled = true;
  try {
    await chrome.storage.local.set({ [STORAGE_KEYS.baseUrl]: baseUrl });
    const token = await login(baseUrl, username, password);
    await setToken(token);
    $('password').value = '';
    await loadCredentials();
  } catch (err) {
    setError($('login-error'), err.message || 'Sign in failed');
  } finally {
    $('login-btn').disabled = false;
  }
});

$('logout-btn').addEventListener('click', async () => {
  await setToken(null);
  showPanel('login-section');
  setError($('login-error'), '');
  setError($('credentials-error'), '');
});

document.addEventListener('DOMContentLoaded', () => {
  loadCredentials();
});
