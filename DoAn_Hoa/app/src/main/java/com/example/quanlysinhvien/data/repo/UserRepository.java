package com.example.quanlysinhvien.data.repo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.quanlysinhvien.data.db.DatabaseHelper;
import com.example.quanlysinhvien.data.model.User;
import com.example.quanlysinhvien.util.HashUtil;

import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    private final DatabaseHelper dbHelper;
    private final AuditRepository auditRepo;
    private static final String TAG = "UserRepository";

    public UserRepository(Context context) {
        Log.d(TAG, "UserRepository constructor");
        this.dbHelper = DatabaseHelper.getInstance(context.getApplicationContext());
        this.auditRepo = new AuditRepository(context);
    }

    public boolean updateFaceTemplate(long userId, String faceTemplate) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("face_template", faceTemplate);
        int rows = db.update("users", cv, "id = ?", new String[]{String.valueOf(userId)});
        return rows > 0;
    }

    public List<User> getAllStudents(String filter) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<User> list = new ArrayList<>();
        String selection = "role = ?";
        String[] selectionArgs = {"STUDENT"};

        if(filter != null && !filter.isEmpty()) {
            selection += " AND (name LIKE ? OR mssv LIKE ?)";
            String likeFilter = "%" + filter + "%";
            selectionArgs = new String[]{"STUDENT", likeFilter, likeFilter};
        }

        try (Cursor c = db.query("users", null, selection, selectionArgs, null, null, "name ASC")) {
            while (c.moveToNext()) {
                list.add(cursorToUser(c));
            }
        }
        return list;
    }

    public boolean updateUser(User u) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", u.getName());
        cv.put("mssv", u.getMssv());
        cv.put("updated_at", System.currentTimeMillis());

        int rows = db.update("users", cv, "id = ?", new String[]{String.valueOf(u.getId())});
        if (rows > 0) auditRepo.log("USER_UPDATE", null, String.valueOf(u.getId()), u.getName());
        return rows > 0;
    }

    public User getUserByMssv(String mssv) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query("users", null, "mssv = ?", new String[]{mssv}, null, null, null)) {
            if (c.moveToFirst()) {
                return cursorToUser(c);
            }
            return null;
        }
    }

    public User getUserById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query("users", null, "id = ?", new String[]{String.valueOf(id)}, null, null, null)) {
            if (c.moveToFirst()) {
                return cursorToUser(c);
            }
            return null;
        }
    }

    public boolean verifyPassword(User u, String password) {
        if (u == null || u.getPasswordHash() == null) return false;
        try {
            return HashUtil.verifyPassword(password, u.getPasswordHash());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean updateDeviceBinding(long userId, String androidId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("device_id", androidId);
        int rows = db.update("users", cv, "id = ?", new String[]{String.valueOf(userId)});
        return rows > 0;
    }
    
    public boolean resetDeviceBinding(long userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.putNull("device_id");
        int rows = db.update("users", cv, "id = ?", new String[]{String.valueOf(userId)});
        if (rows > 0) auditRepo.log("DEVICE_RESET", null, String.valueOf(userId), "device unbound");
        return rows > 0;
    }

    public boolean deleteUser(long userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete("users", "id = ?", new String[]{String.valueOf(userId)});
        if (rows > 0) auditRepo.log("USER_DELETE", null, String.valueOf(userId), "deleted");
        return rows > 0;
    }

    public long createUser(User u, String password) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("mssv", u.getMssv());
        cv.put("name", u.getName());
        cv.put("role", u.getRole());
        try {
            cv.put("password_hash", HashUtil.hashPassword(password));
        } catch (Exception e) {
            return -1;
        }
        cv.put("created_at", System.currentTimeMillis());
        long id = db.insert("users", null, cv);
        if (id != -1) auditRepo.log("USER_CREATE", null, String.valueOf(id), u.getName());
        return id;
    }

    public boolean changePassword(long userId, String newPassword) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        try {
            cv.put("password_hash", HashUtil.hashPassword(newPassword));
        } catch (Exception e) {
            return false;
        }
        cv.put("password_needs_reset", 0);
        int rows = db.update("users", cv, "id = ?", new String[]{String.valueOf(userId)});
        if (rows > 0) auditRepo.log("PASSWORD_CHANGE", userId, null, "changed password");
        return rows > 0;
    }

    public boolean resetPassword(long userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        try {
            cv.put("password_hash", HashUtil.hashPassword("123456")); // Reset to default password
        } catch (Exception e) {
            return false;
        }
        cv.put("password_needs_reset", 1);
        int rows = db.update("users", cv, "id = ?", new String[]{String.valueOf(userId)});
        if (rows > 0) auditRepo.log("PASSWORD_RESET", null, String.valueOf(userId), "password reset");
        return rows > 0;
    }

    public boolean addStudentToClass(long userId, long classId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("student_id", userId);
        cv.put("class_id", classId);
        return db.insert("class_students", null, cv) != -1;
    }

    public int addStudentsToClass(List<User> students, long classId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        int count = 0;
        try {
            for (User student : students) {
                ContentValues cv = new ContentValues();
                cv.put("student_id", student.getId());
                cv.put("class_id", classId);
                if (db.insert("class_students", null, cv) != -1) {
                    count++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return count;
    }

    public boolean removeStudentFromClass(long userId, long classId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete("class_students", "student_id = ? AND class_id = ?", new String[]{String.valueOf(userId), String.valueOf(classId)}) > 0;
    }

    public List<User> getStudentsByClass(long classId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<User> students = new ArrayList<>();
        String query = "SELECT u.* FROM users u INNER JOIN class_students cs ON u.id = cs.student_id WHERE cs.class_id = ?";
        try (Cursor c = db.rawQuery(query, new String[]{String.valueOf(classId)})) {
            while (c.moveToNext()) {
                students.add(cursorToUser(c));
            }
        }
        return students;
    }

    public boolean updateAuthToken(long userId, String token) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("token", token);
        int rows = db.update("users", cv, "id = ?", new String[]{String.valueOf(userId)});
        return rows > 0;
    }

    private User cursorToUser(Cursor c) {
        User user = new User();
        user.setId(c.getLong(c.getColumnIndexOrThrow("id")));
        user.setMssv(c.getString(c.getColumnIndexOrThrow("mssv")));
        user.setName(c.getString(c.getColumnIndexOrThrow("name")));
        user.setRole(c.getString(c.getColumnIndexOrThrow("role")));
        user.setPasswordHash(c.getString(c.getColumnIndexOrThrow("password_hash")));
        user.setPasswordNeedsReset(c.getInt(c.getColumnIndexOrThrow("password_needs_reset")) == 1);
        int deviceIdCol = c.getColumnIndex("device_id");
        if (deviceIdCol != -1 && !c.isNull(deviceIdCol)) {
            user.setDeviceId(c.getString(deviceIdCol));
        }
        int faceCol = c.getColumnIndex("face_template");
        if (faceCol != -1 && !c.isNull(faceCol)) {
            user.setFaceTemplate(c.getString(faceCol));
        }
        int tokenCol = c.getColumnIndex("token");
        if (tokenCol != -1 && !c.isNull(tokenCol)) {
            user.setToken(c.getString(tokenCol));
        }
        return user;
    }
}
