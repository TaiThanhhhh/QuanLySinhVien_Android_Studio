package com.example.quanlysinhvien.auth;

import com.example.quanlysinhvien.data.model.User;

public class AuthResult {
    public enum Status { SUCCESS, INVALID_CREDENTIALS, DEVICE_MISMATCH, NEEDS_PASSWORD_RESET }

    private final Status status;
    private final User user;
    private final String token;

    public AuthResult(Status status, User user, String token) {
        this.status = status;
        this.user = user;
        this.token = token;
    }

    public AuthResult(Status status, User user) {
        this(status, user, null);
    }

    public Status getStatus() { return status; }
    public User getUser() { return user; }
    public String getToken() { return token; }
}
