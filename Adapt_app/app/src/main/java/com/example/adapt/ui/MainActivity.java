package com.example.adapt.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.adapt.R;
import com.example.adapt.api.RetrofitClient;
import com.example.adapt.databinding.ActivityMainBinding;
import com.example.adapt.utils.ThemePreferences;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Ensure RetrofitClient is initialized with auth tokens
        RetrofitClient.init(this);

        AppCompatDelegate.setDefaultNightMode(
                ThemePreferences.isLightMode(this)
                        ? AppCompatDelegate.MODE_NIGHT_NO
                        : AppCompatDelegate.MODE_NIGHT_YES
        );
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
            navController.addOnDestinationChangedListener((controller, destination, arguments) ->
                    binding.contextTitle.setText(destination.getLabel()));
        }
        binding.menuButton.setOnClickListener(view ->
                new CareMenuBottomSheetFragment().show(getSupportFragmentManager(), "care_menu_sheet"));
        binding.assistFab.setOnClickListener(view ->
                new AssistantBottomSheetFragment().show(getSupportFragmentManager(), "assistant_sheet"));
    }
}
