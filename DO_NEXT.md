# 📋 KA POS — DO NEXT Task List & Program Analysis

> Dokumen ini memuat temuan analisa menyeluruh sistem **KA POS v3.0**, mencakup audit keamanan, bug fungsional, integritas data, serta rekomendasi peningkatan program.
> **Panggil task ini di sesi berikutnya:** *"kerjakan TASK 3 di DO_NEXT.md"* atau *"perbaiki bug nomor X"*.

---

## 🔴 PRIORITAS TINGGI (Bug Kritis & Keamanan)

---

### [TASK 3] [SELESAI ✅] Keamanan: Otorisasi PIN / Approval Owner untuk Void Transaksi
- **Lokasi:** `js/views/orders.js` (`submitVoid`), `js/state.js` (`verifyOwnerPin`), `index.html` (`order-void-modal`)
- **Status:** **Tuntas.** Modal verifikasi PIN Owner telah diterapkan dengan opsi toggle visibility, validasi role Owner/default PIN, error feedback, dan audit logging `AUTH_VOID_FAILED`.

---

### [TASK 4] [SELESAI ✅] Sinkronisasi Status Void ke Supabase Cloud
- **Lokasi:** `js/supabase.js`, `js/views/orders.js`
- **Status:** **Tuntas.** Ditambahkan `SupabaseService.updateOrderStatus(orderNo, status, voidData)` yang mengupdate tabel `orders` secara online, memperbarui item di `offlineQueue`, dan mengantrekan aksi `update_status` saat offline.

---

### [TASK 5] [SELESAI ✅] Sanitasi Input (Cegah XSS) & Perbaikan Script Injection pada Cart
- **Lokasi:** `js/views/pos.js`, `js/views/orders.js`, `js/views/customers.js`, `js/state.js`
- **Status:** **Tuntas.** Helper `escapeHtml()` dan `window.escapeHtml` aktif untuk seluruh data teks pelanggan, catatan, alasan void. Tombol cart menggunakan event delegation dengan data attributes aman dari quotes / special characters.

---

### [TASK 6] [SELESAI ✅] Kelengkapan Backup & Restore Database
- **Lokasi:** `js/views/settings.js` (`exportBackup`, `importBackup`), `js/state.js`
- **Status:** **Tuntas.** Ekspor dan impor JSON backup kini mencakup `stockMutations`, `dailyStock` (`ka_daily_stock`), `offlineQueue`, `receivables`, `savedOrders`, dan `stockAuditLog`.

---

### [TASK 7] [SELESAI ✅] Pembatasan Akses Kasir pada CRM Pelanggan & Poin
- **Lokasi:** `js/views/customers.js`
- **Status:** **Tuntas.** Tombol hapus pelanggan disembunyikan untuk kasir & dilindungi di handler JS (`isOwner()`). Input edit poin loyalty dinonaktifkan untuk kasir dan dilindungi dari manipulasi saat form disubmit.

---

## 🟡 PRIORITAS MENENGAH (Integritas Data, Hardware & UX)

---

### [TASK 8] [SELESAI ✅] Cetak Struk via Standar Browser / USB Printer (Fallback Non-Bluetooth)
- **Lokasi:** `js/printer.js` (`printSystemReceipt`), `css/design-system.css` (`@media print`), `index.html` (`#btn-receipt-modal-system-print`)
- **Status:** **Tuntas.** Ditambahkan metode `printSystemReceipt(trx)` yang memicu `window.print()` ke `#receipt-print-frame` dengan styling `@media print` 58mm/80mm (`--receipt-width`), tombol **"Cetak via Komputer / USB"** di modal pratinjau struk, serta fallback otomatis dari `printReceipt()` saat Web Bluetooth tidak tersedia.

---

### [TASK 9] [SELESAI ✅] Perbaikan WhatsApp Struk & URL Encoding
- **Lokasi:** `js/printer.js` (`shareWhatsApp`, `shareShiftWhatsApp`, `_normalizeWaPhone`), `index.html` (`#receipt-wa-phone`)
- **Status:** **Tuntas.** `encodeURI` diganti menjadi `encodeURIComponent` agar karakter `&`, `+`, dan `#` aman. Ditambahkan input opsional nomor WhatsApp pelanggan di modal pratinjau struk (auto-fill dari CRM & normalisasi `08xx` ke `628xx`).

