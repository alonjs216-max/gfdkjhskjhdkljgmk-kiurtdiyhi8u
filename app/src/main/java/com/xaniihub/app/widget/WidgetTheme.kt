package com.xaniihub.app.widget

import android.content.Context
import com.xaniihub.app.data.local.dao.StepDao
import com.xaniihub.app.localization.appLocale
import com.xaniihub.app.tracking.TrackingConstants
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.text.NumberFormat
import java.time.LocalDate

/**
 * Design tokens shared by every widget and by the tracking notification.
 *
 * The home screen surfaces cannot use Compose, so the palette of the app
 * ([com.xaniihub.app.ui.theme.AppThemePalette]) is mirrored here as plain ARGB ints and read
 * straight from the same SharedPreferences the in-app theme controller writes to. Changing the
 * palette in the profile screen therefore re-tints the widgets and the notification as well.
 */
data class WidgetPalette(
    val primary: Int,
    val secondary: Int,
    val tertiary: Int,
    /** Used once the daily goal is passed – same gold as the hero ring in the app. */
    val overflow: Int = 0xFFFFC857.toInt(),
    val textPrimary: Int = 0xFFFFFFFF.toInt(),
    val textSecondary: Int = 0xB3FFFFFF.toInt(),
    val textMuted: Int = 0x80FFFFFF.toInt(),
    val track: Int = 0x1FFFFFFF
) {
    /** Three-stop sweep used by rings, bars and progress lines. */
    val sweep: IntArray get() = intArrayOf(primary, secondary, tertiary, primary)
}

object WidgetTheme {

    private const val THEME_PREFS = "ringwalk_theme"
    private const val KEY_PALETTE = "palette"

    private val mono = WidgetPalette(
        primary = 0xFFFFFFFF.toInt(),
        secondary = 0xFFBFC4CC.toInt(),
        tertiary = 0xFF8A9BB0.toInt()
    )
    private val cyan = WidgetPalette(
        primary = 0xFF00D9FF.toInt(),
        secondary = 0xFF2EFFC2.toInt(),
        tertiary = 0xFF7C6BFF.toInt()
    )
    private val violet = WidgetPalette(
        primary = 0xFFA78BFA.toInt(),
        secondary = 0xFFF472B6.toInt(),
        tertiary = 0xFF60A5FA.toInt()
    )
    private val emerald = WidgetPalette(
        primary = 0xFF5EF05D.toInt(),
        secondary = 0xFF2DD4BF.toInt(),
        tertiary = 0xFFFDE047.toInt()
    )
    private val sunset = WidgetPalette(
        primary = 0xFFFF8A50.toInt(),
        secondary = 0xFFFF4D8D.toInt(),
        tertiary = 0xFFFFD166.toInt()
    )

    /** Palette currently selected in the app (defaults to Violet, like the Compose theme). */
    fun palette(context: Context): WidgetPalette {
        val name = runCatching {
            context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
                .getString(KEY_PALETTE, "VIOLET")
        }.getOrNull() ?: "VIOLET"
        return when (name) {
            "MONO" -> mono
            "CYAN" -> cyan
            "EMERALD" -> emerald
            "SUNSET" -> sunset
            else -> violet
        }
    }

    fun withAlpha(color: Int, alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255f).toInt()
        return (a shl 24) or (color and 0x00FFFFFF)
    }

    fun formatInt(value: Int): String = NumberFormat.getIntegerInstance(appLocale()).format(value)

    fun formatInt(value: Long): String = NumberFormat.getIntegerInstance(appLocale()).format(value)

    fun formatDecimal(value: Double, digits: Int = 1): String =
        NumberFormat.getNumberInstance(appLocale()).apply {
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }.format(value)

    /** Compact form for tight tiles: 12 480 -> "12,5k". */
    fun formatCompact(value: Int): String = when {
        value < 10_000 -> formatInt(value)
        value < 1_000_000 -> formatDecimal(value / 1000.0) + "k"
        else -> formatDecimal(value / 1_000_000.0) + "M"
    }
}

/**
 * Everything the home-screen surfaces need to render, in one immutable bag.
 *
 * @param week steps per day for the last seven days, index 6 being today.
 */
data class WidgetSnapshot(
    val steps: Int = 0,
    val goal: Int = 8_000,
    val calories: Int = 0,
    val distanceMeters: Int = 0,
    val activeMinutes: Int = 0,
    val week: List<Int> = List(7) { 0 },
    val streakDays: Int = 0
) {
    val progress: Float get() = if (goal <= 0) 0f else steps.toFloat() / goal.toFloat()
    val percent: Int get() = (progress * 100f).toInt().coerceIn(0, 9_999)
    val remaining: Int get() = (goal - steps).coerceAtLeast(0)
    val goalReached: Boolean get() = steps >= goal && goal > 0
    val weekTotal: Int get() = week.sum()
    val weekBest: Int get() = week.maxOrNull() ?: 0
}

