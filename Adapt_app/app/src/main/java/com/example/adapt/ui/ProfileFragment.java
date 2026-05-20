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

import com.example.adapt.adapter.PatientAdapter;
import com.example.adapt.databinding.FragmentProfileBinding;
import com.example.adapt.model.Patient;
import com.example.adapt.utils.PatientIntentKeys;
import com.example.adapt.viewmodel.PatientViewModel;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        PatientViewModel viewModel = new ViewModelProvider(this).get(PatientViewModel.class);
        PatientAdapter adapter = new PatientAdapter(this::openPatientDetail);
        binding.patientList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.patientList.setAdapter(adapter);
        
        viewModel.getPatients().observe(getViewLifecycleOwner(), patients -> {
            if (patients != null) {
                adapter.submitList(patients);
            }
        });

        binding.addPatientButton.setOnClickListener(v -> openAddPatient());
        
        return binding.getRoot();
    }

    private void openAddPatient() {
        Intent intent = new Intent(requireContext(), EditPatientActivity.class);
        intent.putExtra("EXTRA_IS_NEW", true);
        startActivity(intent);
    }

    private void openPatientDetail(Patient patient) {
        Intent intent = new Intent(requireContext(), PatientDetailActivity.class);
        intent.putExtra(PatientIntentKeys.EXTRA_ID, patient.getId());
        intent.putExtra(PatientIntentKeys.EXTRA_NAME, patient.getName());
        intent.putExtra(PatientIntentKeys.EXTRA_AGE, patient.getAge());
        intent.putExtra(PatientIntentKeys.EXTRA_GENDER, patient.getGender());
        intent.putExtra(PatientIntentKeys.EXTRA_INITIALS, patient.getInitials());
        intent.putExtra(PatientIntentKeys.EXTRA_SUMMARY, patient.getAiSummary());
        // Note: Some fields like room/status/completion are not in the current API model
        // but can be added back when the API is expanded.
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
