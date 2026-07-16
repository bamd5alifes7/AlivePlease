package com.orenhui.aliveplease.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.orenhui.aliveplease.R
import com.orenhui.aliveplease.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val CANDLE_COUNT = 3

// 蠟燭色彩 — 配合 App 的翡翠綠 / 琥珀金 / 藍色系
private val CandleColors = listOf(
    Color(0xFF00C896),   // 翡翠綠（PrimaryGreen）
    Color(0xFFFFB830),   // 琥珀金（AccentAmber）
    Color(0xFF5BB5E8)    // 天藍
)
private val CandleStripeAlpha = 0.3f

// 火焰
private val FlameInner   = Color(0xFFFFEE88)
private val FlameOuter   = Color(0xFFFF9922)
private val FlameGlow    = Color(0x40FFCC44)
private val SmokeColor   = Color(0xFFBBBBBB)

// Confetti — 配合 App 色系
private val ConfettiColors = listOf(
    Color(0xFF00C896), Color(0xFFFFB830), Color(0xFF5BB5E8),
    Color(0xFF88DDAA), Color(0xFFFFD56B), Color(0xFF8BAAC5)
)

// ─────────────────────────────────────────────
// 生日彩蛋 Dialog
// ─────────────────────────────────────────────

@Composable
fun BirthdayEasterEggDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val wishes = remember {
        context.resources.getStringArray(R.array.birthday_wishes).toList().shuffled().take(CANDLE_COUNT)
    }
    var blownCount by remember { mutableIntStateOf(0) }
    var isAnimating by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val blowThreshold = with(LocalDensity.current) { 72.dp.toPx() }

    val allBlown = blownCount >= CANDLE_COUNT
    val blowProgress = (-dragOffset / blowThreshold).coerceIn(0f, 1f)

    fun blowNext() {
        if (isAnimating || allBlown) return
        isAnimating = true
        blownCount++
        dragOffset = 0f
        scope.launch {
            delay(800)
            isAnimating = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        BackHandler(onBack = onDismiss)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A0F1E),
                            Color(0xFF112233),
                            Color(0xFF0A0F1E)
                        )
                    )
                )
        ) {
            // 關閉按鈕（右上角）
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.birthday_close_button),
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 標題
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (allBlown) stringResource(R.string.birthday_easter_egg_title)
                               else "🎂 生日快樂",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AnimatedVisibility(visible = !allBlown) {
                        Text(
                            text = stringResource(
                                when (blownCount) {
                                    0 -> R.string.birthday_wish_prompt_1
                                    1 -> R.string.birthday_wish_prompt_2
                                    else -> R.string.birthday_wish_prompt_3
                                }
                            ),
                            fontSize = 14.sp,
                            color = AppColors.TextHint,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 蛋糕 + 蠟燭
                BirthdayCakeScene(
                    blownCount = blownCount,
                    selectedWishes = wishes,
                    blowProgress = blowProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(blownCount, isAnimating) {
                            var thresholdCrossed = false
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    dragOffset = 0f
                                    thresholdCrossed = false
                                },
                                onDragCancel = { dragOffset = 0f },
                                onDragEnd = {
                                    if (thresholdCrossed) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        blowNext()
                                    } else {
                                        dragOffset = 0f
                                    }
                                },
                                onHorizontalDrag = { change, amount ->
                                    if (!isAnimating && !allBlown) {
                                        change.consume()
                                        dragOffset = (dragOffset + amount).coerceAtMost(0f)
                                        if (!thresholdCrossed && -dragOffset >= blowThreshold) {
                                            thresholdCrossed = true
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                }
                            )
                        }
                )

                // ── 底部操作區 ──
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 全部吹熄後的祝福文字
                    AnimatedVisibility(
                        visible = allBlown,
                        enter = fadeIn(tween(400)) + scaleIn(tween(400))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.birthday_candle_blown_all),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.AccentAmber,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.birthday_blessing_message),
                                fontSize = 14.sp,
                                color = AppColors.TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 21.sp
                            )
                        }
                    }

                    if (!allBlown) {
                        Text(
                            text = stringResource(R.string.birthday_swipe_hint),
                            fontSize = 13.sp,
                            color = AppColors.TextHint,
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(
                            onClick = { blowNext() },
                            enabled = !isAnimating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = AppColors.TextSecondary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    when (blownCount) {
                                        0 -> R.string.birthday_blow_candle_button_1
                                        1 -> R.string.birthday_blow_candle_button_2
                                        else -> R.string.birthday_blow_candle_button_3
                                    }
                                ),
                                fontSize = 14.sp
                            )
                        }
                    }

                    if (allBlown) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.PrimaryGreen.copy(alpha = 0.85f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.birthday_return_button),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                    }
                }
            }

            // Confetti 粒子
            if (allBlown) {
                ConfettiOverlay()
            }
        }
    }
}

