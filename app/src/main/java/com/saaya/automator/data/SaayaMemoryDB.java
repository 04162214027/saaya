package com.saaya.automator.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SaayaMemoryDB - Enhanced Database with Analytics
 */
public class SaayaMemoryDB extends SQLiteOpenHelper {

    private static final String TAG = "SaayaMemoryDB";
    private static final String DATABASE_NAME = "saaya_brain.db";
    private static final int DATABASE_VERSION = 2;

    // Table: shadow_logs
    private static final String TABLE_LOGS = "shadow_logs";
    private static final String COL_ID = "id";
    private static final String COL_TIMESTAMP = "timestamp";
    private static final String COL_PACKAGE = "package_name";
    private static final String COL_RECIPIENT = "recipient_name";
    private static final String COL_MESSAGE = "message_text";

    private static SaayaMemoryDB instance;

    private SaayaMemoryDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized SaayaMemoryDB getInstance(Context context) {
        if (instance == null) {
            instance = new SaayaMemoryDB(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_LOGS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_TIMESTAMP + " INTEGER NOT NULL, "
                + COL_PACKAGE + " TEXT NOT NULL, "
                + COL_RECIPIENT + " TEXT, "
                + COL_MESSAGE + " TEXT"
                + ")";
        
        db.execSQL(CREATE_TABLE);
        Log.d(TAG, "Database created successfully");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGS);
        onCreate(db);
    }

    /**
     * Save log entry (called from background thread)
     */
    public synchronized boolean saveLog(long timestamp, String packageName, 
                                       String recipientName, String messageText) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COL_TIMESTAMP, timestamp);
        values.put(COL_PACKAGE, packageName);
        values.put(COL_RECIPIENT, recipientName != null ? recipientName : "Unknown");
        values.put(COL_MESSAGE, messageText != null ? messageText : "");

        long result = db.insert(TABLE_LOGS, null, values);
        
        return result != -1;
    }

    /**
     * Get all logs for history view
     */
    public List<LogEntry> getAllLogs() {
        List<LogEntry> logs = new ArrayList<>();
        
        try {
            SQLiteDatabase db = this.getReadableDatabase();

            String query = "SELECT * FROM " + TABLE_LOGS 
                         + " ORDER BY " + COL_TIMESTAMP + " DESC LIMIT 100";

            Cursor cursor = db.rawQuery(query, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    LogEntry entry = new LogEntry(
                        cursor.getInt(0),
                        cursor.getLong(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4)
                    );
                    logs.add(entry);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting all logs: " + e.getMessage());
        }

        return logs;
    }

    /**
     * ANALYTICS: Get top 5 most used apps
     */
    public List<AppUsage> getTopUsedApps() {
        List<AppUsage> apps = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT " + COL_PACKAGE + ", COUNT(*) as count "
                     + "FROM " + TABLE_LOGS
                     + " GROUP BY " + COL_PACKAGE
                     + " ORDER BY count DESC LIMIT 5";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                String packageName = cursor.getString(0);
                int count = cursor.getInt(1);
                
                // Get friendly name
                String appName = getFriendlyAppName(packageName);
                apps.add(new AppUsage(appName, count));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return apps;
    }

    /**
     * ANALYTICS: Get personality profile
     */
    public Map<String, String> getPersonalityProfile() {
        Map<String, String> profile = new HashMap<>();
        
        // Set defaults first
        profile.put("totalMessages", "0");
        profile.put("writingStyle", "N/A");
        profile.put("avgWords", "0");
        profile.put("peakTime", "N/A");
        profile.put("favApp", "N/A");
        
        try {
            SQLiteDatabase db = this.getReadableDatabase();

            // Total messages
            Cursor countCursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_LOGS, null);
            if (countCursor != null && countCursor.moveToFirst()) {
                profile.put("totalMessages", String.valueOf(countCursor.getInt(0)));
                countCursor.close();
            }

            // Average word count
            Cursor avgCursor = db.rawQuery(
                "SELECT AVG(LENGTH(" + COL_MESSAGE + ") - LENGTH(REPLACE(" + COL_MESSAGE + ", ' ', '')) + 1) "
                + "FROM " + TABLE_LOGS + " WHERE " + COL_MESSAGE + " != ''", null);
            if (avgCursor != null && avgCursor.moveToFirst()) {
                int avgWords = avgCursor.getInt(0);
                profile.put("writingStyle", avgWords > 10 ? "Detailed" : "Short");
                profile.put("avgWords", String.valueOf(avgWords));
                avgCursor.close();
            }

            // Peak activity hour
            Cursor peakCursor = db.rawQuery(
                "SELECT strftime('%H', " + COL_TIMESTAMP + "/1000, 'unixepoch', 'localtime') as hour, "
                + "COUNT(*) as count FROM " + TABLE_LOGS
                + " GROUP BY hour ORDER BY count DESC LIMIT 1", null);
            if (peakCursor != null && peakCursor.moveToFirst()) {
                String hour = peakCursor.getString(0);
                profile.put("peakTime", hour + ":00");
                peakCursor.close();
            }

            // Favorite app
            Cursor favCursor = db.rawQuery(
                "SELECT " + COL_PACKAGE + ", COUNT(*) as count FROM " + TABLE_LOGS
                + " GROUP BY " + COL_PACKAGE + " ORDER BY count DESC LIMIT 1", null);
            if (favCursor != null && favCursor.moveToFirst()) {
                String favApp = getFriendlyAppName(favCursor.getString(0));
                profile.put("favApp", favApp);
                favCursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting personality profile: " + e.getMessage());
        }

        return profile;
    }

    /**
     * Get friendly app name from package
     */
    private String getFriendlyAppName(String packageName) {
        if (packageName.contains("whatsapp")) return "WhatsApp";
        if (packageName.contains("messenger") || packageName.contains("orca")) return "Messenger";
        if (packageName.contains("instagram")) return "Instagram";
        if (packageName.contains("twitter")) return "Twitter";
        if (packageName.contains("snapchat")) return "Snapchat";
        return packageName;
    }

    /**
     * Get total count
     */
    public int getTotalCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_LOGS, null);
        int count = 0;
        
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        
        cursor.close();
        return count;
    }

    /**
     * Clear all logs
     */
    public void clearAllLogs() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LOGS, null, null);
        Log.d(TAG, "All logs cleared");
    }

    /**
     * LogEntry - Data model
     */
    public static class LogEntry {
        public final int id;
        public final long timestamp;
        public final String packageName;
        public final String recipientName;
        public final String messageText;

        public LogEntry(int id, long timestamp, String packageName, 
                       String recipientName, String messageText) {
            this.id = id;
            this.timestamp = timestamp;
            this.packageName = packageName;
            this.recipientName = recipientName;
            this.messageText = messageText;
        }
    }

    /**
     * AppUsage - Analytics model
     */
    public static class AppUsage {
        public final String appName;
        public final int count;

        public AppUsage(String appName, int count) {
            this.appName = appName;
            this.count = count;
        }
    }
}
