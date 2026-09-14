package com.xaniihub.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.xaniihub.app.data.local.dao.BodyDao
import com.xaniihub.app.data.local.dao.GamificationDao
import com.xaniihub.app.data.local.dao.GoalDao
import com.xaniihub.app.data.local.dao.StepDao
import com.xaniihub.app.data.local.entity.AchievementStateEntity
import com.xaniihub.app.data.local.entity.BodyParamsEntity
import com.xaniihub.app.data.local.entity.CustomChallengeEntity
import com.xaniihub.app.data.local.entity.DailySummaryEntity
import com.xaniihub.app.data.local.entity.GoalEntity
import com.xaniihub.app.data.local.entity.StepEventEntity
import com.xaniihub.app.data.local.entity.WeightEntryEntity
import com.xaniihub.app.domain.goals.GoalTypesController
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
import com.xaniihub.app.tracking.StepTrackingService
import com.xaniihub.app.tracking.TrackingConstants
import com.xaniihub.app.widget.RingWalkWidgets
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
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

    /**
     * Outlives every screen: the repository is a singleton, and the work started here (rebuilding
     * the derived metrics of the whole history) must not be cancelled just because the user left
     * the screen that triggered it.
     */
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val achievementThresholds = listOf(
        10_000L to "achievement_10k",
        100_000L to "achievement_100k",
        1_000_000L to "achievement_1m"
    )

    /**
     * The current calendar day. Five flows of this repository and the home screen each used to
     * start their own copy of this loop, so the app kept six tickers alive to learn the same
     * date; they now share one.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentDate: Flow<LocalDate> = flow {
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
    }
        .distinctUntilChanged()
        .shareIn(repositoryScope, SharingStarted.WhileSubscribed(TICKER_IDLE_TIMEOUT_MS), replay = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeDashboardStats(): Flow<DashboardStats> =
        observeCurrentDate().flatMapLatest(::observeDashboardStatsForDate)

    override fun observeCurrentDate(): Flow<LocalDate> = currentDate

    override fun observeDashboardStatsForDate(date: LocalDate): Flow<DashboardStats> {
        val dateEpoch = date.toEpochDay()
        return combine(
            stepDao.observeDay(dateEpoch),
            goalDao.observeGoal(),
            stepDao.observeLifetimeSteps(),
            // Bounded window: the streak only needs the days up to the one being displayed, while
            // observing every summary ever recorded re-emitted the whole table on every reading.
            stepDao.observeSummariesUpTo(dateEpoch, STREAK_LOOKBACK_DAYS),
            stepDao.observeEventsForDay(dateEpoch)
        ) { day, goal, lifetime, recent, events ->
            val goalValue = goal?.daily ?: DEFAULT_DAILY_GOAL
            val summary = day ?: DailySummaryEntity(
                dateEpochDay = dateEpoch,
                steps = 0,
                distanceKm = 0f,
                calories = 0f,
                activeMinutes = 0,
                goal = goalValue
            )
            // A finished day is judged against the goal it was recorded with, the day in progress
            // against the current one - otherwise editing the goal would rewrite the history.
            val isPastDay = dateEpoch < LocalDate.now().toEpochDay()
            val effectiveGoal = if (isPastDay && summary.goal > 0) summary.goal else goalValue
            val streaks = calculateStreaks(recent, dateEpoch, goalValue)
            DashboardStats(
                date = date,
                steps = summary.steps,
                dailyGoal = effectiveGoal,
                distanceKm = summary.distanceKm,
                calories = summary.calories,
                activeMinutes = summary.activeMinutes,
                streakDays = streaks.current,
                lifetimeSteps = lifetime,
                hourlySteps = hourlyStepsOf(date, events),
                streakBestDays = streaks.best
            )
        }
    }

    override suspend fun setDailyGoal(goal: Int) {
        val normalized = goal.coerceIn(MIN_DAILY_GOAL, MAX_DAILY_GOAL)
        val existing = goalDao.observeGoal().first() ?: GoalEntity(
            daily = DEFAULT_DAILY_GOAL,
            weekly = DEFAULT_WEEKLY_GOAL,
            monthly = DEFAULT_MONTHLY_GOAL
        )
        goalDao.upsert(
            existing.copy(
                daily = normalized,
                // Only the daily goal can be edited in the UI, so the weekly and monthly ones
                // stayed on their defaults and the period cards compared the progress against a
                // goal the user had never chosen.
                weekly = (normalized.toLong() * 7L).coerceIn(MIN_WEEKLY_GOAL, MAX_WEEKLY_GOAL).toInt(),
                monthly = (normalized.toLong() * 30L).coerceIn(MIN_MONTHLY_GOAL, MAX_MONTHLY_GOAL).toInt()
            )
        )
        snapshotGoalForToday(normalized)
        publishDailyGoal(normalized)
    }

    override suspend fun setGoals(config: GoalConfig) {
        val normalizedDaily = config.daily.coerceIn(MIN_DAILY_GOAL, MAX_DAILY_GOAL)
        val normalizedWeekly = config.weekly.toLong().coerceIn(MIN_WEEKLY_GOAL, MAX_WEEKLY_GOAL).toInt()
        val normalizedMonthly = config.monthly.toLong().coerceIn(MIN_MONTHLY_GOAL, MAX_MONTHLY_GOAL).toInt()
        goalDao.upsert(
            GoalEntity(
                id = 0,
                daily = normalizedDaily,
                weekly = normalizedWeekly,
                monthly = normalizedMonthly
            )
        )
        snapshotGoalForToday(normalizedDaily)
        publishDailyGoal(normalizedDaily)
    }

    /**
     * Records the new goal on the day in progress. Finished days keep the goal they were recorded
     * with, so raising the goal today cannot break a streak that was already earned.
     */
    private suspend fun snapshotGoalForToday(goal: Int) {
        withContext(Dispatchers.IO) {
            ingestMutex.withLock {
                val today = LocalDate.now().toEpochDay()
                val summary = stepDao.getDay(today) ?: return@withLock
                if (summary.goal != goal) {
                    stepDao.upsertDailySummary(summary.copy(goal = goal))
                }
            }
        }
    }

    private fun publishDailyGoal(goal: Int) {
        context.getSharedPreferences(TrackingConstants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(TrackingConstants.PREF_DAILY_GOAL, goal)
            .apply()
        // Third store of the same number: the onboarding wrote the goal here and nothing ever
        // updated it again, so the goal screens could disagree with the dashboard forever.
        GoalTypesController.setStepGoal(context, goal)
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
                    // The daily forecast used to be the current count, so the card promised that
                    // the day would end on exactly the steps already taken.
                    GoalProgress("goal_day", goals.daily, daySteps, forecastForToday(daySteps)),
                    GoalProgress("goal_week", goals.weekly, weekSteps, (weekSteps * 7f / now.dayOfWeek.value).toInt()),
                    GoalProgress("goal_month", goals.monthly, monthSteps, monthlyForecast)
                )
            }
        }

    /**
     * Extrapolates the steps of the day in progress. Extrapolating from the first minutes after
     * midnight produces absurd numbers, so the forecast only starts once enough of the day has
     * passed and never falls below what has already been counted.
     */
    private fun forecastForToday(steps: Int): Int {
        if (steps <= 0) return 0
        val minutesElapsed = LocalTime.now().toSecondOfDay() / 60
        if (minutesElapsed < MIN_FORECAST_MINUTES) return steps
        return ((steps.toLong() * MINUTES_PER_DAY) / minutesElapsed)
            .coerceIn(steps.toLong(), MAX_DAILY_FORECAST.toLong())
            .toInt()
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
                // Dividing by the day of the year buried the average of everybody who installed
                // the app after January: months without the app counted as zero-step days. Count
                // from the first day that actually has data instead.
                val firstTrackedDay = yearData.filter { it.steps > 0 }.minOfOrNull { it.dateEpochDay }
                val elapsedDays = if (firstTrackedDay == null) {
                    1
                } else {
                    (now.toEpochDay() - firstTrackedDay + 1L)
                        .coerceIn(1L, now.dayOfYear.toLong())
                        .toInt()
                }
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
                    // Two day total, despite the key: renaming it needs new localized strings.
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
        val normalized = BodyParamsEntity(
            id = 0,
            weightKg = params.weightKg.coerceIn(25f, 350f),
            heightCm = params.heightCm.coerceIn(100, 250),
            age = params.age.coerceIn(13, 120),
            gender = params.gender.name,
            activityMultiplier = params.activityMultiplier.coerceIn(1.2f, 1.9f),
            targetWeightKg = params.targetWeightKg.coerceIn(25f, 350f)
        )
        val previous = bodyDao.observeBodyParams().first()
        bodyDao.upsertBodyParams(normalized)
        // Calories and distance are derived from weight and height, but they are persisted as
        // per-day totals that were accumulated at ingest time. Without rebuilding them the
        // profile would show the new weight while the dashboard, the widgets and the tracking
        // notification kept reporting numbers computed from the previous (or the default) one.
        if (previous == null ||
            previous.weightKg != normalized.weightKg ||
            previous.heightCm != normalized.heightCm
        ) {
            scheduleRecalculation(normalized)
        } else {
            RingWalkWidgets.refreshAll(context)
        }
    }

    override fun observeWeightTrend(): Flow<List<WeightPoint>> =
        bodyDao.observeWeights().map { list ->
            list.map { WeightPoint(it.timestamp, it.weightKg) }
        }

    override suspend fun saveWeight(weightKg: Float) {
        val normalized = weightKg.coerceIn(25f, 350f)
        bodyDao.upsertWeight(
            WeightEntryEntity(
                timestamp = System.currentTimeMillis(),
                weightKg = normalized
            )
        )
        // A logged weight is the current weight of the user, so the profile - and with it the
        // calorie estimate - has to follow it instead of keeping two sources of truth that
        // silently drift apart.
        val current = bodyDao.observeBodyParams().first()
        if (current != null && current.weightKg == normalized) return
        val updated = current?.copy(weightKg = normalized) ?: BodyParamsEntity(
            id = 0,
            weightKg = normalized,
            heightCm = DEFAULT_HEIGHT_CM,
            age = DEFAULT_AGE,
            gender = GenderType.OTHER.name,
            activityMultiplier = DEFAULT_ACTIVITY_MULTIPLIER,
            targetWeightKg = normalized
        )
        bodyDao.upsertBodyParams(updated)
        scheduleRecalculation(updated)
    }

    /**
     * Rebuilding the whole history takes far longer than the lifetime of a screen, and it used to
     * run in the scope of the caller: leaving the profile mid-rebuild left part of the history
     * converted to the new weight and part of it on the old one. The repository scope keeps it
     * running, and [ingestMutex] serialises concurrent rebuilds.
     */
    private fun scheduleRecalculation(body: BodyParamsEntity) {
        repositoryScope.launch { recalculateDerivedMetrics(body) }
    }

    /**
     * Rebuilds the stored per-day distance and calories from the immutable step events with
     * [body]. Steps and active minutes do not depend on the body params, so they are kept as is,
     * and days without stored events keep whatever they had.
     */
    private suspend fun recalculateDerivedMetrics(body: BodyParamsEntity) {
        withContext(Dispatchers.IO) {
            ingestMutex.withLock {
                val updated = mutableListOf<DailySummaryEntity>()
                stepDao.getAllSummaries().forEach { summary ->
                    val events = stepDao.getEventsForDay(summary.dateEpochDay)
                        .filter { it.stepsDelta > 0 }
                    if (events.isEmpty()) return@forEach
                    var distanceKm = 0f
                    var calories = 0f
                    for (event in events) {
                        distanceKm += event.stepsDelta * StepCalorieCalculator.strideKm(body.heightCm, event.cadence)
                        calories += estimateCalories(
                            steps = event.stepsDelta,
                            weightKg = body.weightKg,
                            heightCm = body.heightCm,
                            cadence = event.cadence
                        )
                    }
                    if (summary.distanceKm != distanceKm || summary.calories != calories) {
                        updated += summary.copy(distanceKm = distanceKm, calories = calories)
                    }
                }
                // One transactional write instead of one per day, so the history is never left
                // half converted if the process is killed in the middle of the rebuild.
                if (updated.isNotEmpty()) stepDao.upsertDailySummaries(updated)
            }
        }
        // The widgets, the quick settings tile and the tracking notification all cache the
        // numbers they display, so they have to be repainted explicitly after a recalculation.
        RingWalkWidgets.refreshAll(context)
        StepTrackingService.refresh(context)
    }

    override suspend fun getTrackingSnapshot(): TrackingSnapshot = withContext(Dispatchers.IO) {
        // The service polls this while the sensor keeps delivering readings, and without the lock
        // it could observe a day whose event was already stored but whose summary was not.
        ingestMutex.withLock {
            val day = stepDao.getDay(LocalDate.now().toEpochDay())
            TrackingSnapshot(
                steps = day?.steps ?: 0,
                calories = (day?.calories ?: 0f).toInt(),
                distanceMeters = (((day?.distanceKm ?: 0f) * 1000f).toInt()).coerceAtLeast(0),
                activeMinutes = day?.activeMinutes ?: 0
            )
        }
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
                }.coerceIn(0, maxPlausibleDelta(previousEventTime, eventTime))

                prefs.edit()
                    .putFloat(TrackingConstants.PREF_LAST_SENSOR_COUNTER, counter)
                    .putLong(TrackingConstants.PREF_LAST_SENSOR_EVENT_TIME, eventTime)
                    .apply()

                pruneStaleEvents(prefs)

                if (delta <= 0) return@withLock

                val cadence = cadenceOf(previousEventTime, eventTime, delta)
                val body = bodyDao.observeBodyParams().first()
                val weightKg = body?.weightKg ?: DEFAULT_WEIGHT_KG
                val heightCm = body?.heightCm ?: DEFAULT_HEIGHT_CM
                val dailyGoal = goalDao.observeGoal().first()?.daily ?: DEFAULT_DAILY_GOAL
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
                                .coerceAtMost(MINUTES_PER_DAY),
                            // The goal the streak judges this day against. A day that already has
                            // one keeps it, so backfilled days never change an earned streak.
                            goal = current?.goal?.takeIf { it > 0 } ?: dailyGoal
                        )
                    )
                }

                persistUnlockedAchievements()
            }
        }
    }

    /**
     * Upper bound for a single reading, derived from the time it covers. The flat 50 000 step cap
     * was wrong in both directions: it accepted an impossible 50 000 step jump within a minute,
     * and it silently discarded real steps when the counter had been running for days without
     * the app. The bound follows the elapsed time at a cadence nobody can sustain, with a floor
     * for batched readings and a ceiling for absurd counter resets.
     */
    private fun maxPlausibleDelta(previousEventTime: Long, eventTime: Long): Int {
        if (previousEventTime <= 0L || eventTime <= previousEventTime) return MIN_PLAUSIBLE_DELTA
        val gapMinutes = (eventTime - previousEventTime) / 60_000L
        return (gapMinutes * MAX_STEPS_PER_MINUTE)
            .coerceIn(MIN_PLAUSIBLE_DELTA.toLong(), MAX_PLAUSIBLE_DELTA.toLong())
            .toInt()
    }

    /**
     * Raw readings are only needed to rebuild the derived metrics and to draw the hourly chart,
     * so they are kept for [EVENT_RETENTION_DAYS] days and dropped afterwards; the table used to
     * grow with every single reading for as long as the app stayed installed. The per-day
     * summaries that the app actually displays are kept forever. Runs at most once a day.
     */
    private suspend fun pruneStaleEvents(prefs: SharedPreferences) {
        val today = LocalDate.now().toEpochDay()
        if (prefs.getLong(PREF_LAST_PRUNE_DAY, Long.MIN_VALUE) == today) return
        stepDao.pruneEventsBefore(today - EVENT_RETENTION_DAYS)
        prefs.edit().putLong(PREF_LAST_PRUNE_DAY, today).apply()
    }

    /**
     * Stores the unlock timestamp the first time a threshold is reached. The achievement list was
     * derived from the lifetime total alone, so nothing ever wrote to the achievement table and
     * every unlocked badge reported an unknown unlock date.
     */
    private suspend fun persistUnlockedAchievements() {
        val lifetime = stepDao.getLifetimeSteps()
        if (lifetime < achievementThresholds.minOf { it.first }) return
        val stored = gamificationDao.observeAchievements().first().associateBy { it.key }
        val unlockedAt = System.currentTimeMillis()
        achievementThresholds.forEach { (threshold, _) ->
            val key = "steps_$threshold"
            if (lifetime >= threshold && stored[key]?.unlockedAt == null) {
                gamificationDao.upsertAchievement(
                    AchievementStateEntity(key = key, unlockedAt = unlockedAt)
                )
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
     * not running - those steps used to be dropped or credited to the wrong day. The window
     * covers a week, so a phone that was not opened over a weekend still gets its days right.
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

    private data class StreakSnapshot(val current: Int, val best: Int)

    /**
     * Counts the consecutive days, ending on [referenceDay], on which the daily goal was reached.
     *
     * A day counts only if its steps reached the goal that was active on it, so a single step no
     * longer keeps a streak alive and editing the goal no longer rewrites the past. [referenceDay]
     * is still in progress: while its goal is not reached yet the streak of the finished days is
     * reported unchanged, and the day joins the streak the moment the goal is met. Finishing a day
     * below the goal breaks the streak - there is no grace day, so the reset happens on its own at
     * midnight, when the missed day stops being the day in progress.
     *
     * [summaries] is the bounded window from StepDao.observeSummariesUpTo, so the reported best is
     * the best within that window.
     */
    private fun calculateStreaks(
        summaries: List<DailySummaryEntity>,
        referenceDay: Long,
        currentGoal: Int
    ): StreakSnapshot {
        if (summaries.isEmpty()) return StreakSnapshot(0, 0)
        val goalMet = HashMap<Long, Boolean>(summaries.size)
        summaries.forEach { summary ->
            val goal = if (summary.goal > 0) summary.goal else currentGoal
            goalMet[summary.dateEpochDay] = goal > 0 && summary.steps >= goal
        }
        var cursor = if (goalMet[referenceDay] == true) referenceDay else referenceDay - 1
        var current = 0
        while (goalMet[cursor] == true) {
            current++
            cursor--
        }
        var best = current
        var run = 0
        // A day with no row at all is a day without steps, so it breaks the run as well - hence
        // the explicit check that the days really follow each other.
        var previousDay = Long.MIN_VALUE
        summaries.asSequence()
            .filter { it.dateEpochDay <= referenceDay }
            .sortedBy { it.dateEpochDay }
            .forEach { summary ->
                val day = summary.dateEpochDay
                run = if (goalMet[day] == true) {
                    if (day == previousDay + 1L) run + 1 else 1
                } else {
                    0
                }
                if (run > best) best = run
                previousDay = day
            }
        return StreakSnapshot(current = current, best = best)
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
        const val DEFAULT_AGE = 27
        const val DEFAULT_ACTIVITY_MULTIPLIER = 1.2f
        const val MIN_DAILY_GOAL = 1_000
        const val MAX_DAILY_GOAL = 100_000
        const val MIN_WEEKLY_GOAL = 7_000L
        const val MAX_WEEKLY_GOAL = 700_000L
        const val MIN_MONTHLY_GOAL = 30_000L
        const val MAX_MONTHLY_GOAL = 3_000_000L
        const val MIN_PLAUSIBLE_DELTA = 1_000
        const val MAX_PLAUSIBLE_DELTA = 250_000
        const val MAX_STEPS_PER_MINUTE = 250L
        const val MAX_SPLIT_WINDOW_MS = 7L * 24L * 60L * 60L * 1_000L
        const val MAX_SPLIT_SEGMENTS = 8
        const val MIN_CADENCE_WINDOW_MS = 5_000L
        const val MAX_CADENCE_WINDOW_MS = 5L * 60L * 1_000L
        const val MAX_CADENCE = 220f
        const val STEPS_PER_ACTIVE_MINUTE = 100L
        const val MINUTES_PER_DAY = 1_440
        const val LAST_MINUTE_OF_DAY = 1_439L
        const val DATE_REFRESH_INTERVAL_MS = 60_000L
        const val TICKER_IDLE_TIMEOUT_MS = 5_000L
        const val STREAK_LOOKBACK_DAYS = 730
        const val EVENT_RETENTION_DAYS = 400L
        const val MIN_FORECAST_MINUTES = 120
        const val MAX_DAILY_FORECAST = 200_000
        const val PREF_LAST_PRUNE_DAY = "last_prune_epoch_day"
    }
}
