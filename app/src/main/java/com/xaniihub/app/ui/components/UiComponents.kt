package com.xaniihub.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xaniihub.app.localization.appText
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------------------------------------
//  Ambient background
// ---------------------------------------------------------------------------------------------

/**
 * Full-screen ambient backdrop: deep base colour with three slowly drifting, palette-tinted
 * light blobs and a bottom vignette. Every screen sits on top of this, so cards can be
 * translucent glass and still read well.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "aurora")
    val t1 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "aurora_1"
    )
    val t2 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "aurora_2"
    )
    val t3 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(30_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "aurora_3"
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .drawBehind {
                val w = size.width
                val h = size.height
                fun blob(color: Color, cx: Float, cy: Float, r: Float, alpha: Float) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = alpha), color.copy(alpha = alpha * 0.35f), Color.Transparent),
                            center = Offset(cx, cy),
                            radius = r
                        ),
                        radius = r,
                        center = Offset(cx, cy)
                    )
                }
                blob(scheme.primary, w * (0.15f + 0.25f * t1), h * (0.05f + 0.12f * t2), w * 0.85f, 0.26f)
                blob(scheme.tertiary, w * (0.95f - 0.25f * t2), h * (0.30f + 0.15f * t3), w * 0.75f, 0.18f)
                blob(scheme.secondary, w * (0.35f + 0.30f * t3), h * (0.85f - 0.10f * t1), w * 0.80f, 0.14f)
                // Vignette so the floating bottom bar and lower cards sit on a darker area.
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, scheme.background.copy(alpha = 0.55f), scheme.background.copy(alpha = 0.9f)),
                        startY = h * 0.45f,
                        endY = h
                    )
                )
            }
    ) {
        content()
    }
}

// ---------------------------------------------------------------------------------------------
//  Glass surfaces
// ---------------------------------------------------------------------------------------------

/**
 * Frosted-glass card: translucent surface, hairline gradient border with a bright top edge,
 * inner sheen and a soft palette-tinted drop shadow.
 */
@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    cornerRadius: androidx.compose.ui.unit.Dp = 28.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .shadow(
                elevation = 22.dp,
                shape = shape,
                ambientColor = accent.copy(alpha = 0.35f),
                spotColor = accent.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(scheme.surface.copy(alpha = 0.80f))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.09f),
                        accent.copy(alpha = 0.07f),
                        Color.Transparent,
                        accent.copy(alpha = 0.05f)
                    ),
                    start = Offset.Zero,
                    end = Offset(900f, 900f)
                )
            )
            .drawBehind {
                val strokeWidth = 1.2.dp.toPx()
                val radius = cornerRadius.toPx()
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.34f),
                            accent.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = strokeWidth),
                    topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth)
                )
            }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        content()
    }
}

/** Round glass button used for compact actions (calendar, day navigation). */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 46.dp,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(size)
            .shadow(10.dp, CircleShape, ambientColor = scheme.primary.copy(alpha = 0.25f), spotColor = scheme.primary.copy(alpha = 0.35f))
            .clip(CircleShape)
            .background(scheme.surface.copy(alpha = 0.78f))
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.16f), Color.Transparent, scheme.primary.copy(alpha = 0.10f))
                )
            )
            .drawBehind {
                drawCircle(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.40f), scheme.primary.copy(alpha = 0.18f), Color.Transparent)
                    ),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

