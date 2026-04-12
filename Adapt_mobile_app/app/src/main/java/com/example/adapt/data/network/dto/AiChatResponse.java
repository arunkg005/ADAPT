package com.example.adapt.data.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AiChatResponse {

    @SerializedName("reply")
    private String reply;

    @SerializedName("actionItems")
    private List<String> actionItems;

    @SerializedName("safetyFlags")
    private List<String> safetyFlags;

    @SerializedName("confidence")
    private Double confidence;

    public String getReply() {
        return reply;
    }

    public List<String> getActionItems() {
        return actionItems;
    }

    public List<String> getSafetyFlags() {
        return safetyFlags;
    }

    public Double getConfidence() {
        return confidence;
    }
}
