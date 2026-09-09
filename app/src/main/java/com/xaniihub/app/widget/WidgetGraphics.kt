package com.xaniihub.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Canvas-drawn artwork for the widgets and the tracking notification.
 *
 * RemoteViews cannot host Compose, so the signature pieces of the main menu – the hero goal
 * ring, the gradient progress line and the weekly bar chart – are re-created here as bitmaps
 * with the same visual grammar: tick marks, multi-stop sweep gradients, soft glows, a comet
 * head at the leading edge and a gold lap once the goal is passed.
 *
 * Results are memoised, because a widget re-renders on every sensor batch.
 */
object WidgetGraphics {

    private const val CACHE_LIMIT = 16
    private val cache = LinkedHashMap<String, Bitmap>()

    private fun cached(key: String, create: () -> Bitmap): Bitmap {
        synchronized(cache) {
            cache[key]?.let { if (!it.isRecycled) return it }
        }
        val bitmap = create()
        synchronized(cache) {
            if (cache.size >= CACHE_LIMIT) {
                cache.keys.firstOrNull()?.let { cache.remove(it) }
            }
            cache[key] = bitmap
        }
        return bitmap
    }

    fun clearCache() {
        synchronized(cache) { cache.clear() }
    }

    private fun px(context: Context, dp: Float): Float = dp * context.resources.displayMetrics.density

    // -----------------------------------------------------------------------------------------
    //  Goal ring
    // -----------------------------------------------------------------------------------------

    /**
     * Hero progress ring.
     *
     * @param sizeDp outer size of the bitmap
     * @param strokeDp thickness of the arc
     * @param ticks draw the 40 tick marks around the ring (skipped on very small rings)
     */
    fun ring(
        context: Context,
        palette: WidgetPalette,
        progress: Float,
        sizeDp: Float,
        strokeDp: Float,
        ticks: Boolean = true
    ): Bitmap {
        val safe = if (progress.isFinite()) progress.coerceIn(0f, 9.99f) else 0f
        val rounded = (safe * 100f).toInt() / 1f // 1% steps keep the cache useful
        val key = "ring:${palette.primary}:$rounded:$sizeDp:$strokeDp:$ticks"
        return cached(key) { drawRing(context, palette, safe, sizeDp, strokeDp, ticks) }
    }

    private fun drawRing(
        context: Context,
        palette: WidgetPalette,
        progress: Float,
        sizeDp: Float,
        strokeDp: Float,
        ticks: Boolean
    ): Bitmap {
        val size = px(context, sizeDp).toInt().coerceIn(1, 1_024)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val stroke = px(context, strokeDp)
        val cx = size / 2f
        val cy = size / 2f

        val tickSpace = if (ticks) px(context, 7f) else 0f
        val radius = size / 2f - stroke / 2f - tickSpace - px(context, 1f)
        if (radius <= 0f) return bitmap
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        val laps = progress.toInt()
        val hasLap = laps > 0
        val remainder = if (hasLap) progress - laps else progress
        val sweep = (remainder.coerceIn(0f, 1f) * 360f)
        val litSweep = if (hasLap) 360f else sweep
        val accent = if (hasLap) palette.overflow else palette.primary

        // 1. Ambient glow underneath everything.
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, radius + stroke,
                intArrayOf(
                    WidgetTheme.withAlpha(accent, 0f),
                    WidgetTheme.withAlpha(accent, if (hasLap) 0.30f else 0.18f),
                    WidgetTheme.withAlpha(accent, 0f)
                ),
                floatArrayOf(0f, 0.78f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, radius + stroke, glowPaint)

        // 2. Tick marks – they light up as progress sweeps past them.
        if (ticks) {
            val tickCount = 40
            val outer = radius + stroke / 2f + px(context, 5.5f)
            val innerMinor = outer - px(context, 3f)
            val innerMajor = outer - px(context, 5.5f)
            val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                strokeCap = Paint.Cap.ROUND
            }
            for (i in 0 until tickCount) {
                val angle = i * (360f / tickCount)
                val rad = Math.toRadians((angle - 90f).toDouble())
                val major = i % 5 == 0
                val lit = angle < litSweep
                tickPaint.strokeWidth = px(context, if (major) 1.6f else 1f)
                tickPaint.color = when {
                    lit -> WidgetTheme.withAlpha(accent, if (major) 0.95f else 0.55f)
                    else -> WidgetTheme.withAlpha(0xFFFFFFFF.toInt(), if (major) 0.20f else 0.10f)
                }
                val inner = if (major) innerMajor else innerMinor
                canvas.drawLine(
                    cx + inner * cos(rad).toFloat(),
                    cy + inner * sin(rad).toFloat(),
                    cx + outer * cos(rad).toFloat(),
                    cy + outer * sin(rad).toFloat(),
                    tickPaint
                )
            }
        }

