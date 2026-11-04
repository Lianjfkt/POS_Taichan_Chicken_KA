# KA POS Enhancements & Compact Professional UI Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement all system improvements identified in the codebase analysis (atomic Room transactions, WorkManager background sync, instant network connectivity auto-sync, security hardening with encrypted preferences & disabled adb backup, lazy ViewModel scoping, deprecation cleanup) and polish the entire UI to be compact ("fit"), professional, and elegant — explicitly excluding kitchen tickets and table layout features.

**Architecture:** 
1. Core Data & Transactions: Wrap order creation, stock deduction, and void reversals in atomic `database.withTransaction {}`.
2. Sync & Connectivity: Integrate Android `ConnectivityManager` network callback for instant offline-to-online auto-sync, plus `WorkManager` for guaranteed background sync.
3. Security & App Manifest: Secure sensitive keys/PINs with EncryptedSharedPreferences fallback and set `allowBackup="false"`.
4. Code Clean-Up & Scoping: Remove legacy SharedPreferences `StockRepository` dualism, deprecation warnings, and scope ViewModels to `NavHost` routes.
5. UI Polish: Systematically refine spacing, card elevations, chips, badges, input fields, bottom navigation, and dialogues to deliver a compact, high-density, professional F&B POS experience.

**Tech Stack:** Kotlin 1.9, Jetpack Compose Material 3 (1.2.0), Room 2.6.1 + `room-ktx`, Android WorkManager 2.9.0, AndroidX Security Crypto 1.1.0-alpha06, OkHttp 4.12.0, Kotlinx Coroutines & Serialization.

## Global Constraints
- Target Android SDK: compileSdk 34, minSdk 26, targetSdk 34.
- All unit tests must pass via `JAVA_HOME=/opt/android-studio/jbr ./gradlew test`.
- Do NOT build kitchen display/ticket (KOT) or table layout features as requested by the user.
- Maintain existing database schema (Room v7) and ensure zero data loss.
- Follow Material 3 standards: replace deprecated `Divider` with `HorizontalDivider` and use `Icons.AutoMirrored`.

---

### Task 1: Add WorkManager & Security Dependencies and Update Manifest

**Files:**
- Modify: `app/build.gradle.kts:111-148`
- Modify: `app/src/main/AndroidManifest.xml:11-18`

**Interfaces:**
- Produces: `androidx.work:work-runtime-ktx:2.9.0` and `androidx.security:security-crypto:1.1.0-alpha06` dependencies.
- Manifest: `android:allowBackup="false"`.

- [ ] **Step 1: Update dependencies in `app/build.gradle.kts`**
Add WorkManager and Security Crypto dependencies:
```kotlin
    // WorkManager for guaranteed background sync
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Security Crypto for secure settings storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
```

- [ ] **Step 2: Update `AndroidManifest.xml`**
Set `android:allowBackup="false"` to prevent dumping databases and shared preferences via adb backup.

- [ ] **Step 3: Run gradle build check**
Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew checkKotlinGradlePluginConfigurationErrors`
Expected: SUCCESS

- [ ] **Step 4: Commit**
```bash
git add app/build.gradle.kts app/src/main/AndroidManifest.xml
git commit -m "build: add WorkManager and Security-Crypto dependencies and disable adb backup"
```

---

### Task 2: Atomic Database Transactions in PosRepository & Void Reversal

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/core/data/repository/PosRepository.kt:366-415, 684-728`
- Test: `app/src/test/java/com/kapos/mobile/core/data/repository/PosRepositoryTwoWaySyncTest.kt`

**Interfaces:**
- Consumes: `androidx.room.withTransaction`, `AppDatabase`, `InventoryRepository`
- Produces: Atomically executed `submitOrder()` and `voidOrder()`

- [ ] **Step 1: Write unit test validating atomic order submission**
Verify `submitOrder` completes both order entity insertion and stock deduction without throwing.

- [ ] **Step 2: Wrap `submitOrder` inside `database.withTransaction`**
In `PosRepository.kt`:
```kotlin
import androidx.room.withTransaction

suspend fun submitOrder(
    order: Order,
    items: List<CartItem>
): Order = withContext(Dispatchers.IO) {
    val orderEntity = OrderEntity(...)
    val itemEntities = items.map { ... }

    if (database != null) {
        database.withTransaction {
            database.orderDao().insertCompleteOrder(orderEntity, itemEntities)
            inventoryRepository?.deductStockForOrderItems(items)
        }
    } else {
        // Fallback for mocks / unit tests without database instance
        inventoryRepository?.deductStockForOrderItems(items)
    }

    scope.launch { syncPendingOrders() }
    order
}
```

