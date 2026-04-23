package com.example.aliveplease.ui

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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aliveplease.R
import com.example.aliveplease.ui.theme.AppColors
import com.example.aliveplease.utils.TimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(factory = MainViewModel.factory(context))
    val uiState = viewModel.uiState
    var buttonPressed by remember { mutableStateOf(false) }

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
        viewModel.refreshCareMessage()
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
                            text = stringResource(R.string.today_care_title),
                            fontSize = 13.sp,
                            color = AppColors.TextHint,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.careMessage,
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
                            viewModel.performCheckIn()
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
                                    text = stringResource(R.string.main_heart_emoji),
                                    fontSize = 30.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.check_in_button),
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
                        title = stringResource(R.string.alive_days_label),
                        value = stringResource(R.string.alive_days_summary, uiState.aliveDays),
                        supportingText = stringResource(R.string.alive_days_supporting)
                    )
                    StatPill(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.streak_days_label),
                        value = stringResource(R.string.streak_days_summary, uiState.streakDays),
                        supportingText = stringResource(
                            R.string.blessing_progress_status_short,
                            uiState.daysUntilNextBlessing
                        )
                    )
                }

                AnimatedVisibility(
                    visible = uiState.showCelebration,
                    enter = fadeIn(animationSpec = tween(250)) + scaleIn(
                        initialScale = 0.92f,
                        animationSpec = tween(250)
                    ),
                    exit = fadeOut(animationSpec = tween(250)) + scaleOut(
                        targetScale = 0.95f,
                        animationSpec = tween(250)
                    )
                ) {
                    CelebrationCard(
                        streakDays = uiState.streakDays,
                        message = uiState.celebrationMessage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                    )
                }

                if (uiState.showCheckInFeedback) {
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
                            text = stringResource(R.string.check_in_success_with_emoji),
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
                            Text(stringResource(R.string.countdown_emoji), fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.family_notify_countdown_label),
                                fontSize = 12.sp,
                                color = AppColors.TextHint,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = TimeFormatter.formatCountdown(context, uiState.countdown),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.AccentAmber
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                com.example.aliveplease.ui.components.CalendarView(
                    checkInDates = uiState.checkInDates
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
    value: String,
    supportingText: String? = null
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
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier.height(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!supportingText.isNullOrBlank()) {
                    Text(
                        text = supportingText,
                        fontSize = 11.sp,
                        color = AppColors.TextHint,
                        textAlign = TextAlign.Center
                    )
                }
            }
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
                text = stringResource(R.string.celebration_title, streakDays),
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
