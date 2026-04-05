package com.example.adapt.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {
    private static final String PREF_NAME = "adapt_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_ROLE = "user_role";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public PrefManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void saveToken(String token) {
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }

    public String getToken() {
        return pref.getString(KEY_TOKEN, null);
    }

    public void saveRole(String role) {
        editor.putString(KEY_USER_ROLE, role);
        editor.apply();
    }

    public String getRole() {
        return pref.getString(KEY_USER_ROLE, "patient");
    }

    public void clear() {
        editor.clear();
        editor.apply();
    }
}
