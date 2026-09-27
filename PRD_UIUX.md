# PRODUCT REQUIREMENT DOCUMENT (PRD) — UI/UX REDESIGN
## KA POS v3.0: Modern Cloud Point of Sale & Kitchen Management
**Spesialisasi F&B:** *KA Taichan & Chicken*  
**Arsitektur Target:** *Vercel (Frontend PWA) + Supabase (PostgreSQL, Auth, Realtime)*  
**Versi Dokumen:** 3.0.0  
**Status:** Approved for Implementation  

---

## 1. Executive Summary & Visi Produk

### 1.1 Latar Belakang & Masalah UI/UX Saat Ini
Aplikasi KA POS saat ini memiliki fungsionalitas dasar yang lengkap, namun memiliki kendala besar dalam adopsi pengguna (*user experience*):
1. **Terlalu Padat & Kaku di Layar Kecil (Mobile):** Tampilan kasir pada smartphone terasa sempit, keranjang (*cart*) menutupi menu, dan elemen navigasi sulit dijangkau dengan satu tangan (*one-thumb navigation*).
2. **Ketiadaan Visual Table Management:** Meja makan dan pesanan aktif (*on-proses*) hanya berupa daftar teks statis tanpa representasi visual denah meja.
3. **Menu Modifiers Kurang Dinamis:** Tingkat kepedasan dan catatan khusus tercampur dalam nama produk atau catatan tersembunyi, memperlambat proses input kasir di jam sibuk (*rush hour*).
4. **Kitchen Display System (KDS) Pasif:** Layar dapur tidak memiliki peringatan suara (*audio chime*) dan visual *aging timer* yang kontras saat pesanan menumpuk.
5. **Alur Kasir (Shift & Drawer) Rentan Manipulasi:** Tidak adanya alur *Blind Cash Drop* (hitung uang fisik mandiri tanpa melihat saldo sistem) saat tutup kasir.

### 1.2 Visi & Nilai Inti UI/UX Baru
- **Ultra-Fast Ordering (Target < 15 detik/order):** Alur pemesanan intuitif, modal modifier cepat, dan kalkulasi uang instan.
- **Adaptive Layout:** 
  - *Smartphone (Kasir Keliling/Waiter):* Floating Cart Drawer, thumb-friendly navigation.
  - *Tablet (Kasir Utama & Dapur):* Split-view 2-kolom dinamis, grid KDS tiket berukuran besar.
  - *Desktop/Laptop (Owner):* Dashboard analitik penuh, manajemen inventaris & resep tabel komprehensif.
- **Offline-First Resilient:** Indikator sinkronisasi cloud Supabase yang jelas; kasir tetap dapat mengetik pesanan tanpa internet tanpa jeda loading.
- **Aesthetics & Tone:** Modern Dark/Light Mode bernuansa premium, tema warna khas *Flame Orange* (`#F97316`) dan *Deep Slate* (`#0B0F19`), tipografi jernih (*Plus Jakarta Sans* & *DM Mono*).

---

## 2. User Persona & Matriks Kebutuhan

| Persona | Perangkat Utama | Kebutuhan Utama | Hambatan UI/UX Saat Ini |
|---|---|---|---|
| **Kasir (Cashier)** | Smartphone / Tablet 10" | Input order cepat, hitung uang kembalian instan, cetak struk bluetooth, tutup shift akurat | Tombol kalkulator uang kecil, cart sempit di layar HP, selisih kas fisik membingungkan |
| **Koki Dapur (Chef / Cook)** | Tablet Dapur 10"–12" | Melihat daftar makanan yang harus dimasak per meja, update status selesai, peringatan order baru | Tidak ada suara bel saat order masuk, teks bahan & catatan meja kecil |
| **Owner / Manager** | Laptop / PC / Tablet | Pantau omzet harian live dari rumah, cek stok kritis, margin HPP (COGS), kelola menu | Navigasi menu banyak dan bertumpuk, grafik analitik kurang interaktif |

---

## 3. Arsitektur Informasi & Navigasi

