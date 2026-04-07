package com.example.adapt.ui.dashboard;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.adapt.R;
import com.google.android.material.appbar.MaterialToolbar;

public class DetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        MaterialToolbar toolbar = findViewById(R.id.toolbarDetails);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        String patientName = getIntent().getStringExtra("PATIENT_NAME");
        String patientRisk = getIntent().getStringExtra("PATIENT_RISK");
        if (patientName != null && !patientName.trim().isEmpty()) {
            toolbar.setTitle(patientName.trim() + " Analysis");
        }
        if (patientRisk != null && !patientRisk.trim().isEmpty()) {
            toolbar.setSubtitle("Risk: " + patientRisk.trim().toUpperCase());
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }
}
