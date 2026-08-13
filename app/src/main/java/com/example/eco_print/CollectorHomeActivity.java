package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eco_print.adapter.CollectorAssignedReportAdapter;
import com.example.eco_print.adapter.CollectorReportAdapter;
import com.example.eco_print.api.WasteReportApi;
import com.example.eco_print.models.AcceptReportRequest;
import com.example.eco_print.models.WasteReport;
import com.example.eco_print.utils.RoleNavigator;
import com.example.eco_print.utils.SessionManager;
import com.example.eco_print.utils.SupabaseClient;
import com.example.eco_print.utils.SupabaseConfig;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CollectorHomeActivity extends AppCompatActivity {

    private TextView greetingText;
    private TextView availableCountText;
    private TextView acceptedCountText;
    private TextView sectionTitle;
    private TextView sectionSubtitle;
    private TextView emptyStateText;
    private ProgressBar loadingProgress;
    private RecyclerView reportsRecyclerView;
    private ShapeableImageView profileImage;
    private View stateContainer;
    private View availableSummaryCard;
    private View acceptedSummaryCard;
    private MaterialButton retryButton;
    private MaterialButtonToggleGroup sectionToggleGroup;

    private SessionManager sessionManager;
    private CollectorReportAdapter availableAdapter;
    private CollectorAssignedReportAdapter assignedAdapter;

    private boolean showingMyCollections;

    private Call<List<WasteReport>> availableReportsCall;
    private Call<List<WasteReport>> assignedReportsCall;
    private Call<List<WasteReport>> summaryCall;
    private Call<List<WasteReport>> acceptReportCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collector_home);

        bindViews();
        sessionManager = new SessionManager(this);

        if (!sessionManager.isApprovedCollector()) {
            RoleNavigator.openCorrectHome(this, sessionManager);
            return;
        }

        updateGreeting();
        configureRecyclerView();
        configureActions();
        showAvailableSection(false);
    }

    private void bindViews() {
        greetingText = findViewById(R.id.greetingText);
        availableCountText = findViewById(R.id.availableCountText);
        acceptedCountText = findViewById(R.id.acceptedCountText);
        sectionTitle = findViewById(R.id.sectionTitle);
        sectionSubtitle = findViewById(R.id.sectionSubtitle);
        emptyStateText = findViewById(R.id.emptyStateText);
        loadingProgress = findViewById(R.id.loadingProgress);
        reportsRecyclerView = findViewById(R.id.reportsRecyclerView);
        profileImage = findViewById(R.id.profileImage);
        stateContainer = findViewById(R.id.stateContainer);
        retryButton = findViewById(R.id.retryButton);
        sectionToggleGroup = findViewById(R.id.sectionToggleGroup);
        availableSummaryCard = findViewById(R.id.availableSummaryCard);
        acceptedSummaryCard = findViewById(R.id.acceptedSummaryCard);
    }

    private void updateGreeting() {
        String collectorName = sessionManager.getUserName();
        greetingText.setText(
                collectorName == null || collectorName.trim().isEmpty()
                        ? "Ready for collection?"
                        : "Welcome, " + collectorName.trim()
        );
    }

    private void configureRecyclerView() {
        availableAdapter = new CollectorReportAdapter(
                this::showAcceptConfirmation
        );
        assignedAdapter = new CollectorAssignedReportAdapter(
                this::openCollectionDetails
        );

        reportsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );
        reportsRecyclerView.setHasFixedSize(true);
        reportsRecyclerView.setItemAnimator(null);
    }

    private void configureActions() {
        profileImage.setOnClickListener(v -> showProfileMenu());
        retryButton.setOnClickListener(v -> loadCurrentSection());

        sectionToggleGroup.addOnButtonCheckedListener(
                (group, checkedId, isChecked) -> {
                    if (!isChecked) {
                        return;
                    }

                    if (checkedId == R.id.myCollectionsTabButton) {
                        showMyCollectionsSection(true);
                    } else if (checkedId == R.id.availableTabButton) {
                        showAvailableSection(true);
                    }
                }
        );

        availableSummaryCard.setOnClickListener(v -> {
            if (showingMyCollections) {
                sectionToggleGroup.check(R.id.availableTabButton);
            } else {
                loadCollectorDashboard();
            }
        });

        acceptedSummaryCard.setOnClickListener(v -> {
            if (!showingMyCollections) {
                sectionToggleGroup.check(R.id.myCollectionsTabButton);
            } else {
                loadCollectorDashboard();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sessionManager != null && sessionManager.isApprovedCollector()) {
            updateGreeting();
            loadCollectorDashboard();
        }
    }

    private void loadCollectorDashboard() {
        if (!sessionManager.isLoggedIn()) {
            logout();
            return;
        }

        if (!SupabaseConfig.isConfigured()) {
            showErrorState(
                    "Supabase is not configured. Add the anon key and try again."
            );
            return;
        }

        loadCurrentSection();
        loadOtherSectionCount();
    }

    private void loadCurrentSection() {
        if (showingMyCollections) {
            loadMyCollections();
        } else {
            loadAvailableReports();
        }
    }

    private void loadOtherSectionCount() {
        if (summaryCall != null) {
            summaryCall.cancel();
        }

        WasteReportApi api = SupabaseClient.getClient()
                .create(WasteReportApi.class);

        if (showingMyCollections) {
            summaryCall = api.getAvailableReports(
                    SupabaseConfig.SUPABASE_ANON_KEY,
                    sessionManager.getAuthorizationHeader(),
                    "eq.Verified",
                    "is.null",
                    "id",
                    "created_at.desc"
            );
        } else {
            summaryCall = api.getAcceptedReports(
                    SupabaseConfig.SUPABASE_ANON_KEY,
                    sessionManager.getAuthorizationHeader(),
                    "eq." + sessionManager.getUserId(),
                    "id",
                    "assigned_at.desc"
            );
        }

        summaryCall.enqueue(new Callback<List<WasteReport>>() {
            @Override
            public void onResponse(
                    Call<List<WasteReport>> call,
                    Response<List<WasteReport>> response
            ) {
                summaryCall = null;
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    if (showingMyCollections) {
                        availableCountText.setText(
                                String.valueOf(response.body().size())
                        );
                    } else {
                        acceptedCountText.setText(
                                String.valueOf(response.body().size())
                        );
                    }
                }
            }

            @Override
            public void onFailure(
                    Call<List<WasteReport>> call,
                    Throwable throwable
            ) {
                summaryCall = null;
            }
        });
    }

    private void loadAvailableReports() {
        cancelListCalls();
        showLoading(true);
        reportsRecyclerView.setAdapter(availableAdapter);

        WasteReportApi api = SupabaseClient.getClient()
                .create(WasteReportApi.class);

        availableReportsCall = api.getAvailableReports(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "eq.Verified",
                "is.null",
                "*",
                "created_at.desc"
        );

        availableReportsCall.enqueue(new Callback<List<WasteReport>>() {
            @Override
            public void onResponse(
                    Call<List<WasteReport>> call,
                    Response<List<WasteReport>> response
            ) {
                availableReportsCall = null;
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                showLoading(false);
                if (response.code() == 401) {
                    showToast("Your session expired");
                    logout();
                    return;
                }

                if (!response.isSuccessful() || response.body() == null) {
                    showErrorState(
                            "Available reports could not be loaded. Tap Try Again."
                    );
                    return;
                }

                List<WasteReport> reports = response.body();
                availableAdapter.setReports(reports);
                availableCountText.setText(String.valueOf(reports.size()));

                if (reports.isEmpty()) {
                    showEmptyState(
                            "No available reports right now.\n\n"
                                    + "New administrator-verified reports will appear here."
                    );
                } else {
                    showReports();
                }
            }

            @Override
            public void onFailure(
                    Call<List<WasteReport>> call,
                    Throwable throwable
            ) {
                availableReportsCall = null;
                if (call.isCanceled() || isFinishing() || isDestroyed()) {
                    return;
                }

                showLoading(false);
                showErrorState(
                        "Could not connect to the server. Check your internet and try again."
                );
            }
        });
    }

    private void loadMyCollections() {
        cancelListCalls();
        showLoading(true);
        reportsRecyclerView.setAdapter(assignedAdapter);

        WasteReportApi api = SupabaseClient.getClient()
                .create(WasteReportApi.class);

        assignedReportsCall = api.getAcceptedReports(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "eq." + sessionManager.getUserId(),
                "*",
                "assigned_at.desc"
        );

        assignedReportsCall.enqueue(new Callback<List<WasteReport>>() {
            @Override
            public void onResponse(
                    Call<List<WasteReport>> call,
                    Response<List<WasteReport>> response
            ) {
                assignedReportsCall = null;
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                showLoading(false);
                if (response.code() == 401) {
                    showToast("Your session expired");
                    logout();
                    return;
                }

                if (!response.isSuccessful() || response.body() == null) {
                    showErrorState(
                            "Your collections could not be loaded. Tap Try Again."
                    );
                    return;
                }

                List<WasteReport> reports = response.body();
                assignedAdapter.setReports(reports);
                acceptedCountText.setText(String.valueOf(reports.size()));

                if (reports.isEmpty()) {
                    showEmptyState(
                            "You have not accepted a collection yet.\n\n"
                                    + "Open Available Reports and accept a task."
                    );
                } else {
                    showReports();
                }
            }

            @Override
            public void onFailure(
                    Call<List<WasteReport>> call,
                    Throwable throwable
            ) {
                assignedReportsCall = null;
                if (call.isCanceled() || isFinishing() || isDestroyed()) {
                    return;
                }

                showLoading(false);
                showErrorState(
                        "Could not connect to the server. Check your internet and try again."
                );
            }
        });
    }

    private void showAvailableSection(boolean loadNow) {
        showingMyCollections = false;
        sectionTitle.setText("Available Waste Reports");
        sectionSubtitle.setText(
                "Tap a card for details or accept it directly."
        );
        reportsRecyclerView.setAdapter(availableAdapter);
        if (loadNow && sessionManager != null) {
            loadCollectorDashboard();
        }
    }

    private void showMyCollectionsSection(boolean loadNow) {
        showingMyCollections = true;
        sectionTitle.setText("My Collections");
        sectionSubtitle.setText(
                "Open an assigned task to view its map and update progress."
        );
        reportsRecyclerView.setAdapter(assignedAdapter);
        if (loadNow && sessionManager != null) {
            loadCollectorDashboard();
        }
    }

    private void showAcceptConfirmation(WasteReport report) {
        new AlertDialog.Builder(this)
                .setTitle("Accept Collection?")
                .setMessage(
                        "You will become responsible for collecting this waste report. "
                                + "The citizen will immediately see the status as Assigned."
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton(
                        "Accept",
                        (dialog, which) -> acceptReport(report)
                )
                .show();
    }

    private void acceptReport(WasteReport report) {
        if (report.getId() == null || report.getId().trim().isEmpty()) {
            showToast("Report ID is missing");
            return;
        }

        if (acceptReportCall != null) {
            return;
        }

        availableAdapter.setAcceptingReportId(report.getId());
        WasteReportApi api = SupabaseClient.getClient()
                .create(WasteReportApi.class);

        acceptReportCall = api.acceptWasteReport(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                new AcceptReportRequest(report.getId())
        );

        acceptReportCall.enqueue(new Callback<List<WasteReport>>() {
            @Override
            public void onResponse(
                    Call<List<WasteReport>> call,
                    Response<List<WasteReport>> response
            ) {
                acceptReportCall = null;
                availableAdapter.setAcceptingReportId("");

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (response.code() == 401) {
                    showToast("Your session expired");
                    logout();
                    return;
                }

                if (!response.isSuccessful()) {
                    showAcceptError(
                            report,
                            "The report could not be accepted. Error "
                                    + response.code()
                    );
                    return;
                }

                if (response.body() == null || response.body().isEmpty()) {
                    showToast(
                            "Another collector may have already accepted it."
                    );
                    loadCollectorDashboard();
                    return;
                }

                showToast("Collection accepted successfully");
                sectionToggleGroup.check(R.id.myCollectionsTabButton);
            }

            @Override
            public void onFailure(
                    Call<List<WasteReport>> call,
                    Throwable throwable
            ) {
                acceptReportCall = null;
                availableAdapter.setAcceptingReportId("");

                if (call.isCanceled() || isFinishing() || isDestroyed()) {
                    return;
                }

                showAcceptError(
                        report,
                        "Check your internet connection and try again."
                );
            }
        });
    }

    private void showAcceptError(WasteReport report, String message) {
        new AlertDialog.Builder(this)
                .setTitle("Collection not accepted")
                .setMessage(message)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(
                        "Try Again",
                        (dialog, which) -> acceptReport(report)
                )
                .show();
    }

    private void openCollectionDetails(WasteReport report) {
        if (report.getId() == null || report.getId().trim().isEmpty()) {
            showToast("Report ID is missing");
            return;
        }

        Intent intent = new Intent(
                this,
                CollectorTaskDetailsActivity.class
        );
        intent.putExtra(
                CollectorTaskDetailsActivity.EXTRA_REPORT_ID,
                report.getId()
        );
        startActivity(intent);
    }

    private void showLoading(boolean loading) {
        loadingProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            reportsRecyclerView.setVisibility(View.GONE);
            stateContainer.setVisibility(View.GONE);
        }
    }

    private void showReports() {
        loadingProgress.setVisibility(View.GONE);
        stateContainer.setVisibility(View.GONE);
        reportsRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showEmptyState(String message) {
        loadingProgress.setVisibility(View.GONE);
        reportsRecyclerView.setVisibility(View.GONE);
        stateContainer.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
        emptyStateText.setText(message);
    }

    private void showErrorState(String message) {
        loadingProgress.setVisibility(View.GONE);
        reportsRecyclerView.setVisibility(View.GONE);
        stateContainer.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.VISIBLE);
        emptyStateText.setText(message);
    }

    private void showProfileMenu() {
        String name = sessionManager.getUserName();
        String displayName = name == null || name.trim().isEmpty()
                ? "Collector"
                : name.trim();

        new AlertDialog.Builder(this)
                .setTitle(displayName)
                .setMessage(
                        sessionManager.getUserEmail()
                                + "\n\nRole: Collector"
                                + "\nStatus: "
                                + sessionManager.getCollectorStatus()
                )
                .setNegativeButton("Close", null)
                .setPositiveButton(
                        "Logout",
                        (dialog, which) -> logout()
                )
                .show();
    }

    private void cancelListCalls() {
        if (availableReportsCall != null) {
            availableReportsCall.cancel();
            availableReportsCall = null;
        }
        if (assignedReportsCall != null) {
            assignedReportsCall.cancel();
            assignedReportsCall = null;
        }
    }

    private void logout() {
        sessionManager.logout();
        Intent intent = new Intent(this, WelcomeActivity.class);
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
        cancelListCalls();
        if (summaryCall != null) {
            summaryCall.cancel();
            summaryCall = null;
        }
        if (acceptReportCall != null) {
            acceptReportCall.cancel();
            acceptReportCall = null;
        }
        super.onDestroy();
    }
}
