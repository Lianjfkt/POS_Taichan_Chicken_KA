# Desain Spesifikasi: Perbaikan Menyeluruh UI/UX Mobile KA POS

**Tanggal:** 2026-10-14  
**Status:** Validated & Approved  
**Topik:** Mobile UI/UX Overhaul & Stacked Card Layout Architecture  
**Target Viewport:** Smartphone (Resolusi 360px – 480px, Tablet ≤ 768px)  
**File Target Utama:** `css/mobile.css` (didukung penyesuaian kelas di view bila diperlukan)

---

## 1. Latar Belakang & Tujuan
Pada KA POS v3.0, tampilan mobile masih mengalami benturan (*overlap*), pemotongan teks, tombol saling jepit, dan luapan horizontal (*horizontal overflow*) di layar ponsel pintar. Masalah paling krusial adalah:
1. Search bar dan tombol cepat pada POS Top Bar saling bertabrakan di layar sempit.
2. Modul riwayat pesanan, stok harian (Taichan & Ayam), laporan finansial, CRM member, dan pengaturan belum memiliki aturan responsif mobile, sehingga tabel multi-kolom dan grid desktop meluap keluar layar.
3. Selector CSS modal salah sasaran (`.modal-dialog` alih-alih `.modal-card` dan `.modal-box`).
4. Tombol aksi kuantitas (`.qty-btn`) terlalu kecil (22px) untuk sentuhan jari kasir.

Tujuan perbaikan ini adalah menghadirkan antarmuka kasir dan owner yang pas (*fit*), elegan, bebas tumpang tindih, dan nyaman dioperasikan satu tangan (*one-thumb navigation*).

---

## 2. Arsitektur Solusi & Komponen

### 2.1 POS Terminal & Alur Kasir
1. **Header Kompak:**
   - `.app-header`: Tinggi tetap 50px, logo KA POS proporsional, status sync cloud dibuat hemat ruang (*dot indicator + teks ringkas*).
   - Safe-area support untuk poni/notch kamera HP.
2. **Top Bar Operasional (`.pos-top-bar`):**
   - Menggunakan flex-column 2 baris terstruktur:
     - Baris 1: Pencarian menu (`.pos-search-wrapper`) lebar 100% penuh.
     - Baris 2: Switcher tipe order (*Dine In | Bungkus | Ojol*) sejajar dengan tombol aksi cepat (*Update Stok, Fast Stock, Riwayat*) berbentuk ikon tombol kompak 36px-40px.
3. **Katalog Produk:**
   - Grid 2-kolom (`repeat(2, minmax(0, 1fr))`) dengan jarak 8px.
   - Judul produk dibatasi 2 baris (`-webkit-line-clamp: 2`).
   - Tombol tambah `+` dan harga tampil rapi di bagian bawah kartu.
   - Padding bawah katalog diset `115px` agar menu terbawah tidak tertutup Floating Cart.
4. **Floating Cart Pill:**
   - Posisi tetap di atas dock navigasi: `bottom: calc(var(--mobile-nav-h, 60px) + 8px + env(safe-area-inset-bottom, 0px))`.
   - Menampilkan jumlah item, total harga tebal, dan tombol aksi bayar langsung.
5. **Drawer Keranjang (Bottom Sheet):**
   - Muncul dari bawah dengan `max-height: 90dvh` dan *drag handle*.
   - Tombol kuantitas (`.qty-btn`) diperbesar menjadi **36px x 36px** dengan target sentuh lega.
   - Pemisah item dan tombol hapus tidak saling tumpuk.

---

### 2.2 Transformasi Data Table ke Stacked Card Layout
Pada layar `≤ 768px`, seluruh elemen `table.data-table` otomatis bertransformasi menjadi kartu mandiri:
- `thead` disembunyikan (`display: none !important;`).
- Setiap baris `tr` bertransformasi menjadi kartu (`display: block; border-radius: 14px; background: var(--surface-container); padding: 12px 14px; margin-bottom: 10px; border: 1px solid rgba(255,255,255,0.07); box-shadow: 0 2px 8px rgba(0,0,0,0.25);`).
- Setiap sel `td` disusun vertikal/horizontal sesuai perannya:

#### A. Riwayat Transaksi (`#view-orders`)
- **Atas:** Nomor Nota (oranye monospace) + Badge Status (`SELESAI` / `BATAL`) di kanan.
- **Tengah:** Nama Pelanggan, Kasir, Waktu Transaksi, dan rincian item menu yang dipesan.
- **Bawah:** Metode Pembayaran, Total Tagihan (Rp nominal besar), serta baris tombol aksi (*Struk*, *Detail*, *Void*) berukuran minimal 38px tinggi dengan label yang jelas.

