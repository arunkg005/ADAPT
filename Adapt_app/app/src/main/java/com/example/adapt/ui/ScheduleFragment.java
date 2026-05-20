package com.example.adapt.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.adapt.adapter.ScheduleAdapter;
import com.example.adapt.databinding.FragmentScheduleBinding;
import com.example.adapt.model.CareTask;
import com.example.adapt.viewmodel.ScheduleViewModel;

public class ScheduleFragment extends Fragment implements ScheduleAdapter.OnScheduleItemClickListener {

    private FragmentScheduleBinding binding;
    private ScheduleViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentScheduleBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(ScheduleViewModel.class);
        
        ScheduleAdapter adapter = new ScheduleAdapter(this);
        binding.scheduleList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.scheduleList.setAdapter(adapter);

        viewModel.getSchedule().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                adapter.submitList(items);
            }
        });

        // Setup day selectors (simplified for demo)
        setupDaySelectors();

        return binding.getRoot();
    }

    private void setupDaySelectors() {
        binding.day1.setOnClickListener(v -> viewModel.selectDay("MON 12"));
        binding.day2.setOnClickListener(v -> viewModel.selectDay("TUE 13"));
        binding.day3.setOnClickListener(v -> viewModel.selectDay("WED 14"));
        binding.day4.setOnClickListener(v -> viewModel.selectDay("THU 15"));
        binding.day5.setOnClickListener(v -> viewModel.selectDay("FRI 16"));
    }

    @Override
    public void onScheduleItemClick(CareTask item, int position) {
        Toast.makeText(requireContext(), "Item: " + item.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStartTaskClick(CareTask item, int position) {
        Toast.makeText(requireContext(), "Starting: " + item.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