- [ ] **Step 3: Wrap `voidOrder` stock and loyalty reversal in transaction**
In `PosRepository.kt`:
```kotlin
suspend fun voidOrder(orderId: String, reason: String = "Dibatalkan Kasir") = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    if (database != null) {
        database.withTransaction {
            val orderEntity = database.orderDao().getOrderById(orderId)
            val itemEntities = database.orderDao().getItemsForOrder(orderId)
            val domainItems = itemEntities.map { ... }
            inventoryRepository?.restoreStockForOrderItems(domainItems)

            if (orderEntity != null) {
                val phone = orderEntity.customerPhone
                if (!phone.isNullOrBlank()) {
                    val customer = customerRepository?.getCustomerByPhone(phone)
                    if (customer != null) {
                        val pointsEarned = (orderEntity.total / 10000.0).toInt().coerceAtLeast(0)
                        customerRepository.reverseCustomerTransaction(
                            customerId = customer.id,
                            totalSpent = orderEntity.total,
                            pointsRedeemed = 0,
                            pointsEarned = pointsEarned
                        )
                    }
                }
            }
            database.orderDao().voidOrderWithReason(orderId, reason, now)
        }
    }
    scope.launch { syncPendingOrders() }
}
```

- [ ] **Step 4: Run unit tests**
Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests "com.kapos.mobile.core.data.repository.*"`
Expected: ALL PASS

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/kapos/mobile/core/data/repository/PosRepository.kt
git commit -m "fix(pos): wrap submitOrder and voidOrder in Room atomic transactions"
```

---

### Task 3: Network Monitor & WorkManager Background Order Sync

**Files:**
- Create: `app/src/main/java/com/kapos/mobile/core/utils/NetworkMonitor.kt`
- Create: `app/src/main/java/com/kapos/mobile/core/sync/SyncOrdersWorker.kt`
- Modify: `app/src/main/java/com/kapos/mobile/KaPosApplication.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/repository/PosRepository.kt`

**Interfaces:**
- Produces: `NetworkMonitor(context).isOnlineFlow: StateFlow<Boolean>`
- Produces: `SyncOrdersWorker` runnable by Android WorkManager
- Consumes: `PosRepository.syncPendingOrders()`

- [ ] **Step 1: Create `NetworkMonitor.kt`**
Implement `NetworkMonitor` using Android `ConnectivityManager` with `registerDefaultNetworkCallback` to provide a real-time reactive `isOnlineFlow: StateFlow<Boolean>`. When connectivity transitions from false to true, it triggers an event.

- [ ] **Step 2: Create `SyncOrdersWorker.kt`**
Implement `CoroutineWorker`:
- Reads `AppDatabase.getInstance(applicationContext)` and `SupabaseClient`.
- Retrieves pending orders and pushes them to Supabase REST.
- Updates syncStatus to `SYNCED`.
- Returns `Result.success()`, `Result.retry()`, or `Result.failure()`.

- [ ] **Step 3: Register periodic WorkManager sync in `KaPosApplication.kt`**
Enqueue `PeriodicWorkRequestBuilder<SyncOrdersWorker>(15, TimeUnit.MINUTES)` with network constraint `NetworkType.CONNECTED`.
In addition, listen to `NetworkMonitor.isOnlineFlow`: when network is restored, trigger `posRepository.syncAll()` immediately!

- [ ] **Step 4: Run unit tests**
Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest`
Expected: ALL PASS

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/kapos/mobile/core/utils/NetworkMonitor.kt app/src/main/java/com/kapos/mobile/core/sync/SyncOrdersWorker.kt app/src/main/java/com/kapos/mobile/KaPosApplication.kt app/src/main/java/com/kapos/mobile/core/data/repository/PosRepository.kt
git commit -m "feat(sync): add NetworkMonitor for instant reconnect sync and WorkManager for background sync"
```

---

