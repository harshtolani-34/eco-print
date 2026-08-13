package com.example.eco_print.models;

import com.google.gson.annotations.SerializedName;

public class ReviewWasteReportRequest {

    @SerializedName("p_report_id")
    private final String reportId;

    @SerializedName("p_action")
    private final String action;

    @SerializedName("p_rejection_reason")
    private final String rejectionReason;

    public ReviewWasteReportRequest(
            String reportId,
            String action,
            String rejectionReason
    ) {
        this.reportId = reportId;
        this.action = action;
        this.rejectionReason = rejectionReason;
    }

    public static ReviewWasteReportRequest verify(String reportId) {
        return new ReviewWasteReportRequest(reportId, "verify", null);
    }

    public static ReviewWasteReportRequest reject(
            String reportId,
            String reason
    ) {
        return new ReviewWasteReportRequest(reportId, "reject", reason);
    }
}
