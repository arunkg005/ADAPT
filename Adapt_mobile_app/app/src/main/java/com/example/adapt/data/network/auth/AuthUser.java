package com.example.adapt.data.network.auth;

import com.google.gson.annotations.SerializedName;

public class AuthUser {

    private String id;
    private String email;
    private String role;

    @SerializedName("first_name")
    private String firstName;

    @SerializedName("last_name")
    private String lastName;

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDisplayName() {
        String safeFirstName = firstName == null ? "" : firstName.trim();
        String safeLastName = lastName == null ? "" : lastName.trim();
        String merged = (safeFirstName + " " + safeLastName).trim();

        if (!merged.isEmpty()) {
            return merged;
        }

        if (email == null || !email.contains("@")) {
            return "ADAPT User";
        }

        return email.substring(0, email.indexOf('@'));
    }
}
