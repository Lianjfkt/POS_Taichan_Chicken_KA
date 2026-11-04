# Spesifikasi Desain: Fitur Input Transaksi Susulan (Khusus Owner)

## 1. Ringkasan Fitur
Fitur **Transaksi Susulan (Backdated Order)** memungkinkan pemilik toko (**Owner**) untuk menginput transaksi penjualan yang terlewat atau lupa dicatat di masa lampau (misalnya transaksi manual saat listrik padam atau kendala teknis).

Fitur ini:
* **Khusus Owner**: Terlindungi oleh hak akses `UserRole.OWNER` atau otorisasi **PIN Owner**.
* **Menjaga Integritas Data (Immutable Audit Trail)**: Menggunakan pola pencatatan nota baru bertanggal lampau (*backdated*) alih-alih mengedit data transaksi yang sudah ditutup (*in-place edit*), sehingga sinkronisasi Supabase dan audit pembukuan tetap konsisten.
* **Otomasi Stok & Laporan**: Memotong stok bahan baku (BOM FIFO) secara otomatis dan langsung teragregasi ke dalam laporan keuangan harian pada tanggal lampau yang dipilih.

---

## 2. Alur Pengguna (User Journey)

```
[Halaman Riwayat Transaksi]
         │
         ▼
[Klik Tombol "+ Susulan"]
         │
         ├── Sesi Aktif == OWNER ──────────────┐
         │                                     │
         └── Sesi Aktif == CASHIER             │
                  │                            │
                  ▼                            │
         [Popup PIN Owner]                     │
                  │ (PIN Valid)                │
                  └────────────────────────────┤
                                               ▼
                                 [Dialog Transaksi Susulan]
                                 ├── 1. Pilih Tanggal & Waktu (Masa Lampau)
                                 ├── 2. Pilih Menu & Kuantitas (+ / -)
                                 ├── 3. Pilih Metode Bayar (CASH / QRIS / TRANSFER)
                                 └── 4. Isi Alasan / Catatan (Wajib)
                                               │
                                               ▼
                                  [Simpan Transaksi Susulan]
                                               │
                                               ├── Buat Order dengan createdAt = Tanggal Lampau
                                               ├── Potong Stok BOM Bahan Baku
                                               ├── Simpan ke Room DB (Offline SSOT)
                                               ├── Push ke Supabase (PENDING_SYNC)
                                               └── Refresh List & Beri Badge [Susulan]
```

---

## 3. Komponen & Perubahan Teknis

### A. UI Layer
1. **`BackdatedOrderDialog.kt` (`feature/transactions/`)**
   * Dialog modal Jetpack Compose berisikan:
     * Header judul dan peringatan otorisasi owner.
     * Field pemilihan Tanggal (*DatePicker*) dan Waktu (*TimePicker*). Batas maksimum adalah waktu sekarang (`System.currentTimeMillis()`).
     * Search bar & daftar item produk aktif yang bisa dipilih dan diatur kuantitasnya (`+` / `-`).
     * Pilihan chip metode pembayaran (`CASH`, `QRIS`, `TRANSFER`).
     * Kolom teks catatan / alasan transaksi susulan (wajib diisi untuk keperluan audit).
     * Ringkasan subtotal, total belanja, dan tombol aksi "Batal" & "Simpan Transaksi".

2. **Perubahan pada `TransactionsScreen.kt`**
   * Pada `Top Bar`, di samping tombol `Sync`, ditambahkan tombol aksi `+ Susulan` dengan styling warna aksen `SurfaceTint` / Amber.
   * Pada item daftar transaksi (`TransactionItemCard`), jika `order.notes` mengandung tag `[Susulan]`, tampilkan badge khusus berwarna Amber: `[Susulan]`.
   * Integrasi modal dialog `BackdatedOrderDialog` dan dialog PIN otorisasi Owner.

### B. ViewModel & State Layer
1. **`TransactionsUiState.kt`**
   * `isOwner: Boolean = false`
   * `showBackdatedOrderDialog: Boolean = false`
   * `showOwnerPinDialog: Boolean = false`
   * `ownerPinInput: String = ""`
   * `ownerPinError: String? = null`
   * `availableProducts: List<Product> = emptyList()`
   * `isSubmittingBackdated: Boolean = false`
   * `actionSuccessMessage: String? = null`

2. **`TransactionsViewModel.kt`**
   * Mengamati `settingsRepository.settingsFlow` untuk mendeteksi `currentSession.role == UserRole.OWNER`.
   * Mengamati `repository.observeProducts()` untuk mendapatkan katalog produk yang tersedia.
   * `fun onOpenBackdatedDialog()`: Jika user adalah Owner, langsung buka `showBackdatedOrderDialog`. Jika Kasir, buka dialog otorisasi `showOwnerPinDialog`.
   * `fun onVerifyOwnerPin()`: Memverifikasi PIN Owner via `settingsRepository.verifyPin(UserRole.OWNER, pin)`.
   * `fun submitBackdatedOrder(...)`:
     * Generate `orderNo` menggunakan `generateOrderNo(Date(timestamp))`.
     * Bentuk objek `Order` dengan `createdAt = timestamp`, `paidAt = timestamp`, `cashierName = "Owner (Koreksi)"`, `notes = "[Susulan] $reason"`, dan `paymentStatus = PaymentStatus.PAID`.
     * Panggil `repository.submitOrder(order, cartItems)`.

### C. Data & Domain Layer
* Memanfaatkan method `PosRepository.submitOrder(order, items)` yang sudah ada.
* Pemotongan bahan baku (BOM) otomatis dijalankan oleh `inventoryRepository.deductStockForOrderItems(items)`.
* Tidak memerlukan migrasi Room DB atau perubahan skema tabel Supabase karena tag `[Susulan]` disimpan di dalam field `notes` yang sudah ada.

---

## 4. Rencana Pengujian (Testing & Verification)
1. **Otorisasi Role**:
   * Login sebagai Kasir -> klik `+ Susulan` -> muncul popup PIN Owner. Masukkan PIN salah -> tolak; PIN benar -> dialog terbuka.
   * Login sebagai Owner -> klik `+ Susulan` -> dialog langsung terbuka tanpa meminta PIN lagi.
2. **Validasi Input**:
   * Tanggal tidak boleh di masa depan (*future date*).
   * Produk minimal 1 item dengan kuantitas > 0.
   * Catatan alasan wajib diisi.
3. **Integritas Data & Laporan**:
   * Buat transaksi susulan bertanggal 2 hari lalu.
   * Buka menu *Riwayat Transaksi* -> cek apakah nota baru muncul dengan badge `[Susulan]` dan tanggal yang sesuai.
   * Buka menu *Laporan* (`ReportScreen`) -> filter ke tanggal 2 hari lalu -> pastikan omzet dan item penjualan tanggal tersebut bertambah.
   * Periksa stok bahan baku -> pastikan stok berkurang sesuai resep menu.
