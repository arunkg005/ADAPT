package com.example.adapt.data.network.dto;

import com.google.gson.annotations.SerializedName;

public class AssistNextActionResponse {

    @SerializedName("actionType")
    private String actionType;

    @SerializedName("uiPrompt")
    private String uiPrompt;

    @SerializedName("voicePrompt")
    private String voicePrompt;

    @SerializedName("assistanceMode")
    private String assistanceMode;

    @SerializedName("escalate")
    private boolean escalate;

    @SerializedName("severity")
    private String severity;

    @SerializedName("alertCreated")
    private boolean alertCreated;

    public String getActionType() {
        return actionType;
    }

    public String getUiPrompt() {
        return uiPrompt;
    }

    public String getVoicePrompt() {
        return voicePrompt;
    }

    public String getAssistanceMode() {
        return assistanceMode;
    }

    public boolean isEscalate() {
        return escalate;
    }

    public String getSeverity() {
        return severity;
    }

    public boolean isAlertCreated() {
        return alertCreated;
    }
}
