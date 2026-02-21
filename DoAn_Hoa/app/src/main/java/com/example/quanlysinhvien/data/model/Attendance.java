package com.example.quanlysinhvien.data.model;

public class Attendance {
    private long id;
    private long sessionId;
    private long studentId;
    private long timestamp;
    private String status; // e.g., ON_TIME, LATE, ABSENT
    private double latitude;
    private double longitude;

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getSessionId() { return sessionId; }
    public void setSessionId(long sessionId) { this.sessionId = sessionId; }

    public long getStudentId() { return studentId; }
    public void setStudentId(long studentId) { this.studentId = studentId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
