# Comprehensive KA POS Fixes & Enhancements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Memperbaiki seluruh critical bug, melengkapi logika bisnis transaksi void (restorasi stok BOM & poin member), mengaktifkan modul laporan yang terisolasi, menangani izin Bluetooth Android 12+, dan melengkapi sinkronisasi cloud Supabase pada aplikasi Android KA POS.

**Architecture:** Memperluas `InventoryRepository` dengan fungsi restorasi stok saat void order, memperluas `CustomerRepository` dengan pembalikan transaksi member, menyempurnakan `EscPosBuilder` dengan tanda `*** BATAL / VOID ***` dan pembersihan karakter non-ASCII, menambahkan permission launcher Bluetooth di Compose/MainActivity, memperbarui navigasi `KaPosApp` & `AppBottomNav` untuk menghubungkan `ReportScreen` dan tab `Shift` untuk Owner, serta melengkapi payload `SupabaseOrderPayload` (`created_at`, `paid_at`, `voidReason`).

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Room SQLite, OkHttp + Kotlinx Serialization, Bluetooth RFCOMM SPP, JUnit.

## Global Constraints
- Target platform: Android SDK 26 (Min) / 34 (Target), Kotlin 1.9+, Jetpack Compose Material 3.
- Database migrations use `fallbackToDestructiveMigration()`.
- Currency formatting must follow Indonesian Rupiah (`Rp #,###`).
- Pin verification must check against `SettingsRepository.settingsFlow.value.ownerPin`.
- JAVA_HOME must be set to `/opt/android-studio/jbr` when running Gradle commands.

---

### Task 1: Bluetooth Runtime Permissions & Printer Error Handling

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/core/printer/BluetoothPrinterService.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/checkout/CheckoutViewModel.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/checkout/CheckoutUiState.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/transactions/TransactionsViewModel.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/transactions/TransactionsUiState.kt`
- Test: `app/src/test/java/com/kapos/mobile/core/printer/EscPosBuilderTest.kt`

**Interfaces:**
- Produces: Runtime permission check helper, toast/snackbar feedback when printer fails during checkout or transaction reprint.

- [ ] **Step 1: Write test for EscPosBuilder non-ASCII character handling & VOID header**

```kotlin
// In app/src/test/java/com/kapos/mobile/core/printer/EscPosBuilderTest.kt
@Test
fun testVoidReceiptContainsVoidHeader() {
    val builder = EscPosBuilder(58)
    val bytes = builder.buildReceipt(
        storeName = "Taichan & Chicken KA",
        address = "Bandar Lampung",
        phone = "081234567890",
        receiptFooter = "Terima kasih!",
        orderNo = "KA-20261019-001",
        dateStr = "19 Okt 2026, 12:00",
        orderType = "Dine-In",
        tableNo = "01",
        cashierName = "Kasir Utama",
        customerName = null,
        items = emptyList(),
        subtotal = 50000.0,
        discount = 0.0,
        total = 50000.0,
        paymentMethod = "Tunai",
        cashGiven = 50000.0,
        change = 0.0,
        isVoid = true
    )
    val receiptText = String(bytes, Charsets.ISO_8859_1)
    assertTrue(receiptText.contains("BATAL / VOID"))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests com.kapos.mobile.core.printer.EscPosBuilderTest`
Expected: FAIL (isVoid parameter unresolvable)

- [ ] **Step 3: Update EscPosBuilder to support isVoid and replace non-ASCII bullets**

In `app/src/main/java/com/kapos/mobile/core/printer/EscPosBuilder.kt`:
- Add `isVoid: Boolean = false` parameter to `buildReceipt`.
- When `isVoid == true`, print bold centered `*** BATAL / VOID ***` below store header and above totals.
- Replace bullet character `•` with standard dash `- ` in item modifier details so ISO-8859-1 printers print cleanly without garbled characters.

- [ ] **Step 4: Add Bluetooth permission launcher in SettingsScreen**

In `app/src/main/java/com/kapos/mobile/feature/settings/SettingsScreen.kt`:
- Add `rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions())` for `BLUETOOTH_CONNECT` and `BLUETOOTH_SCAN` on Android 12+ (Build.VERSION.SDK_INT >= 31).
- Show an alert card or button "Beri Izin Bluetooth" when permissions are not granted.

- [ ] **Step 5: Surface printer errors in CheckoutViewModel and TransactionsViewModel**

- In `CheckoutViewModel`: Add `printerErrorMessage: String? = null` to `CheckoutUiState` and capture result of `bluetoothPrinterService.printBytes()`.
- In `TransactionsViewModel`: Add `printerFeedbackMessage: String? = null` to `TransactionsUiState` and capture result of `bluetoothPrinterService.printBytes()`.

- [ ] **Step 6: Run tests and verify they pass**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests com.kapos.mobile.core.printer.EscPosBuilderTest`
Expected: PASS

