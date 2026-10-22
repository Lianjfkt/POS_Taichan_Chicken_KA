package com.kapos.mobile.feature.settings

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestaurantMenu
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
    onNavigateToMenuManagement: () -> Unit = {},
    onNavigateToCustomers: () -> Unit = {},
    onNavigateToReport: () -> Unit = {},
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
        onAddStaff = viewModel::onAddStaff,
        onAutoLockMinutesChange = viewModel::onAutoLockMinutesChange,
        onAutoSyncIntervalChange = viewModel::onAutoSyncIntervalChange,
        onExportBackup = { viewModel.exportDatabaseBackup(context) },
        onImportBackup = viewModel::importDatabaseBackup,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onLockClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onNavigateToMenuManagement: () -> Unit = {},
    onNavigateToCustomers: () -> Unit = {},
    onNavigateToReport: () -> Unit = {},
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
    onAddStaff: (String) -> Unit = {},
    onAutoLockMinutesChange: (Int) -> Unit = {},
    onAutoSyncIntervalChange: (Int) -> Unit = {},
    onExportBackup: () -> Unit = {},
    onImportBackup: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                    val content = reader.readText()
                    onImportBackup(content)
                }
            } catch (_: Exception) {}
        }
    }
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
                // Section 0: Hub Kelola Menu & Pelanggan
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.Md)
                        .background(SurfaceWhite)
                        .border(1.dp, OutlineSoft, AppShapes.Md)
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Kelola Bisnis & Operasional",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Akses cepat manajemen master menu dan data member",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Menu Management Tile
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppShapes.Sm)
                                .background(SurfaceTint)
                                .border(1.dp, OutlineSoft, AppShapes.Sm)
                                .clickable(onClick = onNavigateToMenuManagement)
                                .padding(12.dp),
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
                                        Icons.Default.RestaurantMenu,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Kelola Menu & Kategori",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = "Tambah produk, atur harga, HPP modal & kategori",
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

                        Spacer(modifier = Modifier.height(8.dp))

                        // Customer Management Tile
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppShapes.Sm)
                                .background(SurfaceTint)
                                .border(1.dp, OutlineSoft, AppShapes.Sm)
                                .clickable(onClick = onNavigateToCustomers)
                                .padding(12.dp),
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
                                        Icons.Default.People,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Kelola Pelanggan (CRM) & Poin",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = "Database member, riwayat belanja & loyalty points",
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

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sales Report & Summary Tile
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppShapes.Sm)
                                .background(SurfaceTint)
                                .border(1.dp, OutlineSoft, AppShapes.Sm)
                                .clickable(onClick = onNavigateToReport)
                                .padding(12.dp),
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
                                        Icons.Default.Assessment,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Laporan & Rekap Penjualan",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = "Cetak rekap harian, shift, laba kotor & export",
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
                }

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

                        if (uiState.staffList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Pilih Kasir Bertugas:",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
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

                        Spacer(modifier = Modifier.height(14.dp))

                        // Auto-lock Inactivity setting
                        Text(
                            text = "Kunci Otomatis (Inactivity Timer)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                0 to "Nonaktif",
                                1 to "1 Menit",
                                2 to "2 Menit",
                                5 to "5 Menit"
                            ).forEach { (mins, label) ->
                                val isSelected = uiState.autoLockMinutes == mins
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(AppShapes.Sm)
                                        .background(if (isSelected) Primary else SurfaceTint)
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

                        if (!uiState.hasBluetoothPermission) {
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
                                        text = "Izin Perangkat Sekitar Diperlukan",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = "Android 12+ memerlukan izin koneksi Bluetooth untuk mendeteksi dan menghubungkan printer thermal.",
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
                            Spacer(modifier = Modifier.height(10.dp))
                        }

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
                                val lastSyncStr = uiState.lastSyncTime?.let {
                                    val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale("id", "ID"))
                                    "Terakhir sinkron: ${sdf.format(java.util.Date(it))}"
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
                            Spacer(modifier = Modifier.height(10.dp))
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

                        Spacer(modifier = Modifier.height(14.dp))

                        // Interval Sinkronisasi Otomatis
                        Text(
                            text = "Sinkronisasi Otomatis (Auto-Sync):",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Data pesanan dikirim otomatis ke cloud sesuai interval yang dipilih",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                0 to "Nonaktif",
                                2 to "2 Mnt",
                                5 to "5 Mnt",
                                15 to "15 Mnt",
                                30 to "30 Mnt"
                            ).forEach { (mins, label) ->
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

                // Section: Cadangan & Pemulihan Data (Backup & Restore JSON)
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
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Cadangan & Pemulihan Data",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Simpan data offline ke file JSON atau pulihkan cadangan",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                )
                            }
                        }

                        if (uiState.backupStatusMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AppShapes.Sm)
                                    .background(if (uiState.isBackupSuccess == true) Color(0xFFE8F7EE) else Color(0xFFFDE4E5))
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
                        }

                        Spacer(modifier = Modifier.height(12.dp))

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
                                    onClick = {
                                        filePickerLauncher.launch("*/*")
                                    },
                                    enabled = !uiState.isExportingBackup
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Save CTA
                PrimaryButton(
                    text = "Simpan Pengaturan",
                    icon = Icons.Default.Save,
                    onClick = onSave
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "KA POS Mobile v1.2.0 (Build 3) • Taichan & Chicken KA",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
