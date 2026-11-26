# Design Spec: Backdated Restock & Anti-Fraud Stock Ingestion

**Date:** 2026-11-02  
**Status:** Draft  
**Scope:** Android App (`StockScreen`, `RawMaterialDialogs`, `InventoryRepository`, Room Database `material_batches` & `stock_movements`)

---

## 1. Context & Business Problem
Ketika owner atau staf lupa menginput penerimaan stok bahan baku di hari kedatangannya (misal bahan tiba kemarin), mereka baru menginputnya keesokan harinya.
Jika sistem hanya mencatat waktu saat ini (`System.currentTimeMillis()`):
1. **Perhitungan kadaluarsa bergeser:** Umur kadaluarsa di sistem menjadi lebih panjang dari kondisi fisik aslinya di dapur.
2. **Prioritas FIFO terganggu:** Bahan yang datang lebih dulu tidak diprioritaskan sebagaimana mestinya.
3. **Potensi Celah Fraud:** Tanpa audit trail pencatatan ganda (*received date* vs *entry date*), staf bisa menyusupkan mutasi barang lama untuk menutupi selisih stok (kehilangan/pencurian).

---

## 2. Core Requirements & Constraints

1. **Input Tanggal Masuk (Received Date) dengan Kalender:**
   - Menyediakan pemilih tanggal kalender standar (Material 3 `DatePickerDialog`) di dialog restock.
   - Default: **Hari Ini**.
2. **Batasan Ketat Tanggal (Anti-Tampering):**
   - Batas maksimal backdate adalah **7 hari ke belakang** dari hari ini.
   - Tanggal di masa depan (**future date**) **dilarang keras**.
   - Divalidasi di sisi UI (`SelectableDates`) dan di sisi domain/repository (`require`).
3. **Perhitungan Otomatis Kadaluarsa Relatif:**
   - Shortcut masa simpan (`+3 Hari`, `+7 Hari`, `+14 Hari`, `+30 Hari`) dihitung dari **Tanggal Masuk Barang** (`receivedDate`), bukan tanggal input hari ini.
   - Rumus: $\text{expiryDate} = \text{receivedDate} + (\text{days} \times 24\text{ jam})$.
   - Jika hasil perhitungan kadaluarsa ternyata $\le \text{waktu sekarang}$ (sudah expired saat diinput), sistem menampilkan peringatan visual di dialog dan otomatis menetapkan status batch sebagai `EXPIRED` saat disimpan.
4. **Audit Trail Anti-Fraud (Transparansi Mutasi):**
   - Di `StockMovementEntity` dan `MaterialBatchEntity`:
     - Jika tanggal barang masuk berbeda hari dengan hari penginputan, sistem secara otomatis membubuhkan watermark audit pada catatan:
       `[BACKDATED: Diterima dd/MM/yyyy | Diinput dd/MM/yyyy HH:mm]`.
     - Kasir/staf tidak dapat menghapus atau memanipulasi watermark ini.
5. **Kesesuaian Urutan FIFO & Moving Weighted Average:**
   - Batch number dihasilkan menggunakan tanggal kedatangan: `B-yyyyMMdd-...`.
   - Urutan konsumsi FIFO di Room (`getActiveBatchesForFIFO`) otomatis mengurutkan batch backdated ini sesuai tanggal kedaluarsa dan tanggal penerimaan aslinya.
   - Master bahan baku (`raw_materials.lastRestockedAt`) diperbarui dengan `maxOf(currentLastRestockedAt, receivedDate)`.

---

## 3. Architecture & Data Flow

```
[ RestockMaterialDialog ]
       │
       ├── User memilih Tanggal Diterima via DatePickerDialog (Max: H-7 s/d Hari Ini)
       ├── User memilih Masa Simpan (+3, +7, dst.)
       ├── Preview: Tanggal Kadaluarsa (dihitung dari Tanggal Diterima) & Peringatan jika sudah expired
       │
       ▼ (onConfirm: quantity, cost, expiryDate, notes, receivedDate)
[ StockViewModel.restockWithBatch ]
       │
       ▼
[ InventoryRepository.restockMaterialWithBatch ]
       │
       ├── Anti-fraud validation: require(receivedDate in (now - 7 days)..now)
       ├── Format Batch Number: B-{receivedDate_yyyyMMdd}-{seq/time}
       ├── Generate Anti-fraud audit note if backdated
       ├── Insert MaterialBatchEntity (receivedDate, expiryDate, costPerUnit, notes)
       ├── Calculate Moving Weighted Average Cost (anti-divide-by-zero safe)
       ├── Update RawMaterialEntity (currentStock, costPerUnit, lastRestockedAt, expiryState)
       ├── Insert StockMovementEntity (timestamp = receivedDate, notes = auditWatermark + notes)
       └── Cascade BOP Recalculation (products.costPrice) via recipes
```

