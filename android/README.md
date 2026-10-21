# KA POS — Android Native Mobile App (Kotlin + Jetpack Compose)

Aplikasi kasir **Point of Sale (POS)** mobile native Android untuk gerai **Taichan & Chicken KA**, dibangun dengan **Kotlin**, **Jetpack Compose (Material 3)**, dan mengacu pada standar desain *Soft Teal Glass* (rounded cards 16-24dp, token warna disiplin, background gradient lembut).

Aplikasi ini mengadopsi arsitektur **Offline-First**:
- Menggunakan **Room Database** lokal sebagai Single Source of Truth (SSOT).
- Tetap dapat menerima pesanan & memproses pembayaran tanpa koneksi internet.
- Pesanan offline otomatis masuk antrean `PENDING_SYNC` dan disinkronkan ke tabel PostgreSQL Supabase (`orders` & `order_items`) saat koneksi pulih.

---

## 📱 Modul & Fitur Utama

### 1. POS Katalog & Keranjang (`feature/pos`, `feature/cart`)
- Search bar menu instan & Chip filter kategori (Sate Taichan, Ayam Goreng, Minuman, Nasi & Ekstra).
- Grid 2-kolom kartu produk bergaya modern dengan thumbnail pastel emoji, harga bold, dan tombol `+` cepat.
- Bar ringkasan keranjang melayang (*Floating Cart Summary*).
- Modal Bottom Sheet keranjang dengan stepper kuantitas bulat `- / +`, rincian subtotal, dan tombol *"Lanjut ke Pembayaran"*.

### 2. Konfirmasi Penjualan / Checkout (`feature/checkout`)
- Generator nomor nota otomatis (`KA-YYYYMMDD-HHmmss`).
- Switch tipe pesanan: **Dine-In** (lengkap dengan input No. Meja) & **Take-Away**.
- Input nama pelanggan & catatan khusus pesanan.
- Pilihan metode bayar: **Tunai (Cash)**, **QRIS**, dan **Transfer**.
- Kalkulator uang kembalian instan dengan tombol nominal cepat (Uang Pas, 50k, 100k, 200k).

### 3. Riwayat Transaksi & Struk Digital (`feature/transactions`)
- Daftar transaksi lengkap dengan pencarian nomor nota/pelanggan.
- Filter status: Semua, Lunas (`Paid`), dan Pending.
- Indikator status sinkronisasi awan (*Cloud Synced* vs *Pending Upload*).
- Modal Struk Digital dengan rincian item, kasir, total, dan opsi cetak struk.

### 4. Laporan & Statistik Penjualan (`feature/report`)
- Filter periode: Hari Ini, 7 Hari, dan Bulan Ini.
- Grid 2x2 Stat Cards: **Total Omzet**, **Total Transaksi**, **Tunai (Kas Laci)**, dan **QRIS/Transfer**.
- Rekap Shift Kasir aktif & nilai rata-rata pesanan (*Basket Size*).
- Action bar cetak rekap, unduh PDF, dan bagikan.

### 5. Manajemen Menu & BOM Resep (`feature/menu`)
- CRUD produk menu dengan emoji, level pedas, harga jual & kategori.
- **Resep BOM (Bill of Materials)** per produk — menghubungkan menu ke bahan baku.
- Kalkulasi **BOP (Biaya Operasional Pokok / HPP)** otomatis dari harga beli bahan baku.
- Badge margin dinamis: 🟢 ≥20% (Sehat) / 🟡 <20% (Rendah) / 🔴 Merugi.
- Preview estimasi BOP per porsi langsung di dialog Resep BOM.

### 6. Manajemen Stok & Inventori (`feature/stock`) ⭐ *v1.2 — Baru*
- **Sistem Batch per Kedatangan**: setiap restock bahan baku dicatat sebagai batch terpisah dengan harga beli & tanggal kadaluarsa sendiri.
- **Moving Weighted Average (Rata-Rata Tertimbang Bergerak)**: harga beli rata-rata bahan baku selalu akurat dan otomatis ter-update setiap restock.
- **Auto BOP Sync**: setiap perubahan harga beli bahan baku otomatis merecalculate BOP (HPP) semua produk terkait via Resep BOM.
- **FIFO Deduction**: stok bahan baku dikurangi dari batch yang paling dulu datang / paling dekat kadaluarsa saat pesanan diproses.
- **Tracking Kadaluarsa & Kesegaran Batch**:
  - 🟢 **Segar** — masa simpan > 2 hari
  - 🟡 **Segera Exp** — ≤ 2 hari menuju kadaluarsa
  - 🔴 **Kadaluarsa / Tidak Layak** — lewat tanggal atau ditandai rusak
