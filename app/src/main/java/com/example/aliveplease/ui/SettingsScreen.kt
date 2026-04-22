package com.example.aliveplease.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aliveplease.R
import com.example.aliveplease.ui.theme.AppColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onSettingsSaved: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onReplayOnboarding: () -> Unit,
    tutorialMode: Boolean = false
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(context))
    val uiState = viewModel.uiState
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val sendingTestEmailMessage = stringResource(R.string.sending_test_email)

    LaunchedEffect(tutorialMode) {
        viewModel.reloadState(tutorialMode)
    }

    val tutorialSteps = listOf(
        TutorialStep(
            key = TutorialKey.UserName,
            title = stringResource(R.string.tutorial_user_name_title),
            description = stringResource(R.string.tutorial_user_name_description),
            requirement = TutorialRequirement.Required
        ),
        TutorialStep(
            key = TutorialKey.FamilyEmail,
            title = stringResource(R.string.tutorial_family_email_title),
            description = stringResource(R.string.tutorial_family_email_description),
            requirement = TutorialRequirement.Required
        ),
        TutorialStep(
            key = TutorialKey.Webhook,
            title = stringResource(R.string.tutorial_webhook_title),
            description = stringResource(R.string.tutorial_webhook_description),
            requirement = TutorialRequirement.Optional
        ),
        TutorialStep(
            key = TutorialKey.CareToggle,
            title = stringResource(R.string.tutorial_care_toggle_title),
            description = stringResource(R.string.tutorial_care_toggle_description),
            requirement = TutorialRequirement.Optional
        ),
        TutorialStep(
            key = TutorialKey.Save,
            title = stringResource(R.string.tutorial_save_title),
            description = stringResource(R.string.tutorial_save_description),
            requirement = TutorialRequirement.Required
        )
    )
    val currentStep = tutorialSteps.getOrNull(uiState.tutorialStepIndex)

    LaunchedEffect(uiState.tutorialStepIndex, tutorialMode) {
        if (!tutorialMode) return@LaunchedEffect
        val targets = listOf(0, 560, 1010, 1360, 1710)
        scrollState.animateScrollTo(targets.getOrElse(uiState.tutorialStepIndex) { targets.last() })
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AppColors.PrimaryGreen,
        unfocusedBorderColor = AppColors.TextHint.copy(alpha = 0.4f),
        focusedLabelColor = AppColors.PrimaryGreen,
        unfocusedLabelColor = AppColors.TextHint,
        focusedTextColor = AppColors.TextPrimary,
        unfocusedTextColor = AppColors.TextPrimary,
        cursorColor = AppColors.PrimaryGreen,
        errorBorderColor = AppColors.Error,
        errorLabelColor = AppColors.Error,
        unfocusedContainerColor = AppColors.SurfaceDark,
        focusedContainerColor = AppColors.SurfaceDark
    )

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = AppColors.SurfaceMid,
                    contentColor = AppColors.TextPrimary,
                    actionColor = AppColors.PrimaryGreen,
                    shape = RoundedCornerShape(14.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (tutorialMode) R.string.settings_tutorial_title
                            else R.string.settings_center_title
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.TextPrimary,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = AppColors.TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.SurfaceDark)
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
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SettingsHeroCard(
                    tutorialMode = tutorialMode,
                    careNotificationEnabled = uiState.careNotificationEnabled,
                    familyEmail = uiState.familyEmail,
                    gasWebhookUrl = uiState.gasWebhookUrl
                )

                SettingSection(
                    modifier = sectionModifier(currentStep?.key, TutorialKey.UserName, tutorialMode),
                    highlighted = currentStep?.key == TutorialKey.UserName && tutorialMode,
                    eyebrow = stringResource(R.string.identity_section),
                    icon = "A",
                    title = stringResource(R.string.user_name_title)
                ) {
                    OutlinedTextField(
                        value = uiState.userName,
                        onValueChange = viewModel::onUserNameChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.display_name_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        colors = fieldColors
                    )
                }

                SettingSection(
                    modifier = sectionModifier(currentStep?.key, null, tutorialMode),
                    eyebrow = stringResource(R.string.reminder_pacing_section),
                    icon = "1",
                    title = stringResource(R.string.check_in_interval)
                ) {
                    OutlinedTextField(
                        value = uiState.checkInInterval,
                        onValueChange = viewModel::onCheckInIntervalChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.check_in_interval_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = fieldColors
                    )
                }

                SettingSection(
                    modifier = sectionModifier(currentStep?.key, null, tutorialMode),
                    eyebrow = stringResource(R.string.family_notification_section),
                    icon = "2",
                    title = stringResource(R.string.family_wait_time_title)
                ) {
                    OutlinedTextField(
                        value = uiState.familyInterval,
                        onValueChange = viewModel::onFamilyIntervalChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.family_wait_time_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = fieldColors
                    )
                }

                SettingSection(
                    modifier = sectionModifier(currentStep?.key, TutorialKey.FamilyEmail, tutorialMode),
                    highlighted = currentStep?.key == TutorialKey.FamilyEmail && tutorialMode,
                    eyebrow = stringResource(R.string.family_notification_section),
                    icon = "@",
                    title = stringResource(R.string.family_email)
                ) {
                    OutlinedTextField(
                        value = uiState.familyEmail,
                        onValueChange = viewModel::onFamilyEmailChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.family_email_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        isError = uiState.emailError,
                        colors = fieldColors
                    )

                    if (uiState.emailError) {
                        Text(
                            text = stringResource(R.string.invalid_email),
                            color = AppColors.Error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                }

                SettingSection(
                    modifier = sectionModifier(currentStep?.key, null, tutorialMode),
                    eyebrow = stringResource(R.string.family_notification_section),
                    icon = "稱",
                    title = stringResource(R.string.recipient_title_label)
                ) {
                    OutlinedTextField(
                        value = uiState.familyRecipientTitle,
                        onValueChange = viewModel::onFamilyRecipientTitleChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.recipient_title_label_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        supportingText = { Text(stringResource(R.string.recipient_title_supporting)) },
                        colors = fieldColors
                    )
                }

                SettingSection(
                    modifier = sectionModifier(currentStep?.key, TutorialKey.Webhook, tutorialMode),
                    highlighted = currentStep?.key == TutorialKey.Webhook && tutorialMode,
                    eyebrow = stringResource(R.string.mail_delivery_section),
                    icon = "G",
                    title = stringResource(R.string.webhook_title)
                ) {
                    OutlinedTextField(
                        value = uiState.gasWebhookUrl,
                        onValueChange = viewModel::onGasWebhookUrlChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.webhook_default_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        singleLine = true,
                        colors = fieldColors
                    )

                    Text(
                        text = stringResource(R.string.webhook_description),
                        color = AppColors.TextHint,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar(sendingTestEmailMessage)
                                snackbarHostState.showSnackbar(viewModel.sendTestEmail())
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    AppColors.PrimaryGreen.copy(alpha = 0.7f),
                                    AppColors.PrimaryGreenDim.copy(alpha = 0.7f)
                                )
                            )
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.PrimaryGreen)
                    ) {
                        Text(stringResource(R.string.send_test_email), fontWeight = FontWeight.SemiBold)
                    }
                }

                DarkCard(
                    modifier = sectionModifier(currentStep?.key, TutorialKey.CareToggle, tutorialMode)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.AccentAmberGlow),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("!", fontSize = 16.sp, color = AppColors.AccentAmber)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.care_toggle_title),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.care_toggle_description),
                                    color = AppColors.TextHint,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                        Switch(
                            checked = uiState.careNotificationEnabled,
                            onCheckedChange = viewModel::onCareNotificationEnabledChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppColors.PrimaryGreen,
                                uncheckedThumbColor = AppColors.TextHint,
                                uncheckedTrackColor = AppColors.SurfaceMid
                            )
                        )
                    }
                }

                DarkCard(modifier = sectionModifier(currentStep?.key, null, tutorialMode)) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        OutlinedButton(
                            onClick = onNavigateToLogs,
                            modifier = Modifier.fillMaxWidth(),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        AppColors.TextHint.copy(alpha = 0.4f),
                                        AppColors.TextHint.copy(alpha = 0.2f)
                                    )
                                )
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextSecondary)
                        ) {
                            Text(stringResource(R.string.view_logs), fontWeight = FontWeight.Medium)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (!tutorialMode) {
                            OutlinedButton(
                                onClick = onReplayOnboarding,
                                modifier = Modifier.fillMaxWidth(),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            AppColors.PrimaryGreen.copy(alpha = 0.45f),
                                            AppColors.TextHint.copy(alpha = 0.25f)
                                        )
                                    )
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.PrimaryGreen)
                            ) {
                                Text(stringResource(R.string.replay_onboarding), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                Box(
                    modifier = sectionModifier(currentStep?.key, TutorialKey.Save, tutorialMode)
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(AppColors.PrimaryGreen, AppColors.PrimaryGreenDim)
                            )
                        )
                ) {
                    Button(
                        onClick = {
                            if (viewModel.saveSettings()) {
                                onSettingsSaved()
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues()
                    ) {
                        Text(
                            text = stringResource(R.string.save_settings),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                if (uiState.showSaveMessage) {
                    LaunchedEffect(uiState.showSaveMessage) {
                        kotlinx.coroutines.delay(2000)
                        viewModel.onSaveMessageShown()
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppColors.PrimaryGreen.copy(alpha = 0.12f))
                            .border(1.dp, AppColors.PrimaryGreen.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_saved),
                            color = AppColors.PrimaryGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (tutorialMode) 190.dp else 16.dp))
            }

            if (tutorialMode && currentStep != null) {
                TutorialOverlay(
                    step = currentStep,
                    stepIndex = uiState.tutorialStepIndex,
                    totalSteps = tutorialSteps.size,
                    placeAtTop = uiState.tutorialStepIndex >= 2,
                    onNext = {
                        if (viewModel.onTutorialNext()) {
                            onNavigateBack()
                        }
                    },
                    onBack = {
                        if (viewModel.onTutorialBack()) {
                            onNavigateBack()
                        }
                    },
                    onClose = onNavigateBack
                )
            }
        }
    }
}

