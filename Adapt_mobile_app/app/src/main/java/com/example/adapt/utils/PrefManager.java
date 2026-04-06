package com.example.adapt.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {
    private static final String PREF_NAME = "adapt_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_ROUTINE_SEEDED = "routine_seeded";
    private static final String KEY_ACTIVITY_LOG_SEEDED = "activity_log_seeded";
    private static final String KEY_TASK_SEEDED_PREFIX = "task_seeded_";

    private final SharedPreferences pref;

    public PrefManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String token, String role, String name, String email) {
        pref.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USER_ROLE, role)
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_EMAIL, email)
                .apply();
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.trim().isEmpty();
    }

    public void saveToken(String token) {
        pref.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return pref.getString(KEY_TOKEN, null);
    }

    public void saveRole(String role) {
        pref.edit().putString(KEY_USER_ROLE, role).apply();
    }

    public String getRole() {
        return pref.getString(KEY_USER_ROLE, "patient");
    }

    public void saveUserProfile(String name, String email) {
        pref.edit()
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_EMAIL, email)
                .apply();
    }

    public String getUserName() {
        return pref.getString(KEY_USER_NAME, "ADAPT User");
    }

    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, "No email linked");
    }

    public void setReminderEnabled(boolean enabled) {
        pref.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply();
    }

    public boolean isReminderEnabled() {
        return pref.getBoolean(KEY_REMINDER_ENABLED, true);
    }

    public void setRoutineSeeded(boolean seeded) {
        pref.edit().putBoolean(KEY_ROUTINE_SEEDED, seeded).apply();
    }

    public boolean isRoutineSeeded() {
        return pref.getBoolean(KEY_ROUTINE_SEEDED, false);
    }

    public void setActivityLogSeeded(boolean seeded) {
        pref.edit().putBoolean(KEY_ACTIVITY_LOG_SEEDED, seeded).apply();
    }

    public boolean isActivityLogSeeded() {
        return pref.getBoolean(KEY_ACTIVITY_LOG_SEEDED, false);
    }

    public void setTaskSeeded(int routineId, boolean seeded) {
        pref.edit().putBoolean(getTaskSeededKey(routineId), seeded).apply();
    }

    public boolean isTaskSeeded(int routineId) {
        return pref.getBoolean(getTaskSeededKey(routineId), false);
    }

    private String getTaskSeededKey(int routineId) {
        return KEY_TASK_SEEDED_PREFIX + routineId;
    }

    public void clear() {
        pref.edit().clear().apply();
    }
}
