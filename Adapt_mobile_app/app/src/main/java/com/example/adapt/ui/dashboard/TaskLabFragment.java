package com.example.adapt.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.adapt.R;
import com.example.adapt.data.network.ApiService;
import com.example.adapt.data.network.NetworkClient;
import com.example.adapt.data.network.dto.TaskPlanCreateRequest;
import com.example.adapt.data.network.dto.TaskPlanResponse;
import com.example.adapt.data.network.dto.TaskPlanStepPayload;
import com.example.adapt.ui.assistant.AiAssistantActivity;
import com.example.adapt.utils.PrefManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.example.adapt.viewmodel.RoutineViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TaskLabFragment extends Fragment {

    private RoutineViewModel viewModel;
    private PrefManager prefManager;
    private ApiService apiService;
    private View cardImportedDraft;
    private TextView tvImportedDraftTitle;
    private TextView tvImportedDraftDescription;
    private TextView tvImportedDraftTime;
    private final Gson gson = new Gson();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_lab, container, false);

        viewModel = new ViewModelProvider(this).get(RoutineViewModel.class);
        prefManager = new PrefManager(requireContext());
        apiService = NetworkClient.getRetrofit(requireContext()).create(ApiService.class);

        Button btnTemplateMedication = view.findViewById(R.id.btnTemplateMedication);
        Button btnTemplateDailyCare = view.findViewById(R.id.btnTemplateDailyCare);
        Button btnTemplateWellness = view.findViewById(R.id.btnTemplateWellness);
        Button btnOpenAiPlanner = view.findViewById(R.id.btnOpenAiPlanner);
        Button btnPublishImportedDraft = view.findViewById(R.id.btnPublishImportedDraft);
        Button btnDiscardImportedDraft = view.findViewById(R.id.btnDiscardImportedDraft);

        cardImportedDraft = view.findViewById(R.id.cardImportedDraft);
        tvImportedDraftTitle = view.findViewById(R.id.tvImportedDraftTitle);
        tvImportedDraftDescription = view.findViewById(R.id.tvImportedDraftDescription);
        tvImportedDraftTime = view.findViewById(R.id.tvImportedDraftTime);

        btnTemplateMedication.setOnClickListener(v -> {
            viewModel.createRoutinePlan(
                    "Medication Support",
                    "Morning medicine checklist with hydration confirmation",
                    "08:00 AM"
            );
                syncTaskPlanToCloud(
                    "Medication Support",
                    "Morning medicine checklist with hydration confirmation",
                    "08:00 AM",
                    "TEMPLATE",
                    "MEDICATION",
                    safeValue(prefManager.getActivePatientRisk(), "MEDIUM"),
                    "LOW",
                    "medication_round",
                    buildDefaultSteps()
                );
            showTemplateAdded("Medication Support");
        });

        btnTemplateDailyCare.setOnClickListener(v -> {
            viewModel.createRoutinePlan(
                    "Daily Care Routine",
                    "Hygiene, meal prep, and mobility check",
                    "09:00 AM"
            );
                syncTaskPlanToCloud(
                    "Daily Care Routine",
                    "Hygiene, meal prep, and mobility check",
                    "09:00 AM",
                    "TEMPLATE",
                    "HYGIENE",
                    safeValue(prefManager.getActivePatientRisk(), "MEDIUM"),
                    "LOW",
                    "morning_stability",
                    buildDefaultSteps()
                );
            showTemplateAdded("Daily Care Routine");
        });

        btnTemplateWellness.setOnClickListener(v -> {
            viewModel.createRoutinePlan(
                    "Wellness Activity",
                    "Exercise, breathing, and social check-in",
                    "05:30 PM"
            );
                syncTaskPlanToCloud(
                    "Wellness Activity",
                    "Exercise, breathing, and social check-in",
                    "05:30 PM",
                    "TEMPLATE",
                    "EXERCISE",
                    safeValue(prefManager.getActivePatientRisk(), "MEDIUM"),
                    "MEDIUM",
                    "fall_risk_evening",
                    buildDefaultSteps()
                );
            showTemplateAdded("Wellness Activity");
        });

        btnOpenAiPlanner.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AiAssistantActivity.class);
            startActivity(intent);
        });

        btnPublishImportedDraft.setOnClickListener(v -> publishImportedDraft());
        btnDiscardImportedDraft.setOnClickListener(v -> {
            prefManager.clearTaskLabDraft();
            bindImportedDraft();
            Toast.makeText(requireContext(), "Draft discarded.", Toast.LENGTH_SHORT).show();
        });

        bindImportedDraft();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        bindImportedDraft();
    }

    private void showTemplateAdded(String name) {
        Toast.makeText(requireContext(), name + " template added to Tasks.", Toast.LENGTH_SHORT).show();
    }

    private void bindImportedDraft() {
        if (cardImportedDraft == null || prefManager == null) {
            return;
        }

        if (!prefManager.hasTaskLabDraft()) {
            cardImportedDraft.setVisibility(View.GONE);
            return;
        }

        cardImportedDraft.setVisibility(View.VISIBLE);
        String title = safeValue(prefManager.getTaskLabDraftTitle(), "AI Routine Draft");
        String description = safeValue(prefManager.getTaskLabDraftDescription(), "Generated by AI assistant");
        String time = safeValue(prefManager.getTaskLabDraftTime(), "09:00 AM");

        tvImportedDraftTitle.setText(title);
        tvImportedDraftDescription.setText(description);
        tvImportedDraftTime.setText("Scheduled time: " + time);
    }

    private void publishImportedDraft() {
        if (!prefManager.hasTaskLabDraft()) {
            Toast.makeText(requireContext(), "No AI draft available.", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = safeValue(prefManager.getTaskLabDraftTitle(), "AI Routine Draft");
        String description = safeValue(prefManager.getTaskLabDraftDescription(), "Generated by AI assistant");
        String time = safeValue(prefManager.getTaskLabDraftTime(), "09:00 AM");

        String taskType = safeValue(prefManager.getTaskLabDraftTaskType(), inferTaskType(title, description));
        String riskLevel = safeValue(prefManager.getTaskLabDraftRiskLevel(), safeValue(prefManager.getActivePatientRisk(), "MEDIUM"));
        String complexity = safeValue(prefManager.getTaskLabDraftComplexity(), "MEDIUM");
        String templateKey = safeValue(prefManager.getTaskLabDraftTemplateKey(), null);
        List<TaskPlanStepPayload> steps = parseDraftSteps(prefManager.getTaskLabDraftStepsJson());
        if (steps.isEmpty()) {
            steps = buildDefaultSteps();
        }

        viewModel.createRoutinePlan(title, description, time);
        syncTaskPlanToCloud(title, description, time, "AI", taskType, riskLevel, complexity, templateKey, steps);
        prefManager.clearTaskLabDraft();
        bindImportedDraft();
        Toast.makeText(requireContext(), "AI draft published to Tasks.", Toast.LENGTH_SHORT).show();
    }

    private void syncTaskPlanToCloud(
            String title,
            String description,
            String scheduledTime,
            String source,
            String draftTaskType,
            String draftRiskLevel,
            String draftComplexity,
            String draftTemplateKey,
            List<TaskPlanStepPayload> draftSteps
    ) {
        String patientId = safeValue(prefManager.getActivePatientId(), "");
        if (patientId.isEmpty()) {
            return;
        }

        String riskLevel = safeValue(draftRiskLevel, safeValue(prefManager.getActivePatientRisk(), "MEDIUM")).toUpperCase(Locale.US);
        String normalizedSource = safeValue(source, "MANUAL").toUpperCase(Locale.US);
        String taskType = safeValue(draftTaskType, inferTaskType(title, description)).toUpperCase(Locale.US);
        String complexity = safeValue(draftComplexity, "MEDIUM").toUpperCase(Locale.US);
        List<TaskPlanStepPayload> steps = (draftSteps == null || draftSteps.isEmpty()) ? buildDefaultSteps() : draftSteps;

        TaskPlanCreateRequest request = new TaskPlanCreateRequest(
                patientId,
                title,
                description,
                scheduledTime,
                taskType,
                riskLevel,
                complexity,
                "DRAFT",
                normalizedSource,
                draftTemplateKey,
                steps
        );

        apiService.createTaskLabPlan(request).enqueue(new Callback<TaskPlanResponse>() {
            @Override
            public void onResponse(Call<TaskPlanResponse> call, Response<TaskPlanResponse> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Saved locally. Cloud sync pending.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(requireContext(), "Task Lab synced to cloud.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<TaskPlanResponse> call, Throwable t) {
                Toast.makeText(requireContext(), "Saved locally. Cloud sync pending.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<TaskPlanStepPayload> parseDraftSteps(String stepsJson) {
        String payload = safeValue(stepsJson, "");
        if (payload.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            List<TaskPlanStepPayload> parsed = gson.fromJson(
                    payload,
                    new TypeToken<List<TaskPlanStepPayload>>() { }.getType()
            );
            return parsed == null ? new ArrayList<>() : parsed;
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private List<TaskPlanStepPayload> buildDefaultSteps() {
        List<TaskPlanStepPayload> steps = new ArrayList<>();
        steps.add(new TaskPlanStepPayload(1, "Prepare", "Set up the environment and review instructions.", true));
        steps.add(new TaskPlanStepPayload(2, "Execute", "Complete the core action with adaptive guidance.", true));
        steps.add(new TaskPlanStepPayload(3, "Confirm", "Confirm completion and note follow-up details.", true));
        return steps;
    }

    private String inferTaskType(String title, String description) {
        String joined = (safeValue(title, "") + " " + safeValue(description, "")).toLowerCase(Locale.US);

        if (joined.contains("medication") || joined.contains("dose") || joined.contains("pill")) {
            return "MEDICATION";
        }

        if (joined.contains("hygiene") || joined.contains("bath") || joined.contains("wash")) {
            return "HYGIENE";
        }

        if (joined.contains("meal") || joined.contains("nutrition") || joined.contains("breakfast")) {
            return "MEAL";
        }

        if (joined.contains("exercise") || joined.contains("fall") || joined.contains("mobility")) {
            return "EXERCISE";
        }

        if (joined.contains("social") || joined.contains("engagement")) {
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
}
