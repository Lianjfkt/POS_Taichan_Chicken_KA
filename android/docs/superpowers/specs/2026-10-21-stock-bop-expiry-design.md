# Spesifikasi Desain: Fitur Stok Terintegrasi Menu (BOP Otomatis, Batch, & Kontrol Kadaluarsa)

## 1. Latar Belakang & Masalah
Saat ini terdapat dua sistem stok yang saling tumpang tindih (*redundant*):
1. **Sistem Stok Harian Hardcoded (SharedPreferences)**: Tab khusus Sate Taichan & Ayam Potong terpisah dengan pelacakan manual mentah vs matang.
2. **Sistem Bahan Baku BOM (Room Database)**: Tabel `raw_materials`, `recipe_items`, dan `stock_movements`.

Selain itu, terdapat kekurangan kritis:
- Biaya Operasional Pokok / Harga Pokok Produksi (**BOP / HPP**) pada produk menu masih berupa angka statis manual di `ProductEntity.costPrice`, belum dihitung otomatis dari resep bahan baku.
- Saat restock bahan masuk, biaya beli tidak memperbarui harga rata-rata bahan baku dan tidak memperbarui BOP produk menu.
- Belum ada pelacakan tanggal kadaluarsa (*expiry date*) dan status kelayakan (*usable vs expired/damaged*) per pengiriman bahan.

## 2. Tujuan Desain & Penyederhanaan (Simplifikasi)
1. **Unifikasi & Hapus Redundansi**:
   - Menghapus sistem SharedPreferences ganda (*TaichanBatch* & *ChickenBatch*) dan mengkonsolidasikan seluruh stok ke dalam satu sistem terpadu berbasis Room Database.
   - Semua bahan (daging ayam, tusuk sate, bumbu cabai, beras, packaging) dikelola seragam dalam satu manajemen inventaris profesional.
2. **Kalkulasi Otomatis BOP Menu (Real-Time HPP & Margin)**:
   - Menghitung biaya bahan baku menggunakan metode akuntansi standar: **Moving Weighted Average (Biaya Rata-Rata Tertimbang)** dari seluruh batch aktif.
   - Setiap kali restock disimpan, sistem otomatis menghitung ulang dan memperbarui `costPrice` produk menu yang menggunakan bahan tersebut dalam Resep BOM.
3. **Kontrol Kadaluarsa & Kelayakan Bahan (Batch Expiry & Waste)**:
   - Setiap restock mencatat nomor batch, kuantitas awal, sisa stok, harga beli satuan, dan tanggal kadaluarsa.
   - Indikator visual non-blocking:
     - 🟢 **Segar / Aman**: > 2 hari sebelum expired.
     - 🟡 **Mendekati Kadaluarsa**: $\le 2$ hari s/d hari H.
     - 🔴 **Kadaluarsa / Tidak Layak**: Melewati hari H atau ditandai manual tidak layak.
   - Fitur cepat **"Catat Waste / Pemusnahan"** untuk memotong stok rusak/kadaluarsa dan mencatat kerugian finansial ke audit mutasi.
4. **Pemotongan Stok Berbasis FIFO**:
   - Saat pesanan di kasir selesai, bahan terpotong otomatis dari batch dengan tanggal kadaluarsa terdekat / batch tertua lebih dulu.

---

## 3. Desain Skema Database (Room Database)

### 3.1. Entitas Baru: `MaterialBatchEntity` (Tabel `material_batches`)
```kotlin
@Entity(
    tableName = "material_batches",
    foreignKeys = [
        ForeignKey(
            entity = RawMaterialEntity::class,
            parentColumns = ["id"],
            childColumns = ["materialId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("materialId"), Index("expiryDate")]
)
data class MaterialBatchEntity(
    @PrimaryKey val id: String,
    val materialId: String,
    val batchNumber: String,        // Format: B-YYYYMMDD-XXXX
    val initialQuantity: Double,    // Kuantitas awal masuk
    val remainingQuantity: Double,  // Sisa kuantitas batch ini
    val costTotal: Double,          // Total biaya pembelian batch ini
    val costPerUnit: Double,        // costTotal / initialQuantity
    val receivedDate: Long,         // Waktu terima / restock
    val expiryDate: Long?,          // Waktu kadaluarsa (null jika non-perishable)
    val isUsable: Boolean = true,   // Status kelayakan
    val notes: String = ""          // Catatan vendor / supplier
)
```

### 3.2. Penyesuaian `RawMaterialEntity`
Memastikan `RawMaterialEntity` menyimpan data ringkasan terkini untuk pencarian cepat:
- `currentStock`: Double (total sisa kuantitas seluruh batch aktif).
- `costPerUnit`: Double (rata-rata tertimbang biaya per unit saat ini).
- `earliestExpiryDate`: Long? (tanggal expired terdekat dari batch yang masih bersisa).
- `hasExpiredBatch`: Boolean (true jika ada batch yang sudah lewat tanggal expired atau `isUsable == false`).

### 3.3. Penyesuaian `StockMovementEntity`
Menambahkan informasi batch dan nilai moneter kerugian:
- `batchId`: String?
- `costTotal`: Double (mencatat nilai rupiah pembelian saat restock, atau nilai kerugian saat waste).

---

## 4. Logika Bisnis & Formula Perhitungan

### 4.1. Formula Moving Weighted Average (Biaya Rata-Rata Bahan)
Ketika restock batch baru masuk dengan jumlah $Q_{\text{baru}}$ dan total biaya $C_{\text{baru}}$:
$$\text{CostPerUnit}_{\text{baru}} = \frac{(\text{StokLama} \times \text{CostLama}) + C_{\text{baru}}}{\text{StokLama} + Q_{\text{baru}}}$$

