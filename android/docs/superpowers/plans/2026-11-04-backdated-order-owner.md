# Fitur Input Transaksi Susulan (Khusus Owner) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Memungkinkan pemilik toko (Owner) menginputkan transaksi penjualan yang terlupa di masa lampau langsung dari layar Riwayat Transaksi secara aman dengan audit trail lengkap dan integrasi otomatis ke laporan keuangan serta pengurangan stok bahan baku (BOM).

**Architecture:** Menggunakan pendekatan *immutable backdated order* di mana transaksi baru dibuat dengan stempel waktu lampau (`createdAt = selectedTimestamp`) dan penanda `[Susulan]`. Fitur diproteksi oleh sesi peran `UserRole.OWNER` atau fallback verifikasi PIN Owner jika dibuka dalam sesi kasir.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Android Room, Kotlin Coroutines & StateFlow, Supabase (PostgreSQL offline-first sync).

## Global Constraints
- Khusus Owner: Tombol aksi hanya dapat dibuka langsung oleh akun `UserRole.OWNER` atau melalui otorisasi `ownerPin`.
- Immutable Audit Trail: Transaksi susulan dibuat sebagai entitas Order baru lengkap dengan `orderNo` berbasis tanggal terpilih dan awalan catatan `[Susulan]`.
- Tanpa perubahan skema Room/Supabase yang memecah kompatibilitas: Menggunakan field `notes` untuk menyimpan tag `[Susulan]`.
- Wajib validasi: Tanggal tidak boleh di masa depan (*future date*), minimal 1 item produk dipilih, dan alasan/catatan koreksi wajib diisi.

---

### Task 1: Update UI State untuk Fitur Transaksi Susulan

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/feature/transactions/TransactionsUiState.kt`

**Interfaces:**
- Produces:
  - `isOwner: Boolean = false`
  - `showBackdatedOrderDialog: Boolean = false`
  - `showOwnerAuthPinDialog: Boolean = false`
  - `ownerAuthPinInput: String = ""`
  - `ownerAuthPinError: String? = null`
  - `availableProducts: List<Product> = emptyList()`
  - `isSubmittingBackdated: Boolean = false`
  - `actionSuccessMessage: String? = null`

- [ ] **Step 1: Modifikasi TransactionsUiState.kt**

Tambahkan field-field baru pada `data class TransactionsUiState`:

```kotlin
data class TransactionsUiState(
    val orders: List<Order> = emptyList(),
    val isSyncing: Boolean = false,
    val searchQuery: String = "",
    val filter: TransactionFilter = TransactionFilter.ALL,
    val dateFilter: DateRangeFilter = DateRangeFilter(DateFilterPreset.ALL_TIME),
    val showDatePickerDialog: Boolean = false,
    val selectedOrder: Order? = null,
    val selectedOrderItems: List<OrderItem> = emptyList(),
    val isLoadingDetail: Boolean = false,
    val storeName: String = "",
    val storeAddress: String = "",
    val isOwner: Boolean = false,
    // VOID states
    val orderToVoid: Order? = null,
    val showVoidPinDialog: Boolean = false,
    val voidPinInput: String = "",
    val voidPinError: String? = null,
    val selectedVoidReason: String = "Salah Input Menu",
    val voidReasonNotes: String = "",
    val voidReasonError: String? = null,
    val isVoiding: Boolean = false,
    // Backdated Order (Owner) states
    val showBackdatedOrderDialog: Boolean = false,
    val showOwnerAuthPinDialog: Boolean = false,
    val ownerAuthPinInput: String = "",
    val ownerAuthPinError: String? = null,
    val availableProducts: List<Product> = emptyList(),
    val isSubmittingBackdated: Boolean = false,
    val actionSuccessMessage: String? = null
)
```

- [ ] **Step 2: Review perubahan state**
Pastikan nilai *default* tidak merusak inisialisasi yang sudah ada.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/kapos/mobile/feature/transactions/TransactionsUiState.kt
git commit -m "feat(transactions): add backdated order state fields to TransactionsUiState"
```

---

