package com.example.quanlysinhvien.data.model;

public class AttendanceStatus {
    private User student;
    private String status; // ON_TIME, LATE, ABSENT
    private long timestamp;

    public AttendanceStatus(User student, String status, long timestamp) {
        this.student = student;
        this.status = status;
        this.timestamp = timestamp;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
