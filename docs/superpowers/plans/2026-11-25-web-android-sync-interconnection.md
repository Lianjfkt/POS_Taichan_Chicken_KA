# KA POS Web & Android Unified Cloud Interconnection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Menghubungkan Web App (PWA) dan Android Native App secara penuh melalui Supabase Cloud dengan skema v3.0 terpadu, sinkronisasi 2-arah (pull & push order, order items, menu, customer), Supabase Realtime CDC, dan perbaikan printer & security (tanpa fitur layar dapur / KDS).

**Architecture:** Memperbarui skema Supabase PostgreSQL di root agar selaras dengan skema v3.0 Android (12 tabel). Merefaktor `js/supabase.js` di Web App untuk memisahkan insert `orders` dan `order_items` secara relasional, memperbaiki nama-nama kolom pada kategori/produk, membangun engine Two-Way Sync (`pullOrders`, `pullCategoriesAndProducts`, `pullCustomers`, `syncAll`), serta memasang Realtime CDC Listener pada tabel `orders`. Menambahkan fallback System Print (`window.print()`) dan hash SHA-256 untuk PIN staff/owner di Web App.

**Tech Stack:** 
- Web App: HTML5, Vanilla JavaScript (ES6+), CSS3, Supabase JS Client (v2 CDN), Web Crypto API
- Android App: Kotlin, Jetpack Compose, Room SQLite v7, OkHttp, Kotlinx Serialization
- Backend: Supabase PostgreSQL Database, PostgREST, Realtime Engine

## Global Constraints
- Seluruh kode Web App menggunakan vanilla JavaScript tanpa build step (tidak memerlukan webpack/vite).
- Mengikuti skema tabel Supabase v3.0 dari `android/docs/supabase_schema.sql`.
- DILARANG menyertakan, membuat, atau mempertahankan fitur layar dapur (KDS) dan dependensinya.
- Android unit tests (`./gradlew testDebugUnitTest`) harus tetap 100% lolos (84 tests passing).

---

### Task 1: Unifikasi Skema Database Supabase (`supabase_schema.sql`)

**Files:**
- Modify: `supabase_schema.sql` (di root repository)

**Interfaces:**
- Produces: Skema DDL PostgreSQL v3.0 tunggal untuk Web & Android (12 tabel: `categories`, `products`, `modifiers`, `orders`, `order_items`, `raw_materials`, `material_batches`, `recipes`, `recipe_items`, `customers`, `shifts`, `cash_movements` + RLS anti-fraud, tanpa role/fitur dapur).

- [ ] **Step 1: Mutakhirkan `supabase_schema.sql` di root**
Gantikan isi `supabase_schema.sql` dengan skema v3.0 yang bersih dari `android/docs/supabase_schema.sql`. Pastikan tabel `orders` dan `order_items` terdefinisi dengan kolom yang presisi, RLS aktif, dan tidak ada enum/tabel dapur:
```sql
-- Salin skema v3.0 dari android/docs/supabase_schema.sql
-- Pastikan tidak ada dependensi kds/dapur
```

- [ ] **Step 2: Validasi sintaks SQL dengan skrip pengujian**
Jalankan pengecekan validitas SQL menggunakan regex/parser python untuk memastikan tidak ada sintaks rusak:
```bash
python3 -c "
with open('/media/lian/Ubuntu/POS KA/supabase_schema.sql') as f:
    sql = f.read()
assert 'create table if not exists public.orders' in sql.lower()
assert 'create table if not exists public.order_items' in sql.lower()
assert 'create table if not exists public.raw_materials' in sql.lower()
print('SQL schema validation passed!')
"
```

- [ ] **Step 3: Commit perubahan skema**
```bash
git -C "/media/lian/Ubuntu/POS KA" add supabase_schema.sql
git -C "/media/lian/Ubuntu/POS KA" commit -m "feat(db): unify root supabase schema to v3.0 specification"
```

---

### Task 2: Refactor Order & Order Items Sync di Web App (`js/supabase.js`)

**Files:**
- Modify: `js/supabase.js:160-315`

**Interfaces:**
- Consumes: Transaksi dari `window.State.transactions` atau `window.State.offlineQueue`.
- Produces: `SupabaseService.queueTransaction(trx)`, `SupabaseService.syncOfflineQueue()`, dan `SupabaseService.updateOrderStatus(orderNo, status, voidData)` yang menyimpan ke tabel `orders` dan `order_items` secara relasional.

