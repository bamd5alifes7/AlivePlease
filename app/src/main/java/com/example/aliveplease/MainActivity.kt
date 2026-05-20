package com.example.aliveplease

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.aliveplease.data.AppDataStore
import com.example.aliveplease.notifications.NotificationHelper
import com.example.aliveplease.ui.LogScreen
import com.example.aliveplease.ui.MainScreen
import com.example.aliveplease.ui.OnboardingScreen
import com.example.aliveplease.ui.SettingsScreen
import com.example.aliveplease.ui.theme.AppColors
import com.example.aliveplease.workers.CheckInReminderWorker
import com.example.aliveplease.utils.WorkSchedulerHelper
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var dataStore: AppDataStore

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            scheduleWorkers()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dataStore = AppDataStore(this)
        NotificationHelper.createNotificationChannels(this)
        requestNotificationPermission()
        scheduleWorkers()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppColors.Background
                ) {
                    AlivePleaseApp(
                        dataStore = dataStore,
                        onSettingsSaved = { scheduleWorkers() }
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> Unit
                else -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun scheduleWorkers() {
        val workManager = WorkManager.getInstance(this)

        val checkInRequest = PeriodicWorkRequestBuilder<CheckInReminderWorker>(
            dataStore.getNotifyInterval(),
            TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            "check_in_reminder",
            ExistingPeriodicWorkPolicy.UPDATE,
            checkInRequest
        )

        workManager.cancelUniqueWork("care_notification")

        if (dataStore.isCareNotificationOn()) {
            WorkSchedulerHelper.scheduleNextCareNotification(this)
        } else {
            WorkSchedulerHelper.cancelCareNotification(this)
        }

        workManager.cancelUniqueWork("family_notification_check")

        if (dataStore.shouldScheduleFamilyNotification()) {
            WorkSchedulerHelper.scheduleFamilyNotification(
                this,
                dataStore.getTimeUntilFamilyNotification()
            )
        } else {
            WorkSchedulerHelper.cancelFamilyNotification(this)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlivePleaseApp(
    dataStore: AppDataStore,
    onSettingsSaved: () -> Unit
) {
    val navController = rememberNavController()
    val startDestination = if (dataStore.isFirstLaunch()) "onboarding" else "home"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onPrimaryAction = {
                    dataStore.setFirstLaunchCompleted()
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                    navController.navigate("settings_tutorial")
                },
                onSecondaryAction = {
                    dataStore.setFirstLaunchCompleted()
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomePagerScreen(
                initialPage = 0,
                onSettingsSaved = onSettingsSaved,
                onNavigateToLogs = {
                    navController.navigate("logs")
                },
                onReplayOnboarding = {
                    navController.navigate("settings_tutorial")
                }
            )
        }

        composable("main") {
            HomePagerScreen(
                initialPage = 0,
                onSettingsSaved = onSettingsSaved,
                onNavigateToLogs = {
                    navController.navigate("logs")
                },
                onReplayOnboarding = {
                    navController.navigate("settings_tutorial")
                }
            )
        }

        composable("settings") {
            HomePagerScreen(
                initialPage = 1,
                onSettingsSaved = onSettingsSaved,
                onNavigateToLogs = {
                    navController.navigate("logs")
                },
                onReplayOnboarding = {
                    navController.navigate("settings_tutorial")
                }
            )
        }

        composable("settings_tutorial") {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSettingsSaved = onSettingsSaved,
                onNavigateToLogs = {
                    navController.navigate("logs")
                },
                onReplayOnboarding = {},
                tutorialMode = true
            )
        }

        composable("logs") {
            LogScreen(
                dataStore = dataStore,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomePagerScreen(
    initialPage: Int,
    onSettingsSaved: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onReplayOnboarding: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { 2 }
    )
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> MainScreen(
                    onNavigateToSettings = {
                        scope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    }
                )
                1 -> SettingsScreen(
                    onNavigateBack = {
                        scope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    onSettingsSaved = onSettingsSaved,
                    onNavigateToLogs = onNavigateToLogs,
                    onReplayOnboarding = onReplayOnboarding
                )
            }
        }
    }
}
