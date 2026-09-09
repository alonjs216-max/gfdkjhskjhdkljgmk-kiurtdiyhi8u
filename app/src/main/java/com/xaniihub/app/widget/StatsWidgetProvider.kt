package com.xaniihub.app.widget

import android.content.Context
import android.widget.RemoteViews
import com.xaniihub.app.R
import com.xaniihub.app.localization.appString

/**
 * Metrics dashboard (4×2): four glass tiles – steps, calories, distance, active time – over a
 * gradient progress line with the goal and the remaining steps, like the metric grid on the
 * home screen.
 */
class StatsWidgetProvider : BaseWidgetProvider() {

    override fun build(
        context: Context,
        size: WidgetSize,
        snapshot: WidgetSnapshot,
        palette: WidgetPalette
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_stats)
        views.setImageViewBitmap(R.id.widgetDot, WidgetGraphics.dot(context, WidgetBinder.accent(snapshot, palette)))
        views.setTextViewText(R.id.widgetTitle, appString("widget_title"))
        WidgetBinder.bindPercent(views, R.id.widgetPercent, snapshot, palette)

        views.setTextViewText(R.id.widgetSteps, WidgetTheme.formatInt(snapshot.steps))
        if (snapshot.goalReached) views.setTextColor(R.id.widgetSteps, palette.overflow)
        views.setTextViewText(R.id.widgetStepsLabel, WidgetBinder.stepsLabel())
        WidgetBinder.bindCalories(views, R.id.widgetCalories, R.id.widgetCaloriesLabel, snapshot.calories)
        views.setTextColor(R.id.widgetCalories, palette.secondary)
        WidgetBinder.bindDistance(views, R.id.widgetDistance, R.id.widgetDistanceLabel, snapshot.distanceMeters)
        views.setTextColor(R.id.widgetDistance, palette.tertiary)
        WidgetBinder.bindMinutes(views, R.id.widgetMinutes, R.id.widgetMinutesLabel, snapshot.activeMinutes)
        views.setTextColor(R.id.widgetMinutes, palette.primary)

        val barWidth = (size.width(250) - 26).coerceAtLeast(120)
        views.setImageViewBitmap(
            R.id.widgetBar,
            WidgetGraphics.progressBar(context, palette, snapshot.progress, barWidth.toFloat(), 7f)
        )
        WidgetBinder.bindGoalLine(views, R.id.widgetGoal, R.id.widgetRemaining, snapshot, palette)
        return views
    }
}