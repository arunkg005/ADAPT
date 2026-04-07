package com.example.adapt.ui.dashboard;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapt.R;
import com.example.adapt.data.model.ActivityLog;
import com.example.adapt.data.network.dto.ApiListResponse;
import com.example.adapt.data.network.dto.BackendAlert;
import com.example.adapt.utils.PrefManager;
import com.example.adapt.viewmodel.RoutineViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LogsFragment extends Fragment {

    private static final int ALERT_LIMIT = 30;

    private LogAdapter adapter;
    private RoutineViewModel viewModel;
    private PrefManager prefManager;
    private final List<ActivityLog> localLogs = new ArrayList<>();
    private final List<ActivityLog> backendLogs = new ArrayList<>();
    private final List<BackendAlert> backendAlerts = new ArrayList<>();
    private boolean backendFetchAttempted = false;
    private Button btnAcknowledgeAlert;
    private TextView tvAlertStatus;
    private boolean canAcknowledgeAlerts = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_logs, container, false);

        RecyclerView rvLogs = view.findViewById(R.id.rvLogs);
        rvLogs.setLayoutManager(new LinearLayoutManager(requireContext()));

        prefManager = new PrefManager(requireContext());
        String role = safeString(prefManager.getRole(), "patient").toLowerCase(Locale.US);
        canAcknowledgeAlerts = "caregiver".equals(role);

        adapter = new LogAdapter(new ArrayList<>());
        rvLogs.setAdapter(adapter);

        EditText etLogSearch = view.findViewById(R.id.etLogSearch);
        if (etLogSearch != null) {
            etLogSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // No-op.
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    adapter.search(s == null ? "" : s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // No-op.
                }
            });
        }

        btnAcknowledgeAlert = view.findViewById(R.id.btnAcknowledgeAlert);
        tvAlertStatus = view.findViewById(R.id.tvAlertStatus);

        if (btnAcknowledgeAlert != null) {
            btnAcknowledgeAlert.setOnClickListener(v -> acknowledgeNextAlert());
            btnAcknowledgeAlert.setVisibility(canAcknowledgeAlerts ? View.VISIBLE : View.GONE);
        }

        if (tvAlertStatus != null && !canAcknowledgeAlerts) {
            tvAlertStatus.setText("Alert acknowledgment requires a caregiver account.");
        }

        viewModel = new ViewModelProvider(this).get(RoutineViewModel.class);
        viewModel.getAllLogs().observe(getViewLifecycleOwner(), logs -> {
            if (logs != null && !logs.isEmpty()) {
                localLogs.clear();
                localLogs.addAll(logs);
                updateMergedLogs();
                return;
            }

            if (!prefManager.isActivityLogSeeded()) {
                prefManager.setActivityLogSeeded(true);
                seedInitialLogs();
            } else {
                localLogs.clear();
                updateMergedLogs();
            }
        });

        fetchBackendAlerts(false);

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

    private void fetchBackendAlerts(boolean forceRefresh) {
        if (backendFetchAttempted && !forceRefresh) {
            return;
        }

        backendFetchAttempted = true;
        viewModel.fetchAlerts(ALERT_LIMIT, 0).enqueue(new Callback<ApiListResponse<BackendAlert>>() {
            @Override
            public void onResponse(Call<ApiListResponse<BackendAlert>> call, Response<ApiListResponse<BackendAlert>> response) {
                if (!isAdded()) {
                    return;
                }

                backendLogs.clear();
                backendAlerts.clear();
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    backendAlerts.addAll(response.body().getData());
                    for (BackendAlert alert : response.body().getData()) {
                        backendLogs.add(mapAlertToLog(alert));
                    }
                }

                updateAlertActionState();
                updateMergedLogs();
            }

            @Override
            public void onFailure(Call<ApiListResponse<BackendAlert>> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }

                backendLogs.clear();
                backendAlerts.clear();
                updateAlertActionState();
                updateMergedLogs();
            }
        });
    }

    private void acknowledgeNextAlert() {
        if (!canAcknowledgeAlerts) {
            Toast.makeText(requireContext(), "Caregiver access is required to acknowledge alerts.", Toast.LENGTH_SHORT).show();
            return;
        }

        BackendAlert target = findNextUnacknowledgedAlert();
        if (target == null || target.getId() == null || target.getId().trim().isEmpty()) {
            Toast.makeText(requireContext(), "No pending backend alerts to acknowledge.", Toast.LENGTH_SHORT).show();
            updateAlertActionState();
            return;
        }

        if (btnAcknowledgeAlert != null) {
            btnAcknowledgeAlert.setEnabled(false);
            btnAcknowledgeAlert.setText("Acknowledging...");
        }

        viewModel.acknowledgeAlert(target.getId()).enqueue(new Callback<BackendAlert>() {
            @Override
            public void onResponse(Call<BackendAlert> call, Response<BackendAlert> response) {
                if (!isAdded()) {
                    return;
                }

                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Alert acknowledged.", Toast.LENGTH_SHORT).show();
                    fetchBackendAlerts(true);
                } else {
                    Toast.makeText(requireContext(), "Unable to acknowledge alert.", Toast.LENGTH_SHORT).show();
                    updateAlertActionState();
                }
            }

            @Override
            public void onFailure(Call<BackendAlert> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }

                Toast.makeText(requireContext(), "Alert acknowledgment failed.", Toast.LENGTH_SHORT).show();
                updateAlertActionState();
            }
        });
    }

    private BackendAlert findNextUnacknowledgedAlert() {
        BackendAlert latest = null;
        long latestTimestamp = Long.MIN_VALUE;

        for (BackendAlert alert : backendAlerts) {
            if (alert == null || alert.isAcknowledged()) {
                continue;
            }

            long timestamp = parseBackendTimestamp(alert.getCreatedAt());
            if (latest == null || timestamp > latestTimestamp) {
                latest = alert;
                latestTimestamp = timestamp;
            }
        }

        return latest;
    }

    private void updateAlertActionState() {
        if (!isAdded()) {
            return;
        }

        int unacknowledgedCount = 0;
        for (BackendAlert alert : backendAlerts) {
            if (alert != null && !alert.isAcknowledged()) {
                unacknowledgedCount++;
            }
        }

        if (tvAlertStatus != null) {
            if (!canAcknowledgeAlerts) {
                tvAlertStatus.setText("Alert acknowledgment requires a caregiver account.");
            } else if (unacknowledgedCount > 0) {
                tvAlertStatus.setText("Pending backend alerts: " + unacknowledgedCount);
            } else {
                tvAlertStatus.setText("No pending backend alerts.");
            }
        }

        if (btnAcknowledgeAlert != null) {
            btnAcknowledgeAlert.setText("Acknowledge Next Backend Alert");
            btnAcknowledgeAlert.setEnabled(canAcknowledgeAlerts && unacknowledgedCount > 0);
        }
    }

    private void updateMergedLogs() {
        List<ActivityLog> merged = new ArrayList<>(backendLogs.size() + localLogs.size());
        merged.addAll(backendLogs);
        merged.addAll(localLogs);
        merged.sort((first, second) -> Long.compare(second.getTimestamp(), first.getTimestamp()));
        adapter.setLogs(merged);
    }

    private ActivityLog mapAlertToLog(BackendAlert alert) {
        String severity = safeString(alert.getSeverity(), "UNKNOWN").toUpperCase(Locale.US);
        String taskName = safeString(alert.getTaskName(), "Care Alert");
        String taskState = safeString(alert.getTaskState(), "UNKNOWN");
        String primaryIssue = safeString(alert.getPrimaryIssue(), "Not specified");
        String assistanceMode = safeString(alert.getAssistanceMode(), "Not specified");
        String patientId = safeString(alert.getPatientId(), "Unknown patient");

        String title = severity + " - " + taskName;
        String description = "Alert event";
        String detailInfo =
                "Patient: " + patientId +
            "\nCategory: alert" +
                "\nState: " + taskState +
                "\nIssue: " + primaryIssue +
                "\nAssistance: " + assistanceMode +
                "\nAcknowledged: " + (alert.isAcknowledged() ? "Yes" : "No");

        long timestamp = parseBackendTimestamp(alert.getCreatedAt());
        ActivityLog.Type type = "NONE".equals(severity) ? ActivityLog.Type.INFO : ActivityLog.Type.WARNING;

        return new ActivityLog(
                title,
                description,
                timestamp,
                ActivityLog.Source.IOT,
                type,
                detailInfo
        );
    }

    private String safeString(String value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private long parseBackendTimestamp(String rawTimestamp) {
        if (rawTimestamp == null || rawTimestamp.trim().isEmpty()) {
            return System.currentTimeMillis();
        }

        String value = rawTimestamp.trim();
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd HH:mm:ssX",
                "yyyy-MM-dd HH:mm:ss"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                Date parsedDate = format.parse(value);
                if (parsedDate != null) {
                    return parsedDate.getTime();
                }
            } catch (ParseException ignored) {
                // Try next pattern.
            }
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return System.currentTimeMillis();
        }
    }

    private void seedInitialLogs() {
        for (ActivityLog log : buildInitialLogs()) {
            viewModel.insertActivityLog(log);
        }
    }

    private List<ActivityLog> buildInitialLogs() {
        List<ActivityLog> logs = new ArrayList<>();
        long now = System.currentTimeMillis();
        String patientLabel = safeString(prefManager.getActivePatientName(), "General");

        logs.add(new ActivityLog(
                "Morning Medication",
                "Recorded via Mobile App",
                now - 3600000,
                ActivityLog.Source.MOBILE,
                ActivityLog.Type.SUCCESS,
            "Patient: " + patientLabel +
                "\nCategory: task/routine" +
                "\nThe patient successfully confirmed taking their morning pills at 08:00 AM."
        ));

        logs.add(new ActivityLog(
                "Kitchen Activity",
                "IoT Surveillance",
                now - 7200000,
                ActivityLog.Source.IOT,
                ActivityLog.Type.INFO,
            "Patient: " + patientLabel +
                "\nCategory: iot/sensor" +
                "\nMovement detected in kitchen area for 15 minutes. Activity matches typical breakfast routine."
        ));

        logs.add(new ActivityLog(
                "Vitals Check",
                "Wearable Sync",
                now - 10800000,
                ActivityLog.Source.IOT,
                ActivityLog.Type.SUCCESS,
            "Patient: " + patientLabel +
                "\nCategory: iot/sensor" +
                "\nHeart rate: 72 BPM, Blood Oxygen: 98%. All vitals within normal parameters."
        ));

        logs.add(new ActivityLog(
                "Fall Detection System",
                "IoT Monitoring",
                now - 14400000,
                ActivityLog.Source.IOT,
                ActivityLog.Type.WARNING,
            "Patient: " + patientLabel +
                "\nCategory: alert" +
                "\nAbnormal movement detected. System auto-checked status. False alarm confirmed by patient."
        ));

        logs.add(new ActivityLog(
                "Walk Detected",
                "Smart Shoes IoT",
                now - 18000000,
                ActivityLog.Source.IOT,
                ActivityLog.Type.SUCCESS,
            "Patient: " + patientLabel +
                "\nCategory: task/routine" +
                "\nPatient completed a 500-step walk. Goal reached."
        ));

        return logs;
    }
}
