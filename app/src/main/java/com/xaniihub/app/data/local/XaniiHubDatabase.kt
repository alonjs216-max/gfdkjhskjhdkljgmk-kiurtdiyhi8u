package com.xaniihub.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xaniihub.app.data.local.dao.BodyDao
import com.xaniihub.app.data.local.dao.GamificationDao
import com.xaniihub.app.data.local.dao.GoalDao
import com.xaniihub.app.data.local.dao.StepDao
import com.xaniihub.app.data.local.entity.AchievementStateEntity
import com.xaniihub.app.data.local.entity.BodyParamsEntity
import com.xaniihub.app.data.local.entity.ChallengeProgressEntity
import com.xaniihub.app.data.local.entity.CustomChallengeEntity
import com.xaniihub.app.data.local.entity.DailySummaryEntity
import com.xaniihub.app.data.local.entity.GoalEntity
import com.xaniihub.app.data.local.entity.StepEventEntity
import com.xaniihub.app.data.local.entity.WeightEntryEntity

@Database(
    entities = [
        StepEventEntity::class,
        DailySummaryEntity::class,
        GoalEntity::class,
        BodyParamsEntity::class,
        WeightEntryEntity::class,
        AchievementStateEntity::class,
        ChallengeProgressEntity::class,
        CustomChallengeEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class XaniiHubDatabase : RoomDatabase() {
    abstract fun stepDao(): StepDao
    abstract fun goalDao(): GoalDao
    abstract fun bodyDao(): BodyDao
    abstract fun gamificationDao(): GamificationDao

    companion object {
        /** Preserves version-1 step history while adding gamification tables in version 2. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `achievement_state` (`key` TEXT NOT NULL, `unlockedAt` INTEGER, PRIMARY KEY(`key`))")
                database.execSQL("CREATE TABLE IF NOT EXISTS `challenge_progress` (`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `targetSteps` INTEGER NOT NULL, `progressSteps` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                database.execSQL("CREATE TABLE IF NOT EXISTS `custom_challenges` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `metric` TEXT NOT NULL, `target` REAL NOT NULL, `durationDays` INTEGER NOT NULL, `startEpochDay` INTEGER NOT NULL)")
            }
        }

        /** Rebuilds derived distance/calories without changing immutable step events. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    UPDATE daily_summary
                    SET distanceKm = COALESCE((
                        SELECT SUM(
                            e.stepsDelta *
                            (CASE WHEN e.activityKind = 'RUNNING' THEN 0.65 ELSE 0.415 END) *
                            COALESCE((SELECT heightCm FROM body_params WHERE id = 0 AND heightCm BETWEEN 100 AND 250), 175) /
                            100000.0
                        )
                        FROM step_events e
                        WHERE e.dateEpochDay = daily_summary.dateEpochDay
                    ), 0),
                    calories = COALESCE((
                        SELECT SUM(
                            (CASE WHEN e.activityKind = 'RUNNING' THEN 8.5 ELSE 3.3 END) *
                            COALESCE((SELECT weightKg FROM body_params WHERE id = 0 AND weightKg BETWEEN 25 AND 350), 70) *
                            ((e.stepsDelta *
                                (CASE WHEN e.activityKind = 'RUNNING' THEN 0.65 ELSE 0.415 END) *
                                COALESCE((SELECT heightCm FROM body_params WHERE id = 0 AND heightCm BETWEEN 100 AND 250), 175) /
                                100000.0) /
                                (CASE WHEN e.activityKind = 'RUNNING' THEN 9.0 ELSE 5.0 END)) /
                            1.0
                        )
                        FROM step_events e
                        WHERE e.dateEpochDay = daily_summary.dateEpochDay
                    ), 0)
                    """.trimIndent()
                )
            }
        }
    }
}
