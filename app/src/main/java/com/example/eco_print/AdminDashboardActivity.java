package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eco_print.api.AdminApi;
import com.example.eco_print.models.UserProfile;
import com.example.eco_print.models.WasteReport;
import com.example.eco_print.utils.SessionManager;
import com.example.eco_print.utils.SupabaseClient;
import com.example.eco_print.utils.SupabaseConfig;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private TextView pendingCountText;
    private TextView verifiedCountText;
    private TextView rejectedCountText;
    private TextView duplicateCountText;
    private TextView pendingCollectorCountText;
    private TextView dashboardStatusText;

    private int pendingReportCount = -1;
    private int pendingCollectorCount = -1;

    private Call<List<WasteReport>> reportsCall;
    private Call<List<UserProfile>> collectorsCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn() || !sessionManager.isAdmin()) {
            openAdminLogin();
            return;
        }

        TextView greetingText = findViewById(R.id.greetingText);
        pendingCountText = findViewById(R.id.pendingCountText);
        verifiedCountText = findViewById(R.id.verifiedCountText);
        rejectedCountText = findViewById(R.id.rejectedCountText);
        duplicateCountText = findViewById(R.id.duplicateCountText);
        pendingCollectorCountText = findViewById(
                R.id.pendingCollectorCountText
        );
        dashboardStatusText = findViewById(R.id.dashboardStatusText);
        Button reviewReportsButton = findViewById(R.id.reviewReportsButton);
        Button collectorApplicationsButton = findViewById(
                R.id.collectorApplicationsButton
        );
        Button logoutButton = findViewById(R.id.logoutButton);

        String name = sessionManager.getUserName();
        greetingText.setText(
                name == null || name.trim().isEmpty()
                        ? "Administrator access active"
                        : "Welcome, " + name.trim()
        );

        reviewReportsButton.setOnClickListener(v ->
                startActivity(new Intent(
                        this,
                        AdminReportListActivity.class
                ))
        );

        collectorApplicationsButton.setOnClickListener(v ->
                startActivity(new Intent(
                        this,
                        AdminCollectorListActivity.class
                ))
        );

        logoutButton.setOnClickListener(v -> confirmLogout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager != null && sessionManager.isAdmin()) {
            loadDashboardCounts();
        }
    }

    private void loadDashboardCounts() {
        if (!SupabaseConfig.isConfigured()) {
            dashboardStatusText.setText("Supabase is not configured");
            return;
        }

        cancelCalls();
        pendingReportCount = -1;
        pendingCollectorCount = -1;
        dashboardStatusText.setText("Refreshing administrator summary...");

        AdminApi api = SupabaseClient.getClient().create(AdminApi.class);
        loadReportCounts(api);
        loadCollectorCount(api);
    }

    private void loadReportCounts(AdminApi api) {
        reportsCall = api.getAllReports(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "*",
                "created_at.desc"
        );

        reportsCall.enqueue(new Callback<List<WasteReport>>() {
            @Override
            public void onResponse(
                    Call<List<WasteReport>> call,
                    Response<List<WasteReport>> response
            ) {
                reportsCall = null;
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (response.code() == 401) {
                    sessionManager.logout();
                    openAdminLogin();
                    return;
                }

                if (!response.isSuccessful() || response.body() == null) {
                    dashboardStatusText.setText(
                            "Report summary could not be loaded"
                    );
                    return;
                }

                int pending = 0;
                int verified = 0;
                int rejected = 0;
                int duplicates = 0;

                for (WasteReport report : response.body()) {
                    String status = report.getStatus() == null
                            ? ""
                            : report.getStatus();
                    if ("Pending".equalsIgnoreCase(status)) {
                        pending++;
                    }
                    if ("Verified".equalsIgnoreCase(status)) {
                        verified++;
                    }
                    if ("Rejected".equalsIgnoreCase(status)) {
                        rejected++;
                    }
                    if (report.isPossibleDuplicate()) {
                        duplicates++;
                    }
                }

                pendingReportCount = pending;
                pendingCountText.setText(String.valueOf(pending));
                verifiedCountText.setText(String.valueOf(verified));
                rejectedCountText.setText(String.valueOf(rejected));
                duplicateCountText.setText(String.valueOf(duplicates));
                updateSummaryText();
            }

            @Override
            public void onFailure(
                    Call<List<WasteReport>> call,
                    Throwable throwable
            ) {
                reportsCall = null;
                if (!call.isCanceled()
                        && !isFinishing()
                        && !isDestroyed()) {
                    dashboardStatusText.setText(
                            "Could not connect to the server"
                    );
                }
            }
        });
    }

    private void loadCollectorCount(AdminApi api) {
        collectorsCall = api.getCollectorApplications(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "eq.collector",
                "id,collector_status",
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
                    sessionManager.logout();
                    openAdminLogin();
                    return;
                }

                if (!response.isSuccessful() || response.body() == null) {
                    pendingCollectorCountText.setText("—");
                    return;
                }

                int pending = 0;
                for (UserProfile profile : response.body()) {
                    if ("pending".equalsIgnoreCase(
                            profile.getCollectorStatus()
                    )) {
                        pending++;
                    }
                }

                pendingCollectorCount = pending;
                pendingCollectorCountText.setText(String.valueOf(pending));
                updateSummaryText();
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
                    pendingCollectorCountText.setText("—");
                }
            }
        });
    }

    private void updateSummaryText() {
        if (pendingReportCount < 0 && pendingCollectorCount < 0) {
            dashboardStatusText.setText("Refreshing administrator summary...");
            return;
        }

        if (pendingReportCount >= 0 && pendingCollectorCount >= 0) {
            if (pendingReportCount == 0 && pendingCollectorCount == 0) {
                dashboardStatusText.setText(
                        "Nothing is waiting for administrator review"
                );
            } else {
                dashboardStatusText.setText(
                        pendingReportCount
                                + " report"
                                + (pendingReportCount == 1 ? "" : "s")
                                + " and "
                                + pendingCollectorCount
                                + " collector application"
                                + (pendingCollectorCount == 1 ? "" : "s")
                                + " waiting for review"
                );
            }
            return;
        }

        if (pendingReportCount >= 0) {
            dashboardStatusText.setText(
                    pendingReportCount
                            + " report"
                            + (pendingReportCount == 1 ? "" : "s")
                            + " waiting for review"
            );
        } else {
            dashboardStatusText.setText(
                    pendingCollectorCount
                            + " collector application"
                            + (pendingCollectorCount == 1 ? "" : "s")
                            + " waiting for review"
            );
        }
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout administrator?")
                .setMessage(
                        "You will need to verify the administrator account again."
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Logout", (dialog, which) -> {
                    sessionManager.logout();
                    openAdminLogin();
                })
                .show();
    }

    private void openAdminLogin() {
        Intent intent = new Intent(this, AdminLoginActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        startActivity(intent);
        finish();
    }

    private void cancelCalls() {
        if (reportsCall != null) {
            reportsCall.cancel();
            reportsCall = null;
        }
        if (collectorsCall != null) {
            collectorsCall.cancel();
            collectorsCall = null;
        }
    }

    @Override
    protected void onDestroy() {
        cancelCalls();
        super.onDestroy();
    }
}
