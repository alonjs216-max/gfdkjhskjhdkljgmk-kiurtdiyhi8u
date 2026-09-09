package com.xaniihub.app.tracking

import android.content.Context
import android.util.TypedValue
import android.widget.RemoteViews
import com.xaniihub.app.R
import com.xaniihub.app.localization.appString
import com.xaniihub.app.widget.WidgetBinder
import com.xaniihub.app.widget.WidgetGraphics
import com.xaniihub.app.widget.WidgetPalette
import com.xaniihub.app.widget.WidgetSnapshot
import com.xaniihub.app.widget.WidgetTheme

/**
 * RemoteViews for the persistent tracking notification, drawn with the same ring, gradient
 * progress line and glass tiles as the widgets and the home screen, and tinted with the
 * palette selected in the app.
 */
object TrackingNotificationViews {

    /** Collapsed row: small ring, steps, three metrics. */
    fun compact(context: Context, snapshot: WidgetSnapshot, palette: WidgetPalette): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.notification_tracking_compact)
        views.setImageViewBitmap(
            R.id.nRingImg,
            WidgetGraphics.ring(context, palette, snapshot.progress, 46f, 5f, ticks = false)
        )
        bindShared(views, snapshot, palette)
        // Adaptive size so values up to 100k stay on one line.
        val stepsSize = when {
            snapshot.steps < 10_000 -> 26f
            snapshot.steps < 100_000 -> 22f
            else -> 18f
        }
        views.setTextViewTextSize(R.id.nSteps, TypedValue.COMPLEX_UNIT_SP, stepsSize)
        return views
    }

    /** Expanded card: header with percent pill, hero ring, progress line, metric tiles. */
    fun expanded(context: Context, snapshot: WidgetSnapshot, palette: WidgetPalette): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.notification_tracking_expanded)
        val accent = WidgetBinder.accent(snapshot, palette)

        views.setImageViewBitmap(R.id.nDot, WidgetGraphics.dot(context, accent, 7f))
        views.setTextViewText(R.id.nTitle, appString("widget_title"))
        WidgetBinder.bindPercent(views, R.id.nPercentPill, snapshot, palette)

        views.setImageViewBitmap(
            R.id.nRingImg,
            WidgetGraphics.ring(context, palette, snapshot.progress, 72f, 6.5f, ticks = true)
        )
        bindShared(views, snapshot, palette)
        val stepsSize = when {
            snapshot.steps < 10_000 -> 30f
            snapshot.steps < 100_000 -> 26f
            else -> 22f
        }
        views.setTextViewTextSize(R.id.nSteps, TypedValue.COMPLEX_UNIT_SP, stepsSize)

        views.setImageViewBitmap(
            R.id.nBar,
            WidgetGraphics.progressBar(context, palette, snapshot.progress, 260f, 7f)
        )
        views.setTextViewText(
            R.id.nRemaining,
            WidgetBinder.remainingText(snapshot) + "  ·  " + WidgetBinder.goalText(snapshot)
        )
        if (snapshot.goalReached) views.setTextColor(R.id.nRemaining, palette.overflow)
        return views
    }

    private fun bindShared(views: RemoteViews, snapshot: WidgetSnapshot, palette: WidgetPalette) {
        views.setTextViewText(R.id.nRingText, WidgetBinder.percentText(snapshot))
        views.setTextColor(R.id.nRingText, if (snapshot.goalReached) palette.overflow else palette.textPrimary)

        views.setTextViewText(R.id.nSteps, WidgetTheme.formatInt(snapshot.steps))
        if (snapshot.goalReached) views.setTextColor(R.id.nSteps, palette.overflow)
        views.setTextViewText(R.id.nStepsLabel, WidgetBinder.stepsLabel())

        WidgetBinder.bindCalories(views, R.id.nCalories, R.id.nCaloriesLabel, snapshot.calories)
        views.setTextColor(R.id.nCalories, palette.secondary)
        WidgetBinder.bindDistance(views, R.id.nDistance, R.id.nDistanceLabel, snapshot.distanceMeters)
        views.setTextColor(R.id.nDistance, palette.tertiary)
        WidgetBinder.bindMinutes(views, R.id.nMinutes, R.id.nMinutesLabel, snapshot.activeMinutes)
        views.setTextColor(R.id.nMinutes, palette.primary)
    }
}
