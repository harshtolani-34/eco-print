package com.example.eco_print.utils;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseClient {

    private static final String BASE_URL =
            "https://mgjikkjowdugemmmwjpz.supabase.co/";

    private static Retrofit retrofit;

    public static Retrofit getClient() {

        if (retrofit == null) {

            HttpLoggingInterceptor interceptor =
                    new HttpLoggingInterceptor();

            interceptor.setLevel(
                    HttpLoggingInterceptor.Level.BODY
            );

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit;
    }
}