package com.xaniihub.app.widget

import android.content.Context
import android.widget.RemoteViews
import com.xaniihub.app.R
import com.xaniihub.app.localization.appString
import kotlin.math.min

/**
 * The main "goal ring" widget. Adapts to the space the launcher gives it:
 *
 *  - **small** (2×2): only the hero ring with steps and percentage inside;
 *  - **medium** (3–4×2, low): ring on the left, steps + gradient progress line + goal pill;
 *  - **large** (4×3+): header, ring, four metric tiles and the seven-day chart.
 */
class StepsWidgetProvider : BaseWidgetProvider() {

    override fun build(
        context: Context,
        size: WidgetSize,
        snapshot: WidgetSnapshot,
        palette: WidgetPalette
    ): RemoteViews {
        val w = size.width(110)
        val h = size.height(110)
        return when {
            w >= 250 && h >= 190 -> large(context, w, h, snapshot, palette)
            w >= 170 -> medium(context, w, snapshot, palette)
            else -> small(context, w, h, snapshot, palette)
        }
    }

    private fun small(context: Context, w: Int, h: Int, snapshot: WidgetSnapshot, palette: WidgetPalette): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_steps_small)
        val ringDp = (min(w, h) - 16).coerceIn(70, 220)
        views.setImageViewBitmap(
            R.id.widgetRing,
            WidgetGraphics.ring(context, palette, snapshot.progress, ringDp.toFloat(), ringDp * 0.10f, ticks = ringDp >= 96)
        )
        views.setTextViewText(R.id.widgetSteps, WidgetTheme.formatCompact(snapshot.steps))
        views.setTextViewTextSize(R.id.widgetSteps, android.util.TypedValue.COMPLEX_UNIT_SP, if (ringDp >= 120) 24f else 20f)
        if (snapshot.goalReached) views.setTextColor(R.id.widgetSteps, palette.overflow)
        WidgetBinder.bindPercent(views, R.id.widgetPercent, snapshot, palette)
        return views
    }

    private fun medium(context: Context, w: Int, snapshot: WidgetSnapshot, palette: WidgetPalette): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_steps_medium)
        views.setImageViewBitmap(R.id.widgetRing, WidgetGraphics.ring(context, palette, snapshot.progress, 82f, 8f, ticks = false))
        WidgetBinder.bindPercent(views, R.id.widgetPercent, snapshot, palette)
        views.setImageViewBitmap(R.id.widgetDot, WidgetGraphics.dot(context, WidgetBinder.accent(snapshot, palette)))
        views.setTextViewText(R.id.widgetTitle, appString("widget_title"))
        views.setTextViewText(R.id.widgetSteps, WidgetTheme.formatInt(snapshot.steps))
        if (snapshot.goalReached) views.setTextColor(R.id.widgetSteps, palette.overflow)
        val barWidth = (w - 124).coerceAtLeast(60)
        views.setImageViewBitmap(
            R.id.widgetBar,
            WidgetGraphics.progressBar(context, palette, snapshot.progress, barWidth.toFloat(), 7f)
        )
        WidgetBinder.bindGoalLine(views, R.id.widgetGoal, R.id.widgetRemaining, snapshot, palette)
        return views
    }

    private fun large(context: Context, w: Int, h: Int, snapshot: WidgetSnapshot, palette: WidgetPalette): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_steps_large)
        views.setImageViewBitmap(R.id.widgetDot, WidgetGraphics.dot(context, WidgetBinder.accent(snapshot, palette)))
        views.setTextViewText(R.id.widgetTitle, appString("widget_title"))
        WidgetBinder.bindPercent(views, R.id.widgetPercent, snapshot, palette)

        views.setImageViewBitmap(R.id.widgetRing, WidgetGraphics.ring(context, palette, snapshot.progress, 88f, 8f, ticks = false))
        views.setTextViewText(R.id.widgetSteps, WidgetTheme.formatCompact(snapshot.steps))
        if (snapshot.goalReached) views.setTextColor(R.id.widgetSteps, palette.overflow)
        views.setTextViewText(R.id.widgetStepsLabel, WidgetBinder.stepsLabel())

        WidgetBinder.bindCalories(views, R.id.widgetCalories, R.id.widgetCaloriesLabel, snapshot.calories)
        views.setTextColor(R.id.widgetCalories, palette.secondary)
        WidgetBinder.bindDistance(views, R.id.widgetDistance, R.id.widgetDistanceLabel, snapshot.distanceMeters)
        views.setTextColor(R.id.widgetDistance, palette.tertiary)
        WidgetBinder.bindMinutes(views, R.id.widgetMinutes, R.id.widgetMinutesLabel, snapshot.activeMinutes)
        views.setTextColor(R.id.widgetMinutes, palette.primary)
        views.setTextViewText(R.id.widgetStreak, WidgetTheme.formatInt(snapshot.streakDays))
        views.setTextViewText(R.id.widgetStreakLabel, appString("widget_streak_label"))
        views.setTextColor(R.id.widgetStreak, palette.overflow)

        val chartWidth = (w - 28).coerceAtLeast(120)
        val chartHeight = (h - 170).coerceIn(38, 96)
        views.setImageViewBitmap(
            R.id.widgetWeek,
            WidgetGraphics.weekChart(
                context, palette, snapshot.week, snapshot.goal, WidgetBinder.weekLabels(),
                chartWidth.toFloat(), chartHeight.toFloat(), showLabels = chartHeight >= 48
            )
        )
        return views
    }
}