- [ ] **Step 1: Refactor `queueTransaction(trx)` untuk relasional `orders` + `order_items`**
Ubah implementasi `queueTransaction(trx)` agar:
1. Menyiapkan header `orders`:
   ```javascript
   const orderPayload = {
     id: trx.no || `ORD-${Date.now()}`,
     order_no: trx.no || `ORD-${Date.now()}`,
     customer_name: trx.pelanggan || null,
     customer_phone: trx.customerPhone || null,
     order_type: trx.tipe || 'dine-in',
     table_no: trx.meja || '',
     subtotal: Number(trx.subtotal || trx.total || 0),
     discount: Number(trx.diskon || 0),
     total: Number(trx.total || 0),
     payment_method: trx.metode || 'cash',
     status: trx.status === 'void' ? 'void' : 'completed',
     cashier_id: trx.kasir || 'Kasir',
     notes: trx.catatan || '',
     void_reason: trx.voidReason || null,
     paid_at: new Date(trx.tgl || Date.now()).toISOString(),
     created_at: new Date(trx.tgl || Date.now()).toISOString()
   };
   ```
2. Menyiapkan detail `order_items`:
   ```javascript
   const itemPayloads = (trx.items || []).map((it, idx) => ({
     id: `${orderPayload.id}_${idx + 1}`,
     order_id: orderPayload.id,
     product_id: String(it.id || it.productId || ''),
     product_name: it.nm || it.name || '',
     price: Number(it.hr || it.price || 0),
     quantity: Number(it.qty || it.quantity || 1),
     spice_level: it.level || it.spiceLevel || '',
     notes: it.catatan || it.notes || '',
     subtotal: Number(it.subtotal || ((it.hr || 0) * (it.qty || 1))),
     created_at: orderPayload.created_at
   }));
   ```
3. Lakukan insert ke `orders`, lalu insert ke `order_items`. Jika gagal atau offline, masukkan `trx` ke `window.State.offlineQueue`.

- [ ] **Step 2: Refactor `syncOfflineQueue()` dengan pemisahan `orders` & `order_items`**
Sesuaikan looping `syncOfflineQueue` agar memproses antrean dengan memisahkan insert header `orders` dan detail `order_items`, serta handling aksi `update_status` (Void).

- [ ] **Step 3: Verifikasi sintaks dan mapping transaksi**
Uji file `js/supabase.js` dengan node sintaks checker:
```bash
node -c "/media/lian/Ubuntu/POS KA/js/supabase.js"
```

- [ ] **Step 4: Commit perubahan order sync**
```bash
git -C "/media/lian/Ubuntu/POS KA" add js/supabase.js
git -C "/media/lian/Ubuntu/POS KA" commit -m "fix(sync): relational orders and order_items push to supabase"
```

---

### Task 3: Refactor Master Data Sync di Web App (`js/supabase.js`)

**Files:**
- Modify: `js/supabase.js:317-355`

**Interfaces:**
- Consumes: `window.State.categories`, `window.State.products`.
- Produces: `SupabaseService.pushLocalData()` yang melakukan upsert dengan nama kolom schema v3.0 (`emoji`, `category_id`, dll).

- [ ] **Step 1: Perbaiki pemetaan kolom pada `pushLocalData()`**
Perbaiki fungsi `pushLocalData()`:
```javascript
// 1. Categories
const catRows = window.State.categories.map(c => ({
  id: String(c.id),
  name: c.nm,
  emoji: c.emj || '🍽️',
  color: c.col || '#0B5C73',
  sort_order: Number(c.ord || 0),
  is_active: c.on !== false
}));
const { error: catErr } = await this.client.from('categories').upsert(catRows);
if (catErr) throw catErr;

// 2. Products
const prodRows = window.State.products.map(p => {
  // Cari ID kategori dari c.nm jika p.kat berupa nama kategori
  const catObj = window.State.categories.find(c => c.nm === p.kat || c.id === p.kat);
  const catId = catObj ? catObj.id : (p.kat || null);
  return {
    id: String(p.id),
    category_id: catId,
    name: p.nm,
    price: Number(p.hr || 0),
    cost_price: Number(p.md || 0),
    emoji: p.emj || '🍢',
    spice_level: p.lv || '',
    description: p.desc || '',
    is_active: p.on !== false
  };
});
const { error: prodErr } = await this.client.from('products').upsert(prodRows);
if (prodErr) throw prodErr;
```