---

### [TASK 10] [SELESAI ✅] Proteksi Password & PIN dari Penyimpanan Plaintext
- **Lokasi:** `js/state.js` (`hashPassword`, `hashPin`, `isSha256Hex`, `verifyOwnerPin`), `js/views/auth.js` (`verifyPin`), `js/views/orders.js` (`submitVoid`), `js/views/settings.js` (`handleAddStaff`)
- **Status:** **Tuntas.** Password dan PIN staf kini di-hash dengan SHA-256 (`hashPassword` / `hashPin`) saat penambahan staf di `settings.js`, dengan migrasi otomatis PIN plaintext lama ke SHA-256 saat login di `auth.js` maupun verifikasi PIN Owner di `verifyOwnerPin()`.

---

### [TASK 11] [SELESAI ✅] Indikator Stok Habis Real-time pada Menu POS
- **Lokasi:** `js/views/pos.js` (`isProductOutOfStock`, `renderProducts`, `handleProductClick`)
- **Status:** **Tuntas.** Katalog POS kini memeriksa ketersediaan bahan baku resep BOM secara real-time, me-render label `HABIS` + kartu nonaktif saat bahan baku utama habis, dan me-refresh otomatis saat `LS_KEYS.inv` berubah.

---

### [TASK 12] [SELESAI ✅] Konfirmasi Batal / Reset Keranjang (Mencegah Salah Sentuh)
- **Lokasi:** `js/views/pos.js` (`confirmClearCart`), `index.html` (`#btn-clear-cart`)
- **Status:** **Tuntas.** Ditambahkan tombol **Kosongkan Keranjang** (`#btn-clear-cart`) dengan dialog konfirmasi agar kasir tidak tidak sengaja menghapus pesanan saat antrean ramai.

---

### [TASK 13] Optimasi Elemen Tampilan Mobile (< 480px)
- **Lokasi:** `css/mobile.css`, `css/design-system.css`
- **Temuan Masalah:** Pada layar handphone tertentu, elemen filter tanggal di riwayat nota, sub-tab di manajemen inventaris, dan modal rincian transaksi terasa terlalu padat atau meluap secara horizontal.
- **Solusi Rekomendasi:**
  - Tambahkan scrolling horizontal mulus (`overflow-x: auto; scrollbar-width: none`) pada tab/filter.
  - Pastikan lebar input modal pas 100% dan tidak ada teks statis yang mendesak tombol aksi keluar layar.

---

## 🟢 PRIORITAS RENDAH / PENINGKATAN FITUR (Enhancements)

---

### [TASK 14] Cetak Slip Pesanan Dapur (Kitchen Ticket / Order Slip)
- **Deskripsi:** Pisahkan struk belanja pelanggan dengan slip dapur. Slip dapur hanya mencantumkan nomor nota, waktu, nama menu, jumlah porsi, serta level pedas & catatan sambal, tanpa mencantumkan harga.
- **Manfaat:** Mempermudah tim dapur/goreng membaca pesanan tanpa membingungkan mereka dengan rincian pembayaran.

---

### [TASK 15] [SELESAI ✅] Fitur Split Payment (Multi-Metode Pembayaran)
- **Lokasi:** `index.html` (`#pay-section-split`), `js/views/payment.js`, `js/views/shift.js`, `js/views/reports.js`, `js/printer.js`
- **Status:** **Tuntas.** Pelanggan kini dapat membayar dengan 2 metode sekaligus dalam 1 nota (misal Tunai + QRIS/Transfer), lengkap dengan pencatatan laci kasir khusus porsi tunai, pemisahan laporan & shift, serta rincian di struk WhatsApp.

---

### [TASK 16] [SELESAI ✅] Analisa Varians Stok (Food Cost Variance / Audit Selisih)
- **Lokasi:** `index.html` (`#stock-variance-container`), `js/views/reports.js` (`renderStockVariance`)
- **Status:** **Tuntas.** Ditambahkan kartu laporan **Analisa Varians Stok** untuk Owner yang membandingkan pemakaian teoritis (POS BOM) vs aktual (mutasi keluar & Stock Tracker Taichan) beserta persentase selisih & status `AMAN` / `WASPADA` / `SHRINKAGE`.

---

