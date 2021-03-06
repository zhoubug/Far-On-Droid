package com.openfarmanager.android.core;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.openfarmanager.android.App;
import com.openfarmanager.android.core.dbadapters.BookmarkDBAdapter;
import com.openfarmanager.android.core.dbadapters.NetworkAccountDbAdapter;

public class DataStorageHelper {

    private static final String DB_NAME = "far_on_droid_database";
    private static final int DB_VERSION = 2;

    public static DatabaseHelper sHelper;

    static {
        sHelper = new DatabaseHelper();
    }

    private DataStorageHelper() {}

    public static SQLiteDatabase getDatabase() {
        if(sHelper == null) return null;
        return sHelper.getDatabase();
    }

    public static void closeDatabase() {
        if(sHelper == null) return;
        sHelper.closeDatabase();
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        private final Object mLocker = new Object();
        private SQLiteDatabase mDatabase = null;
        private int mDbOpensCounter = 0;

        public DatabaseHelper() {
            super(App.sInstance, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) throws SQLiteException {
            db.execSQL(BookmarkDBAdapter.CREATE_TABLE);
            db.execSQL(NetworkAccountDbAdapter.CREATE_TABLE);
            Log.d("DataStorageHelper", "Create database, version " + DB_VERSION);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) throws SQLiteException {
            switch (oldVersion) {
                case 1:
                    db.execSQL(NetworkAccountDbAdapter.CREATE_TABLE);
                    Log.d("DataStorageHelper", "Update database from version " + oldVersion + " to " + newVersion);
                    break;
            }
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
            Log.e("DataStorageHelper", String.format("onDowngrade invoked.  OldVersion=%s newVersion=%s", oldVersion, newVersion));
        }

        public SQLiteDatabase getDatabase() {
            synchronized (mLocker) {
                if(mDatabase == null) {
                    try {
                        mDatabase = getWritableDatabase();
                        mDatabase.setLockingEnabled(true);
                        mDbOpensCounter = 0;
                    }
                    catch(SQLiteException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
                mDbOpensCounter++;
                return mDatabase;
            }
        }

        public void closeDatabase() {
            synchronized (mLocker) {
                if(--mDbOpensCounter > 0 || mDatabase == null) return;
                if(mDatabase.isOpen()) mDatabase.close();
                mDatabase = null;
            }
        }

        private void clearDatabase() {
            SQLiteDatabase db = getDatabase();
            if (db == null) return;
            try {
                db.delete(BookmarkDBAdapter.TABLE_NAME, null, null);
                db.delete(NetworkAccountDbAdapter.TABLE_NAME, null, null);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeDatabase();
            }
        }
    }
}