@Composable
private fun SettingsHeroCard(
    tutorialMode: Boolean,
    careNotificationEnabled: Boolean,
    familyEmail: String,
    gasWebhookUrl: String
) {
    DarkCard {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    AppColors.PrimaryGreen.copy(alpha = 0.9f),
                                    AppColors.AccentAmber.copy(alpha = 0.9f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(
                            if (tutorialMode) R.string.settings_hero_tutorial_title
                            else R.string.settings_hero_title
                        ),
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (tutorialMode) {
                            stringResource(R.string.settings_hero_tutorial_description)
                        } else {
                            stringResource(R.string.settings_hero_description)
                        },
                        color = AppColors.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusChip(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.status_care_reminder),
                    value = stringResource(
                        if (careNotificationEnabled) R.string.status_enabled else R.string.status_disabled
                    ),
                    accent = if (careNotificationEnabled) AppColors.PrimaryGreen else AppColors.TextHint
                )
                StatusChip(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.status_family_notification),
                    value = stringResource(
                        if (familyEmail.isBlank()) R.string.status_not_configured
                        else R.string.status_configured
                    ),
                    accent = if (familyEmail.isBlank()) AppColors.AccentAmber else AppColors.PrimaryGreen
                )
                StatusChip(
                    modifier = Modifier.weight(1f),
                    label = "Webhook",
                    value = stringResource(
                        if (gasWebhookUrl.isBlank()) R.string.status_default_value
                        else R.string.status_custom_value
                    ),
                    accent = AppColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accent: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = label,
                color = AppColors.TextHint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private enum class TutorialKey {
    UserName,
    FamilyEmail,
    Webhook,
    CareToggle,
    Save
}

private enum class TutorialRequirement {
    Required,
    Optional
}

private data class TutorialStep(
    val key: TutorialKey,
    val title: String,
    val description: String,
    val requirement: TutorialRequirement
)

private fun sectionModifier(
    currentKey: TutorialKey?,
    sectionKey: TutorialKey?,
    tutorialMode: Boolean
): Modifier {
    if (!tutorialMode) return Modifier
    val highlighted = currentKey == sectionKey && sectionKey != null
    val dimmed = !highlighted
    return Modifier
        .alpha(if (dimmed) 0.33f else 1f)
        .border(
            width = 2.dp,
            color = if (highlighted) AppColors.PrimaryGreen.copy(alpha = 0.7f) else Color.Transparent,
            shape = RoundedCornerShape(16.dp)
        )
}

@Composable
private fun TutorialOverlay(
    step: TutorialStep,
    stepIndex: Int,
    totalSteps: Int,
    placeAtTop: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f)),
        contentAlignment = if (placeAtTop) Alignment.TopCenter else Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = if (placeAtTop) 16.dp else 0.dp,
                    bottom = if (placeAtTop) 0.dp else 16.dp
                )
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.tutorial_step_format, stepIndex + 1, totalSteps),
                        color = AppColors.PrimaryGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    RequirementBadge(requirement = step.requirement)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = step.title,
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = step.description,
                    color = AppColors.TextSecondary,
                    fontSize = 15.sp,
                    lineHeight = 23.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (step.key == TutorialKey.Save) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppColors.PrimaryGreen.copy(alpha = 0.10f))
                            .border(
                                1.dp,
                                AppColors.PrimaryGreen.copy(alpha = 0.28f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.tutorial_minimum_setup_title),
                                color = AppColors.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.tutorial_minimum_setup_steps),
                                color = AppColors.TextSecondary,
                                fontSize = 14.sp,
                                lineHeight = 21.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextSecondary)
                    ) {
                        Text(
                            stringResource(
                                if (stepIndex == 0) R.string.tutorial_leave_first
                                else R.string.tutorial_previous
                            )
                        )
                    }

                    Button(
                        onClick = onNext,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryGreen)
                    ) {
                        Text(
                            text = stringResource(
                                if (stepIndex == totalSteps - 1) R.string.tutorial_complete
                                else R.string.tutorial_next
                            ),
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
private fun RequirementBadge(requirement: TutorialRequirement) {
    val background = if (requirement == TutorialRequirement.Required) {
        AppColors.AccentAmber.copy(alpha = 0.18f)
    } else {
        AppColors.PrimaryGreen.copy(alpha = 0.14f)
    }
    val border = if (requirement == TutorialRequirement.Required) {
        AppColors.AccentAmber.copy(alpha = 0.42f)
    } else {
        AppColors.PrimaryGreen.copy(alpha = 0.42f)
    }
    val textColor = if (requirement == TutorialRequirement.Required) {
        AppColors.AccentAmber
    } else {
        AppColors.PrimaryGreen
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(
                if (requirement == TutorialRequirement.Required) R.string.tutorial_required
                else R.string.tutorial_optional
            ),
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun DarkCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(AppColors.SurfaceMid.copy(alpha = 0.92f), AppColors.SurfaceLight.copy(alpha = 0.72f))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
    ) {
        content()
    }
}

@Composable
private fun SettingSection(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    eyebrow: String,
    icon: String,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    DarkCard(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                text = eyebrow,
                color = AppColors.TextHint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppColors.PrimaryGlow),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 15.sp, color = AppColors.TextPrimary)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                if (highlighted) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(AppColors.PrimaryGreen.copy(alpha = 0.16f))
                            .border(
                                1.dp,
                                AppColors.PrimaryGreen.copy(alpha = 0.45f),
                                RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.tutorial_highlight_badge),
                            color = AppColors.PrimaryGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}
