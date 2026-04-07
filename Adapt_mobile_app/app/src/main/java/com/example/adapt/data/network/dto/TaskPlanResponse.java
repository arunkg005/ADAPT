package com.example.adapt.data.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TaskPlanResponse {

    private String id;
    private String title;
    private String description;

    @SerializedName("scheduled_time")
    private String scheduledTime;

    private String status;
    private String source;

    private List<TaskPlanStepPayload> steps;

    public String getId() {
        return id;
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

    public String getStatus() {
        return status;
    }

    public String getSource() {
        return source;
    }

    public List<TaskPlanStepPayload> getSteps() {
        return steps;
    }
}
