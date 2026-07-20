package com.example.eco_print.utils;

import com.example.eco_print.BuildConfig;

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

    public static synchronized Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor loggingInterceptor =
                    new HttpLoggingInterceptor();

            // BODY logging exposes access tokens and request data. BASIC is
            // enough for local debugging, and release builds log nothing.
            loggingInterceptor.setLevel(
                    BuildConfig.DEBUG
                            ? HttpLoggingInterceptor.Level.BASIC
                            : HttpLoggingInterceptor.Level.NONE
            );

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(45, TimeUnit.SECONDS)
                    .writeTimeout(45, TimeUnit.SECONDS)
                    .callTimeout(60, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .addInterceptor(loggingInterceptor)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(SupabaseConfig.SUPABASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit;
    }
}
