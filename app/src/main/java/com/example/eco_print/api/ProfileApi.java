package com.example.eco_print.api;

import com.example.eco_print.models.CitizenProfileRequest;
import com.example.eco_print.models.CollectorApplicationRequest;
import com.example.eco_print.models.UserProfile;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ProfileApi {

    @GET("rest/v1/profiles")
    Call<List<UserProfile>> getMyProfile(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Query("id") String idFilter,
            @Query("select") String select
    );

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/rpc/create_citizen_profile")
    Call<List<UserProfile>> createCitizenProfile(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Body CitizenProfileRequest request
    );

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/rpc/apply_as_collector")
    Call<List<UserProfile>> applyAsCollector(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Body CollectorApplicationRequest request
    );
}