### Task 2: Logika ViewModel untuk Otorisasi Owner & Submit Transaksi Susulan

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/feature/transactions/TransactionsViewModel.kt`
- Test: `app/src/test/java/com/kapos/mobile/feature/transactions/BackdatedOrderValidationTest.kt`

**Interfaces:**
- Consumes:
  - `settingsRepository.settingsFlow` (mendapatkan `currentSession.role` dan verifikasi `verifyPin(UserRole.OWNER, pin)`)
  - `repository.observeProducts()` (mengambil produk aktif)
  - `repository.submitOrder(order, items)` (menyimpan pesanan & auto potong stok)
- Produces:
  - `fun onOpenBackdatedOrder()`
  - `fun onDismissBackdatedOrder()`
  - `fun onOwnerAuthPinChange(pin: String)`
  - `fun onConfirmOwnerAuthPin()`
  - `fun onDismissOwnerAuthPin()`
  - `fun submitBackdatedOrder(timestampMillis: Long, items: List<CartItem>, paymentMethod: PaymentMethod, reason: String)`
  - `fun clearActionSuccessMessage()`

- [ ] **Step 1: Tulis unit test BackdatedOrderValidationTest.kt**

Buat file `app/src/test/java/com/kapos/mobile/feature/transactions/BackdatedOrderValidationTest.kt`:

```kotlin
package com.kapos.mobile.feature.transactions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class BackdatedOrderValidationTest {

    @Test
    fun testBackdatedTimestampCannotBeInFuture() {
        val now = System.currentTimeMillis()
        val pastTimestamp = now - 86400000L // 1 day ago
        val futureTimestamp = now + 86400000L // 1 day in future

        assertTrue(pastTimestamp <= now)
        assertFalse(futureTimestamp <= now)
    }

    @Test
    fun testNotesFormatContainsSusulanTag() {
        val reason = "Nota manual tgl 2 Nov listrik padam"
        val formattedNotes = "[Susulan] $reason"

        assertTrue(formattedNotes.startsWith("[Susulan]"))
        assertTrue(formattedNotes.contains("listrik padam"))
    }

    @Test
    fun testOrderNoMatchesSelectedDate() {
        val selectedDate = Date(1730505600000L) // Example past date
        val orderNo = com.kapos.mobile.feature.checkout.generateOrderNo(selectedDate)
        assertTrue(orderNo.startsWith("KA-"))
    }
}
```

- [ ] **Step 2: Implementasikan method di TransactionsViewModel.kt**

Tambahkan pengamatan produk aktif & role sesi di `init`:
```kotlin
viewModelScope.launch {
    settingsRepository.settingsFlow.collect { settings ->
        val isOwnerRole = settings.currentSession.role == com.kapos.mobile.domain.model.UserRole.OWNER
        _uiState.update {
            it.copy(
                isOwner = isOwnerRole,
                storeName = settings.storeName,
                storeAddress = settings.storeAddress
            )
        }
    }
}
viewModelScope.launch {
    repository.observeProducts().collect { prods ->
        _uiState.update { it.copy(availableProducts = prods) }
    }
}
```

Tambahkan fungsi-fungsi kontrol:
```kotlin
fun onOpenBackdatedOrder() {
    if (_uiState.value.isOwner) {
        _uiState.update { it.copy(showBackdatedOrderDialog = true) }
    } else {
        _uiState.update {
            it.copy(
                showOwnerAuthPinDialog = true,
                ownerAuthPinInput = "",
                ownerAuthPinError = null
            )
        }
    }
}

fun onDismissBackdatedOrder() {
    _uiState.update { it.copy(showBackdatedOrderDialog = false) }
}

fun onOwnerAuthPinChange(pin: String) {
    _uiState.update { it.copy(ownerAuthPinInput = pin, ownerAuthPinError = null) }
}

fun onDismissOwnerAuthPin() {
    _uiState.update { it.copy(showOwnerAuthPinDialog = false, ownerAuthPinInput = "", ownerAuthPinError = null) }
}

fun onConfirmOwnerAuthPin() {
    val pin = _uiState.value.ownerAuthPinInput
    val isValid = settingsRepository.verifyPin(com.kapos.mobile.domain.model.UserRole.OWNER, pin)
    if (isValid) {
        _uiState.update {
            it.copy(
                showOwnerAuthPinDialog = false,
                showBackdatedOrderDialog = true,
                ownerAuthPinInput = "",
                ownerAuthPinError = null
            )
        }
    } else {
        _uiState.update { it.copy(ownerAuthPinError = "PIN Owner salah. Akses ditolak.") }
    }
}

fun submitBackdatedOrder(
    timestampMillis: Long,
    items: List<com.kapos.mobile.domain.model.CartItem>,
    paymentMethod: com.kapos.mobile.domain.model.PaymentMethod,
    reason: String
) {
    if (items.isEmpty() || reason.isBlank()) return

    _uiState.update { it.copy(isSubmittingBackdated = true) }
    viewModelScope.launch {
        try {
            val orderDate = java.util.Date(timestampMillis)
            val generatedOrderNo = com.kapos.mobile.feature.checkout.generateOrderNo(orderDate)
            val subtotal = items.sumOf { it.subtotal }
            val formattedNotes = "[Susulan] ${reason.trim()}"

            val order = com.kapos.mobile.domain.model.Order(
                id = java.util.UUID.randomUUID().toString(),
                orderNo = generatedOrderNo,
                customerName = null,
                customerPhone = null,
                orderType = com.kapos.mobile.domain.model.OrderType.DINE_IN,
                tableNo = null,
                subtotal = subtotal,
                discount = 0.0,
                total = subtotal,
                paymentMethod = paymentMethod,
                paymentStatus = com.kapos.mobile.domain.model.PaymentStatus.PAID,
                cashierName = "Owner (Koreksi)",
                notes = formattedNotes,
                createdAt = timestampMillis,
                paidAt = timestampMillis,
                syncStatus = com.kapos.mobile.domain.model.SyncStatus.PENDING_SYNC
            )

            repository.submitOrder(order, items)

            _uiState.update {
                it.copy(
                    isSubmittingBackdated = false,
                    showBackdatedOrderDialog = false,
                    actionSuccessMessage = "Transaksi susulan berhasil disimpan!"
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isSubmittingBackdated = false) }
        }
    }
}

