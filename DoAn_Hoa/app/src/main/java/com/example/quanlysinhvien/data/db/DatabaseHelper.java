package com.example.quanlysinhvien.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.quanlysinhvien.util.HashUtil;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "attendance.db";
    private static final int DATABASE_VERSION = 4; // Incremented version

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d(TAG, "DatabaseHelper constructor");
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating database tables...");

        // users table with face_template
        db.execSQL("CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "mssv TEXT UNIQUE,"
                + "name TEXT NOT NULL,"
                + "role TEXT NOT NULL CHECK(role IN ('ADMIN','STUDENT')) ,"
                + "password_hash TEXT NOT NULL,"
                + "password_needs_reset INTEGER NOT NULL DEFAULT 1,"
                + "device_id TEXT,"
                + "face_template TEXT,"
                + "created_at INTEGER NOT NULL,"
                + "updated_at INTEGER"
                + ");");

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_users_mssv ON users(mssv);");

        // classes table
        db.execSQL("CREATE TABLE IF NOT EXISTS classes ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "class_code TEXT UNIQUE NOT NULL,"
                + "title TEXT NOT NULL,"
                + "subject TEXT,"
                + "semester TEXT,"
                + "teacher_id INTEGER,"
                + "teacher_name TEXT,"
                + "room TEXT,"
                + "start_date INTEGER,"
                + "end_date INTEGER,"
                + "status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','LOCKED','FINISHED','NOT_STARTED')) ,"
                + "created_at INTEGER NOT NULL,"
                + "updated_at INTEGER,"
                + "FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE SET NULL"
                + ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS class_students ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "class_id INTEGER NOT NULL,"
                + "student_id INTEGER NOT NULL,"
                + "added_at INTEGER NOT NULL,"
                + "UNIQUE(class_id, student_id),"
                + "FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,"
                + "FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE"
                + ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS attendance_sessions ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "class_id INTEGER NOT NULL,"
                + "start_ts INTEGER NOT NULL,"
                + "end_ts INTEGER NOT NULL,"
                + "created_by INTEGER NOT NULL,"
                + "created_at INTEGER NOT NULL,"
                + "nonce TEXT,"
                + "FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,"
                + "FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL"
                + ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS attendance_records ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "session_id INTEGER NOT NULL,"
                + "student_id INTEGER NOT NULL,"
                + "timestamp INTEGER NOT NULL,"
                + "status TEXT NOT NULL CHECK(status IN ('ON_TIME','LATE','ABSENT','EXCUSED')) ,"
                + "remark TEXT,"
                + "recorded_by INTEGER,"
                + "created_at INTEGER NOT NULL,"
                + "UNIQUE(session_id, student_id),"
                + "FOREIGN KEY (session_id) REFERENCES attendance_sessions(id) ON DELETE CASCADE,"
                + "FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,"
                + "FOREIGN KEY (recorded_by) REFERENCES users(id) ON DELETE SET NULL"
                + ");");

        // audit_logs table
        db.execSQL("CREATE TABLE IF NOT EXISTS audit_logs ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "type TEXT NOT NULL,"
                + "user_id INTEGER,"
                + "target TEXT,"
                + "detail TEXT,"
                + "ts INTEGER NOT NULL"
                + ");");

        seedAdminUser(db);
        Log.d(TAG, "Finished creating database tables.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from " + oldVersion + " to " + newVersion);
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE users ADD COLUMN face_template TEXT;");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE classes ADD COLUMN teacher_name TEXT;");
        }
        if (oldVersion < 4) {
            db.execSQL("CREATE TABLE IF NOT EXISTS audit_logs ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "type TEXT NOT NULL,"
                    + "user_id INTEGER,"
                    + "target TEXT,"
                    + "detail TEXT,"
                    + "ts INTEGER NOT NULL"
                    + ");");
        }
        Log.w(TAG, "Finished upgrading database.");
    }

    private void seedAdminUser(SQLiteDatabase db) {
        Cursor c = db.rawQuery("SELECT COUNT(*) as cnt FROM users", null);
        try {
            if (c.moveToFirst() && c.getInt(0) == 0) {
                 try {
                    String hash = HashUtil.hashPassword("admin123");
                    ContentValues cv = new ContentValues();
                    cv.put("mssv", "admin");
                    cv.put("name", "Administrator");
                    cv.put("role", "ADMIN");
                    cv.put("password_hash", hash);
                    cv.put("password_needs_reset", 0);
                    cv.put("created_at", System.currentTimeMillis());
                    db.insert("users", null, cv);
                    Log.i(TAG, "Seeded default admin (mssv=admin, pass=admin123)");
                } catch (Exception ex) {
                    Log.e(TAG, "Failed to seed admin: " + ex.getMessage());
                }
            }
        } finally {
            c.close();
        }
    }
}
