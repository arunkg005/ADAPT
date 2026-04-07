package com.example.adapt.data.network.dto;

import com.google.gson.annotations.SerializedName;

public class AssistNextActionRequest {

    @SerializedName("patientId")
    private final String patientId;

    @SerializedName("currentTask")
    private final CurrentTask currentTask;

    @SerializedName("createAlert")
    private final boolean createAlert;

    public AssistNextActionRequest(String patientId, CurrentTask currentTask, boolean createAlert) {
        this.patientId = patientId;
        this.currentTask = currentTask;
        this.createAlert = createAlert;
    }

    public String getPatientId() {
        return patientId;
    }

    public CurrentTask getCurrentTask() {
        return currentTask;
    }

    public boolean isCreateAlert() {
        return createAlert;
    }

    public static class CurrentTask {
        @SerializedName("taskId")
        private final String taskId;

        @SerializedName("taskName")
        private final String taskName;

        @SerializedName("taskType")
        private final String taskType;

        @SerializedName("riskLevel")
        private final String riskLevel;

        @SerializedName("complexity")
        private final String complexity;

        @SerializedName("scheduledStartTime")
        private final long scheduledStartTime;

        @SerializedName("expectedDurationMs")
        private final long expectedDurationMs;

        @SerializedName("stepCount")
        private final int stepCount;

        public CurrentTask(
                String taskId,
                String taskName,
                String taskType,
                String riskLevel,
                String complexity,
                long scheduledStartTime,
                long expectedDurationMs,
                int stepCount
        ) {
            this.taskId = taskId;
            this.taskName = taskName;
            this.taskType = taskType;
            this.riskLevel = riskLevel;
            this.complexity = complexity;
            this.scheduledStartTime = scheduledStartTime;
            this.expectedDurationMs = expectedDurationMs;
            this.stepCount = stepCount;
        }

        public String getTaskId() {
            return taskId;
        }

        public String getTaskName() {
            return taskName;
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

        public long getScheduledStartTime() {
            return scheduledStartTime;
        }

        public long getExpectedDurationMs() {
            return expectedDurationMs;
        }

        public int getStepCount() {
            return stepCount;
        }
    }
}
