package com.kapos.mobile.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kapos.mobile.core.components.PrimaryButton
import com.kapos.mobile.core.components.SecondaryButton
import com.kapos.mobile.core.theme.AppShapes
import com.kapos.mobile.core.theme.Danger
import com.kapos.mobile.core.theme.OutlineSoft
import com.kapos.mobile.core.theme.Primary
import com.kapos.mobile.core.theme.PrimarySoft
import com.kapos.mobile.core.theme.ScreenBackground
import com.kapos.mobile.core.theme.Success
import com.kapos.mobile.core.theme.SurfaceTint
import com.kapos.mobile.core.theme.SurfaceWhite
import com.kapos.mobile.core.theme.TextPrimary
import com.kapos.mobile.core.theme.TextSecondary
import androidx.compose.material.icons.filled.Logout
import com.kapos.mobile.core.theme.Warning

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onLockClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    SettingsScreen(
        uiState = uiState,
        onLockClick = onLockClick,
        onLogoutClick = onLogoutClick,
        onUrlChange = viewModel::onUrlChange,
        onAnonKeyChange = viewModel::onAnonKeyChange,
        onStoreNameChange = viewModel::onStoreNameChange,
        onStoreAddressChange = viewModel::onStoreAddressChange,
        onStorePhoneChange = viewModel::onStorePhoneChange,
        onCashierNameChange = viewModel::onCashierNameChange,
        onReceiptFooterChange = viewModel::onReceiptFooterChange,
        onCashierPinChange = viewModel::onCashierPinChange,
        onOwnerPinChange = viewModel::onOwnerPinChange,
        onDailyFixedCostChange = viewModel::onDailyFixedCostChange,
        onDailyRevenueTargetChange = viewModel::onDailyRevenueTargetChange,
        onRefreshPrinters = viewModel::loadPairedPrinters,
        onSelectPrinter = viewModel::onSelectPrinter,
        onPaperWidthChange = viewModel::onPaperWidthChange,
        onAutoPrintChange = viewModel::onAutoPrintChange,
        onTestPrint = viewModel::testPrintReceipt,
        onSave = viewModel::saveSettings,
        onTestConnection = viewModel::testConnection,
        onSyncNow = viewModel::syncNow,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onLockClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onUrlChange: (String) -> Unit,
    onAnonKeyChange: (String) -> Unit,
    onStoreNameChange: (String) -> Unit,
    onStoreAddressChange: (String) -> Unit,
    onStorePhoneChange: (String) -> Unit,
    onCashierNameChange: (String) -> Unit,
    onReceiptFooterChange: (String) -> Unit,
    onCashierPinChange: (String) -> Unit,
    onOwnerPinChange: (String) -> Unit,
    onDailyFixedCostChange: (Double) -> Unit,
    onDailyRevenueTargetChange: (Double) -> Unit,
    onRefreshPrinters: () -> Unit,
    onSelectPrinter: (name: String, address: String) -> Unit,
    onPaperWidthChange: (Int) -> Unit,
    onAutoPrintChange: (Boolean) -> Unit,
    onTestPrint: () -> Unit,
    onSave: () -> Unit,
    onTestConnection: () -> Unit,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(bottom = 100.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pengaturan",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Koneksi Supabase, Profil Gerai & Akses",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceWhite)
                            .border(1.dp, OutlineSoft, CircleShape)
                            .clickable { onLockClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Kunci",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFDE4E5))
                            .border(1.dp, Danger.copy(alpha = 0.3f), CircleShape)
                            .clickable { onLogoutClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = Danger,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Supabase Configuration
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.Md)
                        .background(SurfaceWhite)
                        .border(1.dp, OutlineSoft, AppShapes.Md)
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PrimarySoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Koneksi Cloud Supabase",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = uiState.supabaseUrl,
                            onValueChange = onUrlChange,
                            label = { Text("Supabase URL (https://xxx.supabase.co)") },
                            singleLine = true,
                            shape = AppShapes.Sm,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = OutlineSoft
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = uiState.supabaseAnonKey,
                            onValueChange = onAnonKeyChange,
                            label = { Text("Supabase Anon Public Key") },
                            singleLine = true,
                            shape = AppShapes.Sm,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = OutlineSoft
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (uiState.connectionStatusMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val isSuccess = uiState.isConnectionSuccess == true
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AppShapes.Sm)
                                    .background(if (isSuccess) Color(0xFFE8F7EE) else Color(0xFFFDE4E5))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = uiState.connectionStatusMessage,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (isSuccess) Success else Danger,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        SecondaryButton(
                            text = if (uiState.isTestingConnection) "Menguji Koneksi..." else "Tes Koneksi Cloud",
                            onClick = onTestConnection,
                            enabled = !uiState.isTestingConnection
                        )
                    }
                }

                // Section 2: Store Information
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.Md)
                        .background(SurfaceWhite)
                        .border(1.dp, OutlineSoft, AppShapes.Md)
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PrimarySoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Store,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Profil Gerai & Kasir",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = uiState.storeName,
                            onValueChange = onStoreNameChange,
                            label = { Text("Nama Toko / Gerai") },
                            singleLine = true,
                            shape = AppShapes.Sm,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = OutlineSoft
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = uiState.storeAddress,
                            onValueChange = onStoreAddressChange,
                            label = { Text("Alamat Toko") },
                            singleLine = true,
                            shape = AppShapes.Sm,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = OutlineSoft
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = uiState.cashierName,
                            onValueChange = onCashierNameChange,
                            label = { Text("Nama Kasir Aktif") },
                            singleLine = true,
                            shape = AppShapes.Sm,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = OutlineSoft
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = uiState.storePhone,
                            onValueChange = onStorePhoneChange,
                            label = { Text("Nomor Telepon Toko") },
                            singleLine = true,
                            shape = AppShapes.Sm,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = OutlineSoft
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = uiState.receiptFooter,
                            onValueChange = onReceiptFooterChange,
                            label = { Text("Pesan Kaki Struk (Footer)") },
                            singleLine = true,
                            shape = AppShapes.Sm,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = OutlineSoft
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Section 2C: Keamanan & Otorisasi PIN
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.Md)
                        .background(SurfaceWhite)
                        .border(1.dp, OutlineSoft, AppShapes.Md)
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimarySoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Keamanan & PIN Akses",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "PIN keypad 4-digit untuk Kasir dan Owner",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.cashierPin,
                                onValueChange = onCashierPinChange,
                                label = { Text("PIN Kasir") },
                                singleLine = true,
                                shape = AppShapes.Sm,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = OutlineSoft
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = uiState.ownerPin,
                                onValueChange = onOwnerPinChange,
                                label = { Text("PIN Owner") },
                                singleLine = true,
                                shape = AppShapes.Sm,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = OutlineSoft
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Section 2D: Target Finansial & Titik Impas (BEP)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.Md)
                        .background(SurfaceWhite)
                        .border(1.dp, OutlineSoft, AppShapes.Md)
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimarySoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Target Finansial & Titik Impas (BEP)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Parameter kalkulasi progres BEP harian",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = "%.0f".format(uiState.dailyRevenueTarget),
                            onValueChange = { str ->
                                val clean = str.filter { it.isDigit() }
                                onDailyRevenueTargetChange(clean.toDoubleOrNull() ?: 0.0)
                            },
                            label = { Text("Target Omzet Harian (Rp)") },
                            singleLine = true,
                            shape = AppShapes.Sm,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = OutlineSoft
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = "%.0f".format(uiState.dailyFixedCost),
                            onValueChange = { str ->
                                val clean = str.filter { it.isDigit() }
                                onDailyFixedCostChange(clean.toDoubleOrNull() ?: 0.0)
                            },
                            label = { Text("Beban Tetap Harian (Sewa, Listrik, Gaji) (Rp)") },
                            singleLine = true,
                            shape = AppShapes.Sm,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = OutlineSoft
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Section 2B: Pengaturan Printer Thermal (Bluetooth)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.Md)
                        .background(SurfaceWhite)
                        .border(1.dp, OutlineSoft, AppShapes.Md)
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(PrimarySoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Print,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Printer Thermal (Bluetooth)",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "ESC/POS Struk Kasir 58mm / 80mm",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                    )
                                }
                            }

                            IconButton(onClick = onRefreshPrinters) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = Primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Pilih Printer yang Terpasang (Paired):",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        if (uiState.pairedDevices.isEmpty()) {
                            Text(
                                text = "Tidak ada printer Bluetooth ter-pairing. Buka pengaturan Bluetooth ponsel/tablet dan hubungkan printer Anda terlebih dahulu.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                uiState.pairedDevices.forEach { device ->
                                    val isSelected = uiState.printerMacAddress == device.address
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(AppShapes.Sm)
                                            .background(if (isSelected) PrimarySoft else SurfaceTint)
                                            .border(1.dp, if (isSelected) Primary else OutlineSoft, AppShapes.Sm)
                                            .clickable {
                                                onSelectPrinter(device.name, device.address)
                                            }
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = device.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Primary else TextPrimary
                                                )
                                            )
                                            Text(
                                                text = device.address,
                                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                            )
                                        }

                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Lebar Kertas
                        Text(
                            text = "Ukuran Kertas Thermal:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(58 to "58mm (Standar)", 80 to "80mm (Lebar)").forEach { (width, label) ->
                                val isSelected = uiState.paperWidth == width
                                Box(
                                    modifier = Modifier
                                        .clip(AppShapes.Sm)
                                        .background(if (isSelected) Primary else SurfaceTint)
                                        .border(1.dp, if (isSelected) Primary else OutlineSoft, AppShapes.Sm)
                                        .clickable { onPaperWidthChange(width) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) SurfaceWhite else TextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Auto-print Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Cetak Otomatis saat Checkout",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Langsung kirim nota ke printer begitu kasir menyelesaikan transaksi",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                )
                            }
                            Switch(
                                checked = uiState.autoPrintCheckout,
                                onCheckedChange = onAutoPrintChange
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Test Print Button
                        SecondaryButton(
                            text = if (uiState.isTestingPrint) "Mengirim Tes Struk..." else "Tes Cetak Struk Fisik",
                            icon = Icons.Default.Print,
                            onClick = onTestPrint,
                            enabled = !uiState.isTestingPrint && uiState.printerMacAddress.isNotBlank()
                        )

                        if (uiState.printTestStatusMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val isSuccess = uiState.isPrintTestSuccess == true
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AppShapes.Sm)
                                    .background(if (isSuccess) Color(0xFFE8F7EE) else Color(0xFFFFEEEE))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = uiState.printTestStatusMessage,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSuccess) Success else Danger,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }

                // Section 3: Offline Queue & Sync
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.Md)
                        .background(SurfaceWhite)
                        .border(1.dp, OutlineSoft, AppShapes.Md)
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Status Sinkronisasi Offline",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${uiState.pendingSyncCount} transaksi menunggu sinkronisasi",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(AppShapes.Xs)
                                    .background(if (uiState.pendingSyncCount > 0) Color(0xFFFFF4E5) else Color(0xFFE8F7EE))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (uiState.pendingSyncCount > 0) "Pending" else "Tersinkron",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (uiState.pendingSyncCount > 0) Warning else Success,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        SecondaryButton(
                            text = if (uiState.isSyncing) "Menyinkronkan..." else "Sinkronkan Sekarang",
                            icon = Icons.Default.Sync,
                            onClick = onSyncNow,
                            enabled = !uiState.isSyncing
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Save CTA
                PrimaryButton(
                    text = "Simpan Pengaturan",
                    icon = Icons.Default.Save,
                    onClick = onSave
                )
            }
        }
    }
}