```
[ KA POS v3.0 Navigation ]
│
├── 🔑 Auth & Quick Lock Screen (PIN 4-digit / Supabase Auth)
│
├── 🛒 POS (Penjualan Kasir)
│   ├── Filter Kategori & Search Bar
│   ├── Product Grid & Modifier Sheet (Level Pedas, Topping)
│   ├── Floating Cart (Mobile) / Persistent Sidebar Cart (Tablet)
│   └── Payment Sheet (Tunai, QRIS Dinamis, Transfer)
│
├── 🪑 Manajemen Meja & Pesanan Aktif (Dine-In Hub)
│   ├── Visual Grid Denah Meja (Kosong, Terisi, Billing)
│   ├── Detail Pesanan Meja (Add-on pesanan susulan, Split Bill)
│   └── Pindah Meja / Gabung Meja
│
├── 🍳 Kitchen Display System (KDS Dapur)
│   ├── Tiket Pesanan Realtime (Supabase CDC WebSocket)
│   ├── Filter Stasiun: Bakaran Sate | Gorengan Ayam | Bar Minuman
│   └── Audio Alert Chime & Urgency Color Timer
│
├── 💵 Manajemen Shift & Laci Kas (Cash Drawer)
│   ├── Buka Shift (Input Modal Awal)
│   ├── Arus Kas Masuk / Keluar Operasional (Petty Cash)
│   └── Tutup Shift (Blind Cash Count & Selisih Kasir)
│
├── 📦 Inventaris & Resep (BOM)
│   ├── Stok Bahan Baku & Indikator Kritis
│   ├── Resep Menu (Hubungan Bahan Baku ke Penjualan)
│   └── Mutasi Stok & Penyesuaian (Stock Opname)
│
├── 📈 Laporan & Analitik (Owner Hub)
│   ├── Ringkasan Penjualan Realtime & Grafik Omzet
│   ├── Laporan Laba Bersih & Margin HPP (COGS)
│   └── Export CSV, Excel, & Print Laporan A4
│
└── ⚙️ Pengaturan & Supabase Cloud
    ├── Koneksi Supabase (URL, Anon Key, Realtime Status)
    ├── Pengaturan Printer Thermal (Web Bluetooth ESC/POS)
    └── Info Kedai & Akun Staf
```

---

## 4. Spesifikasi Fungsional & Kebutuhan UI/UX Per Modul

### Modul 1: Autentikasi & Quick Lock
- **Tujuan:** Mengamankan akses kasir dan owner, memungkinkan perpindahan shift atau penguncian layar cepat di meja kasir.
- **Komponen UI:**
  - Form Login Utama (Email/Username + Password) terintegrasi dengan Supabase Auth.
  - **Quick Screen Lock (Keypad PIN 4-Digit):** Tombol gembok di pojok kanan atas. Kasir yang meninggalkan meja kasir cukup klik 1 tombol untuk mengunci. Buka kembali dengan PIN 4-digit tanpa perlu ketik ulang email/password panjang.
  - Role Indicator Pill (`Owner`, `Kasir`, `Dapur`) di pojok atas dengan warna distingtif.
- **Interaksi UX:**
  - Auto-lock setelah 15 menit tidak ada aktivitas sentuhan (dapat diatur di Pengaturan).
  - Akses fitur sensitif (Hapus Transaksi, Void, Laporan Keuangan, Reset Data) mewajibkan PIN Owner.

---

### Modul 2: Point of Sale (POS) — Layar Kasir

