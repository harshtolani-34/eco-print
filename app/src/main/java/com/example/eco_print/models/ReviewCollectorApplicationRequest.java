package com.example.eco_print.models;

import com.google.gson.annotations.SerializedName;

public class ReviewCollectorApplicationRequest {

    @SerializedName("p_collector_id")
    private final String collectorId;

    @SerializedName("p_action")
    private final String action;

    @SerializedName("p_rejection_reason")
    private final String rejectionReason;

    private ReviewCollectorApplicationRequest(
            String collectorId,
            String action,
            String rejectionReason
    ) {
        this.collectorId = collectorId;
        this.action = action;
        this.rejectionReason = rejectionReason;
    }

    public static ReviewCollectorApplicationRequest approve(
            String collectorId
    ) {
        return new ReviewCollectorApplicationRequest(
                collectorId,
                "approve",
                null
        );
    }

    public static ReviewCollectorApplicationRequest reject(
            String collectorId,
            String reason
    ) {
        return new ReviewCollectorApplicationRequest(
                collectorId,
                "reject",
                reason
        );
    }
}
