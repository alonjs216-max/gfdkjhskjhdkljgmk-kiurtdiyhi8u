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

    @Query("SELECT * FROM daily_summary WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY dateEpochDay ASC")
    fun observeRange(startEpochDay: Long, endEpochDay: Long): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM step_events ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestEvent(): StepEventEntity?

    @Query("SELECT COALESCE(SUM(steps), 0) FROM daily_summary")
    fun observeLifetimeSteps(): Flow<Long>

    @Query("SELECT * FROM daily_summary ORDER BY dateEpochDay DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM daily_summary")
    suspend fun getAllSummaries(): List<DailySummaryEntity>
}
