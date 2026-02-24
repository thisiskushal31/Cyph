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

const proxyEntry = (context) => ({
  context,
  target,
  secure: false,
  changeOrigin: true,
  onProxyRes: forwardSetCookie,
});

module.exports = [
  proxyEntry(['/api']),
  proxyEntry(['/oauth2']),
  proxyEntry(['/login']),
  proxyEntry(['/logout']),
];
