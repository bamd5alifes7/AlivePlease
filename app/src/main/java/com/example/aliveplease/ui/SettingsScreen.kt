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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aliveplease.data.AppDataStore
import com.example.aliveplease.ui.theme.AppColors
import com.example.aliveplease.utils.EmailContentBuilder
import com.example.aliveplease.utils.TimeFormatter
import com.example.aliveplease.utils.WebhookHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    dataStore: AppDataStore,
    onNavigateBack: () -> Unit,
    onSettingsSaved: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onReplayOnboarding: () -> Unit,
    tutorialMode: Boolean = false
) {
    var userName by remember { mutableStateOf(dataStore.getUserName().takeUnless { it == "??" }.orEmpty()) }
    var checkInInterval by remember { mutableStateOf(dataStore.getNotifyInterval().toString()) }
    var familyInterval by remember {
        val current = dataStore.getFamilyNotifyIntervalFloat()
        mutableStateOf(if (current % 1 == 0f) current.toInt().toString() else current.toString())
    }
    var familyEmail by remember { mutableStateOf(dataStore.getFamilyEmail()) }
    var familyRecipientTitle by remember {
        mutableStateOf(dataStore.getFamilyRecipientTitle().takeUnless { it.contains("??") }.orEmpty())
    }
    var gasWebhookUrl by remember { mutableStateOf(dataStore.getStoredGasWebhookUrl()) }
    var careNotificationEnabled by remember { mutableStateOf(dataStore.isCareNotificationOn()) }
    var showSaveMessage by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var tutorialStepIndex by remember(tutorialMode) { mutableStateOf(if (tutorialMode) 0 else -1) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val userNameFocusRequester = remember { FocusRequester() }
    val familyEmailFocusRequester = remember { FocusRequester() }
    val webhookFocusRequester = remember { FocusRequester() }

    val tutorialSteps = remember {
        listOf(
            TutorialStep(
                key = TutorialKey.UserName,
                title = "先設定你的名字",
                description = "這個名字會出現在通知與寄信內容裡，讓收到訊息的人知道是誰。",
                requirement = TutorialRequirement.Required
            ),
            TutorialStep(
                key = TutorialKey.FamilyEmail,
                title = "填入家人 Email",
                description = "如果你太久沒有報平安，系統就會寄信到這個地址。",
                requirement = TutorialRequirement.Required
            ),
            TutorialStep(
                key = TutorialKey.Webhook,
                title = "Webhook 可先用預設值",
                description = "這欄留空會自動使用內建 GAS。你也可以在這裡按測試寄信。",
                requirement = TutorialRequirement.Optional
            ),
            TutorialStep(
                key = TutorialKey.CareToggle,
                title = "決定是否開啟關懷提醒",
                description = "打開後，app 會主動提醒你回來打卡報平安。",
                requirement = TutorialRequirement.Optional
            ),
            TutorialStep(
                key = TutorialKey.Save,
                title = "最後記得儲存設定",
                description = "最少完成：名字、家人 Email、按下儲存。Webhook 可先留空。",
                requirement = TutorialRequirement.Required
            )
        )
    }
    val currentStep = tutorialSteps.getOrNull(tutorialStepIndex)

    LaunchedEffect(tutorialStepIndex, tutorialMode) {
        if (!tutorialMode) return@LaunchedEffect
        val targets = listOf(0, 520, 980, 1320, 1640)
        scrollState.animateScrollTo(targets.getOrElse(tutorialStepIndex) { targets.last() })
    }

    LaunchedEffect(currentStep?.key, tutorialMode) {
        if (!tutorialMode) return@LaunchedEffect
        when (currentStep?.key) {
            TutorialKey.UserName -> userNameFocusRequester.requestFocus()
            TutorialKey.FamilyEmail -> familyEmailFocusRequester.requestFocus()
            TutorialKey.Webhook -> webhookFocusRequester.requestFocus()
            else -> Unit
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AppColors.PrimaryGreen,
        unfocusedBorderColor = AppColors.TextHint.copy(alpha = 0.5f),
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
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (tutorialMode) "設定導覽" else "設定",
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
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
                        colors = listOf(AppColors.Background, AppColors.SurfaceDark, AppColors.Background)
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
                SettingSection(
                    modifier = sectionModifier(currentStep?.key, TutorialKey.UserName, tutorialMode),
                    highlighted = currentStep?.key == TutorialKey.UserName && tutorialMode,
                    icon = "A",
                    title = "你的名字"
                ) {
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(userNameFocusRequester),
                        label = { Text("輸入你想顯示的名字") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        colors = fieldColors
                    )
                }

                SettingSection(
                    modifier = sectionModifier(currentStep?.key, null, tutorialMode),
                    icon = "1",
                    title = "打卡提醒間隔"
                ) {
                    OutlinedTextField(
                        value = checkInInterval,
                        onValueChange = { checkInInterval = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("幾小時提醒一次") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = fieldColors
                    )
                }

                SettingSection(
                    modifier = sectionModifier(currentStep?.key, null, tutorialMode),
                    icon = "2",
                    title = "家人通知間隔"
                ) {
                    OutlinedTextField(
                        value = familyInterval,
                        onValueChange = { familyInterval = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("幾小時沒打卡就寄信") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = fieldColors
                    )
                }

                SettingSection(
                    modifier = sectionModifier(currentStep?.key, TutorialKey.FamilyEmail, tutorialMode),
                    highlighted = currentStep?.key == TutorialKey.FamilyEmail && tutorialMode,
                    icon = "@",
                    title = "家人 Email"
                ) {
                    OutlinedTextField(
                        value = familyEmail,
                        onValueChange = {
                            familyEmail = it
                            emailError = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(familyEmailFocusRequester),
                        label = { Text("要通知的 Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        isError = emailError,
                        colors = fieldColors
                    )

                    if (emailError) {
                        Text(
                            text = "Email 格式不正確",
                            color = AppColors.Error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                }

                SettingSection(
                    modifier = sectionModifier(currentStep?.key, null, tutorialMode),
                    icon = "稱",
                    title = "收件人稱呼"
                ) {
                    OutlinedTextField(
                        value = familyRecipientTitle,
                        onValueChange = { familyRecipientTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("例如：媽媽、哥哥、家人") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        supportingText = { Text("這會出現在信件開頭。") },
                        colors = fieldColors
                    )
                }

                SettingSection(
                    modifier = sectionModifier(currentStep?.key, TutorialKey.Webhook, tutorialMode),
                    highlighted = currentStep?.key == TutorialKey.Webhook && tutorialMode,
                    icon = "G",
                    title = "GAS Webhook URL"
                ) {
                    OutlinedTextField(
                        value = gasWebhookUrl,
                        onValueChange = { gasWebhookUrl = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(webhookFocusRequester),
                        label = { Text("留空時會使用預設 GAS") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        singleLine = true,
                        colors = fieldColors
                    )

                    Text(
                        text = "這裡可以貼自己的 GAS Webhook。若留空，測試寄信與正式寄信都會先使用預設值。",
                        color = AppColors.TextHint,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            if (familyEmail.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar("請先填入家人 Email") }
                                return@OutlinedButton
                            }
                            if (!TimeFormatter.isValidEmail(familyEmail)) {
                                emailError = true
                                return@OutlinedButton
                            }

                            val safeUserName = userName.trim().ifBlank { dataStore.getUserName() }
                            val safeRecipientTitle = familyRecipientTitle.trim().ifBlank { dataStore.getFamilyRecipientTitle() }
                            val resolvedWebhookUrl = gasWebhookUrl.trim().ifBlank { dataStore.getGasWebhookUrl() }
                            val intervalValue = familyInterval.toFloatOrNull() ?: dataStore.getFamilyNotifyIntervalFloat()

                            val subject = EmailContentBuilder.buildSubject(
                                recipientTitle = safeRecipientTitle,
                                userName = safeUserName,
                                isTest = true
                            )
                            val body = EmailContentBuilder.buildBody(
                                recipientTitle = safeRecipientTitle,
                                userName = safeUserName,
                                intervalHours = intervalValue,
                                isTest = true
                            )

                            scope.launch {
                                snackbarHostState.showSnackbar("正在寄出測試信...")
                                val result = WebhookHelper.sendEmail(
                                    webhookUrl = resolvedWebhookUrl,
                                    to = familyEmail,
                                    subject = subject,
                                    body = body
                                )
                                snackbarHostState.showSnackbar(
                                    if (result.success) "測試信已送出" else result.message ?: "測試寄信失敗"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    AppColors.PrimaryGreen.copy(alpha = 0.6f),
                                    AppColors.PrimaryGreenDim.copy(alpha = 0.6f)
                                )
                            )
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.PrimaryGreen)
                    ) {
                        Text("測試寄信", fontWeight = FontWeight.SemiBold)
                    }
                }

                DarkCard(modifier = sectionModifier(currentStep?.key, TutorialKey.CareToggle, tutorialMode)) {
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.PrimaryGlow),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("!", fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "開啟關懷提醒",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "白天會以 4 到 8 小時的隨機間隔提醒你，23:00 到 07:00 會順延到早上。",
                                    color = AppColors.TextHint,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                        Switch(
                            checked = careNotificationEnabled,
                            onCheckedChange = { careNotificationEnabled = it },
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
                                    listOf(AppColors.TextHint.copy(alpha = 0.4f), AppColors.TextHint.copy(alpha = 0.2f))
                                )
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextSecondary)
                        ) {
                            Text("查看紀錄", fontWeight = FontWeight.Medium)
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
                                Text("重看引導教學", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                Box(
                    modifier = sectionModifier(currentStep?.key, TutorialKey.Save, tutorialMode)
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(AppColors.PrimaryGreen, AppColors.PrimaryGreenDim)
                            )
                        )
                ) {
                    Button(
                        onClick = {
                            if (familyEmail.isNotBlank() && !TimeFormatter.isValidEmail(familyEmail)) {
                                emailError = true
                                return@Button
                            }

                            userName.trim().takeIf { it.isNotBlank() }?.let(dataStore::setUserName)
                            checkInInterval.toLongOrNull()?.takeIf { it > 0 }?.let(dataStore::setNotifyInterval)
                            familyInterval.toFloatOrNull()?.takeIf { it > 0 }?.let(dataStore::setFamilyNotifyIntervalFloat)
                            dataStore.setFamilyEmail(familyEmail)
                            dataStore.setFamilyRecipientTitle(familyRecipientTitle)
                            dataStore.setGasWebhookUrl(gasWebhookUrl)
                            dataStore.setCareNotificationOn(careNotificationEnabled)

                            onSettingsSaved()
                            showSaveMessage = true
                        },
                        modifier = Modifier.fillMaxSize(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues()
                    ) {
                        Text(
                            text = "儲存設定",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                if (showSaveMessage) {
                    LaunchedEffect(showSaveMessage) {
                        kotlinx.coroutines.delay(2000)
                        showSaveMessage = false
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.PrimaryGreen.copy(alpha = 0.12f))
                            .border(1.dp, AppColors.PrimaryGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "設定已儲存",
                            color = AppColors.PrimaryGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (tutorialMode) 180.dp else 16.dp))
            }

            if (tutorialMode && currentStep != null) {
                TutorialOverlay(
                    step = currentStep,
                    stepIndex = tutorialStepIndex,
                    totalSteps = tutorialSteps.size,
                    placeAtTop = tutorialStepIndex >= 2,
                    onNext = {
                        if (tutorialStepIndex < tutorialSteps.lastIndex) {
                            tutorialStepIndex += 1
                        } else {
                            onNavigateBack()
                        }
                    },
                    onBack = {
                        if (tutorialStepIndex > 0) {
                            tutorialStepIndex -= 1
                        } else {
                            onNavigateBack()
                        }
                    },
                    onClose = onNavigateBack
                )
            }
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
            shape = RoundedCornerShape(14.dp)
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
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AppColors.SurfaceMid, AppColors.SurfaceDark)
                    )
                )
                .border(1.dp, AppColors.PrimaryGreen.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "步驟 ${stepIndex + 1} / $totalSteps",
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
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppColors.PrimaryGreen.copy(alpha = 0.10f))
                            .border(
                                1.dp,
                                AppColors.PrimaryGreen.copy(alpha = 0.28f),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "最小可用組合",
                                color = AppColors.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "1. 名字\n2. 家人 Email\n3. 按下儲存",
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
                        Text(if (stepIndex == 0) "先離開" else "上一步")
                    }

                    Button(
                        onClick = onNext,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryGreen)
                    ) {
                        Text(
                            text = if (stepIndex == totalSteps - 1) "完成教學" else "下一步",
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
                    Text("關閉教學")
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
            text = if (requirement == TutorialRequirement.Required) "必要" else "可選",
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
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(AppColors.SurfaceMid.copy(alpha = 0.9f), AppColors.SurfaceLight.copy(alpha = 0.7f))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
    ) {
        content()
    }
}

@Composable
private fun SettingSection(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    icon: String,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    DarkCard(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(AppColors.PrimaryGlow),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 15.sp, color = AppColors.TextPrimary)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextSecondary
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
                            text = "請看這裡",
                            color = AppColors.PrimaryGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
