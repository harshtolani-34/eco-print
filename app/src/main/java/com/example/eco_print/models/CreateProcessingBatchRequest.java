package com.example.eco_print.models;

import com.google.gson.annotations.SerializedName;

public class CreateProcessingBatchRequest {

    @SerializedName("p_report_id")
    private final String reportId;

    @SerializedName("p_usable_weight_kg")
    private final double usableWeightKg;

    @SerializedName("p_rejected_weight_kg")
    private final double rejectedWeightKg;

    @SerializedName("p_notes")
    private final String notes;

    public CreateProcessingBatchRequest(
            String reportId,
            double usableWeightKg,
            double rejectedWeightKg,
            String notes
    ) {
        this.reportId = reportId;
        this.usableWeightKg = usableWeightKg;
        this.rejectedWeightKg = rejectedWeightKg;
        this.notes = notes;
    }
}
