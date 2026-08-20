package com.example.eco_print;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eco_print.api.InventoryApi;
import com.example.eco_print.models.CreateProcessingBatchRequest;
import com.example.eco_print.models.ProcessingBatch;
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

public class CreateProcessingBatchActivity extends AppCompatActivity {

    public static final String EXTRA_REPORT_ID =
            "module7_report_id";
    public static final String EXTRA_PLASTIC_TYPE =
            "module7_plastic_type";
    public static final String EXTRA_INPUT_WEIGHT =
            "module7_input_weight";

    private EditText usableWeightEditText;
    private EditText rejectedWeightEditText;
    private EditText notesEditText;
    private MaterialButton createButton;

    private SessionManager sessionManager;

    private String reportId;
    private double inputWeight;

    private Call<List<ProcessingBatch>> createCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(
                R.layout.activity_create_processing_batch
        );

        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()
                || !sessionManager.isInventoryManager()) {
            RoleNavigator.openCorrectHome(this, sessionManager);
            return;
        }

        reportId = getIntent().getStringExtra(EXTRA_REPORT_ID);
        String plasticType =
                getIntent().getStringExtra(EXTRA_PLASTIC_TYPE);
        inputWeight =
                getIntent().getDoubleExtra(EXTRA_INPUT_WEIGHT, 0);

        if (reportId == null
                || reportId.trim().isEmpty()
                || inputWeight <= 0) {
            Toast.makeText(
                    this,
                    "Collected report details are missing",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        TextView reportText = findViewById(R.id.reportText);
        TextView plasticTypeText =
                findViewById(R.id.plasticTypeText);
        TextView inputWeightText =
                findViewById(R.id.inputWeightText);

        usableWeightEditText =
                findViewById(R.id.usableWeightEditText);
        rejectedWeightEditText =
                findViewById(R.id.rejectedWeightEditText);
        notesEditText =
                findViewById(R.id.notesEditText);
        createButton =
                findViewById(R.id.createButton);

        reportText.setText(
                "Report: " + shortId(reportId)
        );

        plasticTypeText.setText(
                plasticType == null || plasticType.trim().isEmpty()
                        ? "Plastic type not provided"
                        : plasticType
        );

        inputWeightText.setText(
                String.format(
                        Locale.getDefault(),
                        "%.2f kg collected input",
                        inputWeight
                )
        );

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        createButton.setOnClickListener(v ->
                validateAndCreate()
        );
    }

    private void validateAndCreate() {
        double usable;
        double rejected;

        try {
            usable = Double.parseDouble(
                    usableWeightEditText
                            .getText()
                            .toString()
                            .trim()
            );
        } catch (NumberFormatException exception) {
            usableWeightEditText.setError(
                    "Enter a valid usable weight"
            );
            return;
        }

        try {
            rejected = Double.parseDouble(
                    rejectedWeightEditText
                            .getText()
                            .toString()
                            .trim()
            );
        } catch (NumberFormatException exception) {
            rejectedWeightEditText.setError(
                    "Enter a valid rejected weight"
            );
            return;
        }

        if (usable < 0) {
            usableWeightEditText.setError(
                    "Usable weight cannot be negative"
            );
            return;
        }

        if (rejected < 0) {
            rejectedWeightEditText.setError(
                    "Rejected weight cannot be negative"
            );
            return;
        }

        if (usable + rejected > inputWeight) {
            rejectedWeightEditText.setError(
                    "Usable + rejected cannot exceed "
                            + String.format(
                            Locale.getDefault(),
                            "%.2f kg",
                            inputWeight
                    )
            );
            return;
        }

        if (usable == 0 && rejected == 0) {
            usableWeightEditText.setError(
                    "Record at least one quantity"
            );
            return;
        }

        createBatch(
                usable,
                rejected,
                notesEditText.getText().toString().trim()
        );
    }

    private void createBatch(
            double usable,
            double rejected,
            String notes
    ) {
        if (createCall != null) return;

        setLoading(true);

        InventoryApi api = SupabaseClient.getClient()
                .create(InventoryApi.class);

        createCall = api.createProcessingBatch(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                new CreateProcessingBatchRequest(
                        reportId,
                        usable,
                        rejected,
                        notes
                )
        );

        createCall.enqueue(new Callback<List<ProcessingBatch>>() {
            @Override
            public void onResponse(
                    Call<List<ProcessingBatch>> call,
                    Response<List<ProcessingBatch>> response
            ) {
                createCall = null;
                if (isFinishing() || isDestroyed()) return;

                setLoading(false);

                if (!response.isSuccessful()
                        || response.body() == null
                        || response.body().isEmpty()) {
                    Toast.makeText(
                            CreateProcessingBatchActivity.this,
                            "Batch was not created. Error "
                                    + response.code(),
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                Toast.makeText(
                        CreateProcessingBatchActivity.this,
                        "Processing batch created",
                        Toast.LENGTH_LONG
                ).show();
                finish();
            }

            @Override
            public void onFailure(
                    Call<List<ProcessingBatch>> call,
                    Throwable throwable
            ) {
                createCall = null;
                if (!call.isCanceled()
                        && !isFinishing()
                        && !isDestroyed()) {
                    setLoading(false);
                    Toast.makeText(
                            CreateProcessingBatchActivity.this,
                            "Could not create processing batch",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        });
    }

    private void setLoading(boolean loading) {
        createButton.setEnabled(!loading);
        usableWeightEditText.setEnabled(!loading);
        rejectedWeightEditText.setEnabled(!loading);
        notesEditText.setEnabled(!loading);

        createButton.setText(
                loading
                        ? "CREATING BATCH..."
                        : "CREATE PROCESSING BATCH"
        );
    }

    private String shortId(String id) {
        return id.length() <= 12
                ? id
                : id.substring(0, 12) + "...";
    }

    @Override
    protected void onDestroy() {
        if (createCall != null) {
            createCall.cancel();
            createCall = null;
        }
        super.onDestroy();
    }
}
