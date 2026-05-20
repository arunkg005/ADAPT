package com.example.adapt.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.adapt.databinding.FragmentProgressBinding;
import com.example.adapt.viewmodel.ProgressViewModel;

public class ProgressFragment extends Fragment {

    private FragmentProgressBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentProgressBinding.inflate(inflater, container, false);
        ProgressViewModel viewModel = new ViewModelProvider(this).get(ProgressViewModel.class);
        ArrayAdapter<String> patientAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Sarah Jenkins", "All Patients", "Robert Miller", "Asha Verma"}
        );
        binding.patientSpinner.setAdapter(patientAdapter);
        viewModel.getTrend().observe(getViewLifecycleOwner(), binding.progressChart::setValues);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
