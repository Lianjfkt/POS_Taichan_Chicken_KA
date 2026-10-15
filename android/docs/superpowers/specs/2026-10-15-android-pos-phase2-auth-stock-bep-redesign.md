# Spec Desain: KA POS Android — Fase 2 (Redesign Elegan, Login Multi-Role, Stok Harian & Analisa BEP)

**Tanggal:** 2026-10-15  
**Status:** Approved  
**Aplikasi:** KA POS Native Android (Kotlin + Jetpack Compose)  
**Lingkup:** Paritas Fitur Lengkap Web App (Role Separation Kasir vs Owner, Redesign Elegan Bebas Emoji Slop, Modul Stok Harian, dan Tracking Progres Finansial Titik Impas/BEP)

---

## 1. Latar Belakang & Tujuan

Aplikasi KA POS Android Fase 1 telah berhasil mengimplementasikan fungsionalitas inti kasir, keranjang, kustomisasi menu/pedas, diskon, tahan order, dan pencetakan struk thermal ESC/POS. Namun, aplikasi masih memiliki keterbatasan:
1. Belum ada pemisah hak akses antara Kasir dan Owner (siapa pun dapat melihat ringkasan omzet dan mengubah pengaturan toko).
2. Tampilan kartu menu dan dialog masih menggunakan emoji dan kotak pastel yang kurang elegan bagi POS profesional.
3. Belum tersedia modul pelacak stok harian (*Daily Stock Tracker* untuk Taichan & Ayam Potong) seperti yang ada di web app.
4. Belum tersedia analisa keuangan mendalam (HPP, laba kotor/bersih, dan tracking progres target BEP / Break Even Point harian).

Tujuan Fase 2 adalah melengkapi kapabilitas operasional dan manajerial ini ke dalam aplikasi Android native secara offline-first dan aman.

---

## 2. Rincian Fitur & Arsitektur

### 2.1. Redesign Elegan & Pembersihan Icon/Emoji "Slop"
* **Kartu Produk (`feature/pos/ProductCard.kt`)**:
  * Menghapus seluruh container banner emoji berukuran 42sp dan latar warna pastel anak-anak.
  * Kartu dibuat minimalis dengan latar putih bersih, radius sudut `12.dp`, border `OutlineSoft (1.dp)`, serta padding proporsional.
  * Tag Kategori: Teks kapsul lembut (misal `"SATE TAICHAN"`, `"AYAM"`, `"MINUMAN"`) menggunakan font mono/sans dengan warna muted.
  * Nama Produk: Tipografi sans-serif tegas (*SemiBold 15sp*).
  * Indikator Pedas / Info: Teks ringkas tanpa emoji cabai (contoh: `"Opsi Level 0–5"` atau keterangan porsi).
  * Baris Bawah: Harga Rupiah kontras (*Bold 15sp*, warna `Primary Teal`) dan tombol aksi bulat `+` minimalis `32.dp`.
* **Kategori Menu (`core/components/CategoryChips.kt`)**:
  * Menghapus seluruh emoji (🍢, 🍗, 🥤, 🍚).
  * Berupa pill kapsul modern teks bersih: *Semua*, *Sate Taichan*, *Ayam Goreng*, *Minuman*, *Nasi & Ekstra*.
  * Status Aktif: Latar gelap kontras (`Primary Deep Teal`) dengan teks putih.
  * Status Inaktif: Latar putih ber-outline tipis dengan teks netral (`TextSecondary`).
* **Dialog Modifier & Keranjang (`feature/pos/ModifierDialog.kt` & `feature/cart/CartBottomSheet.kt`)**:
  * Hapus icon emoji 28sp & 22sp.
  * Menampilkan informasi terstruktur: Nama menu, subtotal, stepper kuantitas bersih (`−` dan `+`), dan field input catatan ber-outline rapi.

---

### 2.2. Autentikasi & Role Separation (Kasir vs Owner)
* **Model & Session State**:
  * Enum `UserRole`: `CASHIER`, `OWNER`.
  * Data class `AuthSession(isLoggedIn: Boolean, currentUser: String, role: UserRole, activeShiftId: String?)`.
* **Layar `LoginScreen` (`feature/auth/LoginScreen.kt`)**:
  * Header brand minimalis: **KA POS** - *Sistem Kasir & Operasional Gerai*.
  * Tab pemilih role interaktif: `[ Kasir ]` | `[ Owner ]`.
  * Indikator 4 titik PIN (`pin-dots`) yang terisi otomatis saat numpad ditekan dan animasi getar bila PIN salah.
  * Numpad sentuh 4x3 (`1-9`, `C`, `0`, `⌫`).
  * Validasi instan saat 4 digit PIN selesai diinput:
    * Default PIN Kasir: `1111`
    * Default PIN Owner: `1234`
    * PIN tersimpan dan dapat diganti oleh Owner di menu Pengaturan.
