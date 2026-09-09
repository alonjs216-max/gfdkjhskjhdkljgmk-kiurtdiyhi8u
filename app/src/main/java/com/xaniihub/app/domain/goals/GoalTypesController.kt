package com.xaniihub.app.domain.goals

import android.content.Context

enum class GoalKind { STEPS, CALORIES, DISTANCE, ACTIVE_MINUTES }

data class GoalTypeSetting(
    val kind: GoalKind,
    val enabled: Boolean,
    val target: Int
)

/**
 * Lightweight SharedPreferences-backed store for multi-type daily goals.
 * Steps / Calories / Distance(km) / Active minutes.
 */
object GoalTypesController {
    private const val PREFS = "xaniihub_goal_types"

    private val defaults = mapOf(
        GoalKind.STEPS to 8_000,
        GoalKind.CALORIES to 500,
        GoalKind.DISTANCE to 5,
        GoalKind.ACTIVE_MINUTES to 30
    )

    private fun keyTarget(kind: GoalKind) = "target_" + kind.name
    private fun keyEnabled(kind: GoalKind) = "enabled_" + kind.name

    fun get(context: Context, kind: GoalKind): GoalTypeSetting {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return GoalTypeSetting(
            kind = kind,
            enabled = prefs.getBoolean(keyEnabled(kind), kind == GoalKind.STEPS),
            target = prefs.getInt(keyTarget(kind), defaults[kind] ?: 0)
        )
    }

    fun all(context: Context): List<GoalTypeSetting> =
        GoalKind.values().map { get(context, it) }

    fun set(context: Context, kind: GoalKind, enabled: Boolean, target: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(keyEnabled(kind), enabled)
            .putInt(keyTarget(kind), target.coerceAtLeast(0))
            .apply()
    }

    fun setStepGoal(context: Context, target: Int) {
        set(context, GoalKind.STEPS, enabled = true, target = target)
    }
}