package com.orenhui.aliveplease.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.orenhui.aliveplease.R
import com.orenhui.aliveplease.ui.theme.AppColors
import com.orenhui.aliveplease.utils.TimeFormatter
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    isVisible: Boolean = true,
    tutorialMode: Boolean = false,
    onTutorialNext: () -> Unit = {},
    onTutorialBack: () -> Unit = {},
    onTutorialClose: () -> Unit = {},
    exitFarewellEnabled: Boolean = false,
    onExitApp: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(factory = MainViewModel.factory(context))
    val uiState = viewModel.uiState
    var buttonPressed by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var farewellMessage by remember { mutableStateOf<String?>(null) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            viewModel.refreshCareMessage()
        }
    )

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

    LaunchedEffect(isVisible, tutorialMode) {
        if (isVisible && !tutorialMode) viewModel.onHomeVisible()
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(450)
            isRefreshing = false
        }
    }

    BackHandler(enabled = exitFarewellEnabled) {
        if (farewellMessage == null) {
            farewellMessage = context.resources.getStringArray(R.array.exit_farewell_messages).random()
        } else {
            onExitApp()
        }
    }

    LaunchedEffect(farewellMessage) {
        if (farewellMessage != null) {
            delay(EXIT_FAREWELL_DURATION_MILLIS)
            onExitApp()
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
                    IconButton(
                        onClick = onNavigateToSettings,
                        enabled = !tutorialMode
                    ) {
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
                .then(if (tutorialMode) Modifier else Modifier.pullRefresh(pullRefreshState))
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
                // 生日當天的首要內容
                AnimatedVisibility(
                    visible = uiState.isBirthdayToday && !tutorialMode,
                    enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                        initialScale = 0.95f,
                        animationSpec = tween(300)
                    ),
                    exit = fadeOut(animationSpec = tween(200))
                ) {
                    Column {
                        BirthdayInvitationBanner(
                            onOpen = viewModel::openBirthdayEasterEgg
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

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
                        enabled = !tutorialMode,
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

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    com.orenhui.aliveplease.ui.components.CalendarView(
                        checkInDates = uiState.checkInDates,
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            if (!tutorialMode) {
                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = AppColors.SurfaceMid,
                    contentColor = AppColors.PrimaryGreen
                )
            }

            if (tutorialMode) {
                MainTutorialOverlay(
                    onNext = onTutorialNext,
                    onBack = onTutorialBack,
                    onClose = onTutorialClose
                )
            }
        }
    }

    uiState.rapidCheckInEasterEgg?.let { easterEgg ->
        RapidCheckInEasterEggDialog(
            easterEgg = easterEgg,
            onDismissRequest = viewModel::dismissRapidCheckInEasterEgg,
            onContinue = viewModel::dismissRapidCheckInEasterEgg
        )
    }

    if (uiState.showBirthdayPrompt) {
        BirthdayInvitationPromptDialog(
            onStart = viewModel::openBirthdayEasterEgg,
            onDismiss = viewModel::dismissBirthdayPrompt
        )
    }

    // 生日彩蛋全螢幕
    if (uiState.showBirthdayEasterEgg) {
        BirthdayEasterEggDialog(
            onDismiss = viewModel::closeBirthdayEasterEgg
        )
    }

    farewellMessage?.let { message ->
        FarewellDialog(
            message = message,
            onDismissRequest = onExitApp
        )
    }
}

@Composable
private fun BirthdayInvitationPromptDialog(
    onStart: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(AppColors.SurfaceLight, AppColors.SurfaceDark)
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            AppColors.AccentAmber.copy(alpha = 0.75f),
                            AppColors.PrimaryGreen.copy(alpha = 0.35f)
                        )
                    ),
                    RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 26.dp, vertical = 28.dp)
        ) {
            Text(
                text = "✦",
                color = AppColors.AccentAmber.copy(alpha = 0.75f),
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.TopStart)
            )
            Text(
                text = "✦",
                color = AppColors.PrimaryGreen.copy(alpha = 0.6f),
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.TopEnd)
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    AppColors.AccentAmber.copy(alpha = 0.3f),
                                    AppColors.AccentAmber.copy(alpha = 0.06f)
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎂", fontSize = 42.sp)
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.birthday_invitation_title),
                    color = AppColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.birthday_invitation_message),
                    color = AppColors.TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.AccentAmber
                    )
                ) {
                    Text(
                        text = stringResource(R.string.birthday_invitation_open),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.birthday_invitation_later),
                        color = AppColors.TextHint
                    )
                }
            }
        }
    }
}

