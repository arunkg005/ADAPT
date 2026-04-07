package com.example.adapt.data.network.dto;

import com.google.gson.annotations.SerializedName;

public class EvaluateTelemetryRequest {

    @SerializedName("taskContext")
    private final TaskContext taskContext;

    @SerializedName("limitMs")
    private final Long limitMs;

    public EvaluateTelemetryRequest(TaskContext taskContext, Long limitMs) {
        this.taskContext = taskContext;
        this.limitMs = limitMs;
    }

    public static class TaskContext {
        @SerializedName("taskId")
        private final String taskId;

        @SerializedName("taskName")
        private final String taskName;

        @SerializedName("taskType")
        private final String taskType;

        @SerializedName("scheduledStartTime")
        private final Long scheduledStartTime;

        @SerializedName("expectedDurationMs")
        private final Long expectedDurationMs;

        @SerializedName("stepCount")
        private final Integer stepCount;

        @SerializedName("riskLevel")
        private final String riskLevel;

        @SerializedName("complexity")
        private final String complexity;

        public TaskContext(
                String taskId,
                String taskName,
                String taskType,
                Long scheduledStartTime,
                Long expectedDurationMs,
                Integer stepCount,
                String riskLevel,
                String complexity
        ) {
            this.taskId = taskId;
            this.taskName = taskName;
            this.taskType = taskType;
            this.scheduledStartTime = scheduledStartTime;
            this.expectedDurationMs = expectedDurationMs;
            this.stepCount = stepCount;
            this.riskLevel = riskLevel;
            this.complexity = complexity;
        }
    }
}
