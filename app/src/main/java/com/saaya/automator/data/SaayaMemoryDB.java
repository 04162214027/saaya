package com.saaya.automator.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * SaayaMemoryDB - Local SQLite Database for Pattern Learning
 * Stores user interactions and context to build a learning memory.
 */
public class SaayaMemoryDB extends SQLiteOpenHelper {

    private static final String TAG = "SaayaMemoryDB";
    private static final String DATABASE_NAME = "saaya_brain.db";
    private static final int DATABASE_VERSION = 1;

    // Table: shadow_logs
    private static final String TABLE_SHADOW_LOGS = "shadow_logs";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_PACKAGE = "package";
    private static final String COLUMN_CONTEXT_TEXT = "context_text";
    private static final String COLUMN_USER_REPLY = "user_reply";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    // Singleton instance
    private static SaayaMemoryDB instance;

    private SaayaMemoryDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Get singleton instance of SaayaMemoryDB
     */
    public static synchronized SaayaMemoryDB getInstance(Context context) {
        if (instance == null) {
            instance = new SaayaMemoryDB(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_SHADOW_LOGS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_PACKAGE + " TEXT NOT NULL, "
                + COLUMN_CONTEXT_TEXT + " TEXT, "
                + COLUMN_USER_REPLY + " TEXT, "
                + COLUMN_TIMESTAMP + " INTEGER NOT NULL"
                + ")";
        
        db.execSQL(CREATE_TABLE);
        Log.d(TAG, "Database created successfully");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SHADOW_LOGS);
        onCreate(db);
    }

    /**
     * Learn a new pattern by storing user interaction
     * 
     * @param packageName The app package where interaction occurred
     * @param contextText The text content from the screen
     * @param userReply   The user's action or input
     * @return true if successfully stored, false otherwise
     */
    public boolean learnPattern(String packageName, String contextText, String userReply) {
        if (packageName == null || packageName.isEmpty()) {
            Log.w(TAG, "Cannot learn pattern: package name is empty");
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_PACKAGE, packageName);
        values.put(COLUMN_CONTEXT_TEXT, contextText != null ? contextText : "");
        values.put(COLUMN_USER_REPLY, userReply != null ? userReply : "");
        values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());

        long result = db.insert(TABLE_SHADOW_LOGS, null, values);
        
        if (result != -1) {
            Log.d(TAG, "Pattern learned for package: " + packageName);
            return true;
        } else {
            Log.e(TAG, "Failed to learn pattern");
            return false;
        }
    }

    /**
     * Recall similar patterns based on context
     * Uses simple text matching to find similar past interactions
     * 
     * @param context The current context to match against
     * @return List of matching user replies from past interactions
     */
    public List<String> recallPattern(String context) {
        List<String> suggestions = new ArrayList<>();
        
        if (context == null || context.isEmpty()) {
            return suggestions;
        }

        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_USER_REPLY + " FROM " + TABLE_SHADOW_LOGS
                + " WHERE " + COLUMN_CONTEXT_TEXT + " LIKE ? "
                + " ORDER BY " + COLUMN_TIMESTAMP + " DESC LIMIT 10";

        Cursor cursor = db.rawQuery(query, new String[]{"%" + context + "%"});

        if (cursor.moveToFirst()) {
            do {
                String reply = cursor.getString(0);
                if (reply != null && !reply.isEmpty() && !suggestions.contains(reply)) {
                    suggestions.add(reply);
                }
            } while (cursor.moveToNext());
        }

        cursor.close();
        Log.d(TAG, "Recalled " + suggestions.size() + " patterns for context");
        return suggestions;
    }

    /**
     * Get all patterns for a specific app package
     * 
     * @param packageName The package name to query
     * @return List of PatternEntry objects
     */
    public List<PatternEntry> getPatternsByPackage(String packageName) {
        List<PatternEntry> patterns = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_SHADOW_LOGS
                + " WHERE " + COLUMN_PACKAGE + " = ? "
                + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";

        Cursor cursor = db.rawQuery(query, new String[]{packageName});

        if (cursor.moveToFirst()) {
            do {
                PatternEntry entry = new PatternEntry(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getLong(4)
                );
                patterns.add(entry);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return patterns;
    }

    /**
     * Get total count of learned patterns
     */
    public int getTotalPatternCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SHADOW_LOGS, null);
        int count = 0;
        
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        
        cursor.close();
        return count;
    }

    /**
     * Clear all learned patterns (for reset/debugging)
     */
    public void clearAllPatterns() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SHADOW_LOGS, null, null);
        Log.d(TAG, "All patterns cleared");
    }

    /**
     * PatternEntry - Data model for database records
     */
    public static class PatternEntry {
        public final int id;
        public final String packageName;
        public final String contextText;
        public final String userReply;
        public final long timestamp;

        public PatternEntry(int id, String packageName, String contextText, 
                          String userReply, long timestamp) {
            this.id = id;
            this.packageName = packageName;
            this.contextText = contextText;
            this.userReply = userReply;
            this.timestamp = timestamp;
        }
    }
}
