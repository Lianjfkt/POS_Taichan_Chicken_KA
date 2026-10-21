# Rencana Implementasi Peningkatan Lanjutan KA POS Mobile v1.2.0

> **Untuk Pekerja Agentik:** Gunakan sub-skill `superpowers:executing-plans` atau `superpowers:subagent-driven-development` untuk mengeksekusi rencana ini secara bertahap tugas demi tugas. Setiap langkah menggunakan format checkbox (`- [ ]`).

**Tujuan:** Mengembangkan 7 fitur strategis pada KA POS Mobile: Add-ons & Pelengkap Fleksibel, Proteksi Menu Habis (Sold Out), Backup & Restore JSON, Pembayaran Ganda (Split Payment), Mode Tablet 2-Panel, Auto-Lock Inactivity, dan Modul Marketing Promo.

**Arsitektur:** Clean Architecture Android native (Kotlin + Jetpack Compose, Room Database v5, StateFlow, Coroutines) dengan modularisasi komponen dan isolasi domain model.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android Room Database, Kotlinx Serialization, Android Storage Access Framework (SAF), ESC/POS Thermal Printing.

---

## Daftar Fitur yang Disepakati (Scope)

1. ❌ **Fitur Dapur:** *Dikeluarkan dari cakupan (tidak diperlukan)*.
2. ✅ **Add-ons & Pelengkap Fleksibel:** Variasi sambal, sarung tangan plastik, sedotan, ekstra sambal, topping, packaging yang bisa dipasang ke menu mana saja.
3. ✅ **Proteksi Menu Habis (Sold Out Indicator):** Deteksi otomatis bahan baku habis / stok kosong dan penguncian kartu menu.
4. ✅ **Backup & Restore Database JSON:** Ekspor cadangan file JSON dan impor pemulihan database Room di menu Pengaturan.
5. ✅ **Pembayaran Ganda (Split Payment):** Pembayaran kombinasi Tunai + QRIS / Transfer dengan kalkulasi kembalian akurat. *(Fitur kirim struk WA dilewati sesuai permintaan)*.
6. ✅ **Mode Tablet / Layar Landscape (2-Panel POS):** Tampilan adaptif split screen (Kiri: Katalog & Kategori, Kanan: Keranjang permanen).
7. ✅ **Auto-Lock Inactivity Timer:** Penguncian otomatis ke layar PIN jika kasir tidak menyentuh layar selama waktu tertentu (1, 2, atau 5 menit).
8. ✅ **Modul Marketing Promo:** Sistem promo diskon nominal, persentase, minimal belanja, dan periode aktif yang memotong total checkout secara otomatis.

---

## Rincian File yang Dibuat & Dimodifikasi

- **Task 1: Add-ons & Pelengkap Fleksibel**
  - Buat: `android/app/src/main/java/com/kapos/mobile/domain/model/ModifierModels.kt`
  - Buat: `android/app/src/main/java/com/kapos/mobile/core/data/local/entity/ModifierEntity.kt`
  - Buat: `android/app/src/main/java/com/kapos/mobile/core/data/local/dao/ModifierDao.kt`
  - Modifikasi: `android/app/src/main/java/com/kapos/mobile/domain/model/Models.kt` (CartItem & OrderItem)
  - Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/pos/ModifierDialog.kt`
  - Modifikasi: `android/app/src/main/java/com/kapos/mobile/core/data/local/AppDatabase.kt`
  - Test: `android/app/src/test/java/com/kapos/mobile/feature/pos/CartModifierTest.kt`

- **Task 2: Proteksi Stok Habis (Sold Out Indicator)**
  - Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/pos/PosUiState.kt`
  - Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/pos/PosViewModel.kt`
  - Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/pos/ProductCard.kt`
  - Test: `android/app/src/test/java/com/kapos/mobile/feature/pos/SoldOutProtectionTest.kt`

- **Task 3: Backup & Restore Database JSON**
  - Buat: `android/app/src/main/java/com/kapos/mobile/core/utils/DatabaseBackupManager.kt`
  - Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/settings/SettingsUiState.kt`
  - Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/settings/SettingsViewModel.kt`
  - Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/settings/SettingsScreen.kt`
  - Test: `android/app/src/test/java/com/kapos/mobile/core/utils/DatabaseBackupManagerTest.kt`

- **Task 4: Pembayaran Ganda (Split Payment)**
  - Modifikasi: `android/app/src/main/java/com/kapos/mobile/domain/model/Models.kt` (Order split fields)
  - Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/checkout/CheckoutUiState.kt`
  - Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/checkout/CheckoutViewModel.kt`
  - Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/checkout/ConfirmSaleScreen.kt`
  - Modifikasi: `android/app/src/main/java/com/kapos/mobile/core/printer/EscPosBuilder.kt`
  - Test: `android/app/src/test/java/com/kapos/mobile/feature/checkout/SplitPaymentCalculationTest.kt`

