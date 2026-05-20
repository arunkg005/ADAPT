package com.example.adapt.api;

import com.google.gson.annotations.SerializedName;

/**
 * Response body from /api/auth/token/ (JWT token pair).
 */
public class AuthResponse {
    @SerializedName("access")
    public String access;

    @SerializedName("refresh")
    public String refresh;
}
