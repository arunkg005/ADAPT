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
import com.example.adapt.data.network.dto.TaskLabGenerateRequest;
import com.example.adapt.data.network.dto.TaskLabGenerateResponse;
import com.example.adapt.utils.PrefManager;
import com.google.android.material.appbar.MaterialToolbar;

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

    private static class PlanDraft {
        private final String title;
        private final String description;
        private final String scheduledTime;

        private PlanDraft(String title, String description, String scheduledTime) {
            this.title = title;
            this.description = description;
            this.scheduledTime = scheduledTime;
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

        appendAssistant("Hello, I am your ADAPT care assistant. Ask for routine planning, risk triage, or task design.");

        btnSendPrompt.setOnClickListener(v -> sendPrompt());
        btnQuickMedication.setOnClickListener(v -> sendQuickPrompt("Create a medication reminder plan for a high-risk patient"));
        btnQuickFallRisk.setOnClickListener(v -> sendQuickPrompt("Assess fall-risk action plan for this evening"));
        btnQuickRoutine.setOnClickListener(v -> sendQuickPrompt("Generate a low-complexity morning routine"));
        btnSendToTaskLab.setOnClickListener(v -> sendDraftToTaskLab());

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
        requestDraftFromBackend(prompt);
        etPrompt.setText("");
    }

    private void sendQuickPrompt(String quickPrompt) {
        if (isGenerating) {
            return;
        }

        etPrompt.setText(quickPrompt);
        sendPrompt();
    }

    private void requestDraftFromBackend(String prompt) {
        setGeneratingState(true);

        apiService.generateTaskLabDraft(new TaskLabGenerateRequest(prompt)).enqueue(new Callback<TaskLabGenerateResponse>() {
            @Override
            public void onResponse(Call<TaskLabGenerateResponse> call, Response<TaskLabGenerateResponse> response) {
                setGeneratingState(false);

                TaskLabGenerateResponse payload = response.body();
                if (!response.isSuccessful() || payload == null || payload.getDraft() == null) {
                    applyLocalFallback(prompt);
                    return;
                }

                latestDraft = mapBackendDraft(payload.getDraft(), prompt);
                appendAssistant(buildAssistantMessage(prompt, latestDraft, payload.getGuidance()));
                renderDraftPreview(latestDraft);
            }

            @Override
            public void onFailure(Call<TaskLabGenerateResponse> call, Throwable t) {
                setGeneratingState(false);
                applyLocalFallback(prompt);
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

    private void applyLocalFallback(String prompt) {
        latestDraft = generatePlanDraft(prompt);
        appendAssistant(generateDecisionSupportResponse(prompt, latestDraft) + "\n\nCloud AI unavailable, local fallback used.");
        renderDraftPreview(latestDraft);
    }

    private PlanDraft mapBackendDraft(TaskLabGenerateResponse.Draft draft, String prompt) {
        if (draft == null) {
            return generatePlanDraft(prompt);
        }

        String title = safeValue(draft.getTitle(), "AI Routine Draft");
        String description = safeValue(draft.getDescription(), "Generated by ADAPT cloud AI.");
        String scheduledTime = safeValue(draft.getScheduledTime(), "09:00 AM");

        return new PlanDraft(title, description, scheduledTime);
    }

    private String buildAssistantMessage(String prompt, PlanDraft draft, String guidance) {
        String baseGuidance = guidance == null || guidance.trim().isEmpty()
                ? generateDecisionSupportResponse(prompt, draft)
                : guidance.trim();

        return baseGuidance
                + "\n\nDraft prepared: "
                + draft.title
                + " at "
                + draft.scheduledTime
                + ". Tap Send Draft to Task Lab to publish it later.";
    }

    private String generateDecisionSupportResponse(String prompt, PlanDraft draft) {
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

        return guidance
                + "\n\nDraft prepared: "
                + draft.title
                + " at "
                + draft.scheduledTime
                + ". Tap Send Draft to Task Lab to publish it later.";
    }

    private PlanDraft generatePlanDraft(String prompt) {
        String normalized = prompt == null ? "" : prompt.trim().toLowerCase(Locale.US);

        if (normalized.contains("medication")) {
            return new PlanDraft(
                    "Medication Adherence Round",
                    "Prepare medication kit, complete reminder checks, and confirm hydration after dose.",
                    "08:00 AM"
            );
        }

        if (normalized.contains("fall") || normalized.contains("risk")) {
            return new PlanDraft(
                    "Fall-Risk Safety Sweep",
                    "Run home safety scan, slow mobility warm-up, and caregiver check-in if instability appears.",
                    "07:30 PM"
            );
        }

        if (normalized.contains("routine") || normalized.contains("schedule")) {
            return new PlanDraft(
                    "Morning Stability Routine",
                    "One-step-at-a-time preparation, guided action phase, then completion confirmation.",
                    "08:30 AM"
            );
        }

        if (normalized.contains("alert")) {
            return new PlanDraft(
                    "Alert Response Drill",
                    "Review alert queue, triage by severity, and assign follow-up ownership for unresolved cases.",
                    "02:00 PM"
            );
        }

        return new PlanDraft(
                "Adaptive Care Routine",
                "Blend observation, guided activity, and a short caregiver summary handoff.",
                "10:00 AM"
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

        prefManager.saveTaskLabDraft(latestDraft.title, latestDraft.description, latestDraft.scheduledTime);
        Toast.makeText(this, "Draft sent to Task Lab.", Toast.LENGTH_SHORT).show();
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
