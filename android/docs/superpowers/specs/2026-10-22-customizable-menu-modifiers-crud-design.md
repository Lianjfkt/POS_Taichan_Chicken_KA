# Design Specification: Customizable Menu Modifiers CRUD & Product Association

- **Date**: 2026-10-22
- **Topic**: Dynamic CRUD for Menu Modifiers (Level Pedas, Sambal, Pelengkap & Alat Makan, Ekstra Topping & Kemasan) & Per-Product Association
- **Target Platform**: Android Native (Jetpack Compose, Room Database, Kotlin Coroutines & Flow, Supabase REST Sync)

---

## 1. Executive Summary & Problem Statement

### 1.1 Existing Problem
Di versi sebelumnya, kustomisasi produk di halaman kasir (`POS`) menampilkan modal `ModifierDialog` secara statis untuk **semua produk**, tanpa membedakan apakah produk tersebut berupa makanan utama (Sate/Ayam) atau minuman (Es Teh/Air Mineral) atau nasi pulen.
Popup dialog tersebut secara paksa menampilkan 4 seksi sekaligus:
1. Level Pedas / Varian (Original, Lv.1 – Lv.5)
2. Pilihan Sambal (Sambal Taichan Ori, Sambal Bawang, Sambal Matah, Sambal Ijo)
3. Pelengkap & Alat Makan (Sarung Tangan Plastik, Sedotan, Sendok Garpu, Tisu)
4. Ekstra Topping & Kemasan (Ekstra Sambal, Jeruk Nipis, Bawang Goreng, Keju Melt, Kotak, Paper Bag)

Semua opsi ini bersumber dari hardcoded data (`PresetModifiers.allOptions`) sehingga:
- Kasir/pemilik resto tidak dapat menambah sambal baru, mengubah harga topping, atau menambah level pedas baru.
- Menu non-pedas/minuman (misal Es Teh) ikut memunculkan opsi sambal dan keju melt, menyebabkan UI/UX menumpuk dan memperlambat kecepatan transaksi kasir.

### 1.2 Proposed Solution
1. **CRUD Modifiers**: Menyediakan manajemen CRUD penuh untuk opsi Level Pedas, Pilihan Sambal, Pelengkap & Alat Makan, serta Ekstra Topping & Kemasan di layar Manajemen Menu (Tab `Varian & Modifier`).
2. **Product-Modifier Association**: Setiap produk memiliki opsi pengaturan modifier mana saja yang diizinkan (`allowSpiceLevel`, `allowSambal`, `allowComplimentary`, `allowToppings`).
3. **Streamlined POS Flow**:
   - Produk tanpa modifier aktif (misal: Es Teh, Air Mineral, Nasi Putih) **langsung masuk keranjang (+1)** dengan 1 kali ketuk tanpa dialog popup.
   - Produk dengan modifier aktif (misal: Sate Taichan) membuka `ModifierDialog` yang **hanya menampilkan seksi yang diizinkan**, dengan data dinamis dari database Room.

---

## 2. Architecture & Data Model

### 2.1 Modifier Categories
```kotlin
enum class ModifierCategory(val displayName: String) {
    SPICE_LEVEL("Level Pedas"),
    SAMBAL("Pilihan Sambal"),
    COMPLIMENTARY("Pelengkap & Alat Makan"),
    EXTRA("Ekstra & Topping"),
    PACKAGING("Kemasan")
}
```

### 2.2 Room Entity: `ModifierEntity`
```kotlin
@Entity(tableName = "modifiers")
data class ModifierEntity(
    @PrimaryKey val id: String,
    val name: String,
    val price: Double = 0.0,
    val category: String, // SPICE_LEVEL, SAMBAL, COMPLIMENTARY, EXTRA, PACKAGING
    val isDefault: Boolean = false,
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)
```

### 2.3 Product Updates (`ProductEntity` & `Product`)
Menambahkan 4 flag boolean pada `ProductEntity` & domain model `Product`:
- `allowSpiceLevel: Boolean = false`
- `allowSambal: Boolean = false`
- `allowComplimentary: Boolean = false`
- `allowToppings: Boolean = false`

*Catatan Migrasi & Default*:
Untuk produk yang sudah ada di database:
- Jika kategori produk adalah `cat_taichan` atau `cat_chicken` (atau `spiceLevel` tidak kosong): `allowSpiceLevel = true`, `allowSambal = true`, `allowToppings = true`.
- Jika kategori produk adalah `cat_drinks` atau `cat_sides`: `allowSpiceLevel = false`, `allowSambal = false`, `allowToppings = false`.

### 2.4 Supabase Schema & Sync Updates
- Tabel baru `public.modifiers`:
  ```sql
  CREATE TABLE IF NOT EXISTS public.modifiers (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      price NUMERIC(12, 2) NOT NULL DEFAULT 0,
      category TEXT NOT NULL,
      is_default BOOLEAN DEFAULT FALSE,
      is_active BOOLEAN DEFAULT TRUE,
      sort_order INT DEFAULT 0,
      created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()),
      updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW())
  );
  ALTER TABLE public.modifiers ENABLE ROW LEVEL SECURITY;
  DROP POLICY IF EXISTS "Allow all for modifiers" ON public.modifiers;
  CREATE POLICY "Allow all for modifiers" ON public.modifiers
      FOR ALL USING (TRUE) WITH CHECK (TRUE);
  ```
