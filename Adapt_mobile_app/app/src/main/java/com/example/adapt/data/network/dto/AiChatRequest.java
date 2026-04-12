package com.example.adapt.data.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class AiChatRequest {

    public static class ConversationTurn {

        @SerializedName("role")
        private final String role;

        @SerializedName("content")
        private final String content;

        public ConversationTurn(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }

    @SerializedName("prompt")
    private final String prompt;

    @SerializedName("patientId")
    private final String patientId;

    @SerializedName("roleContext")
    private final String roleContext;

    @SerializedName("conversationHistory")
    private final List<ConversationTurn> conversationHistory;

    public AiChatRequest(String prompt) {
        this(prompt, null, null, null);
    }

    public AiChatRequest(String prompt, String patientId, String roleContext) {
        this(prompt, patientId, roleContext, null);
    }

    public AiChatRequest(
            String prompt,
            String patientId,
            String roleContext,
            List<ConversationTurn> conversationHistory
    ) {
        this.prompt = prompt;
        this.patientId = patientId;
        this.roleContext = roleContext;
        this.conversationHistory = conversationHistory == null
                ? null
                : new ArrayList<>(conversationHistory);
    }

    public String getPrompt() {
        return prompt;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getRoleContext() {
        return roleContext;
    }

    public List<ConversationTurn> getConversationHistory() {
        return conversationHistory;
    }
}
