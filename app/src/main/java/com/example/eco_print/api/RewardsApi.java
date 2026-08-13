package com.example.eco_print.api;

import com.example.eco_print.models.EcoNotification;
import com.example.eco_print.models.MarkNotificationReadRequest;
import com.example.eco_print.models.RewardTransaction;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface RewardsApi {

    /*
     * =====================================================
     * ECO POINT BALANCE
     * =====================================================
     */

    @POST("rest/v1/rpc/get_my_eco_points")
    Call<Integer> getMyEcoPoints(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization
    );


    /*
     * =====================================================
     * REWARD HISTORY
     * =====================================================
     */

    @GET("rest/v1/reward_transactions")
    Call<List<RewardTransaction>> getMyRewards(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Query("select") String select,
            @Query("order") String order
    );


    /*
     * =====================================================
     * NOTIFICATION HISTORY
     * =====================================================
     */

    @GET("rest/v1/user_notifications")
    Call<List<EcoNotification>> getMyNotifications(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Query("select") String select,
            @Query("order") String order
    );


    /*
     * =====================================================
     * MARK NOTIFICATION AS READ
     * =====================================================
     */

    @POST("rest/v1/rpc/mark_notification_read")
    Call<List<EcoNotification>> markNotificationRead(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Body MarkNotificationReadRequest request
    );
}