- [ ] **Step 2: Verifikasi sintaks**
```bash
node -c "/media/lian/Ubuntu/POS KA/js/supabase.js"
```

- [ ] **Step 3: Commit perubahan master data push**
```bash
git -C "/media/lian/Ubuntu/POS KA" add js/supabase.js
git -C "/media/lian/Ubuntu/POS KA" commit -m "fix(sync): correct column mappings for categories and products in pushLocalData"
```

---

### Task 4: Implementasi Two-Way Sync (Pull Engine) di Web App (`js/supabase.js`)

**Files:**
- Modify: `js/supabase.js`

**Interfaces:**
- Produces: 
  - `SupabaseService.pullCategoriesAndProducts()`
  - `SupabaseService.pullOrders(limit)`
  - `SupabaseService.pullCustomers()`
  - `SupabaseService.syncAll()`

- [ ] **Step 1: Implementasikan `pullCategoriesAndProducts()`**
Tambahkan metode untuk mengunduh master menu dari Cloud (Cloud-Wins) dan memperbarui `window.State.categories` & `window.State.products`:
```javascript
async pullCategoriesAndProducts() {
  if (!this.client) return false;
  try {
    const { data: cats, error: catErr } = await this.client
      .from('categories')
      .select('*')
      .order('sort_order', { ascending: true });
    if (catErr) throw catErr;

    const { data: prods, error: prodErr } = await this.client
      .from('products')
      .select('*')
      .order('name', { ascending: true });
    if (prodErr) throw prodErr;

    if (cats && cats.length > 0) {
      window.State.categories = cats.map(c => ({
        id: c.id,
        nm: c.name,
        emj: c.emoji || '🍽️',
        col: c.color || '#0B5C73',
        ord: c.sort_order || 0,
        on: c.is_active !== false
      }));
      window.State.save(window.LS_KEYS.kat, window.State.categories);
      window.State.emit('categories:update', window.State.categories);
    }

    if (prods && prods.length > 0) {
      window.State.products = prods.map(p => {
        const cat = window.State.categories.find(c => c.id === p.category_id);
        return {
          id: p.id,
          nm: p.name,
          kat: cat ? cat.nm : 'Lainnya',
          hr: Number(p.price || 0),
          md: Number(p.cost_price || 0),
          emj: p.emoji || '🍢',
          lv: p.spice_level || '',
          desc: p.description || '',
          on: p.is_active !== false,
          bom: []
        };
      });
      window.State.save(window.LS_KEYS.prod, window.State.products);
      window.State.emit('products:update', window.State.products);
    }
    return true;
  } catch (err) {
    console.warn('[Supabase] pullCategoriesAndProducts error:', err);
    return false;
  }
}
```

- [ ] **Step 2: Implementasikan `pullOrders(limit = 200)`**
Tambahkan metode untuk mengunduh riwayat pesanan (termasuk pesanan dari Android) ke `window.State.transactions`:
```javascript
async pullOrders(limit = 200) {
  if (!this.client) return false;
  try {
    const { data: remoteOrders, error: orderErr } = await this.client
      .from('orders')
      .select('*')
      .order('created_at', { ascending: false })
      .limit(limit);
    if (orderErr) throw orderErr;
    if (!remoteOrders || remoteOrders.length === 0) return true;

    const orderIds = remoteOrders.map(o => o.id);
    const { data: remoteItems, error: itemErr } = await this.client
      .from('order_items')
      .select('*')
      .in('order_id', orderIds);
    if (itemErr) throw itemErr;

    const itemsByOrder = {};
    (remoteItems || []).forEach(it => {
      if (!itemsByOrder[it.order_id]) itemsByOrder[it.order_id] = [];
      itemsByOrder[it.order_id].push({
        id: it.product_id,
        nm: it.product_name,
        hr: Number(it.price || 0),
        qty: Number(it.quantity || 1),
        level: it.spice_level || '',
        catatan: it.notes || '',
        subtotal: Number(it.subtotal || 0)
      });
    });

    // Merge dengan transaksi lokal (berdasarkan nomor nota)
    const localMap = new Map((window.State.transactions || []).map(t => [t.no || t.id, t]));
    remoteOrders.forEach(ro => {
      const key = ro.order_no || ro.id;
      const existing = localMap.get(key);
      const items = itemsByOrder[ro.id] || (existing ? existing.items : []);
      const mergedTrx = {
        no: key,
        tgl: ro.created_at ? new Date(ro.created_at).getTime() : (existing ? existing.tgl : Date.now()),
        pelanggan: ro.customer_name || 'Pelanggan Umum',
        customerPhone: ro.customer_phone || '',
        tipe: ro.order_type || 'dine-in',
        meja: ro.table_no || '',
        subtotal: Number(ro.subtotal || ro.total || 0),
        diskon: Number(ro.discount || 0),
        total: Number(ro.total || 0),
        bayar: Number(existing ? existing.bayar : ro.total),
        kembali: Number(existing ? existing.kembali : 0),
        metode: ro.payment_method || 'cash',
        status: ro.status || 'completed',
        kasir: ro.cashier_id || 'Kasir',
        voidReason: ro.void_reason || (existing ? existing.voidReason : ''),
        voidBy: ro.void_by || (existing ? existing.voidBy : ''),
        items: items
      };
      localMap.set(key, mergedTrx);
    });

    const updatedTransactions = Array.from(localMap.values())
      .sort((a, b) => (b.tgl || 0) - (a.tgl || 0));

    window.State.transactions = updatedTransactions;
    window.State.save(window.LS_KEYS.trx, updatedTransactions);
    window.State.emit('transactions:update', updatedTransactions);
    return true;
  } catch (err) {
    console.warn('[Supabase] pullOrders error:', err);
    return false;
  }
}
```

