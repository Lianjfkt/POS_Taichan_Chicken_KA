# Fitur Custom Date Data Penjualan Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Menambahkan fitur filter tanggal fleksibel (Preset Hari Ini, 7 Hari, Bulan Ini, Semua, dan Custom Date Range dengan Material 3 DateRangePicker) pada menu Riwayat Transaksi, Laporan Keuangan, dan Analisa BEP.

**Architecture:** Model filter waktu tunggal (`DateRangeFilter`) di domain/model bersama komponen UI terpadu (`DateFilterBar` & `DateRangePickerDialog`) di core/components. Masing-masing ViewModel memfilter pesanan dan agregasi keuangan berdasarkan rentang timestamp millisecond yang dihasilkan dari `DateRangeFilter`.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 (`DateRangePicker`, `DatePickerDialog`), Coroutines/Flow, JUnit 4.

## Global Constraints
- Target platform: Android (Kotlin & Jetpack Compose).
- Timezone akurat: Jam awal 00:00:00.000 dan jam akhir 23:59:59.999 pada zona waktu lokal perangkat (`Calendar.getInstance()`).
- Single day selection: Jika pengguna memilih satu hari, rentang berakhir pada 23:59:59.999 hari yang sama.
- Clean Code & TDD: Setiap task memiliki pengujian otomatis (Unit Test) sebelum committed.

---

### Task 1: Domain Model `DateRangeFilter` & Unit Test

**Files:**
- Create: `app/src/main/java/com/kapos/mobile/domain/model/DateRangeFilter.kt`
- Test: `app/src/test/java/com/kapos/mobile/domain/model/DateRangeFilterTest.kt`

**Interfaces:**
- Produces:
  - `enum class DateFilterPreset(val displayName: String)`
  - `data class DateRangeFilter(val preset: DateFilterPreset = DateFilterPreset.TODAY, val customStartMillis: Long? = null, val customEndMillis: Long? = null)`
  - `fun DateRangeFilter.getTimeRange(nowMillis: Long = System.currentTimeMillis()): Pair<Long, Long>`
  - `fun DateRangeFilter.getDisplayLabel(): String`

- [ ] **Step 1: Write the failing unit tests**

Create `app/src/test/java/com/kapos/mobile/domain/model/DateRangeFilterTest.kt`:
```kotlin
package com.kapos.mobile.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DateRangeFilterTest {

    @Test
    fun testTodayTimeRange_startsAtMidnight_endsAtEndOfDay() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 19, 14, 30, 0)
        val testNow = cal.timeInMillis

        val filter = DateRangeFilter(preset = DateFilterPreset.TODAY)
        val (start, end) = filter.getTimeRange(testNow)

        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        val endCal = Calendar.getInstance().apply { timeInMillis = end }

        assertEquals(2026, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, startCal.get(Calendar.MONTH))
        assertEquals(19, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, startCal.get(Calendar.MINUTE))
        assertEquals(0, startCal.get(Calendar.SECOND))

        assertEquals(19, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertEquals(59, endCal.get(Calendar.SECOND))
    }

    @Test
    fun testCustomDateRange_singleDay_coversWholeDay() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 15, 10, 0, 0)
        val selectedDay = cal.timeInMillis

        val filter = DateRangeFilter(
            preset = DateFilterPreset.CUSTOM,
            customStartMillis = selectedDay,
            customEndMillis = selectedDay
        )
        val (start, end) = filter.getTimeRange()

        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        val endCal = Calendar.getInstance().apply { timeInMillis = end }

        assertEquals(15, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun testCustomDateRange_multiDays_correctBoundaries() {
        val startDay = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 10, 12, 0) }.timeInMillis
        val endDay = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 14, 8, 0) }.timeInMillis

        val filter = DateRangeFilter(
            preset = DateFilterPreset.CUSTOM,
            customStartMillis = startDay,
            customEndMillis = endDay
        )
        val (start, end) = filter.getTimeRange()

        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        val endCal = Calendar.getInstance().apply { timeInMillis = end }

        assertEquals(10, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(14, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
    }

    @Test
    fun testGetDisplayLabel() {
        assertEquals("Hari Ini", DateRangeFilter(DateFilterPreset.TODAY).getDisplayLabel())
        assertEquals("7 Hari", DateRangeFilter(DateFilterPreset.LAST_7_DAYS).getDisplayLabel())
        assertEquals("Bulan Ini", DateRangeFilter(DateFilterPreset.THIS_MONTH).getDisplayLabel())
        assertEquals("Semua", DateRangeFilter(DateFilterPreset.ALL).getDisplayLabel())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.kapos.mobile.domain.model.DateRangeFilterTest"`
