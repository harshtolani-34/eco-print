package com.example.eco_print.api;

import com.example.eco_print.models.AcceptReportRequest;
import com.example.eco_print.models.StorageUploadResponse;
import com.example.eco_print.models.WasteReport;

import java.util.List;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface WasteReportApi {

    @POST("storage/v1/object/{bucket}/{path}")
    Call<StorageUploadResponse> uploadWasteImage(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Header("x-upsert") String upsert,
            @Header("Content-Type") String contentType,
            @Path("bucket") String bucket,
            @Path(value = "path", encoded = true) String path,
            @Body RequestBody imageBody
    );

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/waste_reports")
    Call<List<WasteReport>> createWasteReport(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Body WasteReport wasteReport
    );

    @GET("rest/v1/waste_reports")
    Call<List<WasteReport>> getMyWasteReports(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Query("select") String select,
            @Query("order") String order
    );

    @GET("rest/v1/waste_reports")
    Call<List<WasteReport>> getWasteReportById(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Query("id") String idFilter,
            @Query("select") String select
    );

    @GET("rest/v1/waste_reports")
    Call<List<WasteReport>> getAvailableReports(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Query("status") String statusFilter,
            @Query("collector_id") String collectorFilter,
            @Query("select") String select,
            @Query("order") String order
    );

    @GET("rest/v1/waste_reports")
    Call<List<WasteReport>> getAcceptedReports(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Query("collector_id") String collectorFilter,
            @Query("select") String select,
            @Query("order") String order
    );

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/rpc/accept_waste_report")
    Call<List<WasteReport>> acceptWasteReport(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Body AcceptReportRequest request
    );
}
