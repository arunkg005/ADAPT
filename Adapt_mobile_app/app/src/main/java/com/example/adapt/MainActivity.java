package com.example.adapt;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.adapt.ui.assistant.AiAssistantActivity;
import com.example.adapt.data.model.ActivityLog;
import com.example.adapt.data.repository.AppRepository;
import com.example.adapt.ui.dashboard.LogsFragment;
import com.example.adapt.ui.dashboard.ProfileFragment;
import com.example.adapt.ui.dashboard.RoutineFragment;
import com.example.adapt.ui.dashboard.StatusFragment;
import com.example.adapt.ui.dashboard.AssistModeActivity;
import com.example.adapt.ui.dashboard.TaskLabFragment;
import com.example.adapt.ui.login.LoginActivity;
import com.example.adapt.utils.CrashRecoveryManager;
import com.example.adapt.utils.MonitoringService;
import com.example.adapt.utils.PrefManager;
import com.example.adapt.utils.TaskGuardianScheduler;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_POST_NOTIFICATIONS = 301;

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNav;
    private NavigationView navigationView;
    private FloatingActionButton fabTaskLab;
    private FloatingActionButton fabAiAssistant;
    private PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefManager = new PrefManager(this);
        if (!prefManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        applyThemeMode(prefManager.isDarkModeEnabled());

        setContentView(R.layout.activity_main);

        reportPendingCrashIfAny();

        requestNotificationPermissionIfNeeded();
        startMonitoringService();
        if (prefManager.isReminderEnabled()) {
            TaskGuardianScheduler.ensureScheduled(this);
        }

        // Toolbar setup
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Drawer setup
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, 
                R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        updateDrawerHeader(prefManager);
        syncDrawerToggles();
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_account || itemId == R.id.nav_settings) {
                bottomNav.setSelectedItemId(R.id.nav_profile);
            } else if (itemId == R.id.nav_help || itemId == R.id.nav_assist_mode) {
                startActivity(new Intent(this, AssistModeActivity.class));
            } else if (itemId == R.id.nav_ai_assistant) {
                startActivity(new Intent(this, AiAssistantActivity.class));
            } else if (itemId == R.id.nav_toggle_theme) {
                toggleTheme(item);
            } else if (itemId == R.id.nav_toggle_ai) {
                toggleAiAssistant(item);
            } else if (itemId == R.id.nav_about) {
                showAboutDialog();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Bottom Navigation setup
        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new StatusFragment();
            } else if (itemId == R.id.nav_logs) {
                selectedFragment = new LogsFragment();
            } else if (itemId == R.id.nav_tasks) {
                selectedFragment = new RoutineFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });

        // Set default selection
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        setupFloatingActions();
    }

    private void setupFloatingActions() {
        fabTaskLab = findViewById(R.id.fabTaskLab);
        fabAiAssistant = findViewById(R.id.fabAiAssistant);

        fabTaskLab.setOnClickListener(v -> loadFragment(new TaskLabFragment()));
        fabAiAssistant.setOnClickListener(v -> startActivity(new Intent(this, AiAssistantActivity.class)));

        updateAiAssistantVisibility();
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void startMonitoringService() {
        Intent serviceIntent = new Intent(this, MonitoringService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQUEST_POST_NOTIFICATIONS
        );
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void reportPendingCrashIfAny() {
        String crashReport = CrashRecoveryManager.consumePendingCrashReport(this);
        if (crashReport == null || crashReport.trim().isEmpty()) {
            return;
        }

        AppRepository repository = new AppRepository(getApplication());
        repository.insertActivityLog(new ActivityLog(
                "Crash Recovery",
                "App recovered after an unexpected stop",
                System.currentTimeMillis(),
                ActivityLog.Source.MOBILE,
                ActivityLog.Type.WARNING,
                crashReport
        ));

        Toast.makeText(this, "Recovered from unexpected stop. Details logged.", Toast.LENGTH_LONG).show();
    }

    private void updateDrawerHeader(PrefManager prefManager) {
        if (navigationView == null) {
            return;
        }

        View headerView = navigationView.getHeaderView(0);
        TextView tvDrawerUserName = headerView.findViewById(R.id.tvDrawerUserName);
        TextView tvDrawerUserEmail = headerView.findViewById(R.id.tvDrawerUserEmail);

        tvDrawerUserName.setText(prefManager.getUserName());
        tvDrawerUserEmail.setText(prefManager.getUserEmail());
    }

    private void syncDrawerToggles() {
        if (navigationView == null) {
            return;
        }

        Menu menu = navigationView.getMenu();
        MenuItem themeItem = menu.findItem(R.id.nav_toggle_theme);
        MenuItem aiItem = menu.findItem(R.id.nav_toggle_ai);

        if (themeItem != null) {
            themeItem.setChecked(prefManager.isDarkModeEnabled());
        }

        if (aiItem != null) {
            aiItem.setChecked(prefManager.isAiAssistantEnabled());
        }
    }

    private void toggleTheme(MenuItem item) {
        boolean enabled = !prefManager.isDarkModeEnabled();
        prefManager.setDarkModeEnabled(enabled);
        item.setChecked(enabled);
        applyThemeMode(enabled);
        recreate();
    }

    private void applyThemeMode(boolean darkModeEnabled) {
        AppCompatDelegate.setDefaultNightMode(
                darkModeEnabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private void toggleAiAssistant(MenuItem item) {
        boolean enabled = !prefManager.isAiAssistantEnabled();
        prefManager.setAiAssistantEnabled(enabled);
        item.setChecked(enabled);
        updateAiAssistantVisibility();
        Toast.makeText(this, enabled ? "AI assistant enabled" : "AI assistant disabled", Toast.LENGTH_SHORT).show();
    }

    private void updateAiAssistantVisibility() {
        if (fabAiAssistant == null) {
            return;
        }

        fabAiAssistant.setVisibility(prefManager.isAiAssistantEnabled() ? View.VISIBLE : View.GONE);
    }

    private void showAboutDialog() {
        String version = "1.0";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {
            // Fall back to static version text.
        }

        String message = "ADAPT Care Platform\nVersion " + version + "\n\nCopyright (c) 2026 ADAPT";
        new MaterialAlertDialogBuilder(this)
                .setTitle("About ADAPT")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
