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

import com.example.eco_print.adapter.AdminCollectorAdapter;
import com.example.eco_print.api.AdminApi;
import com.example.eco_print.models.UserProfile;
import com.example.eco_print.utils.SessionManager;
import com.example.eco_print.utils.SupabaseClient;
import com.example.eco_print.utils.SupabaseConfig;
import com.google.android.material.button.MaterialButton;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminCollectorListActivity extends AppCompatActivity {

    private RecyclerView collectorsRecyclerView;
    private ProgressBar loadingProgress;
    private View stateContainer;
    private TextView stateText;
    private TextView countText;
    private MaterialButton retryButton;
    private AdminCollectorAdapter adapter;
    private SessionManager sessionManager;
    private Call<List<UserProfile>> collectorsCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_collector_list);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn() || !sessionManager.isAdmin()) {
            redirectToAdminLogin();
            return;
        }

        ImageButton backButton = findViewById(R.id.backButton);
        collectorsRecyclerView = findViewById(R.id.collectorsRecyclerView);
        loadingProgress = findViewById(R.id.loadingProgress);
        stateContainer = findViewById(R.id.stateContainer);
        stateText = findViewById(R.id.stateText);
        countText = findViewById(R.id.countText);
        retryButton = findViewById(R.id.retryButton);

        adapter = new AdminCollectorAdapter(this::openCollector);
        collectorsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );
        collectorsRecyclerView.setAdapter(adapter);
        collectorsRecyclerView.setHasFixedSize(true);
        collectorsRecyclerView.setItemAnimator(null);

        backButton.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );
        retryButton.setOnClickListener(v -> loadCollectors());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager != null && sessionManager.isAdmin()) {
            loadCollectors();
        }
    }

    private void loadCollectors() {
        if (!SupabaseConfig.isConfigured()) {
            showError("Supabase is not configured.");
            return;
        }

        if (collectorsCall != null) {
            collectorsCall.cancel();
        }

        showLoading();
        AdminApi api = SupabaseClient.getClient().create(AdminApi.class);
        collectorsCall = api.getCollectorApplications(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "eq.collector",
                "*",
                "created_at.desc"
        );

        collectorsCall.enqueue(new Callback<List<UserProfile>>() {
            @Override
            public void onResponse(
                    Call<List<UserProfile>> call,
                    Response<List<UserProfile>> response
            ) {
                collectorsCall = null;
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (response.code() == 401) {
                    showToast("Administrator session expired");
                    sessionManager.logout();
                    redirectToAdminLogin();
                    return;
                }

                if (!response.isSuccessful() || response.body() == null) {
                    showError(
                            "Collector applications could not be loaded. Error "
                                    + response.code()
                    );
                    return;
                }

                List<UserProfile> profiles = response.body();
                Collections.sort(profiles, new Comparator<UserProfile>() {
                    @Override
                    public int compare(UserProfile first, UserProfile second) {
                        return Integer.compare(
                                statusPriority(first.getCollectorStatus()),
                                statusPriority(second.getCollectorStatus())
                        );
                    }
                });

                int pending = 0;
                for (UserProfile profile : profiles) {
                    if ("pending".equalsIgnoreCase(
                            profile.getCollectorStatus()
                    )) {
                        pending++;
                    }
                }

                adapter.setCollectors(profiles);
                countText.setText(
                        pending + " pending • " + profiles.size() + " total"
                );

                if (profiles.isEmpty()) {
                    showEmpty();
                } else {
                    showCollectors();
                }
            }

            @Override
            public void onFailure(
                    Call<List<UserProfile>> call,
                    Throwable throwable
            ) {
                collectorsCall = null;
                if (!call.isCanceled()
                        && !isFinishing()
                        && !isDestroyed()) {
                    showError(
                            "Could not connect to the server. Check your internet and retry."
                    );
                }
            }
        });
    }

    private int statusPriority(String status) {
        String value = status == null
                ? ""
                : status.trim().toLowerCase(Locale.US);
        if ("pending".equals(value)) {
            return 0;
        }
        if ("rejected".equals(value)) {
            return 1;
        }
        if ("approved".equals(value)) {
            return 2;
        }
        return 3;
    }

    private void openCollector(UserProfile profile) {
        if (profile.getId() == null || profile.getId().trim().isEmpty()) {
            return;
        }

        Intent intent = new Intent(
                this,
                AdminCollectorDetailsActivity.class
        );
        intent.putExtra(
                AdminCollectorDetailsActivity.EXTRA_COLLECTOR_ID,
                profile.getId()
        );
        startActivity(intent);
    }

    private void showLoading() {
        loadingProgress.setVisibility(View.VISIBLE);
        collectorsRecyclerView.setVisibility(View.GONE);
        stateContainer.setVisibility(View.GONE);
        countText.setText("Loading collector applications...");
    }

    private void showCollectors() {
        loadingProgress.setVisibility(View.GONE);
        stateContainer.setVisibility(View.GONE);
        collectorsRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showEmpty() {
        loadingProgress.setVisibility(View.GONE);
        collectorsRecyclerView.setVisibility(View.GONE);
        stateContainer.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
        stateText.setText(
                "No collector applications\n\n"
                        + "New applications will appear here for approval."
        );
    }

    private void showError(String message) {
        loadingProgress.setVisibility(View.GONE);
        collectorsRecyclerView.setVisibility(View.GONE);
        stateContainer.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.VISIBLE);
        stateText.setText(message);
        countText.setText("Applications could not be loaded");
    }

    private void redirectToAdminLogin() {
        Intent intent = new Intent(this, AdminLoginActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        if (collectorsCall != null) {
            collectorsCall.cancel();
            collectorsCall = null;
        }
        super.onDestroy();
    }
}
