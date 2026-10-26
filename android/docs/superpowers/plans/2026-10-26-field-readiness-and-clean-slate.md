# Field Readiness and Clean Slate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Menghapus seluruh data dummy (*clean slate*) pada aplikasi POS KA dan menyediakan fitur *Factory Reset* terproteksi PIN Owner pada menu Pengaturan untuk memastikan kesiapan 100% penggunaan operasional lapangan.

**Architecture:** Mematikan seluruh mekanisme *auto-seeding* pada lapisan repositori (`PosRepository`, `CustomerRepository`, `InventoryRepository`, `StockRepository`), menghapus `SampleData.kt`, serta memperluas `SettingsViewModel` dan `SettingsScreen` dengan kapabilitas *Factory Reset* atomik (membersihkan Room DB, SharedPreferences stok harian, dan draf pesanan) yang dilindungi oleh validasi PIN Owner terenkripsi.

**Tech Stack:** Kotlin, Jetpack Compose, Room Database (SQLite), SharedPreferences, Coroutines/Flow, JUnit4.

## Global Constraints
- Seluruh tes unit harus lulus tanpa kegagalan: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest`.
- Proyek harus berhasil dikompilasi hingga menghasilkan APK debug: `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug`.
- PIN verifikasi harus menggunakan `settingsRepository.verifyPin(UserRole.OWNER, pin)` yang menggunakan perbandingan hash SHA-256 constant-time.
- Tidak boleh meninggalkan file dummy atau data inisial yang tidak diinginkan di branch produksi.

---

### Task 1: Hapus Auto-Seeding dari Seluruh Repositori dan Hapus SampleData

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/core/data/repository/PosRepository.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/repository/CustomerRepository.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/repository/InventoryRepository.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/repository/StockRepository.kt`
- Delete: `app/src/main/java/com/kapos/mobile/core/data/mock/SampleData.kt`
- Test: `app/src/test/java/com/kapos/mobile/core/data/repository/CleanSlateRepositoryTest.kt`

**Interfaces:**
- Produces: Repositori yang mengawali basis data dalam kondisi murni kosong (0 kategori, 0 produk, 0 modifier, 0 pelanggan, 0 bahan baku, 0 batch stok harian).

- [ ] **Step 1: Tulis unit test untuk memverifikasi repositori bersih tanpa auto-seed**
Buat `app/src/test/java/com/kapos/mobile/core/data/repository/CleanSlateRepositoryTest.kt`:
```kotlin
package com.kapos.mobile.core.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanSlateRepositoryTest {
    @Test
    fun stockRepository_startsEmptyWhenNoPreferences() {
        val repo = StockRepository(context = null)
        assertTrue("Taichan batches should be empty by default", repo.taichanBatches.value.isEmpty())
        assertTrue("Chicken batches should be empty by default", repo.chickenBatches.value.isEmpty())
    }
}
```

- [ ] **Step 2: Jalankan test untuk memverifikasi test gagal karena StockRepository masih memiliki dummy batch**
Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests com.kapos.mobile.core.data.repository.CleanSlateRepositoryTest`
Expected: FAIL karena `StockRepository(null)` saat ini mengisi TC-1 dan CK-1.

- [ ] **Step 3: Hapus auto-seeding di StockRepository, PosRepository, CustomerRepository, dan InventoryRepository**
1. Di `StockRepository.kt`:
Ubah `loadData()` agar saat `prefs == null` tidak lagi mengisi `TC-1` dan `CK-1`, melainkan membiarkan `_taichanBatches.value = emptyList()` dan `_chickenBatches.value = emptyList()`.
2. Di `PosRepository.kt`:
Hapus pemanggilan `seedInitialDataIfEmpty()` di blok `init` dan hapus implementasi `seedInitialDataIfEmpty()`.
3. Di `CustomerRepository.kt`:
Hapus pemanggilan `seedInitialDataIfEmpty()` di blok `init` dan hapus implementasi `seedInitialDataIfEmpty()`.
4. Di `InventoryRepository.kt`:
Hapus pemanggilan `seedInitialDataIfEmpty()` di blok `init` dan hapus implementasi `seedInitialDataIfEmpty()`.
5. Hapus file `app/src/main/java/com/kapos/mobile/core/data/mock/SampleData.kt` serta impor terkait.

- [ ] **Step 4: Jalankan unit test untuk memastikan test lulus**
Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL (semua test unit lulus).

- [ ] **Step 5: Commit perubahan Task 1**
```bash
git add app/src/main/java/com/kapos/mobile/core/data/repository/ app/src/test/java/com/kapos/mobile/core/data/repository/CleanSlateRepositoryTest.kt
git rm app/src/main/java/com/kapos/mobile/core/data/mock/SampleData.kt
git commit -m "feat: eliminate auto-seeding and purge SampleData mock"
```

---

### Task 2: Tambahkan Metode Pembersihan Data di StockRepository & SavedOrderRepository

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/core/data/repository/StockRepository.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/repository/SavedOrderRepository.kt`
- Test: `app/src/test/java/com/kapos/mobile/core/data/repository/StockRepositoryClearTest.kt`

