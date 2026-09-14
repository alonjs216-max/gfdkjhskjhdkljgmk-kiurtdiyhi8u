package com.xaniihub.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xaniihub.app.localization.appLocale
import com.xaniihub.app.localization.appText
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ---------------------------------------------------------------------------------------------
//  Interactive charts: press (or press and drag) a bar / a heatmap cell to read its value
// ---------------------------------------------------------------------------------------------

private fun ColorScheme.chartSheen(alpha: Float): Color =
    if (background.luminance() < 0.5f) Color.White.copy(alpha = alpha)
    else onSurface.copy(alpha = alpha * 0.55f)

private fun Modifier.chartDescription(description: String?): Modifier =
    if (description == null) this else this.semantics { contentDescription = description }

private const val TABULAR_DIGITS = "tnum"

/** Small floating label that reports the value of the touched data point. */
@Composable
private fun ChartTooltip(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    width: Dp = 118.dp
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .width(width)
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.surface.copy(alpha = 0.94f))
            .drawBehind {
                drawRoundRect(
                    color = scheme.chartSheen(0.35f),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = TABULAR_DIGITS),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Same visual language as [TinyBarChart], but the chart reacts to touch: pressing anywhere on it
 * selects the nearest bar and shows a tooltip with the number of steps for that bar (and the
 * matching label, e.g. the date or the hour). Dragging across the chart moves the tooltip, and
 * releasing hides it.
 */
@Composable
fun InteractiveBarChart(
    values: List<Int>,
    modifier: Modifier = Modifier,
    labels: List<String> = emptyList(),
    height: Dp = 110.dp,
    contentDescription: String? = null
) {
    if (values.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val max = values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val peakIndex = values.indices.maxByOrNull { values[it] } ?: -1
    val locale = appLocale()
    val integerFormat = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val stepsTemplate = appText("steps_value")
    var active by remember(values) { mutableIntStateOf(-1) }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val count = values.size
        val gap = if (count > 12) 3.dp else 6.dp
        val barWidth = ((maxWidth - gap * (count - 1)) / count).coerceAtLeast(1.dp)
        val tooltipWidth = 118.dp

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .chartDescription(contentDescription)
                .pointerInput(count) {
                    awaitEachGesture {
                        fun indexAt(x: Float): Int {
                            val slot = size.width.toFloat() / count
                            if (slot <= 0f) return -1
                            return (x / slot).toInt().coerceIn(0, count - 1)
                        }
                        val down = awaitFirstDown(requireUnconsumed = false)
                        active = indexAt(down.position.x)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            active = indexAt(change.position.x)
                        }
                        active = -1
                    }
                }
        ) {
            val gapPx = gap.toPx()
            val barW = (size.width - gapPx * (count - 1)) / count
            val radius = CornerRadius(barW / 2f, barW / 2f)
            listOf(0.25f, 0.5f, 0.75f).forEach { f ->
                val y = size.height * (1f - f)
                drawLine(
                    color = scheme.onSurface.copy(alpha = 0.06f),
                    start = Offset(0f, y), end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            drawLine(
                color = scheme.onSurface.copy(alpha = 0.14f),
                start = Offset(0f, size.height), end = Offset(size.width, size.height),
                strokeWidth = 1.dp.toPx()
            )
            values.forEachIndexed { i, v ->
                val x = i * (barW + gapPx)
                val minH = 3.dp.toPx()
                val h = (size.height * (v.toFloat() / max)).coerceAtLeast(minH)
                val top = size.height - h
                val isActive = i == active
                val isPeak = i == peakIndex && v > 0
                if (isActive) {
                    // Vertical guide for the touched bar.
                    drawLine(
                        color = scheme.chartSheen(0.30f),
                        start = Offset(x + barW / 2f, 0f),
                        end = Offset(x + barW / 2f, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                if (v <= 0) {
                    drawRoundRect(
                        color = scheme.onSurface.copy(alpha = if (isActive) 0.16f else 0.06f),
                        topLeft = Offset(x, size.height - minH),
                        size = Size(barW, minH),
                        cornerRadius = radius
                    )
                    return@forEachIndexed
                }
                if (isPeak || isActive) {
                    drawRoundRect(
                        color = (if (isActive) scheme.tertiary else scheme.secondary).copy(alpha = if (isActive) 0.36f else 0.28f),
                        topLeft = Offset(x - 3.dp.toPx(), top - 3.dp.toPx()),
                        size = Size(barW + 6.dp.toPx(), h + 6.dp.toPx()),
                        cornerRadius = CornerRadius(barW / 2f + 3.dp.toPx(), barW / 2f + 3.dp.toPx())
                    )
                }
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = when {
                            isActive -> listOf(scheme.tertiary, scheme.primary, scheme.primary.copy(alpha = 0.7f))
                            isPeak -> listOf(scheme.secondary, scheme.primary, scheme.primary.copy(alpha = 0.55f))
                            else -> listOf(scheme.primary.copy(alpha = 0.95f), scheme.primary.copy(alpha = 0.35f))
                        },
                        startY = top, endY = size.height
                    ),
                    topLeft = Offset(x, top),
                    size = Size(barW, h),
                    cornerRadius = radius
                )
            }
        }

        if (active in values.indices) {
            val center = barWidth / 2 + (barWidth + gap) * active
            val x = (center - tooltipWidth / 2).coerceIn(0.dp, (maxWidth - tooltipWidth).coerceAtLeast(0.dp))
            ChartTooltip(
                title = labels.getOrElse(active) { "" },
                value = stepsTemplate.format(integerFormat.format(values[active])),
                width = tooltipWidth,
                modifier = Modifier.offset(x = x)
            )
        }
    }
}

/**
 * GitHub-style activity heatmap where tapping a cell shows the date and how many steps were
 * walked that day. Tapping the selected cell again clears the selection.
 */
@Composable
fun InteractiveHeatMap(
    values: List<Int>,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val data = values.ifEmpty { List(35) { 0 } }
    val startDate = LocalDate.now().minusDays((data.size - 1).toLong())
    val leadingBlanks = startDate.dayOfWeek.value - 1
    val normalized: List<Int?> = List(leadingBlanks) { null } + data
    val max = data.maxOrNull()?.coerceAtLeast(1) ?: 1
    val columns = 7
    val rows = (normalized.size + columns - 1) / columns
    val weekdayLabels = appText("weekday_letters").split(",")
    val scheme = MaterialTheme.colorScheme
    val todayOutline = scheme.chartSheen(0.8f)
    val locale = appLocale()
    val integerFormat = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("d MMMM yyyy", locale) }
    val stepsTemplate = appText("steps_value")
    val activeDays = data.count { it > 0 }
    val activeAverage = if (activeDays == 0) 0 else data.filter { it > 0 }.sum() / activeDays
    val description = contentDescription ?: appText("heatmap_desc").format(
        integerFormat.format(activeDays),
        integerFormat.format(activeAverage)
    )
    var selected by remember(values) { mutableIntStateOf(-1) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .chartDescription(description),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Read-out line for the selected day; kept at a constant height so the grid never jumps.
        Box(modifier = Modifier.fillMaxWidth().height(18.dp), contentAlignment = Alignment.CenterStart) {
            val dayIndex = selected - leadingBlanks
            if (dayIndex in data.indices) {
                Text(
                    text = "${startDate.plusDays(dayIndex.toLong()).format(dateFormatter)} • " +
                        stepsTemplate.format(integerFormat.format(data[dayIndex])),
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = TABULAR_DIGITS),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val gap = 6.dp
            val cell = ((maxWidth - gap * (columns - 1)) / columns).coerceAtLeast(18.dp)
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    repeat(columns) { col ->
                        Box(modifier = Modifier.width(cell), contentAlignment = Alignment.Center) {
                            Text(
                                text = weekdayLabels.getOrElse(col) { "" },
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                }
                repeat(rows) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(columns) { col ->
                            val index = row * columns + col
                            if (index < normalized.size) {
                                val value = normalized[index]
                                if (value == null) {
                                    Spacer(Modifier.size(cell))
                                } else {
                                    val intensity = if (value <= 0) 0f else (value.toFloat() / max).coerceIn(0.18f, 1f)
                                    val isToday = index == normalized.lastIndex
                                    val isSelected = index == selected
                                    Box(
                                        modifier = Modifier
                                            .size(cell)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (intensity == 0f) scheme.onSurface.copy(alpha = 0.06f)
                                                else Color.Transparent
                                            )
                                            .then(
                                                if (intensity > 0f) Modifier.background(
                                                    Brush.verticalGradient(
                                                        listOf(
                                                            scheme.secondary.copy(alpha = intensity),
                                                            scheme.primary.copy(alpha = intensity)
                                                        )
                                                    )
                                                ) else Modifier
                                            )
                                            .clickable { selected = if (isSelected) -1 else index }
                                            .drawBehind {
                                                if (isSelected) {
                                                    drawRoundRect(
                                                        color = scheme.tertiary,
                                                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                                        style = Stroke(width = 2.dp.toPx())
                                                    )
                                                } else if (isToday) {
                                                    drawRoundRect(
                                                        color = todayOutline,
                                                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                                        style = Stroke(width = 1.5.dp.toPx())
                                                    )
                                                }
                                            }
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.size(cell))
                            }
                        }
                    }
                }
            }
        }
    }
}
