package com.xaniihub.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xaniihub.app.data.local.entity.BodyParamsEntity
import com.xaniihub.app.data.local.entity.WeightEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBodyParams(params: BodyParamsEntity)

    @Query("SELECT * FROM body_params WHERE id = 0 LIMIT 1")
    fun observeBodyParams(): Flow<BodyParamsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeight(entry: WeightEntryEntity)

    @Query("SELECT * FROM weight_entries ORDER BY timestamp ASC")
    fun observeWeights(): Flow<List<WeightEntryEntity>>
}