* **Akses & Pembatasan Fitur**:
  * **Role CASHIER**:
    * Tab aktif: Kasir (POS), Transaksi, dan Buka/Tutup Shift.
    * Tombol VOID transaksi pada riwayat transaksi memerlukan konfirmasi PIN Owner.
    * Tab Laporan Detail, Stok, dan Pengaturan Toko disembunyikan.
  * **Role OWNER**:
    * Akses penuh ke seluruh fitur: POS, Transaksi, Shift, Stok Harian, Analisa Keuangan & BEP, serta Pengaturan Toko & PIN.
* **Quick Lock**:
  * Tombol gembok di header atas kasir untuk mengunci layar sementara saat kasir meninggalkan meja POS tanpa mereset keranjang belanja.

---

### 2.3. Modul Pelacak Stok Harian (`feature/stock/`)
* **Penyimpanan Lokal (Room / SharedPreferences)**:
  * Entitas / Data Class `DailyStockBatch`:
    * Taichan: `batchId`, `date`, `raw` (mentah - tusuk), `fried` (matang - tusuk), `sold` (terjual - tusuk, sinkron otomatis dari transaksi hari ini), `notes`.
    * Ayam: `part` (`sayap`, `paha_atas`, `paha_bawah`, `dada`), `date`, `raw` (potong), `fried` (potong), `sold` (potong).
* **Indikator Kesegaran (Freshness Check)**:
  * Hari ke-1: *Segar (Hari Ini)* (Badge Hijau)
  * Hari ke-2: *Hari ke-2* (Badge Biru)
  * Hari ke-3: *Hari ke-3 (Waspada)* (Badge Kuning)
  * $\ge 4$ Hari: *Tidak Layak Jual* (Badge Merah)
* **Fitur Tambahan**:
  * Modal input Restock batch baru untuk stok mentah.
  * Tombol update status batch goreng/matang.

---

### 2.4. Modul Analisa Keuangan & Progress Tracking BEP (`feature/analytics/`)
* **Parameter Toko (`AppSettings`)**:
  * `dailyFixedCost: Double`: Biaya tetap harian toko (sewa, listrik, gas, gaji harian). Default: Rp 350.000.
  * `dailyRevenueTarget: Double`: Target omzet harian (default: Rp 1.500.000).
* **Indikator Progres BEP Real-Time**:
  * Linear progress bar persentase capaian target BEP:
    $$\text{Progress \%} = \min\left(100\%, \frac{\text{Omzet Hari Ini}}{\text{Target BEP Harian}} \times 100\%\right)$$
  * Dinamika status teks motivasi gerai:
    * Bila di bawah target: *"Kurang Rp X untuk mencapai Titik Impas (BEP) hari ini."*
    * Bila mencapai target: *"🎉 Titik Impas (BEP) Tercapai! Omzet berikutnya merupakan akumulasi Laba Bersih."*
* **Kalkulasi Laba Rugi Komprehensif**:
  * **Omzet Kotor (Gross Sales)**: Total transaksi berstatus `PAID` pada periode terpilih (Hari Ini / 7 Hari / 30 Hari).
  * **Total HPP Bahan (COGS)**: Dihitung dari akumulasi item terjual dikalikan `costPrice` (modal) produk Room DB.
  * **Laba Kotor (Gross Profit)**: $\text{Omzet} - \text{Total HPP}$.
  * **Beban Operasional**: $\text{Biaya Tetap Harian} \times \text{Jumlah Hari Periode}$.
  * **Estimasi Laba Bersih (Net Profit)**: $\text{Laba Kotor} - \text{Beban Operasional}$.
  * **Net Margin**: $\frac{\text{Net Profit}}{\text{Omzet}} \times 100\%$.

---

### 2.5. Struktur Navigasi Dinamis (`ui/KaPosApp.kt`)
* Menambahkan rute navigasi baru:
  * `login`: Layar login PIN.
  * `stock`: Modul pelacak stok harian (Owner).
  * `analytics`: Layar analisa keuangan & tracking BEP (Owner).
* Navigasi bawah (`AppBottomNav.kt`) menyesuaikan secara reaktif terhadap role aktif:
  * Kasir: Kasir, Transaksi, Shift.
  * Owner: Kasir, Transaksi, Stok, Analisa BEP, Pengaturan.

---

## 3. Rencana Pengujian

1. **Pengujian UI Bebas Emoji**:
   * Memastikan tidak ada lagi emoji banner di `ProductCard`, `CategoryChips`, `ModifierDialog`, dan `CartBottomSheet`.
   * Memverifikasi tampilan bersih dan rapi di resolusi layar smartphone maupun tablet kasir.
2. **Pengujian Keamanan & Role Access**:
   * Kasir tidak dapat masuk ke layar Stok, Analisa BEP, dan Pengaturan tanpa login sebagai Owner.
   * Tombol Void transaksi terkunci dan mewajibkan otorisasi PIN Owner.
3. **Pengujian Stok Harian & Sinkronisasi Kasir**:
   * Menambahkan pesanan sate taichan dan ayam di POS, lalu memeriksa apakah nilai `sold` di modul stok bertambah secara otomatis.
4. **Pengujian Kalkulasi Finansial & BEP**:
   * Memvalidasi kebenaran kalkulasi HPP, Laba Kotor, Progres BEP %, dan Laba Bersih terhadap transaksi aktual.
