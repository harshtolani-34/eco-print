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

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportHistoryActivity extends AppCompatActivity {

    private RecyclerView reportsRecyclerView;
    private ProgressBar loadingProgress;
    private TextView emptyStateText;
    private TextView reportCountText;

    private ReportHistoryAdapter adapter;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_history);

        ImageButton backButton = findViewById(R.id.backButton);
        reportsRecyclerView = findViewById(R.id.reportsRecyclerView);
        loadingProgress = findViewById(R.id.loadingProgress);
        emptyStateText = findViewById(R.id.emptyStateText);
        reportCountText = findViewById(R.id.reportCountText);

        sessionManager = new SessionManager(this);
        adapter = new ReportHistoryAdapter();

        reportsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );
        reportsRecyclerView.setAdapter(adapter);
        reportsRecyclerView.setHasFixedSize(true);

        backButton.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );
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

        showLoading(true);

        WasteReportApi api = SupabaseClient.getClient()
                .create(WasteReportApi.class);

        api.getMyWasteReports(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
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
                    showToast("Your session expired. Please log in again.");
                    redirectToLogin();
                    return;
                }

                if (!response.isSuccessful() || response.body() == null) {
                    showToast("Unable to load reports. Error code: "
                            + response.code());
                    showEmptyState(true);
                    return;
                }

                List<WasteReport> reports = response.body();
                adapter.setReports(reports);

                int count = reports.size();
                reportCountText.setText(count == 1
                        ? "1 submitted report"
                        : count + " submitted reports");
                showEmptyState(count == 0);
            }

            @Override
            public void onFailure(
                    Call<List<WasteReport>> call,
                    Throwable throwable
            ) {
                showLoading(false);
                showEmptyState(true);
                showToast("Unable to load reports: "
                        + throwable.getMessage());
            }
        });
    }

    private void showLoading(boolean loading) {
        loadingProgress.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );
        reportsRecyclerView.setVisibility(
                loading ? View.GONE : View.VISIBLE
        );
        if (loading) {
            emptyStateText.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(boolean empty) {
        emptyStateText.setVisibility(empty ? View.VISIBLE : View.GONE);
        reportsRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);

        if (empty) {
            reportCountText.setText("No reports submitted yet");
        }
    }

    private void redirectToLogin() {
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
