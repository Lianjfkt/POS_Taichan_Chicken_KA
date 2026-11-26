# Backdated Stock Restock & Anti-Fraud Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable backdated stock restock entry (up to 7 days in the past) with automatic relative expiry date calculation, visual expiry warnings, and an immutable anti-fraud audit watermark on mutation records.

**Architecture:** Extend `InventoryRepository.restockMaterialWithBatch` to accept `receivedDate`, enforce strict 7-day past boundary validation, generate audit watermarks for backdated mutations, and maintain FIFO order. Update `RestockMaterialDialog` in `RawMaterialDialogs.kt` with a Material 3 `DatePickerDialog` restricted via `SelectableDates`, relative expiry date calculation, and real-time expiration warning banner.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 DatePicker, Room Database, Coroutines, JUnit4.

## Global Constraints
- Target platform: Android (SDK 26+), Jetpack Compose with Material 3.
- Backdated restock entry is strictly limited between `(now - 7 days)` and `now`.
- Future dates (`receivedDate > now`) must be rejected at both UI and repository levels.
- Expiry date is calculated relative to `receivedDate`: `expiryDate = receivedDate + (days * 24h)`.
- Anti-fraud watermark is automatically appended to `notes` when `receivedDate` is on a past day.
- Monetary calculations must use `Double` with zero-safe division.

---

### Task 1: Repository Layer & Anti-Fraud Audit Logic

**Files:**
- Create: `app/src/test/java/com/kapos/mobile/core/data/repository/InventoryBackdatedRestockTest.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/repository/InventoryRepository.kt:115-185`

**Interfaces:**
- Consumes: `RawMaterialDao`, `MaterialBatchDao`, `StockMovementDao`, `ProductDao`, `RecipeDao`
- Produces:
  - `InventoryRepository.restockMaterialWithBatch(materialId: String, quantity: Double, costTotal: Double, expiryDate: Long? = null, notes: String = "", receivedDate: Long = System.currentTimeMillis()): MaterialBatch`
  - Validates `receivedDate in (now - 7 days)..now + 60_000L`
  - Appends `[BACKDATED: Diterima dd/MM/yyyy | Diinput dd/MM/yyyy HH:mm]` to notes when backdated.
  - Updates `raw_materials.lastRestockedAt` using `maxOf(currentLastRestockedAt, receivedDate)`.

- [ ] **Step 1: Write the failing unit tests for backdated restock and anti-fraud validation**

```kotlin
// app/src/test/java/com/kapos/mobile/core/data/repository/InventoryBackdatedRestockTest.kt
package com.kapos.mobile.core.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InventoryBackdatedRestockTest {

    private val oneDayMs = 24L * 60 * 60 * 1000

    @Test
    fun testBackdatedValidationRules() {
        val now = 1730520000000L // arbitrary fixed timestamp
        val sevenDaysAgo = now - (7L * oneDayMs)

        // Valid: today
        assertTrue(isValidReceivedDate(now, now))
        // Valid: 2 days ago
        assertTrue(isValidReceivedDate(now - (2L * oneDayMs), now))
        // Valid: exactly 7 days ago
        assertTrue(isValidReceivedDate(sevenDaysAgo, now))

        // Invalid: 8 days ago
        assertFalse(isValidReceivedDate(now - (8L * oneDayMs), now))
        // Invalid: future date (+5 minutes)
        assertFalse(isValidReceivedDate(now + (5 * 60 * 1000L), now))
    }

    @Test
    fun testAntiFraudWatermarkGeneration() {
        val now = 1730520000000L // 02 Nov 2024 04:00 GMT
        val yesterday = now - oneDayMs

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val backdatedWatermark = generateAuditWatermark(
            receivedDate = yesterday,
            recordedAt = now,
            userNotes = "Pasar Senen"
        )

        assertTrue(backdatedWatermark.contains("Pasar Senen"))
        assertTrue(backdatedWatermark.contains("[BACKDATED: Diterima ${dateFormat.format(Date(yesterday))} | Diinput ${timeFormat.format(Date(now))}]"))

        // Same day restock should NOT have BACKDATED tag
        val sameDayNotes = generateAuditWatermark(
            receivedDate = now,
            recordedAt = now,
            userNotes = "Pasar Senen"
        )
        assertEquals("Pasar Senen", sameDayNotes)
    }

    @Test
    fun testRelativeExpiryCalculationFromReceivedDate() {
        val receivedDate = 1730433600000L // yesterday
        val shelfLifeDays = 3
        val expectedExpiry = receivedDate + (shelfLifeDays * oneDayMs)

        assertEquals(receivedDate + (3L * oneDayMs), expectedExpiry)
    }

    private fun isValidReceivedDate(receivedDate: Long, now: Long): Boolean {
        val minAllowed = now - (7L * 24 * 60 * 60 * 1000)
        val maxAllowed = now + 60_000L
        return receivedDate in minAllowed..maxAllowed
    }

    private fun generateAuditWatermark(receivedDate: Long, recordedAt: Long, userNotes: String): String {
        val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val isBackdated = dayFormat.format(Date(receivedDate)) != dayFormat.format(Date(recordedAt))
        return if (isBackdated) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val watermark = "[BACKDATED: Diterima ${dateFormat.format(Date(receivedDate))} | Diinput ${timeFormat.format(Date(recordedAt))}]"
            if (userNotes.isNotBlank()) "${userNotes.trim()} $watermark" else watermark
        } else {
            userNotes.trim()
        }
    }
}
```

