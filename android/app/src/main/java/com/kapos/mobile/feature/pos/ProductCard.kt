package com.kapos.mobile.feature.pos

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kapos.mobile.core.theme.OutlineSoft
import com.kapos.mobile.core.theme.Primary
import com.kapos.mobile.core.theme.PrimarySoft
import com.kapos.mobile.core.theme.SurfaceTint
import com.kapos.mobile.core.theme.SurfaceWhite
import com.kapos.mobile.core.theme.TextPrimary
import com.kapos.mobile.core.theme.TextSecondary
import com.kapos.mobile.core.utils.CurrencyFormatter
import com.kapos.mobile.domain.model.Product

@Composable
fun ProductCard(
    product: Product,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    categoryName: String? = null
) {
    val categoryLabel = categoryName?.uppercase() ?: when (product.categoryId) {
        "cat_taichan" -> "SATE TAICHAN"
        "cat_chicken" -> "AYAM GORENG"
        "cat_drinks" -> "MINUMAN"
        "cat_sides" -> "EKSTRA"
        else -> "MENU"
    }

    val isSoldOut = product.isSoldOut

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSoldOut) SurfaceTint.copy(alpha = 0.6f) else SurfaceWhite)
            .border(1.dp, if (isSoldOut) OutlineSoft else OutlineSoft, RoundedCornerShape(14.dp))
            .clickable(enabled = !isSoldOut, onClick = onAddClick)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Category Tag & Spice / SoldOut Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSoldOut) OutlineSoft else SurfaceTint)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = categoryLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSoldOut) TextSecondary else Primary
                        )
                    )
                }

                if (isSoldOut) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(com.kapos.mobile.core.theme.Danger)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "HABIS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SurfaceWhite
                            )
                        )
                    }
                } else if (product.spiceLevel.isNotBlank()) {
                    Text(
                        text = "Level 0–5",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSoldOut) TextSecondary else TextPrimary
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description / Subtitle
            val desc = if (isSoldOut) {
                "Bahan baku sedang habis"
            } else if (product.description.isNotBlank()) {
                product.description
            } else if (product.spiceLevel.isNotBlank()) {
                "Pilihan rasa pedas gurih"
            } else {
                "Porsi segar siap saji"
            }
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = TextSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Price & Add Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = CurrencyFormatter.formatRupiah(product.price),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = if (isSoldOut) TextSecondary else Primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isSoldOut) OutlineSoft else Primary)
                        .clickable(enabled = !isSoldOut, onClick = onAddClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah ${product.name}",
                        tint = SurfaceWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