---

## 4. Component Changes

### 4.1 UI Component: `RawMaterialDialogs.kt` (`RestockMaterialDialog`)
- Tambah field interaktif: **Tanggal Masuk Barang**.
- Menampilkan teks tanggal terpilih (format: `dd MMM yyyy`, default: Hari Ini).
- Klik tombol/field membuka M3 `DatePickerDialog`:
  - `SelectableDates` membatasi range `[startOfDay(now - 7 days) .. endOfDay(now)]`.
- Preview perhitungan masa kadaluarsa yang reaktif terhadap perubahan `selectedReceivedDate` dan `selectedExpiryDays`.
- Tampilan peringatan merah jika kombinasi tanggal menyebabkan batch sudah kadaluarsa saat ini.

### 4.2 Domain & Repository: `InventoryRepository.kt`
- Update fungsi `restockMaterialWithBatch`:
  ```kotlin
  suspend fun restockMaterialWithBatch(
      materialId: String,
      quantity: Double,
      costTotal: Double,
      expiryDate: Long? = null,
      notes: String = "",
      receivedDate: Long = System.currentTimeMillis()
  ): MaterialBatch
  ```
- Validasi keamanan:
  ```kotlin
  val now = System.currentTimeMillis()
  val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000)
  require(receivedDate in sevenDaysAgo..now + 60_000L) {
      "Tanggal penerimaan tidak valid (maksimal 7 hari ke belakang dan tidak boleh di masa depan)"
  }
  ```
- Pembentukan catatan anti-fraud otomatis:
  ```kotlin
  val isBackdated = !isSameDay(receivedDate, now)
  val auditSuffix = if (isBackdated) {
      val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
      val timeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
      " [BACKDATED: Diterima ${dateFormat.format(Date(receivedDate))} | Diinput ${timeFormat.format(Date(now))}]"
  } else ""
  val finalNotes = (notes.trim() + auditSuffix).trim()
  ```

### 4.3 ViewModel: `StockViewModel.kt`
- Update parameter pemanggilan ke `inventoryRepository.restockMaterialWithBatch(...)` untuk meneruskan `receivedDate`.

---

## 5. Error Handling & Edge Cases
1. **Pilihan Tanggal di Luar 7 Hari:** Kalender menonaktifkan (*disable*) tanggal di luar rentang H-7 s/d Hari Ini. Jika ada bypass, repository melempar `IllegalArgumentException`.
2. **Bahan Sudah Kadaluarsa Saat Diinput:** Sistem tetap mengizinkan input (misal untuk mencatat barang yang telat dicatat dan langsung di-waste), namun status batch langsung berlabel `EXPIRED` dan badge peringatan bahan langsung menyala merah.
3. **Kuantitas atau Biaya $\le 0$:** Validasi tetap ketat melempar error validasi sebelum transaksi database disimpan.
4. **Manipulasi Jam/Detik:** `receivedDate` dinormalisasi ke waktu tengah hari atau jam kedatangan saat ini agar tidak terjadi anomali urutan millisecond.

---

## 6. Testing Strategy
- **Unit Tests (`InventoryRepositoryTest`):**
  - Menguji `restockMaterialWithBatch` dengan `receivedDate` kemarin (H-1).
  - Menguji validasi gagal jika `receivedDate` > now (masa depan).
  - Menguji validasi gagal jika `receivedDate` < 7 hari lalu.
  - Memverifikasi watermark audit anti-fraud tertulis di `StockMovementEntity.notes` dan `MaterialBatchEntity.notes`.
  - Memverifikasi `expiryDate` tepat bernilai `receivedDate + (N * 24h)`.
- **UI State & Date Validation Tests:**
  - Memverifikasi batas pemilihan kalender tepat 7 hari.
