package com.example.adapt.data.network.auth;

import com.google.gson.annotations.SerializedName;

public class SocialLoginRequest {

    private final String provider;
    private final String email;

    @SerializedName("first_name")
    private final String firstName;

    @SerializedName("last_name")
    private final String lastName;

    public SocialLoginRequest(String provider, String email, String firstName, String lastName) {
        this.provider = provider;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
