package com.kapos.mobile.core.theme

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
