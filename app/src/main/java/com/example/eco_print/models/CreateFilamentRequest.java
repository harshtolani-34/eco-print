package com.example.eco_print.models;

import com.google.gson.annotations.SerializedName;

public class CreateFilamentRequest {

    @SerializedName("p_batch_id")
    private final String batchId;

    @SerializedName("p_filament_type")
    private final String filamentType;

    @SerializedName("p_colour")
    private final String colour;

    @SerializedName("p_diameter_mm")
    private final double diameterMm;

    @SerializedName("p_produced_weight_kg")
    private final double producedWeightKg;

    @SerializedName("p_spool_count")
    private final int spoolCount;

    public CreateFilamentRequest(
            String batchId,
            String filamentType,
            String colour,
            double diameterMm,
            double producedWeightKg,
            int spoolCount
    ) {
        this.batchId = batchId;
        this.filamentType = filamentType;
        this.colour = colour;
        this.diameterMm = diameterMm;
        this.producedWeightKg = producedWeightKg;
        this.spoolCount = spoolCount;
    }
}
