package com.example.eco_print.models;

import com.google.gson.annotations.SerializedName;

public class RecordInventoryTransactionRequest {

    @SerializedName("p_inventory_id")
    private final String inventoryId;

    @SerializedName("p_transaction_type")
    private final String transactionType;

    @SerializedName("p_quantity_kg")
    private final double quantityKg;

    @SerializedName("p_reference_note")
    private final String referenceNote;

    public RecordInventoryTransactionRequest(
            String inventoryId,
            String transactionType,
            double quantityKg,
            String referenceNote
    ) {
        this.inventoryId = inventoryId;
        this.transactionType = transactionType;
        this.quantityKg = quantityKg;
        this.referenceNote = referenceNote;
    }
}