- [ ] **Step 2: Run test to verify it passes math & logic checks**

Run: `./gradlew testDebugUnitTest --tests "com.kapos.mobile.core.data.repository.InventoryBackdatedRestockTest"`
Expected: PASS

- [ ] **Step 3: Update `InventoryRepository.kt` with `receivedDate` parameter and anti-fraud logic**

In `app/src/main/java/com/kapos/mobile/core/data/repository/InventoryRepository.kt`:
Update `restockMaterialWithBatch`:
```kotlin
    suspend fun restockMaterialWithBatch(
        materialId: String,
        quantity: Double,
        costTotal: Double,
        expiryDate: Long? = null,
        notes: String = "",
        receivedDate: Long = System.currentTimeMillis()
    ): MaterialBatch = withContext(Dispatchers.IO) {
        if (quantity <= 0.0) throw IllegalArgumentException("Jumlah masuk harus > 0")
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000)
        require(receivedDate in sevenDaysAgo..(now + 60_000L)) {
            "Tanggal penerimaan tidak valid (maksimal 7 hari ke belakang dan tidak boleh di masa depan)"
        }

        val material = rawMaterialDao.getById(materialId) ?: throw IllegalArgumentException("Bahan tidak ditemukan")

        val unitCost = if (quantity > 0) costTotal / quantity else 0.0
        val batchId = UUID.randomUUID().toString()
        val batchNumber = "B-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date(receivedDate))

        // Generate Anti-Fraud Audit Watermark
        val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val isBackdated = dayFormat.format(Date(receivedDate)) != dayFormat.format(Date(now))
        val finalNotes = if (isBackdated) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val watermark = "[BACKDATED: Diterima ${dateFormat.format(Date(receivedDate))} | Diinput ${timeFormat.format(Date(now))}]"
            if (notes.isNotBlank()) "${notes.trim()} $watermark" else watermark
        } else {
            notes.trim()
        }

        val batchEntity = MaterialBatchEntity(
            id = batchId,
            materialId = materialId,
            batchNumber = batchNumber,
            initialQuantity = quantity,
            remainingQuantity = quantity,
            costTotal = costTotal,
            costPerUnit = unitCost,
            receivedDate = receivedDate,
            expiryDate = expiryDate,
            isUsable = true,
            notes = finalNotes
        )
        materialBatchDao.insert(batchEntity)

        // Moving Weighted Average Calculation:
        val oldStock = material.currentStock.coerceAtLeast(0.0)
        val oldCost = material.costPerUnit.coerceAtLeast(0.0)
        val newTotalStock = oldStock + quantity
        val newAvgCost = if (newTotalStock > 0.0) {
            ((oldStock * oldCost) + costTotal) / newTotalStock
        } else {
            unitCost
        }

        rawMaterialDao.upsert(
            material.copy(
                currentStock = newTotalStock,
                costPerUnit = newAvgCost,
                lastRestockedAt = maxOf(material.lastRestockedAt, receivedDate)
            )
        )

        updateMaterialExpiryState(materialId)

        // Record Stock Movement with backdated timestamp and audit watermark
        stockMovementDao.insert(
            StockMovementEntity(
                id = UUID.randomUUID().toString(),
                materialId = materialId,
                type = StockMovementType.RESTOCK.name,
                quantity = quantity,
                costTotal = costTotal,
                notes = "Restock $batchNumber: $finalNotes (Harga Beli: Rp ${unitCost.toInt()}/${material.unit})",
                timestamp = receivedDate
            )
        )

        // Cascade update BOP to all products with this material in Recipe BOM
        recalculateAllProductsBopForMaterial(materialId)

        batchEntity.toDomain()
    }
```

