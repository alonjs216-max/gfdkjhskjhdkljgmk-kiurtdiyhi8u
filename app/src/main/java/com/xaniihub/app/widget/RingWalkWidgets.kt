package com.xaniihub.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.quicksettings.TileService
import android.widget.RemoteViews
import com.xaniihub.app.MainActivity
import com.xaniihub.app.R
import com.xaniihub.app.localization.AppLanguageController
import com.xaniihub.app.localization.appString
import com.xaniihub.app.tile.StepsTileService
import com.xaniihub.app.tracking.TrackingConstants
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Size the launcher currently gives a widget instance, in dp (0 when unknown). */
data class WidgetSize(val widthDp: Int, val heightDp: Int) {
    fun width(default: Int): Int = if (widthDp > 0) widthDp else default
    fun height(default: Int): Int = if (heightDp > 0) heightDp else default
}

/**
 * Common plumbing for every RingWalk home-screen widget.
 *
 * Subclasses only describe *what* to draw ([build]); this class takes care of *when*:
 * launcher update requests, resizes and midnight/time-zone changes. Rendering happens in two
 * passes so the widget is never blank – an instant one from the SharedPreferences cache and a
 * second one with the real seven-day history from Room.
 */
abstract class BaseWidgetProvider : AppWidgetProvider() {

    abstract fun build(
        context: Context,
        size: WidgetSize,
        snapshot: WidgetSnapshot,
        palette: WidgetPalette
    ): RemoteViews

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refresh(context, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        refresh(context, intArrayOf(appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in RingWalkWidgets.DATE_CHANGE_ACTIONS) {
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, javaClass))
            refresh(context, ids)
            runCatching {
                TileService.requestListeningState(context, ComponentName(context, StepsTileService::class.java))
            }
        }
    }

    private fun refresh(context: Context, ids: IntArray) {
        if (ids.isEmpty()) return
        val manager = AppWidgetManager.getInstance(context)
        RingWalkWidgets.render(context, manager, this, ids, WidgetRepository.cached(context))

        val pending = runCatching { goAsync() }.getOrNull()
        RingWalkWidgets.scope.launch {
            runCatching {
                val full = WidgetRepository.load(context)
                RingWalkWidgets.render(context, manager, this@BaseWidgetProvider, ids, full)
            }
            pending?.finish()
        }
    }
}

/** Registry + shared rendering helpers for all widgets. */
object RingWalkWidgets {

    internal val DATE_CHANGE_ACTIONS = setOf(
        Intent.ACTION_DATE_CHANGED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED
    )

    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val providers: List<Class<out BaseWidgetProvider>> = listOf(
        StepsWidgetProvider::class.java,
        CompactWidgetProvider::class.java,
        StatsWidgetProvider::class.java,
        WeekWidgetProvider::class.java
    )

    /**
     * Called by the tracking service after every sensor batch: persists the latest numbers for
     * the widgets, the tile and the notification, then repaints every placed widget.
     */
    suspend fun publish(
        context: Context,
        steps: Int,
        goal: Int,
        calories: Int,
        distanceMeters: Int,
        activeMinutes: Int
    ) {
        runCatching {
            context.getSharedPreferences(TrackingConstants.PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putInt(TrackingConstants.PREF_LATEST_STEPS, steps)
                .putLong(TrackingConstants.PREF_LATEST_STEPS_DATE, LocalDate.now().toEpochDay())
                .putInt(TrackingConstants.PREF_DAILY_GOAL, goal)
                .apply()
        }
        WidgetRepository.save(context, steps, goal, calories, distanceMeters, activeMinutes)
        refreshAll(context)
    }

    /** Repaints every placed widget with fresh data. Safe to call from any thread. */
    suspend fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val targets = providers.mapNotNull { cls ->
            val ids = runCatching { manager.getAppWidgetIds(ComponentName(context, cls)) }.getOrNull()
            if (ids == null || ids.isEmpty()) null else cls to ids
        }
        if (targets.isEmpty()) return
        val snapshot = WidgetRepository.load(context)
        targets.forEach { (cls, ids) ->
            runCatching {
                render(context, manager, cls.getDeclaredConstructor().newInstance(), ids, snapshot)
            }
        }
    }

