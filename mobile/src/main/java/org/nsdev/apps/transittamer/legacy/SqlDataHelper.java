package org.nsdev.apps.transittamer.legacy;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

public class SqlDataHelper extends OrmLiteSqliteOpenHelper {

    private static final String DATABASE_NAME = "transittamer.db";

    private static final int DATABASE_VERSION = 11;

    private Dao<Stop, Object> stopDao = null;
    private Dao<StopNickname, Object> stopNicknameDao = null;

    static {
        OpenHelperManager.setOpenHelperClass(SqlDataHelper.class);
    }

    public SqlDataHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @SuppressWarnings("hiding")
    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            Log.i(SqlDataHelper.class.getName(), "onCreate");
            TableUtils.createTable(connectionSource, Stop.class);
        } catch (SQLException e) {
            Log.e(SqlDataHelper.class.getName(), "Can't create database", e);
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("hiding")
    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            Log.i(SqlDataHelper.class.getName(), "onUpgrade");

            TableUtils.dropTable(connectionSource, Stop.class, true);

            onCreate(db);
        } catch (SQLException e) {
            Log.e(SqlDataHelper.class.getName(), "Can't drop databases", e);
            throw new RuntimeException(e);
        }
    }

    public Dao<Stop, Object> getStopDao() throws SQLException {
        if (stopDao == null) {
            stopDao = DaoManager.createDao(getConnectionSource(), Stop.class);
        }
        return stopDao;
    }

    public Dao<StopNickname, Object> getStopNicknameDao() throws SQLException {
        if (stopNicknameDao == null) {
            TableUtils.createTableIfNotExists(getConnectionSource(), StopNickname.class);
            stopNicknameDao = DaoManager.createDao(getConnectionSource(), StopNickname.class);
        }
        return stopNicknameDao;
    }

    @Override
    public void close() {
        super.close();
        stopDao = null;
    }

}