---

### Task 2: Void Order Reversal (BOM Stock & Member Points)

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/core/data/repository/InventoryRepository.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/repository/CustomerRepository.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/repository/PosRepository.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/transactions/TransactionsViewModel.kt`
- Create: `app/src/test/java/com/kapos/mobile/core/data/repository/VoidOrderReversalTest.kt`

**Interfaces:**
- Produces: `InventoryRepository.restoreStockForOrderItems(items: List<OrderItem>)`, `CustomerRepository.reverseCustomerTransaction(customerId: String, totalSpent: Double, pointsRedeemed: Int, pointsEarned: Int)`

- [ ] **Step 1: Write unit tests for stock and customer point reversal**

```kotlin
// app/src/test/java/com/kapos/mobile/core/data/repository/VoidOrderReversalTest.kt
package com.kapos.mobile.core.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class VoidOrderReversalTest {
    @Test
    fun testReversalDeltas() {
        // Points delta when voiding an order that awarded 5 points and redeemed 20 points
        val pointsRedeemed = 20
        val pointsEarned = 5
        val netCustomerPointsToAdjust = pointsRedeemed - pointsEarned // Should restore +15 points
        assertEquals(15, netCustomerPointsToAdjust)
    }
}
```

- [ ] **Step 2: Run test to verify initial logic**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests com.kapos.mobile.core.data.repository.VoidOrderReversalTest`
Expected: PASS

- [ ] **Step 3: Implement restoreStockForOrderItems in InventoryRepository**

In `app/src/main/java/com/kapos/mobile/core/data/repository/InventoryRepository.kt`:
```kotlin
suspend fun restoreStockForOrderItems(orderItems: List<OrderItem>) = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val allRecipes = recipeDao.getAll()
    val movementsToInsert = mutableListOf<StockMovementEntity>()

    for (item in orderItems) {
        val recipesForProduct = allRecipes.filter { it.productId == item.productId }
        for (recipe in recipesForProduct) {
            val totalRestoration = recipe.quantityRequired * item.quantity
            rawMaterialDao.adjustStock(recipe.materialId, totalRestoration, now)
            movementsToInsert.add(
                StockMovementEntity(
                    id = UUID.randomUUID().toString(),
                    materialId = recipe.materialId,
                    type = StockMovementType.ADJUSTMENT.name,
                    quantity = totalRestoration,
                    costTotal = 0.0,
                    notes = "Void Order Restoration: ${item.quantity}x ${item.productName}",
                    timestamp = now
                )
            )
        }
    }
    if (movementsToInsert.isNotEmpty()) {
        stockMovementDao.insertAll(movementsToInsert)
    }
}
```

- [ ] **Step 4: Implement reverseCustomerTransaction in CustomerRepository**

In `app/src/main/java/com/kapos/mobile/core/data/repository/CustomerRepository.kt`:
```kotlin
suspend fun reverseCustomerTransaction(
    customerId: String,
    totalSpent: Double,
    pointsRedeemed: Int,
    pointsEarned: Int
) = withContext(Dispatchers.IO) {
    val pointsDelta = pointsRedeemed - pointsEarned // restore redeemed, deduct earned
    customerDao.updateStats(
        id = customerId,
        pointsDelta = pointsDelta,
        spendDelta = -totalSpent,
        orderDelta = -1
    )
}
```

