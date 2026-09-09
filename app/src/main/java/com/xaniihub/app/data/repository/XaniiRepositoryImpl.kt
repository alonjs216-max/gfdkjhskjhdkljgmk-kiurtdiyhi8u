package com.xaniihub.app.data.repository

import android.content.Context
import com.xaniihub.app.data.local.dao.BodyDao
import com.xaniihub.app.data.local.dao.GamificationDao
import com.xaniihub.app.data.local.dao.GoalDao
import com.xaniihub.app.data.local.dao.StepDao
import com.xaniihub.app.data.local.entity.BodyParamsEntity
import com.xaniihub.app.data.local.entity.CustomChallengeEntity
import com.xaniihub.app.data.local.entity.DailySummaryEntity
import com.xaniihub.app.data.local.entity.GoalEntity
import com.xaniihub.app.data.local.entity.StepEventEntity
import com.xaniihub.app.data.local.entity.WeightEntryEntity
import com.xaniihub.app.domain.model.Achievement
import com.xaniihub.app.domain.model.ChallengeMetric
import com.xaniihub.app.domain.model.CustomChallenge
import com.xaniihub.app.domain.model.ActivityKind
import com.xaniihub.app.domain.model.AnalyticsOverview
import com.xaniihub.app.domain.model.BodyParams
import com.xaniihub.app.domain.model.DailyPoint
import com.xaniihub.app.domain.model.DashboardStats
import com.xaniihub.app.domain.model.GenderType
import com.xaniihub.app.domain.model.GoalConfig
import com.xaniihub.app.domain.model.GoalProgress
import com.xaniihub.app.domain.model.MiniChallenge
import com.xaniihub.app.domain.model.TrackingSnapshot
import com.xaniihub.app.domain.model.WeightPoint
import com.xaniihub.app.domain.repository.XaniiRepository
import com.xaniihub.app.tracking.TrackingConstants
import com.xaniihub.app.widget.RingWalkWidgets
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

