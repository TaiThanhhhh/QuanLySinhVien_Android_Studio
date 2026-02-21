package com.example.quanlysinhvien.data.repo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.quanlysinhvien.data.db.DatabaseHelper;
import com.example.quanlysinhvien.data.model.ClassModel;

import java.util.ArrayList;
import java.util.List;

public class ClassRepository {
    private final DatabaseHelper dbHelper;

    public ClassRepository(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context.getApplicationContext());
    }

    public long createClass(ClassModel classModel) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("title", classModel.getTitle());
        cv.put("subject", classModel.getSubject());
        cv.put("teacher_id", classModel.getTeacherId());
        cv.put("teacher_name", classModel.getTeacher());
        cv.put("semester", classModel.getSemester());
        cv.put("room", classModel.getRoom());
        cv.put("start_date", classModel.getStartDate());
        cv.put("end_date", classModel.getEndDate());
        cv.put("class_code", classModel.getClassCode());
        cv.put("created_at", System.currentTimeMillis());
        return db.insert("classes", null, cv);
    }

    public boolean updateClass(ClassModel classModel) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("title", classModel.getTitle());
        cv.put("subject", classModel.getSubject());
        cv.put("teacher_name", classModel.getTeacher());
        cv.put("semester", classModel.getSemester());
        cv.put("room", classModel.getRoom());
        cv.put("start_date", classModel.getStartDate());
        cv.put("end_date", classModel.getEndDate());
        int rows = db.update("classes", cv, "id = ?", new String[]{String.valueOf(classModel.getId())});
        return rows > 0;
    }

    public boolean deleteClass(long classId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // Also delete related enrollments and attendance sessions
        db.delete("class_students", "class_id = ?", new String[]{String.valueOf(classId)});
        db.delete("attendance_sessions", "class_id = ?", new String[]{String.valueOf(classId)});
        int rows = db.delete("classes", "id = ?", new String[]{String.valueOf(classId)});
        return rows > 0;
    }

    public ClassModel getClassById(long classId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query("classes", null, "id = ?", new String[]{String.valueOf(classId)}, null, null, null)) {
            if (c.moveToFirst()) {
                return cursorToClassModel(c);
            }
        }
        return null;
    }

    public List<ClassModel> listClasses(String filter) {
        List<ClassModel> classList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = null;
        String[] selectionArgs = null;
        if (filter != null && !filter.isEmpty()) {
            selection = "title LIKE ? OR subject LIKE ?";
            selectionArgs = new String[]{"%" + filter + "%", "%" + filter + "%"};
        }
        try (Cursor c = db.query("classes", null, selection, selectionArgs, null, null, "created_at DESC")) {
            while (c.moveToNext()) {
                classList.add(cursorToClassModel(c));
            }
        }
        return classList;
    }

    private ClassModel cursorToClassModel(Cursor c) {
        ClassModel model = new ClassModel();
        model.setId(c.getLong(c.getColumnIndexOrThrow("id")));
        model.setTitle(c.getString(c.getColumnIndexOrThrow("title")));
        model.setSubject(c.getString(c.getColumnIndexOrThrow("subject")));
        model.setTeacherId(c.getLong(c.getColumnIndexOrThrow("teacher_id")));
        model.setTeacher(c.getString(c.getColumnIndexOrThrow("teacher_name")));
        model.setSemester(c.getString(c.getColumnIndexOrThrow("semester")));
        model.setRoom(c.getString(c.getColumnIndexOrThrow("room")));
        model.setStartDate(c.getLong(c.getColumnIndexOrThrow("start_date")));
        model.setEndDate(c.getLong(c.getColumnIndexOrThrow("end_date")));
        model.setClassCode(c.getString(c.getColumnIndexOrThrow("class_code")));
        return model;
    }
}
