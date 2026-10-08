# 📋 KA POS — DO NEXT Task List

> File ini berisi daftar tugas/fitur yang akan dikerjakan berikutnya.
> Update file ini untuk menambah, mengedit, atau menandai tugas selesai.
> **Panggil task ini di sesi berikutnya:** *"kerjakan DO_NEXT.md"* atau *"kerjakan TASK 1 di DO_NEXT.md"*

---

## 🔴 PRIORITAS TINGGI

---

### TASK 1 — Pembatasan Akses Riwayat Transaksi untuk Kasir

**Status:** ✅ Selesai (2026-10-08)

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

---

### TASK 2 — Kasir: Update Stock Harian Tanpa Melihat Total Nilai + Audit Trail

**Status:** ✅ Selesai (2026-10-08)

**Deskripsi:**
Kasir dapat melakukan **update stok harian** (input jumlah bahan baku tersedia/terpakai), namun **tidak bisa melihat harga & total nilai stok (Rp)** serta rekonsiliasi total proses & terjual disembunyikan (blind audit). Setiap perubahan stok oleh kasir dicatat sebagai **audit log** (`State.addStockAuditLog`) agar owner bisa mendeteksi ketidaksesuaian fisik.

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

---

## 🟡 PRIORITAS MENENGAH

*(Tambahkan task baru di sini ketika ada kebutuhan)*

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
- [x] TASK 1: Pembatasan akses riwayat transaksi & nominal penjualan untuk kasir (masked `••••`, omzet masked `—`, modal rincian masked)
- [x] TASK 2: Update stock harian kasir blind audit (total proses tersembunyi) + pencatatan log audit stok & tab Log Audit untuk Owner

---

*Terakhir diupdate: 2026-10-08*
