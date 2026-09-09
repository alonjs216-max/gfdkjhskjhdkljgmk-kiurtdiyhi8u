package com.xaniihub.app.widget

import android.content.Context
import android.widget.RemoteViews
import com.xaniihub.app.R
import com.xaniihub.app.localization.appString

/**
 * Seven-day overview (4×2): the analytics bar chart with the goal line, plus the weekly total,
 * daily average and the current goal streak.
 */
class WeekWidgetProvider : BaseWidgetProvider() {

    override fun build(
        context: Context,
        size: WidgetSize,
        snapshot: WidgetSnapshot,
        palette: WidgetPalette
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_week)
        val w = size.width(250)
        val h = size.height(110)

        views.setImageViewBitmap(R.id.widgetDot, WidgetGraphics.dot(context, WidgetBinder.accent(snapshot, palette)))
        views.setTextViewText(R.id.widgetTitle, appString("chart_week"))
        views.setTextViewText(R.id.widgetWeekTotal, appString("widget_week_total").format(WidgetTheme.formatCompact(snapshot.weekTotal)))
        views.setTextColor(R.id.widgetWeekTotal, palette.primary)

        val chartWidth = (w - 26).coerceAtLeast(120)
        val chartHeight = (h - 74).coerceIn(44, 180)
        views.setImageViewBitmap(
            R.id.widgetWeek,
            WidgetGraphics.weekChart(
                context, palette, snapshot.week, snapshot.goal, WidgetBinder.weekLabels(),
                chartWidth.toFloat(), chartHeight.toFloat(), showLabels = true
            )
        )

        val activeDays = snapshot.week.count { it > 0 }.coerceAtLeast(1)
        val average = snapshot.weekTotal / activeDays
        views.setTextViewText(R.id.widgetWeekAvg, appString("widget_week_avg").format(WidgetTheme.formatInt(average)))
        views.setTextViewText(R.id.widgetStreak, appString("streak_short").format(WidgetTheme.formatInt(snapshot.streakDays)))
        views.setTextColor(R.id.widgetStreak, if (snapshot.streakDays > 0) palette.overflow else palette.textMuted)
        return views
    }
}