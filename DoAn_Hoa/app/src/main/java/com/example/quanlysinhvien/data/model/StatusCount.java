package com.example.quanlysinhvien.data.model;

public class StatusCount {
    private String status;
    private int count;

    public StatusCount(String status, int count) {
        this.status = status;
        this.count = count;
    }

    public String getStatus() {
        return status;
    }

    public int getCount() {
        return count;
    }
}
