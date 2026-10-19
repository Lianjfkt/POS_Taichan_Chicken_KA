package com.kapos.mobile.feature.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kapos.mobile.core.components.PrimaryButton
import com.kapos.mobile.core.components.SecondaryButton
import com.kapos.mobile.core.components.StatCard
import com.kapos.mobile.core.theme.AppShapes
import com.kapos.mobile.core.theme.Danger
import com.kapos.mobile.core.theme.OutlineSoft
import com.kapos.mobile.core.theme.Primary
import com.kapos.mobile.core.theme.PrimarySoft
import com.kapos.mobile.core.theme.ScreenBackground
import com.kapos.mobile.core.theme.SurfaceTint
import com.kapos.mobile.core.theme.SurfaceWhite
import com.kapos.mobile.core.theme.TextPrimary
import com.kapos.mobile.core.theme.TextSecondary
import com.kapos.mobile.core.theme.Warning
import com.kapos.mobile.core.utils.CurrencyFormatter
import com.kapos.mobile.domain.model.Customer

@Composable
fun CustomerRoute(
    viewModel: CustomerViewModel,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    CustomerScreen(
        uiState = uiState,
        onBack = onBack,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onOpenAddCustomer = viewModel::openAddCustomer,
        onOpenEditCustomer = viewModel::openEditCustomer,
        onCloseAddEditDialog = viewModel::closeAddEditDialog,
        onSaveCustomer = viewModel::saveCustomer,
        onOpenDeleteCustomer = viewModel::openDeleteDialog,
        onCloseDeleteDialog = viewModel::closeDeleteDialog,
        onConfirmDeleteCustomer = viewModel::confirmDeleteCustomer,
        modifier = modifier
    )
}

@Composable
fun CustomerScreen(
    uiState: CustomerUiState,
    onBack: (() -> Unit)? = null,
    onSearchQueryChange: (String) -> Unit,
    onOpenAddCustomer: () -> Unit,
    onOpenEditCustomer: (Customer) -> Unit,
    onCloseAddEditDialog: () -> Unit,
    onSaveCustomer: (name: String, phone: String, points: Int) -> Unit,
    onOpenDeleteCustomer: (Customer) -> Unit,
    onCloseDeleteDialog: () -> Unit,
    onConfirmDeleteCustomer: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                .padding(bottom = 90.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Column {
                        Text(
                            text = "Pelanggan & Loyalty",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Manajemen data member & poin loyalitas",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Primary)
                        .clickable(onClick = onOpenAddCustomer)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = SurfaceWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tambah",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SurfaceWhite,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // Stat Cards Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Member",
                    value = "${uiState.totalMembers}",
                    icon = Icons.Default.People,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Poin Beredar",
                    value = "${uiState.totalPointsCirculating}",
                    icon = Icons.Default.Star,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Total Belanja",
                    value = CurrencyFormatter.formatRupiah(uiState.totalMemberSpend),
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1.2f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Cari berdasarkan nama atau No. HP...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = OutlineSoft,
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Customer List
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (uiState.filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (uiState.searchQuery.isBlank()) "Belum ada data pelanggan" else "Pelanggan tidak ditemukan",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredCustomers, key = { it.id }) { customer ->
                        CustomerCardItem(
                            customer = customer,
                            onEdit = { onOpenEditCustomer(customer) },
                            onDelete = { onOpenDeleteCustomer(customer) }
                        )
                    }
                }
            }
        }

        // Add / Edit Customer Dialog
        if (uiState.isAddEditCustomerOpen) {
            AddEditCustomerFullDialog(
                customer = uiState.customerToEdit,
                onSave = onSaveCustomer,
                onDismiss = onCloseAddEditDialog
            )
        }

        // Delete Customer Dialog
        if (uiState.customerToDelete != null) {
            DeleteCustomerConfirmDialog(
                customer = uiState.customerToDelete,
                onConfirm = onConfirmDeleteCustomer,
                onDismiss = onCloseDeleteDialog
            )
        }
    }
}

@Composable
fun CustomerCardItem(
    customer: Customer,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.Md)
            .background(SurfaceWhite)
            .border(1.dp, OutlineSoft, AppShapes.Md)
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PrimarySoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = customer.name.firstOrNull()?.uppercase() ?: "U",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = customer.phone.ifBlank { "Tidak ada No. HP" },
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Danger, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges row: Poin, Total Orders, Total Spend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFFEF3C7))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Warning, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${customer.points} Pts",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SurfaceTint)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${customer.orderCount}x Order",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SurfaceTint)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Total: ${CurrencyFormatter.formatRupiah(customer.totalSpend)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Primary,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCustomerFullDialog(
    customer: Customer?,
    onSave: (name: String, phone: String, points: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var pointsStr by remember { mutableStateOf(customer?.points?.toString() ?: "0") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.Lg)
                .background(SurfaceWhite)
                .border(1.dp, OutlineSoft, AppShapes.Lg)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (customer == null) "Tambah Member Baru" else "Edit Data Member",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Nama Member *") },
                    placeholder = { Text("Contoh: Budi Santoso") },
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
                    value = phone,
                    onValueChange = {
                        phone = it
                        errorMessage = null
                    },
                    label = { Text("No. HP / WhatsApp") },
                    placeholder = { Text("Contoh: 081234567890") },
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
                    value = pointsStr,
                    onValueChange = { pointsStr = it.filter { char -> char.isDigit() } },
                    label = { Text("Saldo Poin Loyalty") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = OutlineSoft
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall.copy(color = Danger)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryButton(
                        text = "Batal",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = "Simpan",
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Nama member wajib diisi!"
                            } else {
                                onSave(name, phone, pointsStr.toIntOrNull() ?: 0)
                            }
                        },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteCustomerConfirmDialog(
    customer: Customer,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.Lg)
                .background(SurfaceWhite)
                .border(1.dp, OutlineSoft, AppShapes.Lg)
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFDE4E5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Danger, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Hapus Member?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Apakah Anda yakin ingin menghapus data member '${customer.name}'? Data riwayat belanja dan saldo ${customer.points} poin akan terhapus.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryButton(
                        text = "Batal",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(AppShapes.Sm)
                            .background(Danger)
                            .clickable(onClick = onConfirm)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Hapus",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = SurfaceWhite,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
