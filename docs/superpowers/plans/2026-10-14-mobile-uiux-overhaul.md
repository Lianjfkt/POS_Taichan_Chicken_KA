# Mobile UI/UX Overhaul & Stacked Card Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Menghilangkan seluruh elemen tumpang tindih/tabrakan (*collision*), luapan horizontal (*overflow*), dan touch target sempit pada antarmuka mobile smartphone (360px – 480px) dan tablet (≤ 768px), serta mentransformasikan tabel multi-kolom menjadi *stacked card layout* yang mulus dioperasikan satu tangan.

**Architecture:** Pure CSS responsive architecture terpusat di `css/mobile.css` dengan media query `max-width: 768px` dan `max-width: 480px`. Memanfaatkan CSS flex/grid reflow untuk merestrukturisasi POS Top Bar, CSS card-transformation untuk elemen `.data-table` tanpa mengubah JavaScript data logic, serta perbaikan selector modal `.modal-card` dan touch targets (min 36px–48px).

**Tech Stack:** Vanilla CSS3 (CSS Grid, Flexbox, Safe Area Insets `env()`, Touch Targets), HTML5, JavaScript ES6 (KA POS Architecture).

## Global Constraints
- Seluruh perbaikan mobile WAJIB terisolasi di dalam media query `max-width: 768px` (dan `max-width: 480px` untuk ekstra sempit) sehingga tampilan desktop/web tetap 100% normal tanpa perubahan visual/distorsi.
- Tidak boleh merusak event listeners, id, atau atribut data-* yang dibutuhkan oleh `State`, `POSView`, `OrdersView`, `StockTracker`, `ShiftView`, `PaymentView`, dan `SupabaseService`.
- Tombol aksi mobile harus memiliki ukuran sentuh minimal: tombol kuantitas `36px x 36px`, tombol aksi biasa min `40px` tinggi, dan keypad pembayaran min `46px` tinggi.
- Hindari horizontal scrolling pada container utama (`overflow-x: hidden`).

---

### Task 1: POS Terminal & Top Bar Responsive Layout

**Files:**
- Modify: `css/mobile.css:215-252`
- Modify: `index.html:410-458` (menambahkan kelas semantik pendukung tanpa merubah id/onclick)

**Interfaces:**
- Consumes: `#pos-top-bar`, `.pos-search-wrapper`, `.order-type-group`, `#pos-product-grid`
- Produces: 2-baris layout terstruktur pada POS Top Bar (Baris 1: Full-width search; Baris 2: Order switcher + quick action buttons)

- [ ] **Step 1: Berikan class semantik pada kontainer POS Top Bar di index.html**
  Buka `index.html` baris 411-458. Pastikan kontainer kiri memiliki class `pos-top-left` dan kontainer kanan memiliki class `pos-top-right`, serta ketiga tombol cepat (`Update Stok`, `Fast Stock`, `Riwayat`) memiliki class `btn-pos-quick`.

- [ ] **Step 2: Terapkan aturan CSS flex 2-baris di mobile.css**
  Di `css/mobile.css`, perbarui `.pos-top-bar`, `.pos-top-left`, `.pos-top-right`, `.pos-search-wrapper`, dan `.btn-pos-quick`:
  - `.pos-top-bar`: `flex-direction: column !important; align-items: stretch !important; padding: 8px 10px !important; gap: 8px !important;`
  - `.pos-top-right`: `display: flex !important; flex-direction: column !important; width: 100% !important; gap: 8px !important;`
  - `.pos-search-wrapper`: `width: 100% !important; max-width: none !important; order: -1 !important;`
  - `.pos-top-actions`: `display: flex !important; gap: 6px !important; width: 100% !important; align-items: center !important;`
  - `.order-type-group`: `flex: 1 !important; width: auto !important;`
  - `.btn-pos-quick`: `padding: 7px 10px !important; height: 36px !important; flex-shrink: 0 !important;`

- [ ] **Step 3: Sesuaikan padding bawah katalog menu**
  Pastikan `.catalog-grid` memiliki `padding: 10px 10px 120px !important;` dan kartu produk `.product-card` memiliki `min-height: 94px !important;` dengan judul produk dibatasi 2 baris (`-webkit-line-clamp: 2`).

- [ ] **Step 4: Verifikasi tampilan POS Top Bar**
  Periksa kode untuk memastikan search input tidak terjepit oleh tombol aksi cepat dan order switcher tampil proporsional.

