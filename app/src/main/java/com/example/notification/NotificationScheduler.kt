package com.example.notification

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.worker.NewMovieWorker
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    private const val WORK_NAME_PERIODIC = "PeriodicNewMovieCheckWork"
    private const val WORK_NAME_IMMEDIATE = "ImmediateNewMovieCheckWork"

    fun schedulePeriodicCheck(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWorkRequest = PeriodicWorkRequestBuilder<NewMovieWorker>(
                1, TimeUnit.HOURS,
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            context.getSharedPreferences("movie_notifications_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("is_notifications_enabled", true)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun triggerImmediateCheck(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val immediateRequest = OneTimeWorkRequestBuilder<NewMovieWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_IMMEDIATE,
                ExistingWorkPolicy.REPLACE,
                immediateRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelPeriodicCheck(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            context.getSharedPreferences("movie_notifications_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("is_notifications_enabled", false)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isNotificationsEnabled(context: Context): Boolean {
        return context.getSharedPreferences("movie_notifications_prefs", MODE_PRIVATE)
            .getBoolean("is_notifications_enabled", true)
    }

    fun getLastCheckTime(context: Context): Long {
        return context.getSharedPreferences("movie_notifications_prefs", MODE_PRIVATE)
            .getLong("last_check_timestamp", 0L)
    }
}