- Kolom baru di `public.products`:
  ```sql
  ALTER TABLE public.products ADD COLUMN IF NOT EXISTS allow_spice_level BOOLEAN DEFAULT FALSE;
  ALTER TABLE public.products ADD COLUMN IF NOT EXISTS allow_sambal BOOLEAN DEFAULT FALSE;
  ALTER TABLE public.products ADD COLUMN IF NOT EXISTS allow_complimentary BOOLEAN DEFAULT FALSE;
  ALTER TABLE public.products ADD COLUMN IF NOT EXISTS allow_toppings BOOLEAN DEFAULT FALSE;
  ```

---

## 3. UI/UX Specifications

### 3.1 Menu Management Screen (`MenuManagementScreen.kt`)
1. **Tabs**:
   - `[Daftar Produk]` (Menampilkan daftar menu, harga, margin, status modifier)
   - `[Kategori Menu]` (CRUD kategori menu)
   - `[Varian & Modifier]` (CRUD modifier options)
2. **Tab "Varian & Modifier"**:
   - Filter Chips berdasarkan kategori: `Semua`, `Level Pedas`, `Pilihan Sambal`, `Pelengkap & Alat`, `Ekstra Topping & Kemasan`.
   - Card Modifier: Nama, Kategori, Harga (misal `Gratis` atau `+Rp 3.000`), status default, switch/toggle aktif, tombol edit & hapus.
   - Tombol FAB / Header: `+ Tambah Modifier`.
   - Dialog Tambah/Edit Modifier:
     - Input Nama Modifier (misal: "Sambal Matah Bali", "Lv. 6 Super Pedas", "Keju Mozzarella")
     - Pilihan Kategori (Level Pedas, Sambal, Pelengkap/Alat, Ekstra/Topping, Kemasan)
     - Input Harga Tambahan (Rp 0 untuk gratis)
     - Checkbox "Jadikan Default" (opsional, misal sambal taichan ori / lv. 0)
3. **Dialog Tambah / Edit Produk (`AddEditProductDialog`)**:
   - Seksi baru: **Pengaturan Kustomisasi & Modifier Kasir**:
     - Checkbox: `[✓] Aktifkan Level Pedas`
     - Checkbox: `[✓] Aktifkan Pilihan Sambal`
     - Checkbox: `[✓] Aktifkan Pelengkap & Alat Makan`
     - Checkbox: `[✓] Aktifkan Ekstra Topping & Kemasan`
   - *Smart Autofill*: Saat user memilih kategori "Minuman" atau "Nasi & Ekstra", checkbox pedas & sambal otomatis tidak tercentang; saat memilih "Sate Taichan" atau "Ayam Goreng", otomatis tercentang. User tetap dapat mengubah centang secara manual.

### 3.2 POS Screen (`PosScreen.kt` & `ModifierDialog.kt`)
1. **Fast-Tap Add to Cart**:
   - Ketika kasir mengetuk item produk di grid menu:
     ```kotlin
     val hasModifiers = product.allowSpiceLevel || product.allowSambal || product.allowComplimentary || product.allowToppings
     if (!hasModifiers) {
         // Langsung masuk keranjang (+1)
         viewModel.addToCart(product)
     } else {
         // Buka ModifierDialog yang telah disaring
         viewModel.onProductClick(product)
     }
     ```
2. **Compact Dynamic `ModifierDialog.kt`**:
   - Data modifier diambil secara dinamis dari Repository (`observeActiveModifiers()`).
   - Seksi hanya dirender jika diizinkan:
     - `if (product.allowSpiceLevel && spiceOptions.isNotEmpty())` -> Render chip level pedas.
     - `if (product.allowSambal && sambalOptions.isNotEmpty())` -> Render chip pilihan sambal.
     - `if (product.allowComplimentary && complimentaryOptions.isNotEmpty())` -> Render chip alat makan (disaring sesuai makanan/minuman).
     - `if (product.allowToppings && toppingOptions.isNotEmpty())` -> Render chip ekstra topping & kemasan.
     - Seksi Catatan Khusus & Stepper Quantity tetap tersedia.
   - Dialog menjadi ramping, rapi, dan tidak memakan tinggi layar secara berlebihan.

---

## 4. Testing & Verification Plan

1. **Unit Testing**:
   - `ModifierDaoTest`: Uji operasi CRUD Room (insert, query by category, update, delete).
   - `MenuManagementViewModelTest`: Uji pengelolaan state modifier (add, update, delete, switch filter).
   - `PosViewModelModifierTest`: Uji alur klik produk (produk tanpa modifier langsung masuk keranjang vs produk dengan modifier membuka dialog).
2. **UI & Build Verification**:
   - `./gradlew testDebugUnitTest` memastikan seluruh rangkaian test lulus tanpa regresi.
   - `./gradlew assembleDebug` memastikan aplikasi ter-compile dengan bersih tanpa error compose maupun Room database.
