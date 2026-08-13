package com.example.eco_print.models;

import com.google.gson.annotations.SerializedName;

public class RewardTransaction {

    private String id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("report_id")
    private String reportId;

    private int points;

    @SerializedName("reward_type")
    private String rewardType;

    private String description;

    @SerializedName("created_at")
    private String createdAt;

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getReportId() {
        return reportId;
    }

    public int getPoints() {
        return points;
    }

    public String getRewardType() {
        return rewardType;
    }

    public String getDescription() {
        return description;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}