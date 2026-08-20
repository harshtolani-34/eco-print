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

import com.example.eco_print.adapter.CollectedPlasticAdapter;
import com.example.eco_print.api.InventoryApi;
import com.example.eco_print.models.ProcessingBatch;
import com.example.eco_print.models.WasteReport;
import com.example.eco_print.utils.RoleNavigator;
import com.example.eco_print.utils.SessionManager;
import com.example.eco_print.utils.SupabaseClient;
import com.example.eco_print.utils.SupabaseConfig;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CollectedPlasticActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar loadingProgress;
    private View stateContainer;
    private TextView stateText;
    private TextView summaryText;
    private MaterialButton retryButton;

    private SessionManager sessionManager;
    private CollectedPlasticAdapter adapter;

    private Call<List<ProcessingBatch>> batchesCall;
    private Call<List<WasteReport>> reportsCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collected_plastic);

        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()
                || !sessionManager.isInventoryManager()) {
            RoleNavigator.openCorrectHome(this, sessionManager);
            return;
        }

        recyclerView = findViewById(R.id.recyclerView);
        loadingProgress = findViewById(R.id.loadingProgress);
        stateContainer = findViewById(R.id.stateContainer);
        stateText = findViewById(R.id.stateText);
        summaryText = findViewById(R.id.summaryText);
        retryButton = findViewById(R.id.retryButton);

        adapter = new CollectedPlasticAdapter(this::openCreateBatch);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(null);

        ImageButton backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        retryButton.setOnClickListener(v ->
                loadCollectedPlastic()
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCollectedPlastic();
    }

    private void loadCollectedPlastic() {
        if (!SupabaseConfig.isConfigured()) {
            showError("Supabase is not configured.");
            return;
        }

        cancelCalls();
        showLoading();

        InventoryApi api = SupabaseClient.getClient()
                .create(InventoryApi.class);

        batchesCall = api.getProcessingBatches(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "report_id",
                "created_at.desc"
        );

        batchesCall.enqueue(new Callback<List<ProcessingBatch>>() {
            @Override
            public void onResponse(
                    Call<List<ProcessingBatch>> call,
                    Response<List<ProcessingBatch>> response
            ) {
                batchesCall = null;
                if (!safeToUpdate()) return;

                if (response.code() == 401) {
                    logoutToLogin();
                    return;
                }

                if (!response.isSuccessful()
                        || response.body() == null) {
                    showError(
                            "Processing batches could not be checked. Error "
                                    + response.code()
                    );
                    return;
                }

                Set<String> processedReportIds = new HashSet<>();

                for (ProcessingBatch batch : response.body()) {
                    if (batch.getReportId() != null) {
                        processedReportIds.add(batch.getReportId());
                    }
                }

                loadReports(api, processedReportIds);
            }

            @Override
            public void onFailure(
                    Call<List<ProcessingBatch>> call,
                    Throwable throwable
            ) {
                batchesCall = null;
                if (!call.isCanceled() && safeToUpdate()) {
                    showError("Could not connect to the server.");
                }
            }
        });
    }

    private void loadReports(
            InventoryApi api,
            Set<String> processedReportIds
    ) {
        reportsCall = api.getCollectedReports(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "eq.Collected",
                "*",
                "collected_at.desc"
        );

        reportsCall.enqueue(new Callback<List<WasteReport>>() {
            @Override
            public void onResponse(
                    Call<List<WasteReport>> call,
                    Response<List<WasteReport>> response
            ) {
                reportsCall = null;
                if (!safeToUpdate()) return;

                if (response.code() == 401) {
                    logoutToLogin();
                    return;
                }

                if (!response.isSuccessful()
                        || response.body() == null) {
                    showError(
                            "Collected plastic could not be loaded. Error "
                                    + response.code()
                    );
                    return;
                }

                List<WasteReport> available = new ArrayList<>();

                for (WasteReport report : response.body()) {
                    if (report.getId() != null
                            && !processedReportIds.contains(
                            report.getId()
                    )) {
                        available.add(report);
                    }
                }

                adapter.setReports(available);

                summaryText.setText(
                        available.size() == 1
                                ? "1 collected report ready for processing"
                                : available.size()
                                  + " collected reports ready for processing"
                );

                if (available.isEmpty()) {
                    showEmpty();
                } else {
                    showList();
                }
            }

            @Override
            public void onFailure(
                    Call<List<WasteReport>> call,
                    Throwable throwable
            ) {
                reportsCall = null;
                if (!call.isCanceled() && safeToUpdate()) {
                    showError("Could not load collected plastic.");
                }
            }
        });
    }

    private void openCreateBatch(WasteReport report) {
        if (report == null || report.getId() == null) {
            return;
        }

        Intent intent = new Intent(
                this,
                CreateProcessingBatchActivity.class
        );

        intent.putExtra(
                CreateProcessingBatchActivity.EXTRA_REPORT_ID,
                report.getId()
        );
        intent.putExtra(
                CreateProcessingBatchActivity.EXTRA_PLASTIC_TYPE,
                report.getWasteType()
        );
        intent.putExtra(
                CreateProcessingBatchActivity.EXTRA_INPUT_WEIGHT,
                report.getEstimatedWeightKg()
        );

        startActivity(intent);
    }

    private void showLoading() {
        loadingProgress.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        stateContainer.setVisibility(View.GONE);
        summaryText.setText("Loading collected plastic...");
    }

    private void showList() {
        loadingProgress.setVisibility(View.GONE);
        stateContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void showEmpty() {
        loadingProgress.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        stateContainer.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
        stateText.setText(
                "No collected plastic is waiting for processing.\n\n"
                        + "A report appears here after the collector marks it as Collected."
        );
    }

    private void showError(String message) {
        loadingProgress.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        stateContainer.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.VISIBLE);
        stateText.setText(message);
        summaryText.setText(
                "Collected plastic could not be loaded"
        );
    }

    private boolean safeToUpdate() {
        return !isFinishing() && !isDestroyed();
    }

    private void logoutToLogin() {
        Toast.makeText(
                this,
                "Your session expired",
                Toast.LENGTH_LONG
        ).show();

        sessionManager.logout();

        Intent intent = new Intent(
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

    private void cancelCalls() {
        if (batchesCall != null) {
            batchesCall.cancel();
            batchesCall = null;
        }
        if (reportsCall != null) {
            reportsCall.cancel();
            reportsCall = null;
        }
    }

    @Override
    protected void onDestroy() {
        cancelCalls();
        super.onDestroy();
    }
}
