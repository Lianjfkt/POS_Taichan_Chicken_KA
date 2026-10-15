# KA POS Android Fase 2: Redesign Elegan, Login Multi-Role PIN, Stok Harian & Analisa BEP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Menghadirkan paritas fitur lengkap dari Web App ke Android: membersihkan seluruh icon/emoji "slop" menjadi UI minimalis elegan, menambahkan autentikasi pemisah role Kasir & Owner via keypad PIN 4-digit, modul pelacak stok harian (Taichan & Ayam), serta dashboard analisa finansial dengan tracking progres Break Even Point (BEP).

**Architecture:** MVI/MVVM dengan StateFlow reaktif di Jetpack Compose. State sesi (`AuthSession`) mengatur akses menu dinamis secara offline-first. Repository persisten Room & SharedPreferences mengelola inventori batch dan parameter finansial.

**Tech Stack:** Kotlin 1.9, Jetpack Compose Material 3, Android Room Database, Kotlin Coroutines & StateFlow, JUnit 4.

## Global Constraints

- Platform: Android 8.0+ (API 26+) dengan target Android 14 (API 34).
- Bebas dari emoji/icon kartun "slop" pada komponen kartu menu, modal, keranjang, dan kategori.
- Offline-first: Seluruh fitur autentikasi PIN, stok harian, dan kalkulasi BEP harus dapat berfungsi tanpa internet.
- Verifikasi build/test menggunakan perintah: `JAVA_HOME="/home/lian/Downloads/android-studio/jbr" ./gradlew testDebugUnitTest`.

---

### Task 1: Redesign Elegan & Pembersihan Emoji Slop

**Files:**
- Modify: `android/app/src/main/java/com/kapos/mobile/feature/pos/ProductCard.kt`
- Modify: `android/app/src/main/java/com/kapos/mobile/core/components/CategoryChips.kt`
- Modify: `android/app/src/main/java/com/kapos/mobile/feature/pos/ModifierDialog.kt`
- Modify: `android/app/src/main/java/com/kapos/mobile/feature/cart/CartBottomSheet.kt`

**Interfaces:**
- Consumes: `Product`, `Category`
- Produces: Komponen kartu produk bersih tanpa emoji banner, chips kategori teks modern, dialog modifier & cart tanpa emoji 28sp/22sp.

- [ ] **Step 1: Update ProductCard.kt to eliminate emoji banner**
Gantikan container 88dp pastel + 42sp emoji di `ProductCard.kt` dengan layout kartu minimalis:
Badge kategori kapsul teks, tipografi SemiBold 15sp nama menu, teks deskripsi/opsi pedas halus, harga kontras `Primary`, tombol `+` bundar minimalis.

- [ ] **Step 2: Update CategoryChips.kt to remove emoji chips**
Ganti chip yang menampilkan emoji (🍢, 🍗, 🥤, 🍚) dengan pill minimalis bersih hanya teks ber-outline halus dan status aktif kontras gelap.

- [ ] **Step 3: Update ModifierDialog.kt & CartBottomSheet.kt**
Hapus render emoji di header dialog kustomisasi dan baris keranjang belanja.

