---
name: compose-fintech-pos-ui
description: Panduan membuat UI aplikasi Android dengan Kotlin Jetpack Compose (Material 3) bergaya modern "soft teal glass" untuk aplikasi POS / manajemen penjualan / pembelian / inventori / laporan keuangan. Gunakan skill ini SETIAP KALI user meminta membuat layar, komponen, tema, atau design system di Jetpack Compose yang mirip referensi visual berikut - kartu putih rounded, warna utama teal gelap, background gradient biru-abu lembut, chip filter, bottom navigation bulat, daftar transaksi dengan badge Paid/Unpaid, kartu statistik laporan, form konfirmasi penjualan, halaman pengaturan. Juga gunakan saat user menyebut "desain seperti screenshot/referensi", "UI POS", "purchase management", "sales report", "transactions screen", atau minta Compose UI yang clean, soft, dan modern walaupun tidak menyebut gaya secara eksplisit.
---

# Compose Fintech POS UI

Skill ini memandu agent (Antigravity) membuat UI Jetpack Compose yang meniru bahasa desain aplikasi POS/Sales: **bersih, lembut, kartu putih membulat, aksen teal gelap**, di atas background gradient biru-abu muda.

Baca `references/components.md` saat akan menulis kode komponen (berisi kode siap pakai). File ini berisi aturan, token desain, dan alur kerja.

## 1. Aturan Kerja (WAJIB)

1. **Cek proyek dulu** sebelum menulis kode: baca `build.gradle(.kts)`, `libs.versions.toml`, struktur package, dan tema yang sudah ada. Jangan menimpa tema existing tanpa konfirmasi; extend jika sudah ada.
2. **Compose + Material 3 saja.** Tidak memakai XML layout. Tidak memakai library UI berat tanpa izin.
3. **Semua warna, radius, spacing berasal dari token** (bagian 2). Dilarang hardcode `Color(0xFF...)` atau `RoundedCornerShape(17.dp)` di dalam screen. Taruh di `ui/theme/`.
4. **Pisahkan stateless UI dan state.** Setiap Composable layar punya pasangan: `XxxRoute` (ambil ViewModel, collect state) dan `XxxScreen(state, onEvent)` (stateless, bisa di-preview).
5. **Setiap Composable publik wajib punya `@Preview`** (light), dengan data dummy dari `PreviewData.kt`.
6. **Kerjakan bertahap**: Theme -> komponen dasar -> layar. Setelah tiap tahap pastikan proyek compile (`./gradlew assembleDebug`) sebelum lanjut.
7. Gunakan `Modifier` sebagai parameter pertama opsional, `LazyColumn`/`LazyVerticalGrid` untuk list, `key =` pada item list, dan `collectAsStateWithLifecycle()`.
8. Aksesibilitas: `contentDescription` pada ikon yang bisa diklik, target sentuh minimal 48dp, kontras teks memadai.
9. Teks UI memakai `stringResource` (siapkan `strings.xml`), jangan hardcode untuk teks final.
10. Jangan menyalin gambar/aset dari referensi. Pakai placeholder (`Box` berwarna / `painterResource` dummy) dan beri tahu user di mana mengganti gambar asli.

## 2. Design Tokens

### Warna (hasil observasi referensi)

| Token | Hex | Pemakaian |
|---|---|---|
| `Primary` | `#0B5C73` | Tombol utama, nav aktif, angka penting, link |
| `PrimaryDark` | `#08485B` | Pressed state, header teks tebal |
| `PrimarySoft` | `#E3F1F5` | Ikon container, chip aktif, tile terpilih |
| `BgTop` | `#DCE9EE` | Gradient background atas |
| `BgBottom` | `#C7DAE1` | Gradient background bawah |
| `Surface` | `#FFFFFF` | Kartu, bottom sheet, input |
| `SurfaceTint` | `#F3F8FA` | Field/kartu sekunder, search bar |
| `Outline` | `#DCE5E9` | Border tipis kartu/input |
| `TextPrimary` | `#12303B` | Judul, nilai |
| `TextSecondary` | `#6B8189` | Label, caption, placeholder |
| `Success` / `SuccessBg` | `#2E9E6B` / `#DDF3E8` | Badge "Paid" |
| `Danger` / `DangerBg` | `#E5484D` / `#FDE4E5` | "Clear Items", badge "Unpaid", hapus |
| `Warning` | `#E8A33D` | Indikator/ikon peringatan |
| `Info` / `InfoBg` | `#3C7A8C` / `#E1EEF2` | Badge kategori ("General") |