- **Badge status** otomatis muncul di kartu bahan baku sesuai kondisi terburuk batch aktif.
- **Catat Waste (Pemusnahan)**: mencatat kuantitas rusak/terbuang dengan kalkulasi estimasi nilai kerugian finansial ke jurnal mutasi stok.
- **Toggle Kelayakan Batch**: staf dapat menandai batch tertentu sebagai Tidak Layak Jual tanpa menghapus data.
- 2 tab utama: **Inventori (Bahan Baku & Batch)** dan **Audit Mutasi & Waste**.

### 7. Pengaturan & Koneksi Supabase (`feature/settings`)
- Form konfigurasi dinamis Supabase URL & Anon Public Key.
- Tombol pengujian koneksi cloud instan (*Test Connection*).
- Profil gerai & kasir aktif (nama toko, alamat, footer struk).
- Status antrean offline & tombol sinkronisasi paksa (*Sync Now*).

---

## 🏗️ Arsitektur & Tech Stack

```
com.kapos.mobile/
├── core/
│   ├── data/db/         # Room DAO, Entity, AppDatabase
│   ├── data/repository/ # InventoryRepository, OrderRepository, etc.
│   ├── components/      # StatCard, PrimaryButton, dll (shared UI)
│   ├── theme/           # AppColors, AppShapes, AppTypography
│   └── utils/           # CurrencyFormatter, DateUtils
├── domain/
│   └── model/           # Models.kt, StockModels.kt (pure Kotlin)
└── feature/
    ├── pos/             # POS screen + ViewModel
    ├── cart/            # Cart screen + ViewModel
    ├── checkout/        # Checkout + payment flow
    ├── transactions/    # Transaction history + receipt
    ├── report/          # Sales report + stats
    ├── menu/            # Menu management + RecipeBomDialog
    ├── stock/           # Stock management + batch dialogs
    └── settings/        # App settings + Supabase config
```

| Layer | Teknologi |
|-------|-----------|
| **UI** | Jetpack Compose (Material 3) |
| **State Management** | ViewModel + StateFlow |
| **Local DB** | Room `2.6.1` (KSP annotation processor) |
| **Networking** | OkHttp `4.12.0` + Kotlinx Serialization |
| **Cloud Sync** | Supabase (PostgreSQL REST API) |
| **Build** | Gradle KTS, minSdk 26, targetSdk 34 |

---

## 🛠️ Persyaratan Sistem

- **Android Studio**: Hedgehog (2023.1.1) atau lebih baru
- **JDK**: Java 17
- **Min SDK**: 26 (Android 8.0 Oreo)
- **Target / Compile SDK**: 34
- **Jetpack Compose BOM**: `2024.02.01` (Material 3 `1.2.0`)

---

## 🚀 Cara Menjalankan di Android Studio

1. Buka **Android Studio**.
2. Pilih **Open** dan arahkan ke folder:
   ```
   <repo-root>/android
   ```
3. Tunggu proses **Gradle Sync** selesai mengunduh dependensi.
4. Hubungkan perangkat Android fisik via USB Debugging atau jalankan Android Emulator.
5. Tekan **Run (`Shift + F10`)** untuk meng-install dan menjalankan aplikasi `KaPosMobile`.
6. Saat pertama kali dijalankan, aplikasi sudah menyertakan menu bawaan (*seed data*) Sate Taichan & Chicken KA — langsung siap digunakan bahkan tanpa internet.

---

## 🌐 Menghubungkan ke Supabase Cloud

1. Buka tab **Pengaturan** di navigasi bawah aplikasi.
2. Masukkan **Supabase URL** dan **Supabase Anon Key** gerai Anda.
3. Tekan **Tes Koneksi Cloud**.
4. Tekan **Simpan Pengaturan** & **Sinkronkan Sekarang**.
5. Katalog menu akan otomatis disinkronkan dari tabel `categories` & `products`, dan setiap transaksi yang dicatat kasir akan otomatis terkirim ke tabel `orders` & `order_items` Supabase.

---

## 📋 Changelog

### v1.2.0 — Stok Terintegrasi + BOP Otomatis + Kadaluarsa Batch
- Sistem batch per kedatangan dengan Moving Weighted Average
- Auto-recalculate BOP produk via Resep BOM setiap restock
- FIFO deduction stok saat pesanan diproses
- Tracking tanggal kadaluarsa & status kelayakan per batch
- Dialog Catat Waste dengan estimasi kerugian finansial
- Badge kesegaran dinamis di kartu bahan baku
- Margin badge dinamis (Sehat / Rendah / Rugi) di manajemen menu
- 84 unit tests passing ✅

### v1.1.0 — Manajemen Menu & BOM Resep
- CRUD produk & kategori menu
- Resep BOM per produk (Bill of Materials)
- Kalkulasi BOP/HPP dari harga beli bahan baku
- Manajemen stok dasar bahan baku

### v1.0.0 — POS Core
- Katalog produk & keranjang belanja
- Checkout multi-metode pembayaran (Tunai/QRIS/Transfer)
- Riwayat transaksi & struk digital
- Laporan penjualan harian/mingguan/bulanan
- Pengaturan & sinkronisasi Supabase cloud
- Offline-first architecture
