package com.example.adapt.data.network.dto;

import com.google.gson.annotations.SerializedName;

public class BackendPatient {
    private String id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("first_name")
    private String firstName;

    @SerializedName("last_name")
    private String lastName;

    @SerializedName("date_of_birth")
    private String dateOfBirth;

    @SerializedName("cognitive_condition")
    private String cognitiveCondition;

    @SerializedName("risk_level")
    private String riskLevel;

    @SerializedName("baseline_response_time_ms")
    private Integer baselineResponseTimeMs;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getCognitiveCondition() {
        return cognitiveCondition;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public Integer getBaselineResponseTimeMs() {
        return baselineResponseTimeMs;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
