package com.example.adapt.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.adapt.MainActivity;
import com.example.adapt.R;
import com.example.adapt.data.network.NetworkClient;
import com.example.adapt.data.network.auth.AuthApiService;
import com.example.adapt.data.network.auth.AuthResponse;
import com.example.adapt.data.network.auth.AuthUser;
import com.example.adapt.data.network.auth.RegisterRequest;
import com.example.adapt.utils.PrefManager;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private RadioGroup rgRole;
    private Button btnRegister;
    private TextView tvLogin;
    private PrefManager prefManager;
    private AuthApiService authApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        prefManager = new PrefManager(this);
        authApiService = NetworkClient.getRetrofit(this).create(AuthApiService.class);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        rgRole = findViewById(R.id.rgRole);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            int selectedRoleId = rgRole.getCheckedRadioButtonId();

            if (name.isEmpty()) {
                etName.setError("Name is required");
                etName.requestFocus();
                return;
            }

            if (email.isEmpty()) {
                etEmail.setError("Email is required");
                etEmail.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Enter a valid email address");
                etEmail.requestFocus();
                return;
            }

            if (password.length() < 8) {
                etPassword.setError("Use at least 8 characters");
                etPassword.requestFocus();
                return;
            }

            String selectedRole = selectedRoleId == R.id.rbCaregiver ? "caregiver" : "patient";
            registerWithBackend(name, email, password, selectedRole);
        });

        tvLogin.setOnClickListener(v -> {
            finish();
        });
    }

    private void registerWithBackend(String fullName, String email, String password, String selectedRole) {
        setLoading(true);

        String[] nameParts = splitName(fullName);
        RegisterRequest request = new RegisterRequest(
                email,
                password,
                nameParts[0],
                nameParts[1],
                null,
                selectedRole.toUpperCase(Locale.US)
        );

        authApiService.register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);

                if (!response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, parseApiError(response, "Registration failed"), Toast.LENGTH_LONG).show();
                    return;
                }

                AuthResponse authResponse = response.body();
                if (authResponse == null || authResponse.getToken() == null || authResponse.getToken().trim().isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Invalid server response", Toast.LENGTH_LONG).show();
                    return;
                }

                AuthUser authUser = authResponse.getUser();
                String role = normalizeRole(authUser != null ? authUser.getRole() : selectedRole);
                String userEmail = authUser != null && authUser.getEmail() != null ? authUser.getEmail() : email;
                String displayName = authUser != null ? authUser.getDisplayName() : fullName;

                prefManager.saveSession(authResponse.getToken(), role, displayName, userEmail);

                Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, "Unable to reach server. Check backend connection.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private String[] splitName(String fullName) {
        String safeName = fullName == null ? "" : fullName.trim();
        if (safeName.isEmpty()) {
            return new String[]{"ADAPT", "User"};
        }

        String[] parts = safeName.split("\\s+");
        if (parts.length == 1) {
            return new String[]{parts[0], "User"};
        }

        String firstName = parts[0];
        StringBuilder lastNameBuilder = new StringBuilder();
        for (int i = 1; i < parts.length; i++) {
            if (i > 1) {
                lastNameBuilder.append(' ');
            }
            lastNameBuilder.append(parts[i]);
        }
        return new String[]{firstName, lastNameBuilder.toString()};
    }

    private String normalizeRole(String apiRole) {
        if (apiRole == null) {
            return "patient";
        }

        String normalized = apiRole.trim().toLowerCase();
        if (normalized.contains("caregiver") || normalized.contains("admin")) {
            return "caregiver";
        }

        if (normalized.contains("patient")) {
            return "patient";
        }

        return "patient";
    }

    private void setLoading(boolean loading) {
        btnRegister.setEnabled(!loading);
        btnRegister.setText(loading ? "Creating Account..." : "Register");
    }

    private String parseApiError(Response<?> response, String fallback) {
        if (response.errorBody() == null) {
            return fallback;
        }

        try {
            String raw = response.errorBody().string();
            if (raw == null || raw.trim().isEmpty()) {
                return fallback;
            }

            JSONObject jsonObject = new JSONObject(raw);
            String apiError = jsonObject.optString("error");
            return apiError == null || apiError.trim().isEmpty() ? fallback : apiError;
        } catch (IOException ignored) {
            return fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
