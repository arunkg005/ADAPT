package com.example.adapt.data.network.dto;

import com.google.gson.annotations.SerializedName;

public class PatientCreateRequest {

    @SerializedName("first_name")
    private final String firstName;

    @SerializedName("last_name")
    private final String lastName;

    @SerializedName("cognitive_condition")
    private final String cognitiveCondition;

    @SerializedName("risk_level")
    private final String riskLevel;

    @SerializedName("baseline_response_time_ms")
    private final Integer baselineResponseTimeMs;

    public PatientCreateRequest(
            String firstName,
            String lastName,
            String cognitiveCondition,
            String riskLevel,
            Integer baselineResponseTimeMs
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.cognitiveCondition = cognitiveCondition;
        this.riskLevel = riskLevel;
        this.baselineResponseTimeMs = baselineResponseTimeMs;
    }
}