- [ ] **Step 3: Implementasikan `pullCustomers()` dan `syncAll()`**
Tambahkan fungsi `pullCustomers()` dan gabungkan di `syncAll()`:
```javascript
async syncAll() {
  if (!this.client) return { success: false, message: 'Supabase belum terhubung.' };
  this.setStatus('syncing');
  try {
    // 1. Push pending offline orders
    await this.syncOfflineQueue();
    // 2. Pull latest catalog menu
    await this.pullCategoriesAndProducts();
    // 3. Pull transactions (dari Android & Web)
    await this.pullOrders();
    // 4. Pull customers
    await this.pullCustomers();

    this.setStatus('online');
    window.State.emit('sync:complete', { time: Date.now() });
    return { success: true, message: 'Sinkronisasi 2-arah berhasil!' };
  } catch (err) {
    this.setStatus('online');
    return { success: false, message: `Sinkronisasi gagal: ${err.message}` };
  }
}
```

- [ ] **Step 4: Verifikasi sintaks**
```bash
node -c "/media/lian/Ubuntu/POS KA/js/supabase.js"
```

- [ ] **Step 5: Commit Two-Way Sync implementation**
```bash
git -C "/media/lian/Ubuntu/POS KA" add js/supabase.js
git -C "/media/lian/Ubuntu/POS KA" commit -m "feat(sync): add two-way sync engine (pull orders, menu, customers) in web app"
```

---

### Task 5: Implementasi Supabase Realtime CDC di Web App (`js/supabase.js`)

**Files:**
- Modify: `js/supabase.js:125-158`

**Interfaces:**
- Produces: Realtime channel listener pada tabel `orders` dan `products` (tanpa fitur layar dapur).

- [ ] **Step 1: Update `subscribeRealtime()`**
Perbarui `subscribeRealtime()` agar mendengarkan tabel `orders` dan `products`:
```javascript
subscribeRealtime() {
  if (!this.client) return;
  try {
    if (this.channel) {
      this.client.removeChannel(this.channel);
    }

    this.channel = this.client.channel('public-db-changes')
      .on('postgres_changes', { event: '*', schema: 'public', table: 'orders' }, payload => {
        this.handleOrderChange(payload);
      })
      .on('postgres_changes', { event: '*', schema: 'public', table: 'products' }, payload => {
        this.handleProductChange(payload);
      })
      .subscribe((status) => {
        if (status === 'SUBSCRIBED') {
          console.log('[Supabase] Realtime CDC active for orders & products');
        }
      });
  } catch (e) {
    console.warn('[Supabase] Realtime subscription error:', e);
  }
}
```

- [ ] **Step 2: Tambahkan handler `handleOrderChange` & `handleProductChange`**
Ketika menerima event INSERT atau UPDATE pada `orders`, secara mulus perbarui `window.State.transactions` atau panggil `pullOrders(20)`.

- [ ] **Step 3: Verifikasi sintaks**
```bash
node -c "/media/lian/Ubuntu/POS KA/js/supabase.js"
```

- [ ] **Step 4: Commit Realtime updates**
```bash
git -C "/media/lian/Ubuntu/POS KA" add js/supabase.js
git -C "/media/lian/Ubuntu/POS KA" commit -m "feat(realtime): subscribe to orders and products changes in web app"
```

