# KA POS Android — Fase 1 (Operasional Kasir & Bluetooth Printer ESC/POS) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Membangun fitur operasional kasir utama di Android Native (Cetak Struk Bluetooth ESC/POS 58mm/80mm, Kustomisasi Level Pedas & Catatan Item, Diskon Pesanan Nominal & Persen, serta Tahan/Buka Pesanan Meja).

**Architecture:** Menerapkan Clean Architecture berbasis Kotlin Coroutines & Flows di Jetpack Compose Material 3: `EscPosBuilder` untuk formatting byte printer murni, `BluetoothPrinterService` untuk koneksi RFCOMM SPP, Room Database (versi 2) untuk tabel `saved_orders` & `saved_order_items`, serta ViewModel state-driven UI.

**Tech Stack:** Kotlin 1.9+, Android SDK 34 (Min SDK 26), Jetpack Compose BOM 2024.02.01 (Material 3), Room DB 2.6.1, OkHttp 4.12.0, Android Bluetooth RFCOMM SPP.

## Global Constraints
- Target SDK 34, Min SDK 26 (Android 8.0+).
- Room database migration menggunakan `fallbackToDestructiveMigration()` untuk update schema versi lokal yang mulus.
- Desain UI wajib mematuhi tema `compose-fintech-pos-ui` (Soft Teal Glass, rounded cards 16-24dp, token warna Teal `#0B5C73`, background gradient `#F8FBFB`).
- Seluruh teks dan format mata uang menggunakan helper terpusat `CurrencyFormatter.formatRupiah()`.
- Tidak ada placeholder, TODO, atau hardcoded string acak.

---

### Task 1: Bluetooth Permissions & ESC/POS Byte Builder

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/kapos/mobile/core/printer/EscPosBuilder.kt`
- Create: `app/src/test/java/com/kapos/mobile/core/printer/EscPosBuilderTest.kt`

**Interfaces:**
- Consumes: `Order`, `OrderItem`, `AppSettings`
- Produces:
  ```kotlin
  class EscPosBuilder(val paperWidth: Int = 58) {
      fun buildReceipt(
          storeName: String,
          address: String,
          phone: String,
          receiptFooter: String,
          orderNo: String,
          dateStr: String,
          orderType: String,
          tableNo: String?,
          cashierName: String,
          customerName: String?,
          items: List<EscPosItem>,
          subtotal: Double,
          discount: Double,
          total: Double,
          paymentMethod: String,
          cashGiven: Double,
          change: Double
      ): ByteArray
  }
  ```

- [ ] **Step 1: Write the failing unit test for EscPosBuilder**

```kotlin
// app/src/test/java/com/kapos/mobile/core/printer/EscPosBuilderTest.kt
package com.kapos.mobile.core.printer

import org.junit.Assert.assertTrue
import org.junit.Test

class EscPosBuilderTest {

    @Test
    fun testBuildReceiptReturnsValidEscPosBytes() {
        val builder = EscPosBuilder(paperWidth = 58)
        val items = listOf(
            EscPosItem(
                name = "Taichan Pedas Lv.3",
                price = 18000.0,
                quantity = 2,
                spiceLevel = "Lv.3",
                notes = "sambal dipisah",
                subtotal = 36000.0
            )
        )

        val bytes = builder.buildReceipt(
            storeName = "Taichan & Chicken KA",
            address = "Bandar Lampung",
            phone = "081234567890",
            receiptFooter = "Terima kasih!",
            orderNo = "KA-20261015-001",
            dateStr = "15 Okt 2026, 14:30",
            orderType = "Dine-In",
            tableNo = "05",
            cashierName = "Kasir 1",
            customerName = "Budi",
            items = items,
            subtotal = 36000.0,
            discount = 5000.0,
            total = 31000.0,
            paymentMethod = "Tunai",
            cashGiven = 50000.0,
            change = 19000.0
        )

        assertTrue(bytes.isNotEmpty())
        // Inisialisasi ESC @ (0x1B, 0x40)
        assertTrue(bytes[0] == 0x1B.toByte() && bytes[1] == 0x40.toByte())
        // Mengandung teks nama toko
        val content = String(bytes, Charsets.ISO_8859_1)
        assertTrue(content.contains("Taichan & Chicken KA"))
        assertTrue(content.contains("KA-20261015-001"))
        assertTrue(content.contains("Rp 31.000") || content.contains("31.000"))
    }
}
```

- [ ] **Step 2: Add Bluetooth permissions to AndroidManifest.xml**

Tambahkan permission Bluetooth ke `app/src/main/AndroidManifest.xml`:
```xml
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" />
```

- [ ] **Step 3: Implement EscPosBuilder.kt**

```kotlin
// app/src/main/java/com/kapos/mobile/core/printer/EscPosBuilder.kt
package com.kapos.mobile.core.printer

