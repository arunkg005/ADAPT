package com.example.adapt.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.adapt.databinding.BottomSheetAddTaskBinding;
import com.example.adapt.model.CareTask;
import com.example.adapt.viewmodel.TaskViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class AddTaskBottomSheetFragment extends BottomSheetDialogFragment {
    private BottomSheetAddTaskBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetAddTaskBinding.inflate(inflater, container, false);
        ArrayAdapter<String> priorities = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, new String[]{"HIGH", "MED", "LOW"});
        ArrayAdapter<String> statuses = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, new String[]{"Pending", "Scheduled", "Draft"});
        binding.prioritySpinner.setAdapter(priorities);
        binding.statusSpinner.setAdapter(statuses);
        binding.saveTaskButton.setOnClickListener(view -> saveTask());
        return binding.getRoot();
    }

    private void saveTask() {
        String title = binding.taskNameInput.getText().toString().trim();
        String description = binding.taskDescriptionInput.getText().toString().trim();
        if (title.isEmpty()) {
            binding.taskNameInput.setError("Task name required");
            return;
        }
        if (description.isEmpty()) {
            description = "Quick task logged from Task Lab";
        }
        TaskViewModel viewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        viewModel.addTask(new CareTask(
                0,
                title,
                description,
                binding.prioritySpinner.getSelectedItem().toString(),
                binding.statusSpinner.getSelectedItem().toString(),
                "Now"
        ));
        dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
