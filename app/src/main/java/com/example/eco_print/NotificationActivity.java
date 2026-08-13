package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eco_print.adapter.NotificationAdapter;
import com.example.eco_print.api.RewardsApi;
import com.example.eco_print.models.EcoNotification;
import com.example.eco_print.models.MarkNotificationReadRequest;
import com.example.eco_print.utils.SessionManager;
import com.example.eco_print.utils.SupabaseClient;
import com.example.eco_print.utils.SupabaseConfig;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActivity
        extends AppCompatActivity {

    private RecyclerView notificationsRecyclerView;

    private ProgressBar loadingProgress;

    private View stateContainer;

    private TextView stateText;
    private TextView summaryText;

    private MaterialButton retryButton;

    private NotificationAdapter adapter;
    private SessionManager sessionManager;

    private Call<List<EcoNotification>> notificationsCall;
    private Call<List<EcoNotification>> markReadCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_notifications
        );

        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        bindViews();

        adapter = new NotificationAdapter(
                this::openNotification
        );

        notificationsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        notificationsRecyclerView.setAdapter(adapter);

        notificationsRecyclerView.setHasFixedSize(true);
        notificationsRecyclerView.setItemAnimator(null);

        ImageButton backButton =
                findViewById(R.id.backButton);

        backButton.setOnClickListener(v ->
                getOnBackPressedDispatcher()
                        .onBackPressed()
        );

        retryButton.setOnClickListener(v ->
                loadNotifications()
        );
    }

    private void bindViews() {

        notificationsRecyclerView =
                findViewById(
                        R.id.notificationsRecyclerView
                );

        loadingProgress =
                findViewById(
                        R.id.loadingProgress
                );

        stateContainer =
                findViewById(
                        R.id.stateContainer
                );

        stateText =
                findViewById(
                        R.id.stateText
                );

        summaryText =
                findViewById(
                        R.id.summaryText
                );

        retryButton =
                findViewById(
                        R.id.retryButton
                );
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadNotifications();
    }

    private void loadNotifications() {

        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        if (!SupabaseConfig.isConfigured()) {
            showError(
                    "Supabase is not configured."
            );
            return;
        }

        if (notificationsCall != null) {
            notificationsCall.cancel();
        }

        showLoading();

        RewardsApi api =
                SupabaseClient.getClient()
                        .create(RewardsApi.class);

        notificationsCall =
                api.getMyNotifications(
                        SupabaseConfig.SUPABASE_ANON_KEY,
                        sessionManager.getAuthorizationHeader(),
                        "*",
                        "created_at.desc"
                );

        notificationsCall.enqueue(
                new Callback<List<EcoNotification>>() {

                    @Override
                    public void onResponse(
                            Call<List<EcoNotification>> call,
                            Response<List<EcoNotification>> response
                    ) {
                        notificationsCall = null;

                        if (isFinishing()
                                || isDestroyed()) {
                            return;
                        }

                        if (response.code() == 401) {

                            showToast(
                                    "Your session expired"
                            );

                            redirectToLogin();
                            return;
                        }

                        if (!response.isSuccessful()
                                || response.body() == null) {

                            showError(
                                    "Notifications could not be loaded. Error "
                                            + response.code()
                            );

                            return;
                        }

                        List<EcoNotification> notifications =
                                response.body();

                        adapter.setNotifications(
                                notifications
                        );

                        int unreadCount = 0;

                        for (EcoNotification notification :
                                notifications) {

                            if (!notification.isRead()) {
                                unreadCount++;
                            }
                        }

                        int total =
                                notifications.size();

                        if (total == 0) {

                            showEmpty();

                        } else {

                            summaryText.setText(
                                    unreadCount == 0
                                            ? total
                                              + " notifications • All caught up"
                                            : unreadCount
                                              + " unread • "
                                              + total
                                              + " total"
                            );

                            showNotifications();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<List<EcoNotification>> call,
                            Throwable throwable
                    ) {
                        notificationsCall = null;

                        if (call.isCanceled()
                                || isFinishing()
                                || isDestroyed()) {
                            return;
                        }

                        showError(
                                "Could not connect to the server. Check your internet and try again."
                        );
                    }
                }
        );
    }

    private void openNotification(
            EcoNotification notification
    ) {

        if (notification == null) {
            return;
        }

        if (!notification.isRead()) {
            markAsRead(notification);
        }

        String reportId =
                notification.getReportId();

        if (reportId == null
                || reportId.trim().isEmpty()) {

            showToast(
                    "This notification does not contain a report"
            );

            return;
        }

        Intent intent =
                new Intent(
                        this,
                        ReportDetailsActivity.class
                );

        intent.putExtra(
                ReportDetailsActivity.EXTRA_REPORT_ID,
                reportId
        );

        startActivity(intent);
    }

    private void markAsRead(
            EcoNotification notification
    ) {

        if (notification.getId() == null
                || notification.getId().trim().isEmpty()) {
            return;
        }

        RewardsApi api =
                SupabaseClient.getClient()
                        .create(RewardsApi.class);

        markReadCall =
                api.markNotificationRead(
                        SupabaseConfig.SUPABASE_ANON_KEY,
                        sessionManager.getAuthorizationHeader(),
                        new MarkNotificationReadRequest(
                                notification.getId()
                        )
                );

        markReadCall.enqueue(
                new Callback<List<EcoNotification>>() {

                    @Override
                    public void onResponse(
                            Call<List<EcoNotification>> call,
                            Response<List<EcoNotification>> response
                    ) {
                        markReadCall = null;

                        /*
                         * No toast needed here.
                         *
                         * When the citizen returns to this Activity,
                         * onResume() reloads the list and the notification
                         * appears as read.
                         */
                    }

                    @Override
                    public void onFailure(
                            Call<List<EcoNotification>> call,
                            Throwable throwable
                    ) {
                        markReadCall = null;
                    }
                }
        );
    }

    private void showLoading() {

        loadingProgress.setVisibility(
                View.VISIBLE
        );

        notificationsRecyclerView.setVisibility(
                View.GONE
        );

        stateContainer.setVisibility(
                View.GONE
        );

        summaryText.setText(
                "Loading notifications..."
        );
    }

    private void showNotifications() {

        loadingProgress.setVisibility(
                View.GONE
        );

        stateContainer.setVisibility(
                View.GONE
        );

        notificationsRecyclerView.setVisibility(
                View.VISIBLE
        );
    }

    private void showEmpty() {

        loadingProgress.setVisibility(
                View.GONE
        );

        notificationsRecyclerView.setVisibility(
                View.GONE
        );

        stateContainer.setVisibility(
                View.VISIBLE
        );

        retryButton.setVisibility(
                View.GONE
        );

        stateText.setText(
                "No notifications yet\n\n"
                        + "Updates about your plastic-waste reports "
                        + "will appear here."
        );

        summaryText.setText(
                "You're all caught up"
        );
    }

    private void showError(
            String message
    ) {

        loadingProgress.setVisibility(
                View.GONE
        );

        notificationsRecyclerView.setVisibility(
                View.GONE
        );

        stateContainer.setVisibility(
                View.VISIBLE
        );

        retryButton.setVisibility(
                View.VISIBLE
        );

        stateText.setText(message);

        summaryText.setText(
                "Notifications could not be loaded"
        );
    }

    private void redirectToLogin() {

        sessionManager.logout();

        Intent intent =
                new Intent(
                        this,
                        LoginActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    private void showToast(
            String message
    ) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    protected void onDestroy() {

        if (notificationsCall != null) {
            notificationsCall.cancel();
            notificationsCall = null;
        }

        if (markReadCall != null) {
            markReadCall.cancel();
            markReadCall = null;
        }

        super.onDestroy();
    }
}