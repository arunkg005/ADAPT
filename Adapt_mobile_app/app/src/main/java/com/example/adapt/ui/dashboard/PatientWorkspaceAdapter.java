package com.example.adapt.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapt.R;
import com.example.adapt.data.network.dto.BackendPatient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PatientWorkspaceAdapter extends RecyclerView.Adapter<PatientWorkspaceAdapter.PatientViewHolder> {

    public interface OnPatientActionListener {
        void onAnalyze(BackendPatient patient);
    }

    private final List<BackendPatient> patients = new ArrayList<>();
    private final Map<String, Integer> onlineDevicesByPatientId = new HashMap<>();
    private final OnPatientActionListener listener;

    public PatientWorkspaceAdapter(OnPatientActionListener listener) {
        this.listener = listener;
    }

    public void setWorkspaceData(List<BackendPatient> nextPatients, Map<String, Integer> nextOnlineDevicesByPatientId) {
        patients.clear();
        if (nextPatients != null) {
            patients.addAll(nextPatients);
        }

        onlineDevicesByPatientId.clear();
        if (nextOnlineDevicesByPatientId != null) {
            onlineDevicesByPatientId.putAll(nextOnlineDevicesByPatientId);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient_workspace, parent, false);
        return new PatientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
        holder.bind(patients.get(position));
    }

    @Override
    public int getItemCount() {
        return patients.size();
    }

    class PatientViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvPatientName;
        private final TextView tvRiskBadge;
        private final TextView tvCondition;
        private final TextView tvSnapshot;
        private final Button btnAnalyze;
        private final View cardLiveFeed;
        private final TextView tvLiveStatus;
        private final TextView tvLiveMetric;

        PatientViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tvWorkspacePatientName);
            tvRiskBadge = itemView.findViewById(R.id.tvWorkspaceRiskBadge);
            tvCondition = itemView.findViewById(R.id.tvWorkspaceCondition);
            tvSnapshot = itemView.findViewById(R.id.tvWorkspaceSnapshot);
            btnAnalyze = itemView.findViewById(R.id.btnWorkspaceAnalyze);
            cardLiveFeed = itemView.findViewById(R.id.cardLiveFeed);
            tvLiveStatus = itemView.findViewById(R.id.tvLiveStatus);
            tvLiveMetric = itemView.findViewById(R.id.tvLiveMetric);
        }

        void bind(BackendPatient patient) {
            String firstName = patient.getFirstName() == null ? "" : patient.getFirstName().trim();
            String lastName = patient.getLastName() == null ? "" : patient.getLastName().trim();
            String fullName = (firstName + " " + lastName).trim();
            if (fullName.isEmpty()) {
                fullName = "Patient";
            }

            String risk = patient.getRiskLevel() == null ? "MEDIUM" : patient.getRiskLevel().trim().toUpperCase(Locale.US);
            if (!"LOW".equals(risk) && !"MEDIUM".equals(risk) && !"HIGH".equals(risk)) {
                risk = "MEDIUM";
            }

            String condition = patient.getCognitiveCondition() == null || patient.getCognitiveCondition().trim().isEmpty()
                    ? "Condition: Not specified"
                    : "Condition: " + patient.getCognitiveCondition().trim();

            int onlineDevices = 0;
            String patientId = patient.getId();
            if (patientId != null && onlineDevicesByPatientId.containsKey(patientId)) {
                onlineDevices = Math.max(0, onlineDevicesByPatientId.get(patientId));
            }

            tvPatientName.setText(fullName);
            tvRiskBadge.setText(risk + " RISK");
            tvCondition.setText(condition);
            tvSnapshot.setText(String.format(Locale.US, "Connected devices: %d", onlineDevices));

            // Show Live Feed only if devices are connected
            if (onlineDevices > 0) {
                cardLiveFeed.setVisibility(View.VISIBLE);
                // Simulated live data logic
                tvLiveStatus.setText("Receiving live data...");
                tvLiveMetric.setText("Heart Rate: 72 bpm | Status: Stable");
            } else {
                cardLiveFeed.setVisibility(View.GONE);
            }

            btnAnalyze.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAnalyze(patient);
                }
            });
        }
    }
}