@Singleton
class XaniiRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stepDao: StepDao,
    private val goalDao: GoalDao,
    private val bodyDao: BodyDao,
    private val gamificationDao: GamificationDao
) : XaniiRepository {

    private val ingestMutex = Mutex()

    private val achievementThresholds = listOf(
        10_000L to "achievement_10k",
        100_000L to "achievement_100k",
        1_000_000L to "achievement_1m"
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeDashboardStats(): Flow<DashboardStats> =
        observeCurrentDate().flatMapLatest(::observeDashboardStatsForDate)

    private fun observeCurrentDate(): Flow<LocalDate> = flow {
        while (true) {
            val today = LocalDate.now()
            emit(today)
            val nextMidnight = today.plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            delay((nextMidnight - System.currentTimeMillis()).coerceAtLeast(1_000L))
        }
    }.distinctUntilChanged()

    override fun observeDashboardStatsForDate(date: LocalDate): Flow<DashboardStats> {
        val dateEpoch = date.toEpochDay()
        return combine(
            stepDao.observeDay(dateEpoch),
            goalDao.observeGoal(),
            stepDao.observeLifetimeSteps(),
            stepDao.observeAllSummaries(),
            stepDao.observeEventsForDay(dateEpoch)
        ) { day, goal, lifetime, recent, events ->
            val goalValue = goal?.daily ?: 8_000
            val summary = day ?: DailySummaryEntity(
                dateEpochDay = dateEpoch,
                steps = 0,
                distanceKm = 0f,
                calories = 0f,
                activeMinutes = 0
            )
            val streak = calculateStreak(recent)
            DashboardStats(
                date = date,
                steps = summary.steps,
                dailyGoal = goalValue,
                distanceKm = summary.distanceKm,
                calories = summary.calories,
                activeMinutes = summary.activeMinutes,
                streakDays = streak,
                lifetimeSteps = lifetime,
                hourlySteps = events.groupBy { event ->
                    java.time.Instant.ofEpochMilli(event.timestamp)
                        .atZone(java.time.ZoneId.systemDefault())
                        .hour
                }.let { grouped -> List(24) { hour -> grouped[hour]?.sumOf { it.stepsDelta } ?: 0 } }
            )
        }
    }

    override suspend fun setDailyGoal(goal: Int) {
        val normalized = goal.coerceIn(1_000, 100_000)
        val existing = goalDao.observeGoal().first() ?: GoalEntity(
            daily = 8_000,
            weekly = 56_000,
            monthly = 240_000
        )
        goalDao.upsert(existing.copy(daily = normalized))
        context.getSharedPreferences(TrackingConstants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(TrackingConstants.PREF_DAILY_GOAL, normalized)
            .apply()
        RingWalkWidgets.refreshAll(context)
    }

    override suspend fun setGoals(config: GoalConfig) {
        val normalizedDaily = config.daily.coerceIn(1_000, 100_000)
        val normalizedWeekly = config.weekly.coerceIn(7_000, 700_000)
        val normalizedMonthly = config.monthly.coerceIn(30_000, 3_000_000)
        goalDao.upsert(
            GoalEntity(
                id = 0,
                daily = normalizedDaily,
                weekly = normalizedWeekly,
                monthly = normalizedMonthly
            )
        )
        context.getSharedPreferences(TrackingConstants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(TrackingConstants.PREF_DAILY_GOAL, normalizedDaily)
            .apply()
        RingWalkWidgets.refreshAll(context)
    }

    override fun observeGoalConfig(): Flow<GoalConfig> =
        goalDao.observeGoal().map { goal ->
            GoalConfig(
                daily = goal?.daily ?: 8_000,
                weekly = goal?.weekly ?: 56_000,
                monthly = goal?.monthly ?: 240_000
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeGoalProgress(): Flow<List<GoalProgress>> =
        observeCurrentDate().flatMapLatest { now ->
            val weekStart = now.minusDays(now.dayOfWeek.value.toLong() - 1)
            val monthStart = now.withDayOfMonth(1)
            combine(
                observeGoalConfig(),
                stepDao.observeDay(now.toEpochDay()),
                stepDao.observeRange(weekStart.toEpochDay(), now.toEpochDay()),
                stepDao.observeRange(monthStart.toEpochDay(), now.toEpochDay())
            ) { goals, day, week, month ->
                val daySteps = day?.steps ?: 0
                val weekSteps = week.sumOf { it.steps }
                val monthSteps = month.sumOf { it.steps }
                val monthDays = now.dayOfMonth.coerceAtLeast(1)
                val monthlyForecast = ((monthSteps / monthDays.toFloat()) * now.lengthOfMonth()).toInt()
                listOf(
                    GoalProgress("goal_day", goals.daily, daySteps, daySteps),
                    GoalProgress("goal_week", goals.weekly, weekSteps, (weekSteps * 7f / now.dayOfWeek.value).toInt()),
                    GoalProgress("goal_month", goals.monthly, monthSteps, monthlyForecast)
                )
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAnalyticsOverview(): Flow<AnalyticsOverview> =
        observeCurrentDate().flatMapLatest { now ->
            val startOfYear = LocalDate.of(now.year, 1, 1).toEpochDay()
            val endOfYear = LocalDate.of(now.year, 12, 31).toEpochDay()
            val prevMonth = YearMonth.from(now).minusMonths(1)
            val currMonth = YearMonth.from(now)
            val previousComparableDay = now.dayOfMonth.coerceAtMost(prevMonth.lengthOfMonth())
            combine(
                stepDao.observeRange(startOfYear, endOfYear),
                stepDao.observeRange(prevMonth.atDay(1).toEpochDay(), prevMonth.atDay(previousComparableDay).toEpochDay()),
                stepDao.observeRange(currMonth.atDay(1).toEpochDay(), now.toEpochDay())
            ) { yearData, prevMonthData, currMonthData ->
                val yearly = yearData.sumOf { it.steps.toLong() }
                val elapsedDays = now.dayOfYear.coerceAtLeast(1)
                val average = (yearly / elapsedDays).toInt()
                val active = yearData.maxByOrNull { it.steps }?.let {
                    DailyPoint(LocalDate.ofEpochDay(it.dateEpochDay), it.steps)
                }
                val prevMonthSteps = prevMonthData.sumOf { it.steps }.toFloat()
                val currMonthSteps = currMonthData.sumOf { it.steps }.toFloat()
                val monthDelta = if (prevMonthSteps == 0f) 0f else ((currMonthSteps - prevMonthSteps) / prevMonthSteps) * 100f
                val forecast = (yearly / elapsedDays.toFloat() * now.lengthOfYear()).toLong()
                AnalyticsOverview(
                    yearlySteps = yearly,
                    averageSteps = average,
                    mostActiveDay = active,
                    monthComparisonPercent = monthDelta,
                    forecastYearlySteps = forecast,
                    heatmap = yearData.map { DailyPoint(LocalDate.ofEpochDay(it.dateEpochDay), it.steps) }
                )
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeDailyHeatMap(): Flow<List<Int>> =
        observeCurrentDate().flatMapLatest { today ->
            val end = today.toEpochDay()
            val start = today.minusDays(364).toEpochDay()
            stepDao.observeRange(start, end).map { range ->
                val map = range.associateBy({ it.dateEpochDay }, { it.steps })
                (start..end).map { day -> map[day] ?: 0 }
            }
        }

    override fun observeAchievements(): Flow<List<Achievement>> =
        combine(
            stepDao.observeLifetimeSteps(),
            gamificationDao.observeAchievements()
        ) { lifetime, states ->
            val unlockedMap = states.associateBy { it.key }
            achievementThresholds.map { (threshold, titleKey) ->
                val key = "steps_$threshold"
                val unlockedAt = unlockedMap[key]?.unlockedAt
                val reached = lifetime >= threshold || unlockedAt != null
                Achievement(
                    key = key,
                    title = titleKey,
                    description = "achievement_description",
                    threshold = threshold,
                    unlocked = reached,
                    unlockedAt = unlockedAt
                )
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeChallenges(): Flow<List<MiniChallenge>> =
        observeCurrentDate().flatMapLatest { today ->
            val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
            combine(
                stepDao.observeEventsForDay(today.toEpochDay()),
                stepDao.observeRange(today.minusDays(1).toEpochDay(), today.toEpochDay()),
                stepDao.observeRange(weekStart.toEpochDay(), today.toEpochDay())
            ) { todayEvents, twoDaySummaries, weekSummaries ->
                val morningSteps = todayEvents
                    .filter { java.time.Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).hour < 12 }
                    .sumOf { it.stepsDelta }
                listOf(
                    MiniChallenge(1, "morning_boost", "morning_boost_desc", 3_000, morningSteps),
                    MiniChallenge(2, "evening_streak", "evening_streak_desc", 16_000, twoDaySummaries.sumOf { it.steps }),
                    MiniChallenge(3, "weekly_sprint", "weekly_sprint_desc", 50_000, weekSummaries.sumOf { it.steps })
                )
            }
        }

    override fun observeBodyParams(): Flow<BodyParams> =
        bodyDao.observeBodyParams().map { entity ->
            if (entity == null) {
                BodyParams()
            } else {
                BodyParams(
                    weightKg = entity.weightKg,
                    heightCm = entity.heightCm,
                    age = entity.age,
                    gender = runCatching { GenderType.valueOf(entity.gender) }.getOrDefault(GenderType.OTHER),
                    activityMultiplier = entity.activityMultiplier,
                    targetWeightKg = entity.targetWeightKg
                )
            }
        }

    override suspend fun saveBodyParams(params: BodyParams) {
        bodyDao.upsertBodyParams(
            BodyParamsEntity(
                id = 0,
                weightKg = params.weightKg.coerceIn(25f, 350f),
                heightCm = params.heightCm.coerceIn(100, 250),
                age = params.age.coerceIn(13, 120),
                gender = params.gender.name,
                activityMultiplier = params.activityMultiplier.coerceIn(1.2f, 1.9f),
                targetWeightKg = params.targetWeightKg.coerceIn(25f, 350f)
            )
        )
    }

    override fun observeWeightTrend(): Flow<List<WeightPoint>> =
        bodyDao.observeWeights().map { list ->
            list.map { WeightPoint(it.timestamp, it.weightKg) }
        }

    override suspend fun saveWeight(weightKg: Float) {
        bodyDao.upsertWeight(
            WeightEntryEntity(
                timestamp = System.currentTimeMillis(),
                weightKg = weightKg.coerceIn(25f, 350f)
            )
        )
    }

    override suspend fun getTrackingSnapshot(): TrackingSnapshot {
        val today = LocalDate.now().toEpochDay()
        val day = stepDao.getDay(today)
        return TrackingSnapshot(
            steps = day?.steps ?: 0,
            calories = (day?.calories ?: 0f).toInt(),
            distanceMeters = (((day?.distanceKm ?: 0f) * 1000f).toInt()).coerceAtLeast(0),
            activeMinutes = day?.activeMinutes ?: 0
        )
    }

    override suspend fun ingestSensorTotal(total: Float, cadence: Float) {
        withContext(Dispatchers.IO) {
            ingestMutex.withLock {
            val today = LocalDate.now()
            val todayEpoch = today.toEpochDay()
            val current = stepDao.getDay(todayEpoch)
            val absoluteCounter = total.toInt().coerceAtLeast(0)
            val prefs = context.getSharedPreferences(TrackingConstants.PREFS_NAME, Context.MODE_PRIVATE)
            val previousDate = prefs.getLong(TrackingConstants.PREF_COUNTER_DATE, Long.MIN_VALUE)
            val previousCounter = prefs.getInt(TrackingConstants.PREF_REPOSITORY_COUNTER, -1)
            val delta = when {
                previousCounter < 0 || previousDate != todayEpoch -> 0
                absoluteCounter >= previousCounter -> absoluteCounter - previousCounter
                else -> 0 // Hardware reset: establish a new baseline without a spike.
            }
            prefs.edit()
                .putInt(TrackingConstants.PREF_REPOSITORY_COUNTER, absoluteCounter)
                .putLong(TrackingConstants.PREF_COUNTER_DATE, todayEpoch)
                .apply()

            val newSteps = (current?.steps ?: 0) + delta
            val body = bodyDao.observeBodyParams().first()
            val weightKg = body?.weightKg ?: 70f
            val heightCm = body?.heightCm ?: 175
            val strideKm = StepCalorieCalculator.strideKm(heightCm, cadence)
            val distance = (current?.distanceKm ?: 0f) + delta * strideKm
            val calories = (current?.calories ?: 0f) + estimateCalories(
                steps = delta,
                weightKg = weightKg,
                heightCm = heightCm,
                cadence = cadence
            )

            stepDao.insertEvent(
                StepEventEntity(
                    timestamp = System.currentTimeMillis(),
                    dateEpochDay = todayEpoch,
                    stepsDelta = delta,
                    totalCounter = total,
                    cadence = cadence,
                    activityKind = StepCalorieCalculator.activityKind(cadence).name
                )
            )
            val activeMinutes = stepDao.countActiveMinutesForDay(todayEpoch)

            stepDao.upsertDailySummary(
                DailySummaryEntity(
                    dateEpochDay = todayEpoch,
                    steps = newSteps,
                    distanceKm = distance,
                    calories = calories,
                    activeMinutes = activeMinutes
                )
            )
            }
        }
    }


    private fun estimateCalories(steps: Int, weightKg: Float, heightCm: Int, cadence: Float): Float =
        StepCalorieCalculator.estimate(steps, weightKg, heightCm, cadence)

    private fun calculateStreak(recent: List<DailySummaryEntity>): Int {
        if (recent.isEmpty()) return 0
        val map = recent.associateBy { it.dateEpochDay }
        val today = LocalDate.now().toEpochDay()
        var streak = 0
        // Today is still in progress: until its first step, continue yesterday's streak.
        var cursor = if ((map[today]?.steps ?: 0) > 0) today else today - 1
        while (true) {
            val day = map[cursor]
            if (day != null && day.steps > 0) {
                streak++
                cursor--
            } else {
                break
            }
        }
        return streak
    }

    override fun observeCustomChallenges(): Flow<List<CustomChallenge>> =
        combine(
            gamificationDao.observeCustomChallenges(),
            stepDao.observeAllSummaries()
        ) { challenges, summaries ->
            challenges.map { e ->
                val metric = runCatching { ChallengeMetric.valueOf(e.metric) }
                    .getOrDefault(ChallengeMetric.STEPS)
                val end = e.startEpochDay + e.durationDays - 1
                val progress = summaries
                    .filter { it.dateEpochDay in e.startEpochDay..end }
                    .sumOf { s ->
                        when (metric) {
                            ChallengeMetric.STEPS -> s.steps.toDouble()
                            ChallengeMetric.CALORIES -> s.calories.toDouble()
                            ChallengeMetric.DISTANCE -> s.distanceKm.toDouble()
                        }
                    }
                CustomChallenge(
                    id = e.id,
                    title = e.title,
                    metric = metric,
                    target = e.target,
                    durationDays = e.durationDays,
                    startEpochDay = e.startEpochDay,
                    progress = progress
                )
            }
        }

    override suspend fun createCustomChallenge(challenge: CustomChallenge) {
        gamificationDao.insertCustomChallenge(
            CustomChallengeEntity(
                title = challenge.title,
                metric = challenge.metric.name,
                target = challenge.target,
                durationDays = challenge.durationDays,
                startEpochDay = challenge.startEpochDay
            )
        )
    }

    override suspend fun deleteCustomChallenge(id: Long) {
        gamificationDao.deleteCustomChallenge(id)
    }

}
