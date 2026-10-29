package com.example.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import java.util.Calendar

object NotificationScheduler {
    
    // We schedule 5 different notifications that trigger at 5 different times of day.
    // We use a periodic worker for each specific reminder slot.
    
    fun scheduleDailyNotifications(context: Context) {
        val workManager = WorkManager.getInstance(context)
        
        scheduleSlot(workManager, "morning_slot", 10, "Morning Bloom! 🌸", "Start your day with a sweet Bonbon Bloom!", 1)
        scheduleSlot(workManager, "noon_slot", 13, "Lunch Break? 🍬", "Your lives are full! Enjoy a quick match-3 break!", 2)
        scheduleSlot(workManager, "evening_slot", 18, "Evening Bloom! 🌟", "Unwind with explosive sweet bonbon combos!", 3)
    }

    private fun scheduleSlot(
        workManager: WorkManager,
        uniqueWorkName: String,
        targetHour: Int,
        title: String,
        message: String,
        notificationId: Int
    ) {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()

        dueDate.set(Calendar.HOUR_OF_DAY, targetHour)
        dueDate.set(Calendar.MINUTE, 0)
        dueDate.set(Calendar.SECOND, 0)
        dueDate.set(Calendar.MILLISECOND, 0)

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        val data = Data.Builder()
            .putString(NotificationWorker.KEY_TITLE, title)
            .putString(NotificationWorker.KEY_MESSAGE, message)
            .putInt(NotificationWorker.KEY_NOTIFICATION_ID, notificationId)
            .build()

        val dailyWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        workManager.enqueueUniquePeriodicWork(
            uniqueWorkName,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }
}
