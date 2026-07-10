package com.example.eco_print.models;

import com.google.gson.annotations.SerializedName;

public class CitizenProfileRequest {

    @SerializedName("p_full_name")
    private final String fullName;

    @SerializedName("p_email")
    private final String email;

    public CitizenProfileRequest(
            String fullName,
            String email
    ) {
        this.fullName = fullName;
        this.email = email;
    }
}
