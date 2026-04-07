package com.example.adapt.data.network.dto;

import com.google.gson.annotations.SerializedName;

public class TaskLabGenerateRequest {

    @SerializedName("prompt")
    private final String prompt;

    public TaskLabGenerateRequest(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }
}