**Interfaces:**
- Produces: `StockRepository.clearAllBatches()` dan `SavedOrderRepository.clearAllSavedOrders()` (alias ke `clearAll()`).

- [ ] **Step 1: Tulis unit test untuk StockRepository.clearAllBatches()**
Buat `app/src/test/java/com/kapos/mobile/core/data/repository/StockRepositoryClearTest.kt`:
```kotlin
package com.kapos.mobile.core.data.repository

import com.kapos.mobile.domain.model.ChickenPart
import org.junit.Assert.assertTrue
import org.junit.Test

class StockRepositoryClearTest {
    @Test
    fun clearAllBatches_emptiesBatchesStateFlow() {
        val repo = StockRepository(context = null)
        repo.addTaichanBatch(100, "Test")
        repo.addChickenBatch(ChickenPart.PAHA, 20, "Test")
        repo.clearAllBatches()

        assertTrue(repo.taichanBatches.value.isEmpty())
        assertTrue(repo.chickenBatches.value.isEmpty())
    }
}
```

- [ ] **Step 2: Jalankan test untuk memverifikasi kompilasi gagal karena clearAllBatches belum ada**
Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests com.kapos.mobile.core.data.repository.StockRepositoryClearTest`
Expected: FAIL ("unresolved reference: clearAllBatches").

- [ ] **Step 3: Implementasikan clearAllBatches() di StockRepository**
Tambahkan di `StockRepository.kt`:
```kotlin
fun clearAllBatches() {
    _taichanBatches.value = emptyList()
    _chickenBatches.value = emptyList()
    prefs?.edit()?.remove("taichan_batches")?.remove("chicken_batches")?.apply()
}
```
Dan pastikan `SavedOrderRepository.kt` memiliki metode `clearAllSavedOrders()`:
```kotlin
fun clearAllSavedOrders() {
    clearAll()
}
```

- [ ] **Step 4: Jalankan test untuk memastikan test lulus**
Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests com.kapos.mobile.core.data.repository.StockRepositoryClearTest`
Expected: PASS.

- [ ] **Step 5: Commit perubahan Task 2**
```bash
git add app/src/main/java/com/kapos/mobile/core/data/repository/StockRepository.kt app/src/main/java/com/kapos/mobile/core/data/repository/SavedOrderRepository.kt app/src/test/java/com/kapos/mobile/core/data/repository/StockRepositoryClearTest.kt
git commit -m "feat: add clearAllBatches and clearAllSavedOrders repository methods"
```

---

### Task 3: Implementasikan Logika Factory Reset di SettingsViewModel

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/feature/settings/SettingsUiState.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/kapos/mobile/ui/KaPosApp.kt`
- Test: `app/src/test/java/com/kapos/mobile/feature/settings/SettingsViewModelResetTest.kt`

**Interfaces:**
- Consumes:
  - `database.clearAllTables()`
  - `stockRepository.clearAllBatches()`
  - `savedOrderRepository.clearAll()`
  - `settingsRepository.verifyPin(UserRole.OWNER, pin)`
- Produces:
  - `SettingsUiState.isResettingDatabase: Boolean`
  - `SettingsUiState.resetDatabaseStatusMessage: String?`
  - `SettingsUiState.isResetSuccess: Boolean?`
  - `SettingsViewModel.resetDatabase(ownerPin: String, onComplete: (Boolean) -> Unit)`

- [ ] **Step 1: Tulis unit test untuk verifikasi PIN Owner pada Factory Reset**
Buat `app/src/test/java/com/kapos/mobile/feature/settings/SettingsViewModelResetTest.kt`:
```kotlin
package com.kapos.mobile.feature.settings

import com.kapos.mobile.core.data.repository.SettingsRepository
import com.kapos.mobile.domain.model.UserRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsViewModelResetTest {
    @Test
    fun ownerPinVerification_validatesCorrectly() {
        val settingsRepo = SettingsRepository(context = null)
        // Default owner pin adalah 1234
        assertTrue(settingsRepo.verifyPin(UserRole.OWNER, "1234"))
        assertFalse(settingsRepo.verifyPin(UserRole.OWNER, "9999"))
        assertFalse(settingsRepo.verifyPin(UserRole.OWNER, "1111")) // PIN kasir bukan PIN owner
    }
}
```

- [ ] **Step 2: Jalankan test untuk memverifikasi logika PIN**
Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests com.kapos.mobile.feature.settings.SettingsViewModelResetTest`
Expected: PASS.

