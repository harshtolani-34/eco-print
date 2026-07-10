package com.example.eco_print.models;

import com.google.gson.annotations.SerializedName;

public class CollectorApplicationRequest {

    @SerializedName("p_full_name")
    private final String fullName;

    @SerializedName("p_age")
    private final int age;

    @SerializedName("p_email")
    private final String email;

    @SerializedName("p_company_code")
    private final String companyCode;

    public CollectorApplicationRequest(
            String fullName,
            int age,
            String email,
            String companyCode
    ) {
        this.fullName = fullName;
        this.age = age;
        this.email = email;
        this.companyCode = companyCode;
    }
}
