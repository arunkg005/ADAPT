package com.example.adapt.data.network.dto;

import com.google.gson.annotations.SerializedName;

public class TelemetryIngestRequest {

    @SerializedName("patient_id")
    private final String patientId;

    @SerializedName("device_id")
    private final String deviceId;

    @SerializedName("signal_type")
    private final String signalType;

    @SerializedName("signal_value")
    private final Object signalValue;

    @SerializedName("timestamp_ms")
    private final long timestampMs;

    public TelemetryIngestRequest(
            String patientId,
            String deviceId,
            String signalType,
            Object signalValue,
            long timestampMs
    ) {
        this.patientId = patientId;
        this.deviceId = deviceId;
        this.signalType = signalType;
        this.signalValue = signalValue;
        this.timestampMs = timestampMs;
    }
}