### [TASK 17] Webhook Notifikasi Alarm ke Telegram / WhatsApp Owner
- **Deskripsi:** Ketika terjadi transaksi Void darurat, selain menyalakan alarm di browser, sistem juga dapat menembakkan webhook gratis (misal ke Telegram Bot Owner).
- **Manfaat:** Owner langsung tahu ada transaksi yang dibatalkan meskipun aplikasi di HP Owner sedang tertutup.

---

### [TASK 18] [SELESAI ✅] Shortcut Tombol Keyboard untuk Kasir Cepat (Desktop POS)
- **Lokasi:** `js/views/pos.js` (`setupKeyboardShortcuts`)
- **Status:** **Tuntas.** Shortcut `F2` (fokus pencarian menu), `F4` (buka pembayaran), `F8` (simpan/hold order), `F9` (cetak struk terakhir), dan `Esc` (tutup modal aktif) aktif di Desktop POS.

---

## ✅ FITUR YANG SUDAH SELESAI

- [x] **TASK 1:** Pembatasan akses riwayat transaksi & nominal penjualan untuk kasir (nominal disembunyikan `••••`, omzet `—`, tombol ekspor CSV disembunyikan).
- [x] **TASK 2:** Update stock harian kasir blind audit (total proses tersembunyi) + pencatatan log audit stok & tab Log Audit untuk Owner.
- [x] **TASK 3:** Keamanan Otorisasi PIN Owner untuk Void transaksi + alarm darurat.
- [x] **TASK 4:** Sinkronisasi status Void ke Supabase Cloud (online & offline queue).
- [x] **TASK 5:** Sanitasi input XSS (escapeHtml) & perbaikan script injection cart buttons.
- [x] **TASK 6:** Kelengkapan backup & restore database (mutasi, stok harian, antrean offline, piutang, order tersimpan).
- [x] **TASK 7:** Pembatasan akses kasir pada CRM Pelanggan & proteksi saldo poin loyalty.
- [x] **TASK 8:** Cetak struk fallback via browser / printer USB (`printSystemReceipt` + `@media print` + tombol modal pratinjau).
- [x] **TASK 10:** Proteksi password & PIN staf dengan hash SHA-256 (`hashPin` + migrasi otomatis PIN plaintext).
- [x] Semua icon makanan diganti dari emoji ke SVG vector (FoodIcons system).
- [x] Pemisahan arsitektur stylesheet `web.css` dan `mobile.css`.
- [x] Desktop sidebar diperlebar (220px) dengan label teks horizontal & identitas outlet.
- [x] KPI cards desktop: 4-column grid dengan accent strip.
- [x] Komponen panel-card terintegrasi ke design system.
- [x] PWA offline support & caching untuk seluruh aset aplikasi.
- [x] **MOBILE UI/UX OVERHAUL:** Perbaikan menyeluruh antarmuka mobile smartphone (360px–480px) & tablet:
  - POS Top Bar restrukturisasi 2-baris (pencarian 100% full-width + order switcher & aksi cepat rapi bebas tabrakan).
  - Transformasi otomatis seluruh data table (Riwayat Nota, Stock Tracker Taichan/Ayam, CRM Member, Shift) menjadi Stacked Card Layout modern ramah operasional satu tangan.
  - Responsive reflow 1-kolom pada Laporan Finansial (grafik tren, menu terlaris, peak hours, payment channels), Pengaturan, dan Kelola Menu.
  - Perbaikan selector modal (`.modal-card` & `.modal-box`) dan pembesaran touch target tombol kuantitas keranjang (`36px`) serta keypad pembayaran (`min 46px`).
  - Safe-area bottom padding terintegrasi untuk mencegah overlap dengan bottom navigation dock.
  - **Android Jetpack Compose NavHost Architecture:** Penerapan prinsip single-active destination (`NavHost` / `Scaffold`); memastikan hanya ada tepat 1 view yang aktif dan tampil di layar setiap saat (`.view-pane:not(.active) { display: none !important; }`), memperbaiki bug dua tampilan fitur (POS dan Riwayat) yang sempat muncul bertumpuk dalam satu layar.
  - **Context-Aware Floating Cart:** Floating cart pill otomatis tersembunyi saat berada di luar layar kasir (Riwayat, Laporan, Shift, dll.) agar tidak menutupi informasi halaman lain.

---

*Laporan analisa diperbarui: 2026-10-14 | Versi Sistem: KA POS v3.8.0*
