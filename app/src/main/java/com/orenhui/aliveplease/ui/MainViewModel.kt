package com.orenhui.aliveplease.ui

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.orenhui.aliveplease.R
import com.orenhui.aliveplease.data.AppDataStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MainUiState(
    val aliveDays: Int = 0,
    val streakDays: Int = 0,
    val checkInDates: Set<String> = emptySet(),
    val countdown: Long = 0L,
    val daysUntilNextBlessing: Int = 3,
    val careMessage: String = "",
    val showCheckInFeedback: Boolean = false,
    val celebrationMessage: String = "",
    val showCelebration: Boolean = false,
    val rapidCheckInEasterEgg: RapidCheckInEasterEgg? = null
)

data class RapidCheckInEasterEgg(
    val title: String,
    val message: String
)

class MainViewModel(
    private val appContext: Context,
    private val dataStore: AppDataStore
) : ViewModel() {

    var uiState by mutableStateOf(buildUiState(randomCareMessage()))
        private set

    private var feedbackJob: Job? = null
    private var celebrationJob: Job? = null
    private var careMessageJob: Job? = null
    private val rapidCheckInTracker = RapidCheckInTracker()

    init {
        startCountdownUpdates()
        startCareMessageRotation()
    }

    fun performCheckIn() {
        val isNewDay = dataStore.performCheckIn()
        val streakDays = dataStore.getCurrentStreak()
        val showCelebration = isNewDay && streakDays > 0 && streakDays % 3 == 0
        val rapidCheckInEasterEgg = uiState.rapidCheckInEasterEgg ?: run {
            if (rapidCheckInTracker.record(SystemClock.elapsedRealtime())) {
                randomRapidCheckInEasterEgg()
            } else {
                null
            }
        }

        uiState = buildUiState(randomCareMessage()).copy(
            showCheckInFeedback = true,
            celebrationMessage = if (showCelebration) getCelebrationMessage(streakDays) else "",
            showCelebration = showCelebration,
            rapidCheckInEasterEgg = rapidCheckInEasterEgg
        )

        feedbackJob?.cancel()
        feedbackJob = viewModelScope.launch {
            delay(CHECK_IN_FEEDBACK_DURATION_MILLIS)
            uiState = uiState.copy(showCheckInFeedback = false)
        }

        if (showCelebration) {
            celebrationJob?.cancel()
            celebrationJob = viewModelScope.launch {
                delay(CELEBRATION_DURATION_MILLIS)
                uiState = uiState.copy(showCelebration = false)
            }
        }
    }

    fun refreshCareMessage() {
        uiState = uiState.copy(careMessage = randomCareMessage(excluding = uiState.careMessage))
    }

    fun dismissRapidCheckInEasterEgg() {
        uiState = uiState.copy(rapidCheckInEasterEgg = null)
    }

    private fun startCountdownUpdates() {
        viewModelScope.launch {
            while (isActive) {
                uiState = uiState.copy(countdown = dataStore.getTimeUntilFamilyNotification())
                delay(1000)
            }
        }
    }

    private fun startCareMessageRotation() {
        careMessageJob?.cancel()
        careMessageJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000)
                uiState = uiState.copy(careMessage = randomCareMessage(excluding = uiState.careMessage))
            }
        }
    }

    private fun buildUiState(careMessage: String): MainUiState {
        val streakDays = dataStore.getCurrentStreak()
        return MainUiState(
            aliveDays = dataStore.getAliveDays(),
            streakDays = streakDays,
            checkInDates = dataStore.getCheckInDates(),
            countdown = dataStore.getTimeUntilFamilyNotification(),
            daysUntilNextBlessing = daysUntilNextBlessing(streakDays),
            careMessage = careMessage
        )
    }

    private fun daysUntilNextBlessing(streakDays: Int): Int {
        val remainder = streakDays % 3
        return if (remainder == 0) 3 else 3 - remainder
    }

    private fun randomCareMessage(excluding: String? = null): String {
        val messages = appContext.resources.getStringArray(R.array.care_messages)
        if (messages.isEmpty()) {
            return ""
        }

        val candidates = excluding?.let { current ->
            messages.filterNot { it == current }
        } ?: messages.toList()

        return (if (candidates.isNotEmpty()) candidates else messages.toList()).random()
    }

    private fun getCelebrationMessage(streakDays: Int): String {
        return when (streakDays) {
            3 -> appContext.getString(R.string.celebration_message_3)
            6 -> appContext.getString(R.string.celebration_message_6)
            9 -> appContext.getString(R.string.celebration_message_9)
            else -> appContext.getString(R.string.celebration_message_default, streakDays)
        }
    }

    private fun randomRapidCheckInEasterEgg(): RapidCheckInEasterEgg {
        val messageIds = listOf(
            R.string.rapid_check_in_easter_egg_title_1 to R.string.rapid_check_in_easter_egg_message_1,
            R.string.rapid_check_in_easter_egg_title_2 to R.string.rapid_check_in_easter_egg_message_2,
            R.string.rapid_check_in_easter_egg_title_3 to R.string.rapid_check_in_easter_egg_message_3,
            R.string.rapid_check_in_easter_egg_title_4 to R.string.rapid_check_in_easter_egg_message_4,
            R.string.rapid_check_in_easter_egg_title_5 to R.string.rapid_check_in_easter_egg_message_5
        )
        val (titleId, messageId) = messageIds.random()
        return RapidCheckInEasterEgg(
            title = appContext.getString(titleId),
            message = appContext.getString(messageId)
        )
    }

    companion object {
        private const val CHECK_IN_FEEDBACK_DURATION_MILLIS = 5_000L
        private const val CELEBRATION_DURATION_MILLIS = 10_000L

        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return MainViewModel(
                            appContext,
                            AppDataStore(appContext)
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
