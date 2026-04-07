package com.example.adapt.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.adapt.R;
import com.example.adapt.data.model.ActivityLog;
import com.example.adapt.data.model.Routine;
import com.example.adapt.data.network.ApiService;
import com.example.adapt.data.network.NetworkClient;
import com.example.adapt.data.network.dto.AssistNextActionRequest;
import com.example.adapt.data.network.dto.AssistNextActionResponse;
import com.example.adapt.ui.task.TaskActivity;
import com.example.adapt.utils.PrefManager;
import com.example.adapt.viewmodel.RoutineViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AssistModeActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextView tvAssistRoutineTitle;
    private TextView tvAssistRoutineDescription;
    private TextView tvAssistProgress;
    private TextView tvAssistHint;
    private TextView tvAssistSessionSummary;
    private Button btnViewTask;
    private Button btnNextGuidance;
    private Button btnNeedHelpNow;
    private Button btnConfigureAssistSession;
    private Button btnConfigureRoutineRestrictions;
    private Button btnReadGuidance;
    private RoutineViewModel viewModel;
    private PrefManager prefManager;
    private ApiService apiService;
    private Routine activeRoutine;
    private TextToSpeech textToSpeech;
    private boolean textToSpeechReady = false;
    private boolean canConfigureProfile = false;

    private Handler adaptiveHintHandler;
    private Runnable adaptiveHintRunnable;
    private long lastProgressChangeTimestamp = 0L;
    private int lastCompletedSteps = -1;
    private boolean assistRecommendationInFlight = false;

    private int guidanceIndex = 0;
    private int adaptiveGuidanceIndex = 0;
    private final String[] guidancePrompts = new String[] {
            "Take a breath and focus on one action only.",
            "Read the instruction slowly, then tap when finished.",
            "If the step feels hard, tap Need Help and wait calmly.",
            "You are doing well. Small steps still count as progress."
    };
    private final String[] adaptiveGuidancePrompts = new String[] {
        "Pause and do just one small action now. You can finish the rest after this step.",
        "If this is confusing, tap Need Help and wait. Support is on the way.",
        "Try saying the action out loud, then complete only that action.",
        "You are safe. Focus on the first word of the instruction and follow it slowly."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefManager = new PrefManager(this);
        apiService = NetworkClient.getRetrofit(this).create(ApiService.class);
        canConfigureProfile = isCaregiverOrAdmin(prefManager.getRole());
        
        // Make activity full screen and lock distractions
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // Keep screen on during assist mode
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_assist_mode);

        tvAssistRoutineTitle = findViewById(R.id.tvAssistRoutineTitle);
        tvAssistRoutineDescription = findViewById(R.id.tvAssistRoutineDescription);
        tvAssistProgress = findViewById(R.id.tvAssistProgress);
        tvAssistHint = findViewById(R.id.tvAssistHint);
        tvAssistSessionSummary = findViewById(R.id.tvAssistSessionSummary);
        btnViewTask = findViewById(R.id.btnViewActiveTask);
        btnNextGuidance = findViewById(R.id.btnNextGuidance);
        btnNeedHelpNow = findViewById(R.id.btnNeedHelpNow);
        btnConfigureAssistSession = findViewById(R.id.btnConfigureAssistSession);
        btnConfigureRoutineRestrictions = findViewById(R.id.btnConfigureRoutineRestrictions);
        btnReadGuidance = findViewById(R.id.btnReadGuidance);
        Button btnUnlock = findViewById(R.id.btnCaretakerUnlock);

        textToSpeech = new TextToSpeech(this, this);

        applyAssistAccessibilityProfile();
        updateSessionSummary();

        viewModel = new ViewModelProvider(this).get(RoutineViewModel.class);
        viewModel.getAllRoutines().observe(this, routines -> renderActiveRoutine(selectActiveRoutine(routines)));

        applyGuidancePrompt();
        startAdaptiveHintLoop();

        btnViewTask.setOnClickListener(v -> {
            if (activeRoutine == null) {
                Toast.makeText(this, "No active routine available", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, TaskActivity.class);
            intent.putExtra("ROUTINE_ID", activeRoutine.getId());
            intent.putExtra("ROUTINE_TITLE", activeRoutine.getTitle());
            startActivity(intent);
        });

        btnNextGuidance.setOnClickListener(v -> {
            guidanceIndex = (guidanceIndex + 1) % guidancePrompts.length;
            applyGuidancePrompt();
        });

        btnNeedHelpNow.setOnClickListener(v -> {
            String routineTitle = activeRoutine != null ? activeRoutine.getTitle() : "Unknown routine";
            viewModel.insertActivityLog(new ActivityLog(
                    "Assist Mode Help Request",
                    "Patient tapped Need Help in Focused Assist Mode",
                    System.currentTimeMillis(),
                    ActivityLog.Source.MOBILE,
                    ActivityLog.Type.WARNING,
                    "Routine: " + routineTitle
            ));
            Toast.makeText(this, "Help request saved for caregiver review.", Toast.LENGTH_LONG).show();
            if (prefManager.isAssistVoiceGuidanceEnabled()) {
                speakMessage("Help request sent. Stay calm and wait for caregiver support.");
            }
        });

        btnReadGuidance.setOnClickListener(v -> {
            String hint = tvAssistHint.getText() == null ? "" : tvAssistHint.getText().toString();
            speakMessage(hint);
        });

        if (canConfigureProfile) {
            btnConfigureAssistSession.setVisibility(View.VISIBLE);
            btnConfigureAssistSession.setOnClickListener(v -> showAssistProfileDialog());
            btnConfigureRoutineRestrictions.setVisibility(View.VISIBLE);
            btnConfigureRoutineRestrictions.setOnClickListener(v -> showRoutineRestrictionsDialog());
        } else {
            btnConfigureAssistSession.setVisibility(View.GONE);
            btnConfigureRoutineRestrictions.setVisibility(View.GONE);
        }

        // "Locked" functionality: Caretaker must long press to exit
        btnUnlock.setOnLongClickListener(v -> {
            Toast.makeText(this, "Assist Mode Deactivated", Toast.LENGTH_SHORT).show();
            finish();
            return true;
        });
        
        btnUnlock.setOnClickListener(v -> {
            Toast.makeText(this, "Long press to unlock Assist Mode", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onBackPressed() {
        // Disable back button to maintain focus (Processing Speed & Attention Control)
        Toast.makeText(this, "Exit restricted to Caregiver only.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) {
            return;
        }

        int languageStatus = textToSpeech.setLanguage(Locale.US);
        textToSpeechReady = languageStatus != TextToSpeech.LANG_MISSING_DATA
                && languageStatus != TextToSpeech.LANG_NOT_SUPPORTED;
    }

    @Override
    protected void onDestroy() {
        if (adaptiveHintHandler != null && adaptiveHintRunnable != null) {
            adaptiveHintHandler.removeCallbacks(adaptiveHintRunnable);
        }

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        super.onDestroy();
    }

    private Routine selectActiveRoutine(List<Routine> routines) {
        if (routines == null || routines.isEmpty()) {
            return null;
        }

        for (Routine routine : routines) {
            int totalSteps = Math.max(1, routine.getTotalSteps());
            if (routine.getCompletedSteps() < totalSteps) {
                return routine;
            }
        }

        return routines.get(0);
    }

    private void renderActiveRoutine(Routine routine) {
        activeRoutine = routine;

        if (routine == null) {
            tvAssistRoutineTitle.setText("No routine selected");
            tvAssistRoutineDescription.setText("Open Tasks and create a routine to begin assist mode.");
            tvAssistProgress.setText("Progress: --");
            btnViewTask.setEnabled(false);
            btnConfigureRoutineRestrictions.setEnabled(false);
            lastCompletedSteps = -1;
            lastProgressChangeTimestamp = System.currentTimeMillis();
            updateSessionSummary();
            return;
        }

        int totalSteps = Math.max(1, routine.getTotalSteps());
        int completedSteps = Math.min(routine.getCompletedSteps(), totalSteps);

        if (completedSteps != lastCompletedSteps) {
            lastCompletedSteps = completedSteps;
            lastProgressChangeTimestamp = System.currentTimeMillis();
            adaptiveGuidanceIndex = 0;
        }

        tvAssistRoutineTitle.setText(routine.getTitle());
        tvAssistRoutineDescription.setText(routine.getDescription());
        tvAssistProgress.setText("Progress: " + completedSteps + " / " + totalSteps + " steps");
        btnViewTask.setEnabled(true);
        btnConfigureRoutineRestrictions.setEnabled(canConfigureProfile);
        updateSessionSummary();
    }

    private void applyGuidancePrompt() {
        boolean useAdaptivePrompt = shouldUseAdaptivePrompt();
        String message;

        if (useAdaptivePrompt) {
            message = adaptiveGuidancePrompts[adaptiveGuidanceIndex % adaptiveGuidancePrompts.length];
            adaptiveGuidanceIndex++;
        } else {
            message = guidancePrompts[guidanceIndex % guidancePrompts.length];
        }

        tvAssistHint.setText("Hint: " + message);
        if (prefManager.isAssistVoiceGuidanceEnabled()) {
            speakMessage(message);
        }

        requestCloudAssistRecommendation(message);
    }

    private void showAssistProfileDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_assist_profile, null, false);
        SwitchMaterial switchVoiceGuidance = dialogView.findViewById(R.id.switchVoiceGuidance);
        SwitchMaterial switchLargeText = dialogView.findViewById(R.id.switchLargeText);
        SwitchMaterial switchDoubleConfirm = dialogView.findViewById(R.id.switchDoubleConfirm);
        SwitchMaterial switchAdaptivePrompts = dialogView.findViewById(R.id.switchAdaptivePrompts);

        switchVoiceGuidance.setChecked(prefManager.isAssistVoiceGuidanceEnabled());
        switchLargeText.setChecked(prefManager.isAssistLargeTextEnabled());
        switchDoubleConfirm.setChecked(prefManager.isAssistDoubleConfirmEnabled());
        switchAdaptivePrompts.setChecked(prefManager.isAssistAdaptivePromptsEnabled());

        new MaterialAlertDialogBuilder(this)
                .setTitle("Assist Session Setup")
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Apply", (dialog, which) -> {
                    prefManager.setAssistVoiceGuidanceEnabled(switchVoiceGuidance.isChecked());
                    prefManager.setAssistLargeTextEnabled(switchLargeText.isChecked());
                    prefManager.setAssistDoubleConfirmEnabled(switchDoubleConfirm.isChecked());
                    prefManager.setAssistAdaptivePromptsEnabled(switchAdaptivePrompts.isChecked());

                    applyAssistAccessibilityProfile();
                    updateSessionSummary();
                    applyGuidancePrompt();

                    viewModel.insertActivityLog(new ActivityLog(
                            "Assist Profile Updated",
                            "Caregiver updated assist session controls",
                            System.currentTimeMillis(),
                            ActivityLog.Source.MOBILE,
                            ActivityLog.Type.INFO,
                            buildProfileSummary()
                    ));

                    Toast.makeText(this, "Assist session profile applied.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showRoutineRestrictionsDialog() {
        if (activeRoutine == null) {
            Toast.makeText(this, "Select an active routine first.", Toast.LENGTH_SHORT).show();
            return;
        }

        int routineId = activeRoutine.getId();
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_routine_restrictions, null, false);
        TextView tvRoutinePolicyTarget = dialogView.findViewById(R.id.tvRoutinePolicyTarget);
        SwitchMaterial switchRoutineDoubleConfirm = dialogView.findViewById(R.id.switchRoutineDoubleConfirm);
        SwitchMaterial switchRoutineAllowRepeat = dialogView.findViewById(R.id.switchRoutineAllowRepeat);
        SwitchMaterial switchRoutineAllowHelp = dialogView.findViewById(R.id.switchRoutineAllowHelp);

        tvRoutinePolicyTarget.setText("Routine: " + activeRoutine.getTitle());
        switchRoutineDoubleConfirm.setChecked(prefManager.getRoutineDoubleConfirmRequired(routineId));
        switchRoutineAllowRepeat.setChecked(prefManager.isRoutineRepeatAllowed(routineId));
        switchRoutineAllowHelp.setChecked(prefManager.isRoutineHelpAllowed(routineId));

        new MaterialAlertDialogBuilder(this)
                .setTitle("Task Restrictions")
                .setView(dialogView)
                .setNeutralButton("Reset", (dialog, which) -> {
                    prefManager.clearRoutineRestrictions(routineId);
                    updateSessionSummary();
                    Toast.makeText(this, "Routine restrictions reset to session defaults.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Apply", (dialog, which) -> {
                    prefManager.setRoutineRestrictions(
                            routineId,
                            switchRoutineDoubleConfirm.isChecked(),
                            switchRoutineAllowRepeat.isChecked(),
                            switchRoutineAllowHelp.isChecked()
                    );

                    updateSessionSummary();

                    viewModel.insertActivityLog(new ActivityLog(
                            "Routine Restrictions Updated",
                            "Caregiver updated task restrictions for active routine",
                            System.currentTimeMillis(),
                            ActivityLog.Source.MOBILE,
                            ActivityLog.Type.INFO,
                            buildRoutinePolicySummary(routineId)
                    ));

                    Toast.makeText(this, "Routine restrictions applied.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void applyAssistAccessibilityProfile() {
        boolean largeText = prefManager.isAssistLargeTextEnabled();

        tvAssistRoutineTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, largeText ? 28f : 22f);
        tvAssistRoutineDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, largeText ? 17f : 14f);
        tvAssistProgress.setTextSize(TypedValue.COMPLEX_UNIT_SP, largeText ? 17f : 14f);
        tvAssistHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, largeText ? 20f : 16f);
        btnViewTask.setTextSize(TypedValue.COMPLEX_UNIT_SP, largeText ? 21f : 18f);
        btnNextGuidance.setTextSize(TypedValue.COMPLEX_UNIT_SP, largeText ? 18f : 15f);
        btnNeedHelpNow.setTextSize(TypedValue.COMPLEX_UNIT_SP, largeText ? 18f : 15f);
        btnReadGuidance.setTextSize(TypedValue.COMPLEX_UNIT_SP, largeText ? 16f : 13f);
    }

    private void updateSessionSummary() {
        if (activeRoutine == null) {
            tvAssistSessionSummary.setText(buildProfileSummary());
        } else {
            int routineId = activeRoutine.getId();
            String base = buildProfileSummary();
            String routine = buildRoutinePolicySummary(routineId);
            tvAssistSessionSummary.setText(base + "\n" + routine);
        }

        btnReadGuidance.setEnabled(prefManager.isAssistVoiceGuidanceEnabled());
    }

    private String buildProfileSummary() {
        String voice = prefManager.isAssistVoiceGuidanceEnabled() ? "ON" : "OFF";
        String largeText = prefManager.isAssistLargeTextEnabled() ? "ON" : "OFF";
        String doubleConfirm = prefManager.isAssistDoubleConfirmEnabled() ? "ON" : "OFF";
        String adaptive = prefManager.isAssistAdaptivePromptsEnabled() ? "ON" : "OFF";
        return "Voice " + voice + " | Large text " + largeText + " | Double confirm " + doubleConfirm + " | Adaptive prompts " + adaptive;
    }

    private String buildRoutinePolicySummary(int routineId) {
        if (!prefManager.hasRoutineRestrictions(routineId)) {
            return "Routine policy: Default session controls";
        }

        String doubleConfirm = prefManager.getRoutineDoubleConfirmRequired(routineId) ? "ON" : "OFF";
        String repeat = prefManager.isRoutineRepeatAllowed(routineId) ? "ON" : "OFF";
        String help = prefManager.isRoutineHelpAllowed(routineId) ? "ON" : "OFF";
        return "Routine policy: Double confirm " + doubleConfirm + " | Repeat " + repeat + " | Help " + help;
    }

    private void startAdaptiveHintLoop() {
        if (adaptiveHintHandler != null) {
            return;
        }

        adaptiveHintHandler = new Handler(Looper.getMainLooper());
        adaptiveHintRunnable = new Runnable() {
            @Override
            public void run() {
                guidanceIndex = (guidanceIndex + 1) % guidancePrompts.length;
                applyGuidancePrompt();
                if (adaptiveHintHandler != null) {
                    adaptiveHintHandler.postDelayed(this, 45_000L);
                }
            }
        };

        adaptiveHintHandler.postDelayed(adaptiveHintRunnable, 45_000L);
    }

    private boolean shouldUseAdaptivePrompt() {
        if (!prefManager.isAssistAdaptivePromptsEnabled()) {
            return false;
        }

        if (activeRoutine == null || lastProgressChangeTimestamp <= 0L) {
            return false;
        }

        long idleDurationMs = System.currentTimeMillis() - lastProgressChangeTimestamp;
        return idleDurationMs >= 120_000L;
    }

    private void speakMessage(String message) {
        if (!textToSpeechReady || message == null || message.trim().isEmpty()) {
            return;
        }

        if (!prefManager.isAssistVoiceGuidanceEnabled()) {
            return;
        }

        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "ADAPT_ASSIST_GUIDANCE");
    }

    private void requestCloudAssistRecommendation(String fallbackMessage) {
        String patientId = prefManager.getActivePatientId();
        if (patientId == null || patientId.trim().isEmpty()) {
            return;
        }

        if (assistRecommendationInFlight || apiService == null) {
            return;
        }

        assistRecommendationInFlight = true;

        AssistNextActionRequest request = new AssistNextActionRequest(
                patientId.trim(),
                buildCurrentTaskPayload(),
                false
        );

        apiService.getAssistModeNextAction(request).enqueue(new Callback<AssistNextActionResponse>() {
            @Override
            public void onResponse(Call<AssistNextActionResponse> call, Response<AssistNextActionResponse> response) {
                assistRecommendationInFlight = false;

                AssistNextActionResponse payload = response.body();
                if (!response.isSuccessful() || payload == null) {
                    return;
                }

                String uiPrompt = normalizePrompt(payload.getUiPrompt(), fallbackMessage);
                String voicePrompt = normalizePrompt(payload.getVoicePrompt(), uiPrompt);
                tvAssistHint.setText("Hint: " + uiPrompt);

                if (prefManager.isAssistVoiceGuidanceEnabled()) {
                    speakMessage(voicePrompt);
                }

                if (payload.isEscalate()) {
                    viewModel.insertActivityLog(new ActivityLog(
                            "Assist Escalation Recommended",
                            "Cognitive engine requested caregiver escalation",
                            System.currentTimeMillis(),
                            ActivityLog.Source.MOBILE,
                            ActivityLog.Type.WARNING,
                            "Mode: " + normalizePrompt(payload.getAssistanceMode(), "UNKNOWN")
                    ));
                }
            }

            @Override
            public void onFailure(Call<AssistNextActionResponse> call, Throwable t) {
                assistRecommendationInFlight = false;
            }
        });
    }

    private AssistNextActionRequest.CurrentTask buildCurrentTaskPayload() {
        String taskName = activeRoutine != null ? activeRoutine.getTitle() : "Assist Session Task";
        String taskId = activeRoutine != null ? String.valueOf(activeRoutine.getId()) : "assist-session";
        int stepCount = activeRoutine != null ? Math.max(1, activeRoutine.getTotalSteps()) : 1;

        return new AssistNextActionRequest.CurrentTask(
                taskId,
                taskName,
                inferTaskType(taskName),
                normalizePrompt(prefManager.getActivePatientRisk(), "MEDIUM").toUpperCase(Locale.US),
                "MEDIUM",
                System.currentTimeMillis(),
                300_000L,
                stepCount
        );
    }

    private String inferTaskType(String taskName) {
        String normalized = normalizePrompt(taskName, "").toLowerCase(Locale.US);

        if (normalized.contains("medication") || normalized.contains("dose")) {
            return "MEDICATION";
        }

        if (normalized.contains("hygiene") || normalized.contains("wash")) {
            return "HYGIENE";
        }

        if (normalized.contains("meal") || normalized.contains("breakfast") || normalized.contains("dinner")) {
            return "MEAL";
        }

        if (normalized.contains("exercise") || normalized.contains("fall") || normalized.contains("mobility")) {
            return "EXERCISE";
        }

        if (normalized.contains("social") || normalized.contains("engagement")) {
            return "SOCIAL";
        }

        return "OTHER";
    }

    private String normalizePrompt(String value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private boolean isCaregiverOrAdmin(String role) {
        String normalized = role == null ? "" : role.trim().toLowerCase(Locale.US);
        return "caregiver".equals(normalized) || "admin".equals(normalized);
    }
}
