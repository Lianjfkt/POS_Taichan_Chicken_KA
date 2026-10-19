package com.kapos.mobile.feature.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.kapos.mobile.core.theme.OutlineSoft
import com.kapos.mobile.core.theme.Primary
import com.kapos.mobile.core.theme.PrimarySoft
import com.kapos.mobile.core.theme.SurfaceTint
import com.kapos.mobile.core.theme.SurfaceWhite
import com.kapos.mobile.core.theme.TextPrimary
import com.kapos.mobile.core.theme.TextSecondary
import com.kapos.mobile.core.theme.Warning
import com.kapos.mobile.domain.model.Customer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectCustomerDialog(
    customers: List<Customer>,
    selectedCustomer: Customer?,
    onSelectCustomer: (Customer?) -> Unit,
    onOpenQuickAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else {
            val q = searchQuery.lowercase().trim()
            customers.filter {
                it.name.lowercase().contains(q) || it.phone.contains(q)
            }
        }
    }

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
                    Column {
                        Text(
                            text = "Pilih Member / Pelanggan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Dapatkan & tukar poin loyalty",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari nama atau No. HP...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                    },
                    singleLine = true,
                    shape = AppShapes.Sm,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = OutlineSoft
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action buttons row: Umum (Non-Member) & + Member Baru
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(AppShapes.Sm)
                            .background(if (selectedCustomer == null) PrimarySoft else SurfaceTint)
                            .border(1.dp, if (selectedCustomer == null) Primary else OutlineSoft, AppShapes.Sm)
                            .clickable {
                                onSelectCustomer(null)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "👤 Pelanggan Umum",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (selectedCustomer == null) Primary else TextPrimary
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(AppShapes.Sm)
                            .background(SurfaceWhite)
                            .border(1.dp, Primary, AppShapes.Sm)
                            .clickable {
                                onOpenQuickAdd()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Member Baru",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = OutlineSoft)

                Text(
                    text = "Daftar Member Terdaftar (${filteredCustomers.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (filteredCustomers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tidak ada member yang cocok",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            }
                        }
                    } else {
                        items(filteredCustomers, key = { it.id }) { customer ->
                            val isSelected = selectedCustomer?.id == customer.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AppShapes.Sm)
                                    .background(if (isSelected) PrimarySoft else SurfaceTint)
                                    .border(1.dp, if (isSelected) Primary else OutlineSoft, AppShapes.Sm)
                                    .clickable {
                                        onSelectCustomer(customer)
                                        onDismiss()
                                    }
                                    .padding(10.dp),
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
                                            .background(if (isSelected) Primary else SurfaceWhite)
                                            .border(1.dp, OutlineSoft, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = customer.name.firstOrNull()?.uppercase() ?: "U",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) SurfaceWhite else Primary
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = customer.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        )
                                        Text(
                                            text = customer.phone,
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFFFEF3C7))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Warning,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${customer.points} Poin",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E),
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddCustomerDialog(
    onSave: (name: String, phone: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(PrimarySoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Daftar Member Cepat",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
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
                    label = { Text("Nama Lengkap Member *") },
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
                    label = { Text("No. WhatsApp / HP (Opsional)") },
                    placeholder = { Text("Contoh: 081234567890") },
                    singleLine = true,
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
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Red)
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
                        text = "Simpan & Pilih",
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Nama member wajib diisi!"
                            } else {
                                onSave(name, phone)
                            }
                        },
                        modifier = Modifier.weight(1.4f)
                    )
                }
            }
        }
    }
}
