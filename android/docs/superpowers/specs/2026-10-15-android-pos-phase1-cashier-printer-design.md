# Spec Desain: KA POS Android — Fase 1 (Operasional Kasir & Bluetooth Printer ESC/POS)

**Tanggal:** 2026-10-15  
**Status:** Approved  
**Aplikasi:** KA POS Native Android (Kotlin + Jetpack Compose)  
**Lingkup:** Fase 1 dari 3 (Paritas Fitur Kasir & Cetak Struk Fisik dengan Web App)

---

## 1. Latar Belakang & Tujuan

Aplikasi kasir Web App KA POS telah memiliki kapabilitas operasional lengkap (kustomisasi level pedas & catatan, diskon pesanan, tahan/buka pesanan meja, dan cetak struk via printer thermal Bluetooth). Versi Android native saat ini baru memiliki keranjang dasar tanpa opsi kustomisasi level, diskon, tahan order, dan tombol cetak yang masih berupa placeholder.

Tujuan Fase 1 adalah membawa kapabilitas kasir inti Android menyamai Web App sehingga aplikasi dapat langsung digunakan untuk transaksi nyata di gerai dengan printer thermal fisik.

---

## 2. Fitur & Komponen Baru

### 2.1. Bluetooth Thermal Printer Engine (`core/printer/`)
* **`BluetoothPrinterService`**:
  * Menangani izin runtime Bluetooth Android (Android 12+ `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, serta Android 11 kebawah `BLUETOOTH`, `BLUETOOTH_ADMIN`).
  * Menggunakan `BluetoothAdapter.getBondedDevices()` untuk mendaftar perangkat paired.
  * Membuka koneksi `BluetoothSocket` RFCOMM standard SPP (`UUID: 00001101-0000-1000-8000-00805F9B34FB`).
  * Menghasilkan byte format ESC/POS standar:
    * Inisialisasi printer: `ESC @` (`0x1B, 0x40`)
    * Align Center, Left, Right: `ESC a n`
    * Bold on/off: `ESC E n`
    * Double height / Double width untuk judul struk
    * Line feed & cut command: `GS V 66 0`
    * Cash drawer pulse (opsional): `ESC p 0 25 250`
  * Mendukung mode kertas **58mm (32 karakter/baris)** dan **80mm (48 karakter/baris)**.

* **Format Struk:**
  * Header: Nama Toko, Alamat, Nomor Telepon (Center, Bold)
  * Garis pemisah horizontal
  * Metadata: No. Nota, Tanggal & Jam, Tipe Pesanan (Dine-In/Take-Away), No. Meja, Kasir, Nama Pelanggan
  * Garis pemisah
  * Daftar Item:
    * Baris 1: Nama Item
    * Baris 2: Indent Level Pedas & Catatan Item (jika ada)
    * Baris 3: `Qty x Harga` (kiri) ..... `Subtotal` (kanan)
  * Garis pemisah
  * Ringkasan:
    * Subtotal
    * Diskon (jika > 0)
    * Total Akhir (Bold)
    * Metode Pembayaran
    * Nominal Diberikan & Kembalian (jika Tunai)
  * Garis pemisah
  * Footer struk & 3 baris kosong + cut command.

### 2.2. Pengaturan Printer di `SettingsScreen` & `SettingsRepository`
* Tambahan konfigurasi pada `AppSettings`:
  * `printerMacAddress: String` (Alamat MAC printer yang dipilih)
  * `printerName: String` (Nama Bluetooth printer)
  * `paperWidth: Int` (58 atau 80)
  * `autoPrintCheckout: Boolean` (Cetak otomatis struk setiap transaksi selesai)
* Tampilan UI di Pengaturan:
  * Dropdown/Modal memilih printer yang sudah ter-pairing di Bluetooth Android.
  * Radio button ukuran kertas: 58mm / 80mm.
  * Switch auto-print checkout.
  * Tombol **"Tes Cetak Struk"** dengan feedback toast / snackbar status koneksi.

### 2.3. Dialog Kustomisasi Menu di POS (`feature/pos/ModifierDialog.kt`)
* Saat item menu diklik di POS:
  * Jika kategori/menu memerlukan variasi (misal: Taichan, Ayam Pedas, atau memiliki `spiceLevel != ""`), muncul `ModifierDialog`.
  * Komponen dialog:
    * Judul menu & thumbnail emoji
    * Pilihan level pedas: *Original (Tidak Pedas), Lv. 1, Lv. 2, Lv. 3, Lv. 4, Lv. 5*
    * Input catatan khusus per item (misal: *"sambal dipisah"*, *"goreng kering"*, *"sedikit nasi"*)
    * Stepper kuantitas item awal
    * Tombol **"Tambah ke Keranjang"**
  * Produk tanpa opsi pedas (misal Minuman/Nasi) tetap dapat langsung ditambahkan ke keranjang atau dapat dibuka opsi catatannya melalui klik tahan / opsi edit di keranjang.

### 2.4. Diskon Pesanan di Keranjang & Checkout
* Komponen `DiscountDialog` / Section Diskon di `CartBottomSheet` & `ConfirmSaleScreen`:
  * Mendukung input tipe diskon:
    * **Nominal Rupiah (Rp)**: Contoh Rp 5.000
    * **Persentase (%)**: Contoh 10%
  * Otomatis mengkalkulasi pengurangan:
    $$\text{DiscountAmount} = \begin{cases} \text{Nominal}, & \text{tipe Rp} \\ \text{Subtotal} \times \frac{\text{Persen}}{100}, & \text{tipe \%} \end{cases}$$
    $$\text{Total} = \max(0, \text{Subtotal} - \text{DiscountAmount})$$
  * Diskon tercatat pada entitas `OrderEntity.discount` dan tersinkronkan ke Supabase `orders.discount`.

### 2.5. Tahan & Buka Pesanan (Hold / Save Order)
* Tabel Room lokal baru: `saved_orders` dan `saved_order_items`
  * `SavedOrderEntity`: `id`, `tableNo`, `customerName`, `orderType`, `subtotal`, `discount`, `createdAt`, `notes`
  * `SavedOrderItemEntity`: `id`, `savedOrderId`, `productId`, `productName`, `price`, `quantity`, `spiceLevel`, `notes`, `subtotal`
* Alur Penggunaan:
  * Kasir menekan tombol **"Tahan Pesanan"** di keranjang → pesanan tersimpan di DB lokal, keranjang dikosongkan untuk melayani pelanggan berikutnya.
  * Bar atas POS menampilkan badge jumlah pesanan yang ditahan (misal: `"3 Pesanan Tersimpan"`).
  * Kasir mengklik tombol pesanan tersimpan → memilih meja → pesanan kembali dimuat (*recalled*) ke keranjang POS untuk pembayaran checkout.

### 2.6. Cetak Ulang Struk di Riwayat Transaksi
* Tombol **"Cetak"** di `OrderDetailDialog.kt` diaktifkan:
  * Mengambil transaksi & item aktif.
  * Mengirim ke `BluetoothPrinterService.printOrderReceipt(order, items, appSettings)`.
  * Menampilkan notifikasi visual bila printer belum terhubung atau berhasil dicetak.

---

## 3. Data Flow & Sinkronisasi

1. **POS**: Pilih Menu → Dialog Modifier → Masuk Keranjang (dengan opsi Diskon & Tahan Order).
2. **Checkout**: Konfirmasi Meja & Pelanggan → Pilih Bayar (Tunai/QRIS/Transfer) → Simpan ke Room DB (`OrderEntity` + `OrderItemEntity`).
3. **Cetak**: Jika `autoPrintCheckout == true`, otomatis panggil `BluetoothPrinterService.printOrderReceipt()`.
4. **Cloud Push**: Background coroutine mengirim order ke Supabase Cloud (tabel `orders` & `order_items`).

---

## 4. Rencana Pengujian

1. **Unit & Logic Test**:
   * Kalkulasi diskon nominal & persentase.
   * Format teks baris ESC/POS 58mm & 80mm agar rata kanan-kiri simetris.
2. **UI & Flow Test**:
   * Alur kustomisasi item pedas & catatan masuk ke keranjang dengan benar.
   * Fitur tahan pesanan (Save) dan pemanggilan kembali (Recall) ke keranjang.
3. **Integrasi Printer**:
   * Tes pencetakan format struk fisik pada printer thermal ESC/POS 58mm.
   * Graceful fallback jika Bluetooth mati / printer tidak terjangkau (tidak menyebabkan app crash).
