package com.example.adapt.data.network.dto;

import com.google.gson.annotations.SerializedName;

public class TaskPlanStepPayload {

    @SerializedName("step_order")
    private final int stepOrder;

    @SerializedName("title")
    private final String title;

    @SerializedName("details")
    private final String details;

    @SerializedName("is_required")
    private final boolean isRequired;

    public TaskPlanStepPayload(int stepOrder, String title, String details, boolean isRequired) {
        this.stepOrder = stepOrder;
        this.title = title;
        this.details = details;
        this.isRequired = isRequired;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public String getTitle() {
        return title;
    }

    public String getDetails() {
        return details;
    }

    public boolean isRequired() {
        return isRequired;
    }
}
