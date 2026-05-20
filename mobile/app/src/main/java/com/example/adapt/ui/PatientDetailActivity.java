package com.example.adapt.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.adapt.databinding.ActivityPatientDetailBinding;
import com.example.adapt.utils.PatientIntentKeys;
import com.example.adapt.utils.ThemePreferences;

public class PatientDetailActivity extends AppCompatActivity {
    private ActivityPatientDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(
                ThemePreferences.isLightMode(this)
                        ? AppCompatDelegate.MODE_NIGHT_NO
                        : AppCompatDelegate.MODE_NIGHT_YES
        );
        super.onCreate(savedInstanceState);
        binding = ActivityPatientDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bindPatient();
        binding.backButton.setOnClickListener(view -> finish());
        binding.editProfileButton.setOnClickListener(view -> {
            Intent editIntent = new Intent(this, EditPatientActivity.class);
            if (getIntent().getExtras() != null) {
                editIntent.putExtras(getIntent().getExtras());
            }
            startActivity(editIntent);
        });
    }

    private void bindPatient() {
        Intent intent = getIntent();
        String name = intent.getStringExtra(PatientIntentKeys.EXTRA_NAME);
        int age = intent.getIntExtra(PatientIntentKeys.EXTRA_AGE, 0);
        String gender = intent.getStringExtra(PatientIntentKeys.EXTRA_GENDER);
        String initials = intent.getStringExtra(PatientIntentKeys.EXTRA_INITIALS);
        String summary = intent.getStringExtra(PatientIntentKeys.EXTRA_SUMMARY);
        String allergies = intent.getStringExtra(PatientIntentKeys.EXTRA_CONDITIONS);

        binding.detailInitials.setText(initials);
        binding.detailName.setText(name);
        binding.detailMeta.setText(age + " years    " + gender);
        binding.completionLabel.setText("100%");
        binding.completionProgress.setProgress(100);
        binding.basicName.setText("Name                                      " + name);
        binding.basicAge.setText("Age                                           " + age);
        binding.basicGender.setText("Gender                                  " + gender);
        binding.medicalConditions.setText("ALLERGIES\n" + (allergies != null ? allergies : "None"));
        binding.aiSummary.setText("AI SUMMARY\n\n" + (summary != null ? summary : "No summary available."));
    }
}
