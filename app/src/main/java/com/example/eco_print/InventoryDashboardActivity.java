package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eco_print.api.InventoryApi;
import com.example.eco_print.models.FilamentInventory;
import com.example.eco_print.models.ProcessingBatch;
import com.example.eco_print.models.WasteReport;
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

public class InventoryDashboardActivity extends AppCompatActivity {

    private TextView greetingText;
    private TextView collectedCountText;
    private TextView batchCountText;
    private TextView stockWeightText;
    private TextView statusText;

    private SessionManager sessionManager;

    private Call<List<WasteReport>> collectedCall;
    private Call<List<ProcessingBatch>> batchesCall;
    private Call<List<FilamentInventory>> inventoryCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_dashboard);

        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()
                || !sessionManager.isInventoryManager()) {
            RoleNavigator.openCorrectHome(this, sessionManager);
            return;
        }

        greetingText = findViewById(R.id.greetingText);
        collectedCountText = findViewById(R.id.collectedCountText);
        batchCountText = findViewById(R.id.batchCountText);
        stockWeightText = findViewById(R.id.stockWeightText);
        statusText = findViewById(R.id.statusText);

        MaterialButton collectedButton = findViewById(R.id.collectedButton);
        MaterialButton batchesButton = findViewById(R.id.batchesButton);
        MaterialButton inventoryButton = findViewById(R.id.inventoryButton);
        MaterialButton historyButton = findViewById(R.id.historyButton);
        MaterialButton logoutButton = findViewById(R.id.logoutButton);

        String name = sessionManager.getUserName();
        greetingText.setText(
                name == null || name.trim().isEmpty()
                        ? "Inventory Manager"
                        : "Welcome, " + name.trim()
        );

        collectedButton.setOnClickListener(v ->
                startActivity(new Intent(
                        this,
                        CollectedPlasticActivity.class
                ))
        );

        batchesButton.setOnClickListener(v ->
                startActivity(new Intent(
                        this,
                        ProcessingBatchListActivity.class
                ))
        );

        inventoryButton.setOnClickListener(v ->
                startActivity(new Intent(
                        this,
                        FilamentInventoryActivity.class
                ))
        );

        historyButton.setOnClickListener(v ->
                startActivity(new Intent(
                        this,
                        InventoryTransactionHistoryActivity.class
                ))
        );

        logoutButton.setOnClickListener(v -> confirmLogout());
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sessionManager != null
                && sessionManager.isInventoryManager()) {
            loadDashboard();
        }
    }

    private void loadDashboard() {
        if (!SupabaseConfig.isConfigured()) {
            statusText.setText("Supabase is not configured.");
            return;
        }

        cancelCalls();
        statusText.setText("Refreshing inventory summary...");

        InventoryApi api = SupabaseClient.getClient()
                .create(InventoryApi.class);

        collectedCall = api.getCollectedReports(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "eq.Collected",
                "id",
                "collected_at.desc"
        );

        collectedCall.enqueue(new Callback<List<WasteReport>>() {
            @Override
            public void onResponse(
                    Call<List<WasteReport>> call,
                    Response<List<WasteReport>> response
            ) {
                collectedCall = null;
                if (!safeToUpdate()) return;

                if (response.code() == 401) {
                    logout();
                    return;
                }

                collectedCountText.setText(
                        response.isSuccessful()
                                && response.body() != null
                                ? String.valueOf(response.body().size())
                                : "—"
                );
            }

            @Override
            public void onFailure(
                    Call<List<WasteReport>> call,
                    Throwable throwable
            ) {
                collectedCall = null;
                if (!call.isCanceled() && safeToUpdate()) {
                    collectedCountText.setText("—");
                }
            }
        });

        batchesCall = api.getProcessingBatches(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "id",
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

                batchCountText.setText(
                        response.isSuccessful()
                                && response.body() != null
                                ? String.valueOf(response.body().size())
                                : "—"
                );
            }

            @Override
            public void onFailure(
                    Call<List<ProcessingBatch>> call,
                    Throwable throwable
            ) {
                batchesCall = null;
                if (!call.isCanceled() && safeToUpdate()) {
                    batchCountText.setText("—");
                }
            }
        });

        inventoryCall = api.getFilamentInventory(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "*",
                "created_at.desc"
        );

        inventoryCall.enqueue(new Callback<List<FilamentInventory>>() {
            @Override
            public void onResponse(
                    Call<List<FilamentInventory>> call,
                    Response<List<FilamentInventory>> response
            ) {
                inventoryCall = null;
                if (!safeToUpdate()) return;

                if (!response.isSuccessful()
                        || response.body() == null) {
                    stockWeightText.setText("—");
                    statusText.setText(
                            "Some inventory information could not be loaded."
                    );
                    return;
                }

                double totalStock = 0;
                for (FilamentInventory item : response.body()) {
                    totalStock += item.getAvailableStockKg();
                }

                stockWeightText.setText(
                        String.format(
                                Locale.getDefault(),
                                "%.2f KG",
                                totalStock
                        )
                );
                statusText.setText("Inventory data is up to date.");
            }

            @Override
            public void onFailure(
                    Call<List<FilamentInventory>> call,
                    Throwable throwable
            ) {
                inventoryCall = null;
                if (!call.isCanceled() && safeToUpdate()) {
                    stockWeightText.setText("—");
                    statusText.setText(
                            "Could not refresh inventory summary."
                    );
                }
            }
        });
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout?")
                .setMessage("You will leave the inventory manager dashboard.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton(
                        "Logout",
                        (dialog, which) -> logout()
                )
                .show();
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

    private boolean safeToUpdate() {
        return !isFinishing() && !isDestroyed();
    }

    private void cancelCalls() {
        if (collectedCall != null) {
            collectedCall.cancel();
            collectedCall = null;
        }
        if (batchesCall != null) {
            batchesCall.cancel();
            batchesCall = null;
        }
        if (inventoryCall != null) {
            inventoryCall.cancel();
            inventoryCall = null;
        }
    }

    @Override
    protected void onDestroy() {
        cancelCalls();
        super.onDestroy();
    }
}
