package com.example.eco_print.api;

import com.example.eco_print.models.AuthResponse;
import com.example.eco_print.models.User;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface AuthApi {

    @POST("auth/v1/signup")
    Call<AuthResponse> signUp(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Header("Content-Type") String contentType,
            @Body User user
    );

    @POST("auth/v1/token?grant_type=password")
    Call<AuthResponse> login(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Header("Content-Type") String contentType,
            @Body User user
    );
}
