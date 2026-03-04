const STORAGE_KEYS = { baseUrl: 'cyphBaseUrl' };

document.getElementById('cyph-url').addEventListener('input', () => {
  document.getElementById('save-status').textContent = '';
});

chrome.storage.local.get(STORAGE_KEYS.baseUrl).then(({ baseUrl }) => {
  if (baseUrl) document.getElementById('cyph-url').value = baseUrl;
});

document.getElementById('save-btn').addEventListener('click', async () => {
  const url = document.getElementById('cyph-url').value.trim().replace(/\/$/, '');
  const status = document.getElementById('save-status');
  if (!url) {
    status.textContent = 'Enter a Cyph URL.';
    return;
  }
  await chrome.storage.local.set({ [STORAGE_KEYS.baseUrl]: url });
  status.textContent = 'Saved.';
  setTimeout(() => { status.textContent = ''; }, 2000);
});
