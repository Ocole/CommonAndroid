package common.db.dao;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.util.ArrayList;

import common.db.DbHelper;
import common.db.DbManager.DbParams;
import common.db.DbManager.DbUpdateListener;
import common.db.TablesManager;
import common.db.entity.ColumnEntity;
import common.db.entity.TableEntity;
import common.utils.LogUtils;

/**
 * 描述:
 * Created by mjd on 2017/1/9.
 */
public class DbDao {

    private static final String TAG = DbDao.class.getSimpleName();

    private DbHelper mDbHelper;
    private SQLiteDatabase mDb;

    public DbDao(Context context, DbParams params, DbUpdateListener dbUpdateListener) {
        this.mDbHelper = new DbHelper(context, params.dbName, null, params.dbVersion, dbUpdateListener);
    }

    /**
     * INSERT, UPDATE, DELETE
     */
    public void execute(String sql, String[] bindArgs) throws Exception {
        LogUtils.i(TAG, "准备执行SQL[ " + sql + " ]语句");
        mDb = mDbHelper.getWritableDatabase();
        if (mDb.isOpen()) {
            if (!TextUtils.isEmpty(sql)) {
                if (bindArgs != null) {
                    mDb.execSQL(sql, bindArgs);
                } else {
                    mDb.execSQL(sql);
                }
                LogUtils.i(TAG, "执行完毕！");
            }
        } else {
            throw new Exception("数据库未打开！");
        }
    }

    public long insert(Object entity) {
        mDb = mDbHelper.getWritableDatabase();
        if (mDb.isOpen()) {
            TableEntity tableEntity = TablesManager.getInstance().find(entity.getClass());
            long result = mDb.insert(tableEntity.getTableName(), null, tableEntity.createContentValues(entity));
            mDb.close();
            return result;
        } else {
            LogUtils.e(TAG, "数据库未打开！");
            return -1;
        }
    }

    public boolean delete(Class<?> tableClazz, String whereClause, String[] whereArgs) {
        mDb = mDbHelper.getWritableDatabase();
        if (mDb.isOpen()) {
            TableEntity tableEntity = TablesManager.getInstance().find(tableClazz);
            boolean result = mDb.delete(tableEntity.getTableName(), whereClause, whereArgs) > 0;
            mDb.close();
            return result;
        } else {
            LogUtils.e(TAG, "数据库未打开！");
            return false;
        }
    }

    public long update(Class<?> tableClazz, Object entity,
                       String whereClause, String[] whereArgs) {
        mDb = mDbHelper.getWritableDatabase();
        if (mDb.isOpen()) {
            TableEntity tableEntity = TablesManager.getInstance().find(tableClazz);
            int result = mDb.update(tableEntity.getTableName(), tableEntity.createContentValues(entity),
                    whereClause, whereArgs);
            mDb.close();
            return result;
        } else {
            LogUtils.e(TAG, "数据库未打开！");
            return -1;
        }
    }

    public <T> ArrayList<T> query(boolean distinct,
                                  Class<?> tableClazz, String[] columns, String selection,
                                  String[] selectionArgs, String groupBy, String having,
                                  String orderBy, String limit) {
        mDb = mDbHelper.getReadableDatabase();
        if (!mDb.isOpen()) {
            LogUtils.e(TAG, "数据库未打开！");
            return null;
        }

        TableEntity tableEntity = TablesManager.getInstance().find(tableClazz);
        // 获取结果集
        Cursor cursor = mDb.query(distinct,
                tableEntity.getTableName(), columns, selection,
                selectionArgs, groupBy, having, orderBy, limit);

        ArrayList<T> arrayList = new ArrayList<>();

        int columnCount = cursor.getColumnCount();
        try {
            while (cursor.moveToNext()) {
                T entity = (T) tableClazz.newInstance();
                for (int i = 0; i < columnCount; i++) {
                    ColumnEntity columnEntity = tableEntity.getField(cursor.getColumnName(i));

                    Object value;
                    if (columnEntity.getType().equals(String.class)) {
                        value = cursor.getString(i);
                    } else if (columnEntity.getType().equals(short.class)) {
                        value = cursor.getShort(i);
                    } else if (columnEntity.getType().equals(long.class)) {
                        value = cursor.getLong(i);
                    } else if (columnEntity.getType().equals(float.class)) {
                        value = cursor.getFloat(i);
                    } else if (columnEntity.getType().equals(double.class)) {
                        value = cursor.getDouble(i);
                    } else if (columnEntity.getType().equals(boolean.class)) {
                        value = cursor.getInt(i);
                    } else {
                        value = cursor.getInt(i);
                    }
                    columnEntity.setValue(entity, value);
                }
                arrayList.add(entity);
            }
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage());
        } finally {
            cursor.close();
            mDb.close();
        }
        return arrayList;
    }

}