package com.example.adapt.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.adapt.R;
import com.example.adapt.databinding.ActivityCreateTaskBinding;
import com.example.adapt.databinding.ItemTaskStepEditBinding;
import com.example.adapt.utils.ThemePreferences;

import java.util.ArrayList;
import java.util.List;

public class CreateTaskActivity extends AppCompatActivity {
    private ActivityCreateTaskBinding binding;
    private final List<ItemTaskStepEditBinding> stepBindings = new ArrayList<>();
    private int selectedDayIndex = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(
                ThemePreferences.isLightMode(this)
                        ? AppCompatDelegate.MODE_NIGHT_NO
                        : AppCompatDelegate.MODE_NIGHT_YES
        );
        super.onCreate(savedInstanceState);
        binding = ActivityCreateTaskBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupSpinners();
        setupRecurrenceDays();
        addStep("Initial Vitals Check", "Measure blood pressure and oxygen saturation before breakfast.");
        addStep("", "");
        bindActions();
        refreshPreview();
    }

    private void setupSpinners() {
        ArrayAdapter<String> statuses = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, new String[]{"Draft", "Scheduled", "Active"});
        ArrayAdapter<String> priorities = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, new String[]{"High", "Medium", "Low"});
        binding.statusSpinner.setAdapter(statuses);
        binding.prioritySpinner.setAdapter(priorities);
        binding.prioritySpinner.setSelection(0);
    }

    private void setupRecurrenceDays() {
        String[] labels = {"M\n12", "T\n13", "W\n14", "T\n15", "F\n16", "S\n17", "S\n18"};
        for (int i = 0; i < labels.length; i++) {
            int index = i;
            TextView day = new TextView(this);
            day.setText(labels[i]);
            day.setGravity(android.view.Gravity.CENTER);
            day.setTextSize(10);
            day.setTextColor(getColor(R.color.adapt_on_surface));
            day.setOnClickListener(view -> {
                selectedDayIndex = index;
                updateRecurrenceSelection();
            });
            binding.recurrenceDays.addView(day, new android.widget.LinearLayout.LayoutParams(
                    0,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    1
            ));
        }
        updateRecurrenceSelection();
    }

    private void updateRecurrenceSelection() {
        for (int i = 0; i < binding.recurrenceDays.getChildCount(); i++) {
            TextView day = (TextView) binding.recurrenceDays.getChildAt(i);
            if (i == selectedDayIndex) {
                day.setBackgroundResource(R.drawable.bg_selected_day);
                day.setTextColor(getColor(android.R.color.white));
            } else {
                day.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                day.setTextColor(getColor(R.color.adapt_on_surface));
            }
        }
    }

    private void bindActions() {
        binding.backButton.setOnClickListener(view -> finish());
        binding.taskNameInput.addTextChangedListener(new SimpleTextWatcher(this::refreshPreview));
        binding.addStepButton.setOnClickListener(view -> addStep("", ""));
        binding.saveDraftButton.setOnClickListener(view ->
                Toast.makeText(this, "Draft saved for Asha Verma", Toast.LENGTH_SHORT).show());
        binding.syncRoutineButton.setOnClickListener(view ->
                Toast.makeText(this, "Routine sync queued", Toast.LENGTH_SHORT).show());
        binding.prioritySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                refreshPreview();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void addStep(String title, String description) {
        ItemTaskStepEditBinding stepBinding = ItemTaskStepEditBinding.inflate(LayoutInflater.from(this), binding.stepsContainer, false);
        stepBindings.add(stepBinding);
        binding.stepsContainer.addView(stepBinding.getRoot());
        int index = stepBindings.size();
        stepBinding.stepNumber.setText(String.valueOf(index));
        stepBinding.stepNumber.setBackgroundResource(index == 1 ? R.drawable.bg_small_circle_primary : R.drawable.bg_small_circle_muted);
        stepBinding.stepTitle.setText(title);
        stepBinding.stepDescription.setText(description);
        stepBinding.stepTitle.addTextChangedListener(new SimpleTextWatcher(this::refreshPreview));
        stepBinding.stepDescription.addTextChangedListener(new SimpleTextWatcher(this::refreshPreview));
        stepBinding.durationButton.setOnClickListener(view -> {
            String current = stepBinding.durationButton.getText().toString();
            if (current.contains("5")) {
                stepBinding.durationButton.setText("◷  10 min");
            } else if (current.contains("10")) {
                stepBinding.durationButton.setText("◷  15 min");
            } else {
                stepBinding.durationButton.setText("◷  5 min");
            }
            refreshPreview();
        });
        stepBinding.removeStepButton.setOnClickListener(view -> removeStep(stepBinding));
        refreshStepNumbers();
        refreshPreview();
    }

    private void removeStep(ItemTaskStepEditBinding stepBinding) {
        if (stepBindings.size() <= 1) {
            Toast.makeText(this, "At least one step is required", Toast.LENGTH_SHORT).show();
            return;
        }
        stepBindings.remove(stepBinding);
        binding.stepsContainer.removeView(stepBinding.getRoot());
        refreshStepNumbers();
        refreshPreview();
    }

    private void refreshStepNumbers() {
        for (int i = 0; i < stepBindings.size(); i++) {
            ItemTaskStepEditBinding stepBinding = stepBindings.get(i);
            stepBinding.stepNumber.setText(String.valueOf(i + 1));
            stepBinding.stepNumber.setBackgroundResource(i == 0 ? R.drawable.bg_small_circle_primary : R.drawable.bg_small_circle_muted);
        }
    }

    private void refreshPreview() {
        if (binding == null) {
            return;
        }
        String title = binding.taskNameInput.getText().toString().trim();
        binding.previewTitle.setText(title.isEmpty() ? "[Task Name Placeholder]" : title);
        String priority = binding.prioritySpinner.getSelectedItem() == null
                ? "HIGH"
                : binding.prioritySpinner.getSelectedItem().toString().toUpperCase();
        binding.previewPriority.setText(priority + "\nPRIORITY");

        int completedTitles = 0;
        StringBuilder preview = new StringBuilder();
        for (ItemTaskStepEditBinding stepBinding : stepBindings) {
            String stepTitle = stepBinding.stepTitle.getText().toString().trim();
            if (!stepTitle.isEmpty()) {
                completedTitles++;
                if (preview.length() > 0) {
                    preview.append("\n");
                }
                preview.append(completedTitles).append(". ").append(stepTitle);
            }
        }
        binding.previewSteps.setText(completedTitles == 0 ? "No steps added yet" : preview.toString());
        binding.timelinePreview.setText("6 AM\n7 AM    " + (title.isEmpty() ? "Vitals" : title) + "\n8 AM\n9 AM");
    }

    private static class SimpleTextWatcher implements TextWatcher {
        private final Runnable onChanged;

        SimpleTextWatcher(Runnable onChanged) {
            this.onChanged = onChanged;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            onChanged.run();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}
