/**
 * Dev server proxy. Uses CYPH_API_TARGET in Docker (e.g. http://backend:8080), else localhost:8080.
 * onProxyRes forwards Set-Cookie and rewrites cookie so the browser accepts it when served via proxy.
 */
const target = process.env.CYPH_API_TARGET || 'http://localhost:8080';

function forwardSetCookie(proxyRes) {
  const setCookie = proxyRes.headers['set-cookie'];
  if (!setCookie || !Array.isArray(setCookie)) return;
  // Remove Domain so cookie applies to current host (localhost:4200); ensure SameSite for cross-origin safety
  proxyRes.headers['set-cookie'] = setCookie.map((c) => {
    let s = c.replace(/;\s*Domain=[^;]+/gi, '').trim();
    if (!/;\s*SameSite=/i.test(s)) s += '; SameSite=Lax';
    return s;
  });
}

const proxyOptions = (path) => ({
  target,
  secure: false,
  changeOrigin: true,
  onProxyRes: forwardSetCookie,
});

module.exports = {
  '/api': proxyOptions('/api'),
  '/oauth2': proxyOptions('/oauth2'),
  '/login': proxyOptions('/login'),
  '/logout': proxyOptions('/logout'),
};
