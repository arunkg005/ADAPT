package com.example.adapt.ui;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.adapt.databinding.ActivityEditPatientBinding;
import com.example.adapt.databinding.SectionEditMultilineBinding;
import com.example.adapt.databinding.SectionEditTextBinding;
import com.example.adapt.utils.PatientIntentKeys;
import com.example.adapt.utils.ThemePreferences;

public class EditPatientActivity extends AppCompatActivity {
    private ActivityEditPatientBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(
                ThemePreferences.isLightMode(this)
                        ? AppCompatDelegate.MODE_NIGHT_NO
                        : AppCompatDelegate.MODE_NIGHT_YES
        );
        super.onCreate(savedInstanceState);
        binding = ActivityEditPatientBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        boolean isNew = getIntent().getBooleanExtra("EXTRA_IS_NEW", false);
        if (isNew) {
            binding.titleText.setText("Add Patient");
        }

        bindForm(isNew);
        binding.backButton.setOnClickListener(view -> finish());
        binding.saveButton.setOnClickListener(view -> saveAndClose(isNew));
        binding.updateButton.setOnClickListener(view -> saveAndClose(isNew));
        
        if (isNew) {
            binding.updateButton.setText("Add Patient");
            binding.deleteButton.setVisibility(android.view.View.GONE);
        }

        binding.deleteButton.setOnClickListener(view -> {
            Toast.makeText(this, "Patient file marked for deletion", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void bindForm(boolean isNew) {
        if (isNew) {
            setTextSection(binding.nameCard, "Name", "");
            setTextSection(binding.ageCard, "Age", "");
            setTextSection(binding.genderCard, "Gender", "");
            setMultilineSection(binding.allergiesCard, "Allergies", "");
            setMultilineSection(binding.intolerancesCard, "Intolerances", "");
            setMultilineSection(binding.scenarioCard, "Current scenario description", "");
            setMultilineSection(binding.guidelinesCard, "Doctor guidelines", "");
            setTextSection(binding.reminderMinutesCard, "Routine reminder minutes before", "30");
            setMultilineSection(binding.summaryCard, "Ai summary", "");
        } else {
            setTextSection(binding.nameCard, "Name", getIntent().getStringExtra(PatientIntentKeys.EXTRA_NAME));
            setTextSection(binding.ageCard, "Age", String.valueOf(getIntent().getIntExtra(PatientIntentKeys.EXTRA_AGE, 0)));
            setTextSection(binding.genderCard, "Gender", getIntent().getStringExtra(PatientIntentKeys.EXTRA_GENDER));
            setMultilineSection(binding.allergiesCard, "Allergies", getIntent().getStringExtra(PatientIntentKeys.EXTRA_CONDITIONS));
            setMultilineSection(binding.intolerancesCard, "Intolerances", "");
            setMultilineSection(binding.scenarioCard, "Current scenario description", getIntent().getStringExtra(PatientIntentKeys.EXTRA_SCENARIO));
            setMultilineSection(binding.guidelinesCard, "Doctor guidelines", getIntent().getStringExtra(PatientIntentKeys.EXTRA_GUIDELINES));
            setTextSection(binding.reminderMinutesCard, "Routine reminder minutes before", "30");
            setMultilineSection(binding.summaryCard, "Ai summary", getIntent().getStringExtra(PatientIntentKeys.EXTRA_SUMMARY));
        }
    }

    private void setTextSection(SectionEditTextBinding section, String label, String value) {
        section.sectionLabel.setText(label);
        section.sectionInput.setText(value == null ? "" : value);
    }

    private void setMultilineSection(SectionEditMultilineBinding section, String label, String value) {
        section.sectionLabel.setText(label);
        section.sectionInput.setText(value == null ? "" : value);
    }

    private void saveAndClose(boolean isNew) {
        String message = isNew ? "Patient added successfully" : "Patient file updated";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }
}
