package com.example.adapt.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapt.R;
import com.example.adapt.data.model.ActivityLog;
import com.example.adapt.utils.PrefManager;
import com.example.adapt.viewmodel.RoutineViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class LogsFragment extends Fragment {

    private LogAdapter adapter;
    private RoutineViewModel viewModel;
    private PrefManager prefManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_logs, container, false);

        RecyclerView rvLogs = view.findViewById(R.id.rvLogs);
        rvLogs.setLayoutManager(new LinearLayoutManager(requireContext()));

        prefManager = new PrefManager(requireContext());
        adapter = new LogAdapter(new ArrayList<>());
        rvLogs.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(RoutineViewModel.class);
        viewModel.getAllLogs().observe(getViewLifecycleOwner(), logs -> {
            if (logs != null && !logs.isEmpty()) {
                adapter.setLogs(logs);
                return;
            }

            if (!prefManager.isActivityLogSeeded()) {
                prefManager.setActivityLogSeeded(true);
                seedInitialLogs();
            } else {
                adapter.setLogs(new ArrayList<>());
            }
        });

        ChipGroup chipGroup = view.findViewById(R.id.chipGroup);
        if (chipGroup != null) {
            chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (!checkedIds.isEmpty()) {
                    Chip chip = group.findViewById(checkedIds.get(0));
                    if (chip != null) {
                        adapter.filter(chip.getText().toString());
                    }
                }
            });
        }

        return view;
    }

    private void seedInitialLogs() {
        for (ActivityLog log : buildInitialLogs()) {
            viewModel.insertActivityLog(log);
        }
    }

    private List<ActivityLog> buildInitialLogs() {
        List<ActivityLog> logs = new ArrayList<>();
        long now = System.currentTimeMillis();

        logs.add(new ActivityLog(
                "Morning Medication",
                "Recorded via Mobile App",
                now - 3600000,
                ActivityLog.Source.MOBILE,
                ActivityLog.Type.SUCCESS,
                "The patient successfully confirmed taking their morning pills at 08:00 AM."
        ));

        logs.add(new ActivityLog(
                "Kitchen Activity",
                "IoT Surveillance",
                now - 7200000,
                ActivityLog.Source.IOT,
                ActivityLog.Type.INFO,
                "Movement detected in kitchen area for 15 minutes. Activity matches typical breakfast routine."
        ));

        logs.add(new ActivityLog(
                "Vitals Check",
                "Wearable Sync",
                now - 10800000,
                ActivityLog.Source.IOT,
                ActivityLog.Type.SUCCESS,
                "Heart rate: 72 BPM, Blood Oxygen: 98%. All vitals within normal parameters."
        ));

        logs.add(new ActivityLog(
                "Fall Detection System",
                "IoT Monitoring",
                now - 14400000,
                ActivityLog.Source.IOT,
                ActivityLog.Type.WARNING,
                "Abnormal movement detected. System auto-checked status. False alarm confirmed by patient."
        ));

        logs.add(new ActivityLog(
                "Walk Detected",
                "Smart Shoes IoT",
                now - 18000000,
                ActivityLog.Source.IOT,
                ActivityLog.Type.SUCCESS,
                "Patient completed a 500-step walk. Goal reached."
        ));

        return logs;
    }
}
