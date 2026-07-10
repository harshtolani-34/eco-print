package com.example.eco_print.models;

import com.google.gson.annotations.SerializedName;

public class AcceptReportRequest {

    @SerializedName("p_report_id")
    private final String reportId;

    public AcceptReportRequest(String reportId) {
        this.reportId = reportId;
    }
}