- **Task 5: Mode Tablet / Landscape (2-Panel POS Layout)**
  - Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/pos/PosScreen.kt`
  - Modifikasi: `android/app/src/main/java/com/kapos/mobile/core/components/AppBottomNav.kt`

- **Task 6: Auto-Lock Inactivity Timer**
  - Modifikasi: `android/app/src/main/java/com/kapos/mobile/core/data/repository/SettingsRepository.kt`
  - Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/settings/SettingsScreen.kt`
  - Modifikasi: `android/app/src/main/java/com/kapos/mobile/ui/KaPosApp.kt`
  - Test: `android/app/src/test/java/com/kapos/mobile/core/data/repository/AutoLockSettingsTest.kt`

- **Task 7: Modul Marketing Promo & Diskon Otomatis**
  - Buat: `android/app/src/main/java/com/kapos/mobile/domain/model/PromotionModels.kt`
  - Buat: `android/app/src/main/java/com/kapos/mobile/core/data/local/entity/PromotionEntity.kt`
  - Buat: `android/app/src/main/java/com/kapos/mobile/core/data/local/dao/PromotionDao.kt`
  - Buat: `android/app/src/main/java/com/kapos/mobile/core/data/repository/PromotionRepository.kt`
  - Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/checkout/CheckoutUiState.kt`
  - Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/checkout/ConfirmSaleScreen.kt`
  - Test: `android/app/src/test/java/com/kapos/mobile/feature/checkout/PromotionDiscountTest.kt`

---

## Rincian Tugas Langkah Demi Langkah

### Task 1: Add-ons & Pelengkap Fleksibel (Sambal, Sedotan, Sarung Tangan, Ekstra)
**File:**
- Buat: `android/app/src/main/java/com/kapos/mobile/domain/model/ModifierModels.kt`
- Buat: `android/app/src/main/java/com/kapos/mobile/core/data/local/entity/ModifierEntity.kt`
- Buat: `android/app/src/main/java/com/kapos/mobile/core/data/local/dao/ModifierDao.kt`
- Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/pos/ModifierDialog.kt`
- Test: `android/app/src/test/java/com/kapos/mobile/feature/pos/CartModifierTest.kt`

- [x] **Langkah 1.1:** Buat model `ModifierOption` (id, name, price, category: SAMBAL, COMPLIMENTARY, PACKAGING, EXTRA) dan unit test perhitungan subtotal item keranjang jika ada add-ons berbayar.
- [x] **Langkah 1.2:** Perbarui `CartItem` agar memuat `selectedModifiers: List<ModifierOption>` dan menghitung `subtotal = (product.price + selectedModifiers.sumOf { it.price }) * quantity`.
- [x] **Langkah 1.3:** Tampilkan pilihan variasi sambal, sarung tangan plastik, sedotan, dan topping ekstra pada `ModifierDialog.kt` dengan selektor yang fleksibel untuk setiap item menu.
- [x] **Langkah 1.4:** Jalankan unit test `CartModifierTest` dan pastikan lulus.

---

### Task 2: Indikator & Proteksi Stok Habis (Sold Out Protection)
**File:**
- Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/pos/PosUiState.kt`
- Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/pos/PosViewModel.kt`
- Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/pos/ProductCard.kt`
- Test: `android/app/src/test/java/com/kapos/mobile/feature/pos/SoldOutProtectionTest.kt`

- [x] **Langkah 2.1:** Buat unit test `SoldOutProtectionTest` untuk memverifikasi produk yang bahan bakunya kosong atau stok harian habis ditandai `isSoldOut = true`.
- [x] **Langkah 2.2:** Di `PosViewModel`, hitung ketersediaan tiap produk berdasarkan stok bahan baku dan batch sisa harian.
- [x] **Langkah 2.3:** Di `ProductCard.kt`, berikan overlay redup (alpha 0.55), badge merah mencolok *"HABIS"*, dan nonaktifkan klik jika item habis.
- [x] **Langkah 2.4:** Jalankan unit test dan verifikasi.

---

### Task 3: Fitur Backup & Restore Database JSON
**File:**
- Buat: `android/app/src/main/java/com/kapos/mobile/core/utils/DatabaseBackupManager.kt`
- Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/settings/SettingsViewModel.kt`
- Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/settings/SettingsScreen.kt`
- Test: `android/app/src/test/java/com/kapos/mobile/core/utils/DatabaseBackupManagerTest.kt`

- [x] **Langkah 3.1:** Buat unit test ekspor dan impor payload JSON database mencakup pesanan, pelanggan, menu, bahan baku, dan riwayat shift.
- [x] **Langkah 3.2:** Implementasikan `DatabaseBackupManager` untuk serialisasi seluruh entitas Room DB ke JSON string dan sebaliknya.
- [x] **Langkah 3.3:** Di `SettingsScreen.kt`, tambahkan kartu *"Cadangan & Pemulihan Data"* dengan tombol Ekspor JSON (Share/Download) dan tombol Impor JSON (File Picker).
- [x] **Langkah 3.4:** Jalankan unit test `DatabaseBackupManagerTest`.

---

