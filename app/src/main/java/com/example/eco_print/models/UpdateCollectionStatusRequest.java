package com.example.eco_print.models;

import com.google.gson.annotations.SerializedName;

public class UpdateCollectionStatusRequest {

    @SerializedName("p_report_id")
    private final String reportId;

    @SerializedName("p_new_status")
    private final String newStatus;

    public UpdateCollectionStatusRequest(
            String reportId,
            String newStatus
    ) {
        this.reportId = reportId;
        this.newStatus = newStatus;
    }
}
