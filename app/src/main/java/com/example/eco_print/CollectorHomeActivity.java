package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eco_print.adapter.CollectorReportAdapter;
import com.example.eco_print.api.WasteReportApi;
import com.example.eco_print.models.AcceptReportRequest;
import com.example.eco_print.models.WasteReport;
import com.example.eco_print.utils.SessionManager;
import com.example.eco_print.utils.SupabaseClient;
import com.example.eco_print.utils.SupabaseConfig;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CollectorHomeActivity extends AppCompatActivity {

    private TextView greetingText;
    private TextView availableCountText;
    private TextView acceptedCountText;
    private TextView emptyStateText;
    private ProgressBar loadingProgress;
    private RecyclerView reportsRecyclerView;
    private ShapeableImageView profileImage;
    private View stateContainer;
    private MaterialButton retryButton;

    private SessionManager sessionManager;
    private CollectorReportAdapter adapter;

    private Call<List<WasteReport>> availableReportsCall;
    private Call<List<WasteReport>> acceptedReportsCall;
    private Call<List<WasteReport>> acceptReportCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collector_home);

        greetingText = findViewById(R.id.greetingText);
        availableCountText = findViewById(R.id.availableCountText);
        acceptedCountText = findViewById(R.id.acceptedCountText);
        emptyStateText = findViewById(R.id.emptyStateText);
        loadingProgress = findViewById(R.id.loadingProgress);
        reportsRecyclerView = findViewById(R.id.reportsRecyclerView);
        profileImage = findViewById(R.id.profileImage);
        stateContainer = findViewById(R.id.stateContainer);
        retryButton = findViewById(R.id.retryButton);

        sessionManager = new SessionManager(this);

        if (!sessionManager.isCollector()) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        String collectorName = sessionManager.getUserName();
        greetingText.setText(
                collectorName.isEmpty()
                        ? "Ready for collection?"
                        : "Welcome, " + collectorName
        );

        adapter = new CollectorReportAdapter(
                this::showAcceptConfirmation
        );

        reportsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );
        reportsRecyclerView.setAdapter(adapter);
        reportsRecyclerView.setHasFixedSize(true);
        reportsRecyclerView.setItemAnimator(null);

        profileImage.setOnClickListener(v -> showProfileMenu());
        retryButton.setOnClickListener(v -> loadCollectorDashboard());
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sessionManager != null && sessionManager.isCollector()) {
            loadCollectorDashboard();
        }
    }

    private void loadCollectorDashboard() {
        if (!sessionManager.isLoggedIn()) {
            logout();
            return;
        }

        if (!SupabaseConfig.isConfigured()) {
            showErrorState(
                    "Supabase is not configured. Add the anon key and try again."
            );
            return;
        }

        loadAvailableReports();
        loadAcceptedCount();
    }

    private void loadAvailableReports() {
        if (availableReportsCall != null) {
            availableReportsCall.cancel();
        }

        showLoading(true);

        WasteReportApi api = SupabaseClient.getClient()
                .create(WasteReportApi.class);

        availableReportsCall = api.getAvailableReports(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "eq.Pending",
                "is.null",
                "*",
                "created_at.desc"
        );

        availableReportsCall.enqueue(new Callback<List<WasteReport>>() {
            @Override
            public void onResponse(
                    Call<List<WasteReport>> call,
                    Response<List<WasteReport>> response
            ) {
                availableReportsCall = null;

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                showLoading(false);

                if (response.code() == 401) {
                    showToast("Your session expired");
                    logout();
                    return;
                }

                if (!response.isSuccessful() || response.body() == null) {
                    showErrorState(
                            "Available reports could not be loaded. Tap Try Again."
                    );
                    return;
                }

                List<WasteReport> reports = response.body();
                adapter.setReports(reports);
                availableCountText.setText(String.valueOf(reports.size()));

                if (reports.isEmpty()) {
                    showEmptyState();
                } else {
                    showReports();
                }
            }

            @Override
            public void onFailure(
                    Call<List<WasteReport>> call,
                    Throwable throwable
            ) {
                availableReportsCall = null;

                if (call.isCanceled()
                        || isFinishing()
                        || isDestroyed()) {
                    return;
                }

                showLoading(false);
                showErrorState(
                        "Could not connect to the server. Check your internet and try again."
                );
            }
        });
    }

    private void loadAcceptedCount() {
        if (acceptedReportsCall != null) {
            acceptedReportsCall.cancel();
        }

        WasteReportApi api = SupabaseClient.getClient()
                .create(WasteReportApi.class);

        acceptedReportsCall = api.getAcceptedReports(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "eq." + sessionManager.getUserId(),
                "id",
                "created_at.desc"
        );

        acceptedReportsCall.enqueue(new Callback<List<WasteReport>>() {
            @Override
            public void onResponse(
                    Call<List<WasteReport>> call,
                    Response<List<WasteReport>> response
            ) {
                acceptedReportsCall = null;

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    acceptedCountText.setText(
                            String.valueOf(response.body().size())
                    );
                }
            }

            @Override
            public void onFailure(
                    Call<List<WasteReport>> call,
                    Throwable throwable
            ) {
                acceptedReportsCall = null;
                // Available reports remain usable if this count fails.
            }
        });
    }

    private void showAcceptConfirmation(WasteReport report) {
        new AlertDialog.Builder(this)
                .setTitle("Accept Collection?")
                .setMessage(
                        "You will become responsible for collecting this waste report. The citizen will immediately see the status as Assigned."
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton(
                        "Accept",
                        (dialog, which) -> acceptReport(report)
                )
                .show();
    }

    private void acceptReport(WasteReport report) {
        if (report.getId() == null
                || report.getId().trim().isEmpty()) {
            showToast("Report ID is missing");
            return;
        }

        if (acceptReportCall != null) {
            return;
        }

        adapter.setAcceptingReportId(report.getId());

        WasteReportApi api = SupabaseClient.getClient()
                .create(WasteReportApi.class);

        acceptReportCall = api.acceptWasteReport(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                new AcceptReportRequest(report.getId())
        );

        acceptReportCall.enqueue(new Callback<List<WasteReport>>() {
            @Override
            public void onResponse(
                    Call<List<WasteReport>> call,
                    Response<List<WasteReport>> response
            ) {
                acceptReportCall = null;
                adapter.setAcceptingReportId("");

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (response.code() == 401) {
                    showToast("Your session expired");
                    logout();
                    return;
                }

                if (!response.isSuccessful()) {
                    showAcceptError(
                            report,
                            "The report could not be accepted. Error "
                                    + response.code()
                    );
                    return;
                }

                if (response.body() == null
                        || response.body().isEmpty()) {
                    showToast(
                            "Another collector may have already accepted it."
                    );
                    loadCollectorDashboard();
                    return;
                }

                showToast("Collection accepted successfully");
                loadCollectorDashboard();
            }

            @Override
            public void onFailure(
                    Call<List<WasteReport>> call,
                    Throwable throwable
            ) {
                acceptReportCall = null;
                adapter.setAcceptingReportId("");

                if (call.isCanceled()
                        || isFinishing()
                        || isDestroyed()) {
                    return;
                }

                showAcceptError(
                        report,
                        "Check your internet connection and try again."
                );
            }
        });
    }

    private void showAcceptError(
            WasteReport report,
            String message
    ) {
        new AlertDialog.Builder(this)
                .setTitle("Collection not accepted")
                .setMessage(message)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(
                        "Try Again",
                        (dialog, which) -> acceptReport(report)
                )
                .show();
    }

    private void showLoading(boolean loading) {
        loadingProgress.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        if (loading) {
            reportsRecyclerView.setVisibility(View.GONE);
            stateContainer.setVisibility(View.GONE);
        }
    }

    private void showReports() {
        loadingProgress.setVisibility(View.GONE);
        stateContainer.setVisibility(View.GONE);
        reportsRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        loadingProgress.setVisibility(View.GONE);
        reportsRecyclerView.setVisibility(View.GONE);
        stateContainer.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
        emptyStateText.setText(
                "No available reports right now.\n\nNew Pending citizen reports will appear here."
        );
    }

    private void showErrorState(String message) {
        loadingProgress.setVisibility(View.GONE);
        reportsRecyclerView.setVisibility(View.GONE);
        stateContainer.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.VISIBLE);
        emptyStateText.setText(message);
    }

    private void showProfileMenu() {
        String name = sessionManager.getUserName();
        String displayName = name.isEmpty() ? "Collector" : name;

        new AlertDialog.Builder(this)
                .setTitle(displayName)
                .setMessage(
                        sessionManager.getUserEmail()
                                + "\n\nRole: Collector"
                )
                .setNegativeButton("Close", null)
                .setPositiveButton(
                        "Logout",
                        (dialog, which) -> logout()
                )
                .show();
    }

    private void logout() {
        sessionManager.logout();

        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        if (availableReportsCall != null) {
            availableReportsCall.cancel();
            availableReportsCall = null;
        }
        if (acceptedReportsCall != null) {
            acceptedReportsCall.cancel();
            acceptedReportsCall = null;
        }
        if (acceptReportCall != null) {
            acceptReportCall.cancel();
            acceptReportCall = null;
        }
        super.onDestroy();
    }
}
