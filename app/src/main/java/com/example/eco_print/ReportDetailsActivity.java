package com.example.eco_print;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.eco_print.api.WasteReportApi;
import com.example.eco_print.models.WasteReport;
import com.example.eco_print.utils.SessionManager;
import com.example.eco_print.utils.SupabaseClient;
import com.example.eco_print.utils.SupabaseConfig;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_REPORT_ID = "report_id";

    private ProgressBar detailProgress;
    private ScrollView detailsScroll;
    private ImageView reportImage;
    private TextView statusText;
    private TextView plasticTypeText;
    private TextView weightText;
    private TextView dateText;
    private TextView descriptionText;
    private TextView addressText;
    private TextView coordinatesText;
    private TextView reportIdText;
    private TextView rejectedNotice;

    private TextView stepDot1;
    private TextView stepDot2;
    private TextView stepDot3;
    private TextView stepDot4;
    private TextView stepDot5;

    private TextView stepLabel1;
    private TextView stepLabel2;
    private TextView stepLabel3;
    private TextView stepLabel4;
    private TextView stepLabel5;

    private View line1;
    private View line2;
    private View line3;
    private View line4;

    private SessionManager sessionManager;
    private String reportId;
    private Call<List<WasteReport>> detailsCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_details);

        sessionManager = new SessionManager(this);
        reportId = getIntent().getStringExtra(EXTRA_REPORT_ID);

        bindViews();

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        if (reportId == null || reportId.trim().isEmpty()) {
            showToast("Report ID is missing");
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (reportId != null && !reportId.trim().isEmpty()) {
            loadReportDetails();
        }
    }

    private void bindViews() {
        detailProgress = findViewById(R.id.detailProgress);
        detailsScroll = findViewById(R.id.detailsScroll);
        reportImage = findViewById(R.id.reportImage);
        statusText = findViewById(R.id.statusText);
        plasticTypeText = findViewById(R.id.plasticTypeText);
        weightText = findViewById(R.id.weightText);
        dateText = findViewById(R.id.dateText);
        descriptionText = findViewById(R.id.descriptionText);
        addressText = findViewById(R.id.addressText);
        coordinatesText = findViewById(R.id.coordinatesText);
        reportIdText = findViewById(R.id.reportIdText);
        rejectedNotice = findViewById(R.id.rejectedNotice);

        stepDot1 = findViewById(R.id.stepDot1);
        stepDot2 = findViewById(R.id.stepDot2);
        stepDot3 = findViewById(R.id.stepDot3);
        stepDot4 = findViewById(R.id.stepDot4);
        stepDot5 = findViewById(R.id.stepDot5);

        stepLabel1 = findViewById(R.id.stepLabel1);
        stepLabel2 = findViewById(R.id.stepLabel2);
        stepLabel3 = findViewById(R.id.stepLabel3);
        stepLabel4 = findViewById(R.id.stepLabel4);
        stepLabel5 = findViewById(R.id.stepLabel5);

        line1 = findViewById(R.id.line1);
        line2 = findViewById(R.id.line2);
        line3 = findViewById(R.id.line3);
        line4 = findViewById(R.id.line4);
    }

    private void loadReportDetails() {
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        if (!SupabaseConfig.isConfigured()) {
            showLoadError(
                    "Supabase is not configured. Add the anon key and try again."
            );
            return;
        }

        if (detailsCall != null) {
            detailsCall.cancel();
        }

        setLoading(true);

        WasteReportApi api = SupabaseClient.getClient()
                .create(WasteReportApi.class);

        detailsCall = api.getWasteReportById(
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

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                setLoading(false);

                if (response.code() == 401) {
                    showToast("Your session expired. Please log in again.");
                    redirectToLogin();
                    return;
                }

                if (!response.isSuccessful() || response.body() == null) {
                    showLoadError(
                            "Unable to load this report. Error "
                                    + response.code()
                    );
                    return;
                }

                if (response.body().isEmpty()) {
                    showToast("This report could not be found");
                    finish();
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

                if (call.isCanceled()
                        || isFinishing()
                        || isDestroyed()) {
                    return;
                }

                setLoading(false);
                showLoadError(
                        "Could not connect to the server. Check your internet and try again."
                );
            }
        });
    }

    private void bindReport(WasteReport report) {
        Glide.with(this)
                .load(report.getImageUrl())
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(reportImage);

        String status = safeText(report.getStatus(), "Pending");

        statusText.setText(status);
        plasticTypeText.setText(
                formatWasteType(report.getWasteType())
        );
        weightText.setText(
                formatWeight(report.getEstimatedWeightKg())
        );
        dateText.setText(formatDate(report.getCreatedAt()));
        descriptionText.setText(
                safeText(report.getDescription(), "No description provided")
        );
        addressText.setText(
                safeText(report.getAddress(), "Address unavailable")
        );
        coordinatesText.setText(String.format(
                Locale.getDefault(),
                "Latitude: %.6f\nLongitude: %.6f",
                report.getLatitude(),
                report.getLongitude()
        ));
        reportIdText.setText(safeText(report.getId(), reportId));

        applyStatusStyle(statusText, status);
        updateStatusTracker(status);
    }

    private void updateStatusTracker(String status) {
        String normalized = status == null
                ? ""
                : status.trim().toLowerCase(Locale.US);

        boolean rejected = normalized.equals("rejected");
        int progressIndex;

        switch (normalized) {
            case "verified":
                progressIndex = 1;
                break;
            case "assigned":
                progressIndex = 2;
                break;
            case "in progress":
                progressIndex = 3;
                break;
            case "collected":
                progressIndex = 4;
                break;
            case "rejected":
            case "pending":
            case "reported":
            default:
                progressIndex = 0;
                break;
        }

        TextView[] dots = {
                stepDot1, stepDot2, stepDot3, stepDot4, stepDot5
        };
        TextView[] labels = {
                stepLabel1, stepLabel2, stepLabel3, stepLabel4, stepLabel5
        };
        View[] lines = {line1, line2, line3, line4};

        int completedColor = Color.parseColor("#7CFF5B");
        int futureColor = Color.parseColor("#555555");
        int futureTextColor = Color.parseColor("#888888");

        for (int i = 0; i < dots.length; i++) {
            boolean completed = i <= progressIndex;
            int dotColor = completed ? completedColor : futureColor;

            dots[i].setText(completed ? "✓" : String.valueOf(i + 1));
            dots[i].setTextColor(completed ? Color.BLACK : Color.WHITE);
            setCircleBackground(dots[i], dotColor);
            labels[i].setTextColor(
                    completed ? Color.WHITE : futureTextColor
            );
        }

        for (int i = 0; i < lines.length; i++) {
            lines[i].setBackgroundColor(
                    i < progressIndex ? completedColor : futureColor
            );
        }

        rejectedNotice.setVisibility(
                rejected ? View.VISIBLE : View.GONE
        );

        if (rejected) {
            int rejectedColor = Color.parseColor("#FF5C5C");
            stepLabel1.setText("Report Submitted — Rejected");
            stepLabel1.setTextColor(rejectedColor);
            setCircleBackground(stepDot1, rejectedColor);
            stepDot1.setText("!");
            stepDot1.setTextColor(Color.WHITE);
            line1.setBackgroundColor(futureColor);
        } else {
            stepLabel1.setText("Report Submitted");
        }
    }

    private void applyStatusStyle(TextView view, String status) {
        int color = getStatusColor(status);

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

    private void setCircleBackground(TextView view, int color) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(color);
        view.setBackground(background);
    }

    private int getStatusColor(String status) {
        String value = status == null
                ? ""
                : status.trim().toLowerCase(Locale.US);

        switch (value) {
            case "verified":
                return Color.parseColor("#4DA3FF");
            case "assigned":
                return Color.parseColor("#B388FF");
            case "in progress":
                return Color.parseColor("#FF9F43");
            case "collected":
                return Color.parseColor("#7CFF5B");
            case "rejected":
                return Color.parseColor("#FF5C5C");
            case "pending":
            case "reported":
            default:
                return Color.parseColor("#FFC857");
        }
    }


    private String formatWasteType(String wasteType) {
        if (wasteType == null
                || wasteType.trim().isEmpty()
                || wasteType.equalsIgnoreCase("Not specified")) {
            return "Plastic type not provided";
        }
        return wasteType;
    }

    private String formatWeight(double weight) {
        if (weight <= 0) {
            return "Weight not provided";
        }
        return String.format(
                Locale.getDefault(),
                "%.2f kg",
                weight
        );
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty()
                ? fallback
                : value;
    }

    private String formatDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return "Date unavailable";
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
            if (date != null) {
                return output.format(date);
            }
        } catch (ParseException ignored) {
        }

        return rawDate;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources()
                .getDisplayMetrics().density);
    }

    private void setLoading(boolean loading) {
        detailProgress.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );
        detailsScroll.setVisibility(
                loading ? View.GONE : View.VISIBLE
        );
    }


    private void showLoadError(String message) {
        setLoading(false);

        new AlertDialog.Builder(this)
                .setTitle("Report could not be loaded")
                .setMessage(message)
                .setNegativeButton(
                        "Close",
                        (dialog, which) -> finish()
                )
                .setPositiveButton(
                        "Try Again",
                        (dialog, which) -> loadReportDetails()
                )
                .show();
    }

    private void redirectToLogin() {
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
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
        super.onDestroy();
    }
}
