package com.xaniihub.app.data.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.xaniihub.app.data.local.entity.AchievementStateEntity;
import com.xaniihub.app.data.local.entity.ChallengeProgressEntity;
import com.xaniihub.app.data.local.entity.CustomChallengeEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class GamificationDao_Impl implements GamificationDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AchievementStateEntity> __insertionAdapterOfAchievementStateEntity;

  private final EntityInsertionAdapter<ChallengeProgressEntity> __insertionAdapterOfChallengeProgressEntity;

  private final EntityInsertionAdapter<CustomChallengeEntity> __insertionAdapterOfCustomChallengeEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteCustomChallenge;

  public GamificationDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAchievementStateEntity = new EntityInsertionAdapter<AchievementStateEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `achievement_state` (`key`,`unlockedAt`) VALUES (?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AchievementStateEntity entity) {
        statement.bindString(1, entity.getKey());
        if (entity.getUnlockedAt() == null) {
          statement.bindNull(2);
        } else {
          statement.bindLong(2, entity.getUnlockedAt());
        }
      }
    };
    this.__insertionAdapterOfChallengeProgressEntity = new EntityInsertionAdapter<ChallengeProgressEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `challenge_progress` (`id`,`title`,`description`,`targetSteps`,`progressSteps`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ChallengeProgressEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getDescription());
        statement.bindLong(4, entity.getTargetSteps());
        statement.bindLong(5, entity.getProgressSteps());
      }
    };
    this.__insertionAdapterOfCustomChallengeEntity = new EntityInsertionAdapter<CustomChallengeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `custom_challenges` (`id`,`title`,`metric`,`target`,`durationDays`,`startEpochDay`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CustomChallengeEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getMetric());
        statement.bindDouble(4, entity.getTarget());
        statement.bindLong(5, entity.getDurationDays());
        statement.bindLong(6, entity.getStartEpochDay());
      }
    };
    this.__preparedStmtOfDeleteCustomChallenge = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM custom_challenges WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsertAchievement(final AchievementStateEntity state,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAchievementStateEntity.insert(state);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertChallenge(final ChallengeProgressEntity challenge,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfChallengeProgressEntity.insert(challenge);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertChallenges(final List<ChallengeProgressEntity> challenges,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfChallengeProgressEntity.insert(challenges);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertCustomChallenge(final CustomChallengeEntity challenge,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfCustomChallengeEntity.insertAndReturnId(challenge);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteCustomChallenge(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteCustomChallenge.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteCustomChallenge.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<AchievementStateEntity>> observeAchievements() {
    final String _sql = "SELECT * FROM achievement_state";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"achievement_state"}, new Callable<List<AchievementStateEntity>>() {
      @Override
      @NonNull
      public List<AchievementStateEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfKey = CursorUtil.getColumnIndexOrThrow(_cursor, "key");
          final int _cursorIndexOfUnlockedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "unlockedAt");
          final List<AchievementStateEntity> _result = new ArrayList<AchievementStateEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AchievementStateEntity _item;
            final String _tmpKey;
            _tmpKey = _cursor.getString(_cursorIndexOfKey);
            final Long _tmpUnlockedAt;
            if (_cursor.isNull(_cursorIndexOfUnlockedAt)) {
              _tmpUnlockedAt = null;
            } else {
              _tmpUnlockedAt = _cursor.getLong(_cursorIndexOfUnlockedAt);
            }
            _item = new AchievementStateEntity(_tmpKey,_tmpUnlockedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ChallengeProgressEntity>> observeChallenges() {
    final String _sql = "SELECT * FROM challenge_progress ORDER BY id";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"challenge_progress"}, new Callable<List<ChallengeProgressEntity>>() {
      @Override
      @NonNull
      public List<ChallengeProgressEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfTargetSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "targetSteps");
          final int _cursorIndexOfProgressSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "progressSteps");
          final List<ChallengeProgressEntity> _result = new ArrayList<ChallengeProgressEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChallengeProgressEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final int _tmpTargetSteps;
            _tmpTargetSteps = _cursor.getInt(_cursorIndexOfTargetSteps);
            final int _tmpProgressSteps;
            _tmpProgressSteps = _cursor.getInt(_cursorIndexOfProgressSteps);
            _item = new ChallengeProgressEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpTargetSteps,_tmpProgressSteps);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<CustomChallengeEntity>> observeCustomChallenges() {
    final String _sql = "SELECT * FROM custom_challenges ORDER BY id DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"custom_challenges"}, new Callable<List<CustomChallengeEntity>>() {
      @Override
      @NonNull
      public List<CustomChallengeEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfMetric = CursorUtil.getColumnIndexOrThrow(_cursor, "metric");
          final int _cursorIndexOfTarget = CursorUtil.getColumnIndexOrThrow(_cursor, "target");
          final int _cursorIndexOfDurationDays = CursorUtil.getColumnIndexOrThrow(_cursor, "durationDays");
          final int _cursorIndexOfStartEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "startEpochDay");
          final List<CustomChallengeEntity> _result = new ArrayList<CustomChallengeEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CustomChallengeEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpMetric;
            _tmpMetric = _cursor.getString(_cursorIndexOfMetric);
            final double _tmpTarget;
            _tmpTarget = _cursor.getDouble(_cursorIndexOfTarget);
            final int _tmpDurationDays;
            _tmpDurationDays = _cursor.getInt(_cursorIndexOfDurationDays);
            final long _tmpStartEpochDay;
            _tmpStartEpochDay = _cursor.getLong(_cursorIndexOfStartEpochDay);
            _item = new CustomChallengeEntity(_tmpId,_tmpTitle,_tmpMetric,_tmpTarget,_tmpDurationDays,_tmpStartEpochDay);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
