package com.kapos.mobile.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kapos.mobile.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MotionSplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier,
    splashDurationMillis: Long = 2300L
) {
    // Entrance animations
    val scale = remember { Animatable(0.2f) }
    val alpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(30f) }

    // Loading status text
    var statusText by remember { mutableStateOf("Menyiapkan Sistem POS...") }

    // Continuous running mascot motion (infinite transition)
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_motion")

    // Running bobbing bounce (vertical hop)
    val bobbingOffset by infiniteTransition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_bob"
    )

    // Mascot tilt wobble (running sway)
    val swayRotation by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_tilt"
    )

    // Ambient ripple / radar wave expanding behind logo
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_scale"
    )

    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_alpha"
    )

    // Shimmer bar progress animation
    val progressShimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress_shimmer"
    )

    // Orchestrate entrance sequence
    LaunchedEffect(Unit) {
        // Parallel launch for smooth spring entrance
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350)
            )
        }
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        val phase1 = (splashDurationMillis * 0.15f).toLong()
        val phase2 = (splashDurationMillis * 0.35f).toLong()
        val phase3 = (splashDurationMillis * 0.30f).toLong()
        val phase4 = (splashDurationMillis - phase1 - phase2 - phase3).coerceAtLeast(200L)

        // Text reveal shortly after logo pops in
        delay(phase1)
        launch {
            textAlpha.animateTo(1f, animationSpec = tween(400))
        }
        launch {
            textOffsetY.animateTo(0f, animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium))
        }

        delay(phase2)
        statusText = "Sinkronisasi Menu & Kasir..."

        delay(phase3)
        statusText = "Siap Digunakan!"

        // Small finish delay then callback
        delay(phase4)
        onSplashFinished()
    }

    // Rich Red & Amber Glow Background Gradient
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE50914), // Bold vibrant red
            Color(0xFFC00014), // Rich crimson
            Color(0xFF85000C)  // Deep ember red
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        // Background animated ripple pulse
        Canvas(modifier = Modifier.size(340.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension / 2.6f

            // Outer expanding ripple
            drawCircle(
                color = Color.White.copy(alpha = rippleAlpha * 0.35f),
                radius = baseRadius * rippleScale,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // Inner soft ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD54F).copy(alpha = 0.25f),
                        Color(0xFFFF5252).copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.2f
                ),
                radius = baseRadius * 1.2f,
                center = center
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Mascot Logo with Motion
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(scale.value)
                    .alpha(alpha.value)
                    .offset { IntOffset(x = 0, y = bobbingOffset.dp.roundToPx()) }
                    .rotate(swayRotation)
            ) {
                // Ground dynamic shadow beneath bouncing mascot
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 14.dp)
                        .width((140 - (bobbingOffset * 2)).dp)
                        .height(14.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = (0.28f - (bobbingOffset * 0.01f)).coerceIn(0.12f, 0.40f)))
                )

                // Logo Container with glowing border
                Box(
                    modifier = Modifier
                        .size(176.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(32.dp),
                            spotColor = Color(0xFF4A0004),
                            ambientColor = Color(0xFFFF8A80)
                        )
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White)
                        .border(
                            width = 3.dp,
                            color = Color(0xFFFFE082).copy(alpha = 0.85f),
                            shape = RoundedCornerShape(32.dp)
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "Logo Taichan & Chicken KA",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Branding text animated slide & fade
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .offset { IntOffset(x = 0, y = textOffsetY.value.dp.roundToPx()) }
            ) {
                Text(
                    text = "TAICHAN & CHICKEN KA",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.2.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Tagline badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "POINT OF SALE • OUTLET SYSTEM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFECB3),
                        letterSpacing = 1.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Shimmer progress bar
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(6.dp)
                    .alpha(textAlpha.value)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f))
            ) {
                val shimmerBrush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFFFC107).copy(alpha = 0.2f),
                        Color(0xFFFFF9C4),
                        Color(0xFFFFC107).copy(alpha = 0.2f)
                    ),
                    startX = progressShimmer * 400f - 150f,
                    endX = progressShimmer * 400f + 150f
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(shimmerBrush)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic loading status indicator
            Text(
                text = statusText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.alpha(textAlpha.value)
            )
        }
    }
}