Tambahan: kartu produk boleh memakai gradient pastel lembut (hijau muda, peach, pink) hanya sebagai latar kartu makanan/kategori.

### Bentuk (radius)

| Token | dp | Pemakaian |
|---|---|---|
| `Xs` | 8 | Badge kecil, chip kecil |
| `Sm` | 12 | Input, tile ikon |
| `Md` | 16 | Kartu list, stat card |
| `Lg` | 24 | Kartu besar, tombol pill |
| `Xl` | 32 | Bottom sheet (sudut atas), frame besar |
| `Full` | 50% | Tombol bulat, nav item, avatar |

### Spacing (kelipatan 4)
`4, 8, 12, 16, 20, 24`. Padding layar horizontal **16dp**. Jarak antar kartu **12dp**. Padding dalam kartu **12-16dp**.

### Tipografi
Font bersih sans-serif geometris (default: **Plus Jakarta Sans** via `androidx.compose.ui.text.googlefonts`, fallback `FontFamily.SansSerif`).

| Style | Size/Weight | Pemakaian |
|---|---|---|
| `titleLarge` | 18sp / SemiBold | Judul app bar |
| `titleMedium` | 16sp / SemiBold | Judul section |
| `bodyLarge` | 14sp / Medium | Isi utama, nama item |
| `bodyMedium` | 13sp / Normal | Teks list |
| `labelMedium` | 12sp / Medium | Chip, badge, label field |
| `labelSmall` | 10sp / Medium | Caption, "Vs last month" |
| Angka besar (stat) | 20-22sp / Bold | Nilai pada kartu statistik |

### Elevasi
Hampir datar. Gunakan **border 1dp `Outline`** + shadow sangat halus (`2-4dp`, alpha rendah) bukan elevasi tebal. Efek "glass" cukup dengan Surface putih alpha 0.85-0.95 di atas gradient; **jangan** memakai blur berat.

## 3. Pola Layar yang Harus Bisa Dibuat

Referensi memuat pola berikut. Saat user meminta salah satu, ikuti struktur ini.

1. **Purchase Management (grid produk)**: App bar (menu, judul, tombol "Add Item" pill kecil) -> Search bar dengan ikon mic -> Chip kategori horizontal (All, Fruits aktif, Shakes, Burger, Snacks) -> `LazyVerticalGrid` 2 kolom kartu produk (gambar, nama, deskripsi 2 baris, harga bold, ikon hati) -> Bar ringkasan keranjang di bawah ("2 Items • $05.75" + tombol "Next" pill teal) -> Bottom nav.
2. **Cart bottom sheet**: Header "2 Items" + "Clear Items" merah di kanan; baris item (thumbnail rounded, nama, "2 BOX x $7.50", stepper -/+ bulat, harga di kanan, ikon edit/hapus).
3. **Item editor (sheet)**: Field Quantity, Rate, Discount % (dengan ikon link antara % dan nominal), Total Amount; tombol Cancel (outline) + Save Item (filled teal); footer Subtotal + tombol "Continue Billing" lebar penuh.
4. **Confirm Sale (form)**: Invoice Number + Invoice Date (date picker field), baris "Cash Sale" + tombol "Change", section Billing Items (2) yang expandable, tombol kecil "Add Discount", "Add TAX", "Add Additional Charges" rata kanan, kartu Total Amount + dropdown Payment Mode, area Notes/Remarks (multiline), tombol Cancel + Save Item.
5. **Transactions (list)**: Search + tombol filter, selector periode "All Time" + "Change", list `Sale #1 / Cash Sale / tanggal` dengan nominal kanan dan badge **Paid** (hijau) / **Unpaid** (merah), dua tombol aksi mengambang "Payment In" dan "New Sale" + tombol "+" di tengah, bottom nav.
6. **Transaction type picker**: Grid 3 kolom tile ikon (Sale, Payment In, Sale Return, Quotation) dengan section header "Sales Transactions", "Purchase Transactions", "Other Transactions"; tile terpilih punya border teal + background `PrimarySoft`; tombol tutup (X) bulat di bawah.
7. **Sales Report**: App bar + ikon filter, periode "All Time", **grid 2x2 stat card** (Total Sales "5 Entries", Total Amount, Received Amount, Unpaid Amount; masing-masing ikon kecil, nilai bold, caption "Vs last month"), section "Transaction Lists" dengan avatar + nama + nominal + badge, **action bar bawah** 4 tombol: Download, Print PDF, Excel, Share.
8. **App Settings**: Daftar baris dengan ikon kiri, label, nilai kanan abu-abu, chevron; baris toggle (Privacy Mode, App Lock) memakai `Switch` teal; grup dalam satu kartu putih dengan `HorizontalDivider` tipis.
9. **Bottom navigation**: 4-5 item ikon bulat dalam kapsul putih mengambang; item aktif = lingkaran `Primary` dengan ikon putih (+ label), item nonaktif = ikon `TextSecondary` pada lingkaran `SurfaceTint`.