- [ ] **Step 4: Verify Compilation & Tests**
Run: `JAVA_HOME="/home/lian/Downloads/android-studio/jbr" ./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**
```bash
git add android/app/src/main/java/com/kapos/mobile/feature/pos/ProductCard.kt android/app/src/main/java/com/kapos/mobile/core/components/CategoryChips.kt android/app/src/main/java/com/kapos/mobile/feature/pos/ModifierDialog.kt android/app/src/main/java/com/kapos/mobile/feature/cart/CartBottomSheet.kt
git commit -m "style: redesign product cards, category chips, and dialogs to modern minimal aesthetic without slop emojis"
```

---

### Task 2: Auth Models & SettingsRepository PIN Management

**Files:**
- Create: `android/app/src/main/java/com/kapos/mobile/domain/model/AuthModels.kt`
- Modify: `android/app/src/main/java/com/kapos/mobile/core/data/repository/SettingsRepository.kt`
- Test: `android/app/src/test/java/com/kapos/mobile/core/data/repository/SettingsRepositoryAuthTest.kt`

**Interfaces:**
- Consumes: `SharedPreferences`
- Produces: `UserRole`, `AuthSession`, `cashierPin`, `ownerPin`, `dailyFixedCost`, `dailyRevenueTarget` di `AppSettings`.

- [ ] **Step 1: Write failing unit test for Auth & Settings**
Create `android/app/src/test/java/com/kapos/mobile/core/data/repository/SettingsRepositoryAuthTest.kt` menguji validasi PIN default kasir `1111` dan owner `1234`, verifikasi sesi, serta penyimpanan parameter target BEP harian.

- [ ] **Step 2: Run test to verify it fails**
Run: `JAVA_HOME="/home/lian/Downloads/android-studio/jbr" ./gradlew testDebugUnitTest --tests com.kapos.mobile.core.data.repository.SettingsRepositoryAuthTest`
Expected: FAIL (unresolved references)

- [ ] **Step 3: Create AuthModels.kt and update SettingsRepository.kt**
Definisikan:
```kotlin
enum class UserRole { CASHIER, OWNER }
data class AuthSession(
    val isLoggedIn: Boolean = false,
    val role: UserRole = UserRole.CASHIER,
    val cashierName: String = "Kasir Utama"
)
```
Tambahkan di `AppSettings`:
`cashierPin: String = "1111"`, `ownerPin: String = "1234"`, `dailyFixedCost: Double = 350000.0`, `dailyRevenueTarget: Double = 1500000.0`.
Tambahkan method `saveSession(session: AuthSession)` dan `clearSession()`.

- [ ] **Step 4: Run test to verify it passes**
Run: `JAVA_HOME="/home/lian/Downloads/android-studio/jbr" ./gradlew testDebugUnitTest --tests com.kapos.mobile.core.data.repository.SettingsRepositoryAuthTest`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add android/app/src/main/java/com/kapos/mobile/domain/model/AuthModels.kt android/app/src/main/java/com/kapos/mobile/core/data/repository/SettingsRepository.kt android/app/src/test/java/com/kapos/mobile/core/data/repository/SettingsRepositoryAuthTest.kt
git commit -m "feat(auth): add UserRole, AuthSession, and PIN & BEP target configs to SettingsRepository"
```

---

### Task 3: Login Screen & Keypad PIN Terminal

**Files:**
- Create: `android/app/src/main/java/com/kapos/mobile/feature/auth/LoginUiState.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/feature/auth/LoginViewModel.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/feature/auth/LoginScreen.kt`
- Test: `android/app/src/test/java/com/kapos/mobile/feature/auth/LoginViewModelTest.kt`

**Interfaces:**
- Consumes: `SettingsRepository`, `UserRole`, `AuthSession`
- Produces: `LoginScreen(onLoginSuccess: (AuthSession) -> Unit)`

- [ ] **Step 1: Write failing unit test for LoginViewModel**
Test validasi input 4-digit PIN:
- Input angka `1-1-1-1` pada role CASHIER -> Sukses login sebagai Kasir.
- Input salah -> Trigger error state & reset buffer.
- Input angka `1-2-3-4` pada role OWNER -> Sukses login sebagai Owner.

- [ ] **Step 2: Run test to verify it fails**
Run: `JAVA_HOME="/home/lian/Downloads/android-studio/jbr" ./gradlew testDebugUnitTest --tests com.kapos.mobile.feature.auth.LoginViewModelTest`
Expected: FAIL

- [ ] **Step 3: Implement LoginUiState and LoginViewModel**
Implementasikan pemrosesan PIN buffer (maksimal 4 karakter), role selection, tombol backspace, clear, dan event `onLoginSuccess`.

- [ ] **Step 4: Implement LoginScreen.kt with Jetpack Compose**
Buat antarmuka keypad PIN sentuh minimalis modern:
- Brand title **KA POS** dengan subtitle bersih.
- Segmented role switcher: `[ Kasir ]` | `[ Owner ]`.
- 4 dots indikator PIN dengan state aktif/kosong yang halus.
- Grid Numpad sentuh 4x3 (`1-9`, `C`, `0`, `⌫`).

- [ ] **Step 5: Run tests to verify they pass**
Run: `JAVA_HOME="/home/lian/Downloads/android-studio/jbr" ./gradlew testDebugUnitTest --tests com.kapos.mobile.feature.auth.LoginViewModelTest`
Expected: PASS

- [ ] **Step 6: Commit**
```bash
git add android/app/src/main/java/com/kapos/mobile/feature/auth/ android/app/src/test/java/com/kapos/mobile/feature/auth/
git commit -m "feat(auth): add LoginScreen with responsive 4-digit PIN keypad terminal"
```

---

### Task 4: Modul Stok Harian — Repository & ViewModel

**Files:**
- Create: `android/app/src/main/java/com/kapos/mobile/domain/model/StockModels.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/core/data/repository/StockRepository.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/feature/stock/StockUiState.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/feature/stock/StockViewModel.kt`
- Test: `android/app/src/test/java/com/kapos/mobile/feature/stock/StockViewModelTest.kt`

