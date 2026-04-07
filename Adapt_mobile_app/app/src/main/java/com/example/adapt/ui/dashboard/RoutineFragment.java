package com.example.adapt.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapt.R;
import com.example.adapt.data.model.Routine;
import com.example.adapt.ui.task.TaskActivity;
import com.example.adapt.utils.PrefManager;
import com.example.adapt.viewmodel.RoutineViewModel;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class RoutineFragment extends Fragment {

    private RoutineViewModel viewModel;
    private RoutineAdapter adapter;
    private PrefManager prefManager;
    private boolean canManageRoutines;
    private TextView tvRoutineEmptyState;
    private RecyclerView rvRoutines;
    private FloatingActionButton fabAdd;
    private View layoutSchedulePanel;
    private TextView tvScheduleSummary;
    private boolean showingScheduleSection = false;
    private final List<Routine> cachedRoutines = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routine_list, container, false);

        prefManager = new PrefManager(requireContext());
        viewModel = new ViewModelProvider(this).get(RoutineViewModel.class);

        String role = prefManager.getRole();
        canManageRoutines = "caregiver".equals(role) || "admin".equals(role);

        tvRoutineEmptyState = view.findViewById(R.id.tvRoutineEmptyState);
        rvRoutines = view.findViewById(R.id.rvRoutines);
        fabAdd = view.findViewById(R.id.fabAddRoutine);
        layoutSchedulePanel = view.findViewById(R.id.layoutSchedulePanel);
        tvScheduleSummary = view.findViewById(R.id.tvScheduleSummary);

        Button btnStartAssistMode = view.findViewById(R.id.btnStartAssistMode);
        btnStartAssistMode.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AssistModeActivity.class);
            startActivity(intent);
        });

        Button btnOpenTaskLabFromSchedule = view.findViewById(R.id.btnOpenTaskLabFromSchedule);
        btnOpenTaskLabFromSchedule.setOnClickListener(v -> openTaskLabPlanner());

        MaterialButtonToggleGroup sectionToggle = view.findViewById(R.id.toggleTaskSections);
        sectionToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }

            showingScheduleSection = checkedId == R.id.btnSectionSchedule;
            applySectionMode();
        });

        rvRoutines.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new RoutineAdapter(new RoutineAdapter.OnRoutineClickListener() {
            @Override
            public void onStartClick(Routine routine) {
                Intent intent = new Intent(getActivity(), TaskActivity.class);
                intent.putExtra("ROUTINE_ID", routine.getId());
                intent.putExtra("ROUTINE_TITLE", routine.getTitle());
                startActivity(intent);
            }
        });
        adapter.setUserRole(prefManager.getRole());

        rvRoutines.setAdapter(adapter);

        viewModel.getAllRoutines().observe(getViewLifecycleOwner(), routines -> {
            if (routines != null && !routines.isEmpty()) {
                cachedRoutines.clear();
                cachedRoutines.addAll(routines);
                adapter.setRoutines(routines);
            } else if (!prefManager.isRoutineSeeded()) {
                cachedRoutines.clear();
                addInitialData();
                prefManager.setRoutineSeeded(true);
            } else {
                cachedRoutines.clear();
                adapter.setRoutines(new ArrayList<>());
            }

            updateScheduleSummary();
            applySectionMode();
        });

        if (canManageRoutines) {
            fabAdd.setVisibility(View.VISIBLE);
            fabAdd.setOnClickListener(v -> showCreateRoutineDialog());
        } else {
            fabAdd.setVisibility(View.GONE);
        }

        updateScheduleSummary();
        applySectionMode();

        return view;
    }

    private void applySectionMode() {
        if (showingScheduleSection) {
            if (layoutSchedulePanel != null) {
                layoutSchedulePanel.setVisibility(View.VISIBLE);
            }

            if (rvRoutines != null) {
                rvRoutines.setVisibility(View.GONE);
            }

            if (tvRoutineEmptyState != null) {
                if (cachedRoutines.isEmpty()) {
                    tvRoutineEmptyState.setText("No routines scheduled yet. Use Task Lab Planner to create one.");
                    tvRoutineEmptyState.setVisibility(View.VISIBLE);
                } else {
                    tvRoutineEmptyState.setVisibility(View.GONE);
                }
            }

            if (fabAdd != null) {
                fabAdd.setVisibility(View.GONE);
            }
            return;
        }

        if (layoutSchedulePanel != null) {
            layoutSchedulePanel.setVisibility(View.GONE);
        }

        if (rvRoutines != null) {
            rvRoutines.setVisibility(View.VISIBLE);
        }

        if (cachedRoutines.isEmpty()) {
            tvRoutineEmptyState.setText(canManageRoutines
                    ? "No routines available yet. Use + to create one."
                    : "No routines available yet. Ask your caregiver to create one.");
            tvRoutineEmptyState.setVisibility(View.VISIBLE);
        } else {
            tvRoutineEmptyState.setVisibility(View.GONE);
        }

        if (fabAdd != null) {
            fabAdd.setVisibility(canManageRoutines ? View.VISIBLE : View.GONE);
        }
    }

    private void updateScheduleSummary() {
        if (tvScheduleSummary == null) {
            return;
        }

        if (cachedRoutines.isEmpty()) {
            tvScheduleSummary.setText("No routines scheduled yet. Open Task Lab Planner to generate routines with templates or AI.");
            return;
        }

        StringBuilder summaryBuilder = new StringBuilder();
        int limit = Math.min(cachedRoutines.size(), 6);
        for (int i = 0; i < limit; i++) {
            Routine routine = cachedRoutines.get(i);
            String title = valueOrFallback(routine.getTitle(), "Routine");
            String time = valueOrFallback(routine.getScheduledTime(), "--");
            summaryBuilder.append(i + 1)
                    .append(". ")
                    .append(title)
                    .append(" at ")
                    .append(time)
                    .append("\n");
        }

        if (cachedRoutines.size() > limit) {
            summaryBuilder.append("+")
                    .append(cachedRoutines.size() - limit)
                    .append(" more routine(s)");
        }

        tvScheduleSummary.setText(summaryBuilder.toString().trim());
    }

    private String valueOrFallback(String value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private void openTaskLabPlanner() {
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new TaskLabFragment())
                .commit();
    }

    private void showCreateRoutineDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_routine, null, false);
        TextInputEditText etRoutineTitle = dialogView.findViewById(R.id.etRoutineTitle);
        TextInputEditText etRoutineDescription = dialogView.findViewById(R.id.etRoutineDescription);
        TextInputEditText etRoutineTime = dialogView.findViewById(R.id.etRoutineTime);
        etRoutineTime.setText("08:00 AM");

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Create Routine")
                .setView(dialogView)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .setPositiveButton("Create", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button btnCreate = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnCreate.setOnClickListener(v -> {
                String title = valueOrEmpty(etRoutineTitle.getText());
                String description = valueOrEmpty(etRoutineDescription.getText());
                String time = valueOrEmpty(etRoutineTime.getText());

                if (title.isEmpty()) {
                    etRoutineTitle.setError("Title is required");
                    etRoutineTitle.requestFocus();
                    return;
                }

                if (description.isEmpty()) {
                    description = "Guided routine plan";
                }

                if (time.isEmpty()) {
                    time = "08:00 AM";
                }

                viewModel.createRoutinePlan(title, description, time);
                Toast.makeText(requireContext(), "Routine created with starter tasks", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private String valueOrEmpty(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private void addInitialData() {
        viewModel.createRoutinePlan("Morning Routine", "Sequence: Wake up, Meds, Breakfast", "08:00 AM");
        viewModel.createRoutinePlan("Health Check", "Wearable sync and vitals log", "11:00 AM");
        viewModel.createRoutinePlan("Evening Wind-down", "Low sensory activity and light stretches", "09:00 PM");
    }
}
