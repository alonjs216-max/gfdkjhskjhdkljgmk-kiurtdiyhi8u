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
import com.xaniihub.app.data.local.entity.GoalEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class GoalDao_Impl implements GoalDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<GoalEntity> __insertionAdapterOfGoalEntity;

  public GoalDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGoalEntity = new EntityInsertionAdapter<GoalEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `goals` (`id`,`daily`,`weekly`,`monthly`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GoalEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getDaily());
        statement.bindLong(3, entity.getWeekly());
        statement.bindLong(4, entity.getMonthly());
      }
    };
  }

  @Override
  public Object upsert(final GoalEntity goal, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfGoalEntity.insert(goal);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<GoalEntity> observeGoal() {
    final String _sql = "SELECT * FROM goals WHERE id = 0 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"goals"}, new Callable<GoalEntity>() {
      @Override
      @Nullable
      public GoalEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDaily = CursorUtil.getColumnIndexOrThrow(_cursor, "daily");
          final int _cursorIndexOfWeekly = CursorUtil.getColumnIndexOrThrow(_cursor, "weekly");
          final int _cursorIndexOfMonthly = CursorUtil.getColumnIndexOrThrow(_cursor, "monthly");
          final GoalEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpDaily;
            _tmpDaily = _cursor.getInt(_cursorIndexOfDaily);
            final int _tmpWeekly;
            _tmpWeekly = _cursor.getInt(_cursorIndexOfWeekly);
            final int _tmpMonthly;
            _tmpMonthly = _cursor.getInt(_cursorIndexOfMonthly);
            _result = new GoalEntity(_tmpId,_tmpDaily,_tmpWeekly,_tmpMonthly);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
