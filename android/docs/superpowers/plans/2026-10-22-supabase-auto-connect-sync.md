# Supabase Auto-Connect & Comprehensive Two-Way Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mengotomatiskan koneksi Supabase tanpa input manual saat aplikasi di-install (zero-config) dan mengimplementasikan sinkronisasi 2 arah penuh untuk Kategori, Produk, Orders, dan Order Items.

**Architecture:** Default Supabase credentials disimpan di `AppConfig` dan menjadi default value di `SettingsRepository` (dengan fallback aman jika SharedPreferences kosong). `SupabaseClient` diperluas untuk mendukung operasi upsert dan fetch riwayat transaksi. `PosRepository` mengorkestrasi sinkronisasi 2 arah penuh saat startup, interval berkala, maupun tombol manual.

**Tech Stack:** Kotlin, OkHttp, Kotlinx Serialization, AndroidX Room DB, Jetpack Compose, JUnit 4, Kotlin Coroutines & Flow.

## Global Constraints
- Target SDK: 34, Min SDK: 26, Java 17.
- Default Supabase URL: `https://umddrfuuywkuzxmjvgxj.supabase.co`
- Default Supabase Anon Key: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVtZGRyZnV1eXdrdXp4bWp2Z3hqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTA5MjQ5OTAsImV4cCI6MjEwNjUwMDk5MH0.GvS2q1RDPIH3vr-wA2bnxABd-eSoNgwMmqICOb3u-04`
- Offline-first: aplikasi tidak boleh crash atau terblokir jika network terputus/offline.
- Non-destructive Room migrations / operations: gunakan `upsert` / conflict replace.

---

### Task 1: AppConfig & Default Credentials in SettingsRepository

**Files:**
- Create: `app/src/main/java/com/kapos/mobile/core/config/AppConfig.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/repository/SettingsRepository.kt`
- Test: `app/src/test/java/com/kapos/mobile/core/data/repository/SettingsRepositoryAutoConfigTest.kt`

**Interfaces:**
- Produces:
  - `AppConfig.DEFAULT_SUPABASE_URL: String`
  - `AppConfig.DEFAULT_SUPABASE_ANON_KEY: String`
  - `AppSettings.supabaseUrl` (defaults to `AppConfig.DEFAULT_SUPABASE_URL`)
  - `AppSettings.supabaseAnonKey` (defaults to `AppConfig.DEFAULT_SUPABASE_ANON_KEY`)

- [ ] **Step 1: Write failing unit test for default credentials**
Create `app/src/test/java/com/kapos/mobile/core/data/repository/SettingsRepositoryAutoConfigTest.kt` verifying that an unconfigured/fresh `SettingsRepository` immediately yields active Supabase credentials without manual input.

- [ ] **Step 2: Run test to confirm failure**
Run `./gradlew testDebugUnitTest --tests com.kapos.mobile.core.data.repository.SettingsRepositoryAutoConfigTest` and verify failure.

- [ ] **Step 3: Create AppConfig & update SettingsRepository**
Create `AppConfig.kt` with project URL & Anon key. Update `AppSettings` defaults and `SettingsRepository.loadSettings()` to use `AppConfig.DEFAULT_SUPABASE_URL` and `AppConfig.DEFAULT_SUPABASE_ANON_KEY` when prefs are null or empty.

- [ ] **Step 4: Run test to confirm pass**
Run `./gradlew testDebugUnitTest --tests com.kapos.mobile.core.data.repository.SettingsRepositoryAutoConfigTest`.

- [ ] **Step 5: Commit changes**
`git add . && git commit -m "feat(settings): add AppConfig and automated Supabase credentials on fresh install"`

---

### Task 2: SupabaseClient Two-Way Sync API Extensions

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/core/data/remote/SupabaseClient.kt`
- Test: `app/src/test/java/com/kapos/mobile/core/data/remote/SupabaseClientSyncTest.kt`

**Interfaces:**
- Produces:
  - `SupabaseClient.upsertCategories(categories: List<SupabaseCategoryDto>): Boolean`
  - `SupabaseClient.upsertProducts(products: List<SupabaseProductDto>): Boolean`
  - `SupabaseClient.fetchOrders(limit: Int = 200): List<SupabaseOrderPayload>`
  - `SupabaseClient.fetchOrderItems(orderIds: List<String>): List<SupabaseOrderItemPayload>`

- [ ] **Step 1: Write failing unit test for new SupabaseClient methods**
Test serialization and endpoint generation for `upsertCategories`, `upsertProducts`, `fetchOrders`, and `fetchOrderItems`.

- [ ] **Step 2: Run test to confirm failure**
Run `./gradlew testDebugUnitTest --tests com.kapos.mobile.core.data.remote.SupabaseClientSyncTest`.

