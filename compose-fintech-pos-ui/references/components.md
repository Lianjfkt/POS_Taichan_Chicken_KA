# Referensi Kode Compose

Daftar isi: A. Theme · B. Komponen · C. Contoh layar (Transactions, Sales Report)

Sesuaikan `package` dan import. Kode memakai Material 3.

---

## A. Theme

### `ui/theme/Color.kt`
```kotlin
package com.example.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF0B5C73)
val PrimaryDark = Color(0xFF08485B)
val PrimarySoft = Color(0xFFE3F1F5)
val BgTop = Color(0xFFDCE9EE)
val BgBottom = Color(0xFFC7DAE1)
val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceTint = Color(0xFFF3F8FA)
val OutlineSoft = Color(0xFFDCE5E9)
val TextPrimary = Color(0xFF12303B)
val TextSecondary = Color(0xFF6B8189)
val Success = Color(0xFF2E9E6B)
val SuccessBg = Color(0xFFDDF3E8)
val Danger = Color(0xFFE5484D)
val DangerBg = Color(0xFFFDE4E5)
val Warning = Color(0xFFE8A33D)
val Info = Color(0xFF3C7A8C)
val InfoBg = Color(0xFFE1EEF2)

val ScreenBackground = Brush.verticalGradient(listOf(BgTop, BgBottom))
```

### `ui/theme/Shape.kt`
```kotlin
package com.example.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object AppShapes {
    val Xs = RoundedCornerShape(8.dp)
    val Sm = RoundedCornerShape(12.dp)
    val Md = RoundedCornerShape(16.dp)
    val Lg = RoundedCornerShape(24.dp)
    val SheetTop = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val Full = CircleShape
}

val MaterialShapes = Shapes(
    extraSmall = AppShapes.Xs,
    small = AppShapes.Sm,
    medium = AppShapes.Md,
    large = AppShapes.Lg,
    extraLarge = RoundedCornerShape(32.dp),
)
```

### `ui/theme/Spacing.kt`
```kotlin
package com.example.app.ui.theme

import androidx.compose.ui.unit.dp

object Spacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 24.dp
    val ScreenH = 16.dp
}
```

### `ui/theme/Type.kt`
```kotlin
package com.example.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Ganti dengan Plus Jakarta Sans (Google Fonts provider) bila diinginkan.
private val AppFont = FontFamily.SansSerif

val AppTypography = Typography(
    titleLarge = TextStyle(fontFamily = AppFont, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontFamily = AppFont, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = AppFont, fontSize = 14.sp, fontWeight = FontWeight.Medium),
    bodyMedium = TextStyle(fontFamily = AppFont, fontSize = 13.sp, fontWeight = FontWeight.Normal),
    labelMedium = TextStyle(fontFamily = AppFont, fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontFamily = AppFont, fontSize = 10.sp, fontWeight = FontWeight.Medium),
    headlineSmall = TextStyle(fontFamily = AppFont, fontSize = 20.sp, fontWeight = FontWeight.Bold),
)
```

### `ui/theme/Theme.kt`
```kotlin
package com.example.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = SurfaceWhite,
    primaryContainer = PrimarySoft,
    onPrimaryContainer = PrimaryDark,
    background = BgTop,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceTint,
    onSurfaceVariant = TextSecondary,
    outline = OutlineSoft,
    error = Danger,
    errorContainer = DangerBg,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        shapes = MaterialShapes,
        content = content,
    )
}
```

---

## B. Komponen

### Background layar
```kotlin
@Composable
fun GradientScreen(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(ScreenBackground), content = content)
}
```

### Kartu putih
```kotlin
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    shape: Shape = AppShapes.Md,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .shadow(2.dp, shape, ambientColor = Primary.copy(alpha = 0.08f), spotColor = Primary.copy(alpha = 0.08f))
            .background(SurfaceWhite.copy(alpha = 0.95f), shape)
            .border(1.dp, OutlineSoft, shape),
        content = content,
    )
}
```

### Tombol
```kotlin
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.height(48.dp),
        shape = AppShapes.Lg,
        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = SurfaceWhite),
    ) { Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick, modifier = modifier.height(48.dp), shape = AppShapes.Lg,
        border = BorderStroke(1.dp, OutlineSoft),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceWhite, contentColor = TextPrimary),
    ) { Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold) }
}
```

### Badge status (Paid / Unpaid)
```kotlin
enum class PaymentStatus { Paid, Unpaid }

@Composable
fun StatusBadge(status: PaymentStatus, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (status) {
        PaymentStatus.Paid -> Triple(SuccessBg, Success, "Paid")
        PaymentStatus.Unpaid -> Triple(DangerBg, Danger, "Unpaid")
    }
    Text(
        label, modifier = modifier.background(bg, AppShapes.Xs).padding(horizontal = 8.dp, vertical = 2.dp),
        color = fg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
    )
}
```

