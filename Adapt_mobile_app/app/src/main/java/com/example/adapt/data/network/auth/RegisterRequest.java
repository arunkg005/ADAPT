package com.example.adapt.data.network.auth;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {

    private final String email;
    private final String password;

    @SerializedName("first_name")
    private final String firstName;

    @SerializedName("last_name")
    private final String lastName;

    private final String phone;
    private final String role;

    public RegisterRequest(
            String email,
            String password,
            String firstName,
            String lastName,
            String phone,
            String role
    ) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.role = role;
    }
}
