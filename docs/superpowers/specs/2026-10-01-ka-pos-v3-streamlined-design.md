# KA POS v3.0: Streamlined Specification & Architecture (No Dapur, No Meja)

- **Project:** KA POS v3.0 — Culinary Velocity POS for KA Taichan & Chicken
- **Platform:** Progressive Web App (PWA) + LocalStorage Offline-First + Supabase Backend
- **Design System:** Stitch Design System (Modern F&B Dark Mode)
- **Date:** 2026-10-01
- **Status:** Approved for Implementation

---

## 1. Executive Summary & Scope Adjustments

Per user instructions, the following architectural simplifications are applied:
1. **Fitur Dapur (KDS) Dihapus:**
   - Hapus tab navigasi Dapur dari Sidebar Desktop dan Mobile Bottom Dock.
   - Hapus section view `#view-kds` dan script `js/views/kds.js`.
   - Hapus antrean tiket dapur di local state dan Supabase dispatch.
2. **Fitur Meja (Table Management) Dihapus:**
   - Hapus selector meja (`pos-table-selector-bar` & `pos-table-selector-bar-cart`), tombol quick table (`M-01`, `M-04`, `M-08`, `M-12`), dan input nomor meja.
   - Order types disederhanakan murni menjadi: **Dine In**, **Bungkus (Take Away)**, dan **Ojol (Online Delivery)** tanpa penomoran meja.
3. **Fitur Lain Disempurnakan Sesuai PRD_UIUX.md:**
   - **Modul 1: Autentikasi & Quick Lock:** 4-digit PIN lockpad, auto-lock inactivity, role indicator (Owner/Kasir).
   - **Modul 2: POS Kasir:**
     - Modifier modal lengkap: Pilihan Level Pedas (Lv.0 s/d Lv.5), Checkbox Toppings berbayar (Ekstra Sambal, Jeruk Nipis, Bawang Goreng), dan Quick Tags Catatan Khusus (Tanpa Micin, Bumbu Dipisah, Sedikit Es, Manis Sedang).
     - Kalkulasi harga dinamis di modal modifier.
     - Checkout Cepat: Uang Pas & chip pecahan (20k, 50k, 100k, 200k), kalkulator kembalian instan, QRIS checklist, transfer bank, dan tombol kirim struk WhatsApp.
   - **Modul 5: Manajemen Shift & Laci Kas:**
     - Modal buka shift kasir.
     - Form pencatatan Petty Cash (Kas Masuk & Kas Keluar operasional).
     - **Blind Cash Drop:** Input lembaran fisik (100k, 50k, 20k, 10k, 5k, 2k, 1k, koin) tanpa melihat estimasi sistem -> buka komparasi sistem vs fisik -> audit selisih kas.
     - Cetak X-Report & Z-Report ke printer thermal.
   - **Modul 6: Inventaris & Resep Menu (BOM):**
     - Tabel Bahan Baku: Stok, Batas Kritis, Harga Beli, Nilai Aset.
     - Tab / Editor Resep Menu (Bill of Materials): Pemetaan bahan baku ke menu porsi.
     - Kalkulasi otomatis HPP (COGS) & margin profit %.
     - **Auto-deduct stok bahan baku** saat transaksi POS selesai.
   - **Modul 7: Laporan & Analitik Keuangan (Owner Dashboard):**
     - KPI cards: Omzet Hari Ini, Total Transaksi, Laba Kotor, Laba Bersih, Rata-rata Nilai Order (Avg Basket Size).
     - Grafik tren omzet 7 hari / 30 hari & jam sibuk (Chart.js).
     - Top 5 menu terlaris dan kontribusi profit.
     - Pie chart metode pembayaran (Tunai, QRIS, Transfer).
     - Filter periode (Hari Ini, Kemarin, 7 Hari, Bulan Ini).
     - Export CSV & Cetak format A4 siap print.
   - **Modul 8: Pengaturan Toko & Printer Bluetooth:**
     - Web Bluetooth printer scanner & ESC/POS direct print.
     - Profil toko, rekening pembayaran, dan footer struk.

---

## 2. Navigasi & Struktur View

