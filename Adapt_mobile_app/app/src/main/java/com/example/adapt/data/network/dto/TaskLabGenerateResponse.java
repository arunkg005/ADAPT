package com.example.adapt.data.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TaskLabGenerateResponse {

    private String message;
    private String guidance;
    private Draft draft;

    public String getMessage() {
        return message;
    }

    public String getGuidance() {
        return guidance;
    }

    public Draft getDraft() {
        return draft;
    }

    public static class Draft {
        private String title;
        private String description;

        @SerializedName("scheduled_time")
        private String scheduledTime;

        @SerializedName("task_type")
        private String taskType;

        @SerializedName("risk_level")
        private String riskLevel;

        private String complexity;

        @SerializedName("template_key")
        private String templateKey;

        private List<TaskPlanStepPayload> steps;

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

        public String getTemplateKey() {
            return templateKey;
        }

        public List<TaskPlanStepPayload> getSteps() {
            return steps;
        }
    }
}
