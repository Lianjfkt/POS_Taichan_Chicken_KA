# Phase 1: Cashier Operations, Shift Reconciliation & Void Audit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Menambahkan manajemen Petty Cash (Kas Masuk/Keluar), rekonsiliasi tutup shift dengan Blind Cash Count (pecahan uang), cetak struk shift Bluetooth & WhatsApp share, serta audit alasan void transaksi pada aplikasi Android POS KA.

**Architecture:** Memperluas database Room SQLite (menambahkan entitas `CashMovementEntity`, `CashMovementDao`, memperbarui `OrderEntity` dengan kolom `voidReason`), mengekspos API baru di `ShiftRepository` dan `PosRepository`, memperluas `EscPosBuilder` dengan fungsi `buildShiftReceipt`, dan merombak UI di `ShiftScreen` serta `TransactionsScreen`.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Room SQLite DB, Coroutines & StateFlow, ESC/POS Bluetooth Printer, Android Share Intent.

## Global Constraints
- Target platform: Android SDK 26 (Min) / 34 (Target), Kotlin 1.9+, Jetpack Compose Material 3.
- All database migrations use `fallbackToDestructiveMigration()`.
- Currency formatting must follow Indonesian Rupiah (`Rp #,###`).
- Pin verification must check against `SettingsRepository.settingsFlow.value.ownerPin`.
- JAVA_HOME must be set to `/opt/android-studio/jbr` when running Gradle commands.

---

### Task 1: Room DB & Domain Model Extension (Petty Cash & Void Reason)

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/domain/model/Models.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/local/entity/Entities.kt`
- Create: `app/src/main/java/com/kapos/mobile/core/data/local/dao/CashMovementDao.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/local/dao/OrderDao.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/local/AppDatabase.kt`
- Create: `app/src/test/java/com/kapos/mobile/core/data/local/CashMovementDaoTest.kt`

**Interfaces:**
- Consumes: `AppDatabase`, `ShiftEntity`, `OrderEntity`
- Produces: `CashMovementType`, `CashMovement`, `CashMovementEntity`, `CashMovementDao.observeByShift`, `CashMovementDao.insert`, `OrderDao.voidOrderWithReason`

- [ ] **Step 1: Write the failing unit test for Petty Cash & Void Order Room mapping**

```kotlin
// app/src/test/java/com/kapos/mobile/core/data/local/CashMovementDaoTest.kt
package com.kapos.mobile.core.data.local

import com.kapos.mobile.domain.model.CashMovementType
import org.junit.Assert.assertEquals
import org.junit.Test

