package com.xaniihub.app.tracking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
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
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@AndroidEntryPoint
class StepTrackingService : Service(), SensorEventListener {

    @Inject
    lateinit var repository: XaniiRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sensorIngestMutex = Mutex()
    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private var latestStepCounter = 0
    private var lastEventTime = 0L
    private var cadenceWindowStartTime = 0L
    private var cadenceWindowSteps = 0
    private var latestCadence = 0f

    override fun onCreate() {
        super.onCreate()
        // Restore the last known raw sensor counter so cadence isn't computed against 0
        // after a mid-day service restart (which would otherwise treat the first reading
        // as a huge step delta / spike).
        getSharedPreferences(TrackingConstants.PREFS_NAME, Context.MODE_PRIVATE).also { preferences ->
            latestStepCounter = preferences.getInt(TrackingConstants.PREF_LATEST_COUNTER, 0)
            lastEventTime = preferences.getLong(TrackingConstants.PREF_LAST_SENSOR_EVENT_TIME, 0L)
        }
        createChannels()
        val started = runCatching {
            startForeground(
                TrackingConstants.TRACKING_NOTIFICATION_ID,
                buildFallbackNotification(TrackingSnapshot(0, 0, 0, 0))
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
        sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        sensorManager?.unregisterListener(this)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return
        val total = event.values.firstOrNull() ?: return
        val now = System.currentTimeMillis()
        val previousCounter = latestStepCounter
        val stepDelta = (total.toInt() - previousCounter).coerceAtLeast(0)
        if (cadenceWindowStartTime == 0L) {
            cadenceWindowStartTime = lastEventTime.takeIf { now - it <= 60_000L } ?: now
        }
        cadenceWindowSteps += stepDelta
        val windowElapsedMs = now - cadenceWindowStartTime
        if (windowElapsedMs >= 60_000L) {
            latestCadence = cadenceWindowSteps / (windowElapsedMs / 60_000f)
            cadenceWindowStartTime = now
            cadenceWindowSteps = 0
        }
        latestStepCounter = total.toInt()
        lastEventTime = now
        val cadence = latestCadence
        scope.launch {
            sensorIngestMutex.withLock {
                repository.ingestSensorTotal(total, cadence)
                val snapshot = repository.getTrackingSnapshot()
                saveTrackingState(latestStepCounter, lastEventTime, snapshot.steps)
                updateForegroundNotification(snapshot)
                RingWalkWidgets.publish(
                    context = this@StepTrackingService,
                    steps = snapshot.steps,
                    goal = readDailyGoal(),
                    calories = snapshot.calories,
                    distanceMeters = snapshot.distanceMeters,
                    activeMinutes = snapshot.activeMinutes
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

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

    private fun saveTrackingState(counter: Int, eventTime: Long, steps: Int) {
        getSharedPreferences(TrackingConstants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(TrackingConstants.PREF_LATEST_COUNTER, counter)
            .putLong(TrackingConstants.PREF_LAST_SENSOR_EVENT_TIME, eventTime)
            .putInt(TrackingConstants.PREF_LATEST_STEPS, steps)
            .putLong(TrackingConstants.PREF_LATEST_STEPS_DATE, java.time.LocalDate.now().toEpochDay())
            .apply()
    }

    private fun readDailyGoal(): Int {
        return getSharedPreferences(TrackingConstants.PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(TrackingConstants.PREF_DAILY_GOAL, 8_000)
    }

    private fun formatInt(value: Int): String =
        java.text.NumberFormat.getIntegerInstance(appLocale()).format(value)

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, StepTrackingService::class.java)
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }
        }
    }
}
