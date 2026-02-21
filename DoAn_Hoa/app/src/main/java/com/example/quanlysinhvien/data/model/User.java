package com.example.quanlysinhvien.data.model;

import android.os.Parcel;
import android.os.Parcelable;

public class User implements Parcelable {
    private long id;
    private String mssv;
    private String name;
    private String role;
    private String passwordHash;
    private boolean passwordNeedsReset;
    private String deviceId;
    private String faceTemplate;
    private long createdAt;
    private Long updatedAt;

    public User() {}

    protected User(Parcel in) {
        id = in.readLong();
        mssv = in.readString();
        name = in.readString();
        role = in.readString();
        passwordHash = in.readString();
        passwordNeedsReset = in.readByte() != 0;
        deviceId = in.readString();
        faceTemplate = in.readString();
        createdAt = in.readLong();
        if (in.readByte() == 0) {
            updatedAt = null;
        } else {
            updatedAt = in.readLong();
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(mssv);
        dest.writeString(name);
        dest.writeString(role);
        dest.writeString(passwordHash);
        dest.writeByte((byte) (passwordNeedsReset ? 1 : 0));
        dest.writeString(deviceId);
        dest.writeString(faceTemplate);
        dest.writeLong(createdAt);
        if (updatedAt == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(updatedAt);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getMssv() { return mssv; }
    public void setMssv(String mssv) { this.mssv = mssv; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isPasswordNeedsReset() { return passwordNeedsReset; }
    public void setPasswordNeedsReset(boolean passwordNeedsReset) { this.passwordNeedsReset = passwordNeedsReset; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getFaceTemplate() { return faceTemplate; }
    public void setFaceTemplate(String faceTemplate) { this.faceTemplate = faceTemplate; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
}
