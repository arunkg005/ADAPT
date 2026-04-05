package com.example.adapt.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.adapt.MainActivity;

/**
 * Legacy DashboardActivity.
 * The app now uses MainActivity with Fragments.
 * Redirecting to MainActivity for safety.
 */
public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