/**
 * Widget data source.
 *
 * Two speeds on purpose:
 *  - [cached] is synchronous (SharedPreferences only) so a widget can paint instantly, even
 *    right after boot, without touching Room on the main thread;
 *  - [load] additionally reads the Room daily summaries to fill the real seven-day history
 *    and the goal streak.
 */
object WidgetRepository {

    private const val PREFS = "ringwalk_widget"
    private const val KEY_DATE = "date"
    private const val KEY_STEPS = "steps"
    private const val KEY_GOAL = "goal"
    private const val KEY_CALORIES = "calories"
    private const val KEY_DISTANCE = "distance_m"
    private const val KEY_MINUTES = "minutes"
    private const val KEY_STREAK = "streak"
    private const val KEY_DAY_PREFIX = "day_"

    /** Called by the tracking service on every sensor update. */
    fun save(
        context: Context,
        steps: Int,
        goal: Int,
        calories: Int,
        distanceMeters: Int,
        activeMinutes: Int
    ) {
        val today = LocalDate.now().toEpochDay()
        runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putLong(KEY_DATE, today)
                .putInt(KEY_STEPS, steps)
                .putInt(KEY_GOAL, goal)
                .putInt(KEY_CALORIES, calories)
                .putInt(KEY_DISTANCE, distanceMeters)
                .putInt(KEY_MINUTES, activeMinutes)
                .putInt(KEY_DAY_PREFIX + today, steps)
                .apply()
        }
    }

    fun cached(context: Context): WidgetSnapshot {
        val prefs = runCatching { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }.getOrNull()
        val runtime = runCatching {
            context.getSharedPreferences(TrackingConstants.PREFS_NAME, Context.MODE_PRIVATE)
        }.getOrNull()
        val today = LocalDate.now().toEpochDay()
        val fresh = prefs?.getLong(KEY_DATE, Long.MIN_VALUE) == today

        val goal = runtime?.getInt(TrackingConstants.PREF_DAILY_GOAL, 8_000)
            ?: prefs?.getInt(KEY_GOAL, 8_000) ?: 8_000
        val runtimeSteps = if (runtime?.getLong(TrackingConstants.PREF_LATEST_STEPS_DATE, Long.MIN_VALUE) == today) {
            runtime.getInt(TrackingConstants.PREF_LATEST_STEPS, 0)
        } else {
            0
        }
        val steps = maxOf(runtimeSteps, if (fresh) prefs?.getInt(KEY_STEPS, 0) ?: 0 else 0)
        val week = (0..6).map { offset ->
            val day = today - (6 - offset)
            if (day == today) steps else prefs?.getInt(KEY_DAY_PREFIX + day, 0) ?: 0
        }
        return WidgetSnapshot(
            steps = steps,
            goal = goal.coerceAtLeast(1),
            calories = if (fresh) prefs?.getInt(KEY_CALORIES, 0) ?: 0 else 0,
            distanceMeters = if (fresh) prefs?.getInt(KEY_DISTANCE, 0) ?: 0 else 0,
            activeMinutes = if (fresh) prefs?.getInt(KEY_MINUTES, 0) ?: 0 else 0,
            week = week,
            streakDays = prefs?.getInt(KEY_STREAK, 0) ?: 0
        )
    }

    /** Cached snapshot enriched with the real history stored in Room. */
    suspend fun load(context: Context): WidgetSnapshot {
        val base = cached(context)
        val dao = runCatching {
            EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).stepDao()
        }.getOrNull() ?: return base
        val summaries = runCatching { dao.getAllSummaries() }.getOrNull() ?: return base
        if (summaries.isEmpty()) return base

        val byDay = summaries.associateBy { it.dateEpochDay }
        val today = LocalDate.now().toEpochDay()
        val week = (0..6).map { offset ->
            val day = today - (6 - offset)
            if (day == today) maxOf(base.steps, byDay[day]?.steps ?: 0) else byDay[day]?.steps ?: 0
        }

        // Goal streak: consecutive days (walking backwards) where the daily goal was reached.
        var streak = 0
        var cursor = today
        while (true) {
            val steps = if (cursor == today) maxOf(base.steps, byDay[cursor]?.steps ?: 0) else byDay[cursor]?.steps ?: 0
            if (steps >= base.goal && steps > 0) {
                streak++
                cursor--
            } else {
                break
            }
        }

        val todaySummary = byDay[today]
        val snapshot = base.copy(
            calories = maxOf(base.calories, todaySummary?.calories?.toInt() ?: 0),
            distanceMeters = maxOf(base.distanceMeters, ((todaySummary?.distanceKm ?: 0f) * 1000f).toInt()),
            activeMinutes = maxOf(base.activeMinutes, todaySummary?.activeMinutes ?: 0),
            week = week,
            streakDays = streak
        )
        runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt(KEY_STREAK, streak)
                .apply()
        }
        return snapshot
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun stepDao(): StepDao
}
