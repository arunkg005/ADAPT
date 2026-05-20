package com.example.adapt.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapt.R;
import com.example.adapt.databinding.ItemPatientBinding;
import com.example.adapt.model.Patient;

import java.util.ArrayList;
import java.util.List;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {
    public interface OnPatientClickListener {
        void onPatientClick(Patient patient);
    }

    private final List<Patient> patients = new ArrayList<>();
    private final OnPatientClickListener listener;

    public PatientAdapter(OnPatientClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Patient> nextPatients) {
        patients.clear();
        patients.addAll(nextPatients);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPatientBinding binding = ItemPatientBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PatientViewHolder(binding);
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
        private final ItemPatientBinding binding;

        PatientViewHolder(ItemPatientBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Patient patient) {
            binding.patientInitials.setText(patient.getInitials());
            binding.patientName.setText(patient.getName());
            binding.patientMeta.setText(patient.getAge() + " yrs - " + patient.getGender());
            binding.conditionContainer.removeAllViews();
            
            String allergies = patient.getAllergies();
            if (allergies != null && !allergies.isEmpty()) {
                for (String condition : allergies.split(",")) {
                    TextView chip = new TextView(binding.getRoot().getContext());
                    chip.setText(condition.trim());
                    chip.setTextColor(binding.getRoot().getContext().getColor(R.color.adapt_on_primary_soft));
                    chip.setTextSize(12);
                    chip.setBackgroundResource(R.drawable.bg_chip);
                    chip.setPadding(18, 8, 18, 8);
                    LinearLayoutCompat.addChip(binding.conditionContainer, chip);
                }
            }
            binding.getRoot().setOnClickListener(view -> listener.onPatientClick(patient));
        }
    }

    private static class LinearLayoutCompat {
        static void addChip(ViewGroup container, TextView chip) {
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 10, 0);
            container.addView(chip, params);
        }
    }
}
