package com.example.eco_print;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.eco_print.api.AdminApi;
import com.example.eco_print.models.ReviewWasteReportRequest;
import com.example.eco_print.models.WasteReport;
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

public class AdminReportDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_REPORT_ID = "admin_report_id";

    private ProgressBar loadingProgress;
    private ScrollView contentScroll;
    private ImageView reportImage;
    private TextView statusText;
    private TextView duplicateWarningText;
    private TextView reportIdText;
    private TextView plasticTypeText;
    private TextView weightText;
    private TextView descriptionText;
    private TextView addressText;
    private TextView coordinatesText;
    private TextView dateText;
    private TextView decisionMessageText;
    private MaterialButton verifyButton;
    private MaterialButton rejectButton;

    private SessionManager sessionManager;
    private String reportId;
    private WasteReport currentReport;
    private Call<List<WasteReport>> detailsCall;
    private Call<List<WasteReport>> reviewCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_report_details);

        sessionManager = new SessionManager(this);
        reportId = getIntent().getStringExtra(EXTRA_REPORT_ID);
        if (!sessionManager.isLoggedIn() || !sessionManager.isAdmin()) {
            redirectToAdminLogin();
            return;
        }

        bindViews();
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        verifyButton.setOnClickListener(v -> confirmVerification());
        rejectButton.setOnClickListener(v -> showRejectionDialog());

        if (reportId == null || reportId.trim().isEmpty()) {
            showToast("Report ID is missing");
            finish();
        }
    }

    private void bindViews() {
        loadingProgress = findViewById(R.id.loadingProgress);
        contentScroll = findViewById(R.id.contentScroll);
        reportImage = findViewById(R.id.reportImage);
        statusText = findViewById(R.id.statusText);
        duplicateWarningText = findViewById(R.id.duplicateWarningText);
        reportIdText = findViewById(R.id.reportIdText);
        plasticTypeText = findViewById(R.id.plasticTypeText);
        weightText = findViewById(R.id.weightText);
        descriptionText = findViewById(R.id.descriptionText);
        addressText = findViewById(R.id.addressText);
        coordinatesText = findViewById(R.id.coordinatesText);
        dateText = findViewById(R.id.dateText);
        decisionMessageText = findViewById(R.id.decisionMessageText);
        verifyButton = findViewById(R.id.verifyButton);
        rejectButton = findViewById(R.id.rejectButton);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (reportId != null && !reportId.trim().isEmpty() && reviewCall == null) {
            loadReport();
        }
    }

    private void loadReport() {
        if (detailsCall != null) detailsCall.cancel();
        setLoading(true);
        AdminApi api = SupabaseClient.getClient().create(AdminApi.class);
        detailsCall = api.getReportById(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "eq." + reportId,
                "*"
        );

        detailsCall.enqueue(new Callback<List<WasteReport>>() {
            @Override
            public void onResponse(
                    Call<List<WasteReport>> call,
                    Response<List<WasteReport>> response
            ) {
                detailsCall = null;
                if (isFinishing() || isDestroyed()) return;
                setLoading(false);
                if (response.code() == 401) {
                    sessionManager.logout();
                    redirectToAdminLogin();
                    return;
                }
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    showLoadError("The report could not be loaded. Error " + response.code());
                    return;
                }
                bindReport(response.body().get(0));
            }

            @Override
            public void onFailure(
                    Call<List<WasteReport>> call,
                    Throwable throwable
            ) {
                detailsCall = null;
                if (!call.isCanceled() && !isFinishing() && !isDestroyed()) {
                    setLoading(false);
                    showLoadError("Could not connect to the server.");
                }
            }
        });
    }

    private void bindReport(WasteReport report) {
        currentReport = report;
        Glide.with(this)
                .load(report.getImageUrl())
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(reportImage);

        String status = safeText(report.getStatus(), "Pending");
        statusText.setText(status);
        applyStatusStyle(statusText, status);
        reportIdText.setText(safeText(report.getId(), reportId));
        plasticTypeText.setText(safeText(report.getWasteType(), "Plastic type not provided"));
        weightText.setText(report.getEstimatedWeightKg() <= 0
                ? "Weight not provided"
                : String.format(Locale.getDefault(), "%.2f kg", report.getEstimatedWeightKg()));
        descriptionText.setText(safeText(report.getDescription(), "No description provided"));
        addressText.setText(safeText(report.getAddress(), "Address unavailable"));
        coordinatesText.setText(String.format(
                Locale.getDefault(),
                "Latitude: %.6f\nLongitude: %.6f",
                report.getLatitude(),
                report.getLongitude()
        ));
        dateText.setText(formatDate(report.getCreatedAt()));

        if (report.isPossibleDuplicate()) {
            duplicateWarningText.setVisibility(View.VISIBLE);
            duplicateWarningText.setText(
                    "Possible similar report detected within 100 metres and 30 minutes.\nRelated report: "
                            + safeText(report.getSimilarReportId(), "Unavailable")
            );
        } else {
            duplicateWarningText.setVisibility(View.GONE);
        }

        boolean pending = "Pending".equalsIgnoreCase(status);
        verifyButton.setVisibility(pending ? View.VISIBLE : View.GONE);
        rejectButton.setVisibility(pending ? View.VISIBLE : View.GONE);
        decisionMessageText.setVisibility(pending ? View.GONE : View.VISIBLE);
        if (!pending) {
            String message = "This report has already been " + status.toLowerCase(Locale.US) + ".";
            if ("Rejected".equalsIgnoreCase(status)) {
                message += "\nReason: " + safeText(report.getRejectionReason(), "No reason saved");
            }
            decisionMessageText.setText(message);
        }
    }

    private void confirmVerification() {
        if (currentReport == null || reviewCall != null) return;
        new AlertDialog.Builder(this)
                .setTitle("Verify this report?")
                .setMessage("The report will become visible to collectors as an available collection task.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Verify", (dialog, which) ->
                        submitReview(ReviewWasteReportRequest.verify(reportId), "Verified")
                )
                .show();
    }

    private void showRejectionDialog() {
        if (currentReport == null || reviewCall != null) return;
        EditText input = new EditText(this);
        input.setHint("Reason for rejection");
        input.setMinLines(3);
        input.setPadding(dpToPx(18), dpToPx(10), dpToPx(18), dpToPx(10));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Reject this report?")
                .setMessage("Give the citizen a clear reason so the report can be corrected.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reject", null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String reason = input.getText().toString().trim();
                    if (reason.length() < 5) {
                        input.setError("Enter a clear rejection reason");
                        return;
                    }
                    dialog.dismiss();
                    submitReview(
                            ReviewWasteReportRequest.reject(reportId, reason),
                            "Rejected"
                    );
                }));
        dialog.show();
    }

    private void submitReview(
            ReviewWasteReportRequest request,
            String resultStatus
    ) {
        setReviewLoading(true);
        AdminApi api = SupabaseClient.getClient().create(AdminApi.class);
        reviewCall = api.reviewReport(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                request
        );

        reviewCall.enqueue(new Callback<List<WasteReport>>() {
            @Override
            public void onResponse(
                    Call<List<WasteReport>> call,
                    Response<List<WasteReport>> response
            ) {
                reviewCall = null;
                if (isFinishing() || isDestroyed()) return;
                setReviewLoading(false);
                if (response.code() == 401) {
                    sessionManager.logout();
                    redirectToAdminLogin();
                    return;
                }
                if (!response.isSuccessful()) {
                    showReviewError("The report was not changed. Error " + response.code());
                    return;
                }
                if (response.body() == null || response.body().isEmpty()) {
                    showReviewError("The report may have already been reviewed by another administrator.");
                    return;
                }
                bindReport(response.body().get(0));
                showToast("Report " + resultStatus.toLowerCase(Locale.US) + " successfully");
            }

            @Override
            public void onFailure(
                    Call<List<WasteReport>> call,
                    Throwable throwable
            ) {
                reviewCall = null;
                if (!call.isCanceled() && !isFinishing() && !isDestroyed()) {
                    setReviewLoading(false);
                    showReviewError("Could not connect to the server. The report was not changed.");
                }
            }
        });
    }

    private void setReviewLoading(boolean loading) {
        verifyButton.setEnabled(!loading);
        rejectButton.setEnabled(!loading);
        verifyButton.setText(loading ? "UPDATING..." : "VERIFY REPORT");
        rejectButton.setText(loading ? "PLEASE WAIT" : "REJECT REPORT");
    }

    private void setLoading(boolean loading) {
        loadingProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        contentScroll.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void showLoadError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Report could not be loaded")
                .setMessage(message)
                .setNegativeButton("Close", (dialog, which) -> finish())
                .setPositiveButton("Try Again", (dialog, which) -> loadReport())
                .show();
    }

    private void showReviewError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Review not saved")
                .setMessage(message + " Make sure the Module 5 Phase 2 SQL has been run.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void applyStatusStyle(TextView view, String status) {
        int color;
        if ("Verified".equalsIgnoreCase(status)) {
            color = Color.parseColor("#4DA3FF");
        } else if ("Rejected".equalsIgnoreCase(status)) {
            color = Color.parseColor("#FF5C5C");
        } else {
            color = Color.parseColor("#FFC857");
        }
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.argb(38, Color.red(color), Color.green(color), Color.blue(color)));
        background.setStroke(dpToPx(1), color);
        background.setCornerRadius(dpToPx(50));
        view.setBackground(background);
        view.setTextColor(color);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String formatDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) return "Date unavailable";
        String part = rawDate.length() >= 19 ? rawDate.substring(0, 19) : rawDate;
        SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        SimpleDateFormat output = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        try {
            Date date = input.parse(part);
            return date == null ? rawDate : output.format(date);
        } catch (ParseException ignored) {
            return rawDate;
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void redirectToAdminLogin() {
        Intent intent = new Intent(this, AdminLoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        if (detailsCall != null) detailsCall.cancel();
        if (reviewCall != null) reviewCall.cancel();
        super.onDestroy();
    }
}