```
+-------------------------------------------------------------------------------+
| [🪑 Dine In] [🥡 Takeaway] [🛵 Ojol] | Meja: [ 04 ▼ ] | 🔍 Cari Menu...      |
+-----------------------------------------------------+-------------------------+
| [Semua] [🍢 Taichan] [🍗 Chicken] [🧊 Minum] [🍚 Nasi]| 🛒 PESANAN (Meja 04)    |
+-----------------------------------------------------+-------------------------+
| +--------------+ +--------------+ +---------------+ | 2x Taichan Pedas Lv.3   |
| | 🍢           | | 🍢           | | 🍗            | |    ↳ Lv.3, Ekstra Jeruk |
| | Taichan Pedas| | Taichan Ori  | | Chicken Crispy| |    Rp 36.000        [x] |
| | Rp 18.000    | | Rp 15.000    | | Rp 20.000     | | 1x Es Teh Manis         |
| | [Stok: 45]   | | [Stok: 30]   | | [Stok: 12]    | |    Rp 5.000         [x] |
| +--------------+ +--------------+ +---------------+ |-------------------------|
| +--------------+ +--------------+ +---------------+ | Subtotal:      Rp 41.000|
| | 🧊           | | 🍚           | | 🍳            | | Diskon (0%):        Rp 0|
| | Es Jeruk     | | Nasi Putih   | | Nasi Goreng   | | TOTAL:         Rp 41.000|
| | Rp 7.000     | | Rp 5.000     | | Rp 18.000     | |-------------------------|
| +--------------+ +--------------+ +---------------+ | [Checker] [BAYAR SEKARANG]|
+-------------------------------------------------------------------------------+
```

#### A. Header & Order Type
- **Segmented Control Bar:** Tombol besar bertoggle: `🪑 Makan di Tempat (Dine In)`, `🥡 Bungkus (Take Away)`, `🛵 Online (GoFood/Grab/Shopee)`.
- **Selector No. Meja:** Hanya muncul jika mode *Dine In* aktif. Menampilkan nomor meja aktif atau dropdown pilih meja.
- **Search Bar Responsif:** Auto-focus shortcut (`/`), pencarian instan debounce 150ms berdasarkan nama produk atau SKU.

#### B. Katalog Produk & Kategori
- **Kategori Horizontal Pill:** Tab kategori dengan ikon emoji, warna aksen, dan *counter badge* jumlah menu aktif. Mendukung sentuhan geser (*drag-scroll*).
- **Product Card Touch Target:**
  - Dimensi minimal kartu: 130px x 150px untuk kemudahan tap di tablet/HP.
  - Informasi kartu: Emoji/Foto produk, Nama produk (maks 2 baris), Harga dengan format jelas (`Rp 18.000`), indikator stok kritis jika < 5 porsi.
  - Animasi mikro: *Scale-down 0.97* saat ditekan (*tap active*), memberi feedback fisik instan pada kasir.

#### C. Modal Modifier & Varian Produk (Level Pedas & Topping)
- Saat produk diklik, jika memiliki varian, muncul **BottomSheet / Modal Dialog**:
  - **Tingkat Pedas:** Pilihan tombol radio horizontal: `Lv.0 (Original)` | `Lv.1` | `Lv.2` | `Lv.3` | `Lv.5 (+Rp 2.000)`.
  - **Topping / Add-on Tambahan (Checkbox):** `Ekstra Sambal (+Rp 3.000)`, `Ekstra Jeruk Nipis (+Rp 1.000)`, `Bawang Goreng (+Rp 2.000)`.
  - **Catatan Khusus (Quick Tags):** Tombol tap cepat: `Tanpa Micin`, `Bumbu Dipisah`, `Sedikit Es`, `Manis Sedang`, atau input manual.
  - Tombol aksi: `+ Tambah ke Keranjang — Rp xx.xxx`.

#### D. Keranjang Pesanan (Cart Drawer)
- **Smartphone View:** Menggunakan **Floating Bottom Bar** (seperti aplikasi GoFood/ShopeeFood):
  - Menampilkan ringkasan: `🛒 3 Item · Rp 41.000` dengan tombol hijau `Lanjut Bayar ➜`.
  - Tap untuk membuka keranjang layar penuh (*Full Bottom Sheet*).
- **Tablet & Desktop View:** Kolom permanen di sebelah kanan (lebar 340px–380px):
  - Stepper Quantity: Tombol `[-]` `[Qty]` `[+]` berukuran nyaman disentuh jari.
  - Swipe-to-delete di mobile untuk menghapus item.
  - Rincian biaya: Subtotal, Diskon (nominal/persen), Total Tagihan (font besar dengan warna kontras).

