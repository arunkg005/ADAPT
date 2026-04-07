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
import com.example.adapt.data.network.auth.SocialLoginRequest;
import com.example.adapt.utils.PrefManager;

import org.json.JSONObject;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnGoogleSignIn, btnFacebookSignIn;
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
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        btnFacebookSignIn = findViewById(R.id.btnFacebookSignIn);
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

        btnGoogleSignIn.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (!isValidEmail(email)) {
                return;
            }

            socialLoginWithBackend("google", email, btnGoogleSignIn);
        });

        btnFacebookSignIn.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (!isValidEmail(email)) {
                return;
            }

            socialLoginWithBackend("facebook", email, btnFacebookSignIn);
        });

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void loginWithBackend(String email, String password) {
        setCredentialLoading(true);

        authApiService.login(new LoginRequest(email, password)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setCredentialLoading(false);

                if (!response.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, parseApiError(response, "Login failed"), Toast.LENGTH_LONG).show();
                    return;
                }

                AuthResponse authResponse = response.body();
                if (!isValidAuthResponse(authResponse)) {
                    Toast.makeText(LoginActivity.this, "Invalid server response", Toast.LENGTH_LONG).show();
                    return;
                }

                completeAuth(authResponse, email, "Welcome back");
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setCredentialLoading(false);
                Toast.makeText(LoginActivity.this, "Unable to reach server. Check backend connection.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void socialLoginWithBackend(String provider, String email, Button providerButton) {
        setSocialLoading(true, providerButton);

        String[] nameParts = splitNameFromEmail(email);
        SocialLoginRequest request = new SocialLoginRequest(provider, email, nameParts[0], nameParts[1]);

        authApiService.socialLogin(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setSocialLoading(false, providerButton);

                if (!response.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, parseApiError(response, "Social sign-in failed"), Toast.LENGTH_LONG).show();
                    return;
                }

                AuthResponse authResponse = response.body();
                if (!isValidAuthResponse(authResponse)) {
                    Toast.makeText(LoginActivity.this, "Invalid server response", Toast.LENGTH_LONG).show();
                    return;
                }

                completeAuth(authResponse, email, "Signed in with " + capitalize(provider));
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setSocialLoading(false, providerButton);
                Toast.makeText(LoginActivity.this, "Unable to reach server. Check backend connection.", Toast.LENGTH_LONG).show();
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

    private void completeAuth(AuthResponse authResponse, String fallbackEmail, String message) {
        AuthUser authUser = authResponse.getUser();
        String role = normalizeRole(authUser != null ? authUser.getRole() : null);
        String userEmail = authUser != null && authUser.getEmail() != null ? authUser.getEmail() : fallbackEmail;
        String displayName = authUser != null ? authUser.getDisplayName() : buildDisplayNameFromEmail(fallbackEmail);

        prefManager.saveSession(authResponse.getToken(), role, displayName, userEmail);
        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
        navigateToMain();
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setCredentialLoading(boolean loading) {
        setBaseLoadingState(loading);
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Signing In..." : "Sign In");
        if (!loading) {
            btnGoogleSignIn.setText("Google");
            btnFacebookSignIn.setText("Facebook");
        }
    }

    private void setSocialLoading(boolean loading, Button providerButton) {
        setBaseLoadingState(loading);
        btnLogin.setText("Sign In");

        if (loading) {
            providerButton.setText("Connecting...");
        } else {
            btnGoogleSignIn.setText("Google");
            btnFacebookSignIn.setText("Facebook");
        }
    }

    private void setBaseLoadingState(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnGoogleSignIn.setEnabled(!loading);
        btnFacebookSignIn.setEnabled(!loading);
    }

    private String[] splitNameFromEmail(String email) {
        String[] parts = email.split("@");
        if (parts.length == 0 || parts[0].trim().isEmpty()) {
            return new String[]{"ADAPT", "User"};
        }

        String localPart = parts[0].replace('_', ' ').replace('.', ' ').trim();
        if (localPart.isEmpty()) {
            return new String[]{"ADAPT", "User"};
        }

        String[] nameParts = localPart.split("\\s+");
        if (nameParts.length == 1) {
            return new String[]{capitalize(nameParts[0]), "User"};
        }

        String firstName = capitalize(nameParts[0]);
        StringBuilder lastName = new StringBuilder();
        for (int i = 1; i < nameParts.length; i++) {
            if (i > 1) {
                lastName.append(' ');
            }
            lastName.append(capitalize(nameParts[i]));
        }
        return new String[]{firstName, lastName.toString()};
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
