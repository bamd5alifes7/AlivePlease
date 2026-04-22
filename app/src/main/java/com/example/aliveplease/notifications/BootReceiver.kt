package com.example.aliveplease.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.aliveplease.utils.WorkSchedulerHelper

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> WorkSchedulerHelper.rescheduleAll(context)
        }
    }
}