#### E. Modal Pembayaran (Checkout Flow)
- **Pilihan Metode Pembayaran:**
  1. **💵 Tunai (Cash):**
     - Numpad virtual & Tombol Cepat Uang Pas: `[Pas]`, `[Rp 20.000]`, `[Rp 50.000]`, `[Rp 100.000]`.
     - Tampilan kalkulasi kembalian otomatis dengan font raksasa hijau: `Kembalian: Rp 9.000`.
  2. **📱 QRIS:**
     - Menampilkan QRIS Toko (statis atau dinamis). Tombol checklist konfirmasi: `[✓ Pelanggan Telah Scan & Berhasil]`.
  3. **💳 Transfer Bank / EDC:**
     - Menampilkan nomor rekening BCA / Bank Toko + tombol salin nomor rekening.
- **Aksi Selesai:**
  - Tombol ganda: `🖨️ Cetak Struk (Bluetooth / Thermal)` & `✓ Selesai Tanpa Struk`.
  - Opsi: `Kirim Struk via WhatsApp` (input nomor HP pelanggan untuk auto-generate link WhatsApp).

---

### Modul 3: Visual Table Management (Dine-In Hub)
- **Tujuan:** Mengelola meja restoran F&B Taichan agar pelayan dan kasir tahu meja mana yang kosong, sedang makan, atau menunggu tagihan.
- **Komponen UI:**
  - **Grid Denah Meja Visual:**
    - Kotak meja dengan nomor besar: `M01`, `M02`, `M03`, dst.
    - **Status Warna Meja:**
      - 🟢 **Hijau (Kosong):** Siap ditempati tamu baru.
      - 🟡 **Kuning (Sedang Masak):** Order baru dikirim ke dapur.
      - 🔵 **Biru (Makanan Tersaji):** Tamu sedang menikmati makanan.
      - 🟠 **Oranye (Minta Tagihan / Billing):** Tamu siap membayar.
  - **Pesanan Susulan (Add-on Order):** Kasir cukup tap Meja 03 yang sedang aktif, klik `+ Tambah Pesanan`, dan pesanan baru langsung terhubung ke bill meja tersebut serta otomatis mencetak tiket tambahan ke dapur.
  - **Fitur Pindah Meja / Gabung Meja:** Modal geser meja (misal: tamu Meja 02 pindah ke Meja 05).

---

### Modul 4: Kitchen Display System (KDS / Layar Dapur)

```
+-------------------------------------------------------------------------------+
| 🍳 KITCHEN DISPLAY (Dapur) | [Semua: 5] [🍢 Bakaran: 3] [🍗 Gorengan: 2] | 19:42 |
+-------------------------------------------------------------------------------+
| +-------------------------+ +-------------------------+ +-------------------+ |
| | MEJA 04    #ORD-2609-001| | MEJA 02    #ORD-2609-002| | TAKEAWAY #ORD-003 | |
| | ⏱️ 04:12 mnt   [Dine-In] | | ⏱️ 12:45 mnt (URGENT!) | | ⏱️ 01:20 mnt      | |
| |-------------------------| |-------------------------| |-------------------| |
| | • 2x Taichan Pedas Lv.3 | | • 1x Chicken Crispy     | | • 3x Taichan Ori  | |
| |   ↳ Ekstra Sambal       | | • 1x Nasi Goreng        | | • 2x Es Jeruk     | |
| | • 1x Nasi Putih         | |   ↳ Pedas Sedang        | |                   | |
| |-------------------------| |-------------------------| |-------------------| |
| | [ ▶ MULAI MASAK ]       | | [ ✓ SIAP SAJI / SELESAI]| | [ ▶ MULAI MASAK ] | |
| +-------------------------+ +-------------------------+ +-------------------+ |
+-------------------------------------------------------------------------------+
```

