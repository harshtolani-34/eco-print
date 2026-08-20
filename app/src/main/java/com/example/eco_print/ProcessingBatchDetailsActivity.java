package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eco_print.api.InventoryApi;
import com.example.eco_print.models.ProcessingBatch;
import com.example.eco_print.models.UpdateProcessingStageRequest;
import com.example.eco_print.utils.RoleNavigator;
import com.example.eco_print.utils.SessionManager;
import com.example.eco_print.utils.SupabaseClient;
import com.example.eco_print.utils.SupabaseConfig;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProcessingBatchDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_BATCH_ID =
            "module7_batch_id";

    private static final String[] STAGES = {
            "Received",
            "Sorted",
            "Processing",
            "Completed"
    };

    private TextView stageText;
    private TextView batchIdText;
    private TextView reportIdText;
    private TextView plasticTypeText;
    private TextView inputWeightText;
    private TextView usableWeightText;
    private TextView rejectedWeightText;
    private TextView notesText;
    private TextView completedText;

    private ProgressBar loadingProgress;
    private View contentContainer;
    private MaterialButton stageButton;
    private MaterialButton createFilamentButton;

    private SessionManager sessionManager;
    private String batchId;
    private ProcessingBatch currentBatch;

    private Call<List<ProcessingBatch>> detailsCall;
    private Call<List<ProcessingBatch>> stageCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(
                R.layout.activity_processing_batch_details
        );

        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()
                || !sessionManager.isInventoryManager()) {
            RoleNavigator.openCorrectHome(this, sessionManager);
            return;
        }

        batchId = getIntent().getStringExtra(EXTRA_BATCH_ID);

        if (batchId == null || batchId.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Processing batch ID is missing",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        stageText = findViewById(R.id.stageText);
        batchIdText = findViewById(R.id.batchIdText);
        reportIdText = findViewById(R.id.reportIdText);
        plasticTypeText = findViewById(R.id.plasticTypeText);
        inputWeightText = findViewById(R.id.inputWeightText);
        usableWeightText = findViewById(R.id.usableWeightText);
        rejectedWeightText = findViewById(R.id.rejectedWeightText);
        notesText = findViewById(R.id.notesText);
        completedText = findViewById(R.id.completedText);
        loadingProgress = findViewById(R.id.loadingProgress);
        contentContainer = findViewById(R.id.contentContainer);
        stageButton = findViewById(R.id.stageButton);
        createFilamentButton =
                findViewById(R.id.createFilamentButton);

        ImageButton backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        stageButton.setOnClickListener(v ->
                confirmNextStage()
        );

        createFilamentButton.setOnClickListener(v ->
                openCreateFilament()
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (stageCall == null) {
            loadBatch();
        }
    }

    private void loadBatch() {
        if (detailsCall != null) {
            detailsCall.cancel();
        }

        setLoading(true);

        InventoryApi api = SupabaseClient.getClient()
                .create(InventoryApi.class);

        detailsCall = api.getProcessingBatchById(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "eq." + batchId,
                "*"
        );

        detailsCall.enqueue(new Callback<List<ProcessingBatch>>() {
            @Override
            public void onResponse(
                    Call<List<ProcessingBatch>> call,
                    Response<List<ProcessingBatch>> response
            ) {
                detailsCall = null;
                if (isFinishing() || isDestroyed()) return;

                setLoading(false);

                if (!response.isSuccessful()
                        || response.body() == null
                        || response.body().isEmpty()) {
                    Toast.makeText(
                            ProcessingBatchDetailsActivity.this,
                            "Processing batch could not be loaded",
                            Toast.LENGTH_LONG
                    ).show();
                    finish();
                    return;
                }

                bindBatch(response.body().get(0));
            }

            @Override
            public void onFailure(
                    Call<List<ProcessingBatch>> call,
                    Throwable throwable
            ) {
                detailsCall = null;
                if (!call.isCanceled()
                        && !isFinishing()
                        && !isDestroyed()) {
                    setLoading(false);
                    Toast.makeText(
                            ProcessingBatchDetailsActivity.this,
                            "Could not connect to the server",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        });
    }

    private void bindBatch(ProcessingBatch batch) {
        currentBatch = batch;

        stageText.setText(
                safe(batch.getStage(), "Received")
        );
        batchIdText.setText(
                safe(batch.getId(), batchId)
        );
        reportIdText.setText(
                safe(batch.getReportId(), "Unavailable")
        );
        plasticTypeText.setText(
                safe(
                        batch.getPlasticType(),
                        "Plastic type not provided"
                )
        );
        inputWeightText.setText(
                weight(batch.getInputWeightKg())
        );
        usableWeightText.setText(
                weight(batch.getUsableWeightKg())
        );
        rejectedWeightText.setText(
                weight(batch.getRejectedWeightKg())
        );
        notesText.setText(
                safe(batch.getNotes(), "No processing notes")
        );
        completedText.setText(
                batch.getCompletedAt() == null
                        || batch.getCompletedAt().trim().isEmpty()
                        ? "Not completed yet"
                        : batch.getCompletedAt()
        );

        updateActions(batch.getStage());
    }

    private void updateActions(String stage) {
        String next = nextStage(stage);

        if (next == null) {
            stageButton.setVisibility(View.GONE);
            createFilamentButton.setVisibility(View.VISIBLE);
            return;
        }

        createFilamentButton.setVisibility(View.GONE);
        stageButton.setVisibility(View.VISIBLE);
        stageButton.setText(
                "MOVE TO " + next.toUpperCase(Locale.US)
        );
    }

    private void confirmNextStage() {
        if (currentBatch == null || stageCall != null) {
            return;
        }

        String next = nextStage(currentBatch.getStage());

        if (next == null) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Update processing stage?")
                .setMessage(
                        "Move this batch from "
                                + currentBatch.getStage()
                                + " to "
                                + next
                                + "?"
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton(
                        "Confirm",
                        (dialog, which) -> updateStage(next)
                )
                .show();
    }

    private void updateStage(String nextStage) {
        stageButton.setEnabled(false);
        stageButton.setText("UPDATING...");

        InventoryApi api = SupabaseClient.getClient()
                .create(InventoryApi.class);

        stageCall = api.updateProcessingStage(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                new UpdateProcessingStageRequest(
                        batchId,
                        nextStage
                )
        );

        stageCall.enqueue(new Callback<List<ProcessingBatch>>() {
            @Override
            public void onResponse(
                    Call<List<ProcessingBatch>> call,
                    Response<List<ProcessingBatch>> response
            ) {
                stageCall = null;
                if (isFinishing() || isDestroyed()) return;

                stageButton.setEnabled(true);

                if (!response.isSuccessful()
                        || response.body() == null
                        || response.body().isEmpty()) {
                    updateActions(currentBatch.getStage());
                    Toast.makeText(
                            ProcessingBatchDetailsActivity.this,
                            "Stage was not updated. Error "
                                    + response.code(),
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                bindBatch(response.body().get(0));

                Toast.makeText(
                        ProcessingBatchDetailsActivity.this,
                        "Processing moved to " + nextStage,
                        Toast.LENGTH_LONG
                ).show();
            }

            @Override
            public void onFailure(
                    Call<List<ProcessingBatch>> call,
                    Throwable throwable
            ) {
                stageCall = null;
                if (!call.isCanceled()
                        && !isFinishing()
                        && !isDestroyed()) {
                    stageButton.setEnabled(true);
                    updateActions(currentBatch.getStage());
                    Toast.makeText(
                            ProcessingBatchDetailsActivity.this,
                            "Could not update processing stage",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        });
    }

    private void openCreateFilament() {
        if (currentBatch == null
                || !"Completed".equalsIgnoreCase(
                currentBatch.getStage()
        )) {
            return;
        }

        Intent intent = new Intent(
                this,
                CreateFilamentActivity.class
        );

        intent.putExtra(
                CreateFilamentActivity.EXTRA_BATCH_ID,
                currentBatch.getId()
        );
        intent.putExtra(
                CreateFilamentActivity.EXTRA_PLASTIC_TYPE,
                currentBatch.getPlasticType()
        );
        intent.putExtra(
                CreateFilamentActivity.EXTRA_USABLE_WEIGHT,
                currentBatch.getUsableWeightKg()
        );

        startActivity(intent);
    }

    private String nextStage(String currentStage) {
        if (currentStage == null) {
            return null;
        }

        for (int i = 0; i < STAGES.length - 1; i++) {
            if (STAGES[i].equalsIgnoreCase(currentStage)) {
                return STAGES[i + 1];
            }
        }

        return null;
    }

    private String weight(double value) {
        return String.format(
                Locale.getDefault(),
                "%.2f kg",
                value
        );
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty()
                ? fallback
                : value.trim();
    }

    private void setLoading(boolean loading) {
        loadingProgress.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );
        contentContainer.setVisibility(
                loading ? View.GONE : View.VISIBLE
        );
    }

    @Override
    protected void onDestroy() {
        if (detailsCall != null) {
            detailsCall.cancel();
            detailsCall = null;
        }
        if (stageCall != null) {
            stageCall.cancel();
            stageCall = null;
        }
        super.onDestroy();
    }
}
