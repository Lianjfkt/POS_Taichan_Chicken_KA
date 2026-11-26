# KA POS Web & Android Unified Cloud Interconnection Design

> **Document:** Design Specification  
> **Date:** 2026-11-25  
> **Topic:** Menghubungkan Web App (PWA) dan Android Native App Secara Penuh via Supabase (Tanpa Layar Dapur / KDS)

---

## 1. Background & Problem Statement

Sistem **KA POS** memiliki dua klien:
1. **Web App (PWA)** di root directory (`index.html`, `js/`, `css/`).
2. **Android Native App** di `android/` (`com.kapos.mobile`).

Keduanya dirancang untuk berkomunikasi dengan **Supabase PostgreSQL REST & Realtime API**. Namun saat ini keduanya tidak saling terhubung karena:
1. **Perbedaan Skema Database**: Skema root `supabase_schema.sql` masih v2.0 (outdated), sedangkan Android menggunakan skema v3.0 di `android/docs/supabase_schema.sql`.
2. **Payload Transaksi Web App Tidak Valid**: Web App `js/supabase.js` mencoba meng-insert properti `items` langsung ke tabel `orders` dan menggunakan field non-eksis (`discount_amount`, `total_amount`), sehingga ditolak oleh Supabase (HTTP 400).
3. **Web App Bersifat 1-Arah**: Web App hanya memiliki logika push lokal ke cloud (yang saat ini juga error), tanpa logika pull untuk menarik transaksi, pesanan, atau menu dari Android.
4. **Fitur Layar Dapur (KDS)**: Sesuai arahan pengguna, fitur layar dapur ditiadakan / dihapus total dari arsitektur sinkronisasi dan alur sistem.

---

## 2. Goals & Non-Goals

### Goals
- **Single Source of Truth Skema**: Mengunifikasi `supabase_schema.sql` dengan skema v3.0 (12 tabel, RLS anti-fraud, relasional `orders` dan `order_items`).
- **Two-Way Interconnection**:
  - Transaksi yang dibuat di Android otomatis ditarik dan muncul di riwayat transaksi & laporan Web App.
  - Transaksi yang dibuat di Web App tersimpan valid di tabel `orders` dan `order_items` Supabase dan dapat dibaca oleh Android.
  - Perubahan produk & kategori menu tersinkronisasi 2 arah.
  - Sinkronisasi pelanggan (CRM) dan stok bahan baku 2 arah.
- **Realtime Updates via Supabase CDC**:
  - Web App mendengarkan perubahan realtime pada tabel `orders` sehingga data penjualan terbaru langsung tampil tanpa reload manual.
- **Pembersihan Bersih (Clean Removal) Fitur Layar Dapur**:
  - Memastikan tidak ada sisa dependensi, tab, atau alur KDS/dapur yang membebani sinkronisasi.
- **Peningkatan Tambahan yang Dibutuhkan**:
  - Tombol manual "Sinkronkan Sekarang (2-Arah)" di menu Pengaturan Web App.
  - Cetak struk fallback via System Print (`window.print()`) di Web App.
  - Hash SHA-256 untuk PIN staff & owner di Web App.

### Non-Goals
- Tidak mengimplementasikan atau mempertahankan Layar Dapur (KDS).
- Tidak mengubah arsitektur inti Android (Android sudah memiliki 84 passing tests dan arsitektur Room DB v7 yang stabil).

---

## 3. Architecture & Data Flow

```
┌──────────────────────────────────────────────────────────────┐
│                    SUPABASE POSTGRESQL                       │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ Tables: categories, products, modifiers, orders,         │ │
│ │         order_items, raw_materials, material_batches,     │ │
│ │         recipes, recipe_items, customers, shifts,        │ │
│ │         cash_movements                                   │ │
│ └────────────────────────────┬─────────────────────────────┘ │
└──────────────────────────────┼───────────────────────────────┘
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
 ┌──────────────────────┐              ┌──────────────────────┐
 │     ANDROID APP      │              │       WEB APP        │
 │ Room DB (Local SSOT) │              │LocalStorage (SSOT)   │
 │ SupabaseClient (REST)│              │SupabaseService (REST)│
 │ Auto-sync Background │              │Realtime CDC Channel  │
 └──────────────────────┘              └──────────────────────┘
```

### 3.1 Unifikasi Skema Database
Salin dan mutakhirkan `supabase_schema.sql` di root agar identik dengan `android/docs/supabase_schema.sql`:
- `categories`: `id`, `name`, `emoji`, `color`, `sort_order`, `is_active`, timestamps.
- `products`: `id`, `category_id`, `name`, `price`, `cost_price`, `emoji`, `spice_level`, `description`, `is_active`, `allow_spice_level`, `allow_sambal`, `allow_complimentary`, `allow_toppings`.
- `modifiers`: `id`, `name`, `price`, `category`, `is_default`, `is_active`, `sort_order`.
- `orders`: `id`, `order_no`, `customer_name`, `customer_phone`, `order_type`, `table_no`, `subtotal`, `discount`, `total`, `payment_method`, `status`, `cashier_id`, `notes`, `void_reason`, `paid_at`, `created_at`.
- `order_items`: `id`, `order_id`, `product_id`, `product_name`, `price`, `quantity`, `spice_level`, `notes`, `subtotal`, `created_at`.
- `raw_materials` & `material_batches`: Bahan baku & batch expiry.
- `customers`: `id`, `name`, `phone`, `email`, `points`, `total_spent`, `visit_count`, `last_visit`.
- `shifts` & `cash_movements`: Sesi kasir dan pergerakan kas kecil (petty cash).
- Kebijakan RLS: SELECT, INSERT, UPDATE diizinkan untuk operasional; DELETE fisik pada `orders`, `order_items`, `shifts`, dan `cash_movements` diblokir total untuk audit anti-fraud.

