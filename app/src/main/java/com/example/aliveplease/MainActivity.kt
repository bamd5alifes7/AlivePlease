package com.example.aliveplease

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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

        WorkSchedulerHelper.scheduleFamilyNotification(
            this,
            dataStore.getTimeUntilFamilyNotification()
        )
    }
}

@Composable
fun AlivePleaseApp(
    dataStore: AppDataStore,
    onSettingsSaved: () -> Unit
) {
    val navController = rememberNavController()
    val startDestination = if (dataStore.isFirstLaunch()) "onboarding" else "main"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onPrimaryAction = {
                    dataStore.setFirstLaunchCompleted()
                    navController.navigate("main") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                    navController.navigate("settings_tutorial")
                },
                onSecondaryAction = {
                    dataStore.setFirstLaunchCompleted()
                    navController.navigate("main") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            MainScreen(
                dataStore = dataStore,
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                dataStore = dataStore,
                onNavigateBack = {
                    navController.popBackStack()
                },
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
                dataStore = dataStore,
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
