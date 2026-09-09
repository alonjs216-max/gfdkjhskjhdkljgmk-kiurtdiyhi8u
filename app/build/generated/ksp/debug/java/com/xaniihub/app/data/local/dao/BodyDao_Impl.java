package com.xaniihub.app.data.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.xaniihub.app.data.local.entity.BodyParamsEntity;
import com.xaniihub.app.data.local.entity.WeightEntryEntity;
import java.lang.Class;
import java.lang.Exception;
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
public final class BodyDao_Impl implements BodyDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<BodyParamsEntity> __insertionAdapterOfBodyParamsEntity;

  private final EntityInsertionAdapter<WeightEntryEntity> __insertionAdapterOfWeightEntryEntity;

  public BodyDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfBodyParamsEntity = new EntityInsertionAdapter<BodyParamsEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `body_params` (`id`,`weightKg`,`heightCm`,`age`,`gender`,`activityMultiplier`,`targetWeightKg`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BodyParamsEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindDouble(2, entity.getWeightKg());
        statement.bindLong(3, entity.getHeightCm());
        statement.bindLong(4, entity.getAge());
        statement.bindString(5, entity.getGender());
        statement.bindDouble(6, entity.getActivityMultiplier());
        statement.bindDouble(7, entity.getTargetWeightKg());
      }
    };
    this.__insertionAdapterOfWeightEntryEntity = new EntityInsertionAdapter<WeightEntryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `weight_entries` (`timestamp`,`weightKg`) VALUES (?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WeightEntryEntity entity) {
        statement.bindLong(1, entity.getTimestamp());
        statement.bindDouble(2, entity.getWeightKg());
      }
    };
  }

  @Override
  public Object upsertBodyParams(final BodyParamsEntity params,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBodyParamsEntity.insert(params);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertWeight(final WeightEntryEntity entry,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfWeightEntryEntity.insert(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<BodyParamsEntity> observeBodyParams() {
    final String _sql = "SELECT * FROM body_params WHERE id = 0 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"body_params"}, new Callable<BodyParamsEntity>() {
      @Override
      @Nullable
      public BodyParamsEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
          final int _cursorIndexOfHeightCm = CursorUtil.getColumnIndexOrThrow(_cursor, "heightCm");
          final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "age");
          final int _cursorIndexOfGender = CursorUtil.getColumnIndexOrThrow(_cursor, "gender");
          final int _cursorIndexOfActivityMultiplier = CursorUtil.getColumnIndexOrThrow(_cursor, "activityMultiplier");
          final int _cursorIndexOfTargetWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "targetWeightKg");
          final BodyParamsEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final float _tmpWeightKg;
            _tmpWeightKg = _cursor.getFloat(_cursorIndexOfWeightKg);
            final int _tmpHeightCm;
            _tmpHeightCm = _cursor.getInt(_cursorIndexOfHeightCm);
            final int _tmpAge;
            _tmpAge = _cursor.getInt(_cursorIndexOfAge);
            final String _tmpGender;
            _tmpGender = _cursor.getString(_cursorIndexOfGender);
            final float _tmpActivityMultiplier;
            _tmpActivityMultiplier = _cursor.getFloat(_cursorIndexOfActivityMultiplier);
            final float _tmpTargetWeightKg;
            _tmpTargetWeightKg = _cursor.getFloat(_cursorIndexOfTargetWeightKg);
            _result = new BodyParamsEntity(_tmpId,_tmpWeightKg,_tmpHeightCm,_tmpAge,_tmpGender,_tmpActivityMultiplier,_tmpTargetWeightKg);
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
  public Flow<List<WeightEntryEntity>> observeWeights() {
    final String _sql = "SELECT * FROM weight_entries ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"weight_entries"}, new Callable<List<WeightEntryEntity>>() {
      @Override
      @NonNull
      public List<WeightEntryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
          final List<WeightEntryEntity> _result = new ArrayList<WeightEntryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WeightEntryEntity _item;
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final float _tmpWeightKg;
            _tmpWeightKg = _cursor.getFloat(_cursorIndexOfWeightKg);
            _item = new WeightEntryEntity(_tmpTimestamp,_tmpWeightKg);
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
