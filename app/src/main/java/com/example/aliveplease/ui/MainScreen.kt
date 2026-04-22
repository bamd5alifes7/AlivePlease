package com.example.aliveplease.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aliveplease.R
import com.example.aliveplease.data.AppDataStore
import com.example.aliveplease.ui.theme.AppColors
import com.example.aliveplease.utils.TimeFormatter
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    dataStore: AppDataStore,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current

    var aliveDays by remember { mutableStateOf(dataStore.getAliveDays()) }
    var streakDays by remember { mutableStateOf(dataStore.getCurrentStreak()) }
    var checkInDates by remember { mutableStateOf(dataStore.getCheckInDates()) }
    var countdown by remember { mutableStateOf(dataStore.getTimeUntilFamilyNotification()) }
    var careMessage by remember { mutableStateOf(getCareMessage(context)) }
    var showCheckInFeedback by remember { mutableStateOf(false) }
    var buttonPressed by remember { mutableStateOf(false) }
    var celebrationMessage by remember { mutableStateOf("") }
    var showCelebration by remember { mutableStateOf(false) }

    val buttonScale by animateFloatAsState(
        targetValue = if (buttonPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "buttonScale"
    )

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue = 0.28f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            countdown = dataStore.getTimeUntilFamilyNotification()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = AppColors.PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = context.getString(R.string.main_screen_title),
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = context.getString(R.string.settings),
                            tint = AppColors.TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.SurfaceDark
                )
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AppColors.Background,
                            AppColors.SurfaceDark,
                            AppColors.Background
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "今天想對你說",
                            fontSize = 13.sp,
                            color = AppColors.TextHint,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = careMessage,
                            fontSize = 18.sp,
                            lineHeight = 28.sp,
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(220.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(AppColors.PrimaryGreen.copy(alpha = pulseAlpha))
                    )

                    Button(
                        onClick = {
                            buttonPressed = true
                            val isNewDay = dataStore.performCheckIn()
                            aliveDays = dataStore.getAliveDays()
                            streakDays = dataStore.getCurrentStreak()
                            checkInDates = dataStore.getCheckInDates()
                            countdown = dataStore.getTimeUntilFamilyNotification()
                            careMessage = getCareMessage(context)
                            showCheckInFeedback = true

                            if (isNewDay && streakDays > 0 && streakDays % 3 == 0) {
                                celebrationMessage = getCelebrationMessage(streakDays)
                                showCelebration = true
                            }

                            buttonPressed = false
                        },
                        modifier = Modifier
                            .size(190.dp)
                            .scale(buttonScale),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            AppColors.PrimaryGreen,
                                            AppColors.PrimaryGreenDim
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "❤️",
                                    fontSize = 30.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "我還活著",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatPill(
                        modifier = Modifier.weight(1f),
                        title = "累積活著天數",
                        value = "$aliveDays 天"
                    )
                    StatPill(
                        modifier = Modifier.weight(1f),
                        title = "連續打卡天數",
                        value = "$streakDays 天"
                    )
                }

                AnimatedVisibility(
                    visible = showCelebration,
                    enter = fadeIn(animationSpec = tween(250)) + scaleIn(
                        initialScale = 0.92f,
                        animationSpec = tween(250)
                    ),
                    exit = fadeOut(animationSpec = tween(250)) + scaleOut(
                        targetScale = 0.95f,
                        animationSpec = tween(250)
                    )
                ) {
                    LaunchedEffect(showCelebration) {
                        if (showCelebration) {
                            delay(6000)
                            showCelebration = false
                        }
                    }

                    CelebrationCard(
                        streakDays = streakDays,
                        message = celebrationMessage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                    )
                }

                if (showCheckInFeedback) {
                    LaunchedEffect(showCheckInFeedback) {
                        delay(2000)
                        showCheckInFeedback = false
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = AppColors.PrimaryGreen.copy(alpha = 0.18f),
                        modifier = Modifier.border(
                            1.dp,
                            AppColors.PrimaryGreen.copy(alpha = 0.4f),
                            RoundedCornerShape(50)
                        )
                    ) {
                        Text(
                            text = "🎉 今天的報平安已記下來了",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            fontSize = 15.sp,
                            color = AppColors.PrimaryGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AppColors.AccentAmberGlow),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⏳", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "距離通知親友還有",
                                fontSize = 12.sp,
                                color = AppColors.TextHint,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = TimeFormatter.formatCountdown(context, countdown),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.AccentAmber
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                com.example.aliveplease.ui.components.CalendarView(
                    checkInDates = checkInDates
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StatPill(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = AppColors.SurfaceMid.copy(alpha = 0.92f),
        modifier = modifier.border(
            width = 1.dp,
            color = AppColors.PrimaryGreen.copy(alpha = 0.22f),
            shape = RoundedCornerShape(999.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = AppColors.TextHint,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.PrimaryGreen
            )
        }
    }
}

@Composable
private fun CelebrationCard(
    streakDays: Int,
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        AppColors.AccentAmber.copy(alpha = 0.95f),
                        AppColors.PrimaryGreen.copy(alpha = 0.9f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "✨ 連續 $streakDays 天打卡",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        AppColors.SurfaceMid.copy(alpha = 0.85f),
                        AppColors.SurfaceLight.copy(alpha = 0.6f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.04f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        content()
    }
}

private fun getCareMessage(context: Context): String {
    val messages = context.resources.getStringArray(R.array.care_messages)
    return messages.random()
}

private fun getCelebrationMessage(streakDays: Int): String {
    return when (streakDays) {
        3 -> "你已經連續做到 3 天了，這不是小事，真的很棒。"
        6 -> "6 天連續打卡，代表你一直在努力照顧自己。"
        9 -> "9 天了，你正在慢慢把平安這件事變成習慣。"
        else -> "連續 $streakDays 天都沒有中斷，請把這份穩定也好好誇獎自己。"
    }
}
