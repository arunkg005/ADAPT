package com.example.adapt.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.adapt.R;
import com.example.adapt.ui.task.TaskActivity;

public class AssistModeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Make activity full screen and lock distractions
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // Keep screen on during assist mode
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_assist_mode);

        Button btnViewTask = findViewById(R.id.btnViewActiveTask);
        Button btnUnlock = findViewById(R.id.btnCaretakerUnlock);

        btnViewTask.setOnClickListener(v -> {
            // In a real app, this would fetch the current pending task
            Intent intent = new Intent(this, TaskActivity.class);
            intent.putExtra("ROUTINE_ID", 1);
            intent.putExtra("ROUTINE_TITLE", "Current Focused Task");
            startActivity(intent);
        });

        // "Locked" functionality: Caretaker must long press to exit
        btnUnlock.setOnLongClickListener(v -> {
            Toast.makeText(this, "Assist Mode Deactivated", Toast.LENGTH_SHORT).show();
            finish();
            return true;
        });
        
        btnUnlock.setOnClickListener(v -> {
            Toast.makeText(this, "Long press to unlock Assist Mode", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onBackPressed() {
        // Disable back button to maintain focus (Processing Speed & Attention Control)
        Toast.makeText(this, "Exit restricted to Caregiver only.", Toast.LENGTH_SHORT).show();
    }
}
