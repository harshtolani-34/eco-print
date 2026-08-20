package com.example.eco_print;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eco_print.adapter.InventoryTransactionAdapter;
import com.example.eco_print.api.InventoryApi;
import com.example.eco_print.models.InventoryTransaction;
import com.example.eco_print.utils.RoleNavigator;
import com.example.eco_print.utils.SessionManager;
import com.example.eco_print.utils.SupabaseClient;
import com.example.eco_print.utils.SupabaseConfig;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InventoryTransactionHistoryActivity
        extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar loadingProgress;
    private View stateContainer;
    private TextView stateText;
    private TextView summaryText;
    private MaterialButton retryButton;

    private SessionManager sessionManager;
    private InventoryTransactionAdapter adapter;

    private Call<List<InventoryTransaction>> transactionsCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(
                R.layout.activity_inventory_transactions
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

        adapter = new InventoryTransactionAdapter();

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
                loadTransactions()
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTransactions();
    }

    private void loadTransactions() {
        if (transactionsCall != null) {
            transactionsCall.cancel();
        }

        showLoading();

        InventoryApi api = SupabaseClient.getClient()
                .create(InventoryApi.class);

        transactionsCall = api.getInventoryTransactions(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "*",
                "created_at.desc"
        );

        transactionsCall.enqueue(
                new Callback<List<InventoryTransaction>>() {
                    @Override
                    public void onResponse(
                            Call<List<InventoryTransaction>> call,
                            Response<List<InventoryTransaction>> response
                    ) {
                        transactionsCall = null;
                        if (isFinishing() || isDestroyed()) return;

                        if (!response.isSuccessful()
                                || response.body() == null) {
                            showError(
                                    "Transaction history could not be loaded. Error "
                                            + response.code()
                            );
                            return;
                        }

                        List<InventoryTransaction> items =
                                response.body();

                        adapter.setTransactions(items);

                        summaryText.setText(
                                items.size() == 1
                                        ? "1 inventory transaction"
                                        : items.size()
                                          + " inventory transactions"
                        );

                        if (items.isEmpty()) {
                            showEmpty();
                        } else {
                            showList();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<List<InventoryTransaction>> call,
                            Throwable throwable
                    ) {
                        transactionsCall = null;
                        if (!call.isCanceled()
                                && !isFinishing()
                                && !isDestroyed()) {
                            showError(
                                    "Could not connect to the server."
                            );
                        }
                    }
                }
        );
    }

    private void showLoading() {
        loadingProgress.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        stateContainer.setVisibility(View.GONE);
        summaryText.setText(
                "Loading transaction history..."
        );
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
                "No inventory transactions yet.\n\n"
                        + "Filament production and stock removals will appear here."
        );
    }

    private void showError(String message) {
        loadingProgress.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        stateContainer.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.VISIBLE);
        stateText.setText(message);
        summaryText.setText(
                "Transaction history could not be loaded"
        );
    }

    @Override
    protected void onDestroy() {
        if (transactionsCall != null) {
            transactionsCall.cancel();
            transactionsCall = null;
        }
        super.onDestroy();
    }
}
