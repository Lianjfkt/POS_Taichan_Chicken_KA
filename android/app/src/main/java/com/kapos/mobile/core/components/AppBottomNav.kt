package com.kapos.mobile.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kapos.mobile.core.theme.OutlineSoft
import com.kapos.mobile.core.theme.Primary
import com.kapos.mobile.core.theme.PrimarySoft
import com.kapos.mobile.core.theme.SurfaceTint
import com.kapos.mobile.core.theme.SurfaceWhite
import com.kapos.mobile.core.theme.TextSecondary
import com.kapos.mobile.domain.model.UserRole

sealed class NavDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Pos : NavDestination("pos", "Kasir", Icons.Default.PointOfSale)
    object Transactions : NavDestination("transactions", "Transaksi", Icons.Default.ReceiptLong)
    object Shift : NavDestination("shift", "Shift", Icons.Default.WorkHistory)
    object Stock : NavDestination("stock", "Stok", Icons.Default.List)
    object Analytics : NavDestination("analytics", "Analisa", Icons.Default.BarChart)
    object Dashboard : NavDestination("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Report : NavDestination("report", "Laporan", Icons.Default.Assessment)
    object Settings : NavDestination("settings", "Pengaturan", Icons.Default.Settings)
}

fun getBottomNavDestinations(role: UserRole): List<NavDestination> {
    return when (role) {
        UserRole.CASHIER -> listOf(
            NavDestination.Pos,
            NavDestination.Transactions,
            NavDestination.Shift
        )
        UserRole.OWNER -> listOf(
            NavDestination.Dashboard,
            NavDestination.Pos,
            NavDestination.Transactions,
            NavDestination.Shift,
            NavDestination.Stock,
            NavDestination.Settings
        )
    }
}

val allBottomNavDestinations = listOf(
    NavDestination.Pos,
    NavDestination.Transactions,
    NavDestination.Shift,
    NavDestination.Stock,
    NavDestination.Analytics,
    NavDestination.Report,
    NavDestination.Settings
)

@Composable
fun AppBottomNav(
    currentRoute: String,
    role: UserRole,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val destinations = getBottomNavDestinations(role)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(elevation = 12.dp, shape = CircleShape, spotColor = Primary.copy(alpha = 0.15f))
                .clip(CircleShape)
                .background(SurfaceWhite)
                .border(1.dp, OutlineSoft, CircleShape)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                destinations.forEach { dest ->
                    val isSelected = currentRoute == dest.route
                    BottomNavItem(
                        item = dest,
                        isSelected = isSelected,
                        onClick = { onNavigate(dest.route) }
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavItem(
    item: NavDestination,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) Primary else SurfaceTint)
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (isSelected) 14.dp else 10.dp,
                vertical = 8.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = if (isSelected) SurfaceWhite else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = SurfaceWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}
