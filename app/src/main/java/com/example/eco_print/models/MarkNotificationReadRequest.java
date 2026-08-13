package com.example.eco_print.models;

import com.google.gson.annotations.SerializedName;

public class MarkNotificationReadRequest {

    @SerializedName("p_notification_id")
    private final String notificationId;

    public MarkNotificationReadRequest(
            String notificationId
    ) {
        this.notificationId = notificationId;
    }
}