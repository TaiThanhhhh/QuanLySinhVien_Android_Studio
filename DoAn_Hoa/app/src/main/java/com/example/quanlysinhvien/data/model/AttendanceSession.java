package com.example.quanlysinhvien.data.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AttendanceSession {
    private long id;
    private long classId;
    private long startTime;
    private long endTime;

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getClassId() { return classId; }
    public void setClassId(long classId) { this.classId = classId; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    // Helper to display in Spinner
    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("'Buổi học ngày' dd/MM/yyyy 'lúc' HH:mm", Locale.getDefault());
        return sdf.format(new Date(startTime));
    }
}
