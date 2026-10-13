# 📋 KA POS — DO NEXT Task List & Program Analysis

> Dokumen ini memuat temuan analisa menyeluruh sistem **KA POS v3.0**, mencakup audit keamanan, bug fungsional, integritas data, serta rekomendasi peningkatan program.
> **Panggil task ini di sesi berikutnya:** *"kerjakan TASK 3 di DO_NEXT.md"* atau *"perbaiki bug nomor X"*.

---

## 🔴 PRIORITAS TINGGI (Bug Kritis & Keamanan)

---

### [TASK 3] Keamanan: Otorisasi PIN / Approval Owner untuk Void Transaksi
- **Lokasi:** `js/views/orders.js` (`submitVoid`), `js/state.js`
- **Temuan Bug:** Kasir saat ini dapat membatalkan (void) transaksi kapan saja hanya dengan memilih alasan. Hal ini langsung memotong uang kas (`cashLog`) dan mengembalikan stok. Meskipun ada suara alarm lokal, seorang kasir nakal bisa mengambil uang tunai dari laci lalu mem-void transaksi tanpa otorisasi.
- **Solusi Rekomendasi:**
  1. Tambahkan modal verifikasi **PIN Owner / Supervisor** sebelum pembatalan dieksekusi.
  2. Atau opsi alternatif: Status nota diubah menjadi `pending_void` (Permintaan Batal) dan uang kas tidak dipotong sampai Owner menekan tombol **Setujui / Approve**.

---

### [TASK 4] Sinkronisasi Status Void ke Supabase Cloud
- **Lokasi:** `js/supabase.js`, `js/views/orders.js`
- **Temuan Bug:** Saat transaksi dibatalkan (void) di aplikasi lokal, fungsi `submitVoid()` hanya mengubah `status = 'void'` di `localStorage`. Database Supabase tidak pernah di-update. Akibatnya:
  - Transaksi yang sudah tersinkron ke cloud tetap berstatus `completed`.
  - Laporan omzet di Supabase cloud menjadi tidak akurat (terlalu tinggi).
  - Jika transaksi yang di-void masih ada di `offlineQueue`, antrean tetap mengunggahnya sebagai `completed`.
- **Solusi Rekomendasi:**
  - Tambahkan fungsi `SupabaseService.updateOrderStatus(orderNo, 'void', voidData)`.
  - Jika offline, masukkan perintah update status ke antrean `offlineQueue`.

---

### [TASK 5] Sanitasi Input (Cegah XSS) & Perbaikan Script Injection pada Cart
- **Lokasi:** `js/views/pos.js` (line 414, 420), `js/views/orders.js` (line 454-475), `js/views/customers.js`
- **Temuan Bug:**
  1. **Sintaks Error pada Cart Item:** Tombol hapus dan ubah qty di keranjang menggunakan `onclick="...decodeURIComponent('${safeMod}')..."` secara inline. Jika catatan pesanan mengandung tanda petik `'` atau karakter spesial, tombol menjadi error dan tidak bisa diklik.
  2. **XSS Vulnerability:** Nama pelanggan (`t.pelanggan`), catatan pesanan (`mod`), dan alasan void (`voidReason`) dimasukkan langsung ke `innerHTML` tanpa HTML escape (`escapeHtml`).
- **Solusi Rekomendasi:**
  - Buat helper `escapeHtml(str)` di `State` untuk semua output teks dinamis.
  - Gunakan `data-id` dan `data-mod` pada elemen tombol cart dengan event listener terdelegasi, bukan string escape inline di `onclick`.

---

### [TASK 6] Kelengkapan Backup & Restore Database
- **Lokasi:** `js/views/settings.js` (`exportBackup`, `importBackup`), `js/state.js`
- **Temuan Bug:** Fitur ekspor/impor backup saat ini **TIDAK** menyertakan data kritis berikut:
  - Log Audit Stok Kasir (`ka_stock_audit_log`)
  - Mutasi Stok Bahan Baku (`ka_mut`)
  - Batch Stok Harian Taichan & Ayam (`ka_daily_stock`)
  - Antrean Transaksi Offline (`ka_ofq`)
- **Dampak:** Jika Owner melakukan backup lalu me-restore data, seluruh riwayat audit kasir dan batch stok harian akan hilang total!
- **Solusi Rekomendasi:**
  - Perbarui struktur JSON backup di `SettingsView.exportBackup` dan `importBackup` agar mencakup seluruh key data.

---

### [TASK 7] Pembatasan Akses Kasir pada CRM Pelanggan & Poin
- **Lokasi:** `js/views/customers.js`
- **Temuan Bug:** Kasir saat ini memiliki akses penuh untuk menghapus data pelanggan (`deleteCustomerPrompt`) dan mengedit jumlah poin loyalty secara bebas di modal form pelanggan (`pointsInput`).
- **Solusi Rekomendasi:**
  - Field poin hanya bisa diedit oleh akun bertipe `owner`.
  - Tombol hapus member pelanggan hanya dimunculkan jika `window.State.isOwner()`.

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
- [x] Semua icon makanan diganti dari emoji ke SVG vector (FoodIcons system).
- [x] Pemisahan arsitektur stylesheet `web.css` dan `mobile.css`.
- [x] Desktop sidebar diperlebar (220px) dengan label teks horizontal & identitas outlet.
- [x] KPI cards desktop: 4-column grid dengan accent strip.
- [x] Komponen panel-card terintegrasi ke design system.
- [x] PWA offline support & caching untuk seluruh aset aplikasi.

---

*Laporan analisa diperbarui: 2026-10-08 | Versi Sistem: KA POS v3.0*
