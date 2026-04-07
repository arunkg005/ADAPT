package com.example.adapt.data.network.dto;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

public class EvaluateTelemetryResponse {
    private String message;

    @SerializedName("patientId")
    private String patientId;

    @SerializedName("telemetryCount")
    private int telemetryCount;

    private JsonElement evaluation;

    @SerializedName("alertCreated")
    private boolean alertCreated;

    private BackendAlert alert;

    public String getMessage() {
        return message;
    }

    public String getPatientId() {
        return patientId;
    }

    public int getTelemetryCount() {
        return telemetryCount;
    }

    public JsonElement getEvaluation() {
        return evaluation;
    }

    public boolean isAlertCreated() {
        return alertCreated;
    }

    public BackendAlert getAlert() {
        return alert;
    }
}
