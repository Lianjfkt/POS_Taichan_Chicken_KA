# Spec Desain — Fase 1: Operasional Kasir & Laci Kas (Petty Cash, Blind Cash Count, Cetak Shift, Void Reason)

**Tanggal:** 2026-10-16  
**Status:** Approved for Planning  
**Target:** Android App (`com.kapos.mobile`)  

---

## 1. Ringkasan & Tujuan
Fase ini bertujuan menyempurnakan fitur operasional kasir harian pada aplikasi Android KA POS agar setara dengan Web App v3.0, dengan fokus pada:
1. **Pencatatan Petty Cash (Kas Masuk & Kas Keluar)** langsung dari laci kasir saat shift aktif.
2. **Rekonsiliasi Kas Tutup Shift dengan Blind Cash Count** (kalkulator pecahan lembar uang fisik & koin).
3. **Pencetakan Struk Rekap Shift ke Printer Bluetooth (ESC/POS)** dan berbagi ringkasan shift ke WhatsApp Owner.
4. **Audit Alasan Pembatalan Transaksi (Void Reason)** dengan verifikasi PIN Owner.

---

## 2. Arsitektur Data & Model

### 2.1 Model & Entitas Petty Cash
File: `core/data/local/entity/Entities.kt` & `domain/model/Models.kt`

```kotlin
enum class CashMovementType(val displayName: String) {
    CASH_IN("Kas Masuk"),
    CASH_OUT("Kas Keluar")
}

@Entity(
    tableName = "cash_movements",
    foreignKeys = [
        ForeignKey(
            entity = ShiftEntity::class,
            parentColumns = ["id"],
            childColumns = ["shiftId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("shiftId")]
)
data class CashMovementEntity(
    @PrimaryKey val id: String,
    val shiftId: String,
    val type: String, // CASH_IN, CASH_OUT
    val category: String, // Bahan Baku, Operasional, Galon/Es, Setoran, Lainnya
    val amount: Double,
    val notes: String,
    val createdAt: Long
)

data class CashMovement(
    val id: String,
    val shiftId: String,
    val type: CashMovementType,
    val category: String,
    val amount: Double,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
```

### 2.2 DAO Petty Cash
File: `core/data/local/dao/CashMovementDao.kt`

```kotlin
@Dao
interface CashMovementDao {
    @Query("SELECT * FROM cash_movements WHERE shiftId = :shiftId ORDER BY createdAt DESC")
    fun observeByShift(shiftId: String): Flow<List<CashMovementEntity>>

    @Query("SELECT * FROM cash_movements WHERE shiftId = :shiftId ORDER BY createdAt DESC")
    suspend fun getByShift(shiftId: String): List<CashMovementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movement: CashMovementEntity)
}
```

### 2.3 Penambahan Kolom Void pada OrderEntity
File: `core/data/local/entity/Entities.kt` & `domain/model/Models.kt`

Tambahkan kolom pada `OrderEntity`:
- `voidReason: String? = null`
- `voidedAt: Long? = null`

Tambahkan field pada domain `Order`:
- `voidReason: String? = null`
- `voidedAt: Long? = null`

Perbarui `OrderDao.kt`:
```kotlin
@Query("UPDATE orders SET paymentStatus = 'VOID', voidReason = :reason, voidedAt = :timestamp, syncStatus = 'PENDING_SYNC' WHERE id = :orderId")
suspend fun voidOrderWithReason(orderId: String, reason: String, timestamp: Long)
```

---

## 3. Komponen Bisnis & Logika Repository

### 3.1 ShiftRepository
Menambahkan manajemen arus kas masuk dan kas keluar:
- `observeCashMovements(shiftId: String): Flow<List<CashMovement>>`
- `addCashMovement(shiftId: String, type: CashMovementType, category: String, amount: Double, notes: String): CashMovement`
- Memperbarui perhitungan kalkulasi tutup shift:
  - `totalCashSales`: Total penjualan tunai (order dengan paymentMethod == CASH & paymentStatus == PAID)
  - `totalNonCashSales`: Total penjualan QRIS & Transfer
  - `totalCashIn`: Sum kas masuk pada shift tersebut
  - `totalCashOut`: Sum kas keluar pada shift tersebut
  - `expectedCash = startCash + totalCashSales + totalCashIn - totalCashOut`
  - `selisih = actualCash - expectedCash`

