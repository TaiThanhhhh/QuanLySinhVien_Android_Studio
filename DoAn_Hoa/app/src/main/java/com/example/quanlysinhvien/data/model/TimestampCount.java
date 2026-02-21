package com.example.quanlysinhvien.data.model;

public class TimestampCount {
    private long timestamp;
    private int count;

    public TimestampCount(long timestamp, int count) {
        this.timestamp = timestamp;
        this.count = count;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getCount() {
        return count;
    }
}
