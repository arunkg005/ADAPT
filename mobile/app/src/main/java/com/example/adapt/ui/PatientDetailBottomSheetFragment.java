package com.example.adapt.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.adapt.databinding.BottomSheetPatientDetailBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class PatientDetailBottomSheetFragment extends BottomSheetDialogFragment {
    private static final String ARG_NAME = "name";
    private static final String ARG_META = "meta";
    private static final String ARG_COMPLETION = "completion";
    private static final String ARG_SCENARIO = "scenario";
    private static final String ARG_GUIDELINES = "guidelines";
    private static final String ARG_SUMMARY = "summary";
    private BottomSheetPatientDetailBinding binding;

    public static PatientDetailBottomSheetFragment newInstance(String name, String meta, int completion,
                                                               String scenario, String guidelines, String summary) {
        PatientDetailBottomSheetFragment fragment = new PatientDetailBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putString(ARG_META, meta);
        args.putInt(ARG_COMPLETION, completion);
        args.putString(ARG_SCENARIO, scenario);
        args.putString(ARG_GUIDELINES, guidelines);
        args.putString(ARG_SUMMARY, summary);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetPatientDetailBinding.inflate(inflater, container, false);
        Bundle args = requireArguments();
        binding.detailName.setText(args.getString(ARG_NAME));
        binding.detailMeta.setText(args.getString(ARG_META));
        binding.detailCompletion.setText("Task completion " + args.getInt(ARG_COMPLETION) + "%");
        binding.detailScenario.setText("Current scenario\n" + args.getString(ARG_SCENARIO));
        binding.detailGuidelines.setText("Doctor guidelines\n" + args.getString(ARG_GUIDELINES));
        binding.detailSummary.setText("AI summary\n" + args.getString(ARG_SUMMARY));
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
