package com.kapos.mobile.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
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
import com.kapos.mobile.core.theme.Warning

// ─────────────────────────────────────────────────────────────────────────────
// ExpandableSection: Accordion card — header selalu tampil, konten collapsible
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExpandableSection(
    icon: ImageVector,
    title: String,
    subtitle: String,
    initialExpanded: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initialExpanded) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevron"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.Md)
            .background(SurfaceWhite)
            .border(1.dp, OutlineSoft, AppShapes.Md)
    ) {
        // ── Header row (always visible, tap to expand/collapse) ───────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (expanded) Primary else PrimarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (expanded) SurfaceWhite else Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (expanded) Primary else TextPrimary
                        )
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = if (expanded) Primary else TextSecondary,
                modifier = Modifier
                    .size(22.dp)
                    .rotate(chevronRotation)
            )
        }

        // ── Collapsible content ───────────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                HorizontalDivider(color = OutlineSoft, thickness = 0.8.dp)
                Column(modifier = Modifier.padding(16.dp)) {
                    content()
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NavTile: baris navigasi tap-able dengan chevron ke kanan
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NavTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.Sm)
            .background(SurfaceTint)
            .border(1.dp, OutlineSoft, AppShapes.Sm)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
            }
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SettingsTextField helper
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = AppShapes.Sm,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = OutlineSoft
        ),
        modifier = modifier.fillMaxWidth()
    )
}

// =============================================================================
// Route
// =============================================================================

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onLockClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onNavigateToMenuManagement: () -> Unit = {},
    onNavigateToCustomers: () -> Unit = {},
    onNavigateToReport: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.loadPairedPrinters()
    }
    val uiState by viewModel.uiState.collectAsState()

    SettingsScreen(
        uiState = uiState,
        onLockClick = onLockClick,
        onLogoutClick = onLogoutClick,
        onNavigateToMenuManagement = onNavigateToMenuManagement,
        onNavigateToCustomers = onNavigateToCustomers,
        onNavigateToReport = onNavigateToReport,
        onNavigateToAnalytics = onNavigateToAnalytics,
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
        onRequestBluetoothPermission = {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                permissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.BLUETOOTH_CONNECT,
                        android.Manifest.permission.BLUETOOTH_SCAN
                    )
                )
            }
        },
        onSelectPrinter = viewModel::onSelectPrinter,
        onPaperWidthChange = viewModel::onPaperWidthChange,
        onAutoPrintChange = viewModel::onAutoPrintChange,
        onTestPrint = viewModel::testPrintReceipt,
        onSave = viewModel::saveSettings,
        onTestConnection = viewModel::testConnection,
        onSyncNow = viewModel::syncNow,
        onSelectActiveStaff = viewModel::onSelectActiveStaff,
        onAutoLockMinutesChange = viewModel::onAutoLockMinutesChange,
        onAutoSyncIntervalChange = viewModel::onAutoSyncIntervalChange,
        onExportBackup = { viewModel.exportDatabaseBackup(context) },
        onImportBackup = viewModel::importDatabaseBackup,
        onResetDatabase = viewModel::resetDatabase,
        modifier = modifier
    )
}

