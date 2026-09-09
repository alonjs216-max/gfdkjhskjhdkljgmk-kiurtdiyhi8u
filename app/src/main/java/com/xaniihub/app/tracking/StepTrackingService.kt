package com.xaniihub.app.tracking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.xaniihub.app.MainActivity
import com.xaniihub.app.R
import com.xaniihub.app.domain.model.TrackingSnapshot
import com.xaniihub.app.domain.repository.XaniiRepository
import com.xaniihub.app.localization.appLocale
import com.xaniihub.app.localization.appString
import com.xaniihub.app.widget.RingWalkWidgets
import com.xaniihub.app.widget.WidgetSnapshot
import com.xaniihub.app.widget.WidgetTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@AndroidEntryPoint
class StepTrackingService : Service(), SensorEventListener {

    @Inject
    lateinit var repository: XaniiRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ingestMutex = Mutex()
    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private var listenerRegistered = false
    private var publishedDay = Long.MIN_VALUE
    private var dateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannels()
        val started = runCatching {
            // The manifest declares the health foreground service type, and Android 14+ rejects
            // startForeground() unless the same type is passed here.
            ServiceCompat.startForeground(
                this,
                TrackingConstants.TRACKING_NOTIFICATION_ID,
                buildFallbackNotification(TrackingSnapshot(0, 0, 0, 0)),
                foregroundServiceType()
            )
        }.isSuccess
        if (!started) {
            stopSelf()
            return
        }
        sensorManager = getSystemService(SENSOR_SERVICE) as? SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor == null) {
            stopSelf()
            return
        }
        registerSensorListener()
        // Show today's real numbers immediately instead of the empty placeholder notification.
        scope.launch { ingestMutex.withLock { runCatching { publishSnapshot() } } }
        startDateWatcher()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // onStartCommand also runs for an already running service (boot receiver, app update,
        // sticky restart), so make sure the sensor listener is attached in those cases too -
        // otherwise the foreground notification stays up while nothing is being counted.
        registerSensorListener()
        return START_STICKY
    }

    override fun onDestroy() {
        dateJob?.cancel()
        listenerRegistered = false
        sensorManager?.unregisterListener(this)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return
        val total = event.values.firstOrNull() ?: return
        val eventTime = System.currentTimeMillis()
        scope.launch {
            ingestMutex.withLock {
                runCatching {
                    // The step delta and the cadence are derived inside the repository, which is
                    // the only place that persists the previous reading: a service restart can no
                    // longer reset that state and lose steps or fake a spike.
                    repository.ingestSensorTotal(total, eventTime)
                    publishSnapshot()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun registerSensorListener() {
        if (listenerRegistered) return
        val manager = sensorManager ?: return
        val sensor = stepSensor ?: return
        listenerRegistered = manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun foregroundServiceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        } else {
            0
        }

    /**
     * The step counter is an on-change sensor, so nothing repaints the notification, the tile and
     * the widgets when the day rolls over while the user is not walking - they used to keep
     * showing yesterday's total until the next step.
     */
    private fun startDateWatcher() {
        dateJob?.cancel()
        dateJob = scope.launch {
            while (isActive) {
                delay(millisUntilDateCheck())
                if (LocalDate.now().toEpochDay() != publishedDay) {
                    ingestMutex.withLock { runCatching { publishSnapshot() } }
                }
            }
        }
    }

    private fun millisUntilDateCheck(): Long {
        val nextMidnight = LocalDate.now()
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return (nextMidnight - System.currentTimeMillis()).coerceIn(1_000L, DATE_CHECK_INTERVAL_MS)
    }

    /** Must be called while holding [ingestMutex]; the mutex is not reentrant. */
    private suspend fun publishSnapshot() {
        val snapshot = repository.getTrackingSnapshot()
        updateForegroundNotification(snapshot)
        RingWalkWidgets.publish(
            context = this,
            steps = snapshot.steps,
            goal = readDailyGoal(),
            calories = snapshot.calories,
            distanceMeters = snapshot.distanceMeters,
            activeMinutes = snapshot.activeMinutes
        )
        publishedDay = LocalDate.now().toEpochDay()
    }

    private fun updateForegroundNotification(snapshot: TrackingSnapshot) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = runCatching { buildTrackingNotification(snapshot) }
            .getOrElse { buildFallbackNotification(snapshot) }
        runCatching {
            manager.notify(TrackingConstants.TRACKING_NOTIFICATION_ID, notification)
        }.onFailure {
            manager.notify(TrackingConstants.TRACKING_NOTIFICATION_ID, buildFallbackNotification(snapshot))
        }
    }

    private fun baseNotificationBuilder(snapshot: TrackingSnapshot): NotificationCompat.Builder {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, TrackingConstants.TRACKING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_walk_notification)
            .setContentTitle(appString("tracking_notification_title"))
            .setContentText(
                "${formatInt(snapshot.steps)} ${appString("steps_metric").lowercase()} • ${formatInt(snapshot.calories)} ${appString("kcal")} • ${formatInt(snapshot.distanceMeters)} ${appString("notification_distance_m")} • ${formatInt(snapshot.activeMinutes)} ${appString("min")}"
            )
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
    }

    private fun buildFallbackNotification(snapshot: TrackingSnapshot): Notification {
        return baseNotificationBuilder(snapshot).build()
    }

    private fun buildTrackingNotification(snapshot: TrackingSnapshot): Notification {
        val palette = WidgetTheme.palette(this)
        val widgetSnapshot = WidgetSnapshot(
            steps = snapshot.steps,
            goal = readDailyGoal().coerceAtLeast(1),
            calories = snapshot.calories,
            distanceMeters = snapshot.distanceMeters,
            activeMinutes = snapshot.activeMinutes
        )
        return baseNotificationBuilder(snapshot)
            .setColor(palette.primary)
            .setCustomContentView(TrackingNotificationViews.compact(this, widgetSnapshot, palette))
            .setCustomBigContentView(TrackingNotificationViews.expanded(this, widgetSnapshot, palette))
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .build()
    }

    private fun createChannels() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val tracking = NotificationChannel(
            TrackingConstants.TRACKING_CHANNEL_ID,
            appString("tracking_notification_channel"),
            NotificationManager.IMPORTANCE_LOW
        )
        val inactivity = NotificationChannel(
            TrackingConstants.INACTIVITY_CHANNEL_ID,
            appString("inactivity_notification_channel"),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(tracking)
        manager.createNotificationChannel(inactivity)
    }

    private fun readDailyGoal(): Int {
        return getSharedPreferences(TrackingConstants.PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(TrackingConstants.PREF_DAILY_GOAL, 8_000)
    }

    private fun formatInt(value: Int): String =
        java.text.NumberFormat.getIntegerInstance(appLocale()).format(value)

    companion object {
        private const val DATE_CHECK_INTERVAL_MS = 60_000L

        fun start(context: Context) {
            val intent = Intent(context, StepTrackingService::class.java)
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }
        }
    }
}
