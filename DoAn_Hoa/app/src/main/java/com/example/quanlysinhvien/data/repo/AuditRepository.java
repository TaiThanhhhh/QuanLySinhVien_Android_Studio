package com.example.quanlysinhvien.data.repo;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.quanlysinhvien.data.db.DatabaseHelper;

public class AuditRepository {
    private final DatabaseHelper dbHelper;

    public AuditRepository(Context ctx) {
        this.dbHelper = DatabaseHelper.getInstance(ctx.getApplicationContext());
    }

    public void log(String type, Long userId, String target, String detail) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("type", type);
        if (userId != null) cv.put("user_id", userId);
        cv.put("target", target);
        cv.put("detail", detail);
        cv.put("ts", System.currentTimeMillis());
        db.insert("audit_logs", null, cv);
    }
}

