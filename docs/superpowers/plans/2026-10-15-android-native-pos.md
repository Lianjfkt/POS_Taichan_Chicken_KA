# Android Native Mobile POS KA Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Membangun aplikasi mobile native Android kasir POS untuk gerai Taichan & Chicken KA berbasis Kotlin, Jetpack Compose, Material 3, mengacu pada panduan UI/UX `compose-fintech-pos-ui`, dengan arsitektur Offline-First (Room Database + Supabase REST sync).

**Architecture:** Clean Architecture Single-Module (`android/app`) dengan pembagian layer per fitur (`feature/*`) dan core reusables (`core/*`). Room Database berfungsi sebagai Single Source of Truth (SSOT) yang disinkronkan secara background dengan backend Supabase REST.

**Tech Stack:** Kotlin 1.9+, Jetpack Compose, Material 3, AndroidX Lifecycle & ViewModel, Kotlin Coroutines & StateFlow, Navigation Compose, Room Database 2.6+, OkHttp/Kotlinx Serialization.

## Global Constraints

- Root project directory: `/android`
- Package Name: `com.kapos.mobile`
- Mata uang: Rupiah (`Rp xx.xxx`) melalui helper tunggal `CurrencyFormatter`
- Warna & Token UI: Wajib menggunakan token `core/theme/Color.kt`, `Shape.kt`, `Spacing.kt` sesuai panduan `compose-fintech-pos-ui` (Aksen Teal `#0B5C73`, Background Gradient `#DCE9EE` ke `#C7DAE1`, Kartu Rounded 16-24dp `#FFFFFF`)
- UI Separation: Setiap Composable publik memiliki pasangan Stateless Screen dan `@Preview`
- Tidak ada hardcoded string untuk teks label utama, sediakan `strings.xml`

---

### Task 1: Android Project Scaffolding & Gradle Setup

**Files:**
- Create: `android/settings.gradle.kts`
- Create: `android/build.gradle.kts`
- Create: `android/gradle.properties`
- Create: `android/gradle/wrapper/gradle-wrapper.properties`
- Create: `android/app/build.gradle.kts`
- Create: `android/app/src/main/AndroidManifest.xml`
- Create: `android/app/src/main/res/values/strings.xml`
- Create: `android/app/src/main/res/values/colors.xml`
- Create: `android/app/src/main/java/com/kapos/mobile/KaPosApplication.kt`

**Interfaces:**
- Produces: Base Android application and Gradle configuration targeting Android SDK 34/35 with Compose, Room, and Serialization plugins enabled.

- [ ] **Step 1: Create `android/settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "KaPosMobile"
include(":app")
```

- [ ] **Step 2: Create root `android/build.gradle.kts` and `android/gradle.properties`**

`android/build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false
}
```

