package com.example.adapt.data.network.dto;

import com.google.gson.annotations.SerializedName;

public class TaskLabGenerateRequest {

    public static class PatientProfile {

        private final String id;
        private final String name;

        @SerializedName("risk_level")
        private final String riskLevel;

        public PatientProfile(String id, String name, String riskLevel) {
            this.id = id;
            this.name = name;
            this.riskLevel = riskLevel;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getRiskLevel() {
            return riskLevel;
        }
    }

    public static class Constraints {

        @SerializedName("riskLevel")
        private final String riskLevel;

        public Constraints(String riskLevel) {
            this.riskLevel = riskLevel;
        }

        public String getRiskLevel() {
            return riskLevel;
        }
    }

    @SerializedName("prompt")
    private final String prompt;

    @SerializedName("patientProfile")
    private final PatientProfile patientProfile;

    @SerializedName("constraints")
    private final Constraints constraints;

    public TaskLabGenerateRequest(String prompt) {
        this(prompt, null, null);
    }

    public TaskLabGenerateRequest(String prompt, PatientProfile patientProfile, Constraints constraints) {
        this.prompt = prompt;
        this.patientProfile = patientProfile;
        this.constraints = constraints;
    }

    public String getPrompt() {
        return prompt;
    }

    public PatientProfile getPatientProfile() {
        return patientProfile;
    }

    public Constraints getConstraints() {
        return constraints;
    }
}