## 4. Arsitektur yang Disarankan

```
app/src/main/java/<package>/
├── ui/
│   ├── theme/        Color.kt, Shape.kt, Type.kt, Spacing.kt, Theme.kt
│   ├── components/   AppTopBar, SearchField, CategoryChips, ProductCard,
│   │                 StatCard, StatusBadge, TransactionRow, AppBottomNav,
│   │                 PrimaryButton, SecondaryButton, AmountField, SettingsRow
│   └── screens/
│       ├── purchase/   PurchaseRoute, PurchaseScreen, PurchaseViewModel
│       ├── cart/
│       ├── sale/       ConfirmSaleScreen
│       ├── transactions/
│       ├── report/
│       └── settings/
├── domain/model/     Product, CartItem, Transaction, PaymentStatus
└── PreviewData.kt
```

- State: `data class XxxUiState(...)` + `sealed interface XxxEvent`; ViewModel mengekspos `StateFlow<XxxUiState>`.
- Navigasi: Navigation Compose, route type-safe (`@Serializable`) jika versi mendukung.
- Format uang lewat satu helper (`NumberFormat`/`Currency`), jangan `"$" + value` tersebar.

## 5. Alur Kerja Saat Diminta Membuat Sesuatu

1. **Konfirmasi singkat** (hanya jika belum jelas): package name, mata uang (default `Rp`/IDR atau `$` mengikuti referensi), dark mode perlu atau tidak, font.
2. **Buat/extend Theme** dari bagian 2 (lihat `references/components.md` bagian A).
3. **Buat komponen dasar** yang dibutuhkan layar (bagian B).
4. **Buat layar** mengikuti pola bagian 3, stateless + preview.
5. **Hubungkan ViewModel & navigasi** dengan data dummy dulu.
6. **Verifikasi**: build, jalankan `@Preview`, cek checklist bagian 6.
7. **Laporkan** file yang dibuat, asumsi yang diambil, dan hal yang perlu diganti user (aset gambar, font, string).

## 6. Checklist Kualitas (cek sebelum selesai)

- [ ] Tidak ada warna/dp/sp hardcode di luar `ui/theme`
- [ ] Background layar memakai gradient `BgTop -> BgBottom`, kartu putih berradius 16-24dp
- [ ] Tombol utama pill teal, tombol sekunder outline; teks tombol SemiBold
- [ ] Badge Paid/Unpaid memakai pasangan warna Success/Danger
- [ ] Angka uang rata kanan, bold, format konsisten
- [ ] Padding layar 16dp, jarak antar kartu 12dp
- [ ] Bottom nav tidak menutupi konten terakhir (`contentPadding` bawah)
- [ ] Keyboard tidak menutupi field (`imePadding()`), form bisa di-scroll
- [ ] Layar tetap rapi di lebar 360dp dan 411dp serta font scale 1.3
- [ ] Semua Composable punya `@Preview`; tidak ada warning lint baru
- [ ] Proyek compile bersih

## 7. Yang Harus Dihindari

- Shadow tebal, gradient mencolok pada tombol, warna neon.
- Banyak font berbeda atau ukuran teks di luar skala tipografi.
- Menumpuk logika bisnis di Composable.
- Menggunakan `Color.Teal`/warna default Material (ungu) yang tersisa dari template.
- Menyalin teks/brand dari referensi sebagai milik sendiri; pakai konten generik.