        // 3. Track.
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            color = palette.track
        }
        canvas.drawArc(rect, 0f, 360f, false, trackPaint)

        // 4. Completed lap sits underneath the current one, in gold.
        if (hasLap) {
            val lapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                color = palette.overflow
            }
            canvas.drawArc(rect, 0f, 360f, false, lapPaint)
        }

        if (sweep <= 0.01f) return bitmap

        // 5. Progress arc with a seamless three-colour sweep gradient.
        val gradient = SweepGradient(cx, cy, palette.sweep, floatArrayOf(0f, 0.34f, 0.67f, 1f)).apply {
            setLocalMatrix(Matrix().apply { setRotate(-90f, cx, cy) })
        }
        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            shader = if (hasLap) null else gradient
            if (hasLap) color = palette.primary
        }
        // Soft halo hugging the arc.
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke * 2.1f
            strokeCap = Paint.Cap.ROUND
            shader = arcPaint.shader
            color = arcPaint.color
            alpha = 46
        }
        canvas.drawArc(rect, -90f, sweep, false, haloPaint)
        canvas.drawArc(rect, -90f, sweep, false, arcPaint)

        // 6. Comet head at the leading edge.
        val headRad = Math.toRadians((sweep - 90f).toDouble())
        val hx = cx + radius * cos(headRad).toFloat()
        val hy = cy + radius * sin(headRad).toFloat()
        val headColor = if (hasLap) palette.primary else palette.tertiary
        canvas.drawCircle(
            hx, hy, stroke * 1.5f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    hx, hy, stroke * 1.5f,
                    WidgetTheme.withAlpha(headColor, 0.55f),
                    WidgetTheme.withAlpha(headColor, 0f),
                    Shader.TileMode.CLAMP
                )
            }
        )
        canvas.drawCircle(
            hx, hy, stroke * 0.36f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
        )
        return bitmap
    }

    // -----------------------------------------------------------------------------------------
    //  Progress line
    // -----------------------------------------------------------------------------------------

    /** Thin capsule progress bar with a gradient fill and a bright leading dot. */
    fun progressBar(
        context: Context,
        palette: WidgetPalette,
        progress: Float,
        widthDp: Float,
        heightDp: Float = 8f
    ): Bitmap {
        val safe = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f
        val key = "bar:${palette.primary}:${(safe * 200f).toInt()}:$widthDp:$heightDp"
        return cached(key) {
            val w = px(context, widthDp).toInt().coerceIn(1, 2_048)
            val h = px(context, heightDp).toInt().coerceIn(1, 128)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val radius = h / 2f

            canvas.drawRoundRect(
                RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.track }
            )
            val filled = w * safe
            if (filled > 0.5f) {
                val fillWidth = filled.coerceAtLeast(h.toFloat())
                canvas.drawRoundRect(
                    RectF(0f, 0f, fillWidth, h.toFloat()), radius, radius,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(
                            0f, 0f, fillWidth, 0f,
                            intArrayOf(palette.primary, palette.secondary, palette.tertiary),
                            floatArrayOf(0f, 0.55f, 1f),
                            Shader.TileMode.CLAMP
                        )
                    }
                )
                // Inner sheen for the glass feel.
                canvas.drawRoundRect(
                    RectF(0f, 0f, fillWidth, h * 0.55f), radius, radius,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = WidgetTheme.withAlpha(0xFFFFFFFF.toInt(), 0.20f) }
                )
            }
            bitmap
        }
    }

    // -----------------------------------------------------------------------------------------
    //  Weekly bars
    // -----------------------------------------------------------------------------------------

    /**
     * Seven-day bar chart with a dashed goal line and weekday captions.
     *
     * Days that hit the goal are drawn in gold, today is drawn with the full palette gradient
     * and everything else stays muted, exactly like the analytics chart in the app.
     */
    fun weekChart(
        context: Context,
        palette: WidgetPalette,
        values: List<Int>,
        goal: Int,
        labels: List<String>,
        widthDp: Float,
        heightDp: Float,
        showLabels: Boolean = true
    ): Bitmap {
        val key = "week:${palette.primary}:${values.joinToString(",")}:$goal:$widthDp:$heightDp:$showLabels:${labels.firstOrNull()}"
        return cached(key) {
            val w = px(context, widthDp).toInt().coerceIn(1, 2_048)
            val h = px(context, heightDp).toInt().coerceIn(1, 512)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val labelSize = px(context, 9f)
            val labelSpace = if (showLabels) labelSize + px(context, 5f) else 0f
            val chartHeight = h - labelSpace
            val count = values.size.coerceAtLeast(1)
            val slot = w / count.toFloat()
            val barWidth = min(slot * 0.54f, px(context, 14f))
            val corner = barWidth / 2f
            val maxValue = maxOf(values.maxOrNull() ?: 0, goal, 1)

            // Dashed goal line.
            if (goal > 0) {
                val y = chartHeight - chartHeight * (goal.toFloat() / maxValue) * 0.92f
                val dash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = px(context, 1f)
                    color = WidgetTheme.withAlpha(0xFFFFFFFF.toInt(), 0.22f)
                    pathEffect = android.graphics.DashPathEffect(
                        floatArrayOf(px(context, 3f), px(context, 4f)), 0f
                    )
                }
                canvas.drawPath(Path().apply { moveTo(0f, y); lineTo(w.toFloat(), y) }, dash)
            }

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = labelSize
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }

            values.forEachIndexed { index, value ->
                val centerX = slot * index + slot / 2f
                val isToday = index == values.lastIndex
                val reached = goal > 0 && value >= goal
                val ratio = (value.toFloat() / maxValue).coerceIn(0f, 1f) * 0.92f
                val barHeight = (chartHeight * ratio).coerceAtLeast(if (value > 0) px(context, 4f) else px(context, 3f))
                val top = chartHeight - barHeight
                val rect = RectF(centerX - barWidth / 2f, top, centerX + barWidth / 2f, chartHeight)

                // Empty track behind every bar keeps the rhythm visible on quiet days.
                canvas.drawRoundRect(
                    RectF(centerX - barWidth / 2f, 0f, centerX + barWidth / 2f, chartHeight),
                    corner, corner,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = WidgetTheme.withAlpha(0xFFFFFFFF.toInt(), 0.06f)
                    }
                )

                if (value > 0) {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                    paint.shader = when {
                        reached -> LinearGradient(
                            0f, top, 0f, chartHeight,
                            intArrayOf(palette.overflow, WidgetTheme.withAlpha(palette.overflow, 0.55f)),
                            null, Shader.TileMode.CLAMP
                        )
                        isToday -> LinearGradient(
                            0f, top, 0f, chartHeight,
                            intArrayOf(palette.secondary, palette.primary, WidgetTheme.withAlpha(palette.tertiary, 0.75f)),
                            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
                        )
                        else -> LinearGradient(
                            0f, top, 0f, chartHeight,
                            intArrayOf(
                                WidgetTheme.withAlpha(palette.primary, 0.75f),
                                WidgetTheme.withAlpha(palette.primary, 0.28f)
                            ),
                            null, Shader.TileMode.CLAMP
                        )
                    }
                    if (isToday || reached) {
                        // Glow under the highlighted bar.
                        canvas.drawRoundRect(
                            RectF(rect.left - px(context, 2f), top - px(context, 2f), rect.right + px(context, 2f), chartHeight),
                            corner, corner,
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = WidgetTheme.withAlpha(if (reached) palette.overflow else palette.primary, 0.22f)
                            }
                        )
                    }
                    canvas.drawRoundRect(rect, corner, corner, paint)
                    // Bright cap.
                    canvas.drawRoundRect(
                        RectF(rect.left, top, rect.right, top + barWidth.coerceAtMost(barHeight)),
                        corner, corner,
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = WidgetTheme.withAlpha(0xFFFFFFFF.toInt(), 0.28f)
                        }
                    )
                }

                if (showLabels) {
                    labelPaint.color = when {
                        isToday -> palette.primary
                        reached -> palette.overflow
                        else -> palette.textMuted
                    }
                    val label = labels.getOrNull(index).orEmpty()
                    canvas.drawText(label, centerX, h - px(context, 1f), labelPaint)
                }
            }
            bitmap
        }
    }

    /** Small filled dot used as an accent bullet in headers. */
    fun dot(context: Context, color: Int, sizeDp: Float = 8f): Bitmap {
        val key = "dot:$color:$sizeDp"
        return cached(key) {
            val size = px(context, sizeDp).toInt().coerceIn(1, 64)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val r = size / 2f
            canvas.drawCircle(
                r, r, r,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        r, r, r,
                        intArrayOf(color, WidgetTheme.withAlpha(color, 0.15f)),
                        null, Shader.TileMode.CLAMP
                    )
                }
            )
            bitmap
        }
    }
}
