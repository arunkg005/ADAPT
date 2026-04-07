package com.example.adapt.data.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TaskPlanCreateRequest {

    @SerializedName("patient_id")
    private final String patientId;

    private final String title;
    private final String description;

    @SerializedName("scheduled_time")
    private final String scheduledTime;

    @SerializedName("task_type")
    private final String taskType;

    @SerializedName("risk_level")
    private final String riskLevel;

    private final String complexity;
    private final String status;
    private final String source;

    @SerializedName("template_key")
    private final String templateKey;

    private final List<TaskPlanStepPayload> steps;

    public TaskPlanCreateRequest(
            String patientId,
            String title,
            String description,
            String scheduledTime,
            String taskType,
            String riskLevel,
            String complexity,
            String status,
            String source,
            String templateKey,
            List<TaskPlanStepPayload> steps
    ) {
        this.patientId = patientId;
        this.title = title;
        this.description = description;
        this.scheduledTime = scheduledTime;
        this.taskType = taskType;
        this.riskLevel = riskLevel;
        this.complexity = complexity;
        this.status = status;
        this.source = source;
        this.templateKey = templateKey;
        this.steps = steps;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getScheduledTime() {
        return scheduledTime;
    }

    public String getTaskType() {
        return taskType;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getComplexity() {
        return complexity;
    }

    public String getStatus() {
        return status;
    }

    public String getSource() {
        return source;
    }

    public String getTemplateKey() {
        return templateKey;
    }

    public List<TaskPlanStepPayload> getSteps() {
        return steps;
    }
}
