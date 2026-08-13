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

import com.example.eco_print.adapter.AdminReportAdapter;
import com.example.eco_print.api.AdminApi;
import com.example.eco_print.models.WasteReport;
import com.example.eco_print.utils.SessionManager;
import com.example.eco_print.utils.SupabaseClient;
import com.example.eco_print.utils.SupabaseConfig;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminReportListActivity extends AppCompatActivity {

    private RecyclerView reportsRecyclerView;
    private ProgressBar loadingProgress;
    private View stateContainer;
    private TextView stateText;
    private TextView countText;
    private MaterialButton retryButton;
    private AdminReportAdapter adapter;
    private SessionManager sessionManager;
    private Call<List<WasteReport>> reportsCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_report_list);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn() || !sessionManager.isAdmin()) {
            redirectToAdminLogin();
            return;
        }

        ImageButton backButton = findViewById(R.id.backButton);
        reportsRecyclerView = findViewById(R.id.reportsRecyclerView);
        loadingProgress = findViewById(R.id.loadingProgress);
        stateContainer = findViewById(R.id.stateContainer);
        stateText = findViewById(R.id.stateText);
        countText = findViewById(R.id.countText);
        retryButton = findViewById(R.id.retryButton);

        adapter = new AdminReportAdapter(this::openReport);
        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reportsRecyclerView.setAdapter(adapter);
        reportsRecyclerView.setHasFixedSize(true);
        reportsRecyclerView.setItemAnimator(null);

        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        retryButton.setOnClickListener(v -> loadPendingReports());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager != null && sessionManager.isAdmin()) {
            loadPendingReports();
        }
    }

    private void loadPendingReports() {
        if (!SupabaseConfig.isConfigured()) {
            showError("Supabase is not configured.");
            return;
        }
        if (reportsCall != null) reportsCall.cancel();
        showLoading();

        AdminApi api = SupabaseClient.getClient().create(AdminApi.class);
        reportsCall = api.getPendingReports(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "eq.Pending",
                "*",
                "created_at.asc"
        );

        reportsCall.enqueue(new Callback<List<WasteReport>>() {
            @Override
            public void onResponse(
                    Call<List<WasteReport>> call,
                    Response<List<WasteReport>> response
            ) {
                reportsCall = null;
                if (isFinishing() || isDestroyed()) return;
                if (response.code() == 401) {
                    showToast("Administrator session expired");
                    sessionManager.logout();
                    redirectToAdminLogin();
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    showError("Pending reports could not be loaded. Error " + response.code());
                    return;
                }

                List<WasteReport> reports = response.body();
                adapter.setReports(reports);
                int count = reports.size();
                countText.setText(count + (count == 1 ? " report waiting" : " reports waiting"));
                if (count == 0) {
                    showEmpty();
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
                if (!call.isCanceled() && !isFinishing() && !isDestroyed()) {
                    showError("Could not connect to the server. Check your internet and retry.");
                }
            }
        });
    }

    private void openReport(WasteReport report) {
        if (report.getId() == null || report.getId().trim().isEmpty()) return;
        Intent intent = new Intent(this, AdminReportDetailsActivity.class);
        intent.putExtra(AdminReportDetailsActivity.EXTRA_REPORT_ID, report.getId());
        startActivity(intent);
    }

    private void showLoading() {
        loadingProgress.setVisibility(View.VISIBLE);
        reportsRecyclerView.setVisibility(View.GONE);
        stateContainer.setVisibility(View.GONE);
        countText.setText("Loading pending reports...");
    }

    private void showReports() {
        loadingProgress.setVisibility(View.GONE);
        stateContainer.setVisibility(View.GONE);
        reportsRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showEmpty() {
        loadingProgress.setVisibility(View.GONE);
        reportsRecyclerView.setVisibility(View.GONE);
        stateContainer.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
        stateText.setText("No pending reports\n\nNew citizen reports will appear here for verification.");
    }

    private void showError(String message) {
        loadingProgress.setVisibility(View.GONE);
        reportsRecyclerView.setVisibility(View.GONE);
        stateContainer.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.VISIBLE);
        stateText.setText(message);
        countText.setText("Reports could not be loaded");
    }

    private void redirectToAdminLogin() {
        Intent intent = new Intent(this, AdminLoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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
