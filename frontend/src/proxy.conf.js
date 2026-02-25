/**
 * Dev server proxy. Uses CYPH_API_TARGET in Docker (e.g. http://backend:8080), else localhost:8080.
 * onProxyRes forwards Set-Cookie so the browser accepts it when served via proxy.
 * Angular 18 expects an array with context for path matching.
 */
const target = process.env.CYPH_API_TARGET || 'http://localhost:8080';

function forwardSetCookie(proxyRes) {
  const setCookie = proxyRes.headers['set-cookie'];
  if (!setCookie || !Array.isArray(setCookie)) return;
  proxyRes.headers['set-cookie'] = setCookie.map((c) => {
    let s = c.replace(/;\s*Domain=[^;]+/gi, '').trim();
    if (!/;\s*SameSite=/i.test(s)) s += '; SameSite=Lax';
    return s;
  });
}

const proxyEntry = (context, options = {}) => ({
  context,
  target,
  secure: false,
  changeOrigin: true,
  onProxyRes: forwardSetCookie,
  ...options,
});

/**
 * GET /login and GET /login?... must not be proxied: the browser requests the page on reload,
 * and the dev server should serve index.html so the Angular SPA loads and the router shows the login view.
 * Only POST /login (form submit) and OAuth callbacks should go to the backend.
 */
function loginBypass(req, res, proxyOptions) {
  if (req.method === 'GET' && (req.url === '/login' || req.url.startsWith('/login?'))) {
    return '/index.html';
  }
}

module.exports = [
  proxyEntry(['/api']),
  proxyEntry(['/oauth2']),
  proxyEntry(['/login'], { bypass: loginBypass }),
  proxyEntry(['/logout']),
];