- **Tujuan:** Menggantikan kertas bon dapur yang mudah hilang/kotor dengan layar tablet interaktif di area memasak.
- **Komponen UI & Fitur:**
  - **Realtime Push Ticket:** Tiket muncul seketika saat kasir mengonfirmasi pesanan (menggunakan *Supabase Realtime PostgreSQL CDC*).
  - **Audio Chime / Bell Sound:** Suara bel notifikasi berdentang setiap kali tiket baru masuk (dapat di-toggle mute).
  - **Urgency Aging Timer:**
    - 🟢 Hijau: 0 – 7 menit (Aman).
    - 🟡 Kuning: 8 – 14 menit (Perhatian).
    - 🔴 Merah Berkedip: ≥ 15 menit (Kritis / Pelanggan menunggu lama).
  - **Stasiun Filter Tabs:**
    - `Semua Pesanan`
    - `🍢 Bakaran (Sate Taichan)`
    - `🍗 Gorengan (Ayam Crispy)`
    - `🧊 Bar Minuman`
  - **Aksi 1-Tap:**
    - Tombol `Mulai Masak` (Status berubah menjadi *Cooking*, kasir depan melihat status meja berubah).
    - Tombol `Siap Saji / Selesai` (Tiket hilang dari antrian aktif dapur dan berpindah ke riwayat).

---

### Modul 5: Manajemen Shift & Laci Kas (Cash Drawer)
- **Tujuan:** Mencegah kecurangan, selisih uang kas fisik, dan memastikan uang di laci kasir klop 100% setiap pergantian shift.
- **Komponen UI & Workflow:**
  1. **Buka Shift (Start Shift):**
     - Kasir login dan memasukkan `Modal Awal Kas Fisik` (misal: Rp 150.000 untuk uang kembalian).
     - Laci kas tercatat aktif atas nama kasir tersebut dengan timestamp presisi.
  2. **Pencatatan Petty Cash (Kas Masuk / Keluar Operasional):**
     - Form cepat: Pilihan `Kas Masuk` atau `Kas Keluar`, Nominal, Keterangan (misal: "Beli Es Batu Darurat", "Galon Air Mineral").
  3. **Tutup Shift (Blind Drop Cash Count):**
     - **Prinsip UX:** Kasir **TIDAK** diperlihatkan estimasi sistem terlebih dahulu!
     - Kasir diminta menghitung uang tunai fisik di laci dan mengetikkan nominalnya:
       - Lembar Rp 100.000: x [ ... ]
       - Lembar Rp 50.000:  x [ ... ]
       - Lembar Rp 20.000:  x [ ... ]
       - Uang Koin:         x [ ... ]
     - Setelah kasir klik `Konfirmasi Tutup Shift`, sistem membuka perbandingan:
       - Estimasi Sistem (Hanya Tunai: Modal + Omzet Tunai + In - Out)
       - Uang Fisik Input Kasir
       - **Varian Selisih:** `Sesuai (Rp 0)` / `Kurang (-Rp 10.000)` / `Lebih (+Rp 5.000)`.
  4. **Cetak X-Report & Z-Report:**
     - Cetak rekap shift ke printer thermal dan tombol kirim otomatis ringkasan ke WhatsApp Owner.

---

### Modul 6: Inventaris & Resep Menu (Bill of Materials)
- **Tujuan:** Memastikan stok bahan mentah (Daging Ayam, Bumbu, Kemasan) terpotong otomatis setiap kali menu terjual.
- **Komponen UI:**
  - **Tabel Bahan Baku:** Kolom Nama, Kategori, Satuan (kg, pcs, pak, porsi), Stok Saat Ini, Batas Minimum, Harga Beli.
  - **Badge Status Stok:**
    - `🟢 Normal` (Stok > Min)
    - `🟡 Menipis` (Stok <= Min)
    - `🔴 Habis` (Stok 0)
  - **Modal Editor Resep (BOM):**
    - Saat mengedit produk "Taichan Original", terdapat tab `Resep Bahan Baku`:
      - 1 Porsi = `0.15 kg Daging Ayam Fillet` + `0.10 pak Bumbu Taichan`.
    - Sistem menghitung estimasi **HPP (Harga Pokok Penjualan)** otomatis berdasarkan harga beli bahan baku!

---

