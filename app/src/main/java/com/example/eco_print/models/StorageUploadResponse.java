package com.example.eco_print.models;

import com.google.gson.annotations.SerializedName;

public class StorageUploadResponse {

    @SerializedName("Key")
    private String key;

    public String getKey() {
        return key;
    }
}
