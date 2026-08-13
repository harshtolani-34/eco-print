package com.example.eco_print;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eco_print.api.ProfileApi;
import com.example.eco_print.models.UserProfile;
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

public class CollectorApplicationStatusActivity extends AppCompatActivity {

    private TextView nameText;
    private TextView emailText;
    private TextView statusText;
    private TextView explanationText;
    private TextView rejectionReasonText;
    private ProgressBar loadingProgress;
    private MaterialButton refreshButton;
    private MaterialButton continueButton;
    private MaterialButton logoutButton;

    private SessionManager sessionManager;
    private Call<List<UserProfile>> profileCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collector_application_status);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        bindViews();
        refreshButton.setOnClickListener(v -> loadCurrentProfile());
        continueButton.setOnClickListener(v -> {
            if (sessionManager.isApprovedCollector()) {
                RoleNavigator.openCorrectHome(this, sessionManager);
            }
        });
        logoutButton.setOnClickListener(v -> confirmLogout());

        showLocalStatus();
    }

    private void bindViews() {
        nameText = findViewById(R.id.nameText);
        emailText = findViewById(R.id.emailText);
        statusText = findViewById(R.id.statusText);
        explanationText = findViewById(R.id.explanationText);
        rejectionReasonText = findViewById(R.id.rejectionReasonText);
        loadingProgress = findViewById(R.id.loadingProgress);
        refreshButton = findViewById(R.id.refreshButton);
        continueButton = findViewById(R.id.continueButton);
        logoutButton = findViewById(R.id.logoutButton);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCurrentProfile();
    }

    private void loadCurrentProfile() {
        if (!SupabaseConfig.isConfigured()) {
            showToast("Supabase is not configured");
            return;
        }

        if (profileCall != null) {
            profileCall.cancel();
        }

        setLoading(true);
        ProfileApi api = SupabaseClient.getClient().create(ProfileApi.class);
        profileCall = api.getMyProfile(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "eq." + sessionManager.getUserId(),
                "*"
        );

        profileCall.enqueue(new Callback<List<UserProfile>>() {
            @Override
            public void onResponse(
                    Call<List<UserProfile>> call,
                    Response<List<UserProfile>> response
            ) {
                profileCall = null;
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                setLoading(false);
                if (response.code() == 401) {
                    showToast("Your session expired");
                    sessionManager.logout();
                    redirectToLogin();
                    return;
                }

                if (!response.isSuccessful()
                        || response.body() == null
                        || response.body().isEmpty()) {
                    showToast("Application status could not be refreshed");
                    showLocalStatus();
                    return;
                }

                UserProfile profile = response.body().get(0);
                sessionManager.saveProfile(
                        profile.getRole(),
                        profile.getFullName(),
                        profile.getCollectorStatus()
                );

                if (!sessionManager.isCollector()) {
                    RoleNavigator.openCorrectHome(
                            CollectorApplicationStatusActivity.this,
                            sessionManager
                    );
                    return;
                }

                bindProfile(profile);
            }

            @Override
            public void onFailure(
                    Call<List<UserProfile>> call,
                    Throwable throwable
            ) {
                profileCall = null;
                if (!call.isCanceled()
                        && !isFinishing()
                        && !isDestroyed()) {
                    setLoading(false);
                    showToast(
                            "Could not refresh the application status"
                    );
                    showLocalStatus();
                }
            }
        });
    }

    private void showLocalStatus() {
        nameText.setText(
                safeText(sessionManager.getUserName(), "Collector applicant")
        );
        emailText.setText(
                safeText(sessionManager.getUserEmail(), "Email unavailable")
        );
        bindStatus(
                sessionManager.getCollectorStatus(),
                null
        );
    }

    private void bindProfile(UserProfile profile) {
        nameText.setText(
                safeText(profile.getFullName(), "Collector applicant")
        );
        emailText.setText(
                safeText(profile.getEmail(), sessionManager.getUserEmail())
        );
        bindStatus(
                profile.getCollectorStatus(),
                profile.getCollectorRejectionReason()
        );
    }

    private void bindStatus(String rawStatus, String rejectionReason) {
        String status = safeText(rawStatus, "pending");
        statusText.setText(status.toUpperCase(Locale.US));
        applyStatusStyle(statusText, status);

        continueButton.setVisibility(View.GONE);
        rejectionReasonText.setVisibility(View.GONE);

        if ("approved".equalsIgnoreCase(status)) {
            explanationText.setText(
                    "Your collector application has been approved. You can now open the collector dashboard and accept verified collection tasks."
            );
            continueButton.setVisibility(View.VISIBLE);
        } else if ("rejected".equalsIgnoreCase(status)) {
            explanationText.setText(
                    "Your collector application was not approved. Review the administrator's reason below."
            );
            rejectionReasonText.setVisibility(View.VISIBLE);
            rejectionReasonText.setText(
                    "Reason: " + safeText(
                            rejectionReason,
                            "No rejection reason was saved"
                    )
            );
        } else {
            explanationText.setText(
                    "Your application is waiting for administrator verification. Refresh this page after the administrator reviews it."
            );
        }
    }

    private void applyStatusStyle(TextView view, String status) {
        int color;
        if ("approved".equalsIgnoreCase(status)) {
            color = Color.parseColor("#7CFF5B");
        } else if ("rejected".equalsIgnoreCase(status)) {
            color = Color.parseColor("#FF5C5C");
        } else {
            color = Color.parseColor("#FFC857");
        }

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.argb(
                38,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        ));
        background.setStroke(dpToPx(1), color);
        background.setCornerRadius(dpToPx(50));
        view.setBackground(background);
        view.setTextColor(color);
    }

    private void setLoading(boolean loading) {
        loadingProgress.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );
        refreshButton.setEnabled(!loading);
        refreshButton.setText(
                loading ? "REFRESHING..." : "REFRESH STATUS"
        );
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout?")
                .setMessage(
                        "You can log in again later to check the application status."
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Logout", (dialog, which) -> {
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
                })
                .show();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        startActivity(intent);
        finish();
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty()
                ? fallback
                : value.trim();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources()
                .getDisplayMetrics().density);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        if (profileCall != null) {
            profileCall.cancel();
            profileCall = null;
        }
        super.onDestroy();
    }
}
