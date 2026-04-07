package com.example.adapt.data.network.dto;

import com.google.gson.annotations.SerializedName;

public class AlertCreateRequest {

    @SerializedName("patient_id")
    private final String patientId;

    @SerializedName("task_name")
    private final String taskName;

    @SerializedName("task_state")
    private final String taskState;

    private final String severity;

    @SerializedName("primary_issue")
    private final String primaryIssue;

    @SerializedName("assistance_mode")
    private final String assistanceMode;

    public AlertCreateRequest(
            String patientId,
            String taskName,
            String taskState,
            String severity,
            String primaryIssue,
            String assistanceMode
    ) {
        this.patientId = patientId;
        this.taskName = taskName;
        this.taskState = taskState;
        this.severity = severity;
        this.primaryIssue = primaryIssue;
        this.assistanceMode = assistanceMode;
    }
}