// =============================================================================
// Main Screen
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onLockClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onNavigateToMenuManagement: () -> Unit = {},
    onNavigateToCustomers: () -> Unit = {},
    onNavigateToReport: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
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
    onRequestBluetoothPermission: () -> Unit = {},
    onSelectPrinter: (name: String, address: String) -> Unit,
    onPaperWidthChange: (Int) -> Unit,
    onAutoPrintChange: (Boolean) -> Unit,
    onTestPrint: () -> Unit,
    onSave: () -> Unit,
    onTestConnection: () -> Unit,
    onSyncNow: () -> Unit,
    onSelectActiveStaff: (String) -> Unit = {},
    onAutoLockMinutesChange: (Int) -> Unit = {},
    onAutoSyncIntervalChange: (Int) -> Unit = {},
    onExportBackup: () -> Unit = {},
    onImportBackup: (String) -> Unit = {},
    onResetDatabase: (String, (Boolean) -> Unit) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                    onImportBackup(reader.readText())
                }
            } catch (_: Exception) {}
        }
    }
    val scrollState = rememberScrollState()
    var showResetDialog by remember { mutableStateOf(false) }
    var resetPinInput by remember { mutableStateOf("") }

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

            // ── Top Bar ───────────────────────────────────────────────────────
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
                        text = "Pilih kategori untuk mengatur",
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
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = Danger,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // ── Accordion Sections ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // ── 1. OPERASIONAL ────────────────────────────────────────────
                ExpandableSection(
                    icon = Icons.Default.RestaurantMenu,
                    title = "Operasional",
                    subtitle = "Menu, pelanggan & laporan penjualan"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NavTile(
                            icon = Icons.Default.RestaurantMenu,
                            title = "Kelola Menu & Kategori",
                            subtitle = "Tambah produk, atur harga, HPP modal & kategori",
                            onClick = onNavigateToMenuManagement
                        )
                        NavTile(
                            icon = Icons.Default.People,
                            title = "Kelola Pelanggan (CRM) & Poin",
                            subtitle = "Database member, riwayat belanja & loyalty points",
                            onClick = onNavigateToCustomers
                        )
                        NavTile(
                            icon = Icons.Default.Assessment,
                            title = "Laporan & Rekap Penjualan",
                            subtitle = "Cetak rekap harian, shift, laba kotor & export",
                            onClick = onNavigateToReport
                        )
                        NavTile(
                            icon = Icons.Default.BarChart,
                            title = "Analisa Keuangan",
                            subtitle = "Grafik revenue, BEP, HPP & tren penjualan",
                            onClick = onNavigateToAnalytics
                        )
                    }
                }

                // ── 2. PROFIL GERAI ───────────────────────────────────────────
                ExpandableSection(
                    icon = Icons.Default.Store,
                    title = "Profil Gerai & Kasir",
                    subtitle = "Nama toko, alamat, footer struk & kasir aktif"
                ) {
                    Column {
                        SettingsTextField(uiState.storeName, onStoreNameChange, "Nama Toko / Gerai")
                        Spacer(modifier = Modifier.height(10.dp))
                        SettingsTextField(uiState.storeAddress, onStoreAddressChange, "Alamat Toko")
                        Spacer(modifier = Modifier.height(10.dp))
                        SettingsTextField(uiState.storePhone, onStorePhoneChange, "Nomor Telepon Toko")
                        Spacer(modifier = Modifier.height(10.dp))
                        SettingsTextField(uiState.receiptFooter, onReceiptFooterChange, "Pesan Kaki Struk (Footer)")

                        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = OutlineSoft)

                        Text(
                            text = "Kasir Aktif",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsTextField(uiState.cashierName, onCashierNameChange, "Nama Kasir Aktif")

                        if (uiState.staffList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Pilih Kasir Bertugas:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                uiState.staffList.forEach { staff ->
                                    val isSelected = staff.equals(uiState.cashierName, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isSelected) Primary else SurfaceTint)
                                            .border(1.dp, if (isSelected) Primary else OutlineSoft, CircleShape)
                                            .clickable { onSelectActiveStaff(staff) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = staff,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isSelected) SurfaceWhite else TextPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 3. KEAMANAN ───────────────────────────────────────────────
                ExpandableSection(
                    icon = Icons.Default.Lock,
                    title = "Keamanan & PIN Akses",
                    subtitle = "PIN Kasir, PIN Owner & kunci otomatis"
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.cashierPin,
                                onValueChange = onCashierPinChange,
                                label = { Text("PIN Kasir (Baru)") },
                                placeholder = { Text("•••• (Tersimpan)") },
                                singleLine = true,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
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
                                label = { Text("PIN Owner (Baru)") },
                                placeholder = { Text("•••• (Tersimpan)") },
                                singleLine = true,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                shape = AppShapes.Sm,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = OutlineSoft
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = OutlineSoft)

                        Text(
                            text = "Kunci Otomatis (Inactivity Timer)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0 to "Nonaktif", 1 to "1 Mnt", 2 to "2 Mnt", 5 to "5 Mnt")
                                .forEach { (mins, label) ->
                                    val isSelected = uiState.autoLockMinutes == mins
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(AppShapes.Sm)
                                            .background(if (isSelected) Primary else SurfaceTint)
                                            .border(1.dp, if (isSelected) Primary else OutlineSoft, AppShapes.Sm)
                                            .clickable { onAutoLockMinutesChange(mins) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isSelected) SurfaceWhite else TextPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    }
                                }
                        }
                    }
                }

                // ── 4. FINANSIAL ──────────────────────────────────────────────
                ExpandableSection(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    title = "Target Finansial & BEP",
                    subtitle = "Omzet harian & titik impas (break-even point)"
                ) {
                    Column {
                        SettingsTextField(
                            value = "%.0f".format(uiState.dailyRevenueTarget),
                            onValueChange = { str ->
                                val clean = str.filter { it.isDigit() }
                                onDailyRevenueTargetChange(clean.toDoubleOrNull() ?: 0.0)
                            },
                            label = "Target Omzet Harian (Rp)"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SettingsTextField(
                            value = "%.0f".format(uiState.dailyFixedCost),
                            onValueChange = { str ->
                                val clean = str.filter { it.isDigit() }
                                onDailyFixedCostChange(clean.toDoubleOrNull() ?: 0.0)
                            },
                            label = "Beban Tetap Harian (Sewa, Listrik, Gaji) (Rp)"
                        )
                    }
                }

                // ── 5. PRINTER ────────────────────────────────────────────────
                ExpandableSection(
                    icon = Icons.Default.Print,
                    title = "Printer Thermal",
                    subtitle = "Bluetooth ESC/POS, ukuran kertas & cetak otomatis"
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pilih Printer (Paired):",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            IconButton(onClick = onRefreshPrinters) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Primary)
                            }
                        }

                        if (!uiState.hasBluetoothPermission) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AppShapes.Sm)
                                    .background(Warning.copy(alpha = 0.12f))
                                    .border(1.dp, Warning.copy(alpha = 0.4f), AppShapes.Sm)
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Izin Bluetooth Diperlukan",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold, color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = "Android 12+ memerlukan izin Bluetooth untuk mendeteksi printer.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    PrimaryButton(
                                        text = "Beri Izin Bluetooth",
                                        onClick = onRequestBluetoothPermission,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else if (uiState.pairedDevices.isEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tidak ada printer ter-pairing. Buka pengaturan Bluetooth dan hubungkan printer terlebih dahulu.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                uiState.pairedDevices.forEach { device ->
                                    val isSelected = uiState.printerMacAddress == device.address
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(AppShapes.Sm)
                                            .background(if (isSelected) PrimarySoft else SurfaceTint)
                                            .border(1.dp, if (isSelected) Primary else OutlineSoft, AppShapes.Sm)
                                            .clickable { onSelectPrinter(device.name, device.address) }
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
                                                Icons.Default.Check, contentDescription = "Selected",
                                                tint = Primary, modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = OutlineSoft)

                        Text(
                            text = "Ukuran Kertas:",
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

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = OutlineSoft)

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
                                    text = "Kirim nota ke printer otomatis setelah transaksi",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                )
                            }
                            Switch(checked = uiState.autoPrintCheckout, onCheckedChange = onAutoPrintChange)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        SecondaryButton(
                            text = if (uiState.isTestingPrint) "Mengirim Tes Struk..." else "Tes Cetak Struk",
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

                // ── 6. CLOUD & SINKRONISASI ───────────────────────────────────
                ExpandableSection(
                    icon = Icons.Default.Cloud,
                    title = "Cloud & Sinkronisasi",
                    subtitle = "Supabase, status sync & interval otomatis"
                ) {
                    Column {
                        Text(
                            text = "Koneksi Supabase",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsTextField(
                            value = uiState.supabaseUrl,
                            onValueChange = onUrlChange,
                            label = "Supabase URL (https://xxx.supabase.co)"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SettingsTextField(
                            value = uiState.supabaseAnonKey,
                            onValueChange = onAnonKeyChange,
                            label = "Supabase Anon Public Key"
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
                            text = if (uiState.isTestingConnection) "Menguji..." else "Tes Koneksi Cloud",
                            onClick = onTestConnection,
                            enabled = !uiState.isTestingConnection
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = OutlineSoft)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Status Sinkronisasi Offline",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "${uiState.pendingSyncCount} transaksi pending",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                )
                                val lastSyncStr = uiState.lastSyncTime?.let {
                                    val sdf = java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale("id", "ID"))
                                    "Terakhir: ${sdf.format(java.util.Date(it))}"
                                } ?: "Belum pernah disinkronkan"
                                Text(
                                    text = lastSyncStr,
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

                        if (uiState.syncResultMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val isSuccess = uiState.isSyncSuccess == true
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AppShapes.Sm)
                                    .background(if (isSuccess) Color(0xFFE8F7EE) else Color(0xFFFDE4E5))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = uiState.syncResultMessage,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (isSuccess) Success else Danger,
                                        fontWeight = FontWeight.SemiBold
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

                        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = OutlineSoft)

                        Text(
                            text = "Auto-Sync Interval:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Data dikirim otomatis ke cloud sesuai interval",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0 to "Nonaktif", 2 to "2 Mnt", 5 to "5 Mnt", 15 to "15 Mnt", 30 to "30 Mnt")
                                .forEach { (mins, label) ->
                                    val isSelected = uiState.autoSyncIntervalMinutes == mins
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(AppShapes.Sm)
                                            .background(if (isSelected) Primary else SurfaceTint)
                                            .border(1.dp, if (isSelected) Primary else OutlineSoft, AppShapes.Sm)
                                            .clickable { onAutoSyncIntervalChange(mins) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isSelected) SurfaceWhite else TextPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    }
                                }
                        }
                    }
                }

                // ── 7. DATA & CADANGAN ────────────────────────────────────────
                ExpandableSection(
                    icon = Icons.Default.Save,
                    title = "Data & Cadangan",
                    subtitle = "Ekspor & pulihkan data offline (JSON)"
                ) {
                    Column {
                        if (uiState.backupStatusMessage != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AppShapes.Sm)
                                    .background(
                                        if (uiState.isBackupSuccess == true) Color(0xFFE8F7EE)
                                        else Color(0xFFFDE4E5)
                                    )
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = uiState.backupStatusMessage,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (uiState.isBackupSuccess == true) Success else Danger,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                SecondaryButton(
                                    text = if (uiState.isExportingBackup) "Mengekspor..." else "Ekspor JSON",
                                    icon = Icons.Default.Cloud,
                                    onClick = onExportBackup,
                                    enabled = !uiState.isExportingBackup
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                SecondaryButton(
                                    text = "Pulihkan JSON",
                                    icon = Icons.Default.Refresh,
                                    onClick = { filePickerLauncher.launch("*/*") },
                                    enabled = !uiState.isExportingBackup
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = OutlineSoft)

                        // ── Factory Reset ─────────────────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppShapes.Sm)
                                .background(Color(0xFFFFF0F0))
                                .border(1.dp, Danger.copy(alpha = 0.35f), AppShapes.Sm)
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Danger,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Reset Seluruh Data (Factory Reset)",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Danger
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Mengosongkan total seluruh transaksi, shift, menu produk, bahan baku, dan riwayat pelanggan di perangkat ini untuk memulai operasional bersih dari nol.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                PrimaryButton(
                                    text = if (uiState.isResettingDatabase) "Mereset Database..." else "Reset Seluruh Data",
                                    containerColor = Danger,
                                    onClick = { showResetDialog = true },
                                    enabled = !uiState.isResettingDatabase,
                                    isLoading = uiState.isResettingDatabase
                                )
                            }
                        }

                        if (uiState.resetDatabaseStatusMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val isSuccess = uiState.isResetSuccess == true
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AppShapes.Sm)
                                    .background(if (isSuccess) Color(0xFFE8F7EE) else Color(0xFFFDE4E5))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = uiState.resetDatabaseStatusMessage,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isSuccess) Success else Danger,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }

                // ── Save CTA ─────────────────────────────────────────────────
                Spacer(modifier = Modifier.height(6.dp))
                PrimaryButton(
                    text = "Simpan Pengaturan",
                    icon = Icons.Default.Save,
                    onClick = onSave
                )

                Text(
                    text = "KA POS Mobile v1.2.0 (Build 3) • Taichan & Chicken KA",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 8.dp)
                )
            }
        }

        if (showResetDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = {
                    showResetDialog = false
                    resetPinInput = ""
                },
                title = {
                    Text(
                        text = "Konfirmasi Reset Database",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Danger)
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Tindakan ini TIDAK DAPAT DIBATALKAN. Seluruh data transaksi, menu produk, inventaris, dan pelanggan akan dihapus total.\n\nMasukkan PIN Owner untuk melanjutkan:",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = resetPinInput,
                            onValueChange = { resetPinInput = it },
                            label = { Text("PIN Owner") },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                            ),
                            shape = AppShapes.Sm,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    androidx.compose.material3.Button(
                        onClick = {
                            onResetDatabase(resetPinInput) { success ->
                                if (success) {
                                    showResetDialog = false
                                    resetPinInput = ""
                                }
                            }
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Danger),
                        enabled = !uiState.isResettingDatabase && resetPinInput.isNotBlank()
                    ) {
                        Text("Hapus Permanen", color = Color.White)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showResetDialog = false
                            resetPinInput = ""
                        }
                    ) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}
