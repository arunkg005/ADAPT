package com.example.adapt.ui.assistant;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.adapt.R;
import com.example.adapt.data.network.ApiService;
import com.example.adapt.data.network.NetworkClient;
import com.example.adapt.data.network.dto.AiChatRequest;
import com.example.adapt.data.network.dto.AiChatResponse;
import com.example.adapt.data.network.dto.TaskLabGenerateRequest;
import com.example.adapt.data.network.dto.TaskLabGenerateResponse;
import com.example.adapt.data.network.dto.TaskPlanStepPayload;
import com.example.adapt.utils.PrefManager;
import com.google.gson.Gson;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiAssistantActivity extends AppCompatActivity {

    private TextView tvConversation;
    private EditText etPrompt;
    private TextView tvGeneratedPlanTitle;
    private TextView tvGeneratedPlanDescription;
    private TextView tvGeneratedPlanTime;
    private Button btnSendPrompt;
    private Button btnSendToTaskLab;
    private PrefManager prefManager;
    private ApiService apiService;
    private PlanDraft latestDraft;
    private boolean isGenerating;
    private final List<AiChatRequest.ConversationTurn> conversationHistory = new ArrayList<>();
    private final Gson gson = new Gson();

    private static class PlanDraft {
        private final String title;
        private final String description;
        private final String scheduledTime;
        private final String taskType;
        private final String riskLevel;
        private final String complexity;
        private final String templateKey;
        private final List<TaskPlanStepPayload> steps;

        private PlanDraft(
                String title,
                String description,
                String scheduledTime,
                String taskType,
                String riskLevel,
                String complexity,
                String templateKey,
                List<TaskPlanStepPayload> steps
        ) {
            this.title = title;
            this.description = description;
            this.scheduledTime = scheduledTime;
            this.taskType = taskType;
            this.riskLevel = riskLevel;
            this.complexity = complexity;
            this.templateKey = templateKey;
            this.steps = steps == null ? new ArrayList<>() : new ArrayList<>(steps);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_assistant);

        prefManager = new PrefManager(this);
        apiService = NetworkClient.getRetrofit(this).create(ApiService.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbarAiAssistant);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        tvConversation = findViewById(R.id.tvConversation);
        etPrompt = findViewById(R.id.etAssistantPrompt);
        tvGeneratedPlanTitle = findViewById(R.id.tvGeneratedPlanTitle);
        tvGeneratedPlanDescription = findViewById(R.id.tvGeneratedPlanDescription);
        tvGeneratedPlanTime = findViewById(R.id.tvGeneratedPlanTime);
        btnSendPrompt = findViewById(R.id.btnSendPrompt);
        btnSendToTaskLab = findViewById(R.id.btnSendToTaskLab);

        Button btnQuickMedication = findViewById(R.id.btnQuickMedication);
        Button btnQuickFallRisk = findViewById(R.id.btnQuickFallRisk);
        Button btnQuickRoutine = findViewById(R.id.btnQuickRoutine);

        String greeting = "Hello, I am your ADAPT care assistant. Ask for routine planning, risk triage, or task design.";
        appendAssistant(greeting);
        rememberTurn("assistant", greeting);

        btnSendPrompt.setOnClickListener(v -> sendPrompt());
        btnQuickMedication.setOnClickListener(v -> sendQuickPrompt("Create a medication reminder plan for a high-risk patient"));
        btnQuickFallRisk.setOnClickListener(v -> sendQuickPrompt("Assess fall-risk action plan for this evening"));
        btnQuickRoutine.setOnClickListener(v -> sendQuickPrompt("Generate a low-complexity morning routine"));
        btnSendToTaskLab.setOnClickListener(v -> sendDraftToTaskLab());

        com.google.android.material.button.MaterialButton btnVoiceHandsFree = findViewById(R.id.btnVoiceHandsFree);
        if (btnVoiceHandsFree != null) {
            btnVoiceHandsFree.setOnClickListener(v -> Toast.makeText(this, "Voice capture will connect to your device speech service.", Toast.LENGTH_SHORT).show());
        }

        renderEmptyDraftState();
    }

    private void sendPrompt() {
        if (isGenerating) {
            return;
        }

        String prompt = etPrompt.getText() == null ? "" : etPrompt.getText().toString().trim();
        if (TextUtils.isEmpty(prompt)) {
            Toast.makeText(this, "Type a prompt first", Toast.LENGTH_SHORT).show();
            return;
        }

        appendCaretaker(prompt);
        List<AiChatRequest.ConversationTurn> historySnapshot = snapshotConversationHistory();
        rememberTurn("user", prompt);
        requestAssistantReplyAndDraft(prompt, historySnapshot);
        etPrompt.setText("");
    }

    private void sendQuickPrompt(String quickPrompt) {
        if (isGenerating) {
            return;
        }

        etPrompt.setText(quickPrompt);
        sendPrompt();
    }

    private void requestAssistantReplyAndDraft(String prompt, List<AiChatRequest.ConversationTurn> historySnapshot) {
        setGeneratingState(true);

        String patientId = safeValue(prefManager.getActivePatientId(), "");
        String riskLevel = safeValue(prefManager.getActivePatientRisk(), "MEDIUM");
        AiChatRequest request = new AiChatRequest(
                prompt,
                patientId.isEmpty() ? null : patientId,
                "CAREGIVER_MOBILE_" + riskLevel.toUpperCase(Locale.US),
                historySnapshot
        );
        apiService.chatWithAssistant(request).enqueue(new Callback<AiChatResponse>() {
            @Override
            public void onResponse(Call<AiChatResponse> call, Response<AiChatResponse> response) {
                String chatGuidance = null;
                if (response.isSuccessful() && response.body() != null) {
                    chatGuidance = buildChatGuidance(response.body(), prompt);
                }

                requestDraftFromBackend(prompt, chatGuidance);
            }

            @Override
            public void onFailure(Call<AiChatResponse> call, Throwable t) {
                requestDraftFromBackend(prompt, null);
            }
        });
    }

    private void requestDraftFromBackend(String prompt, String chatGuidance) {
        String patientId = safeValue(prefManager.getActivePatientId(), "");
        String patientName = safeValue(prefManager.getActivePatientName(), "Patient");
        String riskLevel = safeValue(prefManager.getActivePatientRisk(), "MEDIUM");

        TaskLabGenerateRequest.PatientProfile patientProfile = patientId.isEmpty()
                ? null
                : new TaskLabGenerateRequest.PatientProfile(patientId, patientName, riskLevel);
        TaskLabGenerateRequest.Constraints constraints = new TaskLabGenerateRequest.Constraints(riskLevel);

        TaskLabGenerateRequest request = new TaskLabGenerateRequest(prompt, patientProfile, constraints);

        apiService.generateTaskLabDraft(request).enqueue(new Callback<TaskLabGenerateResponse>() {
            @Override
            public void onResponse(Call<TaskLabGenerateResponse> call, Response<TaskLabGenerateResponse> response) {
                setGeneratingState(false);

                TaskLabGenerateResponse payload = response.body();
                if (!response.isSuccessful() || payload == null || payload.getDraft() == null) {
                    applyLocalFallback(prompt, chatGuidance);
                    return;
                }

                latestDraft = mapBackendDraft(payload.getDraft(), prompt);
                String assistantMessage = buildAssistantMessage(prompt, latestDraft, payload.getGuidance(), chatGuidance);
                appendAssistant(assistantMessage);
                rememberTurn("assistant", assistantMessage);
                renderDraftPreview(latestDraft);
            }

            @Override
            public void onFailure(Call<TaskLabGenerateResponse> call, Throwable t) {
                setGeneratingState(false);
                applyLocalFallback(prompt, chatGuidance);
            }
        });
    }

    private void setGeneratingState(boolean generating) {
        isGenerating = generating;
        if (btnSendPrompt != null) {
            btnSendPrompt.setEnabled(!generating);
            btnSendPrompt.setText(generating ? "Generating..." : "Send");
        }
    }

    private void applyLocalFallback(String prompt, String chatGuidance) {
        latestDraft = generatePlanDraft(prompt);
        String assistantMessage = buildAssistantMessage(prompt, latestDraft, null, chatGuidance)
                + "\n\nCloud draft unavailable, local fallback used.";
        appendAssistant(assistantMessage);
        rememberTurn("assistant", assistantMessage);
        renderDraftPreview(latestDraft);
    }

    private PlanDraft mapBackendDraft(TaskLabGenerateResponse.Draft draft, String prompt) {
        if (draft == null) {
            return generatePlanDraft(prompt);
        }

        String title = safeValue(draft.getTitle(), "AI Routine Draft");
        String description = safeValue(draft.getDescription(), "Generated by ADAPT cloud AI.");
        String scheduledTime = safeValue(draft.getScheduledTime(), "09:00 AM");
        String taskType = safeValue(draft.getTaskType(), inferTaskTypeFromPrompt(prompt));
        String riskLevel = safeValue(draft.getRiskLevel(), safeValue(prefManager.getActivePatientRisk(), "MEDIUM"));
        String complexity = safeValue(draft.getComplexity(), "MEDIUM");
        String templateKey = safeValue(draft.getTemplateKey(), null);
        List<TaskPlanStepPayload> steps = normalizeSteps(draft.getSteps(), prompt);

        return new PlanDraft(
            title,
            description,
            scheduledTime,
            taskType,
            riskLevel,
            complexity,
            templateKey,
            steps
        );
    }

    private String buildAssistantMessage(String prompt, PlanDraft draft, String draftGuidance, String chatGuidance) {
        String normalizedChatGuidance = chatGuidance == null ? "" : chatGuidance.trim();
        String normalizedDraftGuidance = draftGuidance == null ? "" : draftGuidance.trim();
        String fallbackGuidance = generateDecisionSupportGuidance(prompt);

        StringBuilder builder = new StringBuilder();
        if (!normalizedChatGuidance.isEmpty()) {
            builder.append(normalizedChatGuidance);
        } else {
            builder.append(fallbackGuidance);
        }

        if (!normalizedDraftGuidance.isEmpty() && !normalizedDraftGuidance.equalsIgnoreCase(normalizedChatGuidance)) {
            builder.append("\n\n").append(normalizedDraftGuidance);
        }

        builder.append("\n\nDraft prepared: ")
                .append(draft.title)
                .append(" at ")
                .append(draft.scheduledTime)
                .append(". Tap Send Draft to Task Lab to publish it later.");

        return builder.toString();
    }

    private String buildChatGuidance(AiChatResponse response, String prompt) {
        if (response == null) {
            return generateDecisionSupportGuidance(prompt);
        }

        String reply = safeValue(response.getReply(), generateDecisionSupportGuidance(prompt));
        StringBuilder builder = new StringBuilder(reply);

        List<String> actionItems = response.getActionItems();
        if (actionItems != null && !actionItems.isEmpty()) {
            builder.append("\n\nAction items:");
            int count = Math.min(actionItems.size(), 3);
            for (int i = 0; i < count; i++) {
                String item = actionItems.get(i);
                if (item != null && !item.trim().isEmpty()) {
                    builder.append("\n- ").append(item.trim());
                }
            }
        }

        List<String> safetyFlags = response.getSafetyFlags();
        if (safetyFlags != null && !safetyFlags.isEmpty()) {
            builder.append("\n\nSafety flags: ").append(TextUtils.join(", ", safetyFlags));
        }

        Double confidence = response.getConfidence();
        if (confidence != null && !confidence.isNaN() && !confidence.isInfinite()) {
            int percentage = (int) Math.round(Math.max(0d, Math.min(1d, confidence)) * 100d);
            builder.append("\n\nConfidence: ").append(percentage).append("%");
        }

        return builder.toString();
    }

    private String generateDecisionSupportGuidance(String prompt) {
        String normalized = prompt.toLowerCase(Locale.US);
        String guidance;

        if (normalized.contains("medication")) {
            guidance = "Suggested protocol: set three reminders, enforce acknowledgement, and escalate to caregiver alert after two misses.";
        } else if (normalized.contains("fall") || normalized.contains("risk")) {
            guidance = "Recommended action: increase check cadence to 15 minutes, keep assist mode active, and monitor inactivity plus gait deviations.";
        } else if (normalized.contains("routine") || normalized.contains("schedule")) {
            guidance = "Draft plan: prepare, execute, and confirm phases with 5-minute buffers. Start with low-complexity tasks and adaptive prompts.";
        } else if (normalized.contains("alert")) {
            guidance = "Guidance: classify severity, acknowledge within SLA, and attach patient context before closing the alert.";
        } else {
            guidance = "I can help with caregiver decisions, routine planning, and risk actions. Try asking for medication, fall-risk, or schedule planning.";
        }

        return guidance;
    }

    private PlanDraft generatePlanDraft(String prompt) {
        String normalized = prompt == null ? "" : prompt.trim().toLowerCase(Locale.US);

        if (normalized.contains("medication")) {
            return new PlanDraft(
                    "Medication Adherence Round",
                    "Prepare medication kit, complete reminder checks, and confirm hydration after dose.",
                    "08:00 AM",
                    "MEDICATION",
                    "HIGH",
                    "LOW",
                    null,
                    buildFallbackSteps("MEDICATION")
            );
        }

        if (normalized.contains("fall") || normalized.contains("risk")) {
            return new PlanDraft(
                    "Fall-Risk Safety Sweep",
                    "Run home safety scan, slow mobility warm-up, and caregiver check-in if instability appears.",
                    "07:30 PM",
                    "EXERCISE",
                    "HIGH",
                    "MEDIUM",
                    null,
                    buildFallbackSteps("EXERCISE")
            );
        }

        if (normalized.contains("routine") || normalized.contains("schedule")) {
            return new PlanDraft(
                    "Morning Stability Routine",
                    "One-step-at-a-time preparation, guided action phase, then completion confirmation.",
                    "08:30 AM",
                    "OTHER",
                    safeValue(prefManager.getActivePatientRisk(), "MEDIUM"),
                    "LOW",
                    null,
                    buildFallbackSteps("OTHER")
            );
        }

        if (normalized.contains("alert")) {
            return new PlanDraft(
                    "Alert Response Drill",
                    "Review alert queue, triage by severity, and assign follow-up ownership for unresolved cases.",
                    "02:00 PM",
                    "OTHER",
                    "MEDIUM",
                    "MEDIUM",
                    null,
                    buildFallbackSteps("OTHER")
            );
        }

        return new PlanDraft(
                "Adaptive Care Routine",
                "Blend observation, guided activity, and a short caregiver summary handoff.",
                "10:00 AM",
                "OTHER",
                safeValue(prefManager.getActivePatientRisk(), "MEDIUM"),
                "MEDIUM",
                null,
                buildFallbackSteps("OTHER")
        );
    }

    private void renderDraftPreview(PlanDraft draft) {
        if (draft == null) {
            renderEmptyDraftState();
            return;
        }

        tvGeneratedPlanTitle.setText(draft.title);
        tvGeneratedPlanDescription.setText(draft.description);
        tvGeneratedPlanTime.setText("Suggested time: " + draft.scheduledTime);
        btnSendToTaskLab.setEnabled(true);
    }

    private void renderEmptyDraftState() {
        tvGeneratedPlanTitle.setText("No draft yet");
        tvGeneratedPlanDescription.setText("Send a prompt and the assistant will produce a Task Lab draft.");
        tvGeneratedPlanTime.setText("Suggested time: --");
        btnSendToTaskLab.setEnabled(false);
    }

    private void sendDraftToTaskLab() {
        if (latestDraft == null) {
            Toast.makeText(this, "Generate a plan draft first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String serializedSteps = gson.toJson(latestDraft.steps);
        prefManager.saveTaskLabDraft(
                latestDraft.title,
                latestDraft.description,
                latestDraft.scheduledTime,
                latestDraft.taskType,
                latestDraft.riskLevel,
                latestDraft.complexity,
                latestDraft.templateKey,
                serializedSteps
        );

        if (safeValue(prefManager.getActivePatientId(), "").isEmpty()) {
            Toast.makeText(this, "Draft saved locally. Select an active patient to sync with cloud from Task Lab.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Draft sent to Task Lab.", Toast.LENGTH_SHORT).show();
        }
    }

    private void rememberTurn(String role, String content) {
        if (content == null || content.trim().isEmpty()) {
            return;
        }

        conversationHistory.add(new AiChatRequest.ConversationTurn(role, content.trim()));
        trimConversationHistory();
    }

    private List<AiChatRequest.ConversationTurn> snapshotConversationHistory() {
        return new ArrayList<>(conversationHistory);
    }

    private void trimConversationHistory() {
        final int maxTurns = 12;
        while (conversationHistory.size() > maxTurns) {
            conversationHistory.remove(0);
        }
    }

    private List<TaskPlanStepPayload> normalizeSteps(List<TaskPlanStepPayload> incoming, String prompt) {
        if (incoming == null || incoming.isEmpty()) {
            return buildFallbackSteps(inferTaskTypeFromPrompt(prompt));
        }

        List<TaskPlanStepPayload> steps = new ArrayList<>();
        for (int i = 0; i < incoming.size(); i++) {
            TaskPlanStepPayload step = incoming.get(i);
            if (step == null || safeValue(step.getTitle(), "").isEmpty()) {
                continue;
            }

            int order = step.getStepOrder() > 0 ? step.getStepOrder() : i + 1;
            steps.add(new TaskPlanStepPayload(
                    order,
                    safeValue(step.getTitle(), "Step " + (i + 1)),
                    safeValue(step.getDetails(), ""),
                    step.isRequired()
            ));
        }

        if (steps.isEmpty()) {
            return buildFallbackSteps(inferTaskTypeFromPrompt(prompt));
        }

        return steps;
    }

    private List<TaskPlanStepPayload> buildFallbackSteps(String taskType) {
        List<TaskPlanStepPayload> steps = new ArrayList<>();
        String normalizedType = safeValue(taskType, "OTHER").toUpperCase(Locale.US);

        if ("MEDICATION".equals(normalizedType)) {
            steps.add(new TaskPlanStepPayload(1, "Prepare Dose", "Arrange medication and hydration before reminder.", true));
            steps.add(new TaskPlanStepPayload(2, "Guided Intake", "Give one instruction at a time and confirm intake.", true));
            steps.add(new TaskPlanStepPayload(3, "Log Outcome", "Record adherence and any side effects.", true));
            return steps;
        }

        if ("EXERCISE".equals(normalizedType)) {
            steps.add(new TaskPlanStepPayload(1, "Safety Sweep", "Clear hazards and verify support aids.", true));
            steps.add(new TaskPlanStepPayload(2, "Guided Movement", "Complete slow, supervised movement blocks.", true));
            steps.add(new TaskPlanStepPayload(3, "Stability Check", "Confirm comfort before ending routine.", true));
            return steps;
        }

        steps.add(new TaskPlanStepPayload(1, "Prepare", "Set the environment and review instructions.", true));
        steps.add(new TaskPlanStepPayload(2, "Execute", "Perform the core activity with adaptive guidance.", true));
        steps.add(new TaskPlanStepPayload(3, "Confirm", "Capture completion and next-step notes.", true));
        return steps;
    }

    private String inferTaskTypeFromPrompt(String prompt) {
        String normalized = safeValue(prompt, "").toLowerCase(Locale.US);

        if (normalized.contains("medication") || normalized.contains("dose") || normalized.contains("pill")) {
            return "MEDICATION";
        }

        if (normalized.contains("hygiene") || normalized.contains("wash") || normalized.contains("bath")) {
            return "HYGIENE";
        }

        if (normalized.contains("meal") || normalized.contains("nutrition") || normalized.contains("breakfast") || normalized.contains("dinner")) {
            return "MEAL";
        }

        if (normalized.contains("fall") || normalized.contains("mobility") || normalized.contains("exercise")) {
            return "EXERCISE";
        }

        if (normalized.contains("social") || normalized.contains("engagement") || normalized.contains("family")) {
            return "SOCIAL";
        }

        return "OTHER";
    }

    private String safeValue(String value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private void appendAssistant(String message) {
        String existing = tvConversation.getText() == null ? "" : tvConversation.getText().toString();
        String next = existing + "\n\nAssistant:\n" + message;
        tvConversation.setText(next.trim());
    }

    private void appendCaretaker(String message) {
        String existing = tvConversation.getText() == null ? "" : tvConversation.getText().toString();
        String next = existing + "\n\nCaretaker:\n" + message;
        tvConversation.setText(next.trim());
    }
}