Expected: FAIL (Unresolved reference: DateRangeFilter)

- [ ] **Step 3: Implement `DateRangeFilter.kt`**

Create `app/src/main/java/com/kapos/mobile/domain/model/DateRangeFilter.kt`:
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

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.kapos.mobile.domain.model.DateRangeFilterTest"`
Expected: PASS (4 tests passed)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kapos/mobile/domain/model/DateRangeFilter.kt app/src/test/java/com/kapos/mobile/domain/model/DateRangeFilterTest.kt
git commit -m "feat(domain): add DateRangeFilter model and unit tests"
```

---

### Task 2: Reusable UI Components (`DateFilterBar` & `DateRangePickerDialog`)

**Files:**
- Create: `app/src/main/java/com/kapos/mobile/core/components/DateRangePickerDialog.kt`
- Create: `app/src/main/java/com/kapos/mobile/core/components/DateFilterBar.kt`

**Interfaces:**
- Consumes: `DateRangeFilter`, `DateFilterPreset` from Task 1.
- Produces:
  - `@Composable fun DateRangePickerDialog(initialStartDate: Long?, initialEndDate: Long?, onConfirm: (startMillis: Long, endMillis: Long) -> Unit, onDismiss: () -> Unit)`
  - `@Composable fun DateFilterBar(currentFilter: DateRangeFilter, onPresetSelected: (DateFilterPreset) -> Unit, onOpenDatePicker: () -> Unit, modifier: Modifier = Modifier)`

- [ ] **Step 1: Implement `DateRangePickerDialog.kt`**

Create `app/src/main/java/com/kapos/mobile/core/components/DateRangePickerDialog.kt`:
Wrap `androidx.compose.material3.DateRangePicker` inside a `DatePickerDialog` with `rememberDateRangePickerState`. Include Indonesian button labels "Batal" dan "Terapkan".

- [ ] **Step 2: Implement `DateFilterBar.kt`**

Create `app/src/main/java/com/kapos/mobile/core/components/DateFilterBar.kt`:
Horizontal scrollable row with chips:
- "Hari Ini"
- "7 Hari"
- "Bulan Ini"
- "Semua"
- "📅 [Label Custom / Pilih Tanggal]"
Active styling: `Primary` background, `SurfaceWhite` text.
Inactive styling: `SurfaceWhite` background, `TextSecondary` text, `OutlineSoft` border.

- [ ] **Step 3: Verify build / lint**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/kapos/mobile/core/components/DateRangePickerDialog.kt app/src/main/java/com/kapos/mobile/core/components/DateFilterBar.kt
git commit -m "feat(ui): add DateFilterBar and DateRangePickerDialog components"
```

---

### Task 3: Integrasi Custom Date ke Riwayat Transaksi

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/feature/transactions/TransactionsUiState.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/transactions/TransactionsViewModel.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/transactions/TransactionsScreen.kt`
- Create: `app/src/test/java/com/kapos/mobile/feature/transactions/TransactionsDateFilterTest.kt`

**Interfaces:**
- Consumes: `DateRangeFilter`, `DateFilterPreset`, `DateFilterBar`, `DateRangePickerDialog` from Tasks 1 & 2.
- Produces: Date filtering on transactions list with date picker dialog support.

- [ ] **Step 1: Write failing test `TransactionsDateFilterTest.kt`**

Test filtering orders by date range + status + search:
```kotlin
package com.kapos.mobile.feature.transactions

