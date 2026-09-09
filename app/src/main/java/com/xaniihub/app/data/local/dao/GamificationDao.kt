package com.xaniihub.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xaniihub.app.data.local.entity.AchievementStateEntity
import com.xaniihub.app.data.local.entity.ChallengeProgressEntity
import com.xaniihub.app.data.local.entity.CustomChallengeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GamificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAchievement(state: AchievementStateEntity)

    @Query("SELECT * FROM achievement_state")
    fun observeAchievements(): Flow<List<AchievementStateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChallenge(challenge: ChallengeProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChallenges(challenges: List<ChallengeProgressEntity>)

    @Query("SELECT * FROM challenge_progress ORDER BY id")
    fun observeChallenges(): Flow<List<ChallengeProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomChallenge(challenge: CustomChallengeEntity): Long

    @Query("SELECT * FROM custom_challenges ORDER BY id DESC")
    fun observeCustomChallenges(): Flow<List<CustomChallengeEntity>>

    @Query("DELETE FROM custom_challenges WHERE id = :id")
    suspend fun deleteCustomChallenge(id: Long)
}