#### B. Stock Tracker Sate Taichan & Ayam (`#view-inventory`)
- **Taichan Batch:** Kartu batch menampilkan Tanggal, Badge Kesegaran (*Layak / Kadaluarsa*), Jumlah Tusuk Mentah (tebal), Keterangan, dan tombol Hapus.
- **Chicken Parts:** Kartu bagian ayam menampilkan Nama Potongan + Emoji, Ringkasan Mentah & Goreng, serta daftar batch harian dengan tombol input yang mudah disentuh.
- **Bahan Baku & Sambal:** Setiap bahan menjadi kartu dengan stok tersedia, indikator kritis, satuan, dan tombol aksi.

#### C. Member CRM (`#view-customers`)
- Kartu member menampilkan Nama, No Telepon/WA (bisa langsung di-tap), Saldo Poin Loyalty, Frekuensi Order, Total Belanja, dan tombol aksi.

#### D. Shift Kasir (`#view-shift`)
- Riwayat arus kas dan riwayat tutup shift tampil sebagai kartu ringkasan waktu, tipe (*Kas Masuk / Kas Keluar*), nominal, dan kasir penanggung jawab.

---

### 2.3 Single-Column Responsive Reflow untuk Modul Sekunder
1. **Laporan & Analitik (`#view-reports`):**
   - Grid grafik & menu terlaris (`3fr 2fr`) diubah menjadi 1 kolom penuh vertikal.
   - Bagian Peak Hours & Payment Methods (`minmax(360px, 1fr)`) diubah menjadi 1 kolom (`1fr`).
   - Kartu KPI analitik ditata menjadi grid 2-kolom (`repeat(2, 1fr)`).
   - Filter periode dibuat horizontal swipeable (*no scrollbar*).
2. **Shift & Kas Drawer (`#view-shift`):**
   - 4 tombol aksi shift (*Kas Masuk, Kas Keluar, Cetak X-Report, Tutup Shift*) ditata menjadi grid 2x2 yang simetris dan mudah ditekan.
   - Kartu metrik saldo disusun 2x2.
3. **Pengaturan Sistem (`#view-settings`):**
   - Formulir Profil Toko, Supabase Cloud, dan Printer Bluetooth ditata dalam 1 kolom vertikal rapi dengan tombol aksi lebar penuh.
4. **Kelola Menu (`#view-menu`):**
   - Grid katalog produk kelola menu diatur menjadi 2 kolom responsif.

---

### 2.4 Perbaikan Modal, Keypad & Safe Area
1. **Modal Dialog (`.modal-card`, `.modal-box`):**
   - Mengganti target selector lama (`.modal-dialog`) ke kelas riil `.modal-card` dan `.modal-box`.
   - Lebar diset `94%` (maksimal 460px) dengan `max-height: 90dvh`, sudut membulat `20px`.
2. **Payment Modal:**
   - Keypad angka dan tombol nominal instan (*Uang Pas, 20rb, 50rb, 100rb, dll.*) diperbesar dengan tinggi minimal **46px–48px**.
   - Ringkasan total dan kembalian ditampilkan dengan kontras tinggi.
3. **Scroll View Padding:**
   - `.view-pane.view-scroll` diberi padding bawah `calc(var(--mobile-nav-h, 60px) + 24px)` sehingga elemen terbawah pada halaman Riwayat, Laporan, atau Pengaturan tidak tertutup oleh Dock Navigasi Bawah.

---

## 3. Strategi Pengujian & Verifikasi
1. **Verifikasi Resolusi:**
   - 360px (Ponsel kompak Android).
   - 390px (Standar iPhone 12/13/14/15/16).
   - 412px (Standar Samsung Galaxy / Pixel).
   - 768px (Tablet portrait).
2. **Ceklist Uji:**
   - [ ] Tidak ada scrollbar horizontal yang tidak disengaja (*overflow-x: hidden* pada shell).
   - [ ] Tidak ada teks yang terpotong atau ikon yang tumpang tindih pada header dan top bar.
   - [ ] Semua tabel (Orders, Stock, Member, Shift) berubah menjadi kartu yang rapi dan mudah di-scroll.
   - [ ] Tombol kuantitas keranjang (`.qty-btn`) dan keypad pembayaran mudah disentuh tanpa salah pencet.
   - [ ] Floating cart tidak menabrak bottom navigation dock.
   - [ ] Tampilan desktop/web tetap normal 100% tanpa distorsi.
