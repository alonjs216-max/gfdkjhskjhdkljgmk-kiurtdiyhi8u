package com.xaniihub.app.domain.repository

import com.xaniihub.app.domain.model.Achievement
import com.xaniihub.app.domain.model.CustomChallenge
import com.xaniihub.app.domain.model.AnalyticsOverview
import com.xaniihub.app.domain.model.BodyParams
import com.xaniihub.app.domain.model.DashboardStats
import com.xaniihub.app.domain.model.GoalConfig
import com.xaniihub.app.domain.model.GoalProgress
import com.xaniihub.app.domain.model.MiniChallenge
import com.xaniihub.app.domain.model.TrackingSnapshot
import com.xaniihub.app.domain.model.WeightPoint
import kotlinx.coroutines.flow.Flow

interface XaniiRepository {
    fun observeDashboardStats(): Flow<DashboardStats>
    fun observeDashboardStatsForDate(date: java.time.LocalDate): Flow<DashboardStats>
    suspend fun setDailyGoal(goal: Int)
    suspend fun setGoals(config: GoalConfig)
    fun observeGoalConfig(): Flow<GoalConfig>
    fun observeGoalProgress(): Flow<List<GoalProgress>>

    fun observeAnalyticsOverview(): Flow<AnalyticsOverview>
    fun observeDailyHeatMap(): Flow<List<Int>>

    fun observeAchievements(): Flow<List<Achievement>>
    fun observeChallenges(): Flow<List<MiniChallenge>>

    fun observeCustomChallenges(): Flow<List<CustomChallenge>>
    suspend fun createCustomChallenge(challenge: CustomChallenge)
    suspend fun deleteCustomChallenge(id: Long)

    fun observeBodyParams(): Flow<BodyParams>
    suspend fun saveBodyParams(params: BodyParams)
    fun observeWeightTrend(): Flow<List<WeightPoint>>
    suspend fun saveWeight(weightKg: Float)
    suspend fun getTrackingSnapshot(): TrackingSnapshot

    /**
     * Ingests a raw TYPE_STEP_COUNTER reading.
     *
     * @param total steps counted by the sensor since the last reboot.
     * @param eventTimeMillis wall clock time of the reading; cadence and the day the steps
     * belong to are derived from it together with the previously stored reading.
     */
    suspend fun ingestSensorTotal(total: Float, eventTimeMillis: Long)
}
