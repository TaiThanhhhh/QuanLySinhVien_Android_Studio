package com.example.quanlysinhvien.data.model;

public class AttendanceRecord {
    private String classTitle;
    private String subject;
    private String teacherName;
    private long sessionTime;
    private long timestamp;
    private String status;

    // Getters and Setters
    public String getClassTitle() {
        return classTitle;
    }

    public void setClassTitle(String classTitle) {
        this.classTitle = classTitle;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public long getSessionTime() {
        return sessionTime;
    }

    public void setSessionTime(long sessionTime) {
        this.sessionTime = sessionTime;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