- [ ] **Step 3: Implement upsert & fetch methods in SupabaseClient**
Add `upsertCategories`, `upsertProducts`, `fetchOrders`, and `fetchOrderItems` with proper HTTP headers (`Prefer: resolution=merge-duplicates`, `apikey`, `Authorization`).

- [ ] **Step 4: Run tests to confirm pass**
Run `./gradlew testDebugUnitTest --tests com.kapos.mobile.core.data.remote.SupabaseClientSyncTest`.

- [ ] **Step 5: Commit changes**
`git add . && git commit -m "feat(remote): add upsert and transaction fetch methods to SupabaseClient"`

---

### Task 3: Comprehensive Two-Way Sync Engine in PosRepository

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/core/data/repository/PosRepository.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/local/dao/OrderDao.kt` (tambahkan query jika perlu untuk bulk check / insert)
- Test: `app/src/test/java/com/kapos/mobile/core/data/repository/PosRepositoryTwoWaySyncTest.kt`

**Interfaces:**
- Produces:
  - `PosRepository.syncAll(): Boolean`
  - `PosRepository.syncMenu(): Boolean`
  - `PosRepository.syncTransactions(): Boolean`

- [ ] **Step 1: Write failing unit test for 2-way sync**
Verify that `syncMenu()` pulls from Supabase and pushes local categories/products, and `syncTransactions()` pushes pending local orders and pulls remote orders into Room DB.

- [ ] **Step 2: Run test to confirm failure**
Run `./gradlew testDebugUnitTest --tests com.kapos.mobile.core.data.repository.PosRepositoryTwoWaySyncTest`.

- [ ] **Step 3: Implement two-way sync logic in PosRepository**
Implement `syncMenu()`, `syncTransactions()`, and consolidate `syncAll()`. Replace `refreshFromSupabase()` to call `syncAll()`. Ensure order items are restored alongside pulled orders.

- [ ] **Step 4: Run test to confirm pass**
Run `./gradlew testDebugUnitTest --tests com.kapos.mobile.core.data.repository.PosRepositoryTwoWaySyncTest`.

- [ ] **Step 5: Commit changes**
`git add . && git commit -m "feat(sync): implement comprehensive two-way sync engine in PosRepository"`

---

### Task 4: UI & Startup Trigger Integration

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/KaPosApplication.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/settings/SettingsScreen.kt`
- Test: `app/src/test/java/com/kapos/mobile/feature/settings/SettingsViewModelSyncTest.kt`

**Interfaces:**
- Consumes: `PosRepository.syncAll()`
- Produces: UI feedback on full sync execution (timestamp, status snackbar / message)

- [ ] **Step 1: Write unit test for SettingsViewModel sync trigger**
Test that `syncNow()` calls `posRepository.syncAll()` and updates UI state with sync status and timestamp.

- [ ] **Step 2: Update SettingsViewModel & SettingsScreen**
Wire `syncNow()` to perform `syncAll()`, display last sync time, and ensure button reflects loading state. In `KaPosApplication.kt`, ensure `supabaseClient` is initialized with default credentials and immediately starts initial sync in background.

- [ ] **Step 3: Run test to confirm pass**
Run `./gradlew testDebugUnitTest --tests com.kapos.mobile.feature.settings.SettingsViewModelSyncTest`.

- [ ] **Step 4: Commit changes**
`git add . && git commit -m "feat(ui): connect full two-way sync to SettingsScreen and startup flow"`

---

### Task 5: Supabase SQL Reference Schema

**Files:**
- Create: `docs/supabase_schema.sql`

**Interfaces:**
- Produces: Ready-to-execute PostgreSQL DDL for tables `customers`, `raw_materials`, `recipes`, `shifts`, `cash_movements`, `stock_movements`, `material_batches`.

- [ ] **Step 1: Write complete Supabase PostgreSQL DDL script**
Write `docs/supabase_schema.sql` containing standard column types, foreign keys, RLS policies, and indexes matching the app's Room entities.

- [ ] **Step 2: Commit file**
`git add docs/supabase_schema.sql && git commit -m "docs: add supabase reference schema for inventory and shift tables"`

---

### Task 6: Full Verification & E2E Integration Test

**Files:**
- Test: All tests in `./gradlew test`

- [ ] **Step 1: Execute full test suite**
Run `./gradlew test` to ensure all existing and new tests pass without regressions.

- [ ] **Step 2: Test live Supabase connection**
Run a verification script to verify live connection and DTO compatibility against the user's Supabase project.

- [ ] **Step 3: Commit and summarize**
Final commit if any polish needed, then present completion summary to user.
