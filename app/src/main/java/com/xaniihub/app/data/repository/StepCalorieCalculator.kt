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
    private const val MIN_WEIGHT_KG = 25f
    private const val MAX_WEIGHT_KG = 350f
    private const val FALLBACK_WEIGHT_KG = 70f
    private const val MIN_HEIGHT_CM = 100
    private const val MAX_HEIGHT_CM = 250
    private const val FALLBACK_HEIGHT_CM = 175

    fun estimate(steps: Int, weightKg: Float, heightCm: Int, cadence: Float): Float {
        if (steps <= 0) return 0f

        // Body params outside of the plausible range used to make this return 0 kcal, so a single
        // corrupted or not yet migrated profile value silently erased the calories of every day
        // instead of degrading gracefully. Clamp the inputs and keep estimating.
        val weight = sanitizedWeightKg(weightKg)
        val height = sanitizedHeightCm(heightCm)

        val activity = if (cadence.isFinite() && cadence in RUNNING_CADENCE..MAX_USABLE_CADENCE) {
            ActivityKind.RUNNING
        } else {
            ActivityKind.WALKING
        }
        val strideCm = height * if (activity == ActivityKind.RUNNING) {
            RUNNING_STRIDE_FACTOR
        } else {
            WALKING_STRIDE_FACTOR
        }
        val distanceKm = steps * strideCm / 100_000f
        val speedKmh = if (activity == ActivityKind.RUNNING) RUNNING_SPEED_KMH else WALKING_SPEED_KMH
        val durationHours = distanceKm / speedKmh
        val met = if (activity == ActivityKind.RUNNING) RUNNING_MET else WALKING_MET
        return (met * weight * durationHours).coerceAtLeast(0f)
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
        // Same reasoning as in estimate(): an implausible height must not silently drop the
        // distance of the day to zero.
        val height = sanitizedHeightCm(heightCm)
        val factor = if (activityKind(cadence) == ActivityKind.RUNNING) {
            RUNNING_STRIDE_FACTOR
        } else {
            WALKING_STRIDE_FACTOR
        }
        return height * factor / 100_000f
    }

    /** Clamps the weight, falling back to an average one when the value is unusable. */
    private fun sanitizedWeightKg(weightKg: Float): Float =
        if (!weightKg.isFinite() || weightKg <= 0f) {
            FALLBACK_WEIGHT_KG
        } else {
            weightKg.coerceIn(MIN_WEIGHT_KG, MAX_WEIGHT_KG)
        }

    /** Clamps the height, falling back to an average one when the value is unusable. */
    private fun sanitizedHeightCm(heightCm: Int): Int =
        if (heightCm <= 0) {
            FALLBACK_HEIGHT_CM
        } else {
            heightCm.coerceIn(MIN_HEIGHT_CM, MAX_HEIGHT_CM)
        }
}