- [ ] **Step 5: Integrate stock and customer reversal into PosRepository.voidOrder**

In `PosRepository.kt`:
- Accept optional `CustomerRepository` in constructor.
- When `voidOrder(orderId, reason)` is called:
  1. Retrieve order items via `orderDao.getItemsForOrder(orderId)`
  2. Call `inventoryRepository?.restoreStockForOrderItems(domainItems)`
  3. If order has `customerPhone`, find customer and reverse points/spending.
  4. Update order status to `VOID`.
  5. Sync pending order.

- [ ] **Step 6: Update TransactionsViewModel.printOrder to pass isVoid flag**

In `TransactionsViewModel.kt`:
Pass `isVoid = (order.paymentStatus == PaymentStatus.VOID)` to `EscPosBuilder.buildReceipt()`.

- [ ] **Step 7: Run test suite**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest`
Expected: PASS

---

### Task 3: Complete Supabase Cloud Sync

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/core/data/remote/dto/SupabaseDto.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/repository/PosRepository.kt`
- Test: `app/src/test/java/com/kapos/mobile/core/data/repository/SupabaseSyncPayloadTest.kt`

**Interfaces:**
- Produces: `SupabaseOrderPayload` with `created_at`, `paid_at`, `void_reason`.

- [ ] **Step 1: Write test verifying payload serialization with dates and void reason**

```kotlin
// app/src/test/java/com/kapos/mobile/core/data/repository/SupabaseSyncPayloadTest.kt
package com.kapos.mobile.core.data.repository

import com.kapos.mobile.core.data.remote.dto.SupabaseOrderPayload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseSyncPayloadTest {
    @Test
    fun testOrderPayloadSerializationIncludesDatesAndVoidReason() {
        val payload = SupabaseOrderPayload(
            id = "test-1",
            orderNo = "KA-20261019-120000-A1",
            orderType = "dine-in",
            subtotal = 50000.0,
            total = 50000.0,
            paymentMethod = "cash",
            status = "void",
            voidReason = "Salah Input Menu",
            createdAt = "2026-10-19T05:00:00Z",
            paidAt = "2026-10-19T05:01:00Z"
        )
        val json = Json.encodeToString(payload)
        assertTrue(json.contains("\"created_at\""))
        assertTrue(json.contains("\"paid_at\""))
        assertTrue(json.contains("\"void_reason\""))
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests com.kapos.mobile.core.data.repository.SupabaseSyncPayloadTest`
Expected: FAIL (voidReason unresolvable in SupabaseOrderPayload)

- [ ] **Step 3: Update SupabaseOrderPayload and syncPendingOrders**

In `app/src/main/java/com/kapos/mobile/core/data/remote/dto/SupabaseDto.kt`:
- Add `@SerialName("void_reason") val voidReason: String? = null` to `SupabaseOrderPayload`.

In `app/src/main/java/com/kapos/mobile/core/data/repository/PosRepository.kt`:
- Format `order.createdAt` and `order.paidAt` into ISO-8601 strings (e.g. `Instant.ofEpochMilli(order.createdAt).toString()`).
- Pass `createdAt`, `paidAt`, and `voidReason = order.voidReason` to `SupabaseOrderPayload`.

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests com.kapos.mobile.core.data.repository.SupabaseSyncPayloadTest`
Expected: PASS

---

### Task 4: Connect Orphaned Report Module & Owner Shift Tab

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/core/components/AppBottomNav.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/kapos/mobile/ui/KaPosApp.kt`
- Test: `app/src/test/java/com/kapos/mobile/core/components/AppNavDestinationsTest.kt`

**Interfaces:**
- Produces: Navigation entry to `report` from `SettingsScreen` (and Owner top bar), inclusion of `Shift` in Owner bottom nav.

- [ ] **Step 1: Write test for Owner bottom nav destinations**