- [ ] **Step 4: Run unit tests to verify repository changes compile and pass**

Run: `./gradlew testDebugUnitTest --tests "com.kapos.mobile.core.data.repository.*"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kapos/mobile/core/data/repository/InventoryRepository.kt app/src/test/java/com/kapos/mobile/core/data/repository/InventoryBackdatedRestockTest.kt
git commit -m "feat(stock): support backdated restock with anti-fraud audit watermark in InventoryRepository"
```

---

### Task 2: ViewModel Layer Update (`StockViewModel`)

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/feature/stock/StockViewModel.kt:380-398`

**Interfaces:**
- Consumes: `InventoryRepository.restockMaterialWithBatch`
- Produces:
  - `StockViewModel.confirmRestockWithBatch(materialId: String, quantity: Double, costTotal: Double, expiryDate: Long?, notes: String, receivedDate: Long = System.currentTimeMillis())`

- [ ] **Step 1: Update `StockViewModel.kt` to forward `receivedDate`**

In `app/src/main/java/com/kapos/mobile/feature/stock/StockViewModel.kt`:
Update `confirmRestockWithBatch`:
```kotlin
    fun confirmRestockWithBatch(
        materialId: String,
        quantity: Double,
        costTotal: Double,
        expiryDate: Long?,
        notes: String,
        receivedDate: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            inventoryRepository?.restockMaterialWithBatch(
                materialId = materialId,
                quantity = quantity,
                costTotal = costTotal,
                expiryDate = expiryDate,
                notes = notes,
                receivedDate = receivedDate
            )
            closeRestockMaterial()
        }
    }
```

- [ ] **Step 2: Run existing Stock ViewModel tests**

Run: `./gradlew testDebugUnitTest --tests "com.kapos.mobile.feature.stock.StockBatchViewModelTest"`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/kapos/mobile/feature/stock/StockViewModel.kt
git commit -m "feat(stock): forward receivedDate in StockViewModel restock action"
```

---

### Task 3: UI DatePicker & Reactive Expiry / Alert in Dialog (`RawMaterialDialogs.kt` & `StockScreen.kt`)

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/feature/stock/RawMaterialDialogs.kt:250-418`
- Modify: `app/src/main/java/com/kapos/mobile/feature/stock/StockScreen.kt:155, 530-538`

**Interfaces:**
- Consumes: Material 3 `DatePickerDialog`, `rememberDatePickerState`, `SelectableDates`
- Produces:
  - `RestockMaterialDialog`:
    - Shows "Tanggal Masuk Barang" with clickable calendar field.
    - `DatePickerDialog` restricted to H-7 s/d Hari Ini.
    - Expiry duration chips calculate relative to `selectedReceivedDate`.
    - Shows warning banner when computed `expiryTimestamp <= now`.
    - Emits `onConfirm(quantity, cost, expiryTimestamp, notes, selectedReceivedDate)`.
  - `StockScreen.kt`:
    - Passes `selectedReceivedDate` to `viewModel.confirmRestockWithBatch`.

- [ ] **Step 1: Update `RestockMaterialDialog` in `RawMaterialDialogs.kt`**

Add state for received date and date picker dialog:
```kotlin
    var selectedReceivedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
