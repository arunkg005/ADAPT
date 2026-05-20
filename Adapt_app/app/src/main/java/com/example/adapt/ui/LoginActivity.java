package com.example.adapt.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.adapt.api.ApiService;
import com.example.adapt.api.AuthResponse;
import com.example.adapt.api.RetrofitClient;
import com.example.adapt.api.TokenManager;
import com.example.adapt.databinding.ActivityLoginBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Login screen — entry point when the user has no stored JWT token.
 * Supports login + a toggle to register a new account.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private TokenManager tokenManager;
    private boolean isRegisterMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Retrofit with context so TokenManager is available
        RetrofitClient.init(this);
        tokenManager = RetrofitClient.getTokenManager();

        // Already logged in → go straight to MainActivity
        if (tokenManager.isLoggedIn()) {
            goToMain();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.loginButton.setOnClickListener(v -> attemptAuth());
        binding.toggleModeText.setOnClickListener(v -> toggleMode());
    }

    private void toggleMode() {
        isRegisterMode = !isRegisterMode;
        if (isRegisterMode) {
            binding.loginTitle.setText("Create Account");
            binding.loginButton.setText("Register");
            binding.toggleModeText.setText("Already have an account? Log in");
            binding.confirmPasswordLayout.setVisibility(View.VISIBLE);
        } else {
            binding.loginTitle.setText("Welcome Back");
            binding.loginButton.setText("Log In");
            binding.toggleModeText.setText("Don't have an account? Register");
            binding.confirmPasswordLayout.setVisibility(View.GONE);
        }
    }

    private void attemptAuth() {
        String username = binding.usernameInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isRegisterMode) {
            String confirm = binding.confirmPasswordInput.getText().toString().trim();
            if (!password.equals(confirm)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }
            doRegister(username, password);
        } else {
            doLogin(username, password);
        }
    }

    private void doLogin(String username, String password) {
        setLoading(true);
        ApiService api = RetrofitClient.getApiService();

        api.login(username, password).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    tokenManager.saveTokens(response.body().access, response.body().refresh);
                    tokenManager.saveUsername(username);
                    // Rebuild Retrofit with the new token
                    RetrofitClient.init(LoginActivity.this);
                    goToMain();
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this,
                        "Connection error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void doRegister(String username, String password) {
        setLoading(true);
        ApiService api = RetrofitClient.getApiService();

        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("username", username);
        body.put("password", password);

        api.register(body).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    tokenManager.saveTokens(response.body().access, response.body().refresh);
                    tokenManager.saveUsername(username);
                    RetrofitClient.init(LoginActivity.this);
                    goToMain();
                } else {
                    Toast.makeText(LoginActivity.this,
                            "Registration failed — username may be taken", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this,
                        "Connection error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.loginButton.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