```kotlin
// app/src/test/java/com/kapos/mobile/core/components/AppNavDestinationsTest.kt
package com.kapos.mobile.core.components

import com.kapos.mobile.domain.model.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavDestinationsTest {
    @Test
    fun testOwnerCanAccessShift() {
        val ownerDests = getBottomNavDestinations(UserRole.OWNER)
        assertTrue(ownerDests.any { it.route == "shift" })
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests com.kapos.mobile.core.components.AppNavDestinationsTest`
Expected: FAIL (Shift not in owner destinations)

- [ ] **Step 3: Update AppBottomNav & KaPosApp navigation**

In `app/src/main/java/com/kapos/mobile/core/components/AppBottomNav.kt`:
- Update `getBottomNavDestinations(UserRole.OWNER)` to include `NavDestination.Shift`.
- Add `NavDestination.Report` as an available destination.

In `app/src/main/java/com/kapos/mobile/feature/settings/SettingsScreen.kt`:
- Add a tile "Laporan & Rekap Penjualan" in the "Kelola Bisnis & Operasional" section that triggers `onNavigateToReport()`.

In `app/src/main/java/com/kapos/mobile/ui/KaPosApp.kt`:
- Pass `onNavigateToReport = { navController.navigate("report") }` to `SettingsRoute`.

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests com.kapos.mobile.core.components.AppNavDestinationsTest`
Expected: PASS

---

### Task 5: POS Polish (Order Number Suffix, Dynamic Category Label, Store Name in Dialog)

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/feature/checkout/CheckoutViewModel.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/pos/ProductCard.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/pos/PosScreen.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/transactions/OrderDetailDialog.kt`
- Test: `app/src/test/java/com/kapos/mobile/feature/checkout/OrderNoGenerationTest.kt`

**Interfaces:**
- Produces: Collision-proof order number generator, dynamic category tags on product cards, store name from settings in receipt dialog.

- [ ] **Step 1: Write test for order number uniqueness & format**

```kotlin
// app/src/test/java/com/kapos/mobile/feature/checkout/OrderNoGenerationTest.kt
package com.kapos.mobile.feature.checkout

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderNoGenerationTest {
    @Test
    fun testGenerateUniqueOrderNo() {
        val no1 = generateOrderNo()
        val no2 = generateOrderNo()
        assertTrue(no1.startsWith("KA-"))
        assertNotEquals(no1, no2)
    }

    private fun generateOrderNo(): String {
        val rand = (100..999).random()
        return "KA-${System.currentTimeMillis()}-$rand"
    }
}
```

- [ ] **Step 2: Run test**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests com.kapos.mobile.feature.checkout.OrderNoGenerationTest`
Expected: PASS

- [ ] **Step 3: Update OrderNo generation in CheckoutViewModel**

In `CheckoutViewModel.initCheckout`:
Add a 3-character random uppercase alphanumeric suffix to prevent duplicate order numbers on same-second checkouts:
`"KA-${dateFormat.format(Date())}-${(100..999).random()}"`.

- [ ] **Step 4: Update ProductCard to accept categoryName parameter**

In `app/src/main/java/com/kapos/mobile/feature/pos/ProductCard.kt`:
- Add optional `categoryName: String? = null` parameter.
- Use `categoryName ?: "MENU"` so custom categories display their actual names instead of hardcoded strings.
- Pass `categories.find { it.id == product.categoryId }?.name` from `PosScreen.kt`.

- [ ] **Step 5: Dynamic store header in OrderDetailDialog**

In `app/src/main/java/com/kapos/mobile/feature/transactions/OrderDetailDialog.kt`:
- Pass `storeName` and `storeAddress` into `OrderDetailDialog` from `TransactionsScreen` (obtained from settings).
- Remove hardcoded `"🍢 Taichan & Chicken KA - Bandar Lampung"`.

- [ ] **Step 6: Run full test suite & verify 100% build pass**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest`
Expected: PASS

---

### Task 6: Final Verification & Smoke Test

- [ ] Run full project compilation: `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug test`
- [ ] Verify zero regressions in unit tests across all features.
