# 📋 KA POS — DO NEXT Task List

> File ini berisi daftar tugas/fitur yang akan dikerjakan berikutnya.
> Update file ini untuk menambah, mengedit, atau menandai tugas selesai.
> **Panggil task ini di sesi berikutnya:** *"kerjakan DO_NEXT.md"* atau *"kerjakan TASK 1 di DO_NEXT.md"*

---

## 🔴 PRIORITAS TINGGI

---

### TASK 1 — Pembatasan Akses Riwayat Transaksi untuk Kasir

**Status:** ⏳ Belum dikerjakan

**Deskripsi:**
Kasir hanya boleh melihat transaksi dan dapat mengajukan refund jika diperlukan, **TANPA bisa melihat nominal penjualan (Rp)**.

**Detail Akses:**

| Kolom / Info | Kasir | Owner |
|---|---|---|
| No. Nota & Waktu | ✅ Bisa lihat | ✅ Bisa lihat |
| Pelanggan & Kasir | ✅ Bisa lihat | ✅ Bisa lihat |
| Detail Item Menu (nama + qty) | ✅ Bisa lihat | ✅ Bisa lihat |
| Metode Bayar | ✅ Bisa lihat | ✅ Bisa lihat |
| Status (Selesai / Void) | ✅ Bisa lihat | ✅ Bisa lihat |
| **Total Nominal (Rp)** | ❌ Disembunyikan | ✅ Bisa lihat |
| KPI: Total Omset | ❌ Disembunyikan | ✅ Bisa lihat |
| KPI: Rata-rata / Nota | ❌ Disembunyikan | ✅ Bisa lihat |
| KPI: Nota Sukses (count) | ✅ Bisa lihat | ✅ Bisa lihat |
| KPI: Batal / Void (count) | ✅ Bisa lihat | ✅ Bisa lihat |
| Tombol Ekspor CSV | ❌ Disembunyikan | ✅ Bisa lihat |
| Tombol Refund / Void | ✅ Bisa akses | ✅ Bisa akses |

**File yang perlu diubah:**
- `js/views/orders.js`
  - `renderKPIs()` → sembunyikan nilai Rp jika `role === 'kasir'`, tampilkan `—`
  - `renderList()` → kolom total (Rp) render sebagai `•••` atau `—` jika kasir
  - Sembunyikan tombol Ekspor CSV jika kasir
- `index.html`
  - Tombol Ekspor CSV → tambahkan class `owner-only`
  - Header pill omzet (`header-omzet-val`) → sembunyikan jika kasir
- `js/app.js` atau `js/state.js` → tambahkan helper `State.isOwner()` jika belum ada

**Catatan Teknis:**
- Cek role via `window.State.currentUser?.role === 'owner'`
- Bisa pakai pattern: set class `role-kasir` atau `role-owner` pada `<body>` saat login
- CSS: `.role-kasir .owner-only-value { visibility: hidden; }` untuk hide nilai tanpa geser layout

---

### TASK 2 — Kasir: Update Stock Harian Tanpa Melihat Total Nilai + Audit Trail

**Status:** ⏳ Belum dikerjakan

**Deskripsi:**
Kasir dapat melakukan **update stok harian** (input jumlah bahan baku tersedia/terpakai), namun **tidak bisa melihat harga & total nilai stok (Rp)**. Setiap perubahan stok oleh kasir dicatat sebagai **audit log** agar owner bisa mendeteksi ketidaksesuaian.

**Detail Akses:**

| Fitur Stok | Kasir | Owner |
|---|---|---|
| Lihat daftar bahan baku (nama + satuan) | ✅ Bisa | ✅ Bisa |
| Input / update qty stok | ✅ Bisa update | ✅ Bisa update |
| Lihat qty saat ini & batas minimum | ✅ Bisa lihat | ✅ Bisa lihat |
| Status stok (aman / kritis / habis) | ✅ Bisa lihat | ✅ Bisa lihat |
| **Harga per unit bahan baku** | ❌ Tersembunyi | ✅ Bisa lihat |
| **Total nilai stok (Rp)** | ❌ Tersembunyi | ✅ Bisa lihat |
| **Total pengeluaran / nilai pemakaian** | ❌ Tersembunyi | ✅ Bisa lihat |
| Tambah bahan baku baru | ❌ Tidak boleh | ✅ Bisa |
| Edit / hapus bahan baku | ❌ Tidak boleh | ✅ Bisa |
| Lihat Log Audit perubahan stok | ❌ Tidak boleh | ✅ Bisa lihat |

**Audit Trail — Format Log:**
```json
{
  "id": "uuid",
  "timestamp": "2026-10-08T09:00:00+07:00",
  "kasir": "nama_kasir",
  "item_id": "bahan_baku_id",
  "item_name": "Ayam Taichan",
  "qty_before": 50,
  "qty_after": 35,
  "delta": -15,
  "keterangan": "Update harian"
}
```

**Storage:** `localStorage` key `ka_stock_audit_log` (array, max 500 entri, FIFO)

**File yang perlu diubah:**
- `js/state.js`
  - Tambahkan `addStockAuditLog(entry)` — push ke array, trim jika > 500
  - Tambahkan `getStockAuditLog()` — return array sorted by timestamp desc
- `js/views/inventory.js`
  - Sembunyikan kolom harga & total nilai jika kasir
  - Sembunyikan tombol "Tambah Bahan Baku" & "Edit" & "Hapus" jika kasir
  - Tambahkan tab baru: **"Log Audit"** (owner only)
  - Di tab Log Audit: tabel dengan kolom Waktu, Kasir, Item, Sebelum, Sesudah, Selisih
- `js/views/stock_tracker.js`
  - Saat kasir submit update stok → panggil `State.addStockAuditLog(entry)` sebelum save
- `index.html`
  - Pastikan tombol owner-only di inventory punya class `owner-only`

---

## 🟡 PRIORITAS MENENGAH

*(Tambahkan task di sini)*

---

## 🟢 PRIORITAS RENDAH / NICE TO HAVE

*(Tambahkan task di sini)*

---

## ✅ SELESAI

- [x] Semua icon makanan diganti dari emoji ke SVG vector (FoodIcons system)
- [x] Pisahkan CSS web.css dan mobile.css agar tidak saling tumpang tindih
- [x] Desktop sidebar diperlebar (220px) dengan label teks horizontal
- [x] Sidebar brand area: logo + nama app + subtitle
- [x] KPI cards desktop: 4-column grid dengan accent strip
- [x] Tambahkan komponen panel-card ke design system
- [x] PWA offline support untuk semua asset baru

---

*Terakhir diupdate: 2026-10-08*