- [ ] **Step 3: Tambahkan field state ke SettingsUiState dan logika resetDatabase ke SettingsViewModel**
1. Di `SettingsUiState.kt`, tambahkan:
```kotlin
val isResettingDatabase: Boolean = false,
val resetDatabaseStatusMessage: String? = null,
val isResetSuccess: Boolean? = null
```
2. Di `SettingsViewModel.kt`:
Tambahkan parameter konstruktor `stockRepository: StockRepository? = null` dan `savedOrderRepository: SavedOrderRepository? = null`.
Tambahkan fungsi:
```kotlin
fun resetDatabase(ownerPin: String, onComplete: (Boolean) -> Unit = {}) {
    if (!settingsRepository.verifyPin(UserRole.OWNER, ownerPin)) {
        _uiState.update {
            it.copy(
                isResettingDatabase = false,
                isResetSuccess = false,
                resetDatabaseStatusMessage = "PIN Owner salah! Reset database dibatalkan."
            )
        }
        onComplete(false)
        return
    }

    _uiState.update { it.copy(isResettingDatabase = true, resetDatabaseStatusMessage = null) }
    viewModelScope.launch {
        try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                database.clearAllTables()
                stockRepository?.clearAllBatches()
                savedOrderRepository?.clearAll()
            }
            _uiState.update {
                it.copy(
                    isResettingDatabase = false,
                    isResetSuccess = true,
                    resetDatabaseStatusMessage = "Database berhasil dikosongkan total! Seluruh data transaksi, menu & bahan telah bersih."
                )
            }
            onComplete(true)
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isResettingDatabase = false,
                    isResetSuccess = false,
                    resetDatabaseStatusMessage = "Gagal mengosongkan database: ${e.message}"
                )
            }
            onComplete(false)
        }
    }
}
```
3. Di `KaPosApp.kt`, perbarui instansiasi `settingsViewModel` untuk meneruskan `stockRepository = app.stockRepository` dan `savedOrderRepository = app.savedOrderRepository`.

- [ ] **Step 4: Jalankan seluruh test unit**
Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit perubahan Task 3**
```bash
git add app/src/main/java/com/kapos/mobile/feature/settings/ app/src/main/java/com/kapos/mobile/ui/KaPosApp.kt app/src/test/java/com/kapos/mobile/feature/settings/SettingsViewModelResetTest.kt
git commit -m "feat: implement Factory Reset logic in SettingsViewModel with Owner PIN"
```

---

### Task 4: Tambahkan Antarmuka Factory Reset & Dialog Konfirmasi ke SettingsScreen

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/feature/settings/SettingsScreen.kt`

**Interfaces:**
- Menampilkan kartu bahaya di seksi Data & Cadangan dengan tombol "Reset Seluruh Data (Factory Reset)".
- Menampilkan dialog konfirmasi dengan input sandi PIN Owner sebelum memanggil `onResetDatabase(pin)`.

- [ ] **Step 1: Perbarui signature SettingsRoute & SettingsScreen**
Tambahkan lambda callback `onResetDatabase: (String, (Boolean) -> Unit) -> Unit` pada `SettingsRoute` dan `SettingsScreen`. Hubungkan ke `viewModel::resetDatabase`.

- [ ] **Step 2: Tambahkan UI Kartu Factory Reset dan AlertDialog di SettingsScreen.kt**
Di dalam seksi `ExpandableSection` untuk Data & Cadangan:
1. Tambahkan tombol / banner berdesain merah (*Danger Card*):
   - Menjelaskan konsekuensi: "Menghapus permanen seluruh transaksi, menu, bahan baku, pelanggan, dan kasir."
   - Tombol bahaya: "Reset Seluruh Data (Factory Reset)".
2. State dialog: `var showResetDialog by remember { mutableStateOf(false) }`, `var pinInput by remember { mutableStateOf("") }`.
3. AlertDialog dengan `OutlinedTextField` visual transformation `PasswordVisualTransformation` untuk input PIN Owner.
4. Ketika konfirmasi diklik, panggil `onResetDatabase(pinInput) { success -> if (success) { showResetDialog = false; pinInput = "" } }`.

- [ ] **Step 3: Uji kompilasi debug**
Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit perubahan Task 4**
```bash
git add app/src/main/java/com/kapos/mobile/feature/settings/SettingsScreen.kt
git commit -m "feat: add Factory Reset button and PIN confirmation dialog in SettingsScreen"
```

---

### Task 5: Validasi Komprehensif Seluruh Sistem & Pembuatan APK Debug

**Files:**
- Build & Verification: Seluruh modul aplikasi POS KA.

- [ ] **Step 1: Jalankan seluruh test suite unit**
Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL (seluruh test suite lulus).

- [ ] **Step 2: Kompilasi dan bangun APK debug Android**
Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL (`app-debug.apk` berhasil terbuat di `app/build/outputs/apk/debug/`).

- [ ] **Step 3: Commit akhir dan verifikasi git status**
```bash
git status
```
Expected: Clean working tree.