### Task 4: Pembayaran Ganda (Split Payment: Tunai + QRIS/Transfer)
**File:**
- Modifikasi: `android/app/src/main/java/com/kapos/mobile/domain/model/Models.kt`
- Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/checkout/CheckoutUiState.kt`
- Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/checkout/ConfirmSaleScreen.kt`
- Modifikasi: `android/app/src/main/java/com/kapos/mobile/core/printer/EscPosBuilder.kt`
- Test: `android/app/src/test/java/com/kapos/mobile/feature/checkout/SplitPaymentCalculationTest.kt`

- [x] **Langkah 4.1:** Buat unit test kalkulasi split payment (misal: Tagihan Rp 75.000, Tunai Rp 50.000, QRIS Rp 25.000 -> Sisa bayar Rp 0).
- [x] **Langkah 4.2:** Tambahkan dukungan `isSplitPayment`, `splitCashGiven`, `splitNonCashAmount`, dan `splitNonCashMethod` pada `CheckoutUiState`.
- [x] **Langkah 4.3:** Di `ConfirmSaleScreen.kt`, sediakan toggle sakelar *"Pembayaran Ganda (Split)"* dengan input nominal tunai dan sisa otomatis dialihkan ke QRIS/Transfer.
- [x] **Langkah 4.4:** Perbarui cetak struk thermal di `EscPosBuilder.kt` agar mencetak rincian split payment jika ada.
- [x] **Langkah 4.5:** Jalankan unit test `SplitPaymentCalculationTest`.

---

### Task 5: Mode Tablet / Landscape (2-Panel Velocity Layout)
**File:**
- Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/pos/PosScreen.kt`

- [x] **Langkah 5.1:** Deteksi orientasi landscape atau lebar layar tablet (`maxWidth >= 720.dp` via `BoxWithConstraints`).
- [x] **Langkah 5.2:** Jika layar tablet / landscape:
  - Panel Kiri (bobot 60%): Kategori, Search Bar, dan Grid Katalog Produk.
  - Panel Kanan (bobot 40%): Ringkasan Keranjang Belanja Sticky permanen lengkap dengan tombol aksi Checkout langsung tanpa perlu modal sheet.
- [x] **Langkah 5.3:** Tetap pertahankan layout 1 kolom dengan bottom cart jika di HP mode portrait biasa.

---

### Task 6: Auto-Lock Inactivity Timer
**File:**
- Modifikasi: `android/app/src/main/java/com/kapos/mobile/core/data/repository/SettingsRepository.kt`
- Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/settings/SettingsScreen.kt`
- Modifikasi: `android/app/src/main/java/com/kapos/mobile/ui/KaPosApp.kt`
- Test: `android/app/src/test/java/com/kapos/mobile/core/data/repository/AutoLockSettingsTest.kt`

- [x] **Langkah 6.1:** Tambahkan opsi pengaturan `autoLockMinutes: Int` (0 = Mati, 1 menit, 2 menit, 5 menit) di `AppSettings`.
- [x] **Langkah 6.2:** Tambahkan dropdown pilihan waktu auto-lock di layar Pengaturan Keamanan.
- [x] **Langkah 6.3:** Di root container `KaPosApp.kt`, pasang pointer/touch detector yang mereset timer inaktivitas. Jika waktu habis, aplikasi otomatis kembali ke mode PIN terkunci (`isLoggedIn = false`).
- [x] **Langkah 6.4:** Jalankan unit test pengaturan auto-lock.

---

### Task 7: Modul Marketing Promo & Diskon Otomatis
**File:**
- Buat: `android/app/src/main/java/com/kapos/mobile/domain/model/PromotionModels.kt`
- Buat: `android/app/src/main/java/com/kapos/mobile/core/data/local/entity/PromotionEntity.kt`
- Buat: `android/app/src/main/java/com/kapos/mobile/core/data/local/dao/PromotionDao.kt`
- Buat: `android/app/src/main/java/com/kapos/mobile/core/data/repository/PromotionRepository.kt`
- Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/checkout/CheckoutUiState.kt`
- Modifikasi: `android/app/src/main/java/com/kapos/mobile/feature/checkout/ConfirmSaleScreen.kt`
- Test: `android/app/src/test/java/com/kapos/mobile/feature/checkout/PromotionDiscountTest.kt`

- [x] **Langkah 7.1:** Buat data model `Promotion` (diskon persen/nominal, minimum belanja, kupon promo, status aktif).
- [x] **Langkah 7.2:** Buat Room entity `PromotionEntity` dan `PromotionRepository` dengan seeding awal beberapa promo menarik (misal: *"PROMO GAJIAN: Diskon 10% Min. Belanja 50rb"*, *"PAKET HEMAT: Potongan Rp 5.000"*).
- [x] **Langkah 7.3:** Di layar konfirmasi checkout, tambahkan selektor *"Pilih Kupon / Promo Aktif"* yang otomatis memotong total belanja jika syarat terpenuhi.
- [x] **Langkah 7.4:** Jalankan unit test `PromotionDiscountTest`.

---

## Verifikasi Akhir
- [x] Eksekusi build unit test: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest`
- [x] Eksekusi build APK: `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug`
- [x] Bump versi ke `v1.2.0` (Build 3) dan salin APK ke root folder.
