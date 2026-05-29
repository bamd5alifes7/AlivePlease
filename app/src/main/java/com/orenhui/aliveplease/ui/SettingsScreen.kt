package com.orenhui.aliveplease.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.orenhui.aliveplease.R
import com.orenhui.aliveplease.ui.theme.AppColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onSettingsSaved: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onReplayOnboarding: () -> Unit,
    onTutorialFinished: () -> Unit = onNavigateBack,
    onTutorialShowHome: () -> Unit = {},
    tutorialStartIndex: Int = 0,
    tutorialDisplayStepOffset: Int = 0,
    tutorialDisplayTotalSteps: Int? = null,
    tutorialMode: Boolean = false
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModelKey = "settings-$tutorialMode-$tutorialStartIndex"
    val viewModel: SettingsViewModel = viewModel(
        key = viewModelKey,
        factory = SettingsViewModel.factory(context, tutorialMode, tutorialStartIndex)
    )
    val uiState = viewModel.uiState
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val tutorialTargetPositions = remember { mutableStateMapOf<TutorialKey, Int>() }
    val sendingTestEmailMessage = stringResource(R.string.sending_test_email)
    val settingsSavedMessage = stringResource(R.string.settings_saved)
    val invalidSettingsMessage = stringResource(R.string.settings_invalid)
    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    var showReplayConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(tutorialMode, tutorialStartIndex) {
        viewModel.reloadState(tutorialMode, tutorialStartIndex)
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val tutorialSteps = listOf(
        TutorialStep(
            key = TutorialKey.UserName,
            focusKeys = setOf(
                TutorialKey.UserName,
                TutorialKey.RecipientTitle,
                TutorialKey.FamilyEmail,
                TutorialKey.FamilyInterval
            ),
            title = stringResource(R.string.tutorial_notification_settings_title),
            description = stringResource(R.string.tutorial_notification_settings_description),
            requirement = TutorialRequirement.Required
        ),
        TutorialStep(
            key = TutorialKey.CheckInInterval,
            focusKeys = setOf(
                TutorialKey.CheckInInterval,
                TutorialKey.FamilyWarning,
                TutorialKey.CareToggle,
                TutorialKey.QuietHours
            ),
            title = stringResource(R.string.tutorial_reminder_types_title),
            description = stringResource(R.string.tutorial_reminder_types_description),
            requirement = TutorialRequirement.Optional,
            overlayPlacement = TutorialOverlayPlacement.Top
        ),
        TutorialStep(
            key = TutorialKey.Webhook,
            focusKeys = setOf(
                TutorialKey.Webhook,
                TutorialKey.TestEmail
            ),
            title = stringResource(R.string.tutorial_mail_test_title),
            description = stringResource(R.string.tutorial_mail_test_description),
            requirement = TutorialRequirement.Optional,
            overlayPlacement = TutorialOverlayPlacement.Top,
            focusOffsetDp = 520
        ),
        TutorialStep(
            key = TutorialKey.MinimumSetup,
            focusKeys = setOf(
                TutorialKey.UserName,
                TutorialKey.RecipientTitle,
                TutorialKey.FamilyEmail
            ),
            scrollKey = TutorialKey.UserName,
            title = stringResource(R.string.tutorial_minimum_setup_title),
            description = stringResource(R.string.tutorial_minimum_setup_description),
            requirement = TutorialRequirement.Required
        )
    )
    val currentStep = tutorialSteps.getOrNull(uiState.tutorialStepIndex)
    val currentTargetScroll = currentStep?.scrollKey?.let { tutorialTargetPositions[it] }

    fun tutorialSectionModifier(sectionKey: TutorialKey?): Modifier {
        val baseModifier = sectionModifier(currentStep?.focusKeys.orEmpty(), sectionKey, tutorialMode)
        if (!tutorialMode || sectionKey == null) return baseModifier

        return baseModifier.then(
            Modifier.onGloballyPositioned { coordinates ->
                val focusOffset = currentStep?.takeIf { it.key == sectionKey }?.focusOffsetDp ?: 148
                val target = (coordinates.positionInRoot().y + scrollState.value - with(density) {
                    focusOffset.dp.roundToPx()
                })
                    .roundToInt()
                    .coerceAtLeast(0)
                tutorialTargetPositions[sectionKey] = target
            }
        )
    }

    fun tutorialHighlighted(sectionKey: TutorialKey): Boolean {
        return tutorialMode && currentStep?.focusKeys?.contains(sectionKey) == true
    }

    LaunchedEffect(uiState.tutorialStepIndex, tutorialMode, currentTargetScroll) {
        if (!tutorialMode) return@LaunchedEffect
        scrollState.animateScrollTo(currentTargetScroll ?: 0)
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
                    gasWebhookUrl = uiState.gasWebhookUrl,
                    notificationsEnabled = notificationsEnabled,
                    onOpenNotificationSettings = {
                        context.startActivity(createNotificationSettingsIntent(context))
                    }
                )

                SettingGroupHeader(
                    title = stringResource(R.string.family_group_title),
                    description = stringResource(R.string.family_group_description)
                )

                SettingSection(
                    modifier = tutorialSectionModifier(TutorialKey.UserName),
                    highlighted = tutorialHighlighted(TutorialKey.UserName),
                    eyebrow = stringResource(R.string.family_notification_section),
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
                        enabled = !tutorialMode,
                        supportingText = {
                            Text(
                                text = stringResource(R.string.display_name_supporting),
                                color = AppColors.TextSecondary
                            )
                        },
                        colors = fieldColors
                    )
                }

                SettingSection(
                    modifier = tutorialSectionModifier(TutorialKey.RecipientTitle),
                    highlighted = tutorialHighlighted(TutorialKey.RecipientTitle),
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
                        enabled = !tutorialMode,
                        supportingText = {
                            Text(
                                text = stringResource(R.string.recipient_title_supporting),
                                color = AppColors.TextSecondary
                            )
                        },
                        colors = fieldColors
                    )
                }

                SettingSection(
                    modifier = tutorialSectionModifier(TutorialKey.FamilyEmail),
                    highlighted = tutorialHighlighted(TutorialKey.FamilyEmail),
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
                        enabled = !tutorialMode,
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

                    Text(
                        text = stringResource(R.string.notification_service_description),
                        color = AppColors.TextHint,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )
                }

                SettingSection(
                    modifier = tutorialSectionModifier(TutorialKey.FamilyInterval),
                    highlighted = tutorialHighlighted(TutorialKey.FamilyInterval),
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
                        isError = uiState.familyIntervalError,
                        enabled = !tutorialMode,
                        colors = fieldColors
                    )

                    Text(
                        text = if (uiState.familyIntervalError) {
                            stringResource(R.string.family_wait_time_error)
                        } else {
                            stringResource(R.string.family_wait_time_description)
                        },
                        color = if (uiState.familyIntervalError) AppColors.Error else AppColors.TextHint,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )
                }

                SettingGroupHeader(
                    title = stringResource(R.string.reminder_group_title),
                    description = stringResource(R.string.reminder_group_description)
                )

                SettingSection(
                    modifier = tutorialSectionModifier(TutorialKey.CheckInInterval),
                    highlighted = tutorialHighlighted(TutorialKey.CheckInInterval),
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
                        isError = uiState.checkInIntervalError,
                        enabled = !tutorialMode,
                        colors = fieldColors
                    )

                    Text(
                        text = if (uiState.checkInIntervalError) {
                            stringResource(R.string.check_in_interval_error)
                        } else {
                            stringResource(R.string.check_in_interval_description)
                        },
                        color = if (uiState.checkInIntervalError) AppColors.Error else AppColors.TextHint,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )
                }

                SettingSection(
                    modifier = tutorialSectionModifier(TutorialKey.FamilyWarning),
                    highlighted = tutorialHighlighted(TutorialKey.FamilyWarning),
                    eyebrow = stringResource(R.string.reminder_pacing_section),
                    icon = "!",
                    title = stringResource(R.string.family_warning_before_title)
                ) {
                    OutlinedTextField(
                        value = uiState.familyWarningBefore,
                        onValueChange = viewModel::onFamilyWarningBeforeChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.family_warning_before_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = uiState.familyWarningError,
                        enabled = !tutorialMode,
                        colors = fieldColors
                    )

                    Text(
                        text = if (uiState.familyWarningError) {
                            stringResource(R.string.family_warning_before_error)
                        } else {
                            stringResource(R.string.family_warning_before_description)
                        },
                        color = if (uiState.familyWarningError) AppColors.Error else AppColors.TextHint,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )
                }

                DarkCard(
                    modifier = tutorialSectionModifier(TutorialKey.CareToggle)
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
                            enabled = !tutorialMode,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppColors.PrimaryGreen,
                                uncheckedThumbColor = AppColors.TextHint,
                                uncheckedTrackColor = AppColors.SurfaceMid
                            )
                        )
                    }
                }

                SettingSection(
                    modifier = tutorialSectionModifier(TutorialKey.QuietHours),
                    highlighted = tutorialHighlighted(TutorialKey.QuietHours),
                    eyebrow = stringResource(R.string.reminder_pacing_section),
                    icon = "Z",
                    title = stringResource(R.string.quiet_hours_title)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.quiet_hours_enabled_label),
                            color = AppColors.TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = uiState.quietHoursEnabled,
                            onCheckedChange = viewModel::onQuietHoursEnabledChanged,
                            enabled = !tutorialMode,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppColors.PrimaryGreen,
                                uncheckedThumbColor = AppColors.TextHint,
                                uncheckedTrackColor = AppColors.SurfaceMid
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = uiState.quietHoursStart,
                            onValueChange = viewModel::onQuietHoursStartChanged,
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.quiet_hours_start_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            singleLine = true,
                            isError = uiState.quietHoursError,
                            enabled = uiState.quietHoursEnabled && !tutorialMode,
                            colors = fieldColors
                        )
                        OutlinedTextField(
                            value = uiState.quietHoursEnd,
                            onValueChange = viewModel::onQuietHoursEndChanged,
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.quiet_hours_end_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            singleLine = true,
                            isError = uiState.quietHoursError,
                            enabled = uiState.quietHoursEnabled && !tutorialMode,
                            colors = fieldColors
                        )
                    }

                    Text(
                        text = if (uiState.quietHoursError) {
                            stringResource(R.string.quiet_hours_error)
                        } else {
                            stringResource(R.string.quiet_hours_description)
                        },
                        color = if (uiState.quietHoursError) AppColors.Error else AppColors.TextHint,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )
                }

                SettingGroupHeader(
                    title = stringResource(R.string.advanced_group_title),
                    description = stringResource(R.string.advanced_group_description)
                )

                SettingSection(
                    modifier = tutorialSectionModifier(TutorialKey.Webhook),
                    highlighted = tutorialHighlighted(TutorialKey.Webhook),
                    eyebrow = stringResource(R.string.advanced_section),
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
                        enabled = !tutorialMode,
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
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(GAS_DOCS_URL))
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !tutorialMode,
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    AppColors.TextHint.copy(alpha = 0.28f),
                                    AppColors.TextHint.copy(alpha = 0.14f)
                                )
                            )
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextSecondary)
                    ) {
                        Text(stringResource(R.string.open_gas_docs), fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar(sendingTestEmailMessage)
                                snackbarHostState.showSnackbar(viewModel.sendTestEmail())
                            }
                        },
                        modifier = tutorialSectionModifier(TutorialKey.TestEmail).fillMaxWidth(),
                        enabled = !tutorialMode,
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

                SettingGroupHeader(
                    title = stringResource(R.string.test_group_title),
                    description = stringResource(R.string.test_group_description)
                )

                Box(
                    modifier = tutorialSectionModifier(TutorialKey.Save)
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
                                scope.launch {
                                    snackbarHostState.showSnackbar(settingsSavedMessage)
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar(invalidSettingsMessage)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        enabled = !tutorialMode,
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

                DarkCard(modifier = tutorialSectionModifier(TutorialKey.LogsAndGuide)) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        OutlinedButton(
                            onClick = onNavigateToLogs,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !tutorialMode,
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
                                onClick = {
                                    if (viewModel.hasUnsavedChanges()) {
                                        showReplayConfirmDialog = true
                                    } else {
                                        onReplayOnboarding()
                                    }
                                },
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

                Spacer(modifier = Modifier.height(if (tutorialMode) 760.dp else 16.dp))
            }

            if (showReplayConfirmDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showReplayConfirmDialog = false
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.replay_tutorial_unsaved_title),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(stringResource(R.string.replay_tutorial_unsaved_message))
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showReplayConfirmDialog = false
                            }
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showReplayConfirmDialog = false
                                onReplayOnboarding()
                            }
                        ) {
                            Text(stringResource(R.string.discard_and_replay))
                        }
                    },
                    containerColor = AppColors.SurfaceMid,
                    titleContentColor = AppColors.TextPrimary,
                    textContentColor = AppColors.TextSecondary
                )
            }

            if (tutorialMode && currentStep != null) {
                TutorialOverlay(
                    step = currentStep,
                    stepIndex = uiState.tutorialStepIndex + tutorialDisplayStepOffset,
                    totalSteps = tutorialDisplayTotalSteps ?: tutorialSteps.size,
                    onNext = {
                        if (currentStep.key == TutorialKey.Webhook) {
                            onTutorialShowHome()
                        } else if (viewModel.onTutorialNext(tutorialSteps.lastIndex)) {
                            onTutorialFinished()
                        }
                    },
                    onBack = {
                        if (currentStep.key == TutorialKey.MinimumSetup) {
                            onTutorialShowHome()
                        } else if (viewModel.onTutorialBack()) {
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
    gasWebhookUrl: String,
    notificationsEnabled: Boolean,
    onOpenNotificationSettings: () -> Unit
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

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusChip(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.notification_permission_title),
                        value = stringResource(
                            if (notificationsEnabled) R.string.notification_permission_status_enabled
                            else R.string.notification_permission_status_disabled
                        ),
                        accent = if (notificationsEnabled) AppColors.PrimaryGreen else AppColors.AccentAmber
                    )
                    StatusChip(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.mail_delivery_section),
                        value = stringResource(
                            if (gasWebhookUrl.isBlank()) R.string.status_default_value
                            else R.string.status_custom_value
                        ),
                        accent = AppColors.TextSecondary
                    )
                }
            }

            if (!notificationsEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onOpenNotificationSettings,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !tutorialMode,
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(
                            listOf(
                                AppColors.AccentAmber.copy(alpha = 0.6f),
                                AppColors.PrimaryGreen.copy(alpha = 0.28f)
                            )
                        )
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.AccentAmber)
                ) {
                    Text(
                        text = stringResource(R.string.notification_permission_open_settings),
                        fontWeight = FontWeight.Medium
                    )
                }
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