// ─────────────────────────────────────────────
// 蛋糕 + 蠟燭場景
// ─────────────────────────────────────────────

@Composable
private fun BirthdayCakeScene(
    blownCount: Int,
    selectedWishes: List<String>,
    blowProgress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cakeFloat")
    val cakeOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cakeFloat"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .offset { IntOffset(0, cakeOffsetY.toInt()) }
        ) {
            Image(
                painter = painterResource(R.drawable.birthday_cake_base),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            val candleSpacing = (maxWidth * (154f / 1254f) - 22.dp).coerceAtLeast(10.dp)
            val candleTop = (maxWidth * (606f / 1254f) - 104.dp).coerceAtLeast(0.dp)

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = candleTop),
                horizontalArrangement = Arrangement.spacedBy(candleSpacing),
                verticalAlignment = Alignment.Bottom
            ) {
                repeat(CANDLE_COUNT) { index ->
                    val isBlown = index < blownCount
                    CandleOnly(
                        candleIndex = index,
                        isBlown = isBlown,
                        blowProgress = if (index == blownCount) blowProgress else 0f,
                        modifier = Modifier.graphicsLayer {
                            alpha = if (index > blownCount) 0.55f else 1f
                        }
                    )
                }
            }

            repeat(CANDLE_COUNT) { index ->
                val visible = index < blownCount
                val startX = maxWidth * (154f / 1254f) * (index - 1).toFloat()
                val finalX = 88.dp * (index - 1).toFloat()
                val wordX by animateDpAsState(
                    targetValue = if (visible) finalX else startX,
                    animationSpec = tween(500, delayMillis = 180),
                    label = "blessingX$index"
                )
                val wordY by animateDpAsState(
                    targetValue = if (visible) 0.dp else candleTop,
                    animationSpec = tween(500, delayMillis = 180),
                    label = "blessingY$index"
                )
                val wordAlpha by animateFloatAsState(
                    targetValue = if (visible) 1f else 0f,
                    animationSpec = tween(450, delayMillis = 180),
                    label = "blessingAlpha$index"
                )
                val wordColor by animateColorAsState(
                    targetValue = if (visible) AppColors.AccentAmber else SmokeColor,
                    animationSpec = tween(450, delayMillis = 180),
                    label = "blessingColor$index"
                )

                Text(
                    text = selectedWishes.getOrNull(index).orEmpty(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = wordColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = wordX, y = wordY)
                        .width(88.dp)
                        .alpha(wordAlpha)
                )
            }
        }

    }
}

// ─────────────────────────────────────────────
// #4: 改善單根蠟燭 + 願望文字
// ─────────────────────────────────────────────

