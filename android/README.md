# KA POS — Android Native Mobile App (Kotlin + Jetpack Compose)

Aplikasi kasir Point of Sale (POS) mobile native Android untuk gerai **Taichan & Chicken KA**, dibangun dengan **Kotlin**, **Jetpack Compose (Material 3)**, dan mengacu pada standar desain **`compose-fintech-pos-ui`** (Soft Teal Glass, rounded cards 16-24dp, token warna disiplin, background gradient lembut).

Aplikasi ini mengadopsi arsitektur **Offline-First**:
- Menggunakan **Room Database** lokal sebagai Single Source of Truth (SSOT).
- Tetap dapat menerima pesanan & memproses pembayaran tanpa koneksi internet.
- Pesanan offline otomatis masuk antrean `PENDING_SYNC` dan disinkronkan ke tabel PostgreSQL Supabase (`orders` & `order_items`) saat koneksi pulih.

---

## 📱 Modul & Fitur Utama

1. **POS Katalog & Keranjang (`feature/pos`, `feature/cart`)**
   - Search bar menu instan & Chip filter kategori (Sate Taichan, Ayam Goreng, Minuman, Nasi & Ekstra).
   - Grid 2-kolom kartu produk bergaya modern dengan thumbnail pastel emoji, harga bold, dan tombol `+` cepat.
   - Bar ringkasan keranjang melayang (*Floating Cart Summary*).
   - Modal Bottom Sheet keranjang dengan stepper kuantitas bulat `- / +`, rincian subtotal, dan tombol *"Lanjut ke Pembayaran"*.

2. **Konfirmasi Penjualan / Checkout (`feature/checkout`)**
   - Generator nomor nota otomatis (`KA-YYYYMMDD-HHmmss`).
   - Switch tipe pesanan: **Dine-In** (lengkap dengan input No. Meja) & **Take-Away**.
   - Input nama pelanggan & catatan khusus pesanan.
   - Pilihan metode bayar: **Tunai (Cash)**, **QRIS**, dan **Transfer**.
   - Kalkulator uang kembalian instan dengan tombol nominal cepat (Uang Pas, 50k, 100k, 200k).

3. **Riwayat Transaksi & Struk Digital (`feature/transactions`)**
   - Daftar transaksi lengkap dengan pencarian nomor nota/pelanggan.
   - Filter status: Semua, Lunas (`Paid`), dan Pending.
   - Indikator status sinkronisasi awan (*Cloud Synced* vs *Pending Upload*).
   - Modal Struk Digital dengan rincian item, kasir, total, dan opsi cetak struk.

4. **Laporan & Statistik Penjualan (`feature/report`)**
   - Filter periode: Hari Ini, 7 Hari, dan Bulan Ini.
   - Grid 2x2 Stat Cards: **Total Omzet**, **Total Transaksi**, **Tunai (Kas Laci)**, dan **QRIS/Transfer**.
   - Rekap Shift Kasir aktif & nilai rata-rata pesanan (*Basket Size*).
   - Action bar cetak rekap, unduh PDF, dan bagikan.

5. **Pengaturan & Koneksi Supabase (`feature/settings`)**
   - Form konfigurasi dinamis Supabase URL & Anon Public Key.
   - Tombol pengujian koneksi cloud instan (*Test Connection*).
   - Profil gerai & kasir aktif (nama toko, alamat, footer struk).
   - Status antrean offline & tombol sinkronisasi paksa (*Sync Now*).

---

## 🛠️ Persyaratan Sistem & Tech Stack

- **Android Studio**: Hedgehog (2023.1.1) / Iguana / Jellyfish / Ladybug atau lebih baru
- **JDK**: Java 17
- **Min SDK**: 26 (Android 8.0 Oreo)
- **Target / Compile SDK**: 34
- **Jetpack Compose BOM**: `2024.02.01` (Material 3 `1.2.0`)
- **Room Database**: `2.6.1` (KSP)
- **Networking**: OkHttp `4.12.0` + Kotlinx Serialization

---

## 🚀 Cara Menjalankan di Android Studio

1. Buka **Android Studio**.
2. Pilih **Open** dan arahkan ke folder:
   ```
   <repo-root>/android
   ```
3. Tunggu proses **Gradle Sync** selesai mengunduh dependensi.
4. Hubungkan perangkat Android fisik via USB Debugging atau jalankan Android Emulator.
5. Tekan tombol **Run (`Shift + F10`)** untuk meng-install dan menjalankan aplikasi `KaPosMobile`.
6. Saat pertama kali dijalankan, aplikasi sudah menyertakan menu bawaan (*seed data*) Sate Taichan & Chicken KA, sehingga langsung siap digunakan bahkan tanpa internet.

---

## 🌐 Menghubungkan ke Supabase Cloud

1. Buka tab **Pengaturan** di navigasi bawah aplikasi.
2. Masukkan **Supabase URL** dan **Supabase Anon Key** gerai Anda.
3. Tekan **Tes Koneksi Cloud**.
4. Tekan **Simpan Pengaturan** & **Sinkronkan Sekarang**.
5. Katalog menu akan otomatis disinkronkan dari tabel `categories` & `products`, dan setiap transaksi yang dicatat kasir akan otomatis terkirim ke tabel `orders` & `order_items` Supabase.
