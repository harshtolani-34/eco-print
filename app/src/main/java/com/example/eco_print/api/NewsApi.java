package com.example.eco_print.api;

import com.example.eco_print.models.NewsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NewsApi {

    @GET("search")
    Call<NewsResponse> getNews(
            @Query("q") String query,
            @Query("lang") String language,
            @Query("max") int max,
            @Query("apikey") String apiKey
    );
}