@Composable
private fun FarewellDialog(
    message: String,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AppColors.SurfaceMid, AppColors.SurfaceDark)
                    )
                )
                .border(
                    1.dp,
                    AppColors.PrimaryGreen.copy(alpha = 0.4f),
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 26.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = AppColors.PrimaryGreen,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                color = AppColors.TextPrimary,
                fontSize = 18.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.exit_farewell_hint),
                color = AppColors.TextHint,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RapidCheckInEasterEggDialog(
    easterEgg: RapidCheckInEasterEgg,
    onDismissRequest: () -> Unit,
    onContinue: () -> Unit
) {
    var allowOutsideDismiss by remember(easterEgg) { mutableStateOf(false) }

    LaunchedEffect(easterEgg) {
        delay(RAPID_CHECK_IN_OUTSIDE_DISMISS_DELAY_MILLIS)
        allowOutsideDismiss = true
    }

    Dialog(
        onDismissRequest = {
            if (allowOutsideDismiss) {
                onDismissRequest()
            }
        },
        properties = DialogProperties(dismissOnBackPress = false)
    ) {
        BackHandler(onBack = onDismissRequest)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AppColors.SurfaceLight, AppColors.SurfaceDark)
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AppColors.AccentAmber.copy(alpha = 0.8f),
                            AppColors.PrimaryGreen.copy(alpha = 0.6f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(AppColors.AccentAmberGlow)
                    .border(1.dp, AppColors.AccentAmber.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = AppColors.AccentAmber,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = AppColors.PrimaryGreen.copy(alpha = 0.14f),
                modifier = Modifier.border(
                    1.dp,
                    AppColors.PrimaryGreen.copy(alpha = 0.4f),
                    RoundedCornerShape(50)
                )
            ) {
                Text(
                    text = stringResource(R.string.rapid_check_in_easter_egg_badge),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    color = AppColors.PrimaryGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = easterEgg.title,
                color = AppColors.TextPrimary,
                fontSize = 23.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = easterEgg.message,
                color = AppColors.TextSecondary,
                fontSize = 15.sp,
                lineHeight = 23.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.rapid_check_in_easter_egg_footer),
                color = AppColors.TextHint,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryGreen)
            ) {
                Text(
                    text = stringResource(R.string.rapid_check_in_easter_egg_continue),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MainTutorialOverlay(
    onNext: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AppColors.SurfaceMid, AppColors.SurfaceDark)
                    )
                )
                .border(1.dp, AppColors.PrimaryGreen.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                .padding(18.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.tutorial_step_format, 4, 5),
                    color = AppColors.PrimaryGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.tutorial_home_title),
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.tutorial_home_description),
                    color = AppColors.TextSecondary,
                    fontSize = 15.sp,
                    lineHeight = 23.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextSecondary)
                    ) {
                        Text(stringResource(R.string.tutorial_previous))
                    }

                    Button(
                        onClick = onNext,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryGreen)
                    ) {
                        Text(
                            text = stringResource(R.string.tutorial_next),
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextSecondary)
                ) {
                    Text(stringResource(R.string.tutorial_close))
                }
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

private const val EXIT_FAREWELL_DURATION_MILLIS = 1_500L
private const val RAPID_CHECK_IN_OUTSIDE_DISMISS_DELAY_MILLIS = 500L