### 3.2 PosRepository
- `voidOrder(orderId: String, reason: String)` meneruskan alasan pembatalan dan timestamp ke DAO.

---

## 4. Format Struk Rekap Shift (ESC/POS) & WhatsApp

### 4.1 EscPosBuilder: `buildShiftReceipt(...)`
Format baris 58mm / 80mm:
- Header: Nama Toko, Tanggal & Jam Tutup Shift
- Info Shift: Nama Kasir, Waktu Buka, Waktu Tutup
- Rincian Arus Kas:
  - Saldo Awal (Modal)
  - Penjualan Tunai
  - Kas Masuk Operasional (+)
  - Kas Keluar Operasional (-)
  - Total Kas Sistem (Diharapkan)
- Hasil Hitung Fisik:
  - Uang Fisik Terhitung
  - Selisih Kasir (Pas / Kurang / Lebih)
- Penjualan Non-Tunai: QRIS, Transfer
- Footer: Tanda tangan Kasir & Owner

### 4.2 Format Pesan WhatsApp
Fungsi format teks rapi dengan emoji untuk dikirim via Android Share Intent (`ACTION_SEND`):
```text
*REKAP SHIFT KASIR — KA POS*
📅 Tanggal: 16 Okt 2026, 22:00
👤 Kasir: Budi
━━━━━━━━━━━━━━━━━━━━
💰 Modal Awal: Rp 200.000
💵 Penjualan Tunai: Rp 1.450.000
📱 Non-Tunai (QRIS/Trf): Rp 820.000
➕ Kas Masuk: Rp 50.000
➖ Kas Keluar: Rp 75.000 (Es batu & galon)
------------------------------------
🎯 Kas Sistem: Rp 1.625.000
💵 Kas Fisik: Rp 1.625.000
⚖️ Selisih: Rp 0 (PAS ✅)
━━━━━━━━━━━━━━━━━━━━
```

---

## 5. Alur UI/UX

### 5.1 ShiftScreen
- Tampilan kartu ringkasan shift aktif:
  - Saldo Awal
  - Total Tunai Masuk
  - Kas Keluar Operasional
  - Kas Masuk Operasional
- Tombol aksi:
  - `+ Kas Masuk` (Membuka dialog kas masuk)
  - `- Kas Keluar` (Membuka dialog kas keluar)
  - `Daftar Pengeluaran/Pemasukan Laci`
  - `Tutup Shift (Blind Count)`
- Dialog Tutup Shift:
  - Mode Input Pecahan: Kasir mengisi jumlah lembar (100k, 50k, 20k, 10k, 5k, 2k, 1k) dan koin.
  - Ringkasan Otomatis: Menghitung total fisik dan menampilkan selisih secara jelas dengan badge warna.
  - Setelah shift ditutup: Menampilkan dialog hasil dengan tombol "Cetak Struk Shift" dan "Kirim Ringkasan ke WhatsApp".

### 5.2 TransactionsScreen (Void Dialog)
- Dialog Void ditingkatkan:
  - Pilihan radio button / chip alasan:
    1. Salah Input Menu
    2. Pelanggan Membatalkan
    3. Meja Pindah / Ganti Tagihan
    4. Komplain Makanan
    5. Lainnya
  - Kolom catatan tambahan (opsional/wajib jika Lainnya).
  - Kolom PIN Owner (6 digit) untuk verifikasi otorisasi.
- Pada `OrderDetailDialog`:
  - Jika transaksi berstatus `VOID`, tampilkan banner merah dengan Alasan Pembatalan dan Waktu Pembatalan.

---

## 6. Kriteria Pengujian & Verifikasi
1. **Unit Test Room Database:**
   - Menyimpan dan memuat `CashMovementEntity`.
   - Mengupdate status `VOID` dengan `voidReason`.
2. **Unit Test EscPosBuilder:**
   - Memastikan `buildShiftReceipt` menghasilkan byte ESC/POS valid tanpa crash pada mode 58mm & 80mm.
3. **Unit Test ShiftViewModel:**
   - Kalkulasi selisih kas fisik vs sistem (uji kasus seimbang, minus, dan surplus).
   - Penambahan kas masuk dan kas keluar memperbarui expected cash secara akurat.
4. **Android Build Verification:**
   - `./gradlew testDebugUnitTest` dan `./gradlew assembleDebug` berhasil tanpa error kompilasi.