/** Pill-shaped primary action with a two-tone gradient and a tinted glow shadow. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .shadow(14.dp, CircleShape, ambientColor = scheme.primary.copy(alpha = 0.45f), spotColor = scheme.primary.copy(alpha = 0.55f))
            .clip(CircleShape)
            .background(Brush.horizontalGradient(listOf(scheme.primary, scheme.secondary)))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(18.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = scheme.onPrimary, fontWeight = FontWeight.Bold)
    }
}

/** Small uppercase eyebrow label used above values and section titles. */
@Composable
fun Eyebrow(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/** Section header with optional trailing slot. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(10.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        trailing?.invoke()
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    PremiumCard(modifier = modifier, accent = accent, cornerRadius = 24.dp) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                }
            } else {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accent))
            }
            Eyebrow(title, modifier = Modifier.fillMaxWidth())
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
//  Goal ring
// ---------------------------------------------------------------------------------------------

/**
 * Hero progress ring.
 *
 *  - sixty tick marks around the outside light up as progress passes them
 *  - multi-pass glow underneath the arc
 *  - seamless three-colour sweep gradient (primary → secondary → tertiary)
 *  - bright "comet" head with a soft halo at the leading edge
 *  - a highlight travels along the arc so the ring feels alive
 *  - an orange completed-goal circle with a themed arc for each new lap
 */
@Composable
fun GoalRing(
    progress: Float,
    centerText: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null
) {
    val scheme = MaterialTheme.colorScheme
    val overflowColor = Color(0xFFFFC857)
    val safeProgress = if (progress.isFinite()) progress.coerceAtLeast(0f) else 0f
    val reached = safeProgress >= 1f

    val animated by animateFloatAsState(
        targetValue = safeProgress,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 90f),
        label = "goal_ring"
    )
    val infinite = rememberInfiniteTransition(label = "ring_fx")
    val shimmer by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_600, easing = LinearEasing)),
        label = "ring_shimmer"
    )
    val breathe by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ring_breathe"
    )
    val haloAlpha by animateFloatAsState(if (reached) 1f else 0f, tween(600), label = "ring_halo")
    val ringScale = if (reached) 1f + 0.012f * breathe else 1f

    Box(
        modifier = modifier.size(304.dp).scale(ringScale),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val stroke = 24.dp.toPx()
            val ringRadius = size.minDimension / 2f - 34.dp.toPx()
            val arcSize = Size(ringRadius * 2f, ringRadius * 2f)
            val arcTopLeft = Offset(center.x - ringRadius, center.y - ringRadius)
            val completedGoals = animated.toInt()
            val hasCompletedGoal = completedGoals > 0
            val remainderProgress = if (hasCompletedGoal) animated % 1f else animated
            val remainderSweep = (360f * remainderProgress).coerceIn(0f, 360f)
            val tickSweep = if (hasCompletedGoal) 360f else remainderSweep
            val arcColor = if (hasCompletedGoal) overflowColor else scheme.primary

            // Goal-reached halo.
            if (haloAlpha > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            arcColor.copy(alpha = (0.22f + 0.18f * breathe) * haloAlpha),
                            arcColor.copy(alpha = 0.06f * haloAlpha),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension / 2f
                    ),
                    radius = size.minDimension / 2f,
                    center = center
                )
            }

            // Tick marks.
            val tickCount = 60
            val tickOuter = ringRadius + stroke / 2f + 12.dp.toPx()
            val tickInner = tickOuter - 6.dp.toPx()
            val majorInner = tickOuter - 10.dp.toPx()
            for (i in 0 until tickCount) {
                val angleDeg = -90f + i * (360f / tickCount)
                val rad = Math.toRadians(angleDeg.toDouble())
                val major = i % 5 == 0
                val inner = if (major) majorInner else tickInner
                val lit = (i * (360f / tickCount)) < tickSweep
                val color = when {
                    lit && hasCompletedGoal -> overflowColor.copy(alpha = if (major) 0.95f else 0.6f)
                    lit -> scheme.primary.copy(alpha = if (major) 0.95f else 0.55f)
                    else -> scheme.onSurface.copy(alpha = if (major) 0.22f else 0.10f)
                }
                drawLine(
                    color = color,
                    start = Offset(center.x + inner * cos(rad).toFloat(), center.y + inner * sin(rad).toFloat()),
                    end = Offset(center.x + tickOuter * cos(rad).toFloat(), center.y + tickOuter * sin(rad).toFloat()),
                    strokeWidth = (if (major) 2.dp else 1.2.dp).toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Track with an inner shadow feel.
            drawArc(
                color = scheme.onSurface.copy(alpha = 0.05f),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = arcTopLeft, size = arcSize,
                style = Stroke(width = stroke + 6.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = scheme.surfaceVariant.copy(alpha = 0.55f),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = arcTopLeft, size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            rotate(degrees = -90f, pivot = center) {
                if (hasCompletedGoal) {
                    // Every fully completed goal is represented by the same orange base circle.
                    listOf(30f to 0.07f, 18f to 0.12f, 8f to 0.20f).forEach { (extra, alpha) ->
                        drawArc(
                            color = overflowColor.copy(alpha = alpha),
                            startAngle = 0f, sweepAngle = 360f, useCenter = false,
                            topLeft = arcTopLeft, size = arcSize,
                            style = Stroke(width = stroke + extra.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    drawArc(
                        color = overflowColor,
                        startAngle = 0f, sweepAngle = 360f, useCenter = false,
                        topLeft = arcTopLeft, size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }

                if (remainderSweep > 0f) {
                    val sweepBrush = Brush.sweepGradient(
                        colors = listOf(scheme.primary, scheme.secondary, scheme.tertiary, scheme.primary),
                        center = center
                    )
                    // Draw the current goal's themed remainder over the orange completed-goal circle.
                    listOf(30f to 0.07f, 18f to 0.12f, 8f to 0.20f).forEach { (extra, alpha) ->
                        drawArc(
                            color = scheme.primary.copy(alpha = alpha),
                            startAngle = 0f, sweepAngle = remainderSweep, useCenter = false,
                            topLeft = arcTopLeft, size = arcSize,
                            style = Stroke(width = stroke + extra.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    drawArc(
                        brush = sweepBrush,
                        startAngle = 0f, sweepAngle = remainderSweep, useCenter = false,
                        topLeft = arcTopLeft, size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    // Specular line towards the outer edge of the arc (offset radius, same centre).
                    val specOffset = stroke * 0.22f
                    drawArc(
                        color = Color.White.copy(alpha = 0.22f),
                        startAngle = 0f, sweepAngle = remainderSweep, useCenter = false,
                        topLeft = Offset(arcTopLeft.x - specOffset, arcTopLeft.y - specOffset),
                        size = Size(arcSize.width + specOffset * 2f, arcSize.height + specOffset * 2f),
                        style = Stroke(width = stroke * 0.16f, cap = StrokeCap.Round)
                    )
                    // Travelling highlight: three nested dashes fade towards the ends.
                    if (remainderSweep > 20f) {
                        val len = 30f
                        val pos = (shimmer * (remainderSweep + len)) - len
                        val start = pos.coerceAtLeast(0f)
                        val end = (pos + len).coerceAtMost(remainderSweep)
                        if (end > start) {
                            listOf(1f to 0.10f, 0.6f to 0.16f, 0.3f to 0.26f).forEach { (portion, alpha) ->
                                val segment = (end - start) * portion
                                val segStart = start + ((end - start) - segment) / 2f
                                drawArc(
                                    color = Color.White.copy(alpha = alpha),
                                    startAngle = segStart, sweepAngle = segment, useCenter = false,
                                    topLeft = arcTopLeft, size = arcSize,
                                    style = Stroke(width = stroke * 0.85f, cap = StrokeCap.Round)
                                )
                            }
                        }
                    }
                }
            }

            if (hasCompletedGoal || remainderSweep > 0f) {
                // Comet head follows the themed remainder, or stays orange at an exact goal boundary.
                val headSweep = if (remainderSweep > 0f) remainderSweep else 360f
                val headColor = if (remainderSweep > 0f) scheme.primary else overflowColor
                val headRad = Math.toRadians((headSweep - 90f).toDouble())
                val head = Offset(center.x + ringRadius * cos(headRad).toFloat(), center.y + ringRadius * sin(headRad).toFloat())
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(headColor.copy(alpha = 0.55f), Color.Transparent),
                        center = head, radius = stroke * 1.4f
                    ),
                    radius = stroke * 1.4f, center = head
                )
                drawCircle(color = headColor, radius = stroke * 0.42f, center = head)
                drawCircle(color = Color.White.copy(alpha = 0.92f), radius = stroke * 0.24f, center = head)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (eyebrow != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background((if (reached) overflowColor else scheme.primary).copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Eyebrow(eyebrow, color = if (reached) overflowColor else scheme.primary)
                }
                Spacer(Modifier.height(6.dp))
            }
            Text(
                text = centerText,
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 52.sp, lineHeight = 56.sp),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                color = if (reached) overflowColor else scheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
//  Charts
// ---------------------------------------------------------------------------------------------

/**
 * Compact bar chart drawn on a canvas: rounded gradient bars, faint guide lines, and the
 * peak bar highlighted with a glow. Bars animate in from the baseline.
 */
@Composable
fun TinyBarChart(
    values: List<Int>,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 110.dp,
    highlightIndex: Int? = null
) {
    if (values.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val max = values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val peakIndex = highlightIndex ?: values.indices.maxByOrNull { values[it] } ?: -1
    // Bars grow from the baseline on first composition.
    val revealAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { revealAnim.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val reveal = revealAnim.value
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val n = values.size
        val gap = if (n > 12) 3.dp.toPx() else 6.dp.toPx()
        val barW = (size.width - gap * (n - 1)) / n
        val radius = CornerRadius(barW / 2f, barW / 2f)
        // Guide lines.
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
            val x = i * (barW + gap)
            val fraction = (v.toFloat() / max) * reveal
            val minH = 3.dp.toPx()
            val h = (size.height * fraction).coerceAtLeast(minH)
            val top = size.height - h
            val isPeak = i == peakIndex && v > 0
            if (v <= 0) {
                drawRoundRect(
                    color = scheme.onSurface.copy(alpha = 0.06f),
                    topLeft = Offset(x, size.height - minH),
                    size = Size(barW, minH),
                    cornerRadius = radius
                )
                return@forEachIndexed
            }
            if (isPeak) {
                drawRoundRect(
                    color = scheme.secondary.copy(alpha = 0.28f),
                    topLeft = Offset(x - 3.dp.toPx(), top - 3.dp.toPx()),
                    size = Size(barW + 6.dp.toPx(), h + 6.dp.toPx()),
                    cornerRadius = CornerRadius(barW / 2f + 3.dp.toPx(), barW / 2f + 3.dp.toPx())
                )
            }
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = if (isPeak) listOf(scheme.secondary, scheme.primary, scheme.primary.copy(alpha = 0.55f))
                    else listOf(scheme.primary.copy(alpha = 0.95f), scheme.primary.copy(alpha = 0.35f)),
                    startY = top, endY = size.height
                ),
                topLeft = Offset(x, top),
                size = Size(barW, h),
                cornerRadius = radius
            )
        }
    }
}

@Composable
fun HeatMap(values: List<Int>, modifier: Modifier = Modifier) {
    val data = values.ifEmpty { List(35) { 0 } }
    val startDate = LocalDate.now().minusDays((data.size - 1).toLong())
    val normalized: List<Int?> = List(startDate.dayOfWeek.value - 1) { null } + data
    val max = data.maxOrNull()?.coerceAtLeast(1) ?: 1
    val columns = 7
    val rows = (normalized.size + columns - 1) / columns
    val weekdayLabels = appText("weekday_letters").split(",")
    val scheme = MaterialTheme.colorScheme
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
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
                                        .drawBehind {
                                            if (isToday) {
                                                drawRoundRect(
                                                    color = Color.White.copy(alpha = 0.7f),
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

@Composable
fun MiniTrendLine(
    values: List<Float>,
    modifier: Modifier = Modifier,
    xLabels: List<String> = emptyList(),
    valueFormat: (Float) -> String = { "%.1f".format(it) }
) {
    if (values.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val secondary = scheme.secondary
    val axisColor = scheme.onSurfaceVariant
    val max = values.maxOrNull() ?: return
    val min = values.minOrNull() ?: return
    val range = max - min
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
            ) {
                val pointRadius = 4.dp.toPx()
                val horizontalPadding = pointRadius * 2
                val verticalPadding = pointRadius * 2
                val chartWidth = size.width - horizontalPadding * 2
                val chartHeight = size.height - verticalPadding * 2
                val points = values.mapIndexed { index, value ->
                    val x = horizontalPadding + (index.toFloat() / values.lastIndex.coerceAtLeast(1)) * chartWidth
                    val y = if (range == 0f) size.height / 2f else {
                        val norm = (value - min) / range
                        verticalPadding + chartHeight - (norm * chartHeight)
                    }
                    Offset(x, y)
                }
                // Smooth path through the points.
                val line = Path().apply {
                    points.forEachIndexed { i, p ->
                        if (i == 0) moveTo(p.x, p.y) else {
                            val prev = points[i - 1]
                            val midX = (prev.x + p.x) / 2f
                            cubicTo(midX, prev.y, midX, p.y, p.x, p.y)
                        }
                    }
                }
                val fill = Path().apply {
                    addPath(line)
                    lineTo(points.last().x, size.height)
                    lineTo(points.first().x, size.height)
                    close()
                }
                listOf(0.25f, 0.5f, 0.75f).forEach { f ->
                    val y = size.height * (1f - f)
                    drawLine(scheme.onSurface.copy(alpha = 0.06f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
                drawPath(
                    path = fill,
                    brush = Brush.verticalGradient(listOf(primary.copy(alpha = 0.35f), Color.Transparent))
                )
                drawPath(
                    path = line,
                    brush = Brush.horizontalGradient(listOf(secondary, primary)),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                points.forEachIndexed { i, p ->
                    val last = i == points.lastIndex
                    if (last) {
                        drawCircle(primary.copy(alpha = 0.30f), radius = pointRadius * 2.6f, center = p)
                    }
                    drawCircle(scheme.background, radius = pointRadius + 1.5.dp.toPx(), center = p)
                    drawCircle(if (last) Color.White else primary, radius = pointRadius, center = p)
                }
            }
            Column(
                modifier = Modifier
                    .height(110.dp)
                    .padding(start = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(valueFormat(max), style = MaterialTheme.typography.labelSmall, color = axisColor)
                Text(valueFormat(min), style = MaterialTheme.typography.labelSmall, color = axisColor)
            }
        }
        if (xLabels.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(xLabels.first(), style = MaterialTheme.typography.labelSmall, color = axisColor)
                if (xLabels.size > 1) {
                    Text(xLabels.last(), style = MaterialTheme.typography.labelSmall, color = axisColor)
                }
            }
        }
    }
}
