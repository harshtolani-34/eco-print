package com.example.eco_print.api;

import com.example.eco_print.models.ReviewCollectorApplicationRequest;
import com.example.eco_print.models.ReviewWasteReportRequest;
import com.example.eco_print.models.UserProfile;
import com.example.eco_print.models.WasteReport;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface AdminApi {

    @GET("rest/v1/waste_reports")
    Call<List<WasteReport>> getAllReports(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Query("select") String select,
            @Query("order") String order
    );

    @GET("rest/v1/waste_reports")
    Call<List<WasteReport>> getPendingReports(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Query("status") String statusFilter,
            @Query("select") String select,
            @Query("order") String order
    );

    @GET("rest/v1/waste_reports")
    Call<List<WasteReport>> getReportById(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Query("id") String idFilter,
            @Query("select") String select
    );

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/rpc/review_waste_report")
    Call<List<WasteReport>> reviewReport(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Body ReviewWasteReportRequest request
    );

    @GET("rest/v1/profiles")
    Call<List<UserProfile>> getCollectorApplications(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Query("role") String roleFilter,
            @Query("select") String select,
            @Query("order") String order
    );

    @GET("rest/v1/profiles")
    Call<List<UserProfile>> getCollectorById(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Query("id") String idFilter,
            @Query("select") String select
    );

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/rpc/review_collector_application")
    Call<List<UserProfile>> reviewCollectorApplication(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Body ReviewCollectorApplicationRequest request
    );
}
