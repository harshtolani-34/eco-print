package com.example.eco_print.api;

import com.example.eco_print.models.FcmTokenRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface PushApi {

    @POST("rest/v1/rpc/save_my_fcm_token")
    Call<Void> saveMyFcmToken(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Body FcmTokenRequest request
    );
}