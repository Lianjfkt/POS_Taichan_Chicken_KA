# Design Spec: Native Android Mobile POS KA (Jetpack Compose & Material 3)

- **Date:** 2026-10-15
- **Package Name:** `com.kapos.mobile`
- **Application Name:** KA POS (Taichan & Chicken KA)
- **Target Platform:** Android (minSdk 26, targetSdk 34/35)
- **UI/UX Reference:** `compose-fintech-pos-ui` (Soft Teal Glass, Material 3, Rounded Cards)
- **Architecture Pattern:** Clean Architecture Single-Module with Offline-First SSOT (Room Database + Supabase REST)

---

## 1. Vision & Goals

Membangun aplikasi mobile native Android kasir POS untuk gerai F&B **Taichan & Chicken KA** yang:
1. **Mengadopsi Penuh Panduan UI/UX `compose-fintech-pos-ui`**: Estetika modern "soft teal glass" dengan kartu putih membulat (radius 16-24dp), warna primer teal gelap (`#0B5C73`), background vertikal gradient lembut (`#DCE9EE` ke `#C7DAE1`), bottom navigation kapsul melayang, dan tipografi Plus Jakarta Sans.
2. **Kinerja Kasir Cepat & Respon Instan**: Navigasi mulus berbasis Jetpack Compose NavHost tanpa lag saat input pesanan di jam sibuk.
3. **Offline-First & Data Reliability**: Menggunakan Room Database sebagai Single Source of Truth (SSOT). Kasir tetap dapat menerima pesanan dan memproses pembayaran saat koneksi internet terputus; transaksi otomatis disinkronkan ke Supabase saat online.
4. **Keselarasan dengan Backend Supabase**: Mengikuti skema tabel PostgreSQL yang ada (`categories`, `products`, `orders`, `order_items`, `shifts`, `store_settings`).

---

## 2. Design Tokens & Styling (`core/theme`)

Mengacu secara ketat pada aturan token di `compose-fintech-pos-ui/SKILL.md`:

### 2.1 Color Palette (`Color.kt`)
* `Primary`: `#0B5C73` — Aksen utama, tombol aksi checkout, navigation aktif, tautan
* `PrimaryDark`: `#08485B` — Pressed state, header tebal
* `PrimarySoft`: `#E3F1F5` — Latar container ikon, chip kategori aktif, highlight kartu
* `BgTop`: `#DCE9EE` — Latar gradient atas layar
* `BgBottom`: `#C7DAE1` — Latar gradient bawah layar
* `SurfaceWhite`: `#FFFFFF` — Latar kartu, bottom sheet, field input
* `SurfaceTint`: `#F3F8FA` — Field pencarian, kartu ringkasan sekunder
* `OutlineSoft`: `#DCE5E9` — Garis border tipis (1.dp) pada kartu dan input
* `TextPrimary`: `#12303B` — Judul utama, nama menu, harga nominal
* `TextSecondary`: `#6B8189` — Label, placeholder, caption
* `Success` / `SuccessBg`: `#2E9E6B` / `#DDF3E8` — Badge status "Paid" / "Lunas"
* `Danger` / `DangerBg`: `#E5484D` / `#FDE4E5` — Badge status "Unpaid", tombol hapus / void
* `Warning`: `#E8A33D` — Indikator peringatan
* `Info` / `InfoBg`: `#3C7A8C` / `#E1EEF2` — Badge kategori umum

### 2.2 Shapes & Radius (`Shape.kt`)
* `Xs`: `RoundedCornerShape(8.dp)` — Badge kecil, chip status
* `Sm`: `RoundedCornerShape(12.dp)` — Input field, thumbnail produk
* `Md`: `RoundedCornerShape(16.dp)` — Kartu produk, kartu transaksi, stat card
* `Lg`: `RoundedCornerShape(24.dp)` — Kartu checkout, tombol pill
* `SheetTop`: `RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)` — Sudut atas Cart Bottom Sheet
* `Full`: `CircleShape` — Tombol kuantitas bulat, avatar kasir, bottom bar kapsul

### 2.3 Typography & Spacing (`Type.kt`, `Spacing.kt`)
* Font: Plus Jakarta Sans / Sans-Serif sistem
* `titleLarge`: 18.sp SemiBold (App Bar)
* `titleMedium`: 16.sp SemiBold (Judul section, nama produk)
* `bodyLarge`: 14.sp Medium (Isi item, rincian nota)
* `bodyMedium`: 13.sp Normal (Deskripsi, teks bantuan)
* `labelMedium`: 12.sp Medium (Chip, badge status)
* `statNumber`: 20-22.sp Bold (Nilai omset & subtotal)
* Spacing standar: `ScreenPadding = 16.dp`, `CardGap = 12.dp`, `InnerPadding = 14.dp`
* Mata uang diformat konsisten menggunakan helper `CurrencyFormatter`: contoh `Rp 25.000`.

---

## 3. Project Architecture & Directory Layout

Proyek ditempatkan di root direktori `/android/`:

