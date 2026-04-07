package com.example.adapt.ui.task;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.adapt.R;
import com.example.adapt.data.model.ActivityLog;
import com.example.adapt.data.model.Task;
import com.example.adapt.data.model.TaskLog;
import com.example.adapt.utils.PrefManager;
import com.example.adapt.viewmodel.RoutineViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TaskActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextView tvRoutineTitle, tvTaskTitle, tvTaskDescription, tvProgressText;
    private Button btnNextTask, btnRepeatTask, btnNeedHelp;
    private ProgressBar taskProgressBar;
    private ImageView ivTaskVisual;
    private RoutineViewModel viewModel;
    private List<Task> taskList = new ArrayList<>();
    private int currentTaskIndex = 0;
    private int routineId;
    private PrefManager prefManager;
    private TextToSpeech textToSpeech;
    private boolean textToSpeechReady = false;
    private boolean assistVoiceGuidanceEnabled = true;
    private boolean assistLargeTextEnabled = true;
    private boolean assistDoubleConfirmEnabled = true;
    private boolean routineRepeatAllowed = true;
    private boolean routineHelpAllowed = true;
    private boolean awaitingDoubleConfirmation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        // Initialize UI Components
        tvRoutineTitle = findViewById(R.id.tvRoutineTitle);
        tvTaskTitle = findViewById(R.id.tvTaskTitle);
        tvTaskDescription = findViewById(R.id.tvTaskDescription);
        tvProgressText = findViewById(R.id.tvProgressText);
        btnNextTask = findViewById(R.id.btnNextTask);
        btnRepeatTask = findViewById(R.id.btnRepeatTask);
        btnNeedHelp = findViewById(R.id.btnNeedHelp);
        taskProgressBar = findViewById(R.id.taskProgressBar);
        ivTaskVisual = findViewById(R.id.ivTaskVisual);
        prefManager = new PrefManager(this);
        textToSpeech = new TextToSpeech(this, this);

        routineId = getIntent().getIntExtra("ROUTINE_ID", -1);
        if (routineId <= 0) {
            Toast.makeText(this, "Invalid routine", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadAssistProfile();
        applyAssistProfileToUI();
        applyRoutineRestrictionsToActions();

        String routineTitle = getIntent().getStringExtra("ROUTINE_TITLE");
        tvRoutineTitle.setText(routineTitle != null ? routineTitle : "Routine");

        viewModel = new ViewModelProvider(this).get(RoutineViewModel.class);
        
        viewModel.getTasksForRoutine(routineId).observe(this, tasks -> {
            if (tasks != null && !tasks.isEmpty()) {
                taskList = tasks;
                if (currentTaskIndex >= taskList.size()) {
                    currentTaskIndex = taskList.size() - 1;
                }
                displayTask(currentTaskIndex);
            } else if (!prefManager.isTaskSeeded(routineId)) {
                addDummyTasks();
                prefManager.setTaskSeeded(routineId, true);
            } else {
                btnNextTask.setEnabled(false);
                btnRepeatTask.setEnabled(false);
                Toast.makeText(this, "No tasks available for this routine", Toast.LENGTH_SHORT).show();
            }
        });

        // "Done" Button - Supporting Executive Function & Memory
        btnNextTask.setOnClickListener(v -> {
            if (taskList.isEmpty()) {
                return;
            }

            if (assistDoubleConfirmEnabled && !awaitingDoubleConfirmation) {
                awaitingDoubleConfirmation = true;
                btnNextTask.setText("Tap Again To Confirm");
                Toast.makeText(this, "Tap once more to confirm this step.", Toast.LENGTH_SHORT).show();
                return;
            }

            logTaskCompletion();
            viewModel.updateRoutineProgress(routineId, currentTaskIndex + 1);
            awaitingDoubleConfirmation = false;

            currentTaskIndex++;
            if (currentTaskIndex < taskList.size()) {
                displayTask(currentTaskIndex);
            } else {
                Toast.makeText(this, getString(R.string.routine_completed), Toast.LENGTH_LONG).show();
                finish();
            }
        });

        // "Repeat" Button - Supporting Processing Speed Limitations
        btnRepeatTask.setOnClickListener(v -> {
            if (taskList.isEmpty()) {
                return;
            }
            displayTask(currentTaskIndex); // Re-triggering display/animation
            Toast.makeText(this, "Focus on this step.", Toast.LENGTH_SHORT).show();
        });

        btnNeedHelp.setOnClickListener(v -> {
            if (taskList.isEmpty()) {
                return;
            }

            Task task = taskList.get(currentTaskIndex);
            ActivityLog helpLog = new ActivityLog(
                    "Help Requested",
                    "Patient requested guidance",
                    System.currentTimeMillis(),
                    ActivityLog.Source.MOBILE,
                    ActivityLog.Type.WARNING,
                    "Routine: " + tvRoutineTitle.getText() + "\nTask: " + task.getTitle() + "\nDescription: " + task.getStepDescription()
            );
            viewModel.insertActivityLog(helpLog);
            Toast.makeText(this, "Help request logged for caregiver review.", Toast.LENGTH_LONG).show();
            speakMessage("Help request recorded. Please stay calm. Guidance is on screen.");
        });
    }

    private void displayTask(int index) {
        if (taskList.isEmpty()) return;

        Task task = taskList.get(index);
        
        // 1. One-Step-at-a-Time (Working Memory Support)
        tvTaskTitle.setText(task.getTitle());
        tvTaskDescription.setText(task.getStepDescription());
        
        // 2. Clear Visual Feedback (Attention Control)
        updateVisualForTask(task.getTitle());
        
        // 3. Progress Tracking (Executive Function Support)
        int totalTasks = taskList.size();
        tvProgressText.setText((index + 1) + " / " + totalTasks);
        taskProgressBar.setMax(totalTasks);
        taskProgressBar.setProgress(index + 1);
        
        // 4. Action text updates
        if (index == totalTasks - 1) {
            btnNextTask.setText(getString(R.string.finish_routine));
        } else {
            btnNextTask.setText("I've Done This");
        }

        awaitingDoubleConfirmation = false;

        speakMessage("Step " + (index + 1) + " of " + totalTasks + ". " + task.getTitle() + ". " + task.getStepDescription());
    }

    private void updateVisualForTask(String title) {
        // In a real app, this would change based on task type
        // Placeholder logic to change icons for visual cues
        if (title.toLowerCase().contains("med")) {
            ivTaskVisual.setImageResource(android.R.drawable.ic_menu_agenda);
        } else if (title.toLowerCase().contains("walk")) {
            ivTaskVisual.setImageResource(android.R.drawable.ic_menu_directions);
        } else {
            ivTaskVisual.setImageResource(android.R.drawable.ic_dialog_info);
        }
    }

    private void logTaskCompletion() {
        if (currentTaskIndex < taskList.size()) {
            Task task = taskList.get(currentTaskIndex);
            String userId = prefManager.getUserEmail();
            if (userId == null || userId.trim().isEmpty()) {
                userId = "local_user";
            }
            TaskLog log = new TaskLog(task.getId(), userId, true, System.currentTimeMillis());
            viewModel.logTaskCompletion(log);
        }
    }

    private void addDummyTasks() {
        viewModel.insertTask(new Task(routineId, "Prepare Space", "Clear the table and sit down.", 1));
        viewModel.insertTask(new Task(routineId, "Take Medicine", "Open the blue box and take one pill.", 2));
        viewModel.insertTask(new Task(routineId, "Drink Water", "Drink a full glass of water.", 3));
        viewModel.insertTask(new Task(routineId, "Log Action", "Confirm you've finished on the screen.", 4));
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

    private void speakMessage(String message) {
        if (!textToSpeechReady || message == null || message.trim().isEmpty()) {
            return;
        }

        if (!assistVoiceGuidanceEnabled) {
            return;
        }

        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "ADAPT_TASK_GUIDANCE");
    }

    private void loadAssistProfile() {
        assistVoiceGuidanceEnabled = prefManager.isAssistVoiceGuidanceEnabled();
        assistLargeTextEnabled = prefManager.isAssistLargeTextEnabled();
        assistDoubleConfirmEnabled = prefManager.getRoutineDoubleConfirmRequired(routineId);
        routineRepeatAllowed = prefManager.isRoutineRepeatAllowed(routineId);
        routineHelpAllowed = prefManager.isRoutineHelpAllowed(routineId);
    }

    private void applyAssistProfileToUI() {
        tvTaskTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, assistLargeTextEnabled ? 34f : 28f);
        tvTaskDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, assistLargeTextEnabled ? 24f : 20f);
        tvProgressText.setTextSize(TypedValue.COMPLEX_UNIT_SP, assistLargeTextEnabled ? 18f : 14f);
        btnNextTask.setTextSize(TypedValue.COMPLEX_UNIT_SP, assistLargeTextEnabled ? 24f : 22f);
        btnRepeatTask.setTextSize(TypedValue.COMPLEX_UNIT_SP, assistLargeTextEnabled ? 19f : 16f);
        btnNeedHelp.setTextSize(TypedValue.COMPLEX_UNIT_SP, assistLargeTextEnabled ? 19f : 16f);
    }

    private void applyRoutineRestrictionsToActions() {
        btnRepeatTask.setVisibility(routineRepeatAllowed ? View.VISIBLE : View.GONE);
        btnNeedHelp.setVisibility(routineHelpAllowed ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
