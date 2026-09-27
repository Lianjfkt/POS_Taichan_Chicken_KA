# Panduan Setup Supabase & Deployment Vercel — KA POS v2.0

Panduan praktis untuk menghubungkan KA POS dengan backend Supabase dan mendeploy ke Vercel.

---

## Bagian 1: Setup Backend Supabase (Database & Realtime)

### Langkah 1: Buat Project Baru di Supabase
1. Kunjungi [supabase.com](https://supabase.com) dan login / register akun.
2. Klik **"New Project"**.
3. Isi informasi project:
   - **Name:** `ka-pos` (atau nama restoran Anda)
   - **Database Password:** Buat password yang kuat dan catat.
   - **Region:** Pilih **Singapore (ap-southeast-1)** untuk latency tercepat ke Indonesia.
4. Klik **"Create new project"** dan tunggu 1–2 menit sampai database siap.

### Langkah 2: Eksekusi Skema Database SQL
1. Di sidebar dashboard Supabase, buka menu **SQL Editor** (ikon `>_`).
2. Klik **"New query"**.
3. Buka file `supabase_schema.sql` dari project ini, salin seluruh kodenya, dan paste ke SQL Editor.
4. Klik tombol **"Run"** (atau tekan `Ctrl+Enter`).
5. Pastikan muncul pesan `Success. No rows returned`. Semua tabel (`categories`, `products`, `orders`, `cashier_shifts`, `inventory_items`, dll), trigger pengurangan stok otomatis, dan data awal menu telah siap.

### Langkah 3: Ambil Kredensial API Supabase
1. Di sidebar Supabase, klik **Project Settings** (ikon gerigi di kiri bawah) → **API**.
2. Salin 2 nilai berikut:
   - **Project URL:** Contoh: `https://abcdefghijklm.supabase.co`
   - **Project API Keys (`anon` / `public`):** Contoh string panjang berawalan `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
3. Nilai ini yang akan Anda masukkan ke menu **Supabase Cloud** di aplikasi KA POS.

---

## Bagian 2: Hubungkan Aplikasi POS ke Supabase

1. Buka aplikasi KA POS di browser.
2. Login sebagai **Owner** (`owner` / `owner123`).
3. Di menu sidebar kiri, buka menu **Supabase Cloud**.
4. Masukkan **Project URL** dan **Anon Key** yang sudah disalin di langkah sebelumnya.
5. Klik **"Test Koneksi"**. Indikator status akan berubah menjadi hijau **"Terhubung"**.
6. Klik **"Push Data Lokal ke Supabase"** untuk mengunggah transaksi dan data lokal yang sudah ada.

---

## Bagian 3: Deployment ke Vercel

### Opsi A: Deploy via GitHub (Paling Direkomendasikan & Otomatis)
1. Buat repository baru di [GitHub](https://github.com/new) bernama `ka-pos`.
2. Push folder project ini ke repository GitHub:
   ```bash
   git init
   git add .
   git commit -m "feat: initial KA POS with Supabase and Vercel support"
   git branch -M main
   git remote add origin https://github.com/<username-anda>/ka-pos.git
   git push -u origin main
   ```
3. Buka [vercel.com](https://vercel.com) dan login dengan akun GitHub Anda.
4. Klik **"Add New..."** → **"Project"**.
5. Pilih repository `ka-pos` yang baru Anda buat, lalu klik **"Import"**.
6. Pada pengaturan project:
   - **Framework Preset:** Pilih `Other` (karena aplikasi saat ini adalah static HTML/PWA).
   - **Root Directory:** `./`
7. Klik **"Deploy"**.
8. Dalam hitungan detik, aplikasi Anda live dengan URL berakhiran `.vercel.app` (misal: `https://ka-pos-taichan.vercel.app`) dan SSL/HTTPS aktif otomatis!

### Opsi B: Deploy Langsung via Vercel CLI
Jika Anda memiliki Vercel CLI di komputer:
```bash
npm install -g vercel
vercel login
vercel --prod
```

---

## Bagian 4: Fitur PWA (Install di HP / Tablet Kasir & Dapur)

Setelah terdeploy di Vercel:
1. Buka URL Vercel di Google Chrome (Android/Laptop) atau Safari (iPhone/iPad).
2. Di Android/Chrome: Klik titik tiga di kanan atas → **"Install App"** atau **"Tambahkan ke Layar Utama"**.
3. Di iOS/Safari: Klik tombol Share (ikon panah ke atas) → **"Add to Home Screen"**.
4. Aplikasi akan terinstall sebagai aplikasi native layar penuh (tanpa bilah URL browser), dapat bekerja offline, dan siap digunakan di meja kasir maupun layar dapur.