### Search field
```kotlin
@Composable
fun SearchField(
    value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    placeholder: String = "Search dishes by name...", trailing: @Composable (() -> Unit)? = null,
) {
    TextField(
        value = value, onValueChange = onValueChange, singleLine = true,
        modifier = modifier.fillMaxWidth().height(52.dp),
        placeholder = { Text(placeholder, color = TextSecondary, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = TextSecondary) },
        trailingIcon = trailing,
        shape = AppShapes.Lg,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = SurfaceWhite, unfocusedContainerColor = SurfaceWhite,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}
```

### Chip kategori
```kotlin
@Composable
fun CategoryChips(items: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = Spacing.ScreenH)) {
        items(items, key = { it }) { label ->
            val active = label == selected
            Text(
                label,
                modifier = Modifier
                    .clip(AppShapes.Lg)
                    .background(if (active) Primary else SurfaceWhite)
                    .clickable { onSelect(label) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = if (active) SurfaceWhite else TextPrimary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
```

### Stat card (laporan)
```kotlin
@Composable
fun StatCard(title: String, value: String, caption: String, icon: ImageVector, modifier: Modifier = Modifier) {
    SoftCard(modifier, shape = AppShapes.Md) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(28.dp).background(PrimarySoft, AppShapes.Sm), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Primary, modifier = Modifier.size(16.dp))
                }
                Text(title, color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            }
            Text(value, color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
            Text(caption, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
    }
}
```

### Baris transaksi
```kotlin
@Composable
fun TransactionRow(title: String, subtitle: String, amount: String, status: PaymentStatus, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(amount, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            StatusBadge(status)
        }
    }
}
```

### Bottom navigation bulat
```kotlin
data class NavItem(val label: String, val icon: ImageVector)

@Composable
fun AppBottomNav(items: List<NavItem>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth()
            .shadow(4.dp, AppShapes.Lg).background(SurfaceWhite, AppShapes.Lg).padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, item ->
            val active = i == selectedIndex
            Row(
                Modifier.clip(AppShapes.Full)
                    .background(if (active) Primary else SurfaceTint)
                    .clickable { onSelect(i) }
                    .padding(horizontal = if (active) 16.dp else 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(item.icon, item.label, tint = if (active) SurfaceWhite else TextSecondary, modifier = Modifier.size(20.dp))
                if (active) Text(item.label, color = SurfaceWhite, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
```

### Baris pengaturan
```kotlin
@Composable
fun SettingsRow(icon: ImageVector, label: String, value: String? = null, onClick: () -> Unit = {}, trailing: @Composable (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
        Text(label, Modifier.weight(1f), color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
        if (value != null) Text(value, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        trailing?.invoke() ?: Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary)
    }
}
```

---

## C. Contoh Layar

### Sales Report (stateless)
```kotlin
data class ReportUiState(
    val totalSales: String, val totalAmount: String, val received: String, val unpaid: String,
    val transactions: List<TxUi>,
)
data class TxUi(val id: String, val name: String, val amount: String, val status: PaymentStatus)

@Composable
fun SalesReportScreen(state: ReportUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    GradientScreen(modifier) {
        LazyColumn(
            contentPadding = PaddingValues(Spacing.ScreenH, 16.dp, Spacing.ScreenH, 96.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.Md),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
                    Text("Sales Report", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.size(48.dp))
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Md)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Md)) {
                        StatCard("Total Sales", state.totalSales, "Vs last month", Icons.Outlined.Receipt, Modifier.weight(1f))
                        StatCard("Total Amount", state.totalAmount, "Vs last month", Icons.Outlined.Payments, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Md)) {
                        StatCard("Received", state.received, "Vs last month", Icons.Outlined.CheckCircle, Modifier.weight(1f))
                        StatCard("Unpaid", state.unpaid, "Vs last month", Icons.Outlined.Pending, Modifier.weight(1f))
                    }
                }
            }
            item { Text("Transaction Lists", style = MaterialTheme.typography.titleMedium) }
            items(state.transactions, key = { it.id }) { tx ->
                SoftCard(Modifier.fillMaxWidth()) {
                    Box(Modifier.padding(horizontal = 12.dp)) {
                        TransactionRow(tx.name, "", tx.amount, tx.status)
                    }
                }
            }
        }
    }
}
```

### Pola lain (ringkas)
- **Cart sheet**: `ModalBottomSheet(shape = AppShapes.SheetTop, containerColor = SurfaceWhite)`; header `Row` dengan `Text("2 Items")` dan `TextButton` berwarna `Danger`.
- **Confirm Sale**: `Column(verticalScroll, imePadding)`, field memakai `OutlinedTextField` dengan `shape = AppShapes.Sm`, border `OutlineSoft`, fokus `Primary`; tombol aksi kecil kanan memakai `AssistChip`-style (background `SurfaceTint`, radius `Sm`).
- **Type picker**: `LazyVerticalGrid(GridCells.Fixed(3))`; tile terpilih `border(1.5.dp, Primary)` + `background(PrimarySoft)`.
- **Action bar laporan**: `Row` 4 item sama lebar (ikon + label `labelSmall`) di dalam `SoftCard(AppShapes.Lg)`.
