package com.example.eco_print;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eco_print.api.InventoryApi;
import com.example.eco_print.models.CreateFilamentRequest;
import com.example.eco_print.models.FilamentInventory;
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

public class CreateFilamentActivity extends AppCompatActivity {

    public static final String EXTRA_BATCH_ID =
            "module7_filament_batch_id";
    public static final String EXTRA_PLASTIC_TYPE =
            "module7_filament_plastic_type";
    public static final String EXTRA_USABLE_WEIGHT =
            "module7_filament_usable_weight";

    private EditText materialTypeEditText;
    private EditText colourEditText;
    private EditText diameterEditText;
    private EditText outputWeightEditText;
    private EditText spoolCountEditText;
    private MaterialButton createButton;

    private SessionManager sessionManager;

    private String batchId;
    private double usableWeight;

    private Call<List<FilamentInventory>> createCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_filament);

        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()
                || !sessionManager.isInventoryManager()) {
            RoleNavigator.openCorrectHome(this, sessionManager);
            return;
        }

        batchId = getIntent().getStringExtra(EXTRA_BATCH_ID);
        String plasticType =
                getIntent().getStringExtra(EXTRA_PLASTIC_TYPE);
        usableWeight =
                getIntent().getDoubleExtra(EXTRA_USABLE_WEIGHT, 0);

        if (batchId == null
                || batchId.trim().isEmpty()
                || usableWeight <= 0) {
            Toast.makeText(
                    this,
                    "Completed batch details are missing",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        TextView batchText = findViewById(R.id.batchText);
        TextView usableWeightText =
                findViewById(R.id.usableWeightText);

        materialTypeEditText =
                findViewById(R.id.materialTypeEditText);
        colourEditText =
                findViewById(R.id.colourEditText);
        diameterEditText =
                findViewById(R.id.diameterEditText);
        outputWeightEditText =
                findViewById(R.id.outputWeightEditText);
        spoolCountEditText =
                findViewById(R.id.spoolCountEditText);
        createButton =
                findViewById(R.id.createButton);

        batchText.setText(
                "Batch: " + shortId(batchId)
        );

        usableWeightText.setText(
                String.format(
                        Locale.getDefault(),
                        "Usable plastic recorded: %.2f kg",
                        usableWeight
                )
        );

        materialTypeEditText.setText(
                plasticType == null
                        || plasticType.trim().isEmpty()
                        ? ""
                        : plasticType
        );
        diameterEditText.setText("1.75");

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        createButton.setOnClickListener(v ->
                validateAndCreate()
        );
    }

    private void validateAndCreate() {
        String materialType =
                materialTypeEditText
                        .getText()
                        .toString()
                        .trim();

        String colour =
                colourEditText
                        .getText()
                        .toString()
                        .trim();

        if (materialType.isEmpty()) {
            materialTypeEditText.setError(
                    "Enter the filament material"
            );
            return;
        }

        double diameter;
        double outputWeight;
        int spoolCount;

        try {
            diameter = Double.parseDouble(
                    diameterEditText
                            .getText()
                            .toString()
                            .trim()
            );
        } catch (NumberFormatException exception) {
            diameterEditText.setError(
                    "Enter a valid diameter"
            );
            return;
        }

        try {
            outputWeight = Double.parseDouble(
                    outputWeightEditText
                            .getText()
                            .toString()
                            .trim()
            );
        } catch (NumberFormatException exception) {
            outputWeightEditText.setError(
                    "Enter a valid output weight"
            );
            return;
        }

        try {
            spoolCount = Integer.parseInt(
                    spoolCountEditText
                            .getText()
                            .toString()
                            .trim()
            );
        } catch (NumberFormatException exception) {
            spoolCountEditText.setError(
                    "Enter a valid spool count"
            );
            return;
        }

        if (diameter <= 0) {
            diameterEditText.setError(
                    "Diameter must be greater than zero"
            );
            return;
        }

        if (outputWeight <= 0) {
            outputWeightEditText.setError(
                    "Output must be greater than zero"
            );
            return;
        }

        if (outputWeight > usableWeight) {
            outputWeightEditText.setError(
                    "Output cannot exceed recorded usable plastic"
            );
            return;
        }

        if (spoolCount < 0) {
            spoolCountEditText.setError(
                    "Spool count cannot be negative"
            );
            return;
        }

        createFilament(
                materialType,
                colour,
                diameter,
                outputWeight,
                spoolCount
        );
    }

    private void createFilament(
            String materialType,
            String colour,
            double diameter,
            double outputWeight,
            int spoolCount
    ) {
        if (createCall != null) return;

        setLoading(true);

        InventoryApi api = SupabaseClient.getClient()
                .create(InventoryApi.class);

        createCall = api.createFilamentInventory(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                new CreateFilamentRequest(
                        batchId,
                        materialType,
                        colour,
                        diameter,
                        outputWeight,
                        spoolCount
                )
        );

        createCall.enqueue(new Callback<List<FilamentInventory>>() {
            @Override
            public void onResponse(
                    Call<List<FilamentInventory>> call,
                    Response<List<FilamentInventory>> response
            ) {
                createCall = null;
                if (isFinishing() || isDestroyed()) return;

                setLoading(false);

                if (!response.isSuccessful()
                        || response.body() == null
                        || response.body().isEmpty()) {
                    Toast.makeText(
                            CreateFilamentActivity.this,
                            "Filament was not added. Error "
                                    + response.code(),
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                Toast.makeText(
                        CreateFilamentActivity.this,
                        "Filament added to inventory",
                        Toast.LENGTH_LONG
                ).show();
                finish();
            }

            @Override
            public void onFailure(
                    Call<List<FilamentInventory>> call,
                    Throwable throwable
            ) {
                createCall = null;
                if (!call.isCanceled()
                        && !isFinishing()
                        && !isDestroyed()) {
                    setLoading(false);
                    Toast.makeText(
                            CreateFilamentActivity.this,
                            "Could not add filament inventory",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        });
    }

    private void setLoading(boolean loading) {
        materialTypeEditText.setEnabled(!loading);
        colourEditText.setEnabled(!loading);
        diameterEditText.setEnabled(!loading);
        outputWeightEditText.setEnabled(!loading);
        spoolCountEditText.setEnabled(!loading);
        createButton.setEnabled(!loading);

        createButton.setText(
                loading
                        ? "ADDING FILAMENT..."
                        : "ADD FILAMENT TO INVENTORY"
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
