package com.kapos.mobile.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kapos.mobile.core.theme.OutlineSoft
import com.kapos.mobile.core.theme.Primary
import com.kapos.mobile.core.theme.SurfaceWhite
import com.kapos.mobile.core.theme.TextPrimary
import com.kapos.mobile.core.theme.TextSecondary
import com.kapos.mobile.domain.model.DateFilterPreset
import com.kapos.mobile.domain.model.DateRangeFilter

@Composable
fun DateFilterBar(
    currentFilter: DateRangeFilter,
    onPresetSelected: (DateFilterPreset) -> Unit,
    onOpenDatePicker: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp)
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = contentPadding
    ) {
        val standardPresets = listOf(
            DateFilterPreset.TODAY,
            DateFilterPreset.LAST_7_DAYS,
            DateFilterPreset.THIS_MONTH,
            DateFilterPreset.ALL
        )

        items(standardPresets.size) { index ->
            val preset = standardPresets[index]
            val isSelected = currentFilter.preset == preset
            DateFilterChipItem(
                label = preset.displayName,
                isSelected = isSelected,
                onClick = { onPresetSelected(preset) }
            )
        }

        item {
            val isCustomSelected = currentFilter.preset == DateFilterPreset.CUSTOM
            val customLabel = if (isCustomSelected && currentFilter.customStartMillis != null) {
                currentFilter.getDisplayLabel()
            } else {
                "Pilih Tanggal"
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isCustomSelected) Primary else SurfaceWhite)
                    .border(
                        width = 1.dp,
                        color = if (isCustomSelected) Primary else OutlineSoft,
                        shape = CircleShape
                    )
                    .clickable(onClick = onOpenDatePicker)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Pilih Tanggal",
                        tint = if (isCustomSelected) SurfaceWhite else TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = customLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            color = if (isCustomSelected) SurfaceWhite else TextPrimary,
                            fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun DateFilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) Primary else SurfaceWhite)
            .border(
                width = 1.dp,
                color = if (isSelected) Primary else OutlineSoft,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                color = if (isSelected) SurfaceWhite else TextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        )
    }
}