- [ ] **Step 5: Commit**
  ```bash
  git add index.html css/mobile.css
  git commit -m "feat(mobile): overhaul pos top bar layout and catalog grid"
  ```

---

### Task 2: Floating Cart Pill & Cart Drawer Touch Optimization

**Files:**
- Modify: `css/mobile.css:326-406`

**Interfaces:**
- Consumes: `.mobile-floating-cart`, `.mobile-cart-drawer-card`, `.cart-item`, `.qty-btn`
- Produces: Elevated floating cart pill di atas bottom dock dan touch-friendly counter button (36px).

- [ ] **Step 1: Perbarui posisi & elevasi Floating Cart Pill**
  Di `css/mobile.css`, pastikan `.mobile-floating-cart`:
  - `bottom: calc(var(--mobile-nav-h, 60px) + 8px + env(safe-area-inset-bottom, 0px)) !important;`
  - `z-index: 45 !important;`
  - `height: 52px !important;`

- [ ] **Step 2: Perbesar tombol kuantitas keranjang (.qty-btn)**
  Tambahkan aturan mobile untuk `.cart-item` di dalam drawer:
  ```css
  .mobile-cart-drawer-items .cart-item {
    padding: 12px !important;
    border-radius: var(--radius-lg, 16px) !important;
  }
  .mobile-cart-drawer-items .cart-qty-ctrl {
    padding: 3px 8px !important;
    gap: 8px !important;
  }
  .mobile-cart-drawer-items .qty-btn {
    width: 36px !important;
    height: 36px !important;
    font-size: 18px !important;
  }
  .mobile-cart-drawer-items .qty-val {
    font-size: 15px !important;
    min-width: 24px !important;
  }
  .mobile-cart-drawer-items .cart-item-delete {
    padding: 8px !important;
    font-size: 20px !important;
    min-width: 36px !important;
    min-height: 36px !important;
    display: inline-flex !important;
    align-items: center !important;
    justify-content: center !important;
  }
  ```

- [ ] **Step 3: Pastikan Sticky Footer Cart Drawer nyaman ditekan**
  Pastikan `.btn-drawer-pay` memiliki tinggi `52px !important; font-size: 15px !important; font-weight: 800 !important; border-radius: var(--radius-lg, 14px) !important;`.

- [ ] **Step 4: Commit**
  ```bash
  git add css/mobile.css
  git commit -m "feat(mobile): optimize floating cart and drawer touch targets"
  ```

---

### Task 3: Generic Data Table Stacked Card Transformation & Orders View

**Files:**
- Modify: `css/mobile.css`

**Interfaces:**
- Consumes: `table.data-table`, `#orders-table-tbody`, `#view-orders`
- Produces: Stacked card layout otomatis untuk seluruh baris nota pesanan.

- [ ] **Step 1: Tulis aturan dasar Stacked Card untuk .data-table di mobile.css**
  ```css
  table.data-table,
  table.data-table tbody {
    display: block !important;
    width: 100% !important;
  }
  table.data-table thead {
    display: none !important;
  }
  table.data-table tr {
    display: block !important;
    width: 100% !important;
    background: var(--surface-container) !important;
    border: 1px solid rgba(255, 255, 255, 0.08) !important;
    border-radius: var(--radius-lg, 16px) !important;
    padding: 14px !important;
    margin-bottom: 12px !important;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.25) !important;
  }
  table.data-table td {
    display: block !important;
    width: 100% !important;
    padding: 4px 0 !important;
    border: none !important;
    text-align: left !important;
    max-width: none !important;
  }
  ```

