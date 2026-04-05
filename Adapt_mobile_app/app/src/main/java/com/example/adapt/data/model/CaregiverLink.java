package com.example.adapt.data.model;

public class CaregiverLink {
    private String caregiverId;
    private String patientId;

    public CaregiverLink(String caregiverId, String patientId) {
        this.caregiverId = caregiverId;
        this.patientId = patientId;
    }

    public String getCaregiverId() { return caregiverId; }
    public void setCaregiverId(String caregiverId) { this.caregiverId = caregiverId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
}
