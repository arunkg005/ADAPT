package com.example.adapt.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.example.adapt.data.network.dto.ApiListResponse;
import com.example.adapt.data.network.dto.BackendAlert;
import com.example.adapt.data.network.dto.BackendDevice;
import com.example.adapt.data.network.dto.BackendPatient;
import com.example.adapt.data.network.dto.DeviceCreateRequest;
import com.example.adapt.data.network.dto.PatientCreateRequest;
import com.example.adapt.utils.PrefManager;
import com.example.adapt.viewmodel.RoutineViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatusFragment extends Fragment {
    private static final int PATIENT_LIMIT = 200;
    private static final int ALERT_LIMIT = 100;
    private static final int DEVICE_LIMIT = 200;

    private TextView tvMetricOneValue;
    private TextView tvMetricOneLabel;
    private TextView tvMetricTwoValue;
    private TextView tvMetricTwoLabel;
    private TextView tvMetricThreeValue;
    private TextView tvMetricThreeLabel;
    private TextView tvMetricFourValue;
    private TextView tvMetricFourLabel;
    private TextView tvWeeklySummary;
    private TextView tvHeroTitle;
    private TextView tvHeroSubtitle;
    private TextView tvWorkspaceHint;
    private TextView tvWorkspaceEmpty;
    private Button btnAddPatient;
    private RecyclerView rvPatientWorkspace;

    private RoutineViewModel viewModel;
    private PrefManager prefManager;
    private PatientWorkspaceAdapter patientWorkspaceAdapter;
    private final List<BackendPatient> workspacePatients = new ArrayList<>();
    private final List<BackendDevice> workspaceDevices = new ArrayList<>();
    private boolean canOnboardPatients = false;

    private int patientCount = 0;
    private int highRiskPatientCount = 0;
    private int openAlertCount = 0;
    private int recentAlertCount = 0;
    private int connectedDeviceCount = 0;

    private boolean patientLoadCompleted = false;
    private boolean alertLoadCompleted = false;
    private boolean deviceLoadCompleted = false;

    private boolean patientLoadFailed = false;
    private boolean alertLoadFailed = false;
    private boolean deviceLoadFailed = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_status, container, false);

        bindViews(view);
        viewModel = new ViewModelProvider(this).get(RoutineViewModel.class);
        prefManager = new PrefManager(requireContext());

        initWorkspaceSection();
        applyDefaultMetrics();
        refreshBackendSnapshot();

        Button btnViewDetails = view.findViewById(R.id.btnViewDetails);
        btnViewDetails.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DetailsActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (prefManager != null) {
            tvMetricFourValue.setText(prefManager.getLatestDiagnosticSeverity());
            updateMetrics();
        }
    }

    private void bindViews(View view) {
        tvMetricOneValue = view.findViewById(R.id.tvMetricOneValue);
        tvMetricOneLabel = view.findViewById(R.id.tvMetricOneLabel);
        tvMetricTwoValue = view.findViewById(R.id.tvMetricTwoValue);
        tvMetricTwoLabel = view.findViewById(R.id.tvMetricTwoLabel);
        tvMetricThreeValue = view.findViewById(R.id.tvMetricThreeValue);
        tvMetricThreeLabel = view.findViewById(R.id.tvMetricThreeLabel);
        tvMetricFourValue = view.findViewById(R.id.tvMetricFourValue);
        tvMetricFourLabel = view.findViewById(R.id.tvMetricFourLabel);
        tvWeeklySummary = view.findViewById(R.id.tvWeeklySummary);
        tvHeroTitle = view.findViewById(R.id.tvHeroTitle);
        tvHeroSubtitle = view.findViewById(R.id.tvHeroSubtitle);
        tvWorkspaceHint = view.findViewById(R.id.tvWorkspaceHint);
        tvWorkspaceEmpty = view.findViewById(R.id.tvWorkspaceEmpty);
        btnAddPatient = view.findViewById(R.id.btnAddPatient);
        rvPatientWorkspace = view.findViewById(R.id.rvPatientWorkspace);
    }

    private void initWorkspaceSection() {
        String role = safeString(prefManager.getRole(), "patient").toLowerCase(Locale.US);
        canOnboardPatients = "caregiver".equals(role) || "admin".equals(role);

        String userName = safeString(prefManager.getUserName(), "Care Team");
        if (tvHeroTitle != null) {
            tvHeroTitle.setText("Welcome back, " + userName);
        }

        if (tvHeroSubtitle != null) {
            tvHeroSubtitle.setText(canOnboardPatients
                    ? "You can onboard patients and monitor live sensor snapshots from this workspace."
                    : "You are in read-only workspace mode. Ask your caregiver/admin to onboard patients.");
        }

        if (rvPatientWorkspace != null) {
            rvPatientWorkspace.setLayoutManager(new LinearLayoutManager(requireContext()));
            patientWorkspaceAdapter = new PatientWorkspaceAdapter(this::openPatientAnalysis);
            rvPatientWorkspace.setAdapter(patientWorkspaceAdapter);
        }

        if (btnAddPatient != null) {
            btnAddPatient.setVisibility(canOnboardPatients ? View.VISIBLE : View.GONE);
            btnAddPatient.setOnClickListener(v -> showAddPatientDialog());
        }

        updateWorkspaceSection();
    }

    private void applyDefaultMetrics() {
        tvMetricOneValue.setText("--");
        tvMetricOneLabel.setText("Patients");

        tvMetricTwoValue.setText("--");
        tvMetricTwoLabel.setText("Connected Devices");

        tvMetricThreeValue.setText("--");
        tvMetricThreeLabel.setText("Open Alerts");

        tvMetricFourValue.setText(prefManager.getLatestDiagnosticSeverity());
        tvMetricFourLabel.setText("Engine Severity");

        tvWeeklySummary.setText("Fetching backend snapshot...");
    }

    private void refreshBackendSnapshot() {
        patientLoadCompleted = false;
        alertLoadCompleted = false;
        deviceLoadCompleted = false;
        patientLoadFailed = false;
        alertLoadFailed = false;
        deviceLoadFailed = false;
        fetchBackendSnapshot();
    }

    private void fetchBackendSnapshot() {
        viewModel.fetchPatients(PATIENT_LIMIT, 0).enqueue(new Callback<ApiListResponse<BackendPatient>>() {
            @Override
            public void onResponse(Call<ApiListResponse<BackendPatient>> call, Response<ApiListResponse<BackendPatient>> response) {
                patientLoadCompleted = true;
                patientLoadFailed = !response.isSuccessful();

                if (!patientLoadFailed && response.body() != null) {
                    List<BackendPatient> patients = response.body().getData();
                    workspacePatients.clear();
                    if (patients != null) {
                        workspacePatients.addAll(patients);
                    }

                    patientCount = patients != null ? patients.size() : 0;
                    highRiskPatientCount = countHighRiskPatients(patients);

                    BackendPatient selected = pickActivePatient(patients);
                    if (selected != null && selected.getId() != null) {
                        prefManager.setActivePatient(
                                selected.getId(),
                                formatPatientName(selected),
                                selected.getRiskLevel() == null ? "MEDIUM" : selected.getRiskLevel()
                        );
                    }
                }

                if (patientLoadFailed) {
                    workspacePatients.clear();
                }

                updateWorkspaceSection();
                updateMetrics();
            }

            @Override
            public void onFailure(Call<ApiListResponse<BackendPatient>> call, Throwable t) {
                patientLoadCompleted = true;
                patientLoadFailed = true;
                workspacePatients.clear();
                updateWorkspaceSection();
                updateMetrics();
            }
        });

        viewModel.fetchAlerts(ALERT_LIMIT, 0).enqueue(new Callback<ApiListResponse<BackendAlert>>() {
            @Override
            public void onResponse(Call<ApiListResponse<BackendAlert>> call, Response<ApiListResponse<BackendAlert>> response) {
                alertLoadCompleted = true;
                alertLoadFailed = !response.isSuccessful();

                if (!alertLoadFailed && response.body() != null) {
                    List<BackendAlert> alerts = response.body().getData();
                    recentAlertCount = alerts != null ? alerts.size() : 0;
                    openAlertCount = Math.max(response.body().getTotal(), 0);
                }

                updateMetrics();
            }

            @Override
            public void onFailure(Call<ApiListResponse<BackendAlert>> call, Throwable t) {
                alertLoadCompleted = true;
                alertLoadFailed = true;
                updateMetrics();
            }
        });

        viewModel.fetchDevices(DEVICE_LIMIT, 0).enqueue(new Callback<ApiListResponse<BackendDevice>>() {
            @Override
            public void onResponse(Call<ApiListResponse<BackendDevice>> call, Response<ApiListResponse<BackendDevice>> response) {
                deviceLoadCompleted = true;
                deviceLoadFailed = !response.isSuccessful();

                if (!deviceLoadFailed && response.body() != null) {
                    workspaceDevices.clear();
                    if (response.body().getData() != null) {
                        workspaceDevices.addAll(response.body().getData());
                    }

                    connectedDeviceCount = countOnlineDevices(response.body().getData());
                    prefManager.setConnectedDevicesCount(connectedDeviceCount);
                } else {
                    workspaceDevices.clear();
                }

                updateWorkspaceSection();
                updateMetrics();
            }

            @Override
            public void onFailure(Call<ApiListResponse<BackendDevice>> call, Throwable t) {
                deviceLoadCompleted = true;
                deviceLoadFailed = true;
                workspaceDevices.clear();
                updateWorkspaceSection();
                updateMetrics();
            }
        });
    }

    private void updateWorkspaceSection() {
        if (!isAdded()) {
            return;
        }

        if (patientWorkspaceAdapter != null) {
            patientWorkspaceAdapter.setWorkspaceData(workspacePatients, buildOnlineDeviceMap(workspaceDevices));
        }

        if (tvWorkspaceHint != null) {
            if (patientLoadFailed) {
                tvWorkspaceHint.setText("Unable to load patient workspace from cloud.");
            } else {
                tvWorkspaceHint.setText(String.format(
                        Locale.US,
                        "Workspace: %d patient(s), %d connected device(s)",
                        workspacePatients.size(),
                        connectedDeviceCount
                ));
            }
        }

        if (tvWorkspaceEmpty != null) {
            if (workspacePatients.isEmpty()) {
                tvWorkspaceEmpty.setVisibility(View.VISIBLE);
                tvWorkspaceEmpty.setText(canOnboardPatients
                        ? "No patients yet. Use Add Patient Workspace to onboard someone now."
                        : "No patients assigned to your workspace yet.");
            } else {
                tvWorkspaceEmpty.setVisibility(View.GONE);
            }
        }
    }

    private Map<String, Integer> buildOnlineDeviceMap(List<BackendDevice> devices) {
        Map<String, Integer> map = new HashMap<>();
        if (devices == null) {
            return map;
        }

        for (BackendDevice device : devices) {
            if (device == null || !device.isOnline()) {
                continue;
            }

            String patientId = safeString(device.getPatientId(), "");
            if (patientId.isEmpty()) {
                continue;
            }

            int current = map.containsKey(patientId) ? map.get(patientId) : 0;
            map.put(patientId, current + 1);
        }

        return map;
    }

    private void showAddPatientDialog() {
        if (!canOnboardPatients) {
            Toast.makeText(requireContext(), "Your role does not allow onboarding.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_patient_workspace, null, false);

        EditText etFirstName = dialogView.findViewById(R.id.etPatientFirstName);
        EditText etLastName = dialogView.findViewById(R.id.etPatientLastName);
        EditText etCondition = dialogView.findViewById(R.id.etPatientCondition);
        EditText etRisk = dialogView.findViewById(R.id.etPatientRisk);
        EditText etDeviceName = dialogView.findViewById(R.id.etDeviceName);
        EditText etDeviceType = dialogView.findViewById(R.id.etDeviceType);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Patient Workspace")
                .setView(dialogView)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .setPositiveButton("Create", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button btnCreate = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnCreate.setOnClickListener(v -> {
                String firstName = valueOrEmpty(etFirstName.getText());
                String lastName = valueOrEmpty(etLastName.getText());
                String condition = valueOrEmpty(etCondition.getText());
                String risk = normalizeRisk(valueOrEmpty(etRisk.getText()));
                String deviceName = valueOrEmpty(etDeviceName.getText());
                String deviceType = valueOrEmpty(etDeviceType.getText());

                if (firstName.isEmpty()) {
                    etFirstName.setError("First name is required");
                    etFirstName.requestFocus();
                    return;
                }

                if (lastName.isEmpty()) {
                    etLastName.setError("Last name is required");
                    etLastName.requestFocus();
                    return;
                }

                if (deviceType.isEmpty()) {
                    deviceType = "wearable";
                }

                PatientCreateRequest request = new PatientCreateRequest(
                        firstName,
                        lastName,
                        condition.isEmpty() ? null : condition,
                        risk,
                        3000
                );

                btnCreate.setEnabled(false);
                createPatientWorkspace(request, deviceName, deviceType, dialog, btnCreate);
            });
        });

        dialog.show();
    }

    private void createPatientWorkspace(
            PatientCreateRequest request,
            String deviceName,
            String deviceType,
            AlertDialog dialog,
            Button btnCreate
    ) {
        viewModel.createPatient(request).enqueue(new Callback<BackendPatient>() {
            @Override
            public void onResponse(Call<BackendPatient> call, Response<BackendPatient> response) {
                if (!isAdded()) {
                    return;
                }

                if (!response.isSuccessful() || response.body() == null) {
                    btnCreate.setEnabled(true);
                    Toast.makeText(requireContext(), "Unable to add patient workspace.", Toast.LENGTH_SHORT).show();
                    return;
                }

                BackendPatient createdPatient = response.body();
                if (createdPatient.getId() == null || createdPatient.getId().trim().isEmpty() || deviceName.isEmpty()) {
                    Toast.makeText(requireContext(), "Patient workspace added.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    refreshBackendSnapshot();
                    return;
                }

                DeviceCreateRequest deviceRequest = new DeviceCreateRequest(
                        createdPatient.getId().trim(),
                        deviceName,
                        deviceType,
                        "android",
                        Collections.singletonList("sensor_stream")
                );

                viewModel.createDevice(deviceRequest).enqueue(new Callback<BackendDevice>() {
                    @Override
                    public void onResponse(Call<BackendDevice> call, Response<BackendDevice> response) {
                        if (!isAdded()) {
                            return;
                        }

                        Toast.makeText(
                                requireContext(),
                                response.isSuccessful() ? "Patient and device linked." : "Patient added. Device link failed.",
                                Toast.LENGTH_SHORT
                        ).show();
                        dialog.dismiss();
                        refreshBackendSnapshot();
                    }

                    @Override
                    public void onFailure(Call<BackendDevice> call, Throwable t) {
                        if (!isAdded()) {
                            return;
                        }

                        Toast.makeText(requireContext(), "Patient added. Device link failed.", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        refreshBackendSnapshot();
                    }
                });
            }

            @Override
            public void onFailure(Call<BackendPatient> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }

                btnCreate.setEnabled(true);
                Toast.makeText(requireContext(), "Patient onboarding failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String valueOrEmpty(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private String normalizeRisk(String riskInput) {
        String value = safeString(riskInput, "MEDIUM").toUpperCase(Locale.US);
        if ("LOW".equals(value) || "MEDIUM".equals(value) || "HIGH".equals(value)) {
            return value;
        }
        return "MEDIUM";
    }

    private void openPatientAnalysis(BackendPatient patient) {
        Intent intent = new Intent(getActivity(), DetailsActivity.class);
        intent.putExtra("PATIENT_ID", safeString(patient.getId(), ""));
        intent.putExtra("PATIENT_NAME", formatPatientName(patient));
        intent.putExtra("PATIENT_RISK", normalizeRisk(safeString(patient.getRiskLevel(), "MEDIUM")));
        startActivity(intent);
    }

    private int countHighRiskPatients(List<BackendPatient> patients) {
        if (patients == null || patients.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (BackendPatient patient : patients) {
            String risk = patient.getRiskLevel();
            if (risk != null && "HIGH".equalsIgnoreCase(risk.trim())) {
                count++;
            }
        }

        return count;
    }

    private void updateMetrics() {
        if (!isAdded()) {
            return;
        }

        if (!patientLoadFailed) {
            tvMetricOneValue.setText(String.valueOf(patientCount));
        }

        if (!deviceLoadFailed) {
            tvMetricTwoValue.setText(String.valueOf(connectedDeviceCount));
        }

        if (!alertLoadFailed) {
            tvMetricThreeValue.setText(String.valueOf(openAlertCount));
        }

        tvMetricFourValue.setText(prefManager.getLatestDiagnosticSeverity());

        if (!patientLoadCompleted || !alertLoadCompleted || !deviceLoadCompleted) {
            return;
        }

        if (patientLoadFailed && alertLoadFailed && deviceLoadFailed) {
            tvWeeklySummary.setText("Backend snapshot unavailable. Showing offline baseline values.");
            return;
        }

        String diagnosticSummary = prefManager.getLatestDiagnosticSummary();

        if (patientLoadFailed) {
            tvWeeklySummary.setText(String.format(Locale.US,
                    "Diagnostics active. Alerts synced: %d open alerts, %d recent entries loaded.\n%s",
                    openAlertCount,
                    recentAlertCount,
                    diagnosticSummary));
            return;
        }

        if (alertLoadFailed) {
            tvWeeklySummary.setText(String.format(Locale.US,
                    "Patients synced: %d total, %d high risk, %d connected devices.\n%s",
                    patientCount,
                    highRiskPatientCount,
                    connectedDeviceCount,
                    diagnosticSummary));
            return;
        }

        tvWeeklySummary.setText(String.format(Locale.US,
                "Cloud sync complete: %d patients, %d high risk, %d connected devices, %d open alerts.\n%s",
                patientCount,
                highRiskPatientCount,
                connectedDeviceCount,
                openAlertCount,
                diagnosticSummary));
    }

    private int countOnlineDevices(List<BackendDevice> devices) {
        if (devices == null || devices.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (BackendDevice device : devices) {
            if (device != null && device.isOnline()) {
                count++;
            }
        }

        return count;
    }

    private BackendPatient pickActivePatient(List<BackendPatient> patients) {
        if (patients == null || patients.isEmpty()) {
            return null;
        }

        return patients.get(0);
    }

    private String formatPatientName(BackendPatient patient) {
        if (patient == null) {
            return "Patient";
        }

        String first = safeString(patient.getFirstName(), "");
        String last = safeString(patient.getLastName(), "");
        String full = (first + " " + last).trim();
        return full.isEmpty() ? "Patient" : full;
    }

    private String safeString(String value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}
