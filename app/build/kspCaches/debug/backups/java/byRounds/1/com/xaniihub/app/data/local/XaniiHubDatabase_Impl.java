package com.xaniihub.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.xaniihub.app.data.local.dao.BodyDao;
import com.xaniihub.app.data.local.dao.BodyDao_Impl;
import com.xaniihub.app.data.local.dao.GamificationDao;
import com.xaniihub.app.data.local.dao.GamificationDao_Impl;
import com.xaniihub.app.data.local.dao.GoalDao;
import com.xaniihub.app.data.local.dao.GoalDao_Impl;
import com.xaniihub.app.data.local.dao.StepDao;
import com.xaniihub.app.data.local.dao.StepDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class XaniiHubDatabase_Impl extends XaniiHubDatabase {
  private volatile StepDao _stepDao;

  private volatile GoalDao _goalDao;

  private volatile BodyDao _bodyDao;

  private volatile GamificationDao _gamificationDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `step_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `dateEpochDay` INTEGER NOT NULL, `stepsDelta` INTEGER NOT NULL, `totalCounter` REAL NOT NULL, `cadence` REAL NOT NULL, `activityKind` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `daily_summary` (`dateEpochDay` INTEGER NOT NULL, `steps` INTEGER NOT NULL, `distanceKm` REAL NOT NULL, `calories` REAL NOT NULL, `activeMinutes` INTEGER NOT NULL, PRIMARY KEY(`dateEpochDay`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `goals` (`id` INTEGER NOT NULL, `daily` INTEGER NOT NULL, `weekly` INTEGER NOT NULL, `monthly` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `body_params` (`id` INTEGER NOT NULL, `weightKg` REAL NOT NULL, `heightCm` INTEGER NOT NULL, `age` INTEGER NOT NULL, `gender` TEXT NOT NULL, `activityMultiplier` REAL NOT NULL, `targetWeightKg` REAL NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `weight_entries` (`timestamp` INTEGER NOT NULL, `weightKg` REAL NOT NULL, PRIMARY KEY(`timestamp`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `achievement_state` (`key` TEXT NOT NULL, `unlockedAt` INTEGER, PRIMARY KEY(`key`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `challenge_progress` (`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `targetSteps` INTEGER NOT NULL, `progressSteps` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `custom_challenges` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `metric` TEXT NOT NULL, `target` REAL NOT NULL, `durationDays` INTEGER NOT NULL, `startEpochDay` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '9c53cd42f4f85584a357c944c22fbc9f')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `step_events`");
        db.execSQL("DROP TABLE IF EXISTS `daily_summary`");
        db.execSQL("DROP TABLE IF EXISTS `goals`");
        db.execSQL("DROP TABLE IF EXISTS `body_params`");
        db.execSQL("DROP TABLE IF EXISTS `weight_entries`");
        db.execSQL("DROP TABLE IF EXISTS `achievement_state`");
        db.execSQL("DROP TABLE IF EXISTS `challenge_progress`");
        db.execSQL("DROP TABLE IF EXISTS `custom_challenges`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsStepEvents = new HashMap<String, TableInfo.Column>(7);
        _columnsStepEvents.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStepEvents.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStepEvents.put("dateEpochDay", new TableInfo.Column("dateEpochDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStepEvents.put("stepsDelta", new TableInfo.Column("stepsDelta", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStepEvents.put("totalCounter", new TableInfo.Column("totalCounter", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStepEvents.put("cadence", new TableInfo.Column("cadence", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStepEvents.put("activityKind", new TableInfo.Column("activityKind", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysStepEvents = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesStepEvents = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoStepEvents = new TableInfo("step_events", _columnsStepEvents, _foreignKeysStepEvents, _indicesStepEvents);
        final TableInfo _existingStepEvents = TableInfo.read(db, "step_events");
        if (!_infoStepEvents.equals(_existingStepEvents)) {
          return new RoomOpenHelper.ValidationResult(false, "step_events(com.xaniihub.app.data.local.entity.StepEventEntity).\n"
                  + " Expected:\n" + _infoStepEvents + "\n"
                  + " Found:\n" + _existingStepEvents);
        }
        final HashMap<String, TableInfo.Column> _columnsDailySummary = new HashMap<String, TableInfo.Column>(5);
        _columnsDailySummary.put("dateEpochDay", new TableInfo.Column("dateEpochDay", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailySummary.put("steps", new TableInfo.Column("steps", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailySummary.put("distanceKm", new TableInfo.Column("distanceKm", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailySummary.put("calories", new TableInfo.Column("calories", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailySummary.put("activeMinutes", new TableInfo.Column("activeMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDailySummary = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDailySummary = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDailySummary = new TableInfo("daily_summary", _columnsDailySummary, _foreignKeysDailySummary, _indicesDailySummary);
        final TableInfo _existingDailySummary = TableInfo.read(db, "daily_summary");
        if (!_infoDailySummary.equals(_existingDailySummary)) {
          return new RoomOpenHelper.ValidationResult(false, "daily_summary(com.xaniihub.app.data.local.entity.DailySummaryEntity).\n"
                  + " Expected:\n" + _infoDailySummary + "\n"
                  + " Found:\n" + _existingDailySummary);
        }
        final HashMap<String, TableInfo.Column> _columnsGoals = new HashMap<String, TableInfo.Column>(4);
        _columnsGoals.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("daily", new TableInfo.Column("daily", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("weekly", new TableInfo.Column("weekly", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("monthly", new TableInfo.Column("monthly", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGoals = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGoals = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoGoals = new TableInfo("goals", _columnsGoals, _foreignKeysGoals, _indicesGoals);
        final TableInfo _existingGoals = TableInfo.read(db, "goals");
        if (!_infoGoals.equals(_existingGoals)) {
          return new RoomOpenHelper.ValidationResult(false, "goals(com.xaniihub.app.data.local.entity.GoalEntity).\n"
                  + " Expected:\n" + _infoGoals + "\n"
                  + " Found:\n" + _existingGoals);
        }
        final HashMap<String, TableInfo.Column> _columnsBodyParams = new HashMap<String, TableInfo.Column>(7);
        _columnsBodyParams.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBodyParams.put("weightKg", new TableInfo.Column("weightKg", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBodyParams.put("heightCm", new TableInfo.Column("heightCm", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBodyParams.put("age", new TableInfo.Column("age", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBodyParams.put("gender", new TableInfo.Column("gender", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBodyParams.put("activityMultiplier", new TableInfo.Column("activityMultiplier", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBodyParams.put("targetWeightKg", new TableInfo.Column("targetWeightKg", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBodyParams = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBodyParams = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBodyParams = new TableInfo("body_params", _columnsBodyParams, _foreignKeysBodyParams, _indicesBodyParams);
        final TableInfo _existingBodyParams = TableInfo.read(db, "body_params");
        if (!_infoBodyParams.equals(_existingBodyParams)) {
          return new RoomOpenHelper.ValidationResult(false, "body_params(com.xaniihub.app.data.local.entity.BodyParamsEntity).\n"
                  + " Expected:\n" + _infoBodyParams + "\n"
                  + " Found:\n" + _existingBodyParams);
        }
        final HashMap<String, TableInfo.Column> _columnsWeightEntries = new HashMap<String, TableInfo.Column>(2);
        _columnsWeightEntries.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeightEntries.put("weightKg", new TableInfo.Column("weightKg", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWeightEntries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWeightEntries = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoWeightEntries = new TableInfo("weight_entries", _columnsWeightEntries, _foreignKeysWeightEntries, _indicesWeightEntries);
        final TableInfo _existingWeightEntries = TableInfo.read(db, "weight_entries");
        if (!_infoWeightEntries.equals(_existingWeightEntries)) {
          return new RoomOpenHelper.ValidationResult(false, "weight_entries(com.xaniihub.app.data.local.entity.WeightEntryEntity).\n"
                  + " Expected:\n" + _infoWeightEntries + "\n"
                  + " Found:\n" + _existingWeightEntries);
        }
        final HashMap<String, TableInfo.Column> _columnsAchievementState = new HashMap<String, TableInfo.Column>(2);
        _columnsAchievementState.put("key", new TableInfo.Column("key", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAchievementState.put("unlockedAt", new TableInfo.Column("unlockedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAchievementState = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAchievementState = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAchievementState = new TableInfo("achievement_state", _columnsAchievementState, _foreignKeysAchievementState, _indicesAchievementState);
        final TableInfo _existingAchievementState = TableInfo.read(db, "achievement_state");
        if (!_infoAchievementState.equals(_existingAchievementState)) {
          return new RoomOpenHelper.ValidationResult(false, "achievement_state(com.xaniihub.app.data.local.entity.AchievementStateEntity).\n"
                  + " Expected:\n" + _infoAchievementState + "\n"
                  + " Found:\n" + _existingAchievementState);
        }
        final HashMap<String, TableInfo.Column> _columnsChallengeProgress = new HashMap<String, TableInfo.Column>(5);
        _columnsChallengeProgress.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChallengeProgress.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChallengeProgress.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChallengeProgress.put("targetSteps", new TableInfo.Column("targetSteps", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChallengeProgress.put("progressSteps", new TableInfo.Column("progressSteps", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysChallengeProgress = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesChallengeProgress = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoChallengeProgress = new TableInfo("challenge_progress", _columnsChallengeProgress, _foreignKeysChallengeProgress, _indicesChallengeProgress);
        final TableInfo _existingChallengeProgress = TableInfo.read(db, "challenge_progress");
        if (!_infoChallengeProgress.equals(_existingChallengeProgress)) {
          return new RoomOpenHelper.ValidationResult(false, "challenge_progress(com.xaniihub.app.data.local.entity.ChallengeProgressEntity).\n"
                  + " Expected:\n" + _infoChallengeProgress + "\n"
                  + " Found:\n" + _existingChallengeProgress);
        }
        final HashMap<String, TableInfo.Column> _columnsCustomChallenges = new HashMap<String, TableInfo.Column>(6);
        _columnsCustomChallenges.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomChallenges.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomChallenges.put("metric", new TableInfo.Column("metric", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomChallenges.put("target", new TableInfo.Column("target", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomChallenges.put("durationDays", new TableInfo.Column("durationDays", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomChallenges.put("startEpochDay", new TableInfo.Column("startEpochDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCustomChallenges = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCustomChallenges = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCustomChallenges = new TableInfo("custom_challenges", _columnsCustomChallenges, _foreignKeysCustomChallenges, _indicesCustomChallenges);
        final TableInfo _existingCustomChallenges = TableInfo.read(db, "custom_challenges");
        if (!_infoCustomChallenges.equals(_existingCustomChallenges)) {
          return new RoomOpenHelper.ValidationResult(false, "custom_challenges(com.xaniihub.app.data.local.entity.CustomChallengeEntity).\n"
                  + " Expected:\n" + _infoCustomChallenges + "\n"
                  + " Found:\n" + _existingCustomChallenges);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "9c53cd42f4f85584a357c944c22fbc9f", "95dbcf65311a632b52d4c0f0298863b7");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "step_events","daily_summary","goals","body_params","weight_entries","achievement_state","challenge_progress","custom_challenges");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `step_events`");
      _db.execSQL("DELETE FROM `daily_summary`");
      _db.execSQL("DELETE FROM `goals`");
      _db.execSQL("DELETE FROM `body_params`");
      _db.execSQL("DELETE FROM `weight_entries`");
      _db.execSQL("DELETE FROM `achievement_state`");
      _db.execSQL("DELETE FROM `challenge_progress`");
      _db.execSQL("DELETE FROM `custom_challenges`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(StepDao.class, StepDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(GoalDao.class, GoalDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(BodyDao.class, BodyDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(GamificationDao.class, GamificationDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public StepDao stepDao() {
    if (_stepDao != null) {
      return _stepDao;
    } else {
      synchronized(this) {
        if(_stepDao == null) {
          _stepDao = new StepDao_Impl(this);
        }
        return _stepDao;
      }
    }
  }

  @Override
  public GoalDao goalDao() {
    if (_goalDao != null) {
      return _goalDao;
    } else {
      synchronized(this) {
        if(_goalDao == null) {
          _goalDao = new GoalDao_Impl(this);
        }
        return _goalDao;
      }
    }
  }

  @Override
  public BodyDao bodyDao() {
    if (_bodyDao != null) {
      return _bodyDao;
    } else {
      synchronized(this) {
        if(_bodyDao == null) {
          _bodyDao = new BodyDao_Impl(this);
        }
        return _bodyDao;
      }
    }
  }

  @Override
  public GamificationDao gamificationDao() {
    if (_gamificationDao != null) {
      return _gamificationDao;
    } else {
      synchronized(this) {
        if(_gamificationDao == null) {
          _gamificationDao = new GamificationDao_Impl(this);
        }
        return _gamificationDao;
      }
    }
  }
}
