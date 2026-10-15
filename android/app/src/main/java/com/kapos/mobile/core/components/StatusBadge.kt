package com.kapos.mobile.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kapos.mobile.core.theme.AppShapes
import com.kapos.mobile.core.theme.Danger
import com.kapos.mobile.core.theme.DangerBg
import com.kapos.mobile.core.theme.Info
import com.kapos.mobile.core.theme.InfoBg
import com.kapos.mobile.core.theme.Success
import com.kapos.mobile.core.theme.SuccessBg
import com.kapos.mobile.core.theme.Warning

enum class BadgeType {
    SUCCESS,
    DANGER,
    WARNING,
    INFO
}

@Composable
fun StatusBadge(
    text: String,
    type: BadgeType,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (type) {
        BadgeType.SUCCESS -> SuccessBg to Success
        BadgeType.DANGER -> DangerBg to Danger
        BadgeType.WARNING -> Color(0xFFFFF4E5) to Warning
        BadgeType.INFO -> InfoBg to Info
    }

    Box(
        modifier = modifier
            .clip(AppShapes.Xs)
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