@Composable
private fun CandleOnly(
    candleIndex: Int,
    isBlown: Boolean,
    blowProgress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "flame$candleIndex")
    val phaseOffset = candleIndex * 350
    val flameSwayX by infiniteTransition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(550 + phaseOffset, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flameSway$candleIndex"
    )
    val flameScaleY by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(450 + phaseOffset, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flameScale$candleIndex"
    )

    val flameAlpha by animateFloatAsState(
        targetValue = if (isBlown) 0f else 1f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "flameAlpha$candleIndex"
    )
    val smokeProgress = remember { Animatable(0f) }
    LaunchedEffect(isBlown) {
        smokeProgress.snapTo(0f)
        if (isBlown) {
            smokeProgress.animateTo(1f, tween(650, easing = LinearEasing))
        }
    }

    val candleColor = CandleColors[candleIndex % CandleColors.size]

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // 煙霧（吹熄後短暫出現）
        Box(
            modifier = Modifier
                .size(width = 10.dp, height = 18.dp)
                .drawWithContent {
                    drawContent()
                    val smokeAlpha = if (smokeProgress.value < 0.25f) {
                        smokeProgress.value / 0.25f
                    } else {
                        (1f - smokeProgress.value) / 0.75f
                    }
                    if (smokeAlpha > 0.01f) {
                        for (i in 0..2) {
                            drawCircle(
                                color = SmokeColor.copy(
                                    alpha = (smokeAlpha * (0.35f - i * 0.10f)).coerceAtLeast(0f)
                                ),
                                radius = 5f - i * 1f,
                                center = Offset(
                                    size.width / 2f,
                                    size.height - i * 7f - smokeProgress.value * 12f
                                )
                            )
                        }
                    }
                }
        )

        // 火焰 + 發光
        Box(
            modifier = Modifier
                .size(width = 22.dp, height = 30.dp)
                .graphicsLayer {
                    translationX = -10.dp.toPx() * blowProgress
                    rotationZ = -28f * blowProgress
                    scaleY = 1f - 0.35f * blowProgress
                    alpha = 1f - 0.35f * blowProgress
                }
                .drawWithContent {
                    drawContent()
                    if (flameAlpha > 0.01f) {
                        val cx = size.width / 2f + flameSwayX
                        val cy = size.height * 0.50f

                        // 發光暈圈
                        drawCircle(
                            color = FlameGlow.copy(alpha = flameAlpha * 0.5f),
                            radius = 16f * flameScaleY,
                            center = Offset(cx, cy)
                        )
                        // 外焰
                        drawOval(
                            color = FlameOuter.copy(alpha = flameAlpha * 0.85f),
                            topLeft = Offset(cx - 7f, cy - 14f * flameScaleY),
                            size = Size(14f, 24f * flameScaleY)
                        )
                        // 內焰
                        drawOval(
                            color = FlameInner.copy(alpha = flameAlpha),
                            topLeft = Offset(cx - 4f, cy - 9f * flameScaleY),
                            size = Size(8f, 15f * flameScaleY)
                        )
                    }
                }
        )

        // 燭芯
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(6.dp)
                .background(Color(0xFF444444))
        )

        // 蠟燭本體
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(50.dp)
                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            candleColor,
                            candleColor.copy(alpha = 0.75f)
                        )
                    )
                )
                .drawWithContent {
                    drawContent()
                    // 條紋裝飾
                    for (i in 0..3) {
                        val y = size.height * 0.15f + i * (size.height * 0.22f)
                        drawRoundRect(
                            color = Color.White.copy(alpha = CandleStripeAlpha),
                            topLeft = Offset(0f, y),
                            size = Size(size.width, 3f),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                }
                .border(
                    0.5.dp,
                    Color.White.copy(alpha = 0.25f),
                    RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 2.dp, bottomEnd = 2.dp)
                )
        )
    }
}

// ─────────────────────────────────────────────
// Confetti 粒子（純 Compose）
// ─────────────────────────────────────────────

private data class ConfettiParticle(
    val x: Float,
    val vx: Float,
    val color: Color,
    val size: Float,
    val rotationSpeed: Float,
    val phase: Float
)

@Composable
private fun ConfettiOverlay() {
    val particles = remember {
        List(60) {
            ConfettiParticle(
                x = Random.nextFloat(),
                vx = (Random.nextFloat() - 0.5f) * 0.3f,
                color = ConfettiColors[Random.nextInt(ConfettiColors.size)],
                size = 6f + Random.nextFloat() * 8f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 720f,
                phase = Random.nextFloat()
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "confetti")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confettiTime"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                particles.forEach { p ->
                    val t = (time + p.phase) % 1f
                    val px = (p.x + p.vx * t) * size.width
                    val py = -p.size + t * size.height * 1.1f
                    val rotation = p.rotationSpeed * t

                    rotate(rotation, pivot = Offset(px, py)) {
                        drawRect(
                            color = p.color.copy(alpha = (1f - t * 0.5f).coerceIn(0f, 1f)),
                            topLeft = Offset(px - p.size / 2f, py - p.size / 4f),
                            size = Size(p.size, p.size / 2f)
                        )
                    }
                }
            }
    )
}

// ─────────────────────────────────────────────
// #2: 首頁邀請 Banner — 用 App 主題色
// ─────────────────────────────────────────────

@Composable
fun BirthdayInvitationBanner(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        AppColors.SurfaceMid.copy(alpha = 0.95f),
                        AppColors.SurfaceDark.copy(alpha = 0.92f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        AppColors.AccentAmber.copy(alpha = 0.5f),
                        AppColors.PrimaryGreen.copy(alpha = 0.3f)
                    )
                ),
                RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(AppColors.AccentAmberGlow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎂", fontSize = 28.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.birthday_invitation_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.birthday_invitation_message),
                        fontSize = 13.sp,
                        color = AppColors.TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOpen,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.AccentAmber.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.birthday_invitation_open),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
