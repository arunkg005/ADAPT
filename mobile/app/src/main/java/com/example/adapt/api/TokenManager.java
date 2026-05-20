package com.example.adapt.api;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages JWT access and refresh tokens using SharedPreferences.
 */
public class TokenManager {
    private static final String PREFS_NAME = "adapt_auth";
    private static final String KEY_ACCESS = "access_token";
    private static final String KEY_REFRESH = "refresh_token";
    private static final String KEY_USERNAME = "username";

    private final SharedPreferences prefs;

    public TokenManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveTokens(String access, String refresh) {
        prefs.edit()
                .putString(KEY_ACCESS, access)
                .putString(KEY_REFRESH, refresh)
                .apply();
    }

    public void saveUsername(String username) {
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH, null);
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    public void clearTokens() {
        prefs.edit().clear().apply();
    }
}
