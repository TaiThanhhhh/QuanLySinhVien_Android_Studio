package com.example.quanlysinhvien.data.repo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.quanlysinhvien.data.db.DatabaseHelper;
import com.example.quanlysinhvien.data.model.User;

import java.util.ArrayList;
import java.util.List;

public class EnrollmentRepository {
    private final DatabaseHelper dbHelper;
    private final AuditRepository auditRepo;

    public EnrollmentRepository(Context ctx) {
        this.dbHelper = DatabaseHelper.getInstance(ctx.getApplicationContext());
        this.auditRepo = new AuditRepository(ctx);
    }

    public boolean isStudentEnrolled(long classId, long studentId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                "class_students",
                new String[]{"class_id"},
                "class_id = ? AND student_id = ?",
                new String[]{String.valueOf(classId), String.valueOf(studentId)},
                null, null, null, "1")) {
            return cursor.getCount() > 0;
        }
    }

    public boolean enrollStudent(long classId, long studentId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("class_id", classId);
        cv.put("student_id", studentId);
        cv.put("added_at", System.currentTimeMillis());
        long id = db.insertWithOnConflict("class_students", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        if (id != -1) auditRepo.log("ENROLL", null, "class="+classId, "student="+studentId);
        return id != -1;
    }

    public boolean removeStudent(long classId, long studentId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete("class_students", "class_id = ? AND student_id = ?", new String[]{String.valueOf(classId), String.valueOf(studentId)});
        if (rows > 0) auditRepo.log("UNENROLL", null, "class="+classId, "student="+studentId);
        return rows > 0;
    }

    public void updateEnrolledStudents(long classId, List<User> students) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // First, remove all existing students from the class
            db.delete("class_students", "class_id = ?", new String[]{String.valueOf(classId)});

            // Then, add the new list of students
            for (User student : students) {
                ContentValues cv = new ContentValues();
                cv.put("class_id", classId);
                cv.put("student_id", student.getId());
                cv.put("added_at", System.currentTimeMillis());
                db.insert("class_students", null, cv);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<User> getStudentsInClass(long classId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT u.* FROM users u JOIN class_students cs ON u.id = cs.student_id WHERE cs.class_id = ? ORDER BY u.mssv";
        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(classId)});
        List<User> list = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                User u = new User();
                u.setId(c.getLong(c.getColumnIndexOrThrow("id")));
                int idx = c.getColumnIndex("mssv"); if (idx != -1) u.setMssv(c.getString(idx));
                u.setName(c.getString(c.getColumnIndexOrThrow("name")));
                u.setRole(c.getString(c.getColumnIndexOrThrow("role")));
                int didx = c.getColumnIndex("device_id"); if (didx != -1) u.setDeviceId(c.getString(didx));
                u.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
                list.add(u);
            }
        } finally {
            c.close();
        }
        return list;
    }
}
