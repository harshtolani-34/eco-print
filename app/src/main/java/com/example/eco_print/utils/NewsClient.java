package com.example.eco_print.utils;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class NewsClient {

    private static final String BASE_URL =
            "https://gnews.io/api/v4/";

    private static volatile Retrofit retrofit;

    private NewsClient() {
        // Prevent object creation.
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            synchronized (NewsClient.class) {
                if (retrofit == null) {
                    OkHttpClient client =
                            new OkHttpClient.Builder()
                                    .connectTimeout(
                                            12,
                                            TimeUnit.SECONDS
                                    )
                                    .readTimeout(
                                            18,
                                            TimeUnit.SECONDS
                                    )
                                    .writeTimeout(
                                            18,
                                            TimeUnit.SECONDS
                                    )
                                    .callTimeout(
                                            25,
                                            TimeUnit.SECONDS
                                    )
                                    .retryOnConnectionFailure(true)
                                    .build();

                    retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(client)
                            .addConverterFactory(
                                    GsonConverterFactory.create()
                            )
                            .build();
                }
            }
        }

        return retrofit;
    }
}
