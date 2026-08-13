package com.example.eco_print.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.eco_print.api.PushApi;
import com.example.eco_print.models.FcmTokenRequest;
import com.google.firebase.messaging.FirebaseMessaging;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class PushNotificationManager {

    private static final int NOTIFICATION_PERMISSION_REQUEST = 6001;

    private PushNotificationManager() {
    }

    public static void requestPermission(Activity activity) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ActivityCompat.requestPermissions(
                activity,
                new String[]{
                        Manifest.permission.POST_NOTIFICATIONS
                },
                NOTIFICATION_PERMISSION_REQUEST
        );
    }

    public static void syncToken(Context context) {

        SessionManager sessionManager =
                new SessionManager(context);

        if (!sessionManager.isLoggedIn()) {
            return;
        }

        FirebaseMessaging.getInstance()
                .getToken()
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        return;
                    }

                    String token = task.getResult();

                    if (token == null || token.trim().isEmpty()) {
                        return;
                    }

                    saveToken(
                            context,
                            token.trim()
                    );
                });
    }

    public static void saveToken(
            Context context,
            String token
    ) {

        SessionManager sessionManager =
                new SessionManager(context);

        if (!sessionManager.isLoggedIn()) {
            return;
        }

        PushApi api = SupabaseClient.getClient()
                .create(PushApi.class);

        api.saveMyFcmToken(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                new FcmTokenRequest(token)
        ).enqueue(new Callback<Void>() {

            @Override
            public void onResponse(
                    Call<Void> call,
                    Response<Void> response
            ) {
                // Nothing needs to be shown to the citizen.
            }

            @Override
            public void onFailure(
                    Call<Void> call,
                    Throwable throwable
            ) {
                // Token will be retried when Home opens again.
            }
        });
    }
}