package com.example.eco_print.models;

import com.google.gson.annotations.SerializedName;

public class FilamentInventory {

    private String id;

    @SerializedName("batch_id")
    private String batchId;

    @SerializedName("filament_type")
    private String filamentType;

    private String colour;

    @SerializedName("diameter_mm")
    private double diameterMm;

    @SerializedName("produced_weight_kg")
    private double producedWeightKg;

    @SerializedName("available_stock_kg")
    private double availableStockKg;

    @SerializedName("spool_count")
    private int spoolCount;

    @SerializedName("stock_status")
    private String stockStatus;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    public String getId() { return id; }
    public String getBatchId() { return batchId; }
    public String getFilamentType() { return filamentType; }
    public String getColour() { return colour; }
    public double getDiameterMm() { return diameterMm; }
    public double getProducedWeightKg() { return producedWeightKg; }
    public double getAvailableStockKg() { return availableStockKg; }
    public int getSpoolCount() { return spoolCount; }
    public String getStockStatus() { return stockStatus; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