**Interfaces:**
- Consumes: `PosRepository` (riwayat pesanan hari ini)
- Produces: `StockRepository`, `StockUiState`, `StockViewModel`

- [ ] **Step 1: Write failing unit test for Stock tracking & Freshness logic**
Test:
- Freshness status: Hari ini = FRESH, 1 hari lalu = DAY_2, 2 hari lalu = DAY_3, >= 3 hari lalu = EXPIRED.
- Taichan sold counting: Menghitung akumulasi tusuk yang terjual dari transaksi hari ini.
- Tambah batch mentah baru dan update jumlah goreng.

- [ ] **Step 2: Run test to verify it fails**
Run: `JAVA_HOME="/home/lian/Downloads/android-studio/jbr" ./gradlew testDebugUnitTest --tests com.kapos.mobile.feature.stock.StockViewModelTest`
Expected: FAIL

- [ ] **Step 3: Implement StockModels, StockRepository, and StockViewModel**
Implementasikan model `TaichanBatch`, `ChickenBatch`, `StockFreshness`, repository penyimpanan lokal (SharedPreferences JSON/Room), dan kalkulasi otomatis item terjual berdasarkan pesanan hari ini.

- [ ] **Step 4: Run test to verify it passes**
Run: `JAVA_HOME="/home/lian/Downloads/android-studio/jbr" ./gradlew testDebugUnitTest --tests com.kapos.mobile.feature.stock.StockViewModelTest`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add android/app/src/main/java/com/kapos/mobile/domain/model/StockModels.kt android/app/src/main/java/com/kapos/mobile/core/data/repository/StockRepository.kt android/app/src/main/java/com/kapos/mobile/feature/stock/StockUiState.kt android/app/src/main/java/com/kapos/mobile/feature/stock/StockViewModel.kt android/app/src/test/java/com/kapos/mobile/feature/stock/StockViewModelTest.kt
git commit -m "feat(stock): add StockRepository and StockViewModel with freshness calculation and sold auto-sync"
```

---

### Task 5: Modul Stok Harian — UI Screen (`StockScreen.kt`)

**Files:**
- Create: `android/app/src/main/java/com/kapos/mobile/feature/stock/StockScreen.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/feature/stock/AddStockBatchDialog.kt`

**Interfaces:**
- Consumes: `StockViewModel`, `StockUiState`
- Produces: `StockRoute(viewModel: StockViewModel)`

- [ ] **Step 1: Implement AddStockBatchDialog.kt**
Dialog minimalis untuk memasukkan batch baru stok mentah (jumlah porsi/tusuk, potongan ayam, dan catatan).

- [ ] **Step 2: Implement StockScreen.kt with Jetpack Compose**
Layar pelacak stok dengan:
- Ringkasan kartu metrik: Total Mentah, Total Goreng/Siap Jual, Total Terjual Hari Ini, Sisa Stok.
- Tab Sub: `Sate Taichan (Tusuk)` dan `Ayam Potong (Sayap, Paha, Dada)`.
- List batch dengan badge kesegaran (Segar, Hari ke-2, Hari ke-3, Expired).
- Stepper/input cepat untuk mengubah kuantitas goreng.
- Tombol Tambah Batch Baru.

- [ ] **Step 3: Run full test suite & compile check**
Run: `JAVA_HOME="/home/lian/Downloads/android-studio/jbr" ./gradlew testDebugUnitTest`
Expected: PASS

- [ ] **Step 4: Commit**
```bash
git add android/app/src/main/java/com/kapos/mobile/feature/stock/StockScreen.kt android/app/src/main/java/com/kapos/mobile/feature/stock/AddStockBatchDialog.kt
git commit -m "feat(stock): implement StockScreen UI and batch restock dialog"
```

---

### Task 6: Modul Analisa Finansial & Tracking Progres BEP

**Files:**
- Create: `android/app/src/main/java/com/kapos/mobile/domain/model/AnalyticsModels.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/feature/analytics/AnalyticsUiState.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/feature/analytics/AnalyticsViewModel.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/feature/analytics/AnalyticsScreen.kt`
- Test: `android/app/src/test/java/com/kapos/mobile/feature/analytics/AnalyticsViewModelTest.kt`

**Interfaces:**
- Consumes: `PosRepository`, `SettingsRepository`
- Produces: `AnalyticsRoute(viewModel: AnalyticsViewModel)`

- [ ] **Step 1: Write failing unit test for AnalyticsViewModel**
Test kalkulasi:
- Omzet kotor periode (Hari Ini, 7 Hari, 30 Hari).
- Akumulasi modal bahan baku (COGS / HPP) dari produk terjual.
- Laba Kotor = Omzet - HPP.
- Estimasi Laba Bersih = Laba Kotor - (Beban Tetap Harian * hari).
- Progress BEP % = min(100%, (Omzet / Target BEP) * 100%).

- [ ] **Step 2: Run test to verify it fails**
Run: `JAVA_HOME="/home/lian/Downloads/android-studio/jbr" ./gradlew testDebugUnitTest --tests com.kapos.mobile.feature.analytics.AnalyticsViewModelTest`
Expected: FAIL

- [ ] **Step 3: Implement AnalyticsModels, AnalyticsUiState, and AnalyticsViewModel**
Implementasikan penghitungan HPP berbasis `Product.costPrice`, beban tetap dari `SettingsRepository`, dan kalkulasi progres BEP harian.

- [ ] **Step 4: Implement AnalyticsScreen.kt with Jetpack Compose**
Layar analisa finansial modern:
- Banner Progres BEP: Linear progress bar elegan, persentase capaian, target omzet BEP, dan status motivasi (Kurang Rp X atau BEP Tercapai).
- Kartu Laba Rugi Komprehensif: Omzet, HPP Bahan, Laba Kotor, Beban Operasional, Laba Bersih, Margin Net %.
- Breakdown metode pembayaran (Tunai vs Non-Tunai) dan rata-rata nilai transaksi (AOV).

- [ ] **Step 5: Run tests to verify they pass**
Run: `JAVA_HOME="/home/lian/Downloads/android-studio/jbr" ./gradlew testDebugUnitTest --tests com.kapos.mobile.feature.analytics.AnalyticsViewModelTest`
Expected: PASS

- [ ] **Step 6: Commit**
```bash
git add android/app/src/main/java/com/kapos/mobile/domain/model/AnalyticsModels.kt android/app/src/main/java/com/kapos/mobile/feature/analytics/ android/app/src/test/java/com/kapos/mobile/feature/analytics/
git commit -m "feat(analytics): implement financial analytics and daily BEP progress tracking"
```

---

### Task 7: Integrasi Navigasi, Role Protection & Pengaturan PIN Toko

**Files:**
- Modify: `android/app/src/main/java/com/kapos/mobile/KaPosApplication.kt`
- Modify: `android/app/src/main/java/com/kapos/mobile/core/components/AppBottomNav.kt`
- Modify: `android/app/src/main/java/com/kapos/mobile/ui/KaPosApp.kt`
- Modify: `android/app/src/main/java/com/kapos/mobile/feature/settings/SettingsScreen.kt`

**Interfaces:**
- Consumes: Seluruh ViewModel (Auth, Pos, Stock, Analytics, Settings, Shift, Transactions)
- Produces: Pengalaman aplikasi utuh dengan autentikasi PIN, proteksi menu per role, Quick Lock, dan konfigurasi PIN & BEP di Pengaturan.

- [ ] **Step 1: Register StockRepository in KaPosApplication**
Tambahkan `val stockRepository: StockRepository` di `KaPosApplication.kt`.

- [ ] **Step 2: Update AppBottomNav.kt for dynamic roles**
Sesuaikan item navigasi bawah:
- Jika Kasir: Tab Kasir, Transaksi, Shift.
- Jika Owner: Tab Kasir, Transaksi, Stok, Analisa BEP, Pengaturan.

- [ ] **Step 3: Update KaPosApp.kt with Login route and Quick Lock Header**
- Jika belum login: Navigasi otomatis mengunci di layar `login`.
- Di bar atas kasir: Tambahkan pill role pengguna (`Kasir` / `Owner`), tombol Quick Lock (gembok) untuk kunci cepat, dan tombol Logout.
- Pasang rute navigasi `stock` dan `analytics`.

- [ ] **Step 4: Update SettingsScreen.kt to manage PINs and BEP Targets**
Tambahkan input field untuk mengubah:
- PIN Kasir (default `1111`)
- PIN Owner (default `1234`)
- Target Omzet BEP Harian (default `Rp 1.500.000`)
- Biaya Tetap Harian (default `Rp 350.000`)

- [ ] **Step 5: Run full test suite and verify build**
Run: `JAVA_HOME="/home/lian/Downloads/android-studio/jbr" ./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL (Semua unit test PASS)

- [ ] **Step 6: Commit**
```bash
git add android/app/src/main/java/com/kapos/mobile/KaPosApplication.kt android/app/src/main/java/com/kapos/mobile/core/components/AppBottomNav.kt android/app/src/main/java/com/kapos/mobile/ui/KaPosApp.kt android/app/src/main/java/com/kapos/mobile/feature/settings/SettingsScreen.kt
git commit -m "feat: integrate dynamic role navigation, quick lock terminal, and BEP settings"
```
