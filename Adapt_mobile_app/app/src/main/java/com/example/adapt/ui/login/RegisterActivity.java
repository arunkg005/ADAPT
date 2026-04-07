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
import com.example.adapt.data.network.auth.RegisterRequest;
import com.example.adapt.data.network.auth.SocialLoginRequest;
import com.example.adapt.utils.PrefManager;

import org.json.JSONObject;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etEmail, etPassword;
    private Button btnRegister, btnGoogleSignUp, btnFacebookSignUp;
    private TextView tvLogin;
    private PrefManager prefManager;
    private AuthApiService authApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        prefManager = new PrefManager(this);
        authApiService = NetworkClient.getRetrofit(this).create(AuthApiService.class);

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoogleSignUp = findViewById(R.id.btnGoogleSignUp);
        btnFacebookSignUp = findViewById(R.id.btnFacebookSignUp);
        tvLogin = findViewById(R.id.tvLogin);

        btnRegister.setOnClickListener(v -> {
            String firstName = etFirstName.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();

            if (firstName.isEmpty()) {
                etFirstName.setError("First name is required");
                etFirstName.requestFocus();
                return;
            }

            if (!isValidEmail(email)) {
                return;
            }

            if (password.length() < 8) {
                etPassword.setError("Use at least 8 characters");
                etPassword.requestFocus();
                return;
            }

            registerWithBackend(firstName, lastName, email, password);
        });

        btnGoogleSignUp.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (!isValidEmail(email)) {
                return;
            }

            String firstName = etFirstName.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();
            socialSignupWithBackend("google", firstName, lastName, email, btnGoogleSignUp);
        });

        btnFacebookSignUp.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (!isValidEmail(email)) {
                return;
            }

            String firstName = etFirstName.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();
            socialSignupWithBackend("facebook", firstName, lastName, email, btnFacebookSignUp);
        });

        tvLogin.setOnClickListener(v -> {
            finish();
        });
    }

    private void registerWithBackend(String firstName, String lastName, String email, String password) {
        setCredentialLoading(true);

        RegisterRequest request = new RegisterRequest(
                email,
                password,
                firstName,
                lastName,
                null,
                "CAREGIVER"
        );

        authApiService.register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setCredentialLoading(false);

                if (!response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, parseApiError(response, "Registration failed"), Toast.LENGTH_LONG).show();
                    return;
                }

                AuthResponse authResponse = response.body();
                if (!isValidAuthResponse(authResponse)) {
                    Toast.makeText(RegisterActivity.this, "Invalid server response", Toast.LENGTH_LONG).show();
                    return;
                }

                String fallbackName = (firstName + " " + lastName).trim();
                completeAuth(authResponse, email, fallbackName, "Registration successful");
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setCredentialLoading(false);
                Toast.makeText(RegisterActivity.this, "Unable to reach server. Check backend connection.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void socialSignupWithBackend(String provider, String firstName, String lastName, String email, Button providerButton) {
        setSocialLoading(true, providerButton);

        String safeFirstName = firstName.isEmpty() ? capitalize(provider) : firstName;
        String safeLastName = lastName.isEmpty() ? "User" : lastName;
        SocialLoginRequest request = new SocialLoginRequest(provider, email, safeFirstName, safeLastName);

        authApiService.socialLogin(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setSocialLoading(false, providerButton);

                if (!response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, parseApiError(response, "Social signup failed"), Toast.LENGTH_LONG).show();
                    return;
                }

                AuthResponse authResponse = response.body();
                if (!isValidAuthResponse(authResponse)) {
                    Toast.makeText(RegisterActivity.this, "Invalid server response", Toast.LENGTH_LONG).show();
                    return;
                }

                completeAuth(authResponse, email, (safeFirstName + " " + safeLastName).trim(), "Signed up with " + capitalize(provider));
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setSocialLoading(false, providerButton);
                Toast.makeText(RegisterActivity.this, "Unable to reach server. Check backend connection.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean isValidEmail(String email) {
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return false;
        }

        return true;
    }

    private boolean isValidAuthResponse(AuthResponse authResponse) {
        return authResponse != null && authResponse.getToken() != null && !authResponse.getToken().trim().isEmpty();
    }

    private void completeAuth(AuthResponse authResponse, String fallbackEmail, String fallbackDisplayName, String message) {
        AuthUser authUser = authResponse.getUser();
        String role = normalizeRole(authUser != null ? authUser.getRole() : "caregiver");
        String userEmail = authUser != null && authUser.getEmail() != null ? authUser.getEmail() : fallbackEmail;
        String displayName = authUser != null ? authUser.getDisplayName() : fallbackDisplayName;

        prefManager.saveSession(authResponse.getToken(), role, displayName, userEmail);

        Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String normalizeRole(String apiRole) {
        if (apiRole == null) {
            return "patient";
        }

        String normalized = apiRole.trim().toLowerCase();
        if (normalized.contains("admin")) {
            return "admin";
        }

        if (normalized.contains("caregiver")) {
            return "caregiver";
        }

        if (normalized.contains("patient")) {
            return "patient";
        }

        return "patient";
    }

    private void setCredentialLoading(boolean loading) {
        setBaseLoadingState(loading);
        btnRegister.setText(loading ? "Creating Account..." : "Create Account");
        if (!loading) {
            btnGoogleSignUp.setText("Google");
            btnFacebookSignUp.setText("Facebook");
        }
    }

    private void setSocialLoading(boolean loading, Button providerButton) {
        setBaseLoadingState(loading);
        btnRegister.setText("Create Account");

        if (loading) {
            providerButton.setText("Connecting...");
        } else {
            btnGoogleSignUp.setText("Google");
            btnFacebookSignUp.setText("Facebook");
        }
    }

    private void setBaseLoadingState(boolean loading) {
        btnRegister.setEnabled(!loading);
        btnGoogleSignUp.setEnabled(!loading);
        btnFacebookSignUp.setEnabled(!loading);
    }

    private String capitalize(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return "";
        }

        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1).toLowerCase();
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
