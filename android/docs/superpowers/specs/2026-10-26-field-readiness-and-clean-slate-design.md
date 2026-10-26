# Desain Teknis: Kesiapan Lapangan & Pembersihan Data Dummy (Clean Slate)

- **Tanggal**: 2026-10-26
- **Aplikasi**: POS Taichan & Chicken KA (Android)
- **Status**: Disetujui

---

## 1. Latar Belakang & Tujuan
Aplikasi POS KA akan dioperasikan secara nyata (*live production*) di gerai lapangan. Seluruh data tiruan (*dummy data*) yang sebelumnya digunakan untuk pengembangan, pengujian, dan *demo/mocking* harus dihapus secara menyeluruh sehingga aplikasi berada dalam kondisi bersih total (*zero records / clean slate*). Pengguna akan menginput data produk, kategori, resep, bahan baku, dan pelanggan langsung dari lapangan.

Selain itu, untuk perangkat kasir fisik yang telah terpasang aplikasi dan menyimpan basis data pengujian lokal, disediakan fitur **Reset Database (Factory Reset)** yang terproteksi oleh PIN Owner di menu Pengaturan.

---

## 2. Ruang Lingkup Perubahan

### A. Pengosongan Data Awal (Auto-Seed Elimination)
1. **`PosRepository.kt`**:
   - Menghapus pemanggilan `seedInitialDataIfEmpty()`.
   - Mengeliminasi *seeding* otomatis untuk kategori, produk, dan opsi modifier (`PresetModifiers`).
   - Tabel `categories`, `products`, dan `modifiers` di Room DB mengawali aplikasi dalam keadaan kosong (0 record).
2. **`CustomerRepository.kt`**:
   - Menghapus pemanggilan `seedInitialDataIfEmpty()`.
   - Mengeliminasi *seeding* dummy pelanggan (Budi Santoso, Siti Aminah, Ahmad Fauzi, Dewi Lestari).
   - Tabel `customers` mengawali dalam keadaan kosong (0 record).
3. **`InventoryRepository.kt`**:
   - Menghapus pemanggilan `seedInitialDataIfEmpty()`.
   - Mengeliminasi *seeding* dummy bahan baku (cabe, daging ayam fillet, beras, dll.), batch awal "B-INIT-...", dan resep bawaan.
   - Tabel `raw_materials`, `material_batches`, dan `recipes` mengawali dalam keadaan kosong (0 record).
4. **`StockRepository.kt`**:
   - Menghapus fallback batch harian tiruan (`TC-1` dan `CK-1`) saat SharedPreferences kosong.
   - Mengawali `_taichanBatches` dan `_chickenBatches` sebagai list kosong (`emptyList()`).
5. **`SampleData.kt`**:
   - Menghapus / mengosongkan file mock `SampleData.kt`.
   - Memperbarui unit test terkait agar menggunakan mock test fixture independen dan tidak bergantung pada `SampleData`.

---

### B. Fitur Reset Database (Factory Reset) di Pengaturan
1. **Lapisan Akses Data & Repositori**:
   - `AppDatabase`: Menggunakan `clearAllTables()` bawaan Room yang mengeksekusi penghapusan seluruh tabel secara atomik dalam transaksi SQLite.
   - `StockRepository`: Menambahkan fungsi `clearAllBatches()` untuk mereset data `taichan_batches` dan `chicken_batches` di `SharedPreferences`.
   - `SavedOrderRepository`: Menambahkan fungsi `clearAllSavedOrders()` untuk membersihkan draf pesanan tersimpan di `SharedPreferences`.
2. **ViewModel (`SettingsViewModel`)**:
   - Menambahkan fungsi `resetDatabase(pin: String): Result<Unit>`:
     - Memverifikasi PIN input terhadap PIN Owner (`settingsRepository.verifyPin(UserRole.OWNER, pin)`).
     - Jika PIN salah: mengembalikan kegagalan dengan pesan "PIN Owner salah!".
     - Jika PIN benar: menjalankan pembersihan tabel Room, pembersihan SharedPreferences batch stok, dan memancarkan status sukses.
   - Menyediakan state reaktif untuk dialog konfirmasi, status loading reset, dan pesan hasil eksekusi.
3. **Antarmuka Pengguna (`SettingsScreen`)**:
   - Menambahkan kartu bahaya (*Danger Card*) pada seksi **Data & Cadangan**:
     - Judul: "Hapus Seluruh Data (Factory Reset)"
     - Subtitle: "Kosongkan seluruh transaksi, produk, bahan baku, pelanggan & kas"
     - Tombol: "Reset Seluruh Data" (Warna Merah / Danger)
   - Dialog Konfirmasi Interaktif:
     - Judul: "Peringatan Factory Reset"
     - Pesan: "Tindakan ini akan menghapus permanen seluruh data menu, bahan baku, pelanggan, shift, dan riwayat transaksi di perangkat ini. Masukkan PIN Owner untuk melanjutkan."
     - Input field: PIN Owner (sensor sandi / masked)
     - Tombol: "Batal" dan "Hapus Semua Data"

---

### C. Penanganan Kondisi Data Kosong (Empty States)
Memastikan seluruh layar operasional menangani kondisi 0 data dengan ramah tanpa crash:
- Layar POS: Menampilkan pesan instruksi saat katalog produk belum ada.
- Layar Inventaris: Menampilkan pesan instruksi saat belum ada bahan baku terdaftar.
- Layar Pelanggan: Menampilkan pesan instruksi saat belum ada member terdaftar.
- Layar Laporan: Menangani kalkulasi agregat omzet & margin 0 dengan benar.

---

## 3. Rencana Pengujian
1. **Unit Tests**:
   - Menguji verifikasi PIN Owner pada fungsi reset database.
   - Menguji bahwa setelah inisialisasi repositori baru (`PosRepository`, `CustomerRepository`, `InventoryRepository`), jumlah entitas di basis data adalah 0.
   - Menjalankan seluruh test suite yang ada (`./gradlew testDebugUnitTest`) untuk memastikan tidak ada regresi.
2. **Pengujian Fungsional**:
   - Verifikasi bahwa input PIN salah pada dialog reset ditolak dengan pesan error.
   - Verifikasi bahwa input PIN benar mengosongkan seluruh tabel Room dan stok harian.
