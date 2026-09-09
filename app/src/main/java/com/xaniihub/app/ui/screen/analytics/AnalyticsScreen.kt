package com.xaniihub.app.ui.screen.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xaniihub.app.ui.components.HeatMap
import com.xaniihub.app.ui.components.MetricCard
import com.xaniihub.app.ui.components.PremiumCard
import com.xaniihub.app.ui.components.TinyBarChart
import com.xaniihub.app.localization.AppLanguageController
import com.xaniihub.app.localization.appLocale
import com.xaniihub.app.localization.appText
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private fun formatInt(value: Number): String =
    java.text.NumberFormat.getIntegerInstance(appLocale()).format(value.toLong())

@Composable
fun AnalyticsScreen(
    paddingValues: PaddingValues,
    state: AnalyticsUiState
) {
    val overview = state.overview
    var selected by remember { mutableIntStateOf(1) }
    val recentBars = when (selected) {
        0 -> state.hourlySteps.take(24).let { values ->
            if (values.isEmpty()) List(24) { 0 } else values + List((24 - values.size).coerceAtLeast(0)) { 0 }
        }
        1 -> state.heatMap.takeLast(7)
        2 -> state.heatMap.takeLast(30)
        else -> aggregateCalendarMonths(state.heatMap)
    }
    val chartTitle = when (selected) {
        0 -> appText("chart_day")
        1 -> appText("chart_week")
        2 -> appText("chart_month")
        else -> appText("chart_year")
    }
    val language = AppLanguageController.language
    val bestDayFormatter = remember(language) {
        DateTimeFormatter.ofPattern("d MMM yyyy", appLocale(language))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(appText("statistics"), style = MaterialTheme.typography.headlineLarge)
                Text(
                    appText("stats_subtitle"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            androidx.compose.material3.Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                .padding(4.dp)
        ) {
            listOf(appText("day"), appText("week"), appText("month"), appText("year")).forEachIndexed { index, label ->
                val active = selected == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .then(
                            if (active) Modifier.background(
                                Brush.horizontalGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                )
                            ) else Modifier
                        )
                        .clickable { selected = index }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            MetricCard(
                title = appText("year_steps"),
                value = formatInt(overview?.yearlySteps ?: 0L),
                modifier = Modifier.fillMaxWidth()
            )
            MetricCard(
                title = appText("average_day"),
                value = formatInt(overview?.averageSteps ?: 0),
                modifier = Modifier.fillMaxWidth()
            )
            MetricCard(
                title = appText("best_day"),
                value = overview?.mostActiveDay?.let {
                    "${formatInt(it.steps)} • ${it.date.format(bestDayFormatter)}"
                } ?: "—",
                modifier = Modifier.fillMaxWidth()
            )

            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(chartTitle, fontWeight = FontWeight.SemiBold)
                    TinyBarChart(values = recentBars.ifEmpty { List(7) { 0 } })
                }
            }

            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(appText("comparison"), fontWeight = FontWeight.SemiBold)
                    Text(
                        appText("prev_month").format(
                            String.format(appLocale(language), "%.1f", overview?.monthComparisonPercent ?: 0f)
                        )
                    )
                    Text(appText("year_forecast").format(overview?.forecastYearlySteps ?: 0L))
                }
            }

            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(appText("activity_heatmap"), fontWeight = FontWeight.SemiBold)
                    HeatMap(values = state.heatMap.takeLast(35))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun aggregateCalendarMonths(values: List<Int>): List<Int> {
    val today = LocalDate.now()
    val firstDate = today.minusDays((values.size - 1).coerceAtLeast(0).toLong())
    val totals = values.mapIndexed { index, steps -> firstDate.plusDays(index.toLong()) to steps }
        .groupBy({ YearMonth.from(it.first) }, { it.second })
        .mapValues { (_, steps) -> steps.sum() }
    val currentMonth = YearMonth.from(today)
    return (11 downTo 0).map { offset -> totals[currentMonth.minusMonths(offset.toLong())] ?: 0 }
}