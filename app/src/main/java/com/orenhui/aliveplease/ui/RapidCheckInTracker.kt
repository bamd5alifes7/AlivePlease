package com.orenhui.aliveplease.ui

import java.util.ArrayDeque

class RapidCheckInTracker(
    private val requiredCheckIns: Int = 10,
    private val windowMillis: Long = 60_000L
) {
    private val checkInTimes = ArrayDeque<Long>()

    fun record(checkInTimeMillis: Long): Boolean {
        while (checkInTimes.isNotEmpty() && checkInTimeMillis - checkInTimes.first > windowMillis) {
            checkInTimes.removeFirst()
        }

        checkInTimes.addLast(checkInTimeMillis)
        if (checkInTimes.size < requiredCheckIns) {
            return false
        }

        checkInTimes.clear()
        return true
    }
}
