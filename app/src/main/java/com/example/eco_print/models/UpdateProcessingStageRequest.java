package com.example.eco_print.models;

import com.google.gson.annotations.SerializedName;

public class UpdateProcessingStageRequest {

    @SerializedName("p_batch_id")
    private final String batchId;

    @SerializedName("p_new_stage")
    private final String newStage;

    public UpdateProcessingStageRequest(
            String batchId,
            String newStage
    ) {
        this.batchId = batchId;
        this.newStage = newStage;
    }
}