`android/gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

`android/gradle/wrapper/gradle-wrapper.properties`:
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- [ ] **Step 3: Create `android/app/build.gradle.kts`**

Configure dependencies: Compose BOM, Material 3, Navigation Compose, Room (with KSP), Lifecycle ViewModel, OkHttp, Kotlinx Serialization.

- [ ] **Step 4: Create `android/app/src/main/AndroidManifest.xml` & `strings.xml`**

Define application name, permissions (`INTERNET`, `ACCESS_NETWORK_STATE`), and default `KaPosApplication` class.

- [ ] **Step 5: Create `android/app/src/main/java/com/kapos/mobile/KaPosApplication.kt`**

Initialize dependency container / database singleton.

- [ ] **Step 6: Commit Task 1**
```bash
git add android/
git commit -m "feat(android): initialize gradle project structure and dependencies"
```

---

### Task 2: Design Tokens & Reusable UI Components (`compose-fintech-pos-ui`)

**Files:**
- Create: `android/app/src/main/java/com/kapos/mobile/core/theme/Color.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/core/theme/Shape.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/core/theme/Spacing.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/core/theme/Type.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/core/theme/Theme.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/core/utils/CurrencyFormatter.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/core/components/AppTopBar.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/core/components/SearchField.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/core/components/CategoryChips.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/core/components/StatusBadge.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/core/components/StatCard.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/core/components/AppBottomNav.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/core/components/PrimaryButton.kt`

**Interfaces:**
- Produces: Theme tokens (`Primary`, `ScreenBackground`, `AppShapes`, `AppTypography`), Currency utility `formatRupiah(amount: Double): String`, and modular reusable UI components.

- [ ] **Step 1: Implement Theme Tokens (`Color.kt`, `Shape.kt`, `Spacing.kt`, `Type.kt`, `Theme.kt`)**

Exact tokens from `compose-fintech-pos-ui`:
`Primary = Color(0xFF0B5C73)`, `PrimaryDark = Color(0xFF08485B)`, `PrimarySoft = Color(0xFFE3F1F5)`, `BgTop = Color(0xFFDCE9EE)`, `BgBottom = Color(0xFFC7DAE1)`, `SurfaceWhite = Color(0xFFFFFFFF)`, `SurfaceTint = Color(0xFFF3F8FA)`, `OutlineSoft = Color(0xFFDCE5E9)`, `TextPrimary = Color(0xFF12303B)`, `TextSecondary = Color(0xFF6B8189)`, `Success = Color(0xFF2E9E6B)`, `Danger = Color(0xFFE5484D)`.

- [ ] **Step 2: Implement `CurrencyFormatter.kt`**

Helper function:
```kotlin
fun formatRupiah(amount: Double): String {
    val format = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
    format.maximumFractionDigits = 0
    return format.format(amount).replace("Rp", "Rp ")
}
```

- [ ] **Step 3: Implement Reusable Core Components with `@Preview`**

Create `AppTopBar`, `SearchField`, `CategoryChips`, `StatusBadge`, `StatCard`, `PrimaryButton`, and floating pill `AppBottomNav`.

- [ ] **Step 4: Commit Task 2**
```bash
git add android/app/src/main/java/com/kapos/mobile/core/
git commit -m "feat(android): implement design tokens and core compose components"
```

---

### Task 3: Domain Models & Local Room Database (Offline-First)

**Files:**
- Create: `android/app/src/main/java/com/kapos/mobile/domain/model/Models.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/core/data/local/entity/Entities.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/core/data/local/dao/CategoryDao.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/core/data/local/dao/ProductDao.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/core/data/local/dao/OrderDao.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/core/data/local/dao/ShiftDao.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/core/data/local/AppDatabase.kt`

**Interfaces:**
- Consumes: Models definitions (`Product`, `Category`, `Order`, `CartItem`, `Shift`).
- Produces: `AppDatabase` singleton, DAOs with Flow queries (`getAllProducts()`, `getOrders()`, `getPendingOrders()`, `insertOrderWithItems()`).

- [ ] **Step 1: Define Domain Models & Enums in `domain/model/Models.kt`**

`Product`, `Category`, `CartItem`, `Order`, `OrderItem`, `PaymentMethod`, `OrderStatus`, `OrderType`, `Shift`.

- [ ] **Step 2: Define Room Entities in `core/data/local/entity/Entities.kt`**

`CategoryEntity`, `ProductEntity`, `OrderEntity` (with `syncStatus = "SYNCED" | "PENDING_SYNC"`), `OrderItemEntity`, `ShiftEntity`.

- [ ] **Step 3: Implement Room DAOs**

- `CategoryDao`: `observeAll()`, `upsertAll(categories)`
- `ProductDao`: `observeAll()`, `observeByCategory(catId)`, `upsertAll(products)`
- `OrderDao`: `observeOrders()`, `getPendingSyncOrders()`, `insertOrder(order)`, `insertOrderItems(items)`, `updateSyncStatus(id, status)`
- `ShiftDao`: `observeActiveShift()`, `insertShift(shift)`, `updateShift(shift)`

- [ ] **Step 4: Create `AppDatabase.kt` and Database TypeConverters**

Define Room Database version 1 with exportSchema = false.

- [ ] **Step 5: Commit Task 3**
```bash
git add android/app/src/main/java/com/kapos/mobile/domain/ android/app/src/main/java/com/kapos/mobile/core/data/local/
git commit -m "feat(android): implement domain models, room entities, and DAOs"
```

---

### Task 4: Remote Supabase REST Client, Mock Data & Repository Layer

**Files:**
- Create: `android/app/src/main/java/com/kapos/mobile/core/data/remote/dto/SupabaseDto.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/core/data/remote/SupabaseClient.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/core/data/mock/SampleData.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/core/data/repository/SettingsRepository.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/core/data/repository/PosRepository.kt`

**Interfaces:**
- Consumes: `AppDatabase`, `SupabaseClient`.
- Produces: `PosRepository` providing `productsFlow`, `categoriesFlow`, `ordersFlow`, `checkout(order, items)`, `syncWithSupabase()`.

- [ ] **Step 1: Define Supabase DTOs & Serialization**

Mapping for categories, products, orders, order items corresponding to `supabase_schema.sql`.

- [ ] **Step 2: Create `SupabaseClient.kt`**

Configurable OkHttp client supporting dynamic Supabase URL & Anon Key headers with JSON parser.

- [ ] **Step 3: Create `SampleData.kt`**

Initial seed data containing authentic Taichan Chicken KA menu (Sate Taichan Daging, Kulit, Campur, Paha, Ceker, Nasi Putih, Sambal Taichan Ekstra, Es Teh Manis) for instant offline ready-to-use experience.

- [ ] **Step 4: Implement `SettingsRepository.kt`**

Persistent settings storage (Supabase URL, Anon Key, Cashier Name, Store Name, Address, Offline Mode flag).

- [ ] **Step 5: Implement `PosRepository.kt`**

Methods:
- `getCategories()`: Emits from Room; refreshes from Supabase in background.
- `getProducts()`: Emits from Room; seeds SampleData if local DB is empty.
- `submitOrder(order: Order, items: List<CartItem>)`: Saves to Room immediately (`PENDING_SYNC`), then launches background sync worker.
- `syncPendingOrders()`: Iterates through pending orders, posts to Supabase, updates status to `SYNCED`.

- [ ] **Step 6: Commit Task 4**
```bash
git add android/app/src/main/java/com/kapos/mobile/core/data/
git commit -m "feat(android): implement supabase client and offline-first pos repository"
```

---

### Task 5: POS Catalog Screen & Cart Bottom Sheet (`feature/pos`, `feature/cart`)

**Files:**
- Create: `android/app/src/main/java/com/kapos/mobile/feature/pos/PosUiState.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/feature/pos/PosViewModel.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/feature/pos/ProductCard.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/feature/cart/CartBottomSheet.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/feature/pos/PosScreen.kt`

**Interfaces:**
- Consumes: `PosRepository`, `Theme`, `CurrencyFormatter`.
- Produces: `PosScreen` (2-column menu grid, category filter, search, floating cart bar) and `CartBottomSheet`.

- [ ] **Step 1: Implement `PosUiState.kt` & `PosViewModel.kt`**

Manage UI state: `searchQuery`, `selectedCategoryId`, `products`, `categories`, `cartItems`, `isCartSheetOpen`.
Methods: `addToCart(product, spiceLevel)`, `updateCartQty(productId, delta)`, `clearCart()`.

- [ ] **Step 2: Implement `ProductCard.kt`**

Rounded 16dp card with pastel emoji background, title, price, and teal rounded plus button.

- [ ] **Step 3: Implement `CartBottomSheet.kt`**

Bottom sheet with header "X Items", "Clear Items", list of items with circular -/+ stepper, subtotal, and full-width "Lanjut ke Pembayaran" button.

- [ ] **Step 4: Implement `PosScreen.kt` and `@Preview`**

Compose `AppTopBar`, `SearchField`, `CategoryChips`, `LazyVerticalGrid`, and `FloatingCartSummaryBar`.

- [ ] **Step 5: Commit Task 5**
```bash
git add android/app/src/main/java/com/kapos/mobile/feature/pos/ android/app/src/main/java/com/kapos/mobile/feature/cart/
git commit -m "feat(android): implement pos catalog screen and cart bottom sheet"
```

---

### Task 6: Checkout / Confirm Sale Screen (`feature/checkout`)

**Files:**
- Create: `android/app/src/main/java/com/kapos/mobile/feature/checkout/CheckoutUiState.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/feature/checkout/CheckoutViewModel.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/feature/checkout/ConfirmSaleScreen.kt`

**Interfaces:**
- Consumes: `CartItem` list, `PosRepository`.
- Produces: `ConfirmSaleScreen` with order details, Dine-in/Take-away switch, Cash change calculator, QRIS & Transfer selector, submit order callback.

- [ ] **Step 1: Implement `CheckoutUiState.kt` & `CheckoutViewModel.kt`**

State: `orderNo`, `orderType` (Dine-in / Take-away), `tableNo`, `customerName`, `paymentMethod` (CASH / QRIS / TRANSFER), `cashGiven`, `changeDue`, `notes`.
Method: `submitOrder()` invoking repository checkout.

- [ ] **Step 2: Implement `ConfirmSaleScreen.kt` with `@Preview`**

- Section 1: Invoice number & Date picker display.
- Section 2: Order Type chips (Dine In + Meja / Take Away).
- Section 3: Billing breakdown (items summary, total).
- Section 4: Payment Mode card with quick cash buttons (Uang Pas, 50k, 100k, 200k) and live change calculator.
- Action Buttons: `Batal` (Outline) and `Selesaikan Transaksi` (Filled Teal).

- [ ] **Step 3: Commit Task 6**
```bash
git add android/app/src/main/java/com/kapos/mobile/feature/checkout/
git commit -m "feat(android): implement confirm sale checkout screen"
```

---

### Task 7: Transactions History & Order Detail Dialog (`feature/transactions`)

**Files:**
- Create: `android/app/src/main/java/com/kapos/mobile/feature/transactions/TransactionsUiState.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/feature/transactions/TransactionsViewModel.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/feature/transactions/TransactionsScreen.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/feature/transactions/OrderDetailDialog.kt`

**Interfaces:**
- Consumes: `PosRepository.ordersFlow`.
- Produces: `TransactionsScreen` and `OrderDetailDialog`.

- [ ] **Step 1: Implement `TransactionsUiState.kt` & `TransactionsViewModel.kt`**

Search filter, status filter (`ALL`, `PAID`, `UNPAID`), date filter.

- [ ] **Step 2: Implement `TransactionsScreen.kt` with `@Preview`**

- Search & Filter chips (`Semua`, `Lunas`, `Pending`).
- `LazyColumn` of transaction cards: Invoice No, Date/Time, Order Type, Bold Total, and Status Badge (`Success` for Paid, `Danger` for Unpaid, cloud icon for pending sync).

- [ ] **Step 3: Implement `OrderDetailDialog.kt`**

Receipt modal preview showing store header, itemized list, subtotal, payment method, cashier name, and Print/Share buttons.

- [ ] **Step 4: Commit Task 7**
```bash
git add android/app/src/main/java/com/kapos/mobile/feature/transactions/
git commit -m "feat(android): implement transactions history and receipt dialog"
```

---

### Task 8: Sales Report & Shift Summary Screen (`feature/report`)

**Files:**
- Create: `android/app/src/main/java/com/kapos/mobile/feature/report/ReportUiState.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/feature/report/ReportViewModel.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/feature/report/ReportScreen.kt`

**Interfaces:**
- Consumes: `PosRepository.ordersFlow`, `ShiftDao`.
- Produces: `ReportScreen` with 2x2 stat cards and payment breakdown.

- [ ] **Step 1: Implement `ReportUiState.kt` & `ReportViewModel.kt`**

Calculate metrics: Total Sales Omzet, Total Transactions Count, Cash Total, QRIS/Transfer Total, Average Order Value.

- [ ] **Step 2: Implement `ReportScreen.kt` with `@Preview`**

- Period selector (Hari Ini, 7 Hari Terakhir, Bulan Ini).
- 2x2 Grid of `StatCard` (Omzet, Transaksi, Tunai vs Non-Tunai, Basket Size).
- Cashier shift drawer reconciliation card.

- [ ] **Step 3: Commit Task 8**
```bash
git add android/app/src/main/java/com/kapos/mobile/feature/report/
git commit -m "feat(android): implement sales report and stat summary screen"
```

---

### Task 9: App Settings & Supabase Connection (`feature/settings`)

**Files:**
- Create: `android/app/src/main/java/com/kapos/mobile/feature/settings/SettingsUiState.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/feature/settings/SettingsViewModel.kt`
- Create: `android/app/src/main/java/com/kapos/mobile/feature/settings/SettingsScreen.kt`

**Interfaces:**
- Consumes: `SettingsRepository`, `PosRepository`.
- Produces: `SettingsScreen`.

- [ ] **Step 1: Implement `SettingsUiState.kt` & `SettingsViewModel.kt`**

Form inputs: Supabase URL, Anon Key, Store Name, Address, Cashier Name. Test connection action & manual sync trigger.

- [ ] **Step 2: Implement `SettingsScreen.kt` with `@Preview`**

Grouped rounded white cards with `HorizontalDivider`, text input fields, status chips, and sync now button.

- [ ] **Step 3: Commit Task 9**
```bash
git add android/app/src/main/java/com/kapos/mobile/feature/settings/
git commit -m "feat(android): implement app settings and supabase configuration screen"
```

---

### Task 10: App Navigation Integration (`MainActivity.kt` & `KaPosApp.kt`) & Verification

**Files:**
- Create: `android/app/src/main/java/com/kapos/mobile/ui/KaPosApp.kt`
- Modify: `android/app/src/main/java/com/kapos/mobile/MainActivity.kt`
- Create: `android/README.md` (Quickstart and Android Studio import instructions)

**Interfaces:**
- Consumes: All feature screens and `AppBottomNav`.
- Produces: Complete navigable Android application with smooth tab switching and checkout flow.

- [ ] **Step 1: Implement `KaPosApp.kt`**

Navigation host with 4 main destinations:
1. `pos` (`PosScreen`)
2. `transactions` (`TransactionsScreen`)
3. `report` (`ReportScreen`)
4. `settings` (`SettingsScreen`)
And sub-route:
5. `checkout` (`ConfirmSaleScreen`)

Overlay floating `AppBottomNav` with safe insets (`contentPadding`).

- [ ] **Step 2: Connect `MainActivity.kt`**

Set Compose content wrapped in `KaPosTheme` with gradient background `ScreenBackground`.

- [ ] **Step 3: Write `android/README.md`**

Documentation explaining how to open the project in Android Studio, configure Supabase credentials, and test offline-first functionality.

- [ ] **Step 4: Verification and Final Commit**
Verify all files, package imports, and syntax consistency across the codebase.
```bash
git add android/
git commit -m "feat(android): complete navigation integration and document android app setup"
```