    /** Fire-and-forget variant for UI callers (palette / language / goal changes). */
    fun refreshAllAsync(context: Context) {
        val app = context.applicationContext
        scope.launch {
            WidgetGraphics.clearCache()
            runCatching { refreshAll(app) }
        }
    }

    internal fun render(
        context: Context,
        manager: AppWidgetManager,
        provider: BaseWidgetProvider,
        ids: IntArray,
        snapshot: WidgetSnapshot
    ) {
        runCatching { AppLanguageController.init(context) }
        val palette = WidgetTheme.palette(context)
        ids.forEach { id ->
            runCatching {
                val options = manager.getAppWidgetOptions(id)
                val size = WidgetSize(
                    widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0),
                    heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
                )
                val views = provider.build(context, size, snapshot, palette)
                views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
                manager.updateAppWidget(id, views)
            }
        }
    }

    fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        100,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

/**
 * Small binding helpers shared by the widgets and the tracking notification so every surface
 * formats numbers, units and goal copy identically.
 */
object WidgetBinder {

    /** Accent for "reached" states – gold once the goal is done, otherwise the palette primary. */
    fun accent(snapshot: WidgetSnapshot, palette: WidgetPalette): Int =
        if (snapshot.goalReached) palette.overflow else palette.primary

    fun percentText(snapshot: WidgetSnapshot): String = "${snapshot.percent}%"

    fun bindPercent(views: RemoteViews, id: Int, snapshot: WidgetSnapshot, palette: WidgetPalette) {
        views.setTextViewText(id, percentText(snapshot))
        views.setTextColor(id, accent(snapshot, palette))
    }

    fun goalText(snapshot: WidgetSnapshot): String =
        appString("widget_goal").format(WidgetTheme.formatInt(snapshot.goal))

    fun remainingText(snapshot: WidgetSnapshot): String = if (snapshot.goalReached) {
        appString("widget_goal_reached")
    } else {
        appString("widget_remaining").format(WidgetTheme.formatInt(snapshot.remaining))
    }

    fun bindGoalLine(views: RemoteViews, goalId: Int, remainingId: Int, snapshot: WidgetSnapshot, palette: WidgetPalette) {
        views.setTextViewText(goalId, goalText(snapshot))
        views.setTextViewText(remainingId, remainingText(snapshot))
        if (snapshot.goalReached) views.setTextColor(remainingId, palette.overflow)
    }

    /** Distance switches to kilometres from 1 000 m. */
    fun bindDistance(views: RemoteViews, valueId: Int, labelId: Int, meters: Int) {
        if (meters >= 1_000) {
            views.setTextViewText(valueId, WidgetTheme.formatDecimal(meters / 1000.0))
            views.setTextViewText(labelId, appString("km"))
        } else {
            views.setTextViewText(valueId, WidgetTheme.formatInt(meters))
            views.setTextViewText(labelId, appString("notification_distance_m"))
        }
    }

    /** Active time switches to hours from 60 min. */
    fun bindMinutes(views: RemoteViews, valueId: Int, labelId: Int, minutes: Int) {
        if (minutes >= 60) {
            views.setTextViewText(valueId, WidgetTheme.formatDecimal(minutes / 60.0))
            views.setTextViewText(labelId, appString("hour_short"))
        } else {
            views.setTextViewText(valueId, WidgetTheme.formatInt(minutes))
            views.setTextViewText(labelId, appString("min"))
        }
    }

    fun bindCalories(views: RemoteViews, valueId: Int, labelId: Int, calories: Int) {
        views.setTextViewText(valueId, WidgetTheme.formatInt(calories))
        views.setTextViewText(labelId, appString("kcal"))
    }

    fun stepsLabel(): String = appString("steps_metric").lowercase()

    /** Weekday captions for the last seven days, ending with today. */
    fun weekLabels(): List<String> {
        val letters = appString("weekday_letters").split(",").map { it.trim() }
        val today = LocalDate.now()
        return (6 downTo 0).map { back ->
            val index = today.minusDays(back.toLong()).dayOfWeek.value - 1 // Monday = 0
            letters.getOrNull(index).orEmpty()
        }
    }
}
