package com.example.eco_print.models;

import com.google.gson.annotations.SerializedName;

public class FcmTokenRequest {

    @SerializedName("p_token")
    private final String token;

    public FcmTokenRequest(String token) {
        this.token = token;
    }
}