---

## 4. Web App Refactoring Specification (`js/supabase.js`)

### 4.1 Order & Order Items Synchronization
Saat order dibuat (`queueTransaction`) atau antrean offline dikirim (`syncOfflineQueue`):
1. **Header Order (`orders`)**:
   ```javascript
   const orderPayload = {
     id: trx.no,
     order_no: trx.no,
     customer_name: trx.pelanggan || null,
     customer_phone: trx.customerPhone || null,
     order_type: trx.tipe || 'dine-in',
     table_no: trx.meja || null,
     subtotal: Number(trx.subtotal || trx.total || 0),
     discount: Number(trx.diskon || 0),
     total: Number(trx.total || 0),
     payment_method: trx.metode || 'cash',
     status: trx.status === 'void' ? 'void' : 'completed',
     cashier_id: trx.kasir || 'Kasir',
     notes: trx.catatan || null,
     void_reason: trx.voidReason || null,
     paid_at: new Date(trx.tgl || Date.now()).toISOString(),
     created_at: new Date(trx.tgl || Date.now()).toISOString()
   };
   ```
2. **Detail Order Items (`order_items`)**:
   ```javascript
   const itemPayloads = (trx.items || []).map((it, idx) => ({
     id: `${trx.no}_ITEM_${idx + 1}`,
     order_id: trx.no,
     product_id: String(it.id || it.productId || ''),
     product_name: it.nm || it.name || '',
     price: Number(it.hr || it.price || 0),
     quantity: Number(it.qty || it.quantity || 1),
     spice_level: it.level || it.spiceLevel || '',
     notes: it.catatan || it.notes || '',
     subtotal: Number(it.subtotal || ((it.hr || 0) * (it.qty || 1))),
     created_at: new Date(trx.tgl || Date.now()).toISOString()
   }));
   ```
3. Eksekusi `upsert` pada `orders` terlebih dahulu, kemudian `upsert` pada `order_items`.

### 4.2 Two-Way Sync Implementation (Pulling Data)
1. `pullOrders(limit = 200)`:
   - Ambil data dari tabel `orders` (order by `created_at.desc`).
   - Ambil data dari `order_items` untuk ID order yang ditarik.
   - Konversikan ke format `window.State.transactions`:
     - `no`: order.order_no || order.id
     - `tgl`: new Date(order.created_at).getTime()
     - `items`: items list sesuai schema internal
     - `total`, `subtotal`, `diskon`, `metode`, `status`, `kasir`, `pelanggan`
   - Gabungkan ke `window.State.transactions` dengan deduplikasi (order ID unik).
   - Simpan ke `localStorage` via `window.State.save(LS_KEYS.trx, ...)`.
2. `pullCategoriesAndProducts()`:
   - Ambil dari tabel `categories` dan `products`.
   - Update `window.State.categories` dan `window.State.products` (Cloud-wins).
   - Simpan ke `localStorage` dan emit event untuk re-render katalog POS.
3. `pullCustomers()`:
   - Ambil dari tabel `customers` dan sinkronkan dengan `window.State.customers`.
4. `syncAll()`:
   - Jalankan `syncOfflineQueue()`, kemudian `pullCategoriesAndProducts()`, `pullOrders()`, `pullCustomers()`.

### 4.3 Realtime Subscriptions (No KDS)
- Subscribe ke `public:orders` (INSERT / UPDATE).
  - Saat ada transaksi baru dari Android: langsung masukkan ke `window.State.transactions` jika belum ada, emit `transactions:updated`, update badge total omzet di dashboard laporan jika sedang dibuka.
- Subscribe ke `public:products` dan `public:categories`.
  - Jika admin/kasir merubah menu di Android, katalog Web App terupdate otomatis.

---

## 5. UI Updates & Quality Improvements

1. **Web App Settings (`js/views/settings.js`)**:
   - Tambahkan tombol **"Sinkronkan Sekarang (2-Arah)"**.
   - Tampilkan indikator status sinkronisasi terakhir (waktu dan jumlah data tersinkron).
2. **Cetak Struk Fallback (TASK 8)**:
   - Di modal cetak nota ([`js/printer.js`](file:///media/lian/Ubuntu/POS%20KA/js/printer.js)), sediakan tombol **"Cetak via Printer Komputer/USB"** yang memanggil `window.print()` dengan CSS `@media print` 58mm/80mm.
3. **PIN Hash Protection (TASK 10)**:
   - Simpan PIN Owner & Kasir dengan hash SHA-256 menggunakan `crypto.subtle.digest`.

---

## 6. Verification & Test Plan

1. **Skema SQL**:
   - Validasi sintaks skema PostgreSQL agar valid dan lolos eksekusi di Supabase SQL Editor.
2. **Android App**:
   - Jalankan `./gradlew testDebugUnitTest` dengan JDK Android Studio, pastikan seluruh 84 unit tests tetap lolos 100%.
3. **Web App Interconnection**:
   - Pengujian simpan order di Web App menghasilkan payload valid untuk tabel `orders` dan `order_items`.
   - Pengujian pull order mengonversi payload Supabase ke entitas lokal Web App secara akurat tanpa data loss.
   - Pengujian offline fallback: jika offline, order tersimpan di `offlineQueue` dan sukses di-flush saat online.
4. **Pembersihan KDS**:
   - Verifikasi tidak ada error di konsol terkait ketiadaan layar dapur.