class CashMovementDaoTest {
    @Test
    fun testCashMovementTypeDisplayName() {
        assertEquals("Kas Masuk", CashMovementType.CASH_IN.displayName)
        assertEquals("Kas Keluar", CashMovementType.CASH_OUT.displayName)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests com.kapos.mobile.core.data.local.CashMovementDaoTest`  
Expected: FAIL (CashMovementType unresolvable)

- [ ] **Step 3: Update Models, Entities, DAOs, and AppDatabase**

Update `Models.kt` with `CashMovementType` and `CashMovement`, and add `voidReason` & `voidedAt` to `Order`.
Update `Entities.kt` with `CashMovementEntity`, and add `voidReason` & `voidedAt` to `OrderEntity`.
Create `CashMovementDao.kt`.
Update `OrderDao.kt` to include `voidOrderWithReason(orderId: String, reason: String, timestamp: Long)`.
Update `AppDatabase.kt` to register `CashMovementEntity::class`, bump version to 2, and add `abstract fun cashMovementDao(): CashMovementDao`.

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests com.kapos.mobile.core.data.local.CashMovementDaoTest`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kapos/mobile/domain/model/Models.kt \
        app/src/main/java/com/kapos/mobile/core/data/local/entity/Entities.kt \
        app/src/main/java/com/kapos/mobile/core/data/local/dao/CashMovementDao.kt \
        app/src/main/java/com/kapos/mobile/core/data/local/dao/OrderDao.kt \
        app/src/main/java/com/kapos/mobile/core/data/local/AppDatabase.kt \
        app/src/test/java/com/kapos/mobile/core/data/local/CashMovementDaoTest.kt
git commit -m "feat(db): add CashMovementEntity and voidReason to OrderEntity"
```

---

### Task 2: Repository Layer (ShiftRepository & PosRepository Updates)

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/core/data/repository/ShiftRepository.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/repository/PosRepository.kt`
- Create: `app/src/test/java/com/kapos/mobile/core/data/repository/ShiftReconciliationTest.kt`

**Interfaces:**
- Consumes: `CashMovementDao`, `OrderDao`, `ShiftDao`
- Produces: `ShiftRepository.observeCashMovements(shiftId: String)`, `ShiftRepository.addCashMovement(...)`, `ShiftRepository.calculateShiftSummary(...)`, `PosRepository.voidOrder(orderId: String, reason: String)`

- [ ] **Step 1: Write the failing unit test for Shift Reconciliation calculation**

```kotlin
// app/src/test/java/com/kapos/mobile/core/data/repository/ShiftReconciliationTest.kt
package com.kapos.mobile.core.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftReconciliationTest {
    @Test
    fun testReconciliationFormula() {
        val startCash = 200_000.0
        val cashSales = 500_000.0
        val cashIn = 50_000.0
        val cashOut = 30_000.0
        val expectedCash = startCash + cashSales + cashIn - cashOut
        val actualCash = 720_000.0
        val selisih = actualCash - expectedCash

        assertEquals(720_000.0, expectedCash, 0.001)
        assertEquals(0.0, selisih, 0.001)
    }
}
```

- [ ] **Step 2: Run test to verify it passes**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests com.kapos.mobile.core.data.repository.ShiftReconciliationTest`  
Expected: PASS

- [ ] **Step 3: Implement repository methods**

Update `ShiftRepository.kt`:
- Add `observeCashMovements(shiftId: String): Flow<List<CashMovement>>`
- Add `addCashMovement(shiftId: String, type: CashMovementType, category: String, amount: Double, notes: String): CashMovement`
- Update `closeShift` to accurately compute `systemSales` and save.

Update `PosRepository.kt`:
- Update `voidOrder(orderId: String, reason: String)` to pass reason and timestamp to `orderDao.voidOrderWithReason`.

- [ ] **Step 4: Run unit tests**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest`  
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kapos/mobile/core/data/repository/ShiftRepository.kt \
        app/src/main/java/com/kapos/mobile/core/data/repository/PosRepository.kt \
        app/src/test/java/com/kapos/mobile/core/data/repository/ShiftReconciliationTest.kt
git commit -m "feat(repo): update ShiftRepository for cash movements and PosRepository for void reason"
```

---

### Task 3: EscPosBuilder Shift Receipt & WhatsApp Text Formatter

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/core/printer/EscPosBuilder.kt`
- Create: `app/src/main/java/com/kapos/mobile/core/utils/ShiftShareFormatter.kt`
- Modify: `app/src/test/java/com/kapos/mobile/core/printer/EscPosBuilderTest.kt`

**Interfaces:**
- Consumes: `EscPosBuilder`, `Shift`, `CashMovement`
- Produces: `EscPosBuilder.buildShiftReceipt(...)`, `ShiftShareFormatter.formatWhatsAppSummary(...)`

- [ ] **Step 1: Write the failing unit test for buildShiftReceipt and ShiftShareFormatter**

```kotlin
// In app/src/test/java/com/kapos/mobile/core/printer/EscPosBuilderTest.kt
@Test
fun testBuildShiftReceiptProducesBytes() {
    val builder = EscPosBuilder(paperWidth = 58)
    val bytes = builder.buildShiftReceipt(
        storeName = "KA Taichan & Chicken",
        cashierName = "Kasir Budi",
        openedAtStr = "16 Okt 2026, 10:00",
        closedAtStr = "16 Okt 2026, 22:00",
        startCash = 200000.0,
        cashSales = 1000000.0,
        nonCashSales = 500000.0,
        cashIn = 50000.0,
        cashOut = 20000.0,
        expectedCash = 1230000.0,
        actualCash = 1230000.0,
        selisih = 0.0
    )
    assertTrue(bytes.isNotEmpty())
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests com.kapos.mobile.core.printer.EscPosBuilderTest`  
Expected: FAIL (`buildShiftReceipt` does not exist)

- [ ] **Step 3: Implement `buildShiftReceipt` and `ShiftShareFormatter`**

In `EscPosBuilder.kt`:
Implement `buildShiftReceipt(...)` formatting store name, shift open/close timestamps, cash in/out breakdown, expected cash vs actual physical cash, difference, non-cash total, and signature line.

Create `ShiftShareFormatter.kt`:
Generate clean WhatsApp text with emojis: store name, cashier, date, start cash, cash sales, non-cash sales, petty cash in/out, expected cash, actual physical cash, and diff status (`PAS ✅`, `MINUS ❌`, `LEBIH ⚠️`).

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests com.kapos.mobile.core.printer.EscPosBuilderTest`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kapos/mobile/core/printer/EscPosBuilder.kt \
        app/src/main/java/com/kapos/mobile/core/utils/ShiftShareFormatter.kt \
        app/src/test/java/com/kapos/mobile/core/printer/EscPosBuilderTest.kt
git commit -m "feat(printer): add buildShiftReceipt to EscPosBuilder and ShiftShareFormatter"
```

---

### Task 4: Shift ViewModel & UI (Petty Cash Dialogs & Blind Cash Count)

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/feature/shift/ShiftUiState.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/shift/ShiftViewModel.kt`
- Create: `app/src/main/java/com/kapos/mobile/feature/shift/PettyCashDialog.kt`
- Create: `app/src/main/java/com/kapos/mobile/feature/shift/BlindCountDialog.kt`
- Create: `app/src/main/java/com/kapos/mobile/feature/shift/ShiftReportSummaryDialog.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/shift/ShiftScreen.kt`
- Create: `app/src/test/java/com/kapos/mobile/feature/shift/ShiftViewModelTest.kt`

**Interfaces:**
- Consumes: `ShiftRepository`, `SettingsRepository`, `BluetoothPrinterService`
- Produces: Complete Shift management UI with Petty Cash In/Out, Blind Cash Count breakdown, and post-shift print/share

- [ ] **Step 1: Write unit tests for ShiftViewModel calculation & petty cash actions**

```kotlin
// app/src/test/java/com/kapos/mobile/feature/shift/ShiftViewModelTest.kt
package com.kapos.mobile.feature.shift

import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftViewModelTest {
    @Test
    fun testBlindCountSum() {
        val breakdown = mapOf(
            100_000 to 5, // 500.000
            50_000 to 4,  // 200.000
            20_000 to 5,  // 100.000
            10_000 to 0,
            5_000 to 0,
            2_000 to 0,
            1_000 to 0
        )
        val coin = 15_000.0
        val total = breakdown.entries.sumOf { it.key * it.value }.toDouble() + coin
        assertEquals(815_000.0, total, 0.001)
    }
}
```

- [ ] **Step 2: Run test to verify it passes**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests com.kapos.mobile.feature.shift.ShiftViewModelTest`  
Expected: PASS

- [ ] **Step 3: Implement ShiftUiState and ShiftViewModel updates**

In `ShiftUiState.kt`:
- Add `cashMovements: List<CashMovement> = emptyList()`
- Add `cashInTotal: Double = 0.0`, `cashOutTotal: Double = 0.0`
- Add `cashSalesTotal: Double = 0.0`, `nonCashSalesTotal: Double = 0.0`
- Add blind count fields: `count100k`, `count50k`, `count20k`, `count10k`, `count5k`, `count2k`, `count1k`, `countCoin`
- Add dialog visibility flags: `showPettyCashDialog`, `pettyCashType`, `showBlindCountDialog`, `showShiftReportDialog`

In `ShiftViewModel.kt`:
- Implement `onShowPettyCashDialog(type: CashMovementType)`
- Implement `submitPettyCash(category: String, amount: Double, notes: String)`
- Implement `onBlindCountChange(denomination: Int, count: Int)`
- Implement `printShiftReceipt()` and `getWhatsAppShareText(): String`

- [ ] **Step 4: Implement Compose Dialogs and ShiftScreen UI**

Create `PettyCashDialog.kt`: Form input category dropdown/chips (Bahan Baku, Operasional, Galon/Es, Setoran, Lainnya), amount, notes.
Create `BlindCountDialog.kt`: Stepper/number input for each denomination with dynamic physical cash sum.
Create `ShiftReportSummaryDialog.kt`: Displays reconciliation status, "Cetak Struk Shift" button, and "Kirim ke WhatsApp" button.
Update `ShiftScreen.kt`: Integrate action buttons `+ Kas Masuk`, `- Kas Keluar`, list of petty cash movements, and trigger blind count on close shift.

- [ ] **Step 5: Run unit tests**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest`  
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/kapos/mobile/feature/shift/* \
        app/src/test/java/com/kapos/mobile/feature/shift/ShiftViewModelTest.kt
git commit -m "feat(ui): add Petty Cash dialogs, Blind Cash Count, and Shift reconciliation UI"
```

---

### Task 5: Transaction Void Dialog with Reason Selection & Audit Banner

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/feature/transactions/TransactionsUiState.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/transactions/TransactionsViewModel.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/transactions/TransactionsScreen.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/transactions/OrderDetailDialog.kt`
- Create: `app/src/test/java/com/kapos/mobile/feature/transactions/VoidReasonValidationTest.kt`

**Interfaces:**
- Consumes: `PosRepository.voidOrder(orderId, reason)`
- Produces: Void Dialog with Reason Chips & Notes, and Void Audit Banner in `OrderDetailDialog`

- [ ] **Step 1: Write unit test for Void reason options & validation**

```kotlin
// app/src/test/java/com/kapos/mobile/feature/transactions/VoidReasonValidationTest.kt
package com.kapos.mobile.feature.transactions

import org.junit.Assert.assertTrue
import org.junit.Test

class VoidReasonValidationTest {
    @Test
    fun testPredefinedVoidReasonsNotEmpty() {
        val reasons = listOf(
            "Salah Input Menu",
            "Pelanggan Membatalkan",
            "Meja Pindah / Ganti Tagihan",
            "Komplain Makanan",
            "Lainnya"
        )
        assertTrue(reasons.size >= 5)
    }
}
```

- [ ] **Step 2: Run test to verify it passes**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests com.kapos.mobile.feature.transactions.VoidReasonValidationTest`  
Expected: PASS

- [ ] **Step 3: Update TransactionsUiState and TransactionsViewModel**

In `TransactionsUiState.kt`:
- Add `selectedVoidReason: String = ""`
- Add `voidReasonNotes: String = ""`
- Add `voidReasonError: String? = null`

In `TransactionsViewModel.kt`:
- Add `onSelectVoidReason(reason: String)`
- Add `onVoidReasonNotesChange(notes: String)`
- Update `confirmVoid()`: require valid reason selection (and notes if "Lainnya" selected) before verifying Owner PIN and calling `repository.voidOrder(order.id, finalReason)`.

- [ ] **Step 4: Update TransactionsScreen and OrderDetailDialog**

In `TransactionsScreen.kt`:
- Enhance `VoidPinDialog`: Add reason chips (`Salah Input Menu`, `Pelanggan Membatalkan`, `Meja Pindah`, `Komplain Makanan`, `Lainnya`), notes textfield, and PIN input.
In `OrderDetailDialog.kt`:
- If `order.paymentStatus == PaymentStatus.VOID`, show prominent red warning banner:
  - "TRANSAKSI DIBATALKAN (VOID)"
  - "Alasan: ${order.voidReason ?: '-'}"
  - "Waktu Void: ${formattedTimestamp}"

- [ ] **Step 5: Run unit tests**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest`  
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/kapos/mobile/feature/transactions/* \
        app/src/test/java/com/kapos/mobile/feature/transactions/VoidReasonValidationTest.kt
git commit -m "feat(transactions): add void reason selection and audit banner in order details"
```

---

### Task 6: Full Verification, Clean Build & APK Assembly

**Files:**
- Entire codebase

**Interfaces:**
- End-to-end verification of Phase 1 deliverables

- [ ] **Step 1: Run all unit tests**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest`  
Expected: All tests pass with zero failures.

- [ ] **Step 2: Build Debug APK**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug`  
Expected: BUILD SUCCESSFUL with `app-debug.apk` generated.

- [ ] **Step 3: Copy generated APK to root for user inspection**

```bash
cp app/build/outputs/apk/debug/app-debug.apk "../KA-POS-debug.apk"
```

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "chore: complete Phase 1 cashier operations, shift reconciliation and void audit"
```