```
Add DatePicker restriction for H-7 s/d Hari Ini:
```kotlin
    val sevenDaysAgo = remember(now) { now - (7L * oneDay) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedReceivedDate,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // Buffer by 1 day UTC to avoid timezone shift issues
                return utcTimeMillis in (sevenDaysAgo - oneDay)..(now + oneDay)
            }
            override fun isSelectableYear(year: Int): Boolean = true
        }
    )
```

In the dialog UI Column, add the "Tanggal Masuk Barang" picker:
- Row displaying `Icons.Default.DateRange` or calendar icon and formatted date: `SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date(selectedReceivedDate))`.
- If `!isSameDay(selectedReceivedDate, now)`, display a subtle warning pill: `Backdated (Tgl Lampau)`.
- Expiry calculation:
  ```kotlin
  val expiryTimestamp = selectedExpiryDays?.let { selectedReceivedDate + (it * oneDay) }
  val isAlreadyExpired = expiryTimestamp != null && expiryTimestamp <= now
  ```
- If `isAlreadyExpired`, show a red banner:
  ```kotlin
  Text(
      text = "⚠️ Perhatian: Berdasarkan tanggal masuk (${formatDate(selectedReceivedDate)}) dan masa simpan, bahan ini sudah KADALUARSA saat ini!",
      style = MaterialTheme.typography.bodySmall.copy(color = Danger, fontWeight = FontWeight.Bold)
  )
  ```
- In `onConfirm` callback invocation:
  ```kotlin
  onConfirm(qty, cost, expiryTimestamp, notes, selectedReceivedDate)
  ```

- [ ] **Step 2: Update `StockScreen.kt` to pass `receivedDate` parameter**

Update callback signature and invocation in `StockScreen.kt`:
```kotlin
onConfirmRestockWithBatch: (materialId: String, quantity: Double, cost: Double, expiryDate: Long?, notes: String, receivedDate: Long) -> Unit
```
And wire it up to `viewModel.confirmRestockWithBatch`.

- [ ] **Step 3: Compile code and run all unit tests**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all unit tests PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/kapos/mobile/feature/stock/RawMaterialDialogs.kt app/src/main/java/com/kapos/mobile/feature/stock/StockScreen.kt
git commit -m "feat(stock-ui): add Material3 DatePicker for backdated restock with reactive expiry warning"
```

---

### Task 4: Comprehensive Regression & Verification

**Files:**
- Modify: `app/src/test/java/com/kapos/mobile/feature/stock/StockIntegrationWorkflowTest.kt`

**Interfaces:**
- Verify: Full end-to-end integration scenario with backdated batch arrival, anti-fraud watermark logging, FIFO consumption priority, and expiry status verification.

- [ ] **Step 1: Add backdated batch test case to `StockIntegrationWorkflowTest.kt`**

Add test verifying:
1. Batch registered with backdated `receivedDate = now - (2 * oneDayMs)`.
2. 3-day expiry correctly computes as `receivedDate + 3 days` = `now + 1 day` (sisa 1 hari).
3. Freshness is evaluated accurately as `EXPIRING_SOON` instead of `FRESH`.
4. FIFO queue places this backdated batch before a newly arrived batch expiring later.

- [ ] **Step 2: Run full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: ALL TESTS PASS.

- [ ] **Step 3: Run full Kotlin compilation check**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/test/java/com/kapos/mobile/feature/stock/StockIntegrationWorkflowTest.kt
git commit -m "test(stock): add regression test for backdated batch restock and FIFO evaluation"
```
