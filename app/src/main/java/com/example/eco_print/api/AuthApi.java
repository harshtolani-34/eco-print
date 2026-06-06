package com.example.eco_print.api;

import com.example.eco_print.models.User;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface AuthApi {

    @Headers({
            "apikey:",
            "Authorization:",
            "Content-Type: application/json"
    })

    @POST("auth/v1/signup")
    Call<Object> signUp(@Body User user);

    @Headers({
            "apikey: ",
            "Authorization:",
            "Content-Type: application/json"
    })
    @POST("auth/v1/token?grant_type=password")
    Call<Object> login(@Body User user);
}