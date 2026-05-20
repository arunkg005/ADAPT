package com.example.adapt.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.adapt.databinding.BottomSheetEditScheduleItemBinding;
import com.example.adapt.model.ScheduleItem;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class EditScheduleItemBottomSheetFragment extends BottomSheetDialogFragment {
    public interface ScheduleEditListener {
        void onScheduleItemSaved(int position, ScheduleItem item);
        void onScheduleItemDeleted(int position);
    }

    private static final String ARG_POSITION = "position";
    private static final String ARG_TIME = "time";
    private static final String ARG_TITLE = "title";
    private static final String ARG_PATIENT = "patient";
    private static final String ARG_TYPE = "type";
    private static final String ARG_CURRENT = "current";

    private BottomSheetEditScheduleItemBinding binding;

    public static EditScheduleItemBottomSheetFragment newInstance(int position, ScheduleItem item) {
        EditScheduleItemBottomSheetFragment fragment = new EditScheduleItemBottomSheetFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        args.putString(ARG_TIME, item.getTime());
        args.putString(ARG_TITLE, item.getTitle());
        args.putString(ARG_PATIENT, item.getPatientName());
        args.putString(ARG_TYPE, item.getType());
        args.putBoolean(ARG_CURRENT, item.isCurrent());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetEditScheduleItemBinding.inflate(inflater, container, false);
        Bundle args = requireArguments();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Medication", "Activity", "Attention", "Cognitive", "Routine"});
        binding.typeSpinner.setAdapter(adapter);
        binding.titleInput.setText(args.getString(ARG_TITLE));
        binding.patientInput.setText(args.getString(ARG_PATIENT));
        binding.timeInput.setText(args.getString(ARG_TIME));
        String type = args.getString(ARG_TYPE);
        int selected = adapter.getPosition(type);
        binding.typeSpinner.setSelection(Math.max(selected, 0));

        binding.saveButton.setOnClickListener(view -> save());
        binding.deleteButton.setOnClickListener(view -> delete());
        return binding.getRoot();
    }

    private void save() {
        if (!(getParentFragment() instanceof ScheduleEditListener)) {
            dismiss();
            return;
        }
        Bundle args = requireArguments();
        String title = binding.titleInput.getText().toString().trim();
        if (title.isEmpty()) {
            binding.titleInput.setError("Title required");
            return;
        }
        ScheduleItem item = new ScheduleItem(
                binding.timeInput.getText().toString().trim(),
                title,
                binding.patientInput.getText().toString().trim(),
                binding.typeSpinner.getSelectedItem().toString(),
                args.getBoolean(ARG_CURRENT)
        );
        ((ScheduleEditListener) getParentFragment()).onScheduleItemSaved(args.getInt(ARG_POSITION), item);
        dismiss();
    }

    private void delete() {
        if (getParentFragment() instanceof ScheduleEditListener) {
            ((ScheduleEditListener) getParentFragment()).onScheduleItemDeleted(requireArguments().getInt(ARG_POSITION));
        }
        dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
