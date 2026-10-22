# Design Spec: Supabase Auto-Connect & Comprehensive Two-Way Sync

- **Date**: 2026-10-22
- **Author**: Antigravity Assistant & Engineering Team
- **Status**: Approved
- **Target Version**: KA POS v1.3.0
- **Scope**: Automated Zero-Config Supabase Connectivity & Bi-directional Data Sync (Categories, Products, Orders, Order Items) + Additional Schema Reference

---

## 1. Problem Statement & Goals

### 1.1 Context & Problems
1. **Manual Credential Typing on Install**: Setiap kali aplikasi KA POS di-install atau di-reinstall pada perangkat Android (termasuk multi-device kasir), URL Supabase dan Anon Key masih kosong secara default. Kasir atau owner harus menyalin-menempel kredensial secara manual di halaman Pengaturan.
2. **One-Way / Incomplete Sync**:
   - Menu & Kategori hanya diunduh dari Supabase (`fetchCategories()`, `fetchProducts()`), tetapi produk baru atau perubahan menu yang dibuat di aplikasi belum di-push ke cloud Supabase.
   - Transaksi (`orders` & `order_items`) hanya dikirim ke cloud (`syncPendingOrders()`), namun perangkat baru tidak dapat menarik riwayat transaksi terdahulu dari cloud (`fetchOrders()`).
   - Apabila aplikasi di-reinstall di perangkat baru, riwayat penjualan lokal tampak kosong.

### 1.2 Goals
1. **Zero-Config Supabase Connection**: URL (`https://umddrfuuywkuzxmjvgxj.supabase.co`) dan Anon Public Key tertanam sebagai default konfigurasi bawaan aplikasi. Saat aplikasi dibuka pertama kali, koneksi Supabase langsung aktif tanpa memerlukan input manual, namun tetap dapat diubah via Pengaturan jika diperlukan.
2. **Comprehensive Two-Way Sync (Menyeluruh 2 Arah)**:
   - **Katalog Menu**: Sinkronisasi 2 arah penuh (Unduh update dari Supabase & Unggah perubahan lokal ke Supabase).
   - **Transaksi**: Sinkronisasi 2 arah penuh (Unggah transaksi baru/VOID ke Supabase & Unduh riwayat transaksi terdahulu dari Supabase).
3. **Database Schema Ekstensi**: Menyediakan file skema SQL (`supabase_schema.sql`) untuk tabel tambahan (`customers`, `raw_materials`, `recipes`, `shifts`, `stock_movements`, `material_batches`) yang siap dieksekusi di Supabase kapan pun owner ingin memperluas sinkronisasi.

---

## 2. Architecture & Components

```
+-------------------------------------------------------+
|                   KaPosApplication                    |
|   - Initializes SupabaseClient with Default Config    |
|   - Triggers initial sync on startup                  |
+-------------------------------------------------------+
                           |
                           v
+--------------------------+----------------------------+
|                    SettingsRepository                 |
|   - Default: SUPABASE_URL & SUPABASE_ANON_KEY         |
|   - SharedPreferences fallback to Default Config      |
+--------------------------+----------------------------+
                           |
                           v
+-------------------------------------------------------+
|                     PosRepository                     |
|  Two-Way Sync Engine:                                 |
|  1. syncAll() -> syncMenu() + syncTransactions()      |
|  2. Periodic background sync (configurable interval)  |
|  3. Manual sync triggered via Settings                |
+--------------------------+----------------------------+
       |                                       |
       v (Local Room DB)                       v (Network REST)
+-----------------------+              +-----------------------+
|  AppDatabase (Room)   |              |    SupabaseClient     |
|  - CategoryDao        |              |  - fetch/upsertCats   |
|  - ProductDao         |              |  - fetch/upsertProds  |
|  - OrderDao           |              |  - fetch/postOrders   |
+-----------------------+              +-----------------------+
                                                   |
                                                   v HTTPS REST
                                       +-----------------------+
                                       |    Supabase Cloud     |
                                       |  - categories         |
                                       |  - products           |
                                       |  - orders             |
                                       |  - order_items        |
                                       +-----------------------+
```

---

## 3. Detailed Component Specifications

### 3.1 AppConfig / Default Supabase Credentials
File: `core/config/AppConfig.kt` (atau `core/data/remote/SupabaseConfig.kt`)
```kotlin
package com.kapos.mobile.core.config

object AppConfig {
    const val DEFAULT_SUPABASE_URL = "https://umddrfuuywkuzxmjvgxj.supabase.co"
    const val DEFAULT_SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVtZGRyZnV1eXdrdXp4bWp2Z3hqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTA5MjQ5OTAsImV4cCI6MjEwNjUwMDk5MH0.GvS2q1RDPIH3vr-wA2bnxABd-eSoNgwMmqICOb3u-04"
}
```

### 3.2 SettingsRepository Updates
File: `core/data/repository/SettingsRepository.kt`
- Di dalam `AppSettings`:
  ```kotlin
  data class AppSettings(
      val supabaseUrl: String = AppConfig.DEFAULT_SUPABASE_URL,
      val supabaseAnonKey: String = AppConfig.DEFAULT_SUPABASE_ANON_KEY,
      ...
  )
  ```