import com.kapos.mobile.domain.model.DateFilterPreset
import com.kapos.mobile.domain.model.DateRangeFilter
import com.kapos.mobile.domain.model.Order
import com.kapos.mobile.domain.model.OrderType
import com.kapos.mobile.domain.model.PaymentMethod
import com.kapos.mobile.domain.model.PaymentStatus
import com.kapos.mobile.domain.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class TransactionsDateFilterTest {
    private fun createOrder(id: String, createdAt: Long, status: PaymentStatus = PaymentStatus.PAID): Order {
        return Order(
            id = id,
            orderNo = "ORD-$id",
            cashierId = "C1",
            cashierName = "Kasir",
            orderType = OrderType.DINE_IN,
            paymentMethod = PaymentMethod.CASH,
            paymentStatus = status,
            subtotal = 10000.0,
            discount = 0.0,
            total = 10000.0,
            syncStatus = SyncStatus.SYNCED,
            createdAt = createdAt,
            updatedAt = createdAt
        )
    }

    @Test
    fun testFilteredOrders_filtersByDateRange() {
        val cal = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 19, 10, 0) }
        val todayMillis = cal.timeInMillis
        val yesterdayMillis = cal.apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis

        val orderToday = createOrder("1", todayMillis)
        val orderYesterday = createOrder("2", yesterdayMillis)

        val state = TransactionsUiState(
            orders = listOf(orderToday, orderYesterday),
            dateFilter = DateRangeFilter(preset = DateFilterPreset.TODAY)
        )

        // filteredOrders should only contain orderToday
        val filtered = state.filteredOrders
        assertEquals(1, filtered.size)
        assertEquals("1", filtered[0].id)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.kapos.mobile.feature.transactions.TransactionsDateFilterTest"`
Expected: FAIL (Cannot resolve dateFilter in TransactionsUiState)

- [ ] **Step 3: Update `TransactionsUiState.kt` and `TransactionsViewModel.kt`**

In `TransactionsUiState.kt`:
- Add `val dateFilter: DateRangeFilter = DateRangeFilter(preset = DateFilterPreset.TODAY)`
- Add `val showDatePickerDialog: Boolean = false`
- In `filteredOrders`:
  ```kotlin
  val (startCutoff, endCutoff) = dateFilter.getTimeRange()
  list = list.filter { it.createdAt in startCutoff..endCutoff }
  ```

In `TransactionsViewModel.kt`:
- Add `fun onDatePresetChange(preset: DateFilterPreset)`
- Add `fun onCustomDateRangeSelected(startMillis: Long, endMillis: Long)`
- Add `fun onShowDatePickerDialog(show: Boolean)`

- [ ] **Step 4: Update `TransactionsScreen.kt`**

- Add `DateFilterBar` above `FilterChips` (Status) and below `SearchField`.
- Show `DateRangePickerDialog` when `uiState.showDatePickerDialog` is true.

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.kapos.mobile.feature.transactions.TransactionsDateFilterTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/kapos/mobile/feature/transactions/ app/src/test/java/com/kapos/mobile/feature/transactions/TransactionsDateFilterTest.kt
git commit -m "feat(transactions): integrate custom date filter and date picker dialog"
```

---

### Task 4: Integrasi Custom Date ke Laporan Keuangan

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/feature/report/ReportUiState.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/report/ReportViewModel.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/report/ReportScreen.kt`
- Create: `app/src/test/java/com/kapos/mobile/feature/report/ReportDateFilterTest.kt`

**Interfaces:**
- Consumes: `DateRangeFilter`, `DateFilterPreset`, `DateFilterBar`, `DateRangePickerDialog`.
- Produces: Accurate financial P&L reporting, CSV exports, and receipts for custom date ranges.

- [ ] **Step 1: Write failing test `ReportDateFilterTest.kt`**

Test financial calculations (revenue, discounts, net profit) within custom date range:
```kotlin
package com.kapos.mobile.feature.report

import com.kapos.mobile.domain.model.DateFilterPreset
import com.kapos.mobile.domain.model.DateRangeFilter
import com.kapos.mobile.domain.model.Order
import com.kapos.mobile.domain.model.OrderType
import com.kapos.mobile.domain.model.PaymentMethod
import com.kapos.mobile.domain.model.PaymentStatus
import com.kapos.mobile.domain.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class ReportDateFilterTest {
    private fun createOrder(id: String, createdAt: Long, total: Double): Order {
        return Order(
            id = id,
            orderNo = "ORD-$id",
            cashierId = "C1",
            cashierName = "Kasir",
            orderType = OrderType.DINE_IN,
            paymentMethod = PaymentMethod.CASH,
            paymentStatus = PaymentStatus.PAID,
            subtotal = total,
            discount = 0.0,
            total = total,
            syncStatus = SyncStatus.SYNCED,
            createdAt = createdAt,
            updatedAt = createdAt
        )
    }

    @Test
    fun testPeriodOrders_calculatedWithinDateRange() {
        val cal = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 19, 10, 0) }
        val order1 = createOrder("1", cal.timeInMillis, 50000.0)

        cal.add(Calendar.DAY_OF_YEAR, -5)
        val order2 = createOrder("2", cal.timeInMillis, 30000.0)

        val stateToday = ReportUiState(
            orders = listOf(order1, order2),
            dateFilter = DateRangeFilter(preset = DateFilterPreset.TODAY)
        )
        assertEquals(50000.0, stateToday.netRevenue, 0.01)

        val state7Days = ReportUiState(
            orders = listOf(order1, order2),
            dateFilter = DateRangeFilter(preset = DateFilterPreset.LAST_7_DAYS)
        )
        assertEquals(80000.0, state7Days.netRevenue, 0.01)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.kapos.mobile.feature.report.ReportDateFilterTest"`
