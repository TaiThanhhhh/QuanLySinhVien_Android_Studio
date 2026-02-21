package com.example.quanlysinhvien.data.repo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.quanlysinhvien.data.db.DatabaseHelper;
import com.example.quanlysinhvien.data.model.Attendance;
import com.example.quanlysinhvien.data.model.AttendanceRecord;
import com.example.quanlysinhvien.data.model.AttendanceStatus;
import com.example.quanlysinhvien.data.model.StatusCount;
import com.example.quanlysinhvien.data.model.TimestampCount;
import com.example.quanlysinhvien.data.model.User;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AttendanceRepository {
    private final DatabaseHelper dbHelper;

    public AttendanceRepository(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context.getApplicationContext());
    }

    public long recordAttendance(Attendance attendance) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("session_id", attendance.getSessionId());
        cv.put("student_id", attendance.getStudentId());
        cv.put("timestamp", attendance.getTimestamp());
        cv.put("status", attendance.getStatus());
        cv.put("created_at", System.currentTimeMillis());
        cv.put("recorded_by", attendance.getStudentId()); // Self-recorded

        try {
            JSONObject locationJson = new JSONObject();
            locationJson.put("latitude", attendance.getLatitude());
            locationJson.put("longitude", attendance.getLongitude());
            cv.put("remark", locationJson.toString());
        } catch (Exception e) {
            // Don't add remark if JSON fails
        }

        return db.insertWithOnConflict("attendance_records", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public boolean hasStudentAttended(long sessionId, long studentId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query("attendance_records", new String[]{"id"}, "session_id = ? AND student_id = ?", new String[]{String.valueOf(sessionId), String.valueOf(studentId)}, null, null, null, "1")) {
            return cursor.getCount() > 0;
        }
    }

    public List<AttendanceRecord> getAttendanceHistoryForStudent(long studentId) {
        List<AttendanceRecord> records = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT c.title as class_title, a.timestamp, a.status FROM attendance_records a JOIN attendance_sessions s ON a.session_id = s.id JOIN classes c ON s.class_id = c.id WHERE a.student_id = ? ORDER BY a.timestamp DESC";
        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(studentId)})) {
            while (cursor.moveToNext()) {
                AttendanceRecord record = new AttendanceRecord();
                record.setClassTitle(cursor.getString(cursor.getColumnIndexOrThrow("class_title")));
                record.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")));
                record.setStatus(cursor.getString(cursor.getColumnIndexOrThrow("status")));
                records.add(record);
            }
        }
        return records;
    }

    public List<AttendanceStatus> getAttendanceStatusForSession(long sessionId, long classId) {
        List<AttendanceStatus> statuses = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT u.id, u.name, u.mssv, ar.status, ar.timestamp FROM class_students cs JOIN users u ON cs.student_id = u.id LEFT JOIN attendance_records ar ON u.id = ar.student_id AND ar.session_id = ? WHERE cs.class_id = ? ORDER BY u.name";
        try (Cursor c = db.rawQuery(query, new String[]{String.valueOf(sessionId), String.valueOf(classId)})) {
            while (c.moveToNext()) {
                User student = new User();
                student.setId(c.getLong(c.getColumnIndexOrThrow("id")));
                student.setName(c.getString(c.getColumnIndexOrThrow("name")));
                student.setMssv(c.getString(c.getColumnIndexOrThrow("mssv")));

                String status = c.getString(c.getColumnIndexOrThrow("status"));
                long timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp"));
                if (status == null) {
                    status = "ABSENT";
                }
                statuses.add(new AttendanceStatus(student, status, timestamp));
            }
        }
        return statuses;
    }

    public boolean updateAttendanceStatus(long sessionId, long studentId, String newStatus, long recordedBy) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("session_id", sessionId);
        cv.put("student_id", studentId);
        cv.put("status", newStatus);
        cv.put("timestamp", System.currentTimeMillis());
        cv.put("recorded_by", recordedBy);
        cv.put("created_at", System.currentTimeMillis());

        long result = db.insertWithOnConflict("attendance_records", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }

    public List<StatusCount> getAttendanceStatusCounts(long sessionId) {
        List<StatusCount> counts = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT status, COUNT(*) as count FROM attendance_records WHERE session_id = ? GROUP BY status";
        try (Cursor c = db.rawQuery(query, new String[]{String.valueOf(sessionId)})) {
            while (c.moveToNext()) {
                counts.add(new StatusCount(c.getString(c.getColumnIndexOrThrow("status")), c.getInt(c.getColumnIndexOrThrow("count"))));
            }
        }
        return counts;
    }

    public List<TimestampCount> getAttendanceTimeline(long sessionId) {
        List<TimestampCount> counts = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT timestamp, COUNT(*) as count FROM attendance_records WHERE session_id = ? GROUP BY strftime('%Y-%m-%d %H:%M', timestamp / 1000, 'unixepoch') ORDER BY timestamp ASC";
        try (Cursor c = db.rawQuery(query, new String[]{String.valueOf(sessionId)})) {
            while (c.moveToNext()) {
                counts.add(new TimestampCount(c.getLong(c.getColumnIndexOrThrow("timestamp")), c.getInt(c.getColumnIndexOrThrow("count"))));
            }
        }
        return counts;
    }

    public List<AttendanceRecord> getAttendanceRecords(long sessionId, long classId) {
        List<AttendanceRecord> records = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT u.name, u.mssv, ar.status FROM class_students cs JOIN users u ON cs.student_id = u.id LEFT JOIN attendance_records ar ON u.id = ar.student_id AND ar.session_id = ? WHERE cs.class_id = ?";
        try (Cursor c = db.rawQuery(query, new String[]{String.valueOf(sessionId), String.valueOf(classId)})) {
            while (c.moveToNext()) {
                AttendanceRecord record = new AttendanceRecord();
                record.setClassTitle(c.getString(c.getColumnIndexOrThrow("name"))); // Not right, but for pdf
                // record.setTimestamp(c.getLong(c.getColumnIndexOrThrow("timestamp")));
                record.setStatus(c.getString(c.getColumnIndexOrThrow("status")));
                records.add(record);
            }
        }
        return records;
    }
}
