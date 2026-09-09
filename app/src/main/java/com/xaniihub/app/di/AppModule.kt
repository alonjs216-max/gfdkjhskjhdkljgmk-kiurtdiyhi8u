package com.xaniihub.app.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.xaniihub.app.data.local.XaniiHubDatabase
import com.xaniihub.app.data.local.dao.BodyDao
import com.xaniihub.app.data.local.dao.GamificationDao
import com.xaniihub.app.data.local.dao.GoalDao
import com.xaniihub.app.data.local.dao.StepDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): XaniiHubDatabase {
        return Room.databaseBuilder(
            context,
            XaniiHubDatabase::class.java,
            "xaniihub.db"
        ).addMigrations(XaniiHubDatabase.MIGRATION_1_2, XaniiHubDatabase.MIGRATION_2_3)
            .build()
    }

    @Provides
    fun provideStepDao(database: XaniiHubDatabase): StepDao = database.stepDao()

    @Provides
    fun provideGoalDao(database: XaniiHubDatabase): GoalDao = database.goalDao()

    @Provides
    fun provideBodyDao(database: XaniiHubDatabase): BodyDao = database.bodyDao()

    @Provides
    fun provideGamificationDao(database: XaniiHubDatabase): GamificationDao = database.gamificationDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)
}
