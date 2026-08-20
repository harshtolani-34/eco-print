package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eco_print.adapter.ProcessingBatchAdapter;
import com.example.eco_print.api.InventoryApi;
import com.example.eco_print.models.ProcessingBatch;
import com.example.eco_print.utils.RoleNavigator;
import com.example.eco_print.utils.SessionManager;
import com.example.eco_print.utils.SupabaseClient;
import com.example.eco_print.utils.SupabaseConfig;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProcessingBatchListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar loadingProgress;
    private View stateContainer;
    private TextView stateText;
    private TextView summaryText;
    private MaterialButton retryButton;

    private SessionManager sessionManager;
    private ProcessingBatchAdapter adapter;

    private Call<List<ProcessingBatch>> batchesCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(
                R.layout.activity_processing_batch_list
        );

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

        adapter = new ProcessingBatchAdapter(this::openBatch);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(null);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        retryButton.setOnClickListener(v -> loadBatches());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBatches();
    }

    private void loadBatches() {
        if (batchesCall != null) {
            batchesCall.cancel();
        }

        showLoading();

        InventoryApi api = SupabaseClient.getClient()
                .create(InventoryApi.class);

        batchesCall = api.getProcessingBatches(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "*",
                "created_at.desc"
        );

        batchesCall.enqueue(new Callback<List<ProcessingBatch>>() {
            @Override
            public void onResponse(
                    Call<List<ProcessingBatch>> call,
                    Response<List<ProcessingBatch>> response
            ) {
                batchesCall = null;
                if (isFinishing() || isDestroyed()) return;

                if (!response.isSuccessful()
                        || response.body() == null) {
                    showError(
                            "Processing batches could not be loaded. Error "
                                    + response.code()
                    );
                    return;
                }

                List<ProcessingBatch> batches = response.body();
                adapter.setBatches(batches);

                summaryText.setText(
                        batches.size() == 1
                                ? "1 processing batch"
                                : batches.size()
                                  + " processing batches"
                );

                if (batches.isEmpty()) {
                    showEmpty();
                } else {
                    showList();
                }
            }

            @Override
            public void onFailure(
                    Call<List<ProcessingBatch>> call,
                    Throwable throwable
            ) {
                batchesCall = null;
                if (!call.isCanceled()
                        && !isFinishing()
                        && !isDestroyed()) {
                    showError("Could not connect to the server.");
                }
            }
        });
    }

    private void openBatch(ProcessingBatch batch) {
        if (batch == null || batch.getId() == null) {
            return;
        }

        Intent intent = new Intent(
                this,
                ProcessingBatchDetailsActivity.class
        );
        intent.putExtra(
                ProcessingBatchDetailsActivity.EXTRA_BATCH_ID,
                batch.getId()
        );
        startActivity(intent);
    }

    private void showLoading() {
        loadingProgress.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        stateContainer.setVisibility(View.GONE);
        summaryText.setText("Loading processing batches...");
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
                "No processing batches yet.\n\n"
                        + "Create one from a Collected waste report."
        );
    }

    private void showError(String message) {
        loadingProgress.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        stateContainer.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.VISIBLE);
        stateText.setText(message);
        summaryText.setText(
                "Processing batches could not be loaded"
        );
    }

    @Override
    protected void onDestroy() {
        if (batchesCall != null) {
            batchesCall.cancel();
            batchesCall = null;
        }
        super.onDestroy();
    }
}