```
android/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/
│   └── gradle-wrapper.properties
└── app/
    ├── build.gradle.kts
    └── src/
        └── main/
            ├── AndroidManifest.xml
            ├── res/
            │   ├── values/strings.xml, colors.xml
            │   └── drawable/
            └── java/com/kapos/mobile/
                ├── KaPosApplication.kt
                ├── MainActivity.kt
                ├── core/
                │   ├── theme/
                │   │   ├── Color.kt
                │   │   ├── Shape.kt
                │   │   ├── Spacing.kt
                │   │   ├── Type.kt
                │   │   └── Theme.kt
                │   ├── components/
                │   │   ├── AppTopBar.kt
                │   │   ├── SearchField.kt
                │   │   ├── CategoryChips.kt
                │   │   ├── ProductCard.kt
                │   │   ├── StatCard.kt
                │   │   ├── StatusBadge.kt
                │   │   ├── AppBottomNav.kt
                │   │   ├── PrimaryButton.kt
                │   │   └── AmountField.kt
                │   ├── utils/
                │   │   ├── CurrencyFormatter.kt
                │   │   └── DateFormatter.kt
                │   └── data/
                │       ├── local/
                │       │   ├── AppDatabase.kt
                │       │   ├── entity/ (CategoryEntity, ProductEntity, OrderEntity, OrderItemEntity, ShiftEntity)
                │       │   └── dao/ (ProductDao, CategoryDao, OrderDao, ShiftDao)
                │       ├── remote/
                │       │   ├── SupabaseConfig.kt
                │       │   ├── SupabaseClient.kt
                │       │   └── dto/ (SupabaseProductDto, SupabaseOrderDto, etc.)
                │       └── repository/
                │           ├── PosRepository.kt (SSOT: Room + Supabase Sync)
                │           └── SettingsRepository.kt
                ├── domain/
                │   └── model/
                │       ├── Product.kt
                │       ├── Category.kt
                │       ├── CartItem.kt
                │       ├── Order.kt
                │       ├── PaymentMethod.kt
                │       ├── OrderStatus.kt
                │       └── Shift.kt
                └── feature/
                    ├── pos/
                    │   ├── PosViewModel.kt
                    │   ├── PosUiState.kt
                    │   ├── PosRoute.kt
                    │   └── PosScreen.kt
                    ├── cart/
                    │   └── CartBottomSheet.kt
                    ├── checkout/
                    │   ├── CheckoutViewModel.kt
                    │   ├── CheckoutUiState.kt
                    │   ├── CheckoutRoute.kt
                    │   └── ConfirmSaleScreen.kt
                    ├── transactions/
                    │   ├── TransactionsViewModel.kt
                    │   ├── TransactionsUiState.kt
                    │   ├── TransactionsRoute.kt
                    │   ├── TransactionsScreen.kt
                    │   └── OrderDetailDialog.kt
                    ├── report/
                    │   ├── ReportViewModel.kt
                    │   ├── ReportUiState.kt
                    │   ├── ReportRoute.kt
                    │   └── ReportScreen.kt
                    └── settings/
                        ├── SettingsViewModel.kt
                        ├── SettingsUiState.kt
                        ├── SettingsRoute.kt
                        └── SettingsScreen.kt
```

---

## 4. Feature Specifications & Screen Breakdown

### 4.1 Modul POS & Katalog Menu (`feature/pos`)
* **Header / Top Bar**:
  - Logo/Teks gerai "Taichan & Chicken KA".
  - Indikator status koneksi (Hijau: Terhubung Supabase / Kuning: Mode Offline).
  - Tombol sinkronisasi cepat (`SyncNow`).
* **Search & Filter**:
  - Input teks pencarian menu instan.
  - Chip horizontal untuk kategori produk (`Semua`, `Taichan`, `Ayam Goreng`, `Minuman`, `Paket`, `Ekstra`).
* **Grid Menu (2 Kolom)**:
  - Menggunakan `LazyVerticalGrid(GridCells.Fixed(2))`.
  - Kartu putih `Md` (16.dp), emoji / thumbnail berlatar pastel, nama menu, harga bold rata kiri-bawah, dan tombol bulat `+` (Primary teal) di kanan bawah.
  - Tap kartu membuka opsi kustomisasi (Level Pedas: Original/Sedang/Pedas Gila, Catatan Khusus).
* **Floating Cart Summary Bar**:
  - Bar melayang di atas navigasi bawah ketika keranjang terisi (`X Item • Rp YYY.000`).
  - Tombol pill teal `"Lihat Keranjang"` membuka `CartBottomSheet`.

### 4.2 Cart Bottom Sheet (`feature/cart`)
* Header berisi total kuantitas item dan tombol merah `"Kosongkan Keranjang"`.
* Daftar item dengan stepper kuantitas bulat (`-` `Qty` `+`), subtotal per baris, dan opsi hapus.
* Rincian Subtotal dan tombol lebar penuh `"Lanjut ke Pembayaran"`.

### 4.3 Confirm Sale / Checkout Form (`feature/checkout`)
* **Info Pesanan**:
  - Generator nomor faktur / invoice (`KA-YYYYMMDD-XXXX`).
  - Toggle tipe order: **Dine-In** (input No. Meja) atau **Take-Away**.
  - Input Nama Pelanggan (opsional).
