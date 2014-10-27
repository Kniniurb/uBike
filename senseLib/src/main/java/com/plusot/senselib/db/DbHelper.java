package com.plusot.senselib.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;

public class DbHelper extends SQLiteOpenHelper {
    private static final String CLASSTAG = DbHelper.class.getSimpleName();

    public static final String DATA_TABLE = "data";
    public static final String DATA_COL_ID = "id";
    public static final String DATA_COL_SESSION = "session";
    public static final String DATA_COL_TYPE = "datatype";
    public static final String DATA_COL_TIME = "timestamp";
    public static final String DATA_COL_SHARED = "shared";
    public static final String DATA_COL_ARCHIVED = "archived";
    public static final String DATA_COL_DATA = "data";

    public static final String SESSION_TABLE = "sessions";
    public static final String SESSION_COL_ID = "id";
    public static final String SESSION_COL_SESSION = "session";
    public static final String SESSION_COL_RIDE = "ride";
    public static final String SESSION_COL_TIME = "timestamp";

    public static final String TOTALS_TABLE = "totals";
    public static final String TOTALS_COL_TIME = "timestamp";
    public static final String TOTALS_COL_PARAM = "param";
    public static final String TOTALS_COL_VALUE = "value";

    public static final String AVG_TABLE = "avgs";
    public static final String AVG_COL_ID = "id";
    public static final String AVG_COL_SESSION = "session";
    public static final String AVG_COL_TIME = "timestamp";
    public static final String AVG_COL_PARAM = "param";
    public static final String AVG_COL_VALUE = "value";

    public static final String DATABASE_NAME = "sensdata.db";
    public static final int DATABASE_VERSION = 3;


    private static final String CREATE_DATA_TABLE =
            "create table " +
                    DATA_TABLE + " (" +
                    DATA_COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DATA_COL_SESSION + " INTEGER NOT NULL, " +
                    DATA_COL_TYPE + " INTEGER NOT NULL, " +
                    DATA_COL_TIME + " INTEGER NOT NULL, " +
                    DATA_COL_DATA + " TEXT NOT NULL, " +
                    DATA_COL_SHARED + " INTEGER DEFAULT 0, " +
                    DATA_COL_ARCHIVED + " INTEGER DEFAULT 0" +
                    ");";

    private static final String CREATE_SESSION_TABLE =
            "create table " +
                    SESSION_TABLE + " (" +
                    SESSION_COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    SESSION_COL_SESSION + " INTEGER NOT NULL, " +
                    SESSION_COL_RIDE + " INTEGER NOT NULL, " +
                    SESSION_COL_TIME + " INTEGER NOT NULL" +
                    ");";
    private static final String CREATE_TOTALS_TABLE =
            "CREATE TABLE " +
                    TOTALS_TABLE + " (" +
                    TOTALS_COL_PARAM + " TEXT PRIMARY KEY, " +
                    TOTALS_COL_VALUE + " INTEGER DEFAULT 0, " +
                    TOTALS_COL_TIME + " INTEGER DEFAULT 0);" +
                    "CREATE UNIQUE INDEX totals_idx ON " + TOTALS_TABLE + " (" + TOTALS_COL_PARAM + ")";
    private static final String CREATE_AVG_TABLE =
            "CREATE TABLE " +
                    AVG_TABLE + " (" +
                    AVG_COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    AVG_COL_SESSION + " INTEGER NOT NULL, " +
                    AVG_COL_PARAM + " TEXT NOT NULL, " +
                    AVG_COL_VALUE + " INTEGER DEFAULT 0, " +
                    AVG_COL_TIME + " INTEGER DEFAULT 0);" +
                    "CREATE UNIQUE INDEX totals_idx ON " + AVG_TABLE + " (" + AVG_COL_SESSION + ",  " + AVG_COL_PARAM + ")";

    public DbHelper(Context context, CursorFactory factory) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
    }

    // Called when no database exists in disk and the helper class needs
    // to create a new one.
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_DATA_TABLE);
        db.execSQL(CREATE_SESSION_TABLE);
        db.execSQL(CREATE_TOTALS_TABLE);
        db.execSQL(CREATE_AVG_TABLE);
    }

    // Called when there is a database version mismatch meaning that the version
    // of the database on disk needs to be upgraded to the current version.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Log the version upgrade.
        LLog.w(Globals.TAG, CLASSTAG + "onUpgrade: Upgrading from version " +
                oldVersion + " to " +
                newVersion + ", which will destroy all old data");

        // Upgrade the existing database to conform to the new version. Multiple
        // previous versions can be handled by comparing _oldVersion and _newVersion
        // values.

        // The simplest case is to drop the old table and create a new one.
        db.execSQL("DROP TABLE IF EXISTS " + DATA_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + SESSION_TABLE);
        // Create a new one.
        onCreate(db);
    }
}
