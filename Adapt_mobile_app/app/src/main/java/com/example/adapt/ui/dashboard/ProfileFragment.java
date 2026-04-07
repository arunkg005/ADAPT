package com.example.adapt.ui.dashboard;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.adapt.R;
import com.example.adapt.ui.login.LoginActivity;
import com.example.adapt.utils.PrefManager;
import com.example.adapt.utils.TaskGuardianScheduler;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class ProfileFragment extends Fragment {

    private PrefManager prefManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        prefManager = new PrefManager(requireContext());

        // Session Info
        TextView tvUserName = view.findViewById(R.id.tvUserName);
        TextView tvUserEmail = view.findViewById(R.id.tvUserEmail);
        TextView tvUserRole = view.findViewById(R.id.tvUserRole);
        TextView tvPermissionStatus = view.findViewById(R.id.tvPermissionStatus);
        TextView tvInternetStatus = view.findViewById(R.id.tvInternetStatus);
        SwitchMaterial switchNotifications = view.findViewById(R.id.switchNotifications);
        
        String name = prefManager.getUserName();
        String email = prefManager.getUserEmail();
        String role = prefManager.getRole();
        String roleLabel = "Patient";

        if ("caregiver".equals(role)) {
            roleLabel = "Caregiver";
        } else if ("admin".equals(role)) {
            roleLabel = "Admin";
        }

        tvUserName.setText(name);
        tvUserEmail.setText(email);
        tvUserRole.setText("Role: " + roleLabel);

        if (switchNotifications != null) {
            switchNotifications.setChecked(prefManager.isReminderEnabled());
            switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefManager.setReminderEnabled(isChecked);
                if (isChecked) {
                    TaskGuardianScheduler.ensureScheduled(requireContext());
                } else {
                    TaskGuardianScheduler.cancelScheduled(requireContext());
                }
                String message = isChecked ? "Routine reminders enabled" : "Routine reminders disabled";
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            });
        }

        if (tvPermissionStatus != null) {
            boolean notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                            == PackageManager.PERMISSION_GRANTED;

            tvPermissionStatus.setText(notificationGranted
                    ? "Background Monitoring: Active"
                    : "Background Monitoring: Notification permission needed");
        }

        if (tvInternetStatus != null) {
            tvInternetStatus.setText(prefManager.isLoggedIn() ? "Cloud Sync: Connected" : "Cloud Sync: Offline");
        }
        
        // Assist Mode Button
        Button btnEnterAssistMode = view.findViewById(R.id.btnEnterAssistMode);
        btnEnterAssistMode.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AssistModeActivity.class);
            startActivity(intent);
        });

        // Logout
        Button btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            TaskGuardianScheduler.cancelScheduled(requireContext());
            prefManager.clear();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }
}
