package com.example.eco_print.utils;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class SupabaseClient {

    private static Retrofit retrofit;

    private SupabaseClient() {
        // Prevent object creation.
    }

    public static Retrofit getClient() {

        if (retrofit == null) {

            HttpLoggingInterceptor loggingInterceptor =
                    new HttpLoggingInterceptor();

            loggingInterceptor.setLevel(
                    HttpLoggingInterceptor.Level.BODY
            );

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(45, TimeUnit.SECONDS)
                    .writeTimeout(45, TimeUnit.SECONDS)
                    .addInterceptor(loggingInterceptor)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(SupabaseConfig.SUPABASE_URL)
                    .client(client)
                    .addConverterFactory(
                            GsonConverterFactory.create()
                    )
                    .build();
        }

        return retrofit;
    }
}
