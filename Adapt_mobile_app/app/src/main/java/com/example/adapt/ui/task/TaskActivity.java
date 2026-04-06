package com.example.adapt.ui.task;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.adapt.R;
import com.example.adapt.data.model.Task;
import com.example.adapt.data.model.TaskLog;
import com.example.adapt.utils.PrefManager;
import com.example.adapt.viewmodel.RoutineViewModel;

import java.util.ArrayList;
import java.util.List;

public class TaskActivity extends AppCompatActivity {

    private TextView tvRoutineTitle, tvTaskTitle, tvTaskDescription, tvProgressText;
    private Button btnNextTask, btnRepeatTask;
    private ProgressBar taskProgressBar;
    private ImageView ivTaskVisual;
    private RoutineViewModel viewModel;
    private List<Task> taskList = new ArrayList<>();
    private int currentTaskIndex = 0;
    private int routineId;
    private PrefManager prefManager;

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
        taskProgressBar = findViewById(R.id.taskProgressBar);
        ivTaskVisual = findViewById(R.id.ivTaskVisual);
        prefManager = new PrefManager(this);

        routineId = getIntent().getIntExtra("ROUTINE_ID", -1);
        if (routineId <= 0) {
            Toast.makeText(this, "Invalid routine", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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

            logTaskCompletion();
            viewModel.updateRoutineProgress(routineId, currentTaskIndex + 1);

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
}
