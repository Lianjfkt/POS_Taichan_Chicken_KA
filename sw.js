const CACHE_NAME = 'kapos-v3-cache-v4';
const ASSETS = [
  '/',
  'index.html',
  'manifest.json',
  'css/design-system.css',
  'js/state.js',
  'js/supabase.js',
  'js/printer.js',
  'js/views/auth.js',
  'js/views/pos.js',
  'js/views/payment.js',
  'js/views/shift.js',
  'js/views/inventory.js',
  'js/views/reports.js',
  'js/views/settings.js',
  'js/app.js',
  'https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Space+Mono:wght@400;700&display=swap',
  'https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200',
  'https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2',
  'https://cdn.jsdelivr.net/npm/chart.js'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(ASSETS).catch((err) => console.warn('Cache addAll error:', err)))
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.map((k) => (k !== CACHE_NAME ? caches.delete(k) : null)))
    )
  );
  self.clients.claim();
});

self.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET') return;
  const url = new URL(event.request.url);

  // Network-first for HTML to ensure user gets latest updates when deployed on Vercel
  if (event.request.headers.get('accept')?.includes('text/html') || url.pathname === '/' || url.pathname.endsWith('index.html')) {
    event.respondWith(
      fetch(event.request)
        .then((response) => {
          if (response.status === 200) {
            const resClone = response.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(event.request, resClone));
          }
          return response;
        })
        .catch(() => caches.match(event.request).then((cached) => cached || caches.match('/index.html')))
    );
    return;
  }

  // Cache-first, fallback to network for static assets
  event.respondWith(
    caches.match(event.request).then((response) => {
      if (response) return response;
      return fetch(event.request).then((networkResponse) => {
        if (networkResponse && networkResponse.status === 200 && (event.request.url.startsWith('http') || event.request.url.startsWith('https'))) {
          const resClone = networkResponse.clone();
          caches.open(CACHE_NAME).then((cache) => cache.put(event.request, resClone));
        }
        return networkResponse;
      }).catch(() => {
        // Return offline fallback if appropriate
      });
    })
  );
});
