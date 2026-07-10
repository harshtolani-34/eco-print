package com.example.eco_print.models;

import com.google.gson.annotations.SerializedName;

public class WasteReport {

    private String id;

    @SerializedName("user_id")
    private final String userId;

    @SerializedName("image_url")
    private final String imageUrl;

    @SerializedName("waste_type")
    private final String wasteType;

    @SerializedName("estimated_weight_kg")
    private final double estimatedWeightKg;

    private final String description;
    private final double latitude;
    private final double longitude;
    private final String address;
    private final String status;

    @SerializedName("created_at")
    private String createdAt;

    public WasteReport(String userId, String imageUrl, String wasteType,
                       double estimatedWeightKg, String description,
                       double latitude, double longitude, String address,
                       String status) {
        this.userId = userId;
        this.imageUrl = imageUrl;
        this.wasteType = wasteType;
        this.estimatedWeightKg = estimatedWeightKg;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.status = status;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getImageUrl() { return imageUrl; }
    public String getWasteType() { return wasteType; }
    public double getEstimatedWeightKg() { return estimatedWeightKg; }
    public String getDescription() { return description; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getAddress() { return address; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}