Jika stok lama kosong ($\le 0$):
$$\text{CostPerUnit}_{\text{baru}} = \frac{C_{\text{baru}}}{Q_{\text{baru}}}$$

### 4.2. Formula BOP Menu (Otomatis & Real-Time)
Untuk setiap produk di tabel `products` yang memiliki resep di `recipe_items`:
$$\text{BOP Produk (costPrice)} = \sum_{i=1}^{n} (\text{quantityRequired}_i \times \text{material.costPerUnit}_i)$$

Pembaruan ini dipicu secara reaktif saat:
1. Restock bahan baku berhasil disimpan.
2. Item resep diubah atau ditambahkan di `RecipeBomDialog`.
3. Harga bahan disesuaikan melalui stok opname.

### 4.3. Formula Margin Keuntungan Menu
$$\text{Gross Margin (Rp)} = \text{Harga Jual (price)} - \text{BOP (costPrice)}$$
$$\text{Margin (\%)} = \left(\frac{\text{Harga Jual} - \text{BOP}}{\text{Harga Jual}}\right) \times 100\%$$

### 4.4. Pemotongan Stok Penjualan (FIFO)
Ketika order dibayar:
1. Ambil daftar bahan resep untuk tiap produk di keranjang.
2. Ambil batch aktif untuk bahan tersebut diurutkan `ORDER BY expiryDate ASC NULLS LAST, receivedDate ASC`.
3. Kurangi stok batch secara berurutan hingga kuantitas yang dibutuhkan terpenuhi.
4. Update sisa stok `currentStock` pada bahan master.

### 4.5. Pencatatan Waste (Barang Rusak / Kadaluarsa)
1. User memilih batch yang kadaluarsa/rusak dan menginput kuantitas buang.
2. Sisa stok batch dikurangi kuantitas buang.
3. Total kerugian finansial dihitung: $\text{Kuantitas Buang} \times \text{batch.costPerUnit}$.
4. Tercatat transaksi mutasi bertipe `WASTE` di `stock_movements`.

---

## 5. Rancangan Tampilan Pengguna (UI & UX Simplifikasi)

### 5.1. Navigasi Tab Layar Stok (`StockScreen`)
Menghapus tab terpisah Taichan dan Chicken yang tumpang tindih. Menyederhanakan menjadi **2 Tab Utama**:
1. **Tab 1: Manajemen Bahan Baku (Inventory)**
   - Header ringkasan: Total Aset Bahan Baku (Rp), Bahan Stok Menipis, dan Peringatan Kadaluarsa.
   - Filter Kategori: Semua, Daging & Protein, Bumbu & Sambal, Karbo, Minuman, Packaging.
   - Kartu Bahan Baku Modern:
     - Nama, Kategori, Unit.
     - Stok saat ini & Nilai HPP per unit (contoh: `Rp 45 / gram`).
     - Badge Status Kadaluarsa (🟢 Segar / 🟡 Mendekati Expired / 🔴 Kadaluarsa).
     - Tombol Aksi: **+ Restock**, **Rincian Batch**, **Waste**, dan **Opsi/Edit**.
2. **Tab 2: Audit Mutasi & Riwayat Waste**
   - Riwayat seluruh pergerakan stok: Pembelian (Restock), Pemotongan Kasir (BOM Auto), Pemusnahan (Waste), dan Opname.
   - Dilengkapi filter jenis mutasi dan tanggal.

### 5.2. Dialog Restock Bahan (`RestockMaterialDialog`)
- Field Input:
  - Jumlah Masuk (numeric / desimal).
  - Total Biaya Pembelian (Rp) $\rightarrow$ *Real-time preview*: Menampilkan estimasi harga satuan (misal: "Rp 42.000 / kg = Rp 42 / gram").
  - Tanggal Kadaluarsa $\rightarrow$ Quick buttons (+3 Hari, +7 Hari, +30 Hari, Pilih Tanggal, Tanpa Expired).
  - Catatan / Supplier.

### 5.3. Dialog Rincian Batch & Waste (`BatchDetailDialog` & `WasteMaterialDialog`)
- Menampilkan daftar batch aktif dari bahan yang dipilih.
- Setiap baris batch menampilkan: No. Batch, Tanggal Masuk, Sisa Stok, Harga Beli, dan Status Tanggal Kadaluarsa.
- Tombol aksi cepat di setiap batch: **Tandai Rusak** atau **Buang (Waste)**.

### 5.4. Integrasi Layar Menu (`MenuManagementScreen` & `RecipeBomDialog`)
- Kartu Menu Produk:
  - Menampilkan BOP hasil kalkulasi BOM secara transparan.
  - Menampilkan persentase margin keuntungan dengan warna indikator: Hijau (> 50%), Kuning (20-50%), Merah (< 20%).
- Dialog Resep BOM:
  - Menampilkan rincian biaya setiap komponen bahan per porsi.
  - Menampilkan Total BOP Otomatis di bagian footer dialog.

---

## 6. Rencana Migrasi & Kompatibilitas
- Data lama dari Room Database (`raw_materials` dan `recipe_items`) tetap dipertahankan.
- Untuk bahan yang sudah ada, dibuatkan batch inisial otomatis dengan kuantitas sesuai `currentStock` dan harga sesuai `costPerUnit` yang telah ada saat ini.
- Kode usang (*deprecated*) SharedPreferences `StockRepository` dilepas secara bersih sehingga arsitektur aplikasi menjadi modular dan ringan.
