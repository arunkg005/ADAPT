package com.example.adapt.data.network.dto;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

public class BackendAlert {
    private String id;

    @SerializedName("patient_id")
    private String patientId;

    @SerializedName("task_name")
    private String taskName;

    @SerializedName("task_state")
    private String taskState;

    private String severity;

    @SerializedName("primary_issue")
    private String primaryIssue;

    @SerializedName("assistance_mode")
    private String assistanceMode;

    @SerializedName("engine_output")
    private JsonElement engineOutput;

    @SerializedName("is_acknowledged")
    private boolean isAcknowledged;

    @SerializedName("acknowledged_by")
    private String acknowledgedBy;

    @SerializedName("acknowledged_at")
    private String acknowledgedAt;

    @SerializedName("created_at")
    private String createdAt;

    public String getId() {
        return id;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getTaskState() {
        return taskState;
    }

    public String getSeverity() {
        return severity;
    }

    public String getPrimaryIssue() {
        return primaryIssue;
    }

    public String getAssistanceMode() {
        return assistanceMode;
    }

    public JsonElement getEngineOutput() {
        return engineOutput;
    }

    public boolean isAcknowledged() {
        return isAcknowledged;
    }

    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public String getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