@Composable
private fun SettingGroupHeader(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp)
    ) {
        Text(
            text = title,
            color = AppColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            color = AppColors.TextHint,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}

private enum class TutorialKey {
    UserName,
    CheckInInterval,
    FamilyInterval,
    FamilyWarning,
    FamilyEmail,
    RecipientTitle,
    CareToggle,
    QuietHours,
    Webhook,
    TestEmail,
    LogsAndGuide,
    Save,
    MinimumSetup
}

private const val GAS_DOCS_URL = "https://github.com/bamd5alifes7/AlivePlease/blob/master/docs/AlivePleaseWebhook.md"

private fun createNotificationSettingsIntent(context: Context): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
}

private enum class TutorialRequirement {
    Required,
    Optional
}

private enum class TutorialOverlayPlacement {
    Top,
    Bottom
}

private data class TutorialStep(
    val key: TutorialKey,
    val focusKeys: Set<TutorialKey> = setOf(key),
    val scrollKey: TutorialKey = key,
    val title: String,
    val description: String,
    val requirement: TutorialRequirement,
    val overlayPlacement: TutorialOverlayPlacement = TutorialOverlayPlacement.Bottom,
    val focusOffsetDp: Int = 148
)

private fun sectionModifier(
    currentKeys: Set<TutorialKey>,
    sectionKey: TutorialKey?,
    tutorialMode: Boolean
): Modifier {
    if (!tutorialMode) return Modifier
    val highlighted = sectionKey != null && currentKeys.contains(sectionKey)
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
    onNext: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f)),
        contentAlignment = when (step.overlayPlacement) {
            TutorialOverlayPlacement.Top -> Alignment.TopCenter
            TutorialOverlayPlacement.Bottom -> Alignment.BottomCenter
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = if (step.overlayPlacement == TutorialOverlayPlacement.Top) 16.dp else 0.dp,
                    bottom = if (step.overlayPlacement == TutorialOverlayPlacement.Bottom) 16.dp else 0.dp
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
