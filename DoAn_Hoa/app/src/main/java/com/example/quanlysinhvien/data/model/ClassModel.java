package com.example.quanlysinhvien.data.model;

import androidx.annotation.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ClassModel {
    private long id;
    private String classCode;
    private String title;
    private String subject;
    private String semester;
    private long teacherId;
    private String teacher;
    private String room;
    private Long startDate;
    private Long endDate;
    private String status; // This can be used for manual override
    private long createdAt;
    private Long updatedAt;

    public enum Status { ONGOING, UPCOMING, FINISHED, LOCKED }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public long getTeacherId() { return teacherId; }
    public void setTeacherId(long teacherId) { this.teacherId = teacherId; }

    public String getTeacher() { return teacher; }
    public void setTeacher(String teacher) { this.teacher = teacher; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public Long getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = convertDateToLong(startDate); }
    public void setStartDate(Long startDate) { this.startDate = startDate; }

    public Long getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = convertDateToLong(endDate); }
    public void setEndDate(Long endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }

    public Status getCalculatedStatus() {
        if ("LOCKED".equalsIgnoreCase(status)) {
            return Status.LOCKED;
        }
        long now = System.currentTimeMillis();
        if (startDate != null && now < startDate) {
            return Status.UPCOMING;
        }
        if (endDate != null && now > endDate) {
            return Status.FINISHED;
        }
        return Status.ONGOING;
    }

    private Long convertDateToLong(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = sdf.parse(dateString);
            return date != null ? date.getTime() : null;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return this.title != null ? this.title : "";
    }
}
