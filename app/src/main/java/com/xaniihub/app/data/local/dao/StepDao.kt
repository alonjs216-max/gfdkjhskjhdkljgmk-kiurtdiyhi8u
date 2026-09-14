package com.xaniihub.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xaniihub.app.data.local.entity.DailySummaryEntity
import com.xaniihub.app.data.local.entity.StepEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: StepEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailySummary(summary: DailySummaryEntity)

    /**
     * Writes a whole batch in one transaction. Rebuilding the derived metrics day by day left
     * half of the history on the new weight and half on the old one whenever the rebuild was
     * interrupted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailySummaries(summaries: List<DailySummaryEntity>)

    @Query("SELECT * FROM daily_summary WHERE dateEpochDay = :dateEpochDay LIMIT 1")
    fun observeDay(dateEpochDay: Long): Flow<DailySummaryEntity?>

    @Query("SELECT * FROM daily_summary WHERE dateEpochDay = :dateEpochDay LIMIT 1")
    suspend fun getDay(dateEpochDay: Long): DailySummaryEntity?

    @Query("SELECT * FROM step_events WHERE dateEpochDay = :dateEpochDay ORDER BY timestamp ASC")
    fun observeEventsForDay(dateEpochDay: Long): Flow<List<StepEventEntity>>

    @Query("SELECT * FROM step_events WHERE dateEpochDay = :dateEpochDay ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestEventForDay(dateEpochDay: Long): StepEventEntity?

    @Query("SELECT * FROM step_events WHERE dateEpochDay = :dateEpochDay ORDER BY timestamp ASC")
    suspend fun getEventsForDay(dateEpochDay: Long): List<StepEventEntity>

    // Active minutes have exactly one definition: XaniiRepositoryImpl.activeMinutesOf() derives
    // them from the steps and the time span of each reading and stores the result in
    // daily_summary.activeMinutes. The former "SELECT COUNT(DISTINCT timestamp / 60000)" query
    // was a second, conflicting definition - it counted every batched reading as a whole active
    // minute - so it has been removed instead of being kept around unused.

    @Query("SELECT * FROM daily_summary ORDER BY dateEpochDay DESC")
    fun observeAllSummaries(): Flow<List<DailySummaryEntity>>

    /**
     * The most recent [limit] days that are not in the future of [endEpochDay], newest first.
     * The streak only needs a bounded window ending on the day being displayed; observing every
     * summary ever recorded re-emitted the full table on every sensor reading.
     */
    @Query("SELECT * FROM daily_summary WHERE dateEpochDay <= :endEpochDay ORDER BY dateEpochDay DESC LIMIT :limit")
    fun observeSummariesUpTo(endEpochDay: Long, limit: Int): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM daily_summary WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY dateEpochDay ASC")
    fun observeRange(startEpochDay: Long, endEpochDay: Long): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM step_events ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestEvent(): StepEventEntity?

    @Query("SELECT COALESCE(SUM(steps), 0) FROM daily_summary")
    fun observeLifetimeSteps(): Flow<Long>

    @Query("SELECT COALESCE(SUM(steps), 0) FROM daily_summary")
    suspend fun getLifetimeSteps(): Long

    @Query("SELECT * FROM daily_summary ORDER BY dateEpochDay DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM daily_summary")
    suspend fun getAllSummaries(): List<DailySummaryEntity>

    /**
     * Drops raw readings older than [beforeEpochDay]. One row per sensor reading forever meant
     * the table grew without any bound; the per-day summaries are what the app actually shows,
     * and they are kept for good.
     */
    @Query("DELETE FROM step_events WHERE dateEpochDay < :beforeEpochDay")
    suspend fun pruneEventsBefore(beforeEpochDay: Long): Int
}