### Modul 7: Laporan & Analitik Keuangan (Owner Dashboard)
- **Tujuan:** Memberikan wawasan bisnis mendalam kepada owner dalam format visual yang memanjakan mata dan mudah dipahami.
- **Komponen UI:**
  - **Kartu KPI Utama:** Total Omzet Hari Ini, Total Transaksi, Laba Kotor (Gross Profit), Estimasi Laba Bersih, Rata-rata Nilai Order (*Average Basket Size*).
  - **Grafik Tren Penjualan:** Garis tren omzet per jam (mendeteksi jam sibuk) dan grafik batang omzet 7 hari / 30 hari terakhir.
  - **Top 5 Menu Terlaris:** Daftar ranking produk terlaris lengkap dengan kontribusi profit.
  - **Pemisahan Metode Pembayaran:** Diagram pie pembagian omzet: `💵 Tunai (40%)`, `📱 QRIS (50%)`, `💳 Transfer (10%)`.
  - **Filter Periode:** `Hari Ini`, `Kemarin`, `7 Hari Terakhir`, `Bulan Ini`, atau `Pilih Rentang Tanggal`.
  - **Export Center:** Download laporan dalam format PDF Siap Cetak (A4), Excel / CSV.

---

### Modul 8: Pengaturan Toko & Integrasi Printer Bluetooth Thermal
- **Tujuan:** Konfigurasi toko fleksibel dan pencetakan struk langsung tanpa dialog browser yang mengganggu.
- **Komponen UI:**
  - **Web Bluetooth Printer Scanner:**
    - Tombol `🔍 Cari Printer Bluetooth`.
    - Daftar perangkat printer thermal yang ditemukan (misal: `RPP02N`, `VSC MP-58`, `Panda PRJ-58D`).
    - Tombol `Test Cetak Struk`.
  - **Opsi Lebar Kertas:** Tombol radio `58 mm` (standar kasir mobile) atau `80 mm`.
  - **Profil Struk:** Header nama kedai, alamat, nomor WhatsApp, catatan kaki struk (*footer*).

---

## 5. Design System & UI Tokens

### 5.1 Skema Warna (Color Palette)
Aplikasi mengusung tema **Modern Cyber F&B Dark Mode** sebagai tema utama (cocok untuk operasional kafe/kedai malam) dengan dukungan **Clean Light Mode**.

```css
:root {
  /* Brand Accent */
  --color-primary: #F97316;       /* Flame Orange — Warna utama brand Taichan */
  --color-primary-hover: #EA580C; /* Deep Flame */
  --color-primary-light: rgba(249, 115, 22, 0.12);

  /* Backgrounds & Surfaces (Dark Mode) */
  --bg-app: #0B0F19;              /* Deep Obsidian */
  --bg-surface: #151D2F;          /* Card & Sidebar Slate */
  --bg-surface-elevated: #1E293B; /* Modal & Popover */
  --border-subtle: rgba(255, 255, 255, 0.08);
  --border-strong: rgba(255, 255, 255, 0.16);

  /* Text & Typography */
  --text-main: #F8FAFC;           /* High contrast white */
  --text-muted: #94A3B8;          /* Slate Grey */
  --text-dim: #64748B;

  /* Semantic Feedback */
  --color-success: #10B981;       /* Hijau transaksi & meja kosong */
  --color-warning: #F59E0B;       /* Kuning proses masak */
  --color-danger: #EF4444;        /* Merah void & stok kritis */
  --color-info: #3B82F6;          /* Biru QRIS / non-tunai */
}
```

### 5.2 Tipografi
- **Font Display & UI:** `'Plus Jakarta Sans', -apple-system, sans-serif`  
  *Alasan:* Karakter font modern, geometric, dan memiliki tingkat keterbacaan tinggi pada layar ponsel atau tablet.
- **Font Angka & Struk:** `'DM Mono', monospace`  
  *Alasan:* Monospaced tabular figures memastikan susunan harga, nominal rupiah, dan kuantitas selalu rata dan tidak bergeser saat angka bertambah.