### Task 4: Secure Settings Storage (EncryptedSharedPreferences Fallback)

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/core/data/repository/SettingsRepository.kt:40-70`
- Test: `app/src/test/java/com/kapos/mobile/core/data/repository/SettingsRepositoryAuthTest.kt`

**Interfaces:**
- Produces: Secure initialization of `SharedPreferences` with `EncryptedSharedPreferences` on Android devices, falling back gracefully to standard `SharedPreferences` if Keystore is unavailable (e.g. during headless unit testing).

- [ ] **Step 1: Update `SettingsRepository` preference initialization**
In `SettingsRepository.kt`:
```kotlin
private val prefs: SharedPreferences? = context?.let { ctx ->
    try {
        val masterKey = androidx.security.crypto.MasterKey.Builder(ctx)
            .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
            .build()
        androidx.security.crypto.EncryptedSharedPreferences.create(
            ctx,
            "ka_pos_secure_settings",
            masterKey,
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Throwable) {
        // Fallback for tests or devices where Keystore initialization fails
        ctx.getSharedPreferences("ka_pos_settings", Context.MODE_PRIVATE)
    }
}
```
Add one-time migration to copy existing settings from `ka_pos_settings` to secure storage.

- [ ] **Step 2: Run settings unit tests**
Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests "com.kapos.mobile.core.data.repository.SettingsRepository*"`
Expected: ALL PASS

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/kapos/mobile/core/data/repository/SettingsRepository.kt
git commit -m "security(settings): implement EncryptedSharedPreferences with fallback and migration"
```

---

### Task 5: Clean Up Legacy StockRepository Dualism & Remove Deprecated Tabs

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/feature/stock/StockUiState.kt:13-25`
- Modify: `app/src/main/java/com/kapos/mobile/feature/stock/StockScreen.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/stock/AddStockBatchDialog.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/stock/StockViewModel.kt`
- Test: `app/src/test/java/com/kapos/mobile/feature/stock/StockViewModelTest.kt`

**Interfaces:**
- Removes: Legacy tabs `TAICHAN`, `CHICKEN`, `MATERIALS` from `StockTab`.
- Preserves: `StockTab.INVENTORY` ("Bahan Baku & Batch") and `StockTab.AUDIT` ("Audit Mutasi & Waste").
- Cleans up dead deprecated code in `StockScreen.kt` and `AddStockBatchDialog.kt`.

- [ ] **Step 1: Simplify `StockTab` enum in `StockUiState.kt`**
Keep only active tabs:
```kotlin
enum class StockTab(val displayName: String) {
    INVENTORY("Bahan Baku & Batch"),
    AUDIT("Audit Mutasi & Waste")
}
```

- [ ] **Step 2: Clean up legacy branches in `StockScreen.kt` & `AddStockBatchDialog.kt`**
Remove deprecated rendering branches for `TAICHAN` and `CHICKEN` tabs, removing compiler warnings.

- [ ] **Step 3: Run stock unit tests**
Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests "com.kapos.mobile.feature.stock.*"`
Expected: ALL PASS

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/kapos/mobile/feature/stock/StockUiState.kt app/src/main/java/com/kapos/mobile/feature/stock/StockScreen.kt app/src/main/java/com/kapos/mobile/feature/stock/AddStockBatchDialog.kt app/src/main/java/com/kapos/mobile/feature/stock/StockViewModel.kt
git commit -m "refactor(stock): eliminate legacy tabs and clean up deprecated stock UI branches"
```

---

### Task 6: Scoped ViewModel Optimization in `KaPosApp.kt` & Deprecations Fix

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/ui/KaPosApp.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/components/AppBottomNav.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/cart/CartBottomSheet.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/checkout/ConfirmSaleScreen.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/pos/PosScreen.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/shift/BlindCountDialog.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/transactions/TransactionsScreen.kt`

**Interfaces:**
- Scoping: Move screen-specific ViewModels into their respective `composable(...)` destination lambdas in `KaPosApp.kt`.
- Deprecations: Replace `Divider` with `HorizontalDivider`, update icons to `Icons.AutoMirrored.Filled.ArrowBack` / `ArrowForward`, and fix `errorMessage != null` warning in `OwnerAuthPinDialog`.

- [ ] **Step 1: Replace deprecated `Divider` with `HorizontalDivider`**
Replace `Divider(...)` in:
- `CartBottomSheet.kt`
- `ConfirmSaleScreen.kt`
- `PosScreen.kt`
- `BlindCountDialog.kt`
- `StockScreen.kt`
Replace `Icons.Default.ArrowBack` and `Icons.Default.ArrowForward` with `Icons.AutoMirrored.Filled.ArrowBack` and `Icons.AutoMirrored.Filled.ArrowForward`.

- [ ] **Step 2: Fix `OwnerAuthPinDialog` in `TransactionsScreen.kt`**
Clean up the color expression so there is no false/redundant condition warning.

- [ ] **Step 3: Optimize ViewModel instantiation in `KaPosApp.kt`**
Keep only shared ViewModels at root (`loginViewModel`, `posViewModel`, `checkoutViewModel`). Move `menuManagementViewModel`, `transactionsViewModel`, `shiftViewModel`, `stockViewModel`, `reportViewModel`, `analyticsViewModel`, `customerViewModel`, and `settingsViewModel` to be lazily instantiated inside their corresponding `composable("route") { ... }` blocks.

- [ ] **Step 4: Verify test suite passes**
Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest`
Expected: ALL PASS with fewer compiler warnings.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/kapos/mobile/ui/KaPosApp.kt app/src/main/java/com/kapos/mobile/feature/cart/CartBottomSheet.kt app/src/main/java/com/kapos/mobile/feature/checkout/ConfirmSaleScreen.kt app/src/main/java/com/kapos/mobile/feature/pos/PosScreen.kt app/src/main/java/com/kapos/mobile/feature/shift/BlindCountDialog.kt app/src/main/java/com/kapos/mobile/feature/transactions/TransactionsScreen.kt
git commit -m "refactor(ui): scope ViewModels to destinations and replace deprecated Material3 Divider and icons"
```

---

### Task 7: Compact, Professional, & Elegant UI Polish Across Components

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/core/theme/Spacing.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/theme/Shape.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/components/AppTopBar.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/components/AppBottomNav.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/components/PrimaryButton.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/components/StatCard.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/components/SearchField.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/components/CategoryChips.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/pos/PosScreen.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/checkout/ConfirmSaleScreen.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/cart/CartBottomSheet.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/transactions/TransactionsScreen.kt`

**Interfaces:**
- Produces: High-density, professional POS design system with compact padding, refined cards (12–16dp radii with subtle 1dp border `#E2E8F0`), sleek navigation bars, crisp typography, and thumb-friendly 44dp buttons without bloated whitespace.

- [ ] **Step 1: Refine Core Component Styling**
- `AppTopBar.kt`: Compact height (52dp instead of 64dp), slim offline/online badge, crisp title typography.
- `AppBottomNav.kt`: Compact bar height (64dp), elegant pill indicator for active tab, subtle elevation, refined icon/text ratio.
- `CategoryChips.kt`: 32dp height chips, subtle background tints, crisp 8dp rounded corner, clean horizontal scrolling without clipping.
- `PrimaryButton.kt`: Standardized 44dp height, crisp typography, subtle ripple, no awkward oversized padding.
- `StatCard.kt`: Compact padding (12dp), clear hierarchy (title, value in tabular figures, trend indicator).
- `SearchField.kt`: 42dp height, subtle light-gray background, clear search icon and clear button.

- [ ] **Step 2: Refine POS Screen & Product Grid**
- Product cards: 2-column grid with balanced aspect ratio, compact 10-12dp padding, crisp emoji container, clear price formatting in bold primary teal, quick `+` button neatly aligned.
- Floating Cart Summary: Compact floating bar with subtle shadow, clear item count badge, total price, and "Bayar" action.

- [ ] **Step 3: Refine Cart Bottom Sheet & Checkout Screen**
- Cart items: compact stepper (`-` / `+`), clear item name and modifiers in muted text, subtotal right-aligned.
- Checkout screen: Compact payment method cards (Tunai, QRIS, Transfer) with clear active border, neat quick-cash buttons (Uang Pas, 50k, 100k, 200k) with uniform height, kembalian calculation prominently styled.

- [ ] **Step 4: Refine Transactions & Receipt Dialogs**
- Transaction item cards: compact rows, clear status badge (Lunas, Void, Pending Sync), formatted date/time and cashier name.
- Receipt dialog: compact paper preview, clean monospaced receipt layout, clear action buttons.

- [ ] **Step 5: Run full test suite & release build check**
Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew test assembleDebug`
Expected: ALL TESTS PASS, BUILD SUCCESSFUL

- [ ] **Step 6: Commit**
```bash
git add app/src/main/java/com/kapos/mobile/core/ app/src/main/java/com/kapos/mobile/feature/
git commit -m "style(ui): polish UI components for a compact, professional, and elegant POS experience"
```

---

### Task 8: Verification & Final Regression Testing

**Files:**
- Test all repository, viewmodel, and UI state tests across the app.

- [ ] **Step 1: Execute all unit tests**
Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew test`
Expected: 100% tests pass.

- [ ] **Step 2: Inspect git diff for clean architecture and no unexpected changes**
Verify that kitchen order tickets and table management features were NOT introduced, while all other optimizations are present and tested.

- [ ] **Step 3: Final commit & tag if appropriate**
```bash
git status
```

