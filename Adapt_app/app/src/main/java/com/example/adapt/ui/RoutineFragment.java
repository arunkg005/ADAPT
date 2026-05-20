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

import com.example.adapt.adapter.RoutineAdapter;
import com.example.adapt.databinding.FragmentRoutineBinding;
import com.example.adapt.viewmodel.RoutineViewModel;

public class RoutineFragment extends Fragment {

    private FragmentRoutineBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentRoutineBinding.inflate(inflater, container, false);
        RoutineViewModel viewModel = new ViewModelProvider(this).get(RoutineViewModel.class);
        RoutineAdapter adapter = new RoutineAdapter(viewModel::toggleExpanded);
        binding.routineList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.routineList.setAdapter(adapter);
        viewModel.getRoutines().observe(getViewLifecycleOwner(), adapter::submitList);
        binding.createRoutineButton.setOnClickListener(view ->
                startActivity(new Intent(requireContext(), CreateRoutineActivity.class)));
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
