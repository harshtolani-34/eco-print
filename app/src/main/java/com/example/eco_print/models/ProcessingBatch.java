package com.example.eco_print.models;

import com.google.gson.annotations.SerializedName;

public class ProcessingBatch {

    private String id;

    @SerializedName("report_id")
    private String reportId;

    @SerializedName("created_by")
    private String createdBy;

    @SerializedName("plastic_type")
    private String plasticType;

    @SerializedName("input_weight_kg")
    private double inputWeightKg;

    @SerializedName("usable_weight_kg")
    private double usableWeightKg;

    @SerializedName("rejected_weight_kg")
    private double rejectedWeightKg;

    private String stage;
    private String notes;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("completed_at")
    private String completedAt;

    public String getId() { return id; }
    public String getReportId() { return reportId; }
    public String getCreatedBy() { return createdBy; }
    public String getPlasticType() { return plasticType; }
    public double getInputWeightKg() { return inputWeightKg; }
    public double getUsableWeightKg() { return usableWeightKg; }
    public double getRejectedWeightKg() { return rejectedWeightKg; }
    public String getStage() { return stage; }
    public String getNotes() { return notes; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getCompletedAt() { return completedAt; }
}
