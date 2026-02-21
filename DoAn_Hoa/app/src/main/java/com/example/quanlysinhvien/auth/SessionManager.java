package com.example.quanlysinhvien.auth;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF = "attendance_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_ROLE = "role";

    private final SharedPreferences prefs;

    public SessionManager(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void saveSession(long userId, String role) {
        prefs.edit().putLong(KEY_USER_ID, userId).putString(KEY_ROLE, role).apply();
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1);
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, null);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}

