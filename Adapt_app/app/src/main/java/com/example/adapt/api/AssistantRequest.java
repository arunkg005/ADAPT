package com.example.adapt.api;

public class AssistantRequest {
    public int patient_id;
    public String text;
    public String session_id;

    public AssistantRequest(int patient_id, String text, String session_id) {
        this.patient_id = patient_id;
        this.text = text;
        this.session_id = session_id;
    }
}