import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class EscPosItem(
    val name: String,
    val price: Double,
    val quantity: Int,
    val spiceLevel: String = "",
    val notes: String = "",
    val subtotal: Double
)

class EscPosBuilder(private val paperWidth: Int = 58) {

    private val maxCols: Int = if (paperWidth >= 80) 48 else 32

    private val df: DecimalFormat = DecimalFormat("#,###", DecimalFormatSymbols(Locale("id", "ID")))

    private fun formatRp(amount: Double): String = "Rp ${df.format(amount)}"

    fun buildReceipt(
        storeName: String,
        address: String,
        phone: String,
        receiptFooter: String,
        orderNo: String,
        dateStr: String,
        orderType: String,
        tableNo: String?,
        cashierName: String,
        customerName: String?,
        items: List<EscPosItem>,
        subtotal: Double,
        discount: Double,
        total: Double,
        paymentMethod: String,
        cashGiven: Double,
        change: Double
    ): ByteArray {
        val out = ByteArrayOutputStream()

        fun write(vararg b: Int) = b.forEach { out.write(it) }
        fun writeBytes(bytes: ByteArray) = out.write(bytes)
        fun writeText(text: String) = out.write(text.toByteArray(Charsets.ISO_8859_1))
        fun line(text: String = "") {
            writeText(text)
            write(0x0A)
        }
        fun divider() = line("-".repeat(maxCols))

        fun twoCols(left: String, right: String): String {
            val space = maxCols - left.length - right.length
            return if (space > 0) left + " ".repeat(space) + right else "$left $right"
        }

        // ESC @ -> Init
        write(0x1B, 0x40)

        // Center Align & Double Size for Store Name
        write(0x1B, 0x61, 0x01) // Center
        write(0x1B, 0x45, 0x01) // Bold ON
        write(0x1D, 0x21, 0x11) // Double width & height
        line(storeName)

        // Normal Size
        write(0x1D, 0x21, 0x00)
        write(0x1B, 0x45, 0x00) // Bold OFF
        if (address.isNotBlank()) line(address)
        if (phone.isNotBlank()) line(phone)
        divider()

        // Metadata (Left Align)
        write(0x1B, 0x61, 0x00)
        line(twoCols("No. Nota", orderNo))
        line(twoCols("Waktu", dateStr))
        line(twoCols("Tipe", orderType + if (!tableNo.isNullOrBlank()) " (Meja $tableNo)" else ""))
        if (!customerName.isNullOrBlank()) line(twoCols("Pelanggan", customerName))
        line(twoCols("Kasir", cashierName))
        divider()

        // Items List
        items.forEach { item ->
            line(item.name)
            val subDetails = mutableListOf<String>()
            if (item.spiceLevel.isNotBlank()) subDetails.add(item.spiceLevel)
            if (item.notes.isNotBlank()) subDetails.add(item.notes)
            if (subDetails.isNotEmpty()) {
                line("  • " + subDetails.joinToString(" - "))
            }
            val qtyPrice = "${item.quantity}x @ ${formatRp(item.price)}"
            val subtotalStr = formatRp(item.subtotal)
            line(twoCols("  $qtyPrice", subtotalStr))
        }
        divider()

        // Totals
        line(twoCols("Subtotal", formatRp(subtotal)))
        if (discount > 0) {
            line(twoCols("Diskon", "- ${formatRp(discount)}"))
        }
        write(0x1B, 0x45, 0x01) // Bold
        line(twoCols("TOTAL", formatRp(total)))
        write(0x1B, 0x45, 0x00)
        line(twoCols("Metode Bayar", paymentMethod))
        if (paymentMethod.equals("Tunai", ignoreCase = true) || paymentMethod.equals("CASH", ignoreCase = true)) {
            line(twoCols("Bayar Tunai", formatRp(cashGiven)))
            line(twoCols("Kembalian", formatRp(change)))
        }
        divider()

        // Footer (Center)
        write(0x1B, 0x61, 0x01)
        line(receiptFooter)
        line("")
        line("www.taichanchickenka.com")
        line("")
        line("")

        // Paper Feed & Cut
        write(0x1D, 0x56, 0x42, 0x00)

        return out.toByteArray()
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd /media/lian/Ubuntu/POS\ KA/android && ./gradlew testDebugUnitTest --tests com.kapos.mobile.core.printer.EscPosBuilderTest`
Expected: BUILD SUCCESSFUL, test passed.

- [ ] **Step 5: Commit Task 1**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/com/kapos/mobile/core/printer/EscPosBuilder.kt app/src/test/java/com/kapos/mobile/core/printer/EscPosBuilderTest.kt
git commit -m "feat(printer): add Bluetooth permissions and EscPosBuilder with unit test"
```

---

### Task 2: BluetoothPrinterService & Settings Integration

**Files:**
- Create: `app/src/main/java/com/kapos/mobile/core/printer/BluetoothPrinterService.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/repository/SettingsRepository.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/settings/SettingsUiState.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/kapos/mobile/KaPosApplication.kt`

**Interfaces:**
- Consumes: `EscPosBuilder`, Android `BluetoothAdapter`
- Produces:
  ```kotlin
  class BluetoothPrinterService(val context: Context) {
      fun getPairedDevices(): List<BluetoothDeviceInfo>
      suspend fun printBytes(macAddress: String, data: ByteArray): Result<Unit>
  }
  ```

- [ ] **Step 1: Implement BluetoothPrinterService.kt**

```kotlin
// app/src/main/java/com/kapos/mobile/core/printer/BluetoothPrinterService.kt
package com.kapos.mobile.core.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

data class BluetoothDeviceInfo(
    val name: String,
    val address: String
)

class BluetoothPrinterService(private val context: Context) {

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        manager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDeviceInfo> {
        val adapter = bluetoothAdapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        return try {
            adapter.bondedDevices.map { device ->
                BluetoothDeviceInfo(
                    name = device.name ?: "Unknown Device",
                    address = device.address
                )
            }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun printBytes(macAddress: String, data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        val adapter = bluetoothAdapter ?: return@withContext Result.failure(Exception("Bluetooth tidak didukung pada perangkat ini"))
        if (!adapter.isEnabled) return@withContext Result.failure(Exception("Bluetooth belum aktif. Silakan hidupkan Bluetooth"))

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(macAddress)
            adapter.cancelDiscovery()

            socket = device.createRfcommSocketToServiceRecord(sppUuid)
            socket.connect()

            outputStream = socket.outputStream
            outputStream.write(data)
            outputStream.flush()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal mencetak: ${e.localizedMessage ?: "Koneksi printer terputus"}"))
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
        }
    }
}
```

- [ ] **Step 2: Update AppSettings & SettingsRepository**

Tambahkan field printer ke `AppSettings`:
```kotlin
data class AppSettings(
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = "",
    val storeName: String = "Taichan & Chicken KA",
    val storeAddress: String = "Bandar Lampung",
    val storePhone: String = "081234567890",
    val cashierName: String = "Kasir Utama",
    val receiptFooter: String = "Terima kasih atas kunjungannya! 🍢",
    val printerMacAddress: String = "",
    val printerName: String = "",
    val paperWidth: Int = 58,
    val autoPrintCheckout: Boolean = true
)
```
Perbarui `loadSettings` & `saveSettings` di `SettingsRepository.kt` untuk menyimpan preferences tersebut.

- [ ] **Step 3: Update SettingsViewModel & SettingsUiState**

Tambahkan field state printer, fungsi `loadPairedPrinters()`, `onSelectPrinter(name, address)`, `onPaperWidthChange(width)`, `onAutoPrintChange(enabled)`, dan fungsi `testPrintReceipt()`.
`testPrintReceipt()` memanggil `EscPosBuilder.buildReceipt()` dengan sample order lalu mencetak via `bluetoothPrinterService.printBytes()`.

- [ ] **Step 4: Update SettingsScreen UI**

Tambahkan section "Pengaturan Printer Thermal (Bluetooth)" di `SettingsScreen.kt`:
- Tombol refresh list printer paired.
- Dropdown / list radio printer Bluetooth.
- Pilihan lebar kertas 58mm / 80mm.
- Switch Auto-print saat transaksi berhasil.
- Tombol "Tes Cetak Struk".

- [ ] **Step 5: Bind in KaPosApplication.kt**

Inisialisasi `bluetoothPrinterService = BluetoothPrinterService(this)` di `KaPosApplication.kt` dan inject ke `SettingsViewModel` dan `CheckoutViewModel`.

- [ ] **Step 6: Run tests and compile verification**

Run: `cd /media/lian/Ubuntu/POS\ KA/android && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit Task 2**

```bash
git add app/src/main/java/com/kapos/mobile/core/printer/BluetoothPrinterService.kt app/src/main/java/com/kapos/mobile/core/data/repository/SettingsRepository.kt app/src/main/java/com/kapos/mobile/feature/settings/ app/src/main/java/com/kapos/mobile/KaPosApplication.kt
git commit -m "feat(settings): integrate Bluetooth printer pairing and test print"
```

---

### Task 3: Kustomisasi Menu di POS (Modifier Dialog)

**Files:**
- Create: `app/src/main/java/com/kapos/mobile/feature/pos/ModifierDialog.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/pos/PosUiState.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/pos/PosViewModel.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/pos/PosScreen.kt`

**Interfaces:**
- Consumes: `Product`, `PosViewModel`
- Produces: `ModifierDialog(product, onDismiss, onAddToCart)`

- [ ] **Step 1: Create ModifierDialog.kt**

```kotlin
// app/src/main/java/com/kapos/mobile/feature/pos/ModifierDialog.kt
package com.kapos.mobile.feature.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kapos.mobile.core.components.PrimaryButton
import com.kapos.mobile.core.theme.*
import com.kapos.mobile.core.utils.CurrencyFormatter
import com.kapos.mobile.domain.model.Product

@Composable
fun ModifierDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirmAddToCart: (product: Product, quantity: Int, spiceLevel: String, notes: String) -> Unit
) {
    val spiceOptions = listOf("Original", "Lv.1", "Lv.2", "Lv.3", "Lv.4", "Lv.5")
    var selectedSpice by remember { mutableStateOf(if (product.spiceLevel.isNotBlank()) product.spiceLevel else "Original") }
    var notes by remember { mutableStateOf("") }
    var quantity by remember { mutableIntStateOf(1) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.Lg)
                .background(SurfaceWhite)
                .border(1.dp, OutlineSoft, AppShapes.Lg)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = product.emoji, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = CurrencyFormatter.formatRupiah(product.price),
                                style = MaterialTheme.typography.bodyMedium.copy(color = Primary, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pilihan Level Pedas
                Text(
                    text = "Level Pedas / Varian",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    spiceOptions.take(3).forEach { option ->
                        SpiceChip(
                            text = option,
                            isSelected = selectedSpice == option,
                            onClick = { selectedSpice = option },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    spiceOptions.drop(3).forEach { option ->
                        SpiceChip(
                            text = option,
                            isSelected = selectedSpice == option,
                            onClick = { selectedSpice = option },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Catatan Khusus
                Text(
                    text = "Catatan Khusus",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Contoh: sambal dipisah, goreng garing...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = AppShapes.Sm
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quantity & Submit Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(SurfaceTint)
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Minus", tint = TextPrimary)
                        }
                        Text(text = "$quantity", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(
                            onClick = { quantity++ },
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(Primary)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Plus", tint = SurfaceWhite)
                        }
                    }

                    PrimaryButton(
                        text = "+ Keranjang (${CurrencyFormatter.formatRupiah(product.price * quantity)})",
                        onClick = {
                            onConfirmAddToCart(product, quantity, selectedSpice, notes)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SpiceChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(AppShapes.Sm)
            .background(if (isSelected) Primary else SurfaceTint)
            .border(1.dp, if (isSelected) Primary else OutlineSoft, AppShapes.Sm)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) SurfaceWhite else TextPrimary
            )
        )
    }
}
```

- [ ] **Step 2: Update PosUiState & PosViewModel**

Tambahkan `activeModifierProduct: Product? = null` ke `PosUiState`.
Saat produk diklik di `PosViewModel.onProductClick(product)`:
- Jika produk merupakan kategori Taichan/Chicken atau memiliki nama pedas / deskripsi pedas: set `_uiState.update { it.copy(activeModifierProduct = product) }`.
- Jika bukan (minuman/nasi): langsung tambahkan ke keranjang.
Tambahkan fungsi `dismissModifier()` dan `confirmModifierAddToCart(product, qty, spice, notes)`.

- [ ] **Step 3: Update PosScreen to display ModifierDialog**

Di `PosScreen.kt`, jika `uiState.activeModifierProduct != null`, tampilkan `ModifierDialog(product = uiState.activeModifierProduct, ...)`.

- [ ] **Step 4: Verify Compilation**

Run: `cd /media/lian/Ubuntu/POS\ KA/android && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit Task 3**

```bash
git add app/src/main/java/com/kapos/mobile/feature/pos/
git commit -m "feat(pos): add ModifierDialog for spicy levels and custom item notes"
```

---

### Task 4: Diskon Pesanan & Cetak Otomatis di Checkout

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/domain/model/Models.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/checkout/CheckoutUiState.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/checkout/CheckoutViewModel.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/checkout/ConfirmSaleScreen.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/cart/CartBottomSheet.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/transactions/OrderDetailDialog.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/transactions/TransactionsViewModel.kt`

**Interfaces:**
- Consumes: `CheckoutViewModel`, `BluetoothPrinterService`, `EscPosBuilder`
- Produces: `CheckoutUiState.discount`, `CheckoutUiState.discountType`, Auto-print on checkout completion, Physical print on OrderDetailDialog.

- [ ] **Step 1: Update Discount Logic in CheckoutUiState & CheckoutViewModel**

Enum tipe diskon:
```kotlin
enum class DiscountType(val displayName: String) {
    NOMINAL("Rp"),
    PERCENT("%")
}
```
Tambahkan `discountType`, `discountInput: Double` ke `CheckoutUiState`.
Hitung kalkulasi `subtotal`, `discount`, `total = maxOf(0.0, subtotal - discount)`.
Tambahkan fungsi `onDiscountChange(amount: Double, type: DiscountType)`.

- [ ] **Step 2: Add Discount Input to CartBottomSheet & ConfirmSaleScreen**

Tambahkan input baris Diskon di `CartBottomSheet.kt` dan `ConfirmSaleScreen.kt` dengan toggle switch `Rp / %` dan input nominal.

- [ ] **Step 3: Auto-print receipt in CheckoutViewModel.submitOrder()**

Saat order berhasil disubmit ke Room, jika `settingsRepository.settingsFlow.value.autoPrintCheckout` aktif dan `printerMacAddress.isNotBlank()`:
Buat byte ESC/POS via `EscPosBuilder` dan panggil `bluetoothPrinterService.printBytes()`.

- [ ] **Step 4: Connect OrderDetailDialog "Cetak" button to BluetoothPrinterService**

Di `TransactionsViewModel.kt`, tambahkan fungsi `printOrderReceipt(order: Order, items: List<OrderItem>)`.
Di `OrderDetailDialog.kt`, hubungkan tombol "Cetak" ke fungsi tersebut.

- [ ] **Step 5: Compile & Verify**

Run: `cd /media/lian/Ubuntu/POS\ KA/android && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit Task 4**

```bash
git add app/src/main/java/com/kapos/mobile/domain/model/ app/src/main/java/com/kapos/mobile/feature/checkout/ app/src/main/java/com/kapos/mobile/feature/cart/ app/src/main/java/com/kapos/mobile/feature/transactions/
git commit -m "feat(checkout): add order discounts and print receipt integration"
```

---

### Task 5: Tahan & Buka Pesanan (Hold / Save Order)

**Files:**
- Create: `app/src/main/java/com/kapos/mobile/core/data/local/entity/SavedOrderEntity.kt`
- Create: `app/src/main/java/com/kapos/mobile/core/data/local/dao/SavedOrderDao.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/local/AppDatabase.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/repository/PosRepository.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/pos/PosUiState.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/pos/PosViewModel.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/pos/PosScreen.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/cart/CartBottomSheet.kt`

**Interfaces:**
- Consumes: Room Database, `PosRepository`
- Produces: `saveCurrentOrder(tableNo, customerName, items)`, `observeSavedOrders()`, `recallSavedOrder(savedOrderId)`

- [ ] **Step 1: Create SavedOrderEntity & SavedOrderDao**

```kotlin
// app/src/main/java/com/kapos/mobile/core/data/local/entity/SavedOrderEntity.kt
package com.kapos.mobile.core.data.local.entity

import androidx.room.*

@Entity(tableName = "saved_orders")
data class SavedOrderEntity(
    @PrimaryKey val id: String,
    val tableNo: String?,
    val customerName: String?,
    val orderType: String,
    val subtotal: Double,
    val discount: Double,
    val total: Double,
    val notes: String?,
    val createdAt: Long
)

@Entity(
    tableName = "saved_order_items",
    foreignKeys = [
        ForeignKey(
            entity = SavedOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["savedOrderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("savedOrderId")]
)
data class SavedOrderItemEntity(
    @PrimaryKey val id: String,
    val savedOrderId: String,
    val productId: String,
    val productName: String,
    val price: Double,
    val quantity: Int,
    val spiceLevel: String,
    val notes: String,
    val subtotal: Double
)
```

```kotlin
// app/src/main/java/com/kapos/mobile/core/data/local/dao/SavedOrderDao.kt
package com.kapos.mobile.core.data.local.dao

import androidx.room.*
import com.kapos.mobile.core.data.local.entity.SavedOrderEntity
import com.kapos.mobile.core.data.local.entity.SavedOrderItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedOrderDao {
    @Query("SELECT * FROM saved_orders ORDER BY createdAt DESC")
    fun observeAllSavedOrders(): Flow<List<SavedOrderEntity>>

    @Query("SELECT * FROM saved_order_items WHERE savedOrderId = :orderId")
    suspend fun getItemsForSavedOrder(orderId: String): List<SavedOrderItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedOrder(order: SavedOrderEntity, items: List<SavedOrderItemEntity>)

    @Query("DELETE FROM saved_orders WHERE id = :orderId")
    suspend fun deleteSavedOrder(orderId: String)
}
```

- [ ] **Step 2: Register in AppDatabase.kt (Version 2)**

Tambahkan `SavedOrderEntity::class` dan `SavedOrderItemEntity::class` ke `AppDatabase`, naikkan `version = 2`, dan ekspos `abstract fun savedOrderDao(): SavedOrderDao`.

- [ ] **Step 3: Update PosRepository with Hold Order Methods**

Tambahkan method `saveOrder(tableNo, customerName, orderType, items, discount)`, `observeSavedOrders()`, `getItemsForSavedOrder()`, dan `deleteSavedOrder()`.

- [ ] **Step 4: Update PosViewModel & UI to Hold/Recall Orders**

- Tambahkan tombol **"Tahan Pesanan"** di `CartBottomSheet.kt`.
- Di `AppTopBar.kt` / `PosScreen.kt`, tampilkan badge tombol **"Pesanan Tersimpan"** jika ada order ditahan.
- Klik tombol memunculkan modal daftar pesanan tersimpan, kasir bisa memilih pesanan untuk dimuat kembali ke keranjang POS.

- [ ] **Step 5: Full verification build & unit test run**

Run: `cd /media/lian/Ubuntu/POS\ KA/android && ./gradlew testDebugUnitTest assembleDebug`
Expected: ALL TESTS PASSED, BUILD SUCCESSFUL.

- [ ] **Step 6: Commit Task 5**

```bash
git add app/src/main/java/com/kapos/mobile/core/data/local/ app/src/main/java/com/kapos/mobile/core/data/repository/ app/src/main/java/com/kapos/mobile/feature/pos/ app/src/main/java/com/kapos/mobile/feature/cart/
git commit -m "feat(pos): add hold and recall saved orders functionality"
```
