package com.example.aliveplease.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.aliveplease.data.AppDataStore
import com.example.aliveplease.notifications.NotificationHelper
import java.util.Calendar

/**
 * 關懷通知 Worker
 * 在白天時段隨機發送關懷訊息
 */
class CareNotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    
    override fun doWork(): Result {
        val dataStore = AppDataStore(applicationContext)
        
        // 檢查是否開啟關懷通知
        if (!dataStore.isCareNotificationOn()) {
            return Result.success()
        }
        
        // 檢查是否在白天時段（8:00 - 22:00）
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        if (hour in 8..21) {
            // 隨機決定是否發送（30% 機率）
            if (Math.random() < 0.3) {
                val message = NotificationHelper.getRandomCareMessage(applicationContext)
                NotificationHelper.sendCareMessage(applicationContext, message)
            }
        }
        
        return Result.success()
    }
}
