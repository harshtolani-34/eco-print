package com.example.eco_print.models;

import com.google.gson.annotations.SerializedName;

public class UserProfile {

    private String id;

    @SerializedName("full_name")
    private String fullName;

    private Integer age;
    private String email;
    private String role;

    @SerializedName("company_code")
    private String companyCode;

    @SerializedName("collector_status")
    private String collectorStatus;

    @SerializedName("collector_rejection_reason")
    private String collectorRejectionReason;

    @SerializedName("collector_reviewed_by")
    private String collectorReviewedBy;

    @SerializedName("collector_reviewed_at")
    private String collectorReviewedAt;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public Integer getAge() {
        return age;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public String getCollectorStatus() {
        return collectorStatus;
    }

    public String getCollectorRejectionReason() {
        return collectorRejectionReason;
    }

    public String getCollectorReviewedBy() {
        return collectorReviewedBy;
    }

    public String getCollectorReviewedAt() {
        return collectorReviewedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
