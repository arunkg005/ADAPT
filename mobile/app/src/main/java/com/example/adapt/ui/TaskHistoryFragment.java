package com.example.adapt.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.adapt.adapter.TaskAdapter;
import com.example.adapt.databinding.FragmentTaskHistoryBinding;
import com.example.adapt.viewmodel.TaskViewModel;

public class TaskHistoryFragment extends Fragment {

    private FragmentTaskHistoryBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentTaskHistoryBinding.inflate(inflater, container, false);
        TaskViewModel viewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        TaskAdapter adapter = new TaskAdapter();
        binding.taskList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.taskList.setAdapter(adapter);
        viewModel.getTasks().observe(getViewLifecycleOwner(), adapter::submitList);
        binding.addTaskFab.setOnClickListener(view ->
                startActivity(new Intent(requireContext(), CreateTaskActivity.class)));
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