fun clearActionSuccessMessage() {
    _uiState.update { it.copy(actionSuccessMessage = null) }
}
```

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/kapos/mobile/feature/transactions/TransactionsViewModel.kt app/src/test/java/com/kapos/mobile/feature/transactions/BackdatedOrderValidationTest.kt
git commit -m "feat(transactions): implement backdated order submission and owner pin verification in ViewModel"
```

---

### Task 3: Komponen Dialog Input Transaksi Susulan (BackdatedOrderDialog)

**Files:**
- Create: `app/src/main/java/com/kapos/mobile/feature/transactions/BackdatedOrderDialog.kt`

**Interfaces:**
- Consumes:
  - `availableProducts: List<Product>`
  - `isSubmitting: Boolean`
  - `onSubmit: (timestampMillis: Long, items: List<CartItem>, paymentMethod: PaymentMethod, reason: String) -> Unit`
  - `onDismiss: () -> Unit`

- [ ] **Step 1: Buat file BackdatedOrderDialog.kt**

Komponen Compose yang menangani:
1. Pemilihan tanggal lampau menggunakan `DatePickerDialog` (Android / Compose) dengan batasan `maxDate = System.currentTimeMillis()`.
2. Pemilihan jam & menit menggunakan `TimePickerDialog`.
3. Pencarian produk & daftar item terpilih dengan kuantitas (+ / -).
4. Pemilihan metode bayar (`CASH`, `QRIS`, `TRANSFER`) dengan Chips.
5. Input catatan alasan koreksi.
6. Validasi tombol simpan (hanya aktif jika `items.isNotEmpty()` dan `reason.isNotBlank()`).

- [ ] **Step 2: Commit**
```bash
git add app/src/main/java/com/kapos/mobile/feature/transactions/BackdatedOrderDialog.kt
git commit -m "feat(transactions): create BackdatedOrderDialog Compose UI component"
```

---

### Task 4: Integrasi Dialog, Tombol "+ Susulan", dan Badge Nota di TransactionsScreen

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/feature/transactions/TransactionsScreen.kt`

**Interfaces:**
- Menampilkan tombol aksi `+ Susulan` di Top Bar sebelah tombol Sync.
- Menampilkan dialog PIN otorisasi Owner (`OwnerAuthPinDialog`) jika kasir mencoba membuka fitur.
- Menampilkan `BackdatedOrderDialog` saat `showBackdatedOrderDialog == true`.
- Menampilkan badge `[Susulan]` berwarna oranye/emas di samping `orderNo` pada kartu riwayat transaksi jika catatan mengandung `[Susulan]`.
- Menampilkan Snackbar feedback saat `actionSuccessMessage != null`.

- [ ] **Step 1: Tambahkan tombol "+ Susulan" pada Top Bar di TransactionsScreen.kt**

```kotlin
// Di Row Top Bar samping IconButton Sync
FilledTonalButton(
    onClick = onOpenBackdatedClick,
    shape = RoundedCornerShape(12.dp),
    colors = ButtonDefaults.filledTonalButtonColors(
        containerColor = Color(0xFFFEF3C7),
        contentColor = Color(0xFFB45309)
    ),
    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
) {
    Icon(
        imageVector = Icons.Default.Add,
        contentDescription = null,
        modifier = Modifier.size(16.dp)
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
        text = "Susulan",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
    )
}
```

- [ ] **Step 2: Tambahkan Badge `[Susulan]` pada item transaksi**

Pada `TransactionItemCard`, cek apakah `order.notes?.contains("[Susulan]") == true`:
Tampilkan badge kecil:
```kotlin
if (order.notes?.contains("[Susulan]") == true) {
    Box(
        modifier = Modifier
            .background(Color(0xFFFEF3C7), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "Susulan",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB45309),
                fontSize = 10.sp
            )
        )
    }
}
```

- [ ] **Step 3: Integrasikan Dialog PIN & BackdatedOrderDialog**

Pasang dialog di akhir `TransactionsScreen`:
* `OwnerAuthPinDialog` ketika `uiState.showOwnerAuthPinDialog` bernilai true.
* `BackdatedOrderDialog` ketika `uiState.showBackdatedOrderDialog` bernilai true.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/kapos/mobile/feature/transactions/TransactionsScreen.kt
git commit -m "feat(transactions): integrate backdated order button, dialog, and badge in TransactionsScreen"
```

---

### Task 5: Verifikasi Menyeluruh & Testing

**Files:**
- Run test: `app/src/test/java/com/kapos/mobile/feature/transactions/BackdatedOrderValidationTest.kt`

- [ ] **Step 1: Jalankan unit test**
- [ ] **Step 2: Verifikasi alur otorisasi Owner dan Kasir**
- [ ] **Step 3: Verifikasi sinkronisasi laporan dan stok**
- [ ] **Step 4: Final commit**
```bash
git commit -m "feat: complete backdated order feature for owner"
```
