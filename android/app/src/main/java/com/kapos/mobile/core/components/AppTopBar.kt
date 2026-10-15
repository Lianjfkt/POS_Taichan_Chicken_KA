package com.kapos.mobile.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kapos.mobile.core.theme.Primary
import com.kapos.mobile.core.theme.PrimaryDark
import com.kapos.mobile.core.theme.PrimarySoft
import com.kapos.mobile.core.theme.Success
import com.kapos.mobile.core.theme.TextPrimary
import com.kapos.mobile.core.theme.TextSecondary
import com.kapos.mobile.core.theme.Warning

@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    isOnline: Boolean = true,
    isSyncing: Boolean = false,
    savedOrdersCount: Int = 0,
    onSavedOrdersClick: () -> Unit = {},
    onSyncClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🍢",
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = PrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Status Indicator
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isOnline) Color(0xFFE8F7EE) else Color(0xFFFFF4E5))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = "Connection Status",
                        tint = if (isOnline) Success else Warning,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isOnline) "Online" else "Offline",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isOnline) Success else Warning,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            // Saved Orders (Hold) Button
            if (savedOrdersCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFFFF3CD))
                        .clickable(onClick = onSavedOrdersClick)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Saved Orders",
                            tint = Color(0xFF856404),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$savedOrdersCount Hold",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF856404),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            IconButton(
                onClick = onSyncClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PrimarySoft)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync",
                    tint = Primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
