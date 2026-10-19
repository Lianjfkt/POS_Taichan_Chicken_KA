# Spesifikasi Desain: Fitur Custom Date Data Penjualan (POS KA)

**Tanggal:** 19 Oktober 2026  
**Status:** Disetujui (Approved)  
**Tujuan:** Menambahkan fungsionalitas pemilihan tanggal kustom (custom date range) di menu **Riwayat Transaksi**, **Laporan Keuangan**, dan **Analisa BEP** agar pemilik dan kasir dapat meninjau data penjualan pada periode tertentu secara fleksibel.

---

## 1. Latar Belakang & Kebutuhan

Saat ini, sistem POS KA memiliki beberapa batasan terkait penyaringan data:
- **Riwayat Transaksi (`TransactionsScreen`)**: Hanya mendukung filter status pembayaran (Semua, Lunas, Pending) dan pencarian teks, tanpa filter tanggal.
- **Laporan Keuangan (`ReportScreen`)**: Hanya memiliki opsi preset statis (`TODAY`, `WEEK`, `MONTH`, `ALL`) tanpa kemampuan memilih tanggal tertentu atau rentang tanggal spesifik.
- **Analisa Keuangan & BEP (`AnalyticsScreen`)**: Hanya memiliki opsi preset statis (`TODAY`, `WEEK`, `MONTH`).

Pengguna membutuhkan opsi yang fleksibel: preset cepat (Hari Ini, 7 Hari, Bulan Ini, Semua) ditambah kemampuan memilih rentang tanggal kustom (Start Date - End Date) menggunakan komponen kalender interaktif Material 3.

---

## 2. Arsitektur & Model Data

### 2.1 Model Terpadu (`com.kapos.mobile.domain.model.DateRangeFilter`)

Dibuat di layer domain/core agar dapat digunakan bersama oleh seluruh fitur:

```kotlin
package com.kapos.mobile.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DateFilterPreset(val displayName: String) {
    TODAY("Hari Ini"),
    LAST_7_DAYS("7 Hari"),
    THIS_MONTH("Bulan Ini"),
    CUSTOM("Pilih Tanggal"),
    ALL("Semua")
}

data class DateRangeFilter(
    val preset: DateFilterPreset = DateFilterPreset.TODAY,
    val customStartMillis: Long? = null,
    val customEndMillis: Long? = null
) {
    /**
     * Mengembalikan pasangan timestamp [startMillis, endMillis] (keduanya inklusif).
     * Waktu disesuaikan dengan timezone lokal perangkat:
     * - Awal hari: 00:00:00.000
     * - Akhir hari: 23:59:59.999
     */
    fun getTimeRange(nowMillis: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return when (preset) {
            DateFilterPreset.TODAY -> {
                cal.timeInMillis = nowMillis
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            DateFilterPreset.LAST_7_DAYS -> {
                cal.timeInMillis = nowMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                val start = end - (7L * 24 * 60 * 60 * 1000 - 1000)
                Pair(start, end)
            }
            DateFilterPreset.THIS_MONTH -> {
                cal.timeInMillis = nowMillis
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            DateFilterPreset.ALL -> {
                Pair(0L, Long.MAX_VALUE)
            }
            DateFilterPreset.CUSTOM -> {
                val startRaw = customStartMillis ?: nowMillis
                val endRaw = customEndMillis ?: startRaw

                val startCal = Calendar.getInstance().apply {
                    timeInMillis = minOf(startRaw, endRaw)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val endCal = Calendar.getInstance().apply {
                    timeInMillis = maxOf(startRaw, endRaw)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                Pair(startCal.timeInMillis, endCal.timeInMillis)
            }
        }
    }

    /**
     * Label teks yang rapi untuk ditampilkan pada UI Chip, Header Ekspor, dan Struk.
     */
    fun getDisplayLabel(): String {
        return when (preset) {
            DateFilterPreset.TODAY -> "Hari Ini"
            DateFilterPreset.LAST_7_DAYS -> "7 Hari"
            DateFilterPreset.THIS_MONTH -> "Bulan Ini"
            DateFilterPreset.ALL -> "Semua"
            DateFilterPreset.CUSTOM -> {
                if (customStartMillis == null) return "Pilih Tanggal"
                val start = customStartMillis
                val end = customEndMillis ?: start
                val sdfSameYear = SimpleDateFormat("dd MMM", Locale("id", "ID"))
                val sdfFull = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

                val calStart = Calendar.getInstance().apply { timeInMillis = start }
                val calEnd = Calendar.getInstance().apply { timeInMillis = end }

                if (calStart.get(Calendar.YEAR) == calEnd.get(Calendar.YEAR) &&
                    calStart.get(Calendar.DAY_OF_YEAR) == calEnd.get(Calendar.DAY_OF_YEAR)) {
                    sdfFull.format(Date(start))
                } else if (calStart.get(Calendar.YEAR) == calEnd.get(Calendar.YEAR)) {
                    "${sdfSameYear.format(Date(start))} - ${sdfFull.format(Date(end))}"
                } else {
                    "${sdfFull.format(Date(start))} - ${sdfFull.format(Date(end))}"
                }
            }
        }
    }
}
```

---

## 3. Komponen UI Terpadu

Dibuat di `com.kapos.mobile.core.components`:

### 3.1 `DateFilterBar`
- Baris chip horizontal scrollable:
  - Chip preset: "Hari Ini", "7 Hari", "Bulan Ini", "Semua".
  - Chip custom: Ikon kalender + label dinamis ("📅 Pilih Tanggal" atau "📅 10 Okt - 15 Okt").
- Status aktif ditandai dengan warna primer `Primary`, teks putih, dan border halus.
- Mengklik chip preset langsung memperbarui filter.
- Mengklik chip custom memicu callback `onOpenCustomDatePicker()`.

### 3.2 `DateRangePickerDialog`
- Dialog Jetpack Compose yang membungkus `androidx.compose.material3.DateRangePicker` & `androidx.compose.material3.DatePickerDialog`.
- Mendukung pemilihan tanggal tunggal (single day) atau rentang dua tanggal.
- Tombol:
  - "Batal": Menutup dialog tanpa perubahan.
  - "Terapkan": Mengambil `selectedStartDateMillis` dan `selectedEndDateMillis`, mengonversinya ke `DateRangeFilter(DateFilterPreset.CUSTOM, start, end)` dan menutup dialog.

---

## 4. Integrasi ke Layar Aplikasi

### 4.1 Riwayat Transaksi (`feature/transactions`)
- **State (`TransactionsUiState`)**:
  - `dateFilter: DateRangeFilter = DateRangeFilter(preset = DateFilterPreset.TODAY)`
  - `showDatePickerDialog: Boolean = false`
- **Filtering (`filteredOrders`)**:
  - Mengambil `(startCutoff, endCutoff) = dateFilter.getTimeRange()`.
  - Memfilter daftar pesanan: `it.createdAt in startCutoff..endCutoff`.
  - Berpadu mulus dengan filter `paymentStatus` (Semua / Lunas / Pending) dan `searchQuery`.
- **UI (`TransactionsScreen`)**:
  - Menempatkan `DateFilterBar` tepat di antara `SearchField` dan chip status pembayaran (`FilterTabs`).

### 4.2 Laporan Keuangan (`feature/report`)
- **State (`ReportUiState`)**:
  - Mengganti `selectedPeriod: ReportPeriod` menjadi `dateFilter: DateRangeFilter = DateRangeFilter(preset = DateFilterPreset.TODAY)`.
  - `showDatePickerDialog: Boolean = false`.
  - Menghitung `periodOrders`, `periodVoidOrders`, `periodCashMovements`, dan `periodOrderItems` berdasarkan `dateFilter.getTimeRange()`.
- **Ekspor CSV**:
  - `exportTransactionsCsv` dan `exportFinancialSummaryCsv` menggunakan `dateFilter.getDisplayLabel()` sebagai nama periode pada metadata file dan nama file yang dibagikan.
- **Cetak Struk Ringkasan Laporan**:
  - Mengirimkan `dateFilter.getDisplayLabel()` ke `EscPosBuilder.buildFinancialReport` agar tercetak jelas pada struk fisik.
- **UI (`ReportScreen`)**:
  - Mengintegrasikan `DateFilterBar` dan dialog picker pada header laporan.

### 4.3 Analisa Keuangan & BEP (`feature/analytics`)
- **State (`AnalyticsUiState`)**:
  - Mengadopsi `dateFilter: DateRangeFilter = DateRangeFilter(preset = DateFilterPreset.TODAY)`.
  - `showDatePickerDialog: Boolean = false`.
  - Menghitung metrik BEP, total penjualan kas vs non-kas, dan margin dalam periode `dateFilter.getTimeRange()`.
- **UI (`AnalyticsScreen`)**:
  - Menampilkan `DateFilterBar` dan dialog picker pada header analisa.

---

## 5. Penanganan Kasus Khusus (Edge Cases)

1. **Pemilihan 1 Hari (Single Date Click)**:
   Jika pengguna hanya memilih 1 tanggal di `DateRangePicker` (start terisi, end null), sistem otomatis menganggap rentang waktu adalah awal hari tersebut (00:00:00) hingga akhir hari (23:59:59.999).
2. **Kondisi Tanpa Data (Empty State)**:
   Jika tidak ada transaksi pada rentang tanggal yang dipilih, UI menampilkan pesan informatif yang ramah dan tidak melempar error pembagian nol pada perhitungan rata-rata atau margin.
3. **Persistensi State**:
   Filter default adalah "Hari Ini" saat membuka layar. Pilihan tanggal dipertahankan selama berada di ViewModel layar tersebut.

---

## 6. Rencana Pengujian (Testing Plan)

1. **Unit Test `DateRangeFilterTest`**:
   - Uji batas waktu `TODAY` (tepat 00:00:00 s.d 23:59:59).
   - Uji rentang `LAST_7_DAYS` dan `THIS_MONTH`.
   - Uji custom date: single-day range dan multi-day range.
   - Uji format label `getDisplayLabel()`.
2. **Unit Test `TransactionsDateFilterTest`**:
   - Memastikan pesanan di luar rentang tanggal tidak muncul di `filteredOrders`.
   - Memastikan kombinasi filter tanggal + status lunas + query pencarian berjalan konsisten.
3. **Unit Test `ReportDateFilterTest`**:
   - Memastikan perhitungan revenue, COGS, dan laba bersih hanya menghitung pesanan dalam rentang tanggal yang dipilih.
