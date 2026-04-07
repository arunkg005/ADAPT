package com.example.adapt.data.network.dto;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

public class BackendTelemetry {
    private String id;

    @SerializedName("patient_id")
    private String patientId;

    @SerializedName("device_id")
    private String deviceId;

    @SerializedName("signal_type")
    private String signalType;

    @SerializedName("signal_value")
    private JsonElement signalValue;

    @SerializedName("timestamp_ms")
    private long timestampMs;

    @SerializedName("created_at")
    private String createdAt;

    public String getId() {
        return id;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getSignalType() {
        return signalType;
    }

    public JsonElement getSignalValue() {
        return signalValue;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
