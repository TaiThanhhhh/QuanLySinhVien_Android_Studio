package com.example.quanlysinhvien.data.repo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.quanlysinhvien.data.db.DatabaseHelper;
import com.example.quanlysinhvien.data.model.AttendanceSession;

import java.util.ArrayList;
import java.util.List;

public class AttendanceSessionRepository {
    private final DatabaseHelper dbHelper;

    public AttendanceSessionRepository(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context.getApplicationContext());
    }

    public long createSession(long classId, long createdBy) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        long now = System.currentTimeMillis();
        cv.put("class_id", classId);
        cv.put("created_by", createdBy);
        cv.put("start_ts", now);
        cv.put("end_ts", now + (45 * 60 * 1000)); // 45 minutes validity
        cv.put("created_at", now);
        return db.insert("attendance_sessions", null, cv);
    }

    public List<AttendanceSession> getSessionsForClass(long classId) {
        List<AttendanceSession> sessions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query(
                "attendance_sessions",
                null, // all columns
                "class_id = ?",
                new String[] { String.valueOf(classId) },
                null, null, "start_ts DESC")) {
            while (c.moveToNext()) {
                sessions.add(cursorToSession(c));
            }
        }
        return sessions;
    }

    public AttendanceSession getLatestSessionForClass(long classId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query(
                "attendance_sessions",
                null, // all columns
                "class_id = ?",
                new String[] { String.valueOf(classId) },
                null, null, "start_ts DESC", "1")) {
            if (c.moveToFirst()) {
                return cursorToSession(c);
            }
        }
        return null;
    }

    private AttendanceSession cursorToSession(Cursor c) {
        AttendanceSession session = new AttendanceSession();
        session.setId(c.getLong(c.getColumnIndexOrThrow("id")));
        session.setClassId(c.getLong(c.getColumnIndexOrThrow("class_id")));
        session.setStartTime(c.getLong(c.getColumnIndexOrThrow("start_ts")));
        session.setEndTime(c.getLong(c.getColumnIndexOrThrow("end_ts")));
        return session;
    }
}
