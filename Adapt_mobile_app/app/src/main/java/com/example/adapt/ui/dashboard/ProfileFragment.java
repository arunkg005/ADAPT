package com.example.adapt.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.adapt.R;
import com.example.adapt.ui.login.LoginActivity;
import com.example.adapt.utils.PrefManager;

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
        
        // Mock data from PrefManager or fallback
        String role = prefManager.getRole();
        tvUserRole.setText("Role: " + (role.equals("caregiver") ? "Caregiver" : "Patient"));
        // Email/Name would ideally come from a User object saved in Prefs
        
        // Assist Mode Button
        Button btnEnterAssistMode = view.findViewById(R.id.btnEnterAssistMode);
        btnEnterAssistMode.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AssistModeActivity.class);
            startActivity(intent);
        });

        // Logout
        Button btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            prefManager.clear();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }
}
