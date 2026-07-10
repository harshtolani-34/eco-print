package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CollectorHomeActivity
        extends AppCompatActivity {

    private TextView greetingText;
    private TextView availableCountText;
    private TextView acceptedCountText;
    private TextView emptyStateText;
    private ProgressBar loadingProgress;
    private RecyclerView reportsRecyclerView;
    private Button logoutButton;

    private SessionManager sessionManager;
    private CollectorReportAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(
                R.layout.activity_collector_home
        );

        greetingText =
                findViewById(R.id.greetingText);

        availableCountText =
                findViewById(R.id.availableCountText);

        acceptedCountText =
                findViewById(R.id.acceptedCountText);

        emptyStateText =
                findViewById(R.id.emptyStateText);

        loadingProgress =
                findViewById(R.id.loadingProgress);

        reportsRecyclerView =
                findViewById(R.id.reportsRecyclerView);

        logoutButton =
                findViewById(R.id.logoutButton);

        sessionManager =
                new SessionManager(this);

        if (!sessionManager.isCollector()) {
            startActivity(
                    new Intent(
                            this,
                            HomeActivity.class
                    )
            );
            finish();
            return;
        }

        String collectorName =
                sessionManager.getUserName();

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

        logoutButton.setOnClickListener(v -> logout());
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sessionManager != null
                && sessionManager.isCollector()) {
            loadCollectorDashboard();
        }
    }

    private void loadCollectorDashboard() {
        loadAvailableReports();
        loadAcceptedCount();
    }

    private void loadAvailableReports() {
        showLoading(true);

        WasteReportApi api =
                SupabaseClient.getClient()
                        .create(WasteReportApi.class);

        api.getAvailableReports(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "eq.Pending",
                "is.null",
                "*",
                "created_at.desc"
        ).enqueue(new Callback<List<WasteReport>>() {

            @Override
            public void onResponse(
                    Call<List<WasteReport>> call,
                    Response<List<WasteReport>> response
            ) {
                showLoading(false);

                if (response.code() == 401) {
                    showToast("Your session expired");
                    logout();
                    return;
                }

                if (!response.isSuccessful()
                        || response.body() == null) {
                    showToast(
                            "Unable to load reports. Error code: "
                                    + response.code()
                    );
                    showEmptyState(true);
                    return;
                }

                List<WasteReport> reports =
                        response.body();

                adapter.setReports(reports);

                availableCountText.setText(
                        String.valueOf(reports.size())
                );

                showEmptyState(reports.isEmpty());
            }

            @Override
            public void onFailure(
                    Call<List<WasteReport>> call,
                    Throwable throwable
            ) {
                showLoading(false);
                showEmptyState(true);

                showToast(
                        "Unable to load reports: "
                                + throwable.getMessage()
                );
            }
        });
    }

    private void loadAcceptedCount() {
        WasteReportApi api =
                SupabaseClient.getClient()
                        .create(WasteReportApi.class);

        api.getAcceptedReports(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "eq." + sessionManager.getUserId(),
                "id",
                "created_at.desc"
        ).enqueue(new Callback<List<WasteReport>>() {

            @Override
            public void onResponse(
                    Call<List<WasteReport>> call,
                    Response<List<WasteReport>> response
            ) {
                if (response.isSuccessful()
                        && response.body() != null) {
                    acceptedCountText.setText(
                            String.valueOf(
                                    response.body().size()
                            )
                    );
                }
            }

            @Override
            public void onFailure(
                    Call<List<WasteReport>> call,
                    Throwable throwable
            ) {
                // Available reports remain usable even if this count fails.
            }
        });
    }

    private void showAcceptConfirmation(
            WasteReport report
    ) {
        new AlertDialog.Builder(this)
                .setTitle("Accept Collection?")
                .setMessage(
                        "You will become responsible for collecting this "
                                + "waste report. The citizen will immediately "
                                + "see the status as Assigned."
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

        adapter.setAcceptingReportId(report.getId());

        WasteReportApi api =
                SupabaseClient.getClient()
                        .create(WasteReportApi.class);

        api.acceptWasteReport(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                new AcceptReportRequest(report.getId())
        ).enqueue(new Callback<List<WasteReport>>() {

            @Override
            public void onResponse(
                    Call<List<WasteReport>> call,
                    Response<List<WasteReport>> response
            ) {
                adapter.setAcceptingReportId("");

                if (response.code() == 401) {
                    showToast("Your session expired");
                    logout();
                    return;
                }

                if (!response.isSuccessful()) {
                    showToast(
                            "Could not accept report. Error code: "
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
                adapter.setAcceptingReportId("");

                showToast(
                        "Accept failed: "
                                + throwable.getMessage()
                );
            }
        });
    }

    private void showLoading(boolean loading) {
        loadingProgress.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        if (loading) {
            reportsRecyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(boolean empty) {
        emptyStateText.setVisibility(
                empty ? View.VISIBLE : View.GONE
        );

        reportsRecyclerView.setVisibility(
                empty ? View.GONE : View.VISIBLE
        );
    }

    private void logout() {
        sessionManager.logout();

        Intent intent = new Intent(
                this,
                WelcomeActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}
