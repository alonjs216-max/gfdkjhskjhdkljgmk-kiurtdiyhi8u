package com.xaniihub.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.xaniihub.app.data.local.entity.DailySummaryEntity;
import com.xaniihub.app.data.local.entity.StepEventEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class StepDao_Impl implements StepDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<StepEventEntity> __insertionAdapterOfStepEventEntity;

  private final EntityInsertionAdapter<DailySummaryEntity> __insertionAdapterOfDailySummaryEntity;

  public StepDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfStepEventEntity = new EntityInsertionAdapter<StepEventEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `step_events` (`id`,`timestamp`,`dateEpochDay`,`stepsDelta`,`totalCounter`,`cadence`,`activityKind`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final StepEventEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTimestamp());
        statement.bindLong(3, entity.getDateEpochDay());
        statement.bindLong(4, entity.getStepsDelta());
        statement.bindDouble(5, entity.getTotalCounter());
        statement.bindDouble(6, entity.getCadence());
        statement.bindString(7, entity.getActivityKind());
      }
    };
    this.__insertionAdapterOfDailySummaryEntity = new EntityInsertionAdapter<DailySummaryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `daily_summary` (`dateEpochDay`,`steps`,`distanceKm`,`calories`,`activeMinutes`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DailySummaryEntity entity) {
        statement.bindLong(1, entity.getDateEpochDay());
        statement.bindLong(2, entity.getSteps());
        statement.bindDouble(3, entity.getDistanceKm());
        statement.bindDouble(4, entity.getCalories());
        statement.bindLong(5, entity.getActiveMinutes());
      }
    };
  }

  @Override
  public Object insertEvent(final StepEventEntity event,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfStepEventEntity.insert(event);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertDailySummary(final DailySummaryEntity summary,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDailySummaryEntity.insert(summary);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<DailySummaryEntity> observeDay(final long dateEpochDay) {
    final String _sql = "SELECT * FROM daily_summary WHERE dateEpochDay = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, dateEpochDay);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"daily_summary"}, new Callable<DailySummaryEntity>() {
      @Override
      @Nullable
      public DailySummaryEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDateEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "dateEpochDay");
          final int _cursorIndexOfSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "steps");
          final int _cursorIndexOfDistanceKm = CursorUtil.getColumnIndexOrThrow(_cursor, "distanceKm");
          final int _cursorIndexOfCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "calories");
          final int _cursorIndexOfActiveMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "activeMinutes");
          final DailySummaryEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpDateEpochDay;
            _tmpDateEpochDay = _cursor.getLong(_cursorIndexOfDateEpochDay);
            final int _tmpSteps;
            _tmpSteps = _cursor.getInt(_cursorIndexOfSteps);
            final float _tmpDistanceKm;
            _tmpDistanceKm = _cursor.getFloat(_cursorIndexOfDistanceKm);
            final float _tmpCalories;
            _tmpCalories = _cursor.getFloat(_cursorIndexOfCalories);
            final int _tmpActiveMinutes;
            _tmpActiveMinutes = _cursor.getInt(_cursorIndexOfActiveMinutes);
            _result = new DailySummaryEntity(_tmpDateEpochDay,_tmpSteps,_tmpDistanceKm,_tmpCalories,_tmpActiveMinutes);
          } else {
            _result = null;
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
  public Object getDay(final long dateEpochDay,
      final Continuation<? super DailySummaryEntity> $completion) {
    final String _sql = "SELECT * FROM daily_summary WHERE dateEpochDay = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, dateEpochDay);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DailySummaryEntity>() {
      @Override
      @Nullable
      public DailySummaryEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDateEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "dateEpochDay");
          final int _cursorIndexOfSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "steps");
          final int _cursorIndexOfDistanceKm = CursorUtil.getColumnIndexOrThrow(_cursor, "distanceKm");
          final int _cursorIndexOfCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "calories");
          final int _cursorIndexOfActiveMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "activeMinutes");
          final DailySummaryEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpDateEpochDay;
            _tmpDateEpochDay = _cursor.getLong(_cursorIndexOfDateEpochDay);
            final int _tmpSteps;
            _tmpSteps = _cursor.getInt(_cursorIndexOfSteps);
            final float _tmpDistanceKm;
            _tmpDistanceKm = _cursor.getFloat(_cursorIndexOfDistanceKm);
            final float _tmpCalories;
            _tmpCalories = _cursor.getFloat(_cursorIndexOfCalories);
            final int _tmpActiveMinutes;
            _tmpActiveMinutes = _cursor.getInt(_cursorIndexOfActiveMinutes);
            _result = new DailySummaryEntity(_tmpDateEpochDay,_tmpSteps,_tmpDistanceKm,_tmpCalories,_tmpActiveMinutes);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<StepEventEntity>> observeEventsForDay(final long dateEpochDay) {
    final String _sql = "SELECT * FROM step_events WHERE dateEpochDay = ? ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, dateEpochDay);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"step_events"}, new Callable<List<StepEventEntity>>() {
      @Override
      @NonNull
      public List<StepEventEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDateEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "dateEpochDay");
          final int _cursorIndexOfStepsDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "stepsDelta");
          final int _cursorIndexOfTotalCounter = CursorUtil.getColumnIndexOrThrow(_cursor, "totalCounter");
          final int _cursorIndexOfCadence = CursorUtil.getColumnIndexOrThrow(_cursor, "cadence");
          final int _cursorIndexOfActivityKind = CursorUtil.getColumnIndexOrThrow(_cursor, "activityKind");
          final List<StepEventEntity> _result = new ArrayList<StepEventEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StepEventEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final long _tmpDateEpochDay;
            _tmpDateEpochDay = _cursor.getLong(_cursorIndexOfDateEpochDay);
            final int _tmpStepsDelta;
            _tmpStepsDelta = _cursor.getInt(_cursorIndexOfStepsDelta);
            final float _tmpTotalCounter;
            _tmpTotalCounter = _cursor.getFloat(_cursorIndexOfTotalCounter);
            final float _tmpCadence;
            _tmpCadence = _cursor.getFloat(_cursorIndexOfCadence);
            final String _tmpActivityKind;
            _tmpActivityKind = _cursor.getString(_cursorIndexOfActivityKind);
            _item = new StepEventEntity(_tmpId,_tmpTimestamp,_tmpDateEpochDay,_tmpStepsDelta,_tmpTotalCounter,_tmpCadence,_tmpActivityKind);
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
  public Object getLatestEventForDay(final long dateEpochDay,
      final Continuation<? super StepEventEntity> $completion) {
    final String _sql = "SELECT * FROM step_events WHERE dateEpochDay = ? ORDER BY timestamp DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, dateEpochDay);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<StepEventEntity>() {
      @Override
      @Nullable
      public StepEventEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDateEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "dateEpochDay");
          final int _cursorIndexOfStepsDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "stepsDelta");
          final int _cursorIndexOfTotalCounter = CursorUtil.getColumnIndexOrThrow(_cursor, "totalCounter");
          final int _cursorIndexOfCadence = CursorUtil.getColumnIndexOrThrow(_cursor, "cadence");
          final int _cursorIndexOfActivityKind = CursorUtil.getColumnIndexOrThrow(_cursor, "activityKind");
          final StepEventEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final long _tmpDateEpochDay;
            _tmpDateEpochDay = _cursor.getLong(_cursorIndexOfDateEpochDay);
            final int _tmpStepsDelta;
            _tmpStepsDelta = _cursor.getInt(_cursorIndexOfStepsDelta);
            final float _tmpTotalCounter;
            _tmpTotalCounter = _cursor.getFloat(_cursorIndexOfTotalCounter);
            final float _tmpCadence;
            _tmpCadence = _cursor.getFloat(_cursorIndexOfCadence);
            final String _tmpActivityKind;
            _tmpActivityKind = _cursor.getString(_cursorIndexOfActivityKind);
            _result = new StepEventEntity(_tmpId,_tmpTimestamp,_tmpDateEpochDay,_tmpStepsDelta,_tmpTotalCounter,_tmpCadence,_tmpActivityKind);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getEventsForDay(final long dateEpochDay,
      final Continuation<? super List<StepEventEntity>> $completion) {
    final String _sql = "SELECT * FROM step_events WHERE dateEpochDay = ? ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, dateEpochDay);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<StepEventEntity>>() {
      @Override
      @NonNull
      public List<StepEventEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDateEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "dateEpochDay");
          final int _cursorIndexOfStepsDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "stepsDelta");
          final int _cursorIndexOfTotalCounter = CursorUtil.getColumnIndexOrThrow(_cursor, "totalCounter");
          final int _cursorIndexOfCadence = CursorUtil.getColumnIndexOrThrow(_cursor, "cadence");
          final int _cursorIndexOfActivityKind = CursorUtil.getColumnIndexOrThrow(_cursor, "activityKind");
          final List<StepEventEntity> _result = new ArrayList<StepEventEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StepEventEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final long _tmpDateEpochDay;
            _tmpDateEpochDay = _cursor.getLong(_cursorIndexOfDateEpochDay);
            final int _tmpStepsDelta;
            _tmpStepsDelta = _cursor.getInt(_cursorIndexOfStepsDelta);
            final float _tmpTotalCounter;
            _tmpTotalCounter = _cursor.getFloat(_cursorIndexOfTotalCounter);
            final float _tmpCadence;
            _tmpCadence = _cursor.getFloat(_cursorIndexOfCadence);
            final String _tmpActivityKind;
            _tmpActivityKind = _cursor.getString(_cursorIndexOfActivityKind);
            _item = new StepEventEntity(_tmpId,_tmpTimestamp,_tmpDateEpochDay,_tmpStepsDelta,_tmpTotalCounter,_tmpCadence,_tmpActivityKind);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object countActiveMinutesForDay(final long dateEpochDay,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(DISTINCT timestamp / 60000) FROM step_events WHERE dateEpochDay = ? AND stepsDelta > 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, dateEpochDay);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<DailySummaryEntity>> observeAllSummaries() {
    final String _sql = "SELECT * FROM daily_summary ORDER BY dateEpochDay DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"daily_summary"}, new Callable<List<DailySummaryEntity>>() {
      @Override
      @NonNull
      public List<DailySummaryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDateEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "dateEpochDay");
          final int _cursorIndexOfSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "steps");
          final int _cursorIndexOfDistanceKm = CursorUtil.getColumnIndexOrThrow(_cursor, "distanceKm");
          final int _cursorIndexOfCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "calories");
          final int _cursorIndexOfActiveMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "activeMinutes");
          final List<DailySummaryEntity> _result = new ArrayList<DailySummaryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailySummaryEntity _item;
            final long _tmpDateEpochDay;
            _tmpDateEpochDay = _cursor.getLong(_cursorIndexOfDateEpochDay);
            final int _tmpSteps;
            _tmpSteps = _cursor.getInt(_cursorIndexOfSteps);
            final float _tmpDistanceKm;
            _tmpDistanceKm = _cursor.getFloat(_cursorIndexOfDistanceKm);
            final float _tmpCalories;
            _tmpCalories = _cursor.getFloat(_cursorIndexOfCalories);
            final int _tmpActiveMinutes;
            _tmpActiveMinutes = _cursor.getInt(_cursorIndexOfActiveMinutes);
            _item = new DailySummaryEntity(_tmpDateEpochDay,_tmpSteps,_tmpDistanceKm,_tmpCalories,_tmpActiveMinutes);
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
  public Flow<List<DailySummaryEntity>> observeRange(final long startEpochDay,
      final long endEpochDay) {
    final String _sql = "SELECT * FROM daily_summary WHERE dateEpochDay BETWEEN ? AND ? ORDER BY dateEpochDay ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startEpochDay);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endEpochDay);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"daily_summary"}, new Callable<List<DailySummaryEntity>>() {
      @Override
      @NonNull
      public List<DailySummaryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDateEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "dateEpochDay");
          final int _cursorIndexOfSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "steps");
          final int _cursorIndexOfDistanceKm = CursorUtil.getColumnIndexOrThrow(_cursor, "distanceKm");
          final int _cursorIndexOfCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "calories");
          final int _cursorIndexOfActiveMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "activeMinutes");
          final List<DailySummaryEntity> _result = new ArrayList<DailySummaryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailySummaryEntity _item;
            final long _tmpDateEpochDay;
            _tmpDateEpochDay = _cursor.getLong(_cursorIndexOfDateEpochDay);
            final int _tmpSteps;
            _tmpSteps = _cursor.getInt(_cursorIndexOfSteps);
            final float _tmpDistanceKm;
            _tmpDistanceKm = _cursor.getFloat(_cursorIndexOfDistanceKm);
            final float _tmpCalories;
            _tmpCalories = _cursor.getFloat(_cursorIndexOfCalories);
            final int _tmpActiveMinutes;
            _tmpActiveMinutes = _cursor.getInt(_cursorIndexOfActiveMinutes);
            _item = new DailySummaryEntity(_tmpDateEpochDay,_tmpSteps,_tmpDistanceKm,_tmpCalories,_tmpActiveMinutes);
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
  public Object getLatestEvent(final Continuation<? super StepEventEntity> $completion) {
    final String _sql = "SELECT * FROM step_events ORDER BY timestamp DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<StepEventEntity>() {
      @Override
      @Nullable
      public StepEventEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDateEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "dateEpochDay");
          final int _cursorIndexOfStepsDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "stepsDelta");
          final int _cursorIndexOfTotalCounter = CursorUtil.getColumnIndexOrThrow(_cursor, "totalCounter");
          final int _cursorIndexOfCadence = CursorUtil.getColumnIndexOrThrow(_cursor, "cadence");
          final int _cursorIndexOfActivityKind = CursorUtil.getColumnIndexOrThrow(_cursor, "activityKind");
          final StepEventEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final long _tmpDateEpochDay;
            _tmpDateEpochDay = _cursor.getLong(_cursorIndexOfDateEpochDay);
            final int _tmpStepsDelta;
            _tmpStepsDelta = _cursor.getInt(_cursorIndexOfStepsDelta);
            final float _tmpTotalCounter;
            _tmpTotalCounter = _cursor.getFloat(_cursorIndexOfTotalCounter);
            final float _tmpCadence;
            _tmpCadence = _cursor.getFloat(_cursorIndexOfCadence);
            final String _tmpActivityKind;
            _tmpActivityKind = _cursor.getString(_cursorIndexOfActivityKind);
            _result = new StepEventEntity(_tmpId,_tmpTimestamp,_tmpDateEpochDay,_tmpStepsDelta,_tmpTotalCounter,_tmpCadence,_tmpActivityKind);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<Long> observeLifetimeSteps() {
    final String _sql = "SELECT COALESCE(SUM(steps), 0) FROM daily_summary";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"daily_summary"}, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            final long _tmp;
            _tmp = _cursor.getLong(0);
            _result = _tmp;
          } else {
            _result = 0L;
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
  public Flow<List<DailySummaryEntity>> observeRecent(final int limit) {
    final String _sql = "SELECT * FROM daily_summary ORDER BY dateEpochDay DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"daily_summary"}, new Callable<List<DailySummaryEntity>>() {
      @Override
      @NonNull
      public List<DailySummaryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDateEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "dateEpochDay");
          final int _cursorIndexOfSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "steps");
          final int _cursorIndexOfDistanceKm = CursorUtil.getColumnIndexOrThrow(_cursor, "distanceKm");
          final int _cursorIndexOfCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "calories");
          final int _cursorIndexOfActiveMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "activeMinutes");
          final List<DailySummaryEntity> _result = new ArrayList<DailySummaryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailySummaryEntity _item;
            final long _tmpDateEpochDay;
            _tmpDateEpochDay = _cursor.getLong(_cursorIndexOfDateEpochDay);
            final int _tmpSteps;
            _tmpSteps = _cursor.getInt(_cursorIndexOfSteps);
            final float _tmpDistanceKm;
            _tmpDistanceKm = _cursor.getFloat(_cursorIndexOfDistanceKm);
            final float _tmpCalories;
            _tmpCalories = _cursor.getFloat(_cursorIndexOfCalories);
            final int _tmpActiveMinutes;
            _tmpActiveMinutes = _cursor.getInt(_cursorIndexOfActiveMinutes);
            _item = new DailySummaryEntity(_tmpDateEpochDay,_tmpSteps,_tmpDistanceKm,_tmpCalories,_tmpActiveMinutes);
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
  public Object getAllSummaries(final Continuation<? super List<DailySummaryEntity>> $completion) {
    final String _sql = "SELECT * FROM daily_summary";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DailySummaryEntity>>() {
      @Override
      @NonNull
      public List<DailySummaryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDateEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "dateEpochDay");
          final int _cursorIndexOfSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "steps");
          final int _cursorIndexOfDistanceKm = CursorUtil.getColumnIndexOrThrow(_cursor, "distanceKm");
          final int _cursorIndexOfCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "calories");
          final int _cursorIndexOfActiveMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "activeMinutes");
          final List<DailySummaryEntity> _result = new ArrayList<DailySummaryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailySummaryEntity _item;
            final long _tmpDateEpochDay;
            _tmpDateEpochDay = _cursor.getLong(_cursorIndexOfDateEpochDay);
            final int _tmpSteps;
            _tmpSteps = _cursor.getInt(_cursorIndexOfSteps);
            final float _tmpDistanceKm;
            _tmpDistanceKm = _cursor.getFloat(_cursorIndexOfDistanceKm);
            final float _tmpCalories;
            _tmpCalories = _cursor.getFloat(_cursorIndexOfCalories);
            final int _tmpActiveMinutes;
            _tmpActiveMinutes = _cursor.getInt(_cursorIndexOfActiveMinutes);
            _item = new DailySummaryEntity(_tmpDateEpochDay,_tmpSteps,_tmpDistanceKm,_tmpCalories,_tmpActiveMinutes);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