---

### Task 6: UI Settings & Tombol Sinkronisasi 2-Arah (`js/views/settings.js`, `index.html`)

**Files:**
- Modify: `js/views/settings.js`
- Modify: `index.html`

**Interfaces:**
- Consumes: `window.SupabaseService.syncAll()`.
- Produces: Tombol "Sinkronkan Sekarang (2-Arah)" dan indikator status sinkronisasi di UI Settings.

- [ ] **Step 1: Tambahkan tombol Sync 2-Arah di Pengaturan Cloud**
Di `index.html` (di bagian `#view-settings` card Supabase) dan di `js/views/settings.js`, sediakan tombol:
```html
<button type="button" class="btn btn-secondary" id="btn-sync-all">
  <span class="icon">🔄</span> Sinkronkan Semua (Tarik & Unggah)
</button>
<span id="sync-last-time" style="font-size:0.8rem; opacity:0.8; margin-left:8px;"></span>
```

- [ ] **Step 2: Bind handler di `js/views/settings.js`**
Hubungkan click listener pada tombol tersebut ke `window.SupabaseService.syncAll()` dengan loading feedback dan toast pemberitahuan.

- [ ] **Step 3: Commit perubahan UI Settings**
```bash
git -C "/media/lian/Ubuntu/POS KA" add index.html js/views/settings.js
git -C "/media/lian/Ubuntu/POS KA" commit -m "feat(ui): add manual two-way sync button and status in settings view"
```

---

### Task 7: Cetak Struk Komputer / USB Fallback (TASK 8) & Hash PIN (TASK 10)

**Files:**
- Modify: `js/printer.js`
- Modify: `js/views/auth.js`
- Modify: `js/state.js`

**Interfaces:**
- Produces:
  - `PrinterService.printSystemReceipt(order)` menggunakan `window.print()`.
  - Fungsi hashing SHA-256 untuk PIN staff & owner sebelum disimpan ke `localStorage`.

- [ ] **Step 1: Tambahkan tombol cetak sistem di modal struk (`js/printer.js`)**
Tambahkan tombol "Cetak via Komputer / USB" yang memanggil `window.print()` dengan CSS `@media print` 58mm/80mm:
```javascript
printSystemReceipt(trx) {
  // Buka dialog window.print() dengan styling cetak struk kasir
  window.print();
}
```

- [ ] **Step 2: Lindungi PIN dengan hash SHA-256 (`js/state.js` & `js/views/auth.js`)**
Buat utility asynchronous helper `hashPin(pin)` menggunakan Web Crypto API:
```javascript
async function hashPin(pin) {
  const msgUint8 = new TextEncoder().encode(String(pin));
  const hashBuffer = await crypto.subtle.digest('SHA-256', msgUint8);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}
```
Gunakan saat registrasi/penyimpanan PIN dan saat verifikasi login/void.

- [ ] **Step 3: Commit perbaikan printer dan hash PIN**
```bash
git -C "/media/lian/Ubuntu/POS KA" add js/printer.js js/views/auth.js js/state.js
git -C "/media/lian/Ubuntu/POS KA" commit -m "feat(security & hardware): add system print fallback and sha-256 pin protection"
```

---

### Task 8: Regresi Android, Verifikasi Sintaks & Dokumentasi

**Files:**
- Test: `./gradlew testDebugUnitTest`
- Modify: `DO_NEXT.md`

**Interfaces:**
- Produces: Verifikasi 100% test lulus dan update status task di `DO_NEXT.md`.

- [ ] **Step 1: Jalankan Android Unit Test**
Pastikan seluruh 84 unit tests di Android tetap lulus tanpa regresi:
```bash
cd "/media/lian/Ubuntu/POS KA/android" && ./gradlew testDebugUnitTest -Dorg.gradle.java.home=/opt/android-studio/jbr
```
Expected: `BUILD SUCCESSFUL` (84 tests passed).

- [ ] **Step 2: Update `DO_NEXT.md`**
Tandai Task 8 (Cetak Struk Fallback) dan Task 10 (Hash PIN) sebagai selesai di `DO_NEXT.md`.

- [ ] **Step 3: Commit final verification**
```bash
git -C "/media/lian/Ubuntu/POS KA" add DO_NEXT.md
git -C "/media/lian/Ubuntu/POS KA" commit -m "docs: update DO_NEXT.md with completed sync and security tasks"
```
