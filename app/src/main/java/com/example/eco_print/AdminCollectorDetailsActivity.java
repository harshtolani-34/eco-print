package com.example.eco_print;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eco_print.api.AdminApi;
import com.example.eco_print.models.ReviewCollectorApplicationRequest;
import com.example.eco_print.models.UserProfile;
import com.example.eco_print.utils.SessionManager;
import com.example.eco_print.utils.SupabaseClient;
import com.example.eco_print.utils.SupabaseConfig;
import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminCollectorDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_COLLECTOR_ID = "admin_collector_id";

    private ProgressBar loadingProgress;
    private ScrollView contentScroll;
    private TextView statusText;
    private TextView nameText;
    private TextView emailText;
    private TextView ageText;
    private TextView companyCodeText;
    private TextView applicationDateText;
    private TextView reviewedDateText;
    private TextView decisionMessageText;
    private MaterialButton approveButton;
    private MaterialButton rejectButton;

    private SessionManager sessionManager;
    private String collectorId;
    private UserProfile currentProfile;
    private Call<List<UserProfile>> detailsCall;
    private Call<List<UserProfile>> reviewCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_collector_details);

        sessionManager = new SessionManager(this);
        collectorId = getIntent().getStringExtra(EXTRA_COLLECTOR_ID);

        if (!sessionManager.isLoggedIn() || !sessionManager.isAdmin()) {
            redirectToAdminLogin();
            return;
        }

        bindViews();
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );
        approveButton.setOnClickListener(v -> confirmApproval());
        rejectButton.setOnClickListener(v -> showRejectionDialog());

        if (collectorId == null || collectorId.trim().isEmpty()) {
            showToast("Collector ID is missing");
            finish();
        }
    }

    private void bindViews() {
        loadingProgress = findViewById(R.id.loadingProgress);
        contentScroll = findViewById(R.id.contentScroll);
        statusText = findViewById(R.id.statusText);
        nameText = findViewById(R.id.nameText);
        emailText = findViewById(R.id.emailText);
        ageText = findViewById(R.id.ageText);
        companyCodeText = findViewById(R.id.companyCodeText);
        applicationDateText = findViewById(R.id.applicationDateText);
        reviewedDateText = findViewById(R.id.reviewedDateText);
        decisionMessageText = findViewById(R.id.decisionMessageText);
        approveButton = findViewById(R.id.approveButton);
        rejectButton = findViewById(R.id.rejectButton);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (collectorId != null && !collectorId.trim().isEmpty()) {
            loadCollector();
        }
    }

    private void loadCollector() {
        if (!SupabaseConfig.isConfigured()) {
            showLoadError("Supabase is not configured.");
            return;
        }

        if (detailsCall != null) {
            detailsCall.cancel();
        }

        setLoading(true);
        AdminApi api = SupabaseClient.getClient().create(AdminApi.class);
        detailsCall = api.getCollectorById(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "eq." + collectorId,
                "*"
        );

        detailsCall.enqueue(new Callback<List<UserProfile>>() {
            @Override
            public void onResponse(
                    Call<List<UserProfile>> call,
                    Response<List<UserProfile>> response
            ) {
                detailsCall = null;
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                setLoading(false);
                if (response.code() == 401) {
                    sessionManager.logout();
                    redirectToAdminLogin();
                    return;
                }

                if (!response.isSuccessful()
                        || response.body() == null
                        || response.body().isEmpty()) {
                    showLoadError(
                            "The collector application could not be loaded. Error "
                                    + response.code()
                    );
                    return;
                }

                UserProfile profile = response.body().get(0);
                if (!"collector".equalsIgnoreCase(profile.getRole())) {
                    showToast("This profile is not a collector application");
                    finish();
                    return;
                }

                bindProfile(profile);
            }

            @Override
            public void onFailure(
                    Call<List<UserProfile>> call,
                    Throwable throwable
            ) {
                detailsCall = null;
                if (!call.isCanceled()
                        && !isFinishing()
                        && !isDestroyed()) {
                    setLoading(false);
                    showLoadError("Could not connect to the server.");
                }
            }
        });
    }

    private void bindProfile(UserProfile profile) {
        currentProfile = profile;
        String status = safeText(profile.getCollectorStatus(), "Pending");

        statusText.setText(status.toUpperCase(Locale.US));
        applyStatusStyle(statusText, status);
        nameText.setText(safeText(profile.getFullName(), "Unnamed collector"));
        emailText.setText(safeText(profile.getEmail(), "Email unavailable"));
        ageText.setText(
                profile.getAge() == null
                        ? "Age not provided"
                        : String.valueOf(profile.getAge())
        );
        companyCodeText.setText(
                safeText(profile.getCompanyCode(), "Company code not provided")
        );
        applicationDateText.setText(formatDate(profile.getCreatedAt()));
        reviewedDateText.setText(formatDate(profile.getCollectorReviewedAt()));

        boolean pending = "pending".equalsIgnoreCase(status);
        boolean rejected = "rejected".equalsIgnoreCase(status);
        boolean approved = "approved".equalsIgnoreCase(status);

        approveButton.setVisibility(
                pending || rejected ? View.VISIBLE : View.GONE
        );
        approveButton.setText(
                rejected ? "APPROVE ON RECONSIDERATION" : "APPROVE COLLECTOR"
        );
        rejectButton.setVisibility(pending ? View.VISIBLE : View.GONE);

        if (pending) {
            decisionMessageText.setVisibility(View.GONE);
        } else {
            decisionMessageText.setVisibility(View.VISIBLE);
            if (approved) {
                decisionMessageText.setText(
                        "This collector has been approved and can access verified collection tasks."
                );
            } else if (rejected) {
                decisionMessageText.setText(
                        "Application rejected.\nReason: "
                                + safeText(
                                profile.getCollectorRejectionReason(),
                                "No reason saved"
                        )
                );
            } else {
                decisionMessageText.setText(
                        "Current collector status: " + status
                );
            }
        }
    }

    private void confirmApproval() {
        if (currentProfile == null || reviewCall != null) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Approve this collector?")
                .setMessage(
                        "The collector will be allowed to view verified reports, accept tasks and update collection progress."
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton(
                        "Approve",
                        (dialog, which) -> submitReview(
                                ReviewCollectorApplicationRequest.approve(
                                        collectorId
                                ),
                                "approved"
                        )
                )
                .show();
    }

    private void showRejectionDialog() {
        if (currentProfile == null || reviewCall != null) {
            return;
        }

        EditText input = new EditText(this);
        input.setHint("Reason for rejecting the application");
        input.setMinLines(3);
        input.setPadding(
                dpToPx(18),
                dpToPx(10),
                dpToPx(18),
                dpToPx(10)
        );

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Reject this collector?")
                .setMessage(
                        "Give a clear reason that can be shown to the applicant."
                )
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reject", null)
                .create();

        dialog.setOnShowListener(ignored ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(v -> {
                            String reason = input.getText()
                                    .toString()
                                    .trim();
                            if (reason.length() < 5) {
                                input.setError(
                                        "Enter a clear rejection reason"
                                );
                                return;
                            }

                            dialog.dismiss();
                            submitReview(
                                    ReviewCollectorApplicationRequest.reject(
                                            collectorId,
                                            reason
                                    ),
                                    "rejected"
                            );
                        })
        );
        dialog.show();
    }

    private void submitReview(
            ReviewCollectorApplicationRequest request,
            String resultStatus
    ) {
        setReviewLoading(true);
        AdminApi api = SupabaseClient.getClient().create(AdminApi.class);
        reviewCall = api.reviewCollectorApplication(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                request
        );

        reviewCall.enqueue(new Callback<List<UserProfile>>() {
            @Override
            public void onResponse(
                    Call<List<UserProfile>> call,
                    Response<List<UserProfile>> response
            ) {
                reviewCall = null;
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                setReviewLoading(false);
                if (response.code() == 401) {
                    sessionManager.logout();
                    redirectToAdminLogin();
                    return;
                }

                if (!response.isSuccessful()) {
                    showReviewError(
                            "The application was not changed. Error "
                                    + response.code()
                    );
                    return;
                }

                if (response.body() == null || response.body().isEmpty()) {
                    showReviewError(
                            "The application may already have been reviewed."
                    );
                    return;
                }

                bindProfile(response.body().get(0));
                showToast(
                        "Collector " + resultStatus + " successfully"
                );
            }

            @Override
            public void onFailure(
                    Call<List<UserProfile>> call,
                    Throwable throwable
            ) {
                reviewCall = null;
                if (!call.isCanceled()
                        && !isFinishing()
                        && !isDestroyed()) {
                    setReviewLoading(false);
                    showReviewError(
                            "Could not connect to the server. The application was not changed."
                    );
                }
            }
        });
    }

    private void setReviewLoading(boolean loading) {
        approveButton.setEnabled(!loading);
        rejectButton.setEnabled(!loading);
        if (loading) {
            approveButton.setText("UPDATING...");
            rejectButton.setText("PLEASE WAIT");
        } else if (currentProfile != null) {
            bindProfile(currentProfile);
        }
    }

    private void setLoading(boolean loading) {
        loadingProgress.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );
        contentScroll.setVisibility(
                loading ? View.GONE : View.VISIBLE
        );
    }

    private void showLoadError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Application could not be loaded")
                .setMessage(message)
                .setNegativeButton(
                        "Close",
                        (dialog, which) -> finish()
                )
                .setPositiveButton(
                        "Try Again",
                        (dialog, which) -> loadCollector()
                )
                .show();
    }

    private void showReviewError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Decision not saved")
                .setMessage(
                        message
                                + " Make sure the Module 5 Phase 3 SQL has been run."
                )
                .setPositiveButton("OK", null)
                .show();
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

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty()
                ? fallback
                : value.trim();
    }

    private String formatDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return "Not reviewed yet";
        }

        String datePart = rawDate.length() >= 19
                ? rawDate.substring(0, 19)
                : rawDate;
        SimpleDateFormat input = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                Locale.US
        );
        SimpleDateFormat output = new SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                Locale.getDefault()
        );

        try {
            Date date = input.parse(datePart);
            return date == null ? rawDate : output.format(date);
        } catch (ParseException ignored) {
            return rawDate;
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources()
                .getDisplayMetrics().density);
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
        if (detailsCall != null) {
            detailsCall.cancel();
            detailsCall = null;
        }
        if (reviewCall != null) {
            reviewCall.cancel();
            reviewCall = null;
        }
        super.onDestroy();
    }
}