- Di dalam `loadSettings()`:
  ```kotlin
  val savedUrl = prefs.getString("supabase_url", null)
  val savedKey = prefs.getString("supabase_anon_key", null)
  val finalUrl = if (savedUrl.isNullOrBlank()) AppConfig.DEFAULT_SUPABASE_URL else savedUrl
  val finalKey = if (savedKey.isNullOrBlank()) AppConfig.DEFAULT_SUPABASE_ANON_KEY else savedKey
  ```
- Menjamin saat aplikasi di-install di perangkat mana pun, `settingsFlow.value.supabaseUrl` dan `supabaseAnonKey` langsung terisi dan `supabaseClient.isConfigured()` langsung mengembalikan `true`.

### 3.3 SupabaseClient Enhancements
File: `core/data/remote/SupabaseClient.kt`
Tambahan kemampuan REST API:
1. **`upsertCategories(categories: List<SupabaseCategoryDto>): Boolean`**:
   - HTTP POST ke `$baseUrl/rest/v1/categories` dengan header `Prefer: resolution=merge-duplicates`.
2. **`upsertProducts(products: List<SupabaseProductDto>): Boolean`**:
   - HTTP POST ke `$baseUrl/rest/v1/products` dengan header `Prefer: resolution=merge-duplicates`.
3. **`fetchOrders(limit: Int = 200): List<SupabaseOrderPayload>`**:
   - HTTP GET ke `$baseUrl/rest/v1/orders?order=created_at.desc&limit=$limit`.
4. **`fetchOrderItems(orderIds: List<String>): List<SupabaseOrderItemPayload>`**:
   - HTTP GET ke `$baseUrl/rest/v1/order_items?order_id=in.(${orderIds.joinToString(",")})`.

### 3.4 Two-Way Sync Engine in PosRepository
File: `core/data/repository/PosRepository.kt`
- Fungsi utama: `suspend fun syncAll(): SyncResult`
  1. **Test Connectivity**: Cek `supabaseClient.testConnection()`. Jika gagal, tandai `_isOnline = false` dan batalkan tanpa melempar crash.
  2. **Sync Categories (2 Arah)**:
     - *Pull*: `fetchCategories()` -> `categoryDao.upsertAll()`.
     - *Push*: Ambil kategori lokal `categoryDao.getAll()`, konversi ke `SupabaseCategoryDto`, lalu `supabaseClient.upsertCategories()`.
  3. **Sync Products (2 Arah)**:
     - *Pull*: `fetchProducts()` -> `productDao.upsertAll()`.
     - *Push*: Ambil produk lokal `productDao.getAll()`, konversi ke `SupabaseProductDto`, lalu `supabaseClient.upsertProducts()`.
  4. **Sync Orders (2 Arah)**:
     - *Push*: `syncPendingOrders()` -> Kirim semua order lokal berstatus `PENDING_SYNC` ke Supabase.
     - *Pull*: `fetchOrders()` -> Bandingkan dengan order yang ada di lokal. Jika ada order dari Supabase yang belum ada di lokal, simpan ke `orderDao.insertOrder(orderEntity)` beserta `fetchOrderItems()` dengan status `SYNCED`.
- Dipicu saat:
  - Startup aplikasi (`init` di `PosRepository`).
  - Interval periodik latar belakang (`startPeriodicSync()`).
  - Manual action dari UI Pengaturan (`onSyncNowClicked`).

---

## 4. Error Handling & Edge Cases

1. **Koneksi Terputus / No Internet**:
   - Seluruh operasi sync dibungkus `try-catch` dengan timeout aman (15 detik).
   - Tidak ada blocking pada UI thread.
   - Status `isOnline` otomatis menjadi `false`.
   - Transaksi kasir tetap 100% berjalan normal dalam mode offline (*Offline-First*).
2. **Konflik Data (Merge Resolution)**:
   - Supabase menggunakan `Prefer: resolution=merge-duplicates` berdasarkan Primary Key (`id`).
   - SQLite Room menggunakan `@Insert(onConflict = OnConflictStrategy.REPLACE)` / `upsertAll`.
3. **Pembersihan Cache / Reset Settings**:
   - Jika pengguna menekan "Reset Default" atau SharedPreferences dihapus, aplikasi otomatis kembali menggunakan kredensial `AppConfig` tanpa pernah menjadi string kosong yang memutus koneksi.

---

## 5. Testing Plan

1. **Unit Testing**:
   - `SettingsRepositoryAutoConfigTest`: Memverifikasi default URL dan Key langsung aktif saat pertama kali instansiasi tanpa file prefs.
   - `PosRepositoryTwoWaySyncTest`: Memverifikasi alur 2-arah (pull kategori/produk/order dari cloud ke DB lokal dan push data lokal ke cloud).
2. **Integration Verification**:
   - Menguji interaksi langsung ke Supabase endpoint dengan unit test atau script koneksi.
   - Memastikan compile build dan seluruh test yang ada tetap 100% passing (`./gradlew test`).
