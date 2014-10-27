package com.plusot.senselib.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.SparseArray;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.senselib.values.Device;
import com.plusot.senselib.values.ValueType;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DbData {
    private static final String CLASSTAG = DbData.class.getSimpleName();
    private static final int AVG_COUNT = 10;
    private static DbData instance = null;
    private static Set<User> users = new HashSet<User>();
    private int session = -1;
    private int ride = 0;
    private SQLiteDatabase db;
    private DbHelper dbHelper;
    private Map<String, Integer> totals = new HashMap<String, Integer>();
    private EnumMap<ValueType, Integer> values = new EnumMap<ValueType, Integer>(ValueType.class);
    private EnumMap<ValueType, SparseArray<Double>> vals = new EnumMap<ValueType, SparseArray<Double>>(ValueType.class);
    private EnumMap<ValueType, SparseArray<Double>> valsLast = new EnumMap<ValueType, SparseArray<Double>>(ValueType.class);
    private EnumMap<ValueType, SparseArray<Double>> valsAvg = new EnumMap<ValueType, SparseArray<Double>>(ValueType.class);
    private long valsAvgTime = 0;
    private long valsTime = 0;
    private int valsSession = 0;

    public interface User {

    }

    public enum StoreType {
        DATA,
        AVGDATA,
        DEVINFO,
        ;
        public static StoreType fromInt(final int index) {
            return StoreType.values()[index % StoreType.values().length];
        }
    };

    private DbData() {
        dbHelper = new DbHelper(Globals.appContext, null);
        LLog.v(Globals.TAG, CLASSTAG + ".DbData: Trying to open database");
        if (db == null) db = dbHelper.getWritableDatabase();
        this.getSession();
    }

    public static DbData getInstance() {
        if (instance != null) return instance;
        if (!Globals.runMode.isRun()) return null;
        synchronized (DbData.class) {
            if (instance == null) instance = new DbData();
        }
        return instance;
    }

    private static void stopInstance() {
        if (instance != null) synchronized (DbData.class) {
            if (instance != null) instance.close();
            instance = null;
        }
    }

    public static void addUser(User user) {
        users.add(user);
    }

    public static void removeUser(User user) {
        users.remove(user);
        if (users.size() == 0) {
            LLog.v(Globals.TAG, CLASSTAG + ".DbData: removed last user, closing instance.");
            stopInstance();
        }
    }

    private void close() {
        if (db == null) return;
        db.close();
        db = null;
    }

    private int getSession() {
        if (session > 0) return session;
        if (db == null) return -1;
        Cursor cursor = db.rawQuery(
                "SELECT MAX(" +
                        DbHelper.SESSION_COL_SESSION + ") AS maxsession, MAX(" +
                        DbHelper.SESSION_COL_RIDE + ") AS rides FROM " +
                        DbHelper.SESSION_TABLE,
                null
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                session = cursor.getInt(cursor.getColumnIndex("maxsession"));
                ride = cursor.getInt(cursor.getColumnIndex("rides"));
            }
            cursor.close();
        }
        if (session <= 0) newSession();
        return session;
    }

    public int newSession() {
        //if (session <= 0) getSession();
        long now = System.currentTimeMillis();
        int newSession = (int)(now / 60000L);
        if (newSession == session) return session;
        session = newSession;
        ContentValues values = new ContentValues();
        values.put(DbHelper.SESSION_COL_SESSION, session);
        values.put(DbHelper.SESSION_COL_RIDE, ride++);
        values.put(DbHelper.SESSION_COL_TIME, now);
        long id = db.insert(DbHelper.SESSION_TABLE, null, values);
        return session;
    }

    public void setTotal(ValueType type, final double value, final long time) {
        //if (totals.get()) {
        String qry = "INSERT OR REPLACE INTO " + DbHelper.TOTALS_TABLE + " ( " +
                DbHelper.TOTALS_COL_PARAM + ", " +
                DbHelper.TOTALS_COL_VALUE + ", " +
                DbHelper.TOTALS_COL_TIME + " " +
                ") VALUES ('" +
                type.toDbString() + "', " +
                value + ", " +
                time +
                ")";
        db.execSQL(qry);
        //}
    }

    private JSONObject toJSON(EnumMap<ValueType, SparseArray<Double>> vals) {
        JSONObject json = new JSONObject();
        for (ValueType vt: vals.keySet()) {
            SparseArray<Double> dbls;
            if ((dbls = vals.get(vt)) != null) {
                for (int i = 0; i < dbls.size(); i++) {
                    Double d = dbls.valueAt(i);
                    if (d != Double.NaN) try {
                        if (i > 0)
                            json.put(vt.getShortName().toLowerCase().substring(1) + (i + 1), (int) (d * vt.dbScaler()));
                        else
                            json.put(vt.getShortName().toLowerCase().substring(1), (int) (d * vt.dbScaler()));

                    } catch (JSONException e) {
                        LLog.e(Globals.TAG, CLASSTAG + ".toJSON", e);
                    }
                }
            }
        }
        return json;
    }

    private void clearVals(EnumMap<ValueType, SparseArray<Double>> vals) {
        for (ValueType vt: vals.keySet()) {
            SparseArray<Double> dbls;
            if ((dbls = vals.get(vt)) != null) {
                for (int i = 0; i < dbls.size(); i++) dbls.setValueAt(i, Double.NaN);
            }
        }
    }

    private void reduceVals(EnumMap<ValueType, SparseArray<Double>> vals) {
        for (ValueType vt: vals.keySet()) {
            SparseArray<Double> dbls;
            if ((dbls = vals.get(vt)) != null) {
                Double value;
                for (int i = 0; i < dbls.size(); i++) {
                    value = dbls.valueAt(i);
                    if (value != null && value != Double.NaN)
                        dbls.setValueAt(i, value * 0.9);
                }
            }
        }
    }

    private void reduceAndAddVals(EnumMap<ValueType, SparseArray<Double>> vals, EnumMap<ValueType, SparseArray<Double>> refVals) {
        for (ValueType vt: vals.keySet()) {
            SparseArray<Double> dbls;
            SparseArray<Double> refDbls;
            if ((dbls = vals.get(vt)) != null && (refDbls = refVals.get(vt)) != null) {
                Double value;
                Double refValue;
                for (int i = 0; i < dbls.size(); i++) {
                    value = dbls.valueAt(i);
                    refValue = refDbls.valueAt(i);
                    if (value != null && value != Double.NaN && refValue != null && refValue != Double.NaN)
                        dbls.setValueAt(i, value * 0.9 + refValue * 0.1);
                }
            }
        }
    }

    private void setValsValue(ValueType vt, Device dev, EnumMap<ValueType, SparseArray<Double>> vals, double value) {
        SparseArray<Double> dbls = vals.get(vt);
        if (dbls == null) vals.put(vt, dbls = new SparseArray<Double>());
        dbls.put(dev.getDeviceType().getPriority() * 100 + dev.getCount(), value);
    }

    public void addValue(ValueType vt, Device dev, double value, long time) {
        if (valsSession != session) {
            if (valsSession != 0) {
                JSONObject json = toJSON(vals);
                insertEntry(valsTime, StoreType.DATA, json.toString());
                clearVals(vals);
                json = toJSON(valsAvg);
                insertEntry(valsAvgTime * 10, StoreType.DATA, json.toString());
                clearVals(valsAvg);
                clearVals(valsLast);
            }
            valsSession = session;
        }
        if (time / 1000 > valsTime) {
            if (valsTime != 0) {
                JSONObject json = toJSON(vals);
                insertEntry(valsTime, StoreType.DATA, json.toString());
                clearVals(vals);
            }
            valsTime = time / 1000;
        }
        if (time / (AVG_COUNT * 1000) > valsAvgTime) {
            if (valsAvgTime != 0) {
                JSONObject json = toJSON(valsAvg);
                insertEntry(valsAvgTime * AVG_COUNT, StoreType.DATA, json.toString());
                //clearVals(valsAvg);
            }
            valsAvgTime = time / (AVG_COUNT * 1000);
        }
        setValsValue(vt, dev, vals, value);
        setValsValue(vt, dev, valsLast, value);
        reduceAndAddVals(valsAvg, valsLast);
    }

    private long insertEntry(long time, StoreType type, String data) {
        if (db == null) return -1;
        long id = -1;
        synchronized (this) {
            ContentValues values = new ContentValues();
            values.put(DbHelper.DATA_COL_SESSION, time);
            values.put(DbHelper.DATA_COL_SHARED, 0);
            values.put(DbHelper.DATA_COL_TIME, time);
            values.put(DbHelper.DATA_COL_TYPE, type.ordinal());
            values.put(DbHelper.DATA_COL_DATA, data);
            id = db.insert(DbHelper.DATA_TABLE, null, values);
        }
        return id;
    }

