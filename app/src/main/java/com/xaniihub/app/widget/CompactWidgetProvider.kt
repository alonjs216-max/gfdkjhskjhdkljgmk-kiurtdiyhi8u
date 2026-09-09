package com.xaniihub.app.widget

import android.content.Context
import android.widget.RemoteViews
import com.xaniihub.app.R

/**
 * One-row strip (4×1): tiny ring, steps, calories and the percentage pill.
 * Meant to sit above a dock or between icon rows without eating a full block.
 */
class CompactWidgetProvider : BaseWidgetProvider() {

    override fun build(
        context: Context,
        size: WidgetSize,
        snapshot: WidgetSnapshot,
        palette: WidgetPalette
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_compact)
        views.setImageViewBitmap(R.id.widgetRing, WidgetGraphics.ring(context, palette, snapshot.progress, 38f, 4.5f, ticks = false))
        views.setTextViewText(R.id.widgetSteps, WidgetTheme.formatInt(snapshot.steps))
        if (snapshot.goalReached) views.setTextColor(R.id.widgetSteps, palette.overflow)
        views.setTextViewText(R.id.widgetStepsLabel, WidgetBinder.stepsLabel())
        WidgetBinder.bindCalories(views, R.id.widgetCalories, R.id.widgetCaloriesLabel, snapshot.calories)
        views.setTextColor(R.id.widgetCalories, palette.secondary)
        WidgetBinder.bindPercent(views, R.id.widgetPercent, snapshot, palette)
        return views
    }
}