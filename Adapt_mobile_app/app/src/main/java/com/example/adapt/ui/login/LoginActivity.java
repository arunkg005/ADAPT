package com.example.adapt.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.adapt.MainActivity;
import com.example.adapt.R;
import com.example.adapt.data.network.NetworkClient;
import com.example.adapt.data.network.auth.AuthApiService;
import com.example.adapt.data.network.auth.AuthResponse;
import com.example.adapt.data.network.auth.AuthUser;
import com.example.adapt.data.network.auth.LoginRequest;
import com.example.adapt.utils.PrefManager;

import org.json.JSONObject;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private PrefManager prefManager;
    private AuthApiService authApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefManager = new PrefManager(this);
        if (prefManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        authApiService = NetworkClient.getRetrofit(this).create(AuthApiService.class);

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();

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

            if (password.isEmpty()) {
                etPassword.setError("Password is required");
                etPassword.requestFocus();
                return;
            }

            loginWithBackend(email, password);
        });

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void loginWithBackend(String email, String password) {
        setLoading(true);

        authApiService.login(new LoginRequest(email, password)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);

                if (!response.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, parseApiError(response, "Login failed"), Toast.LENGTH_LONG).show();
                    return;
                }

                AuthResponse authResponse = response.body();
                if (authResponse == null || authResponse.getToken() == null || authResponse.getToken().trim().isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Invalid server response", Toast.LENGTH_LONG).show();
                    return;
                }

                AuthUser authUser = authResponse.getUser();
                String role = normalizeRole(authUser != null ? authUser.getRole() : null);
                String userEmail = authUser != null && authUser.getEmail() != null ? authUser.getEmail() : email;
                String displayName = authUser != null ? authUser.getDisplayName() : buildDisplayNameFromEmail(email);

                prefManager.saveSession(authResponse.getToken(), role, displayName, userEmail);
                Toast.makeText(LoginActivity.this, "Welcome back", Toast.LENGTH_SHORT).show();
                navigateToMain();
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Unable to reach server. Check backend connection.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Signing In..." : "Sign In");
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

    private String buildDisplayNameFromEmail(String email) {
        String[] parts = email.split("@");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return "ADAPT User";
        }

        String raw = parts[0].replace('.', ' ').replace('_', ' ').trim();
        if (raw.isEmpty()) {
            return "ADAPT User";
        }

        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}
