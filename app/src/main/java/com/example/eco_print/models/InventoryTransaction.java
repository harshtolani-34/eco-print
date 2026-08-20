package com.example.eco_print.models;

import com.google.gson.annotations.SerializedName;

public class InventoryTransaction {

    private String id;

    @SerializedName("inventory_id")
    private String inventoryId;

    @SerializedName("transaction_type")
    private String transactionType;

    @SerializedName("quantity_kg")
    private double quantityKg;

    @SerializedName("reference_note")
    private String referenceNote;

    @SerializedName("performed_by")
    private String performedBy;

    @SerializedName("created_at")
    private String createdAt;

    public String getId() { return id; }
    public String getInventoryId() { return inventoryId; }
    public String getTransactionType() { return transactionType; }
    public double getQuantityKg() { return quantityKg; }
    public String getReferenceNote() { return referenceNote; }
    public String getPerformedBy() { return performedBy; }
    public String getCreatedAt() { return createdAt; }
}
