package com.xaniihub.app.data.repository

import com.xaniihub.app.domain.model.ActivityKind

/** Estimates active calories from steps; it intentionally excludes BMR/TDEE. */
object StepCalorieCalculator {
    private const val WALKING_STRIDE_FACTOR = 0.415f
    private const val RUNNING_STRIDE_FACTOR = 0.65f
    private const val WALKING_SPEED_KMH = 5.0f
    private const val RUNNING_SPEED_KMH = 9.0f
    private const val WALKING_MET = 3.3f
    private const val RUNNING_MET = 8.5f
    private const val RUNNING_CADENCE = 140f
    private const val MAX_USABLE_CADENCE = 220f

    fun estimate(steps: Int, weightKg: Float, heightCm: Int, cadence: Float): Float {
        if (steps <= 0 || !weightKg.isFinite() || weightKg !in 25f..350f || heightCm !in 100..250) {
            return 0f
        }

        val activity = if (cadence.isFinite() && cadence in RUNNING_CADENCE..MAX_USABLE_CADENCE) {
            ActivityKind.RUNNING
        } else {
            ActivityKind.WALKING
        }
        val strideCm = heightCm * if (activity == ActivityKind.RUNNING) {
            RUNNING_STRIDE_FACTOR
        } else {
            WALKING_STRIDE_FACTOR
        }
        val distanceKm = steps * strideCm / 100_000f
        val speedKmh = if (activity == ActivityKind.RUNNING) RUNNING_SPEED_KMH else WALKING_SPEED_KMH
        val durationHours = distanceKm / speedKmh
        val met = if (activity == ActivityKind.RUNNING) RUNNING_MET else WALKING_MET
        return (met * weightKg * durationHours).coerceAtLeast(0f)
    }

    fun activityKind(cadence: Float): ActivityKind = if (
        cadence.isFinite() && cadence in RUNNING_CADENCE..MAX_USABLE_CADENCE
    ) {
        ActivityKind.RUNNING
    } else if (cadence.isFinite() && cadence >= 70f) {
        ActivityKind.WALKING
    } else {
        ActivityKind.IDLE
    }

    fun strideKm(heightCm: Int, cadence: Float): Float {
        if (heightCm !in 100..250) return 0f
        val factor = if (activityKind(cadence) == ActivityKind.RUNNING) {
            RUNNING_STRIDE_FACTOR
        } else {
            WALKING_STRIDE_FACTOR
        }
        return heightCm * factor / 100_000f
    }
}
