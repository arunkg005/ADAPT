package com.example.adapt.data.network.dto;

import com.google.gson.annotations.SerializedName;

public class BackendDevice {
    private String id;

    @SerializedName("patient_id")
    private String patientId;

    @SerializedName("device_name")
    private String deviceName;

    @SerializedName("device_type")
    private String deviceType;

    private String os;

    @SerializedName("is_online")
    private boolean isOnline;

    @SerializedName("last_seen_at")
    private String lastSeenAt;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    public String getId() {
        return id;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public String getOs() {
        return os;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public String getLastSeenAt() {
        return lastSeenAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
