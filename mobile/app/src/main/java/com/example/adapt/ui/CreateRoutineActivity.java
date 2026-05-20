package com.example.adapt.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.adapt.R;
import com.example.adapt.databinding.ActivityCreateRoutineBinding;
import com.example.adapt.databinding.ItemCreateRoutineTaskBinding;
import com.example.adapt.utils.ThemePreferences;

import java.util.ArrayList;
import java.util.List;

public class CreateRoutineActivity extends AppCompatActivity {
    private ActivityCreateRoutineBinding binding;
    private final List<ItemCreateRoutineTaskBinding> taskBindings = new ArrayList<>();
    private int selectedCalendarIndex = 11;
    private int timeWindowIndex = 0;

    private final String[][] timeWindows = {
            {"07:30 AM", "09:00 AM"},
            {"08:00 AM", "09:30 AM"},
            {"09:00 AM", "10:00 AM"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(
                ThemePreferences.isLightMode(this)
                        ? AppCompatDelegate.MODE_NIGHT_NO
                        : AppCompatDelegate.MODE_NIGHT_YES
        );
        super.onCreate(savedInstanceState);
        binding = ActivityCreateRoutineBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.backButton.setOnClickListener(view -> finish());
        setupTasks();
        setupCalendar();
        bindActions();
        updateTimeWindow();
    }

    private void setupTasks() {
        addRoutineTask("Morning vitals and hygiene support", "Daily routine: BP/sugar check and hygiene.", "MEDIUM");
    }

    private void setupCalendar() {
        String[] cells = {
                "MO", "TU", "WE", "TH", "FR", "SA", "SU",
                "28", "29", "30", "31", "1", "2", "3",
                "11", "12", "13", "14", "15", "16", "17"
        };
        binding.calendarGrid.removeAllViews();
        for (int i = 0; i < cells.length; i++) {
            final int index = i;
            TextView cell = new TextView(this);
            cell.setText(cells[i]);
            cell.setGravity(Gravity.CENTER);
            cell.setTextSize(i < 7 ? 11 : 13);
            cell.setTextColor(getColor(i < 7 ? R.color.adapt_on_surface : R.color.adapt_on_surface_variant));
            cell.setOnClickListener(view -> {
                if (index >= 7) {
                    selectedCalendarIndex = index;
                    updateCalendarSelection();
                }
            });
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = (int) (44 * getResources().getDisplayMetrics().density);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            binding.calendarGrid.addView(cell, params);
        }
        updateCalendarSelection();
    }

    private void updateCalendarSelection() {
        for (int i = 0; i < binding.calendarGrid.getChildCount(); i++) {
            TextView cell = (TextView) binding.calendarGrid.getChildAt(i);
            if (i == selectedCalendarIndex) {
                cell.setBackgroundResource(R.drawable.bg_selected_day);
                cell.setTextColor(getColor(android.R.color.white));
                binding.selectedDateLabel.setText("SUN\n" + cell.getText());
            } else {
                cell.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                cell.setTextColor(getColor(i < 7 ? R.color.adapt_on_surface : R.color.adapt_on_surface_variant));
            }
        }
    }

    private void bindActions() {
        binding.addRoutineTaskButton.setOnClickListener(view ->
                addRoutineTask("New routine task", "Tap to configure from library or lab.", "LOW"));
        binding.libraryButton.setOnClickListener(view ->
                addRoutineTask("Medication adherence check", "Confirm medication timing and record adherence.", "MEDIUM"));
        binding.labButton.setOnClickListener(view ->
                addRoutineTask("Glucose reading", "Capture glucose reading and attach lab context.", "HIGH"));
        binding.previousMonthButton.setOnClickListener(view ->
                Toast.makeText(this, "Previous month preview", Toast.LENGTH_SHORT).show());
        binding.nextMonthButton.setOnClickListener(view ->
                Toast.makeText(this, "Next month preview", Toast.LENGTH_SHORT).show());
        binding.startTimeButton.setOnClickListener(view -> cycleTimeWindow());
        binding.endTimeButton.setOnClickListener(view -> cycleTimeWindow());
        binding.saveRoutineButton.setOnClickListener(view -> saveRoutine());
    }

    private void addRoutineTask(String title, String description, String priority) {
        ItemCreateRoutineTaskBinding taskBinding = ItemCreateRoutineTaskBinding.inflate(
                LayoutInflater.from(this),
                binding.routineTasksContainer,
                false
        );
        taskBinding.taskTitle.setText(title);
        taskBinding.taskDescription.setText(description);
        taskBinding.taskPriority.setText(priority);
        taskBinding.taskChecked.setChecked(true);
        taskBinding.removeTaskButton.setOnClickListener(view -> removeRoutineTask(taskBinding));
        taskBinding.getRoot().setOnClickListener(view -> {
            taskBinding.taskChecked.setChecked(!taskBinding.taskChecked.isChecked());
            Toast.makeText(this, taskBinding.taskChecked.isChecked() ? "Task enabled" : "Task disabled", Toast.LENGTH_SHORT).show();
        });
        taskBindings.add(taskBinding);
        binding.routineTasksContainer.addView(taskBinding.getRoot());
    }

    private void removeRoutineTask(ItemCreateRoutineTaskBinding taskBinding) {
        if (taskBindings.size() <= 1) {
            Toast.makeText(this, "At least one routine task is required", Toast.LENGTH_SHORT).show();
            return;
        }
        taskBindings.remove(taskBinding);
        binding.routineTasksContainer.removeView(taskBinding.getRoot());
    }

    private void cycleTimeWindow() {
        timeWindowIndex = (timeWindowIndex + 1) % timeWindows.length;
        updateTimeWindow();
    }

    private void updateTimeWindow() {
        binding.startTimeButton.setText(timeWindows[timeWindowIndex][0]);
        binding.endTimeButton.setText(timeWindows[timeWindowIndex][1]);
        binding.scheduledBlock.setText("SCHEDULED\n" + timeWindows[timeWindowIndex][0] + " - " + timeWindows[timeWindowIndex][1]);
    }

    private void saveRoutine() {
        String routineName = binding.routineNameInput.getText().toString().trim();
        if (routineName.isEmpty()) {
            binding.routineNameInput.setError("Routine name required");
            return;
        }
        int enabledTasks = 0;
        for (ItemCreateRoutineTaskBinding taskBinding : taskBindings) {
            if (taskBinding.taskChecked.isChecked()) {
                enabledTasks++;
            }
        }
        Toast.makeText(this, "Saved " + routineName + " with " + enabledTasks + " active tasks", Toast.LENGTH_LONG).show();
        finish();
    }
}
