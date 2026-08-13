package com.example.eco_print.models;

import com.google.gson.annotations.SerializedName;

public class EcoNotification {

    private String id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("report_id")
    private String reportId;

    @SerializedName("notification_type")
    private String notificationType;

    private String title;

    private String message;

    @SerializedName("is_read")
    private boolean read;

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

    public String getNotificationType() {
        return notificationType;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}