Expected: FAIL (Cannot resolve dateFilter in ReportUiState)

- [ ] **Step 3: Update `ReportUiState.kt` and `ReportViewModel.kt`**

- In `ReportUiState.kt`:
  - Replace `selectedPeriod: ReportPeriod` with `dateFilter: DateRangeFilter = DateRangeFilter(preset = DateFilterPreset.TODAY)`
  - Add `val showDatePickerDialog: Boolean = false`
  - Update `periodCutoff` or replace with `(startCutoff, endCutoff) = dateFilter.getTimeRange()`:
    - `periodOrders: List<Order> get() = orders.filter { it.createdAt in startCutoff..endCutoff && it.paymentStatus == PaymentStatus.PAID }`
    - `periodVoidOrders: List<Order> get() = orders.filter { it.createdAt in startCutoff..endCutoff && it.paymentStatus == PaymentStatus.VOID }`
    - `periodCashMovements: List<CashMovement> get() = cashMovements.filter { it.createdAt in startCutoff..endCutoff }`
- In `ReportViewModel.kt`:
  - Add `fun onDatePresetChange(preset: DateFilterPreset)`
  - Add `fun onCustomDateRangeSelected(startMillis: Long, endMillis: Long)`
  - Add `fun onShowDatePickerDialog(show: Boolean)`
  - Update CSV export and Print receipt to use `dateFilter.getDisplayLabel()`.

- [ ] **Step 4: Update `ReportScreen.kt`**

- Replace static period chip row with `DateFilterBar` and `DateRangePickerDialog`.

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.kapos.mobile.feature.report.ReportDateFilterTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/kapos/mobile/feature/report/ app/src/test/java/com/kapos/mobile/feature/report/ReportDateFilterTest.kt
git commit -m "feat(report): integrate custom date filter and update financial aggregations"
```

---

### Task 5: Integrasi Custom Date ke Analisa BEP

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/feature/analytics/AnalyticsUiState.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/analytics/AnalyticsViewModel.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/analytics/AnalyticsScreen.kt`

**Interfaces:**
- Consumes: `DateRangeFilter`, `DateFilterPreset`, `DateFilterBar`, `DateRangePickerDialog`.
- Produces: Dynamic BEP calculations across custom date ranges.

- [ ] **Step 1: Update `AnalyticsUiState.kt` and `AnalyticsViewModel.kt`**

- In `AnalyticsUiState.kt`:
  - Add `val dateFilter: DateRangeFilter = DateRangeFilter(preset = DateFilterPreset.TODAY)`
  - Add `val showDatePickerDialog: Boolean = false`
- In `AnalyticsViewModel.kt`:
  - Recalculate summary metrics when dateFilter changes:
    - Pass `(startCutoff, endCutoff)` into calculation logic.
  - Add `onDatePresetChange`, `onCustomDateRangeSelected`, `onShowDatePickerDialog`.

- [ ] **Step 2: Update `AnalyticsScreen.kt`**

- Replace hardcoded `AnalyticsPeriod` chips with `DateFilterBar` and dialog.

- [ ] **Step 3: Run unit tests**

Run: `./gradlew testDebugUnitTest`
Expected: ALL PASS

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/kapos/mobile/feature/analytics/
git commit -m "feat(analytics): integrate custom date filter into BEP analysis"
```

---

### Task 6: Full Verification & Build Check

**Files:**
- All modified and created files across tasks 1-5.

- [ ] **Step 1: Run full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all unit tests pass.

- [ ] **Step 2: Run assembleDebug to ensure clean build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Final commit & merge check**

```bash
git status
```
Ensure working tree is clean.