//    public boolean deleteEntry(long rowIndex) {
//        if (db == null) return false;
//        return db.delete(DbHelper.DATA_TABLE, DbHelper.DATA_COL_ID + "=" + rowIndex, null) > 0;
//    }

    public boolean deleteEntries(int session) {
        if (db == null) return false;
        boolean succes = false;
        synchronized (this) {
            String[] args = new String[]{"" + session, "" + StoreType.DATA.ordinal()};
            succes = db.delete(
                    DbHelper.DATA_TABLE,
                    DbHelper.DATA_COL_SESSION + " = ? AND " +
                            DbHelper.DATA_COL_TYPE + " = ?",
                    args
            ) > 0;
        }
        return succes;
    }

    public boolean setShared(int minId, int maxId) {
        if (db == null) return false;
        boolean succes = false;
        synchronized (this) {
            String[] args = new String[]{"" + minId, "" + maxId};
            ContentValues cv = new ContentValues();
            cv.put(DbHelper.DATA_COL_SHARED, "1");
            succes = db.update(
                    DbHelper.DATA_TABLE,
                    cv,
                    DbHelper.DATA_COL_ID + " >= ? AND " + DbHelper.DATA_COL_ID + " <= ?",
                    args
            ) > 0;
        }
        return succes;
    }

    public boolean deleteEntries() {
        if (db == null) return false;
        boolean succes = false;
        synchronized (this) {
            String[] args = new String[]{"" + session};
            succes = db.delete(
                    DbHelper.DATA_TABLE,
                    DbHelper.DATA_COL_SESSION + " != ? AND " +
                            DbHelper.DATA_COL_ARCHIVED + " > 0 AND " +
                            DbHelper.DATA_COL_SHARED + " > 0",
                    null
            ) > 0;
        }
        return succes;
    }

    public DataItems getEntries(int max, int minId) {
        int maxId = -1;
        int realMinId = Integer.MAX_VALUE;
        List<DataItem> items = new ArrayList<DataItem>();
        long maxTime = 0;

        synchronized (this) {

            if (db == null) return null;
            Cursor cursor = db.query(
                    DbHelper.DATA_TABLE,
                    new String[]{DbHelper.DATA_COL_ID, DbHelper.DATA_COL_TYPE, DbHelper.DATA_COL_TIME, DbHelper.DATA_COL_DATA, DbHelper.AVG_COL_SESSION},
                    DbHelper.DATA_COL_ID + " > ? AND " + DbHelper.DATA_COL_SHARED + " = 0",
                    new String[]{"" + minId},
                    null,
                    null,
                    DbHelper.DATA_COL_ID,
                    "" + max);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        int id = cursor.getInt(0);
                        StoreType type = StoreType.fromInt(cursor.getInt(1));
                        realMinId = Math.min(realMinId, id);
                        maxId = Math.max(maxId, id);
                        maxTime = Math.max(maxTime, cursor.getLong(2));
                        String jsonString = cursor.getString(3);
                        int session = cursor.getInt(4);
                        items.add(new DataItem(type, session, jsonString));
                    }
                    while (cursor.moveToNext());
                }
                cursor.close();
            }
        }
        if (maxId == -1) return null;

        return new DataItems(realMinId, maxId, maxTime, items);
    }

    public static class DataItem {
        public final StoreType type;
        public final int session;
        public final String data;
        public DataItem(final StoreType type, final int session, final String data) {
            this.type = type;
            this.session = session;
            this.data = data;
        }
    }

    public static class DataItems {
        public final int maxId;
        public final int minId;
        public final long maxTime;
        public final List<DataItem> data;

        public DataItems(final int minId, final int maxId, final long time, final List<DataItem> data) {
            this.minId = minId;
            this.maxId = maxId;
            this.maxTime = time;
            this.data = data;
        }
    }

}

