package com.example.eco_print.api;

import com.example.eco_print.models.CreateFilamentRequest;
import com.example.eco_print.models.CreateProcessingBatchRequest;
import com.example.eco_print.models.FilamentInventory;
import com.example.eco_print.models.InventoryTransaction;
import com.example.eco_print.models.ProcessingBatch;
import com.example.eco_print.models.RecordInventoryTransactionRequest;
import com.example.eco_print.models.UpdateProcessingStageRequest;
import com.example.eco_print.models.WasteReport;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface InventoryApi {

    @GET("rest/v1/waste_reports")
    Call<List<WasteReport>> getCollectedReports(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Query("status") String statusFilter,
            @Query("select") String select,
            @Query("order") String order
    );

    @GET("rest/v1/processing_batches")
    Call<List<ProcessingBatch>> getProcessingBatches(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Query("select") String select,
            @Query("order") String order
    );

    @GET("rest/v1/processing_batches")
    Call<List<ProcessingBatch>> getProcessingBatchById(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Query("id") String idFilter,
            @Query("select") String select
    );

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/rpc/create_processing_batch")
    Call<List<ProcessingBatch>> createProcessingBatch(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Body CreateProcessingBatchRequest request
    );

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/rpc/update_processing_stage")
    Call<List<ProcessingBatch>> updateProcessingStage(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Body UpdateProcessingStageRequest request
    );

    @GET("rest/v1/filament_inventory")
    Call<List<FilamentInventory>> getFilamentInventory(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Query("select") String select,
            @Query("order") String order
    );

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/rpc/create_filament_inventory")
    Call<List<FilamentInventory>> createFilamentInventory(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Body CreateFilamentRequest request
    );

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/rpc/record_inventory_transaction")
    Call<List<FilamentInventory>> recordInventoryTransaction(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Body RecordInventoryTransactionRequest request
    );

    @GET("rest/v1/inventory_transactions")
    Call<List<InventoryTransaction>> getInventoryTransactions(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Query("select") String select,
            @Query("order") String order
    );
}