- [ ] **Step 2: Styling khusus untuk baris kartu Riwayat Transaksi (#view-orders)**
  - Nomor nota & waktu: ditata sebagai header kartu dengan font monospace dan tanggal.
  - Pelanggan & tipe order (Dine In / Bungkus): disusun rapi dengan badge.
  - Item menu: list item dengan icon SVG dan kuantitas oranye tebal.
  - Total nominal: nominal besar (`font-size: 16px; font-weight: 800;`) di baris tersendiri.
  - Tombol aksi (`Struk`, `Detail`, `Void`): ditata menjadi tombol lebar penuh atau flex-wrap dengan tinggi minimal `38px` dan touch area lega:
  ```css
  #orders-table-tbody td:last-child {
    margin-top: 10px !important;
    padding-top: 10px !important;
    border-top: 1px solid rgba(255, 255, 255, 0.06) !important;
  }
  #orders-table-tbody td:last-child > div {
    display: flex !important;
    gap: 8px !important;
    width: 100% !important;
  }
  #orders-table-tbody td:last-child button {
    flex: 1 !important;
    height: 40px !important;
    justify-content: center !important;
    font-size: 12px !important;
  }
  ```

- [ ] **Step 3: Filter & Period Selector pada Riwayat Pesanan**
  Ubah filter bar `#view-orders .panel-card` menjadi 1 kolom vertikal rapi, dan period button (`Hari Ini`, `Kemarin`, `7 Hari`, dll.) menjadi baris horizontal dengan `overflow-x: auto` yang rapi.

- [ ] **Step 4: Commit**
  ```bash
  git add css/mobile.css
  git commit -m "feat(mobile): transform orders table to stacked card layout"
  ```

---

### Task 4: Stock Tracker & Inventory Responsive Stacked Cards

**Files:**
- Modify: `css/mobile.css`

**Interfaces:**
- Consumes: `#st-taichan-content`, `#st-chicken-content`, `#inventory-raw-tab`, `#inventory-sambal-tab`
- Produces: Stacked cards untuk batch Taichan, potongan ayam, dan bahan baku.

- [ ] **Step 1: Card layout untuk Batch Stok Taichan**
  Ubah tabel batch Taichan (`#st-taichan-content table.data-table tr`) menjadi kartu batch:
  - Baris atas: Tanggal & Badge Kelayakan (*Fresh / Kadaluarsa*).
  - Baris tengah: Jumlah Tusuk Mentah besar (`font-size: 18px; font-weight: 800; color: var(--on-surface);`).
  - Baris bawah: Catatan/notes dan tombol Hapus Batch yang mudah disentuh.

- [ ] **Step 2: Card layout untuk Ayam Potong (Chicken Parts)**
  - Pastikan kartu bagian ayam (Sayap, Paha Atas, Paha Bawah, Dada) tertata dalam grid 1 atau 2 kolom yang tidak meluap.
  - Tabel batch ayam di bawahnya bertransformasi menjadi kartu dengan input kuantitas goreng (`input[type="number"]`) berukuran minimal `44px` tinggi agar mudah diketik kasir.

- [ ] **Step 3: Card layout untuk Bahan Baku & Sambal**
  Ubah tabel bahan baku fisik dan tabel variasi sambal menjadi stacked card dengan indikator stok dan tombol edit/hapus di kanan/bawah.

- [ ] **Step 4: Subtab navigasi inventaris**
  Jadikan subtab (*Stock Harian*, *Bahan Baku*, *Variasi Sambal*, dll.) horizontal swipeable tanpa scrollbar yang mengganggu.

- [ ] **Step 5: Commit**
  ```bash
  git add css/mobile.css
  git commit -m "feat(mobile): transform stock tracker and inventory tables to cards"
  ```

---

### Task 5: Secondary Views Reflow (Reports, Shift, Customers, Settings, Menu)

**Files:**
- Modify: `css/mobile.css`

**Interfaces:**
- Consumes: `#view-reports`, `#view-shift`, `#view-customers`, `#view-settings`, `#view-menu`
- Produces: 1-kolom responsive flow tanpa horizontal overflow pada seluruh modul sekunder.

- [ ] **Step 1: Responsive reflow pada Laporan & Analitik (#view-reports)**
  - Ubah kontainer chart dan top products `grid-template-columns: 3fr 2fr` menjadi `grid-template-columns: 1fr !important; gap: 14px !important;`.
  - Ubah bagian Peak Hours & Channel Bayar `repeat(auto-fit, minmax(360px, 1fr))` menjadi `grid-template-columns: 1fr !important;`.
  - Ubah KPI grid menjadi `grid-template-columns: repeat(2, 1fr) !important; gap: 8px !important;`.

- [ ] **Step 2: Responsive reflow pada Shift Kasir (#view-shift)**
  - Tata 4 tombol aksi shift (*Kas Masuk, Kas Keluar, Cetak X-Report, Tutup Shift*) menjadi grid 2x2 yang simetris (`grid-template-columns: 1fr 1fr; gap: 8px;`).
  - Tata kartu saldo metrik menjadi 2x2 grid.
  - Ubah tabel arus kas dan riwayat tutup shift menjadi stacked cards.

- [ ] **Step 3: Responsive reflow pada CRM Member (#view-customers)**
  - Ubah KPI member menjadi grid 2-kolom atau 1-kolom.
  - Ubah tabel member menjadi stacked card dengan nomor WA yang jelas dan saldo poin mencolok.

- [ ] **Step 4: Responsive reflow pada Pengaturan (#view-settings) & Menu (#view-menu)**
  - Ubah grid kartu pengaturan `minmax(320px, 1fr)` menjadi 1 kolom vertikal (`grid-template-columns: 1fr !important;`).
  - Ubah grid produk kelola menu menjadi 2-kolom proporsional.

- [ ] **Step 5: Berikan safe bottom padding pada seluruh scrollable views**
  ```css
  .view-pane.view-scroll {
    padding: 12px 12px calc(var(--mobile-nav-h, 60px) + 24px) !important;
  }
  ```

- [ ] **Step 6: Commit**
  ```bash
  git add css/mobile.css
  git commit -m "feat(mobile): reflow reports, shift, customers, and settings to responsive single-column"
  ```

---

### Task 6: Modal Dialogs & Payment Keypad Touch Optimization

**Files:**
- Modify: `css/mobile.css`

**Interfaces:**
- Consumes: `.modal-card`, `.modal-box`, `#payment-modal`, `.quick-cash-chip`, `#pay-section-cash`
- Produces: Modal responsif pas di layar HP dengan keypad pembayaran yang lega (min 46px).

- [ ] **Step 1: Koreksi selector modal di mobile.css**
  Hapus aturan `.modal-dialog` yang tidak terpakai, gantikan dengan:
  ```css
  .modal-overlay {
    padding: 10px !important;
    align-items: center !important;
  }
  .modal-card,
  .modal-box {
    width: 95% !important;
    max-width: 480px !important;
    max-height: 90dvh !important;
    border-radius: var(--radius-xl, 20px) !important;
  }
  .modal-body {
    padding: 14px 16px !important;
    max-height: calc(85vh - 120px) !important;
  }
  .modal-footer {
    padding: 12px 16px !important;
  }
  ```

- [ ] **Step 2: Optimasi Payment Modal di Mobile**
  - Tombol pilihan metode pembayaran (*Tunai, QRIS, Transfer*): berikan tinggi dan padding yang proporsional.
  - Input nominal uang diterima (`#payment-cash-input`): tinggi `48px`, font besar `20px`.
  - Tombol nominal instan (`.quick-cash-chip`): tinggi minimal `42px`, padding nyaman `8px 14px`.

- [ ] **Step 3: Commit**
  ```bash
  git add css/mobile.css
  git commit -m "feat(mobile): fix modal-card selectors and enlarge payment keypad targets"
  ```

---

### Task 7: Comprehensive Mobile Viewport Verification & Quality Audit

**Files:**
- Verify: `index.html`, `css/mobile.css`, `css/design-system.css`, `css/web.css`

- [ ] **Step 1: Verifikasi syntax CSS & kelengkapan tag**
  Periksa seluruh kurung kurawal `@media (max-width: 768px)` dan `@media (max-width: 480px)` pada `css/mobile.css`.

- [ ] **Step 2: Verifikasi bebas tabrakan (No Collisions) di seluruh layar**
  - POS Top Bar: search input dan tombol aksi cepat tidak bertabrakan.
  - Floating Cart: tidak menabrak bottom dock navigasi.
  - Orders: baris transaksi berubah menjadi kartu dengan tombol `Struk` dan `Void` tertata rapi.
  - Stock Tracker: batch sate dan potongan ayam tampil sebagai kartu yang mudah di-tap.
  - Reports: tren grafik dan menu terlaris tersusun rapi vertikal tanpa meluap.
  - Shift & Settings: kartu metrik dan formulir fit 100% dengan layar ponsel.

- [ ] **Step 3: Verifikasi tampilan Desktop tetap utuh 100%**
  Pastikan stylesheet `web.css` dan `design-system.css` tidak terpengaruh untuk resolusi desktop (> 768px).

- [ ] **Step 4: Update DO_NEXT.md**
  Tandai pencapaian perbaikan UI/UX mobile menyeluruh di `DO_NEXT.md`.

- [ ] **Step 5: Final Commit**
  ```bash
  git add DO_NEXT.md
  git commit -m "docs: mark comprehensive mobile ui/ux overhaul as completed in DO_NEXT.md"
  ```
