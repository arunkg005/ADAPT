package com.example.adapt.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.adapt.R;
import com.example.adapt.databinding.BottomSheetCareMenuBinding;
import com.example.adapt.utils.ThemePreferences;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class CareMenuBottomSheetFragment extends BottomSheetDialogFragment {
    private BottomSheetCareMenuBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetCareMenuBinding.inflate(inflater, container, false);
        boolean isLightMode = ThemePreferences.isLightMode(requireContext());
        binding.lightModeSwitch.setChecked(isLightMode);
        binding.themeDetail.setText(isLightMode ? R.string.light_mode_detail : R.string.dark_mode_detail);
        binding.lightModeSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            ThemePreferences.setLightMode(requireContext(), checked);
            binding.themeDetail.setText(checked ? R.string.light_mode_detail : R.string.dark_mode_detail);
            AppCompatDelegate.setDefaultNightMode(
                    checked ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES
            );
        });
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
