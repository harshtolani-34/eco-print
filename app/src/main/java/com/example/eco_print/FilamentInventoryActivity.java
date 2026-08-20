package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eco_print.adapter.FilamentInventoryAdapter;
import com.example.eco_print.api.InventoryApi;
import com.example.eco_print.models.FilamentInventory;
import com.example.eco_print.models.RecordInventoryTransactionRequest;
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

public class FilamentInventoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar loadingProgress;
    private View stateContainer;
    private TextView stateText;
    private TextView summaryText;
    private MaterialButton retryButton;

    private SessionManager sessionManager;
    private FilamentInventoryAdapter adapter;

    private Call<List<FilamentInventory>> inventoryCall;
    private Call<List<FilamentInventory>> transactionCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(
                R.layout.activity_filament_inventory
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

        adapter = new FilamentInventoryAdapter(
                this::showStockActionMenu
        );

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(null);

        ImageButton backButton = findViewById(R.id.backButton);
        MaterialButton historyButton =
                findViewById(R.id.historyButton);

        backButton.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        historyButton.setOnClickListener(v ->
                startActivity(new Intent(
                        this,
                        InventoryTransactionHistoryActivity.class
                ))
        );

        retryButton.setOnClickListener(v -> loadInventory());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInventory();
    }

    private void loadInventory() {
        if (inventoryCall != null) {
            inventoryCall.cancel();
        }

        showLoading();

        InventoryApi api = SupabaseClient.getClient()
                .create(InventoryApi.class);

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
                if (isFinishing() || isDestroyed()) return;

                if (!response.isSuccessful()
                        || response.body() == null) {
                    showError(
                            "Filament inventory could not be loaded. Error "
                                    + response.code()
                    );
                    return;
                }

                List<FilamentInventory> items = response.body();
                adapter.setItems(items);

                double total = 0;
                for (FilamentInventory item : items) {
                    total += item.getAvailableStockKg();
                }

                summaryText.setText(
                        String.format(
                                Locale.getDefault(),
                                "%d item%s • %.2f kg available",
                                items.size(),
                                items.size() == 1 ? "" : "s",
                                total
                        )
                );

                if (items.isEmpty()) {
                    showEmpty();
                } else {
                    showList();
                }
            }

            @Override
            public void onFailure(
                    Call<List<FilamentInventory>> call,
                    Throwable throwable
            ) {
                inventoryCall = null;
                if (!call.isCanceled()
                        && !isFinishing()
                        && !isDestroyed()) {
                    showError("Could not connect to the server.");
                }
            }
        });
    }

    private void showStockActionMenu(FilamentInventory item) {
        if (item == null
                || item.getId() == null
                || item.getAvailableStockKg() <= 0) {
            return;
        }

        String[] actions = {
                "Use / Remove Stock",
                "Record Damaged Filament"
        };

        new AlertDialog.Builder(this)
                .setTitle(
                        safe(item.getFilamentType(), "Filament")
                )
                .setItems(actions, (dialog, which) -> {
                    String type = which == 0
                            ? "OUT"
                            : "DAMAGE";
                    showTransactionDialog(item, type);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showTransactionDialog(
            FilamentInventory item,
            String type
    ) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        int padding = Math.round(
                20 * getResources()
                        .getDisplayMetrics()
                        .density
        );
        container.setPadding(
                padding,
                padding / 2,
                padding,
                0
        );

        EditText quantityInput = new EditText(this);
        quantityInput.setHint("Quantity in kg");
        quantityInput.setInputType(
                InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        EditText noteInput = new EditText(this);
        noteInput.setHint(
                type.equals("DAMAGE")
                        ? "Damage reason"
                        : "Usage / reference note"
        );

        container.addView(quantityInput);
        container.addView(noteInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(
                        type.equals("DAMAGE")
                                ? "Record damaged filament"
                                : "Remove filament stock"
                )
                .setMessage(
                        String.format(
                                Locale.getDefault(),
                                "%.2f kg currently available",
                                item.getAvailableStockKg()
                        )
                )
                .setView(container)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(ignored ->
                dialog.getButton(
                                AlertDialog.BUTTON_POSITIVE
                        )
                        .setOnClickListener(v -> {
                            double quantity;

                            try {
                                quantity = Double.parseDouble(
                                        quantityInput
                                                .getText()
                                                .toString()
                                                .trim()
                                );
                            } catch (NumberFormatException exception) {
                                quantityInput.setError(
                                        "Enter a valid quantity"
                                );
                                return;
                            }

                            if (quantity <= 0) {
                                quantityInput.setError(
                                        "Quantity must be greater than zero"
                                );
                                return;
                            }

                            if (quantity > item.getAvailableStockKg()) {
                                quantityInput.setError(
                                        "Cannot exceed available stock"
                                );
                                return;
                            }

                            String note = noteInput
                                    .getText()
                                    .toString()
                                    .trim();

                            dialog.dismiss();

                            recordTransaction(
                                    item,
                                    type,
                                    quantity,
                                    note
                            );
                        })
        );

        dialog.show();
    }

    private void recordTransaction(
            FilamentInventory item,
            String type,
            double quantity,
            String note
    ) {
        if (transactionCall != null) return;

        InventoryApi api = SupabaseClient.getClient()
                .create(InventoryApi.class);

        transactionCall = api.recordInventoryTransaction(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                new RecordInventoryTransactionRequest(
                        item.getId(),
                        type,
                        quantity,
                        note
                )
        );

        transactionCall.enqueue(new Callback<List<FilamentInventory>>() {
            @Override
            public void onResponse(
                    Call<List<FilamentInventory>> call,
                    Response<List<FilamentInventory>> response
            ) {
                transactionCall = null;
                if (isFinishing() || isDestroyed()) return;

                if (!response.isSuccessful()
                        || response.body() == null
                        || response.body().isEmpty()) {
                    Toast.makeText(
                            FilamentInventoryActivity.this,
                            "Stock was not updated. Error "
                                    + response.code(),
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                Toast.makeText(
                        FilamentInventoryActivity.this,
                        type.equals("DAMAGE")
                                ? "Damage recorded"
                                : "Stock updated",
                        Toast.LENGTH_LONG
                ).show();

                loadInventory();
            }

            @Override
            public void onFailure(
                    Call<List<FilamentInventory>> call,
                    Throwable throwable
            ) {
                transactionCall = null;
                if (!call.isCanceled()
                        && !isFinishing()
                        && !isDestroyed()) {
                    Toast.makeText(
                            FilamentInventoryActivity.this,
                            "Could not update stock",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        });
    }

    private void showLoading() {
        loadingProgress.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        stateContainer.setVisibility(View.GONE);
        summaryText.setText("Loading filament inventory...");
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
                "No filament stock yet.\n\n"
                        + "Complete a processing batch and add its filament output."
        );
    }

    private void showError(String message) {
        loadingProgress.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        stateContainer.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.VISIBLE);
        stateText.setText(message);
        summaryText.setText(
                "Filament inventory could not be loaded"
        );
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty()
                ? fallback
                : value.trim();
    }

    @Override
    protected void onDestroy() {
        if (inventoryCall != null) {
            inventoryCall.cancel();
            inventoryCall = null;
        }
        if (transactionCall != null) {
            transactionCall.cancel();
            transactionCall = null;
        }
        super.onDestroy();
    }
}
