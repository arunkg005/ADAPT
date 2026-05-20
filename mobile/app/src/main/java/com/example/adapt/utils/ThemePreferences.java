package com.example.adapt.utils;

import android.content.Context;
import android.content.SharedPreferences;

public final class ThemePreferences {
    private static final String PREFS_NAME = "adapt_theme";
    private static final String KEY_LIGHT_MODE = "light_mode";

    private ThemePreferences() {
    }

    public static boolean isLightMode(Context context) {
        return preferences(context).getBoolean(KEY_LIGHT_MODE, true);
    }

    public static void setLightMode(Context context, boolean lightMode) {
        preferences(context).edit().putBoolean(KEY_LIGHT_MODE, lightMode).apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
