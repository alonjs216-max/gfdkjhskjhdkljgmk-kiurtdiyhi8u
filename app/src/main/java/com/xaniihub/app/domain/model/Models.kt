package com.xaniihub.app.domain.model

import java.time.LocalDate

enum class ActivityKind {
    WALKING,
    RUNNING,
    IDLE
}


enum class GenderType {
    MALE,
    FEMALE,
    OTHER
}

data class DashboardStats(
    val date: LocalDate,
    val steps: Int,
    val dailyGoal: Int,
    val distanceKm: Float,
    val calories: Float,
    val activeMinutes: Int,
    val streakDays: Int,
    val lifetimeSteps: Long,
    val hourlySteps: List<Int> = emptyList()
) {
    val progress: Float = if (dailyGoal <= 0) 0f else (steps.toFloat() / dailyGoal).coerceAtLeast(0f)
}

data class GoalConfig(
    val daily: Int = 8_000,
    val weekly: Int = 56_000,
    val monthly: Int = 240_000
)

data class GoalProgress(
    val periodName: String,
    val target: Int,
    val current: Int,
    val forecast: Int
) {
    val progress: Float = if (target <= 0) 0f else (current.toFloat() / target).coerceIn(0f, 1.5f)
}

data class DailyPoint(
    val date: LocalDate,
    val steps: Int
)

data class AnalyticsOverview(
    val yearlySteps: Long,
    val averageSteps: Int,
    val mostActiveDay: DailyPoint?,
    val monthComparisonPercent: Float,
    val forecastYearlySteps: Long,
    val heatmap: List<DailyPoint>
)

data class Achievement(
    val key: String,
    val title: String,
    val description: String,
    val threshold: Long,
    val unlocked: Boolean,
    val unlockedAt: Long? = null
)

data class MiniChallenge(
    val id: Int,
    val title: String,
    val description: String,
    val targetSteps: Int,
    val progressSteps: Int
) {
    val progress: Float = if (targetSteps <= 0) 0f else (progressSteps.toFloat() / targetSteps).coerceIn(0f, 1f)
}

data class BodyParams(
    val weightKg: Float = 70f,
    val heightCm: Int = 175,
    val age: Int = 27,
    val gender: GenderType = GenderType.OTHER,
    val activityMultiplier: Float = 1.2f,
    val targetWeightKg: Float = 70f
)

data class WeightPoint(
    val timestamp: Long,
    val weightKg: Float
)

data class TrackingSnapshot(
    val steps: Int,
    val calories: Int,
    val distanceMeters: Int,
    val activeMinutes: Int
)

enum class ChallengeMetric {
    STEPS,
    CALORIES,
    DISTANCE
}

data class CustomChallenge(
    val id: Long = 0,
    val title: String,
    val metric: ChallengeMetric,
    val target: Double,
    val durationDays: Int,
    val startEpochDay: Long,
    val progress: Double = 0.0
) {
    val endEpochDay: Long get() = startEpochDay + durationDays - 1
    val percent: Float
        get() = if (target <= 0.0) 0f else (progress / target).toFloat().coerceIn(0f, 1f)
    val completed: Boolean get() = progress >= target && target > 0.0
}
