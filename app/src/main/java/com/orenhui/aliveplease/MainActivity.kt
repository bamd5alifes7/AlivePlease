package com.orenhui.aliveplease

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.WorkManager
import com.orenhui.aliveplease.data.AppDataStore
import com.orenhui.aliveplease.notifications.NotificationHelper
import com.orenhui.aliveplease.ui.LogScreen
import com.orenhui.aliveplease.ui.MainScreen
import com.orenhui.aliveplease.ui.OnboardingScreen
import com.orenhui.aliveplease.ui.SettingsScreen
import com.orenhui.aliveplease.ui.theme.AppColors
import com.orenhui.aliveplease.utils.WorkSchedulerHelper
import kotlinx.coroutines.launch

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

        WorkSchedulerHelper.scheduleCheckInReminder(this)

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
    val startDestination = remember(dataStore) {
        if (dataStore.isFirstLaunch()) "onboarding" else "home"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onPrimaryAction = {
                    dataStore.setSetupTutorialPending()
                    navController.navigateOnce("settings_tutorial") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                onSecondaryAction = {
                    dataStore.setFirstLaunchCompleted()
                    dataStore.clearSetupTutorialPending()
                    navController.navigateOnce("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            SetupTutorialRedirect(dataStore, navController)
            HomePagerScreen(
                initialPage = 0,
                onSettingsSaved = onSettingsSaved,
                onNavigateToLogs = {
                    navController.navigateOnce("logs")
                },
                onReplayOnboarding = {
                    navController.navigateOnce("settings_tutorial")
                }
            )
        }

        composable("main") {
            SetupTutorialRedirect(dataStore, navController)
            HomePagerScreen(
                initialPage = 0,
                onSettingsSaved = onSettingsSaved,
                onNavigateToLogs = {
                    navController.navigateOnce("logs")
                },
                onReplayOnboarding = {
                    navController.navigateOnce("settings_tutorial")
                }
            )
        }

        composable("settings") {
            HomePagerScreen(
                initialPage = 1,
                onSettingsSaved = onSettingsSaved,
                onNavigateToLogs = {
                    navController.navigateOnce("logs")
                },
                onReplayOnboarding = {
                    navController.navigateOnce("settings_tutorial")
                }
            )
        }

        composable("settings_tutorial") {
            LaunchedEffect(Unit) {
                dataStore.setFirstLaunchCompleted()
                dataStore.clearSetupTutorialPending()
            }
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStackOrHome()
                },
                onSettingsSaved = onSettingsSaved,
                onNavigateToLogs = {
                    navController.navigateOnce("logs")
                },
                onReplayOnboarding = {},
                onTutorialFinished = {
                    navController.navigateOnce("settings") {
                        popUpTo("settings")
                    }
                },
                onTutorialShowHome = {
                    navController.navigateOnce("main_tutorial")
                },
                tutorialDisplayTotalSteps = 5,
                tutorialMode = true
            )
        }

        composable("settings_tutorial_preferences") {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStackOrHome()
                },
                onSettingsSaved = onSettingsSaved,
                onNavigateToLogs = {
                    navController.navigateOnce("logs")
                },
                onReplayOnboarding = {},
                onTutorialFinished = {
                    navController.navigateOnce("settings") {
                        popUpTo("settings")
                    }
                },
                onTutorialShowHome = {
                    navController.navigateOnce("main_tutorial")
                },
                tutorialStartIndex = 2,
                tutorialDisplayTotalSteps = 5,
                tutorialMode = true
            )
        }

        composable("settings_tutorial_finish") {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStackOrHome()
                },
                onSettingsSaved = onSettingsSaved,
                onNavigateToLogs = {
                    navController.navigateOnce("logs")
                },
                onReplayOnboarding = {},
                onTutorialFinished = {
                    navController.navigateOnce("settings") {
                        popUpTo("settings")
                    }
                },
                onTutorialShowHome = {
                    navController.navigateOnce("main_tutorial") {
                        popUpTo("settings_tutorial_finish") { inclusive = true }
                    }
                },
                tutorialStartIndex = 3,
                tutorialDisplayStepOffset = 1,
                tutorialDisplayTotalSteps = 5,
                tutorialMode = true
            )
        }

        composable("main_tutorial") {
            MainScreen(
                onNavigateToSettings = {},
                tutorialMode = true,
                onTutorialNext = {
                    navController.navigateOnce("settings_tutorial_finish") {
                        popUpTo("main_tutorial") { inclusive = true }
                    }
                },
                onTutorialBack = {
                    navController.navigateOnce("settings_tutorial_preferences") {
                        popUpTo("main_tutorial") { inclusive = true }
                    }
                },
                onTutorialClose = {
                    navController.navigateOnce("settings") {
                        popUpTo("settings")
                    }
                }
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

@Composable
private fun SetupTutorialRedirect(
    dataStore: AppDataStore,
    navController: NavHostController
) {
    LaunchedEffect(Unit) {
        if (dataStore.consumeSetupTutorialPending()) {
            navController.navigateOnce("settings_tutorial")
        }
    }
}

private fun NavHostController.navigateOnce(
    route: String,
    builder: NavOptionsBuilder.() -> Unit = {}
) {
    if (currentDestination?.route == route) return
    navigate(route) {
        launchSingleTop = true
        builder()
    }
}

private fun NavHostController.popBackStackOrHome() {
    if (!popBackStack()) {
        navigateOnce("home")
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
    val activity = LocalContext.current as? Activity
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { 2 }
    )
    val scope = rememberCoroutineScope()

    BackHandler(enabled = pagerState.currentPage == 1) {
        scope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

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
                    },
                    isVisible = pagerState.currentPage == 0,
                    exitFarewellEnabled = pagerState.currentPage == 0,
                    onExitApp = { activity?.finish() }
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
