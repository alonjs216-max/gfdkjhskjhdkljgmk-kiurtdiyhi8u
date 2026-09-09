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
import java.time.Instant
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
            emit(LocalDate.now())
            // Sleeping straight through to midnight in one delay misses time zone changes and
            // manual clock edits, and drifts in Doze, so poll at least once a minute. The
            // downstream distinctUntilChanged keeps this from re-emitting the same date.
            val nextMidnight = LocalDate.now()
                .plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            delay((nextMidnight - System.currentTimeMillis()).coerceIn(1_000L, DATE_REFRESH_INTERVAL_MS))
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
            val goalValue = goal?.daily ?: DEFAULT_DAILY_GOAL
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
                hourlySteps = hourlyStepsOf(date, events)
            )
        }
    }

    override suspend fun setDailyGoal(goal: Int) {
        val normalized = goal.coerceIn(1_000, 100_000)
        val existing = goalDao.observeGoal().first() ?: GoalEntity(
            daily = DEFAULT_DAILY_GOAL,
            weekly = DEFAULT_WEEKLY_GOAL,
            monthly = DEFAULT_MONTHLY_GOAL
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
                daily = goal?.daily ?: DEFAULT_DAILY_GOAL,
                weekly = goal?.weekly ?: DEFAULT_WEEKLY_GOAL,
                monthly = goal?.monthly ?: DEFAULT_MONTHLY_GOAL
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
                val morningSteps = hourlyStepsOf(today, todayEvents).take(12).sum()
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

    override suspend fun ingestSensorTotal(total: Float, eventTimeMillis: Long) {
        if (!total.isFinite()) return
        withContext(Dispatchers.IO) {
            ingestMutex.withLock {
                val prefs = context.getSharedPreferences(TrackingConstants.PREFS_NAME, Context.MODE_PRIVATE)
                val counter = total.coerceAtLeast(0f)
                val previousCounter = prefs.getFloat(TrackingConstants.PREF_LAST_SENSOR_COUNTER, -1f)
                val previousEventTime = prefs.getLong(TrackingConstants.PREF_LAST_SENSOR_EVENT_TIME, 0L)
                val eventTime = if (eventTimeMillis > 0L) eventTimeMillis else System.currentTimeMillis()

                // TYPE_STEP_COUNTER keeps counting while this process is dead, so the difference
                // between two readings is exactly what still has to be recorded - including the
                // steps taken while the service was restarting or the day was rolling over.
                val delta = when {
                    previousCounter < 0f -> 0 // First reading ever: only establish the baseline.
                    counter >= previousCounter -> (counter - previousCounter).toInt()
                    eventTime >= previousEventTime -> counter.toInt() // Counter restarted after a reboot.
                    else -> 0 // Out of order reading: ignore it.
                }.coerceIn(0, MAX_PLAUSIBLE_DELTA)

                prefs.edit()
                    .putFloat(TrackingConstants.PREF_LAST_SENSOR_COUNTER, counter)
                    .putLong(TrackingConstants.PREF_LAST_SENSOR_EVENT_TIME, eventTime)
                    .apply()

                if (delta <= 0) return@withLock

                val cadence = cadenceOf(previousEventTime, eventTime, delta)
                val body = bodyDao.observeBodyParams().first()
                val weightKg = body?.weightKg ?: DEFAULT_WEIGHT_KG
                val heightCm = body?.heightCm ?: DEFAULT_HEIGHT_CM
                val strideKm = StepCalorieCalculator.strideKm(heightCm, cadence)
                val activityKind = StepCalorieCalculator.activityKind(cadence).name

                splitAcrossDays(previousEventTime, eventTime, delta).forEach { segment ->
                    stepDao.insertEvent(
                        StepEventEntity(
                            timestamp = segment.timestamp,
                            dateEpochDay = segment.dateEpochDay,
                            stepsDelta = segment.steps,
                            totalCounter = total,
                            cadence = cadence,
                            activityKind = activityKind
                        )
                    )
                    val current = stepDao.getDay(segment.dateEpochDay)
                    stepDao.upsertDailySummary(
                        DailySummaryEntity(
                            dateEpochDay = segment.dateEpochDay,
                            steps = (current?.steps ?: 0) + segment.steps,
                            distanceKm = (current?.distanceKm ?: 0f) + segment.steps * strideKm,
                            calories = (current?.calories ?: 0f) + estimateCalories(
                                steps = segment.steps,
                                weightKg = weightKg,
                                heightCm = heightCm,
                                cadence = cadence
                            ),
                            activeMinutes = ((current?.activeMinutes ?: 0) + activeMinutesOf(segment.steps, segment.durationMs))
                                .coerceAtMost(MINUTES_PER_DAY)
                        )
                    )
                }
            }
        }
    }

    private data class StepSegment(
        val dateEpochDay: Long,
        val timestamp: Long,
        val steps: Int,
        val durationMs: Long
    )

    /**
     * Splits [steps] over the calendar days between [previousEventTime] and [eventTime].
     * A single reading can cover midnight, because the sensor counts on its own while the app is
     * not running - those steps used to be dropped or credited to the wrong day.
     */
    private fun splitAcrossDays(previousEventTime: Long, eventTime: Long, steps: Int): List<StepSegment> {
        val zone = ZoneId.systemDefault()
        val eventDay = Instant.ofEpochMilli(eventTime).atZone(zone).toLocalDate()
        val gapMs = if (previousEventTime in 1L until eventTime) eventTime - previousEventTime else 0L
        val single = listOf(
            StepSegment(
                dateEpochDay = eventDay.toEpochDay(),
                timestamp = eventTime,
                steps = steps,
                durationMs = gapMs
            )
        )
        if (gapMs !in 1L..MAX_SPLIT_WINDOW_MS) return single
        val startDay = Instant.ofEpochMilli(previousEventTime).atZone(zone).toLocalDate()
        if (startDay == eventDay) return single

        val covered = mutableListOf<Pair<Long, Long>>()
        var cursorTime = previousEventTime
        var cursorDay = startDay
        while (cursorTime < eventTime && covered.size < MAX_SPLIT_SEGMENTS) {
            val nextMidnight = cursorDay.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val segmentEnd = minOf(nextMidnight, eventTime)
            if (segmentEnd <= cursorTime) break
            covered += cursorDay.toEpochDay() to (segmentEnd - cursorTime)
            cursorTime = segmentEnd
            cursorDay = cursorDay.plusDays(1)
        }
        if (covered.isEmpty() || cursorTime < eventTime) return single

        val totalMs = covered.sumOf { it.second }.coerceAtLeast(1L)
        val segments = mutableListOf<StepSegment>()
        var assigned = 0
        covered.forEachIndexed { index, entry ->
            val day = entry.first
            val durationMs = entry.second
            val portion = if (index == covered.lastIndex) {
                steps - assigned
            } else {
                ((steps.toLong() * durationMs) / totalMs).toInt()
            }
            assigned += portion
            if (portion > 0) {
                val timestamp = if (index == covered.lastIndex) {
                    eventTime
                } else {
                    LocalDate.ofEpochDay(day).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                }
                segments += StepSegment(day, timestamp, portion, durationMs)
            }
        }
        return if (segments.isEmpty()) single else segments
    }

    /**
     * Cadence of a reading, derived from the interval it covers. The service used to accumulate
     * this in memory, which reset to 0 on every restart and distorted calories and distance.
     */
    private fun cadenceOf(previousEventTime: Long, eventTime: Long, steps: Int): Float {
        if (previousEventTime <= 0L || eventTime <= previousEventTime || steps <= 0) return 0f
        val elapsedMs = eventTime - previousEventTime
        if (elapsedMs < MIN_CADENCE_WINDOW_MS || elapsedMs > MAX_CADENCE_WINDOW_MS) return 0f
        return (steps * 60_000f / elapsedMs).coerceIn(0f, MAX_CADENCE)
    }

    /**
     * Active minutes contributed by one reading. Counting distinct minute buckets of the event
     * timestamps (the previous SQL approach) turned every single reading into a full active
     * minute, so a handful of steps could add tens of minutes of "activity".
     */
    private fun activeMinutesOf(steps: Int, durationMs: Long): Int {
        if (steps <= 0) return 0
        val plausibleMinutes = ceilDiv(steps.toLong(), STEPS_PER_ACTIVE_MINUTE)
        val coveredMinutes = if (durationMs > 0L) ceilDiv(durationMs, 60_000L) else plausibleMinutes
        return minOf(plausibleMinutes, coveredMinutes)
            .coerceIn(1L, MINUTES_PER_DAY.toLong())
            .toInt()
    }

    /**
     * Steps per hour of [date]. Readings arrive batched, so charting the whole delta at the
     * timestamp of the reading produced spikes in the wrong hour; spread each reading over the
     * window it actually covers instead.
     */
    private fun hourlyStepsOf(date: LocalDate, events: List<StepEventEntity>): List<Int> {
        val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val buckets = IntArray(24)
        var previousTimestamp = 0L
        events.sortedBy { it.timestamp }.forEach { event ->
            if (event.stepsDelta > 0) {
                val plausibleMs = ceilDiv(event.stepsDelta.toLong(), STEPS_PER_ACTIVE_MINUTE) * 60_000L
                val gapMs = if (previousTimestamp in 1L until event.timestamp) {
                    event.timestamp - previousTimestamp
                } else {
                    plausibleMs
                }
                spreadSteps(
                    buckets = buckets,
                    dayStart = dayStart,
                    start = event.timestamp - minOf(plausibleMs, gapMs),
                    end = event.timestamp,
                    steps = event.stepsDelta
                )
            }
            previousTimestamp = maxOf(previousTimestamp, event.timestamp)
        }
        return buckets.toList()
    }

    private fun spreadSteps(buckets: IntArray, dayStart: Long, start: Long, end: Long, steps: Int) {
        val firstMinute = ((start - dayStart) / 60_000L).coerceIn(0L, LAST_MINUTE_OF_DAY)
        val lastMinute = ((end - dayStart) / 60_000L).coerceIn(firstMinute, LAST_MINUTE_OF_DAY)
        val minuteCount = (lastMinute - firstMinute + 1L).toInt()
        val perMinute = steps / minuteCount
        var remainder = steps - perMinute * minuteCount
        for (minute in firstMinute..lastMinute) {
            var value = perMinute
            if (remainder > 0) {
                value++
                remainder--
            }
            buckets[(minute / 60L).toInt().coerceIn(0, 23)] += value
        }
    }

    private fun ceilDiv(value: Long, divisor: Long): Long =
        if (value <= 0L || divisor <= 0L) 0L else (value + divisor - 1L) / divisor

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

    private companion object {
        const val DEFAULT_DAILY_GOAL = 8_000
        const val DEFAULT_WEEKLY_GOAL = 56_000
        const val DEFAULT_MONTHLY_GOAL = 240_000
        const val DEFAULT_WEIGHT_KG = 70f
        const val DEFAULT_HEIGHT_CM = 175
        const val MAX_PLAUSIBLE_DELTA = 50_000
        const val MAX_SPLIT_WINDOW_MS = 24L * 60L * 60L * 1_000L
        const val MAX_SPLIT_SEGMENTS = 4
        const val MIN_CADENCE_WINDOW_MS = 5_000L
        const val MAX_CADENCE_WINDOW_MS = 5L * 60L * 1_000L
        const val MAX_CADENCE = 220f
        const val STEPS_PER_ACTIVE_MINUTE = 100L
        const val MINUTES_PER_DAY = 1_440
        const val LAST_MINUTE_OF_DAY = 1_439L
        const val DATE_REFRESH_INTERVAL_MS = 60_000L
    }
}
