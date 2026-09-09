package com.xaniihub.app.tracking

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.xaniihub.app.MainActivity
import com.xaniihub.app.R
import com.xaniihub.app.localization.appString
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

@HiltWorker
class InactivityWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(TrackingConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val todayDate = LocalDate.now()
        val latest = if (
            prefs.getLong(TrackingConstants.PREF_LATEST_STEPS_DATE, Long.MIN_VALUE) == todayDate.toEpochDay()
        ) {
            prefs.getInt(TrackingConstants.PREF_LATEST_STEPS, 0)
        } else {
            0
        }
        val today = todayDate.toString()
        val savedDate = prefs.getString(PREF_LAST_CHECK_DATE, null)

        if (savedDate != today) {
            prefs.edit()
                .putString(PREF_LAST_CHECK_DATE, today)
                .putInt(PREF_LAST_CHECK_STEPS, latest)
                .apply()
            return Result.success()
        }

        val hour = LocalTime.now().hour
        if (hour !in ACTIVE_HOUR_START until ACTIVE_HOUR_END) return Result.success()

        ensureChannel()
        val previous = prefs.getInt(PREF_LAST_CHECK_STEPS, latest)
        val increase = (latest - previous).coerceAtLeast(0)
        if (increase < MIN_STEPS_PER_WINDOW) {
            notifyInactivity()
        }
        prefs.edit()
            .putInt(PREF_LAST_CHECK_STEPS, latest)
            .putString(PREF_LAST_CHECK_DATE, today)
            .apply()
        return Result.success()
    }

    private fun notifyInactivity() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            TrackingConstants.INACTIVITY_NOTIFICATION_ID,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, TrackingConstants.INACTIVITY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_walk_notification)
            .setContentTitle("RingWalk")
            .setContentText(appString("inactivity_notification_text"))
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        manager.notify(TrackingConstants.INACTIVITY_NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(TrackingConstants.INACTIVITY_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    TrackingConstants.INACTIVITY_CHANNEL_ID,
                    appString("inactivity_notification_channel"),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
    }

    companion object {
        private const val WORK_NAME = "xaniihub_inactivity_work"
        private const val PREF_LAST_CHECK_STEPS = "last_inactivity_steps"
        private const val PREF_LAST_CHECK_DATE = "last_inactivity_date"
        private const val ACTIVE_HOUR_START = 9
        private const val ACTIVE_HOUR_END = 21
        private const val MIN_STEPS_PER_WINDOW = 100

        fun schedule(context: Context) {
            val work = PeriodicWorkRequestBuilder<InactivityWorker>(2, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                work
            )
        }
    }
}