* **Kalkulasi & Pembayaran**:
  - Ringkasan subtotal pesanan.
  - Pilihan metode: **Tunai (Cash)**, **QRIS**, atau **Transfer**.
  - Untuk metode Tunai: Field jumlah uang dibayarkan, tombol nominal cepat (Uang Pas, 50k, 100k, 200k), serta kalkulasi otomatis uang kembalian.
* **Tombol Aksi**:
  - Tombol Batal (Outline) & Tombol Selesaikan Transaksi (Filled Teal).
  - Saat selesai: mencatat pesanan ke Room secara instan, mengosongkan keranjang, dan memicu sync background ke Supabase.

### 4.4 Riwayat Transaksi (`feature/transactions`)
* Input pencarian nomor nota atau nama pelanggan.
* Tab filter status: `Semua`, `Lunas (Paid)`, `Pending / Belum Lunas (Unpaid)`.
* List transaksi menggunakan `LazyColumn`:
  - Kartu rounded putih menampilkan: Nomor nota, Jam/Tanggal, Tipe Order, Total Nominal (Bold), dan Badge Status (`Success` / `Danger`).
  - Ikon awan kecil bila pesanan masih berstatus `PENDING_SYNC`.
* Klik transaksi membuka modal detail struk dengan tombol salin / bagikan nota.

### 4.5 Laporan Penjualan & Statistik (`feature/report`)
* Selector rentang waktu: `Hari Ini`, `7 Hari Terakhir`, `Bulan Ini`.
* **2x2 Stat Cards**:
  1. **Total Penjualan / Omzet** (Nilai Rp bold).
  2. **Total Transaksi** (Jumlah struk berhasil).
  3. **Transaksi Tunai vs Non-Tunai** (Breakdown persentase & nominal).
  4. **Rata-rata Nilai Transaksi / Basket Size**.
* Rekap shift kasir: Kas awal, total kas masuk, dan uang tunai yang harus ada di laci.

### 4.6 Pengaturan & Koneksi Supabase (`feature/settings`)
* Konfigurasi URL Supabase dan Anon API Key.
* Tombol uji koneksi dan sinkronisasi data paksa.
* Informasi Profil Kasir dan Pengaturan Struk (Nama Toko, Alamat, Catatan Kaki Struk).
* Versi aplikasi dan status database lokal (jumlah transaksi offline yang menunggu sinkronisasi).

---

## 5. Offline-First Data Engine & Synchronization

### 5.1 Entities Room
* `CategoryEntity(id, name, emoji, sortOrder, isActive)`
* `ProductEntity(id, categoryId, name, price, costPrice, emoji, description, isActive)`
* `OrderEntity(id, orderNo, customerName, customerPhone, orderType, tableNo, subtotal, discount, total, paymentMethod, paymentStatus, cashierName, notes, createdAt, paidAt, syncStatus)`
* `OrderItemEntity(id, orderId, productId, productName, price, quantity, spiceLevel, notes, subtotal)`
* `ShiftEntity(id, cashierName, startCash, endCash, systemSales, status, openedAt, closedAt, syncStatus)`

### 5.2 Synchronization Pipeline
1. **Pull (Katalog Produk & Kategori)**:
   - Request `GET /rest/v1/categories` & `GET /rest/v1/products` dari Supabase.
   - Disimpan ke Room melalui DAO (`upsertCategories`, `upsertProducts`).
   - Menyediakan data menu default fallback (Sate Taichan Daging, Kulit, Paha, Ceker, Nasi, Sambal, Es Teh) saat pertama kali diinstal tanpa koneksi internet.
2. **Push (Order Offline Queue)**:
   - Pesanan baru disimpan ke Room dengan `syncStatus = "PENDING_SYNC"`.
   - `PosRepository.syncPendingOrders()` memfilter pesanan pending, lalu mengirimkan payload batch ke Supabase REST endpoint `/rest/v1/orders` dan `/rest/v1/order_items`.
   - Setelah server mengembalikan respon 201 Created, status diubah menjadi `SYNCED`.

---

## 6. Testing & Quality Checklist (Mengacu `compose-fintech-pos-ui`)

- [ ] Tidak ada warna hardcoded di luar `core/theme/Color.kt`.
- [ ] Latar belakang layar menggunakan gradient `ScreenBackground` (`BgTop` -> `BgBottom`).
- [ ] Seluruh kartu menggunakan background `SurfaceWhite` dengan border tipis `OutlineSoft` dan radius `Md` (16.dp).
- [ ] Tombol utama menggunakan pill shape rounded-full teal `#0B5C73`.
- [ ] Badge Paid / Unpaid menggunakan skema warna `Success` dan `Danger`.
- [ ] Format mata uang konsisten rupiah (`Rp xx.xxx`).
- [ ] Layar mendukung keyboard input (`imePadding()`) dan scrollable agar tidak tertutup keyboard.
- [ ] Seluruh composable utama memiliki stateless screen dan `@Preview` dengan dummy mock data.