### 5.3 Komponen Interaktif & Aturan Aksesibilitas (A11y)
1. **Minimum Touch Target:** Setiap tombol aksi kasir memiliki ukuran minimal **44px x 44px** (rekomendasi Apple HIG & Google Material) untuk mencegah salah tekan jari (*fat-finger errors*).
2. **Kontras Warna Tinggi:** Rasio kontras teks terhadap latar belakang minimal **4.5:1 (WCAG AA)**, menjamin layar tetap terbaca jelas di bawah pencahayaan lampu kedai remang-remang maupun di luar ruangan.
3. **Sound & Haptic Feedback:** Getaran mikro (*vibrate*) pada smartphone saat scan/tap item menu, dan denting audio saat pesanan masuk di layar dapur.

---

## 6. Matrix Perbandingan: Sebelum vs Sesudah Redesign UI/UX

| Aspek | UI/UX Lama (v2.0) | UI/UX Baru (v3.0) |
|---|---|---|
| **Layout Mobile** | Cart memakan 50% layar bawah, sempit, scroll bertabrakan | Floating Cart Drawer ala GoFood, full menu view, navigasi jempol bawah |
| **Pilihan Level Pedas** | Nama produk dipecah (`Lv.1`, `Lv.2`, `Lv.3`) | 1 Menu Produk + BottomSheet Modifier Level & Topping dinamis |
| **Visualisasi Meja** | Teks dropdown biasa, tidak ada indikator status | Grid Denah Meja Interaktif dengan 4 warna status & fitur add-on order |
| **Dapur (KDS)** | Tabel statis, refresh manual 10 detik, tanpa audio | WebSocket Realtime Push, timer penuaan warna, alert denting bel suara |
| **Tutup Shift Kasir** | Omzet non-tunai masuk ke laci fisik (selisih minus) | Formula kas laci akurat + Blind Cash Count (hitung uang fisik tanpa contekan) |
| **Koneksi Cloud** | Konfigurasi Firebase tidak lengkap, rawan tabrakan ID | Supabase PostgreSQL terintegrasi, UUID v4, RLS security, Realtime CDC |
| **Cetak Struk** | `window.print()` browser biasa, setup bluetooth fiktif | Web Bluetooth ESC/POS engine murni, langsung tembak printer thermal 58/80mm |

---

## 7. Roadmap & Fase Implementasi UI/UX

```
[ Fase 1: Core POS & Layout Baru ] ➔ [ Fase 2: Meja & KDS Realtime ] ➔ [ Fase 3: Shift Kas & Resep ] ➔ [ Fase 4: Bluetooth & Vercel Deploy ]
  - Shell App & Responsive Nav         - Denah Meja Visual                - Blind Drop Shift Management      - Web Bluetooth ESC/POS Engine
  - Product Card & Modifier Sheet      - Supabase Realtime KDS            - Recipe BOM Auto-Deduct           - PWA Audit & Offline IndexedDB
  - Floating Cart & Quick Pay          - Audio Chime Dapur                - Owner Analytics Dashboard        - Production Deployment di Vercel
```

1. **Fase 1 — Desain Fondasi & Core POS (Sprint 1):**
   - Pembuatan shell aplikasi adaptif (Desktop Sidebar + Mobile Bottom Nav).
   - Implementasi grid katalog menu modern dan *BottomSheet Modifier Level Pedas & Topping*.
   - Floating Cart Drawer dan modal checkout cepat (uang pas & QRIS).
2. **Fase 2 — Visual Table Hub & Kitchen Display System (Sprint 2):**
   - Grid visual denah meja dengan status warna.
   - Layar KDS Dapur terhubung ke *Supabase Realtime Channel* dengan audio alert.
3. **Fase 3 — Manajemen Kas Presisi & Resep Inventaris (Sprint 3):**
   - Alur *Blind Cash Drop* saat tutup shift dan kalkulasi uang fisik akurat.
   - Halaman manajemen resep bahan baku (BOM) dan pengurangan stok otomatis.
4. **Fase 4 — Web Bluetooth Printer & Vercel Production (Sprint 4):**
   - Integrasi Web Bluetooth API untuk printer thermal kasir 58mm/80mm.
   - PWA Service Worker caching final dan deployment ke Vercel.
