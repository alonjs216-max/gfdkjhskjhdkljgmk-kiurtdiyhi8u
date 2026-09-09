package com.xaniihub.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "step_events")
data class StepEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val dateEpochDay: Long,
    val stepsDelta: Int,
    val totalCounter: Float,
    val cadence: Float,
    val activityKind: String
)

@Entity(tableName = "daily_summary")
data class DailySummaryEntity(
    @PrimaryKey val dateEpochDay: Long,
    val steps: Int,
    val distanceKm: Float,
    val calories: Float,
    val activeMinutes: Int
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: Int = 0,
    val daily: Int,
    val weekly: Int,
    val monthly: Int
)

@Entity(tableName = "body_params")
data class BodyParamsEntity(
    @PrimaryKey val id: Int = 0,
    val weightKg: Float,
    val heightCm: Int,
    val age: Int,
    val gender: String,
    val activityMultiplier: Float,
    val targetWeightKg: Float
)

@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
    @PrimaryKey val timestamp: Long,
    val weightKg: Float
)

@Entity(tableName = "achievement_state")
data class AchievementStateEntity(
    @PrimaryKey val key: String,
    val unlockedAt: Long?
)

@Entity(tableName = "challenge_progress")
data class ChallengeProgressEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val description: String,
    val targetSteps: Int,
    val progressSteps: Int
)

@Entity(tableName = "custom_challenges")
data class CustomChallengeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val metric: String,
    val target: Double,
    val durationDays: Int,
    val startEpochDay: Long
)