### Desktop Sidebar (88px)
1. **POS** (`data-view="pos"`) — Kasir 3-Zone Velocity (Katalog, Kategori, Keranjang).
2. **Shift** (`data-view="shift"`) — Buka/Tutup shift, Petty Cash, Blind Cash Drop.
3. **Stok** (`data-view="inventory"`) — Bahan Baku & Resep Menu BOM *(Owner-only)*.
4. **Laporan** (`data-view="reports"`) — Analitik Omzet, Grafik, Laba Bersih *(Owner-only)*.
5. **Setting** (`data-view="settings"`) — Bluetooth Printer, Toko, Supabase *(Owner-only)*.
6. **Kunci** (`lock-item`) — Kunci layar cepat (PIN 4-digit).

### Mobile Bottom Dock (64px)
1. **POS** (`data-view="pos"`)
2. **Shift** (`data-view="shift"`)
3. **Stok** (`data-view="inventory"`, Owner-only)
4. **Laporan** (`data-view="reports"`, Owner-only)
5. **Setting** (`data-view="settings"`, Owner-only)

---

## 3. Spesifikasi Teknis Implementasi

### 3.1 Penghapusan Dapur (KDS) & Meja
- **index.html:**
  - Hapus elemen `#view-kds`, navigasi `kds`, dan script tag `js/views/kds.js`.
  - Hapus `#pos-table-selector-bar` dan `#pos-table-selector-bar-cart`.
- **js/app.js:** Hapus listener dan render `kds`.
- **sw.js:** Hapus cache entry `js/views/kds.js`.
- **js/views/payment.js:** Hapus dispatch tiket ke `kitchenOrders`.

### 3.2 Modul POS & Modifier (js/views/pos.js)
- Modal modifier menyertakan:
  - Level pedas: `Lv.0 (Ori)`, `Lv.1 (Sedang)`, `Lv.2 (Pedas)`, `Lv.3 (Nendang)`, `Lv.4 (Gahar)`, `Lv.5 (Mampus) [+Rp 2.000]`.
  - Checkbox Toppings: `Ekstra Sambal (+Rp 3.000)`, `Ekstra Jeruk Nipis (+Rp 1.000)`, `Bawang Goreng (+Rp 2.000)`.
  - Tombol Quick Tags: `Tanpa Micin`, `Bumbu Dipisah`, `Sedikit Es`, `Manis Sedang`.
  - Kalkulasi total harga item di footer modal secara real-time.
  - Menghapus ketergantungan meja pada keranjang.

### 3.3 Modul Resep BOM & Auto-Deduct (js/state.js & js/views/inventory.js)
- Menyimpan definisi `recipes` di `window.State`:
  - `Taichan Pedas`: Daging Ayam 0.15 kg, Bumbu Taichan 0.1 pak.
  - `Chicken Crispy`: Daging Ayam 0.20 kg, Minyak Goreng 0.05 liter.
  - `Es Teh Manis`: Gelas Cup 1 pcs, Es Batu 0.2 kg.
  - `Es Jeruk`: Gelas Cup 1 pcs, Es Batu 0.2 kg.
- Menghitung HPP otomatis: `HPP = sum(bahan.hr * bom.qty)`.
- Mengurangi stok bahan baku saat `processPayment()` berhasil di `payment.js`.
- Antarmuka Tab di `#view-inventory`:
  - Tab 1: **Daftar Bahan Baku** (Stok, Min, Harga Beli, Nilai Aset).
  - Tab 2: **Resep Menu & HPP** (Daftar Menu, Bahan Resep, Estimasi HPP, Margin Profit).

### 3.4 Modul Laporan Owner (js/views/reports.js)
- Kartu KPI: Omzet, Transaksi, HPP/COGS, Laba Kotor, Rata-rata Keranjang (Basket Size).
- Filter waktu: `Hari Ini`, `Kemarin`, `7 Hari`, `Bulan Ini`.
- Chart omzet per jam / per hari.
- Pie chart metode pembayaran (Tunai, QRIS, Transfer).
- Top 5 menu terlaris.
- Tombol cetak dokumen A4 dan export CSV.

### 3.5 Modul Shift Kasir (js/views/shift.js)
- Buka Shift (Modal Awal).
- Petty cash log (Masuk/Keluar).
- Blind Drop Denominasi (100k, 50k, 20k, 10k, 5k, 2k, 1k, koin).
- Rekap selisih fisik vs sistem.
- Cetak X-Report & Z-Report ke printer bluetooth thermal.
