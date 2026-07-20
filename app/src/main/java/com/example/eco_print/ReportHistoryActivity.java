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

import com.example.eco_print.adapter.ReportHistoryAdapter;
import com.example.eco_print.api.WasteReportApi;
import com.example.eco_print.models.WasteReport;
import com.example.eco_print.utils.SessionManager;
import com.example.eco_print.utils.SupabaseClient;
import com.example.eco_print.utils.SupabaseConfig;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportHistoryActivity extends AppCompatActivity {

    private RecyclerView reportsRecyclerView;
    private ProgressBar loadingProgress;
    private View stateContainer;
    private TextView emptyStateText;
    private TextView reportCountText;
    private MaterialButton retryButton;

    private ReportHistoryAdapter adapter;
    private SessionManager sessionManager;
    private Call<List<WasteReport>> reportsCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_history);

        ImageButton backButton = findViewById(R.id.backButton);
        reportsRecyclerView = findViewById(R.id.reportsRecyclerView);
        loadingProgress = findViewById(R.id.loadingProgress);
        stateContainer = findViewById(R.id.stateContainer);
        emptyStateText = findViewById(R.id.emptyStateText);
        reportCountText = findViewById(R.id.reportCountText);
        retryButton = findViewById(R.id.retryButton);

        sessionManager = new SessionManager(this);
        adapter = new ReportHistoryAdapter();

        reportsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );
        reportsRecyclerView.setAdapter(adapter);
        reportsRecyclerView.setHasFixedSize(true);
        reportsRecyclerView.setItemAnimator(null);

        backButton.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );
        retryButton.setOnClickListener(v -> loadReports());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReports();
    }

    private void loadReports() {
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        if (!SupabaseConfig.isConfigured()) {
            showErrorState(
                    "Supabase is not configured. Add the anon key and try again."
            );
            return;
        }

        if (reportsCall != null) {
            reportsCall.cancel();
        }

        showLoading(true);

        WasteReportApi api = SupabaseClient.getClient()
                .create(WasteReportApi.class);

        reportsCall = api.getMyWasteReports(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "eq." + sessionManager.getUserId(),
                "*",
                "created_at.desc"
        );

        reportsCall.enqueue(new Callback<List<WasteReport>>() {
            @Override
            public void onResponse(
                    Call<List<WasteReport>> call,
                    Response<List<WasteReport>> response
            ) {
                reportsCall = null;

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                showLoading(false);

                if (response.code() == 401) {
                    showToast("Your session expired. Please log in again.");
                    redirectToLogin();
                    return;
                }

                if (!response.isSuccessful() || response.body() == null) {
                    showErrorState(
                            "Unable to load your reports. Tap Try Again."
                    );
                    return;
                }

                List<WasteReport> reports = response.body();
                adapter.setReports(reports);

                int count = reports.size();
                reportCountText.setText(
                        count == 1
                                ? "1 submitted report"
                                : count + " submitted reports"
                );

                if (count == 0) {
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
                reportsCall = null;

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

    private void showLoading(boolean loading) {
        loadingProgress.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        if (loading) {
            reportsRecyclerView.setVisibility(View.GONE);
            stateContainer.setVisibility(View.GONE);
            reportCountText.setText("Loading your submitted reports...");
        }
    }

    private void showReports() {
        loadingProgress.setVisibility(View.GONE);
        stateContainer.setVisibility(View.GONE);
        reportsRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        reportsRecyclerView.setVisibility(View.GONE);
        stateContainer.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
        emptyStateText.setText(
                "No reports yet\n\nYour submitted plastic-waste reports will appear here."
        );
        reportCountText.setText("No reports submitted yet");
    }

    private void showErrorState(String message) {
        loadingProgress.setVisibility(View.GONE);
        reportsRecyclerView.setVisibility(View.GONE);
        stateContainer.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.VISIBLE);
        emptyStateText.setText(message);
        reportCountText.setText("Reports could not be loaded");
    }

    private void redirectToLogin() {
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
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
        if (reportsCall != null) {
            reportsCall.cancel();
            reportsCall = null;
        }
        super.onDestroy();
    }
}
