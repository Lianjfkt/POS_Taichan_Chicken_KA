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

### [TASK 8] Cetak Struk via Standar Browser / USB Printer (Fallback Non-Bluetooth)
- **Lokasi:** `js/printer.js`
- **Temuan Masalah:** Sistem saat ini hanya mengandalkan Web Bluetooth API (`navigator.bluetooth`). Jika kasir menggunakan PC kasir desktop dengan printer thermal USB/LAN atau browser tidak mendukung Bluetooth, kasir tidak bisa mencetak struk fisik sama sekali karena tombol cetak di modal pratinjau kembali memicu pencarian Bluetooth.
- **Solusi Rekomendasi:**
  - Tambahkan tombol **"Cetak via Komputer (System Print)"** yang memicu `window.print()` dengan CSS khusus struk 58mm/80mm (`@media print`).
  - Kasir dapat memilih mode cetak: *Bluetooth Direct* atau *Driver USB/Sistem*.

---

### [TASK 9] Perbaikan WhatsApp Struk & URL Encoding
- **Lokasi:** `js/printer.js` (`shareWhatsApp`, `shareShiftWhatsApp`)
- **Temuan Bug:** Fungsi WhatsApp menggunakan `encodeURI(text)` yang tidak meng-encode karakter `&`, `+`, atau `#`. Akibatnya jika nama outlet, item, atau catatan mengandung tanda tersebut, isi pesan WhatsApp terpotong.
- **Solusi Rekomendasi:**
  - Ganti `encodeURI` menjadi `encodeURIComponent`.
  - Tambahkan input nomor WhatsApp pelanggan agar bisa langsung dikirim ke nomor pelanggan (misal: `https://api.whatsapp.com/send?phone=...&text=...`).

---

### [TASK 10] Proteksi Password & PIN dari Penyimpanan Plaintext
- **Lokasi:** `js/state.js`, `js/views/auth.js`, `js/views/settings.js`
- **Temuan Masalah:** Password dan PIN staf disimpan di `localStorage` (`ka_st`) dalam bentuk string yang dapat dibaca siapa saja melalui Console DevTools.
- **Solusi Rekomendasi:**
  - Pastikan semua password disimpan dalam format hash SHA-256 sebelum disimpan ke `localStorage`.
  - PIN kasir/owner di-hash atau di-encode sehingga tidak terbaca langsung.

---

### [TASK 11] Indikator Stok Habis Real-time pada Menu POS
- **Lokasi:** `js/views/pos.js` (`renderProducts`), `js/state.js`
- **Temuan Masalah:** Di katalog POS, jika stok bahan baku suatu menu habis (berdasarkan resep BOM), tombol menu masih bisa diklik dan dipesan oleh kasir. Kasir baru menyadari stok minus setelah pesanan dibuat.
- **Solusi Rekomendasi:**
  - Periksa ketersediaan bahan resep BOM secara real-time saat me-render menu di POS.
  - Tampilkan label `[HABIS]` atau disable kartu produk dengan opacity jika bahan baku utamanya 0.

---

### [TASK 12] Konfirmasi Batal / Reset Keranjang (Mencegah Salah Sentuh)
- **Lokasi:** `js/views/pos.js`
- **Temuan Masalah:** Tombol batal/hapus keranjang bisa tertekan tanpa konfirmasi saat antrean sedang ramai, menyebabkan kasir harus menginput ulang semua menu pesanan dari awal.
- **Solusi Rekomendasi:**
  - Tambahkan dialog konfirmasi cepat (atau undo toast) saat tombol "Reset / Kosongkan Keranjang" ditekan.

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

### [TASK 15] Fitur Split Payment (Multi-Metode Pembayaran)
- **Deskripsi:** Memungkinkan pelanggan membayar menggunakan 2 metode sekaligus dalam 1 nota (misal: Rp 20.000 Tunai + sisa Rp 30.000 QRIS).
- **Manfaat:** Sering terjadi di lapangan ketika uang tunai pelanggan kurang dan sisanya dibayar transfer.

---

### [TASK 16] Analisa Varians Stok (Food Cost Variance / Audit Selisih)
- **Deskripsi:** Tab laporan khusus untuk Owner yang membandingkan:
  - Jumlah bahan terpakai menurut sistem penjualan POS (teoritis)
  - Jumlah stok fisik yang dilaporkan kasir lewat Stock Tracker (aktual)
  - Menampilkan persentase selisih (shrinkage / pemborosan).

---

### [TASK 17] Webhook Notifikasi Alarm ke Telegram / WhatsApp Owner
- **Deskripsi:** Ketika terjadi transaksi Void darurat, selain menyalakan alarm di browser, sistem juga dapat menembakkan webhook gratis (misal ke Telegram Bot Owner).
- **Manfaat:** Owner langsung tahu ada transaksi yang dibatalkan meskipun aplikasi di HP Owner sedang tertutup.

---

### [TASK 18] Shortcut Tombol Keyboard untuk Kasir Cepat (Desktop POS)
- **Deskripsi:** Tambahkan tombol shortcut keyboard standar POS:
  - `F2`: Fokus pencarian menu
  - `F4`: Bayar / Buka Pembayaran
  - `F8`: Simpan / Antrikan Pesanan
  - `Esc`: Tutup modal aktif

---

## ✅ FITUR YANG SUDAH SELESAI

- [x] **TASK 1:** Pembatasan akses riwayat transaksi & nominal penjualan untuk kasir (nominal disembunyikan `••••`, omzet `—`, tombol ekspor CSV disembunyikan).
- [x] **TASK 2:** Update stock harian kasir blind audit (total proses tersembunyi) + pencatatan log audit stok & tab Log Audit untuk Owner.
- [x] **TASK 3:** Keamanan Otorisasi PIN Owner untuk Void transaksi + alarm darurat.
- [x] **TASK 4:** Sinkronisasi status Void ke Supabase Cloud (online & offline queue).
- [x] **TASK 5:** Sanitasi input XSS (escapeHtml) & perbaikan script injection cart buttons.
- [x] **TASK 6:** Kelengkapan backup & restore database (mutasi, stok harian, antrean offline, piutang, order tersimpan).
- [x] **TASK 7:** Pembatasan akses kasir pada CRM Pelanggan & proteksi saldo poin loyalty.
- [x] Semua icon makanan diganti dari emoji ke SVG vector (FoodIcons system).
- [x] Pemisahan arsitektur stylesheet `web.css` dan `mobile.css`.
- [x] Desktop sidebar diperlebar (220px) dengan label teks horizontal & identitas outlet.
- [x] KPI cards desktop: 4-column grid dengan accent strip.
- [x] Komponen panel-card terintegrasi ke design system.
- [x] PWA offline support & caching untuk seluruh aset aplikasi.

---

*Laporan analisa diperbarui: 2026-10-08 | Versi Sistem: KA POS v3.0*
