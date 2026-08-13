package com.example.eco_print;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
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
import com.example.eco_print.models.UpdateCollectionStatusRequest;
import com.example.eco_print.models.WasteReport;
import com.example.eco_print.utils.RoleNavigator;
import com.example.eco_print.utils.SessionManager;
import com.example.eco_print.utils.SupabaseClient;
import com.example.eco_print.utils.SupabaseConfig;
import com.google.android.material.button.MaterialButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CollectorTaskDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_REPORT_ID = "collector_report_id";

    private ProgressBar detailProgress;
    private ScrollView detailsScroll;
    private ImageView reportImage;
    private TextView statusText;
    private TextView reportIdText;
    private TextView plasticTypeText;
    private TextView weightText;
    private TextView descriptionText;
    private TextView addressText;
    private TextView coordinatesText;
    private TextView assignedAtText;
    private TextView startedAtText;
    private TextView collectedAtText;
    private TextView completedMessageText;
    private MapView mapView;
    private MaterialButton openMapButton;
    private MaterialButton statusActionButton;

    private SessionManager sessionManager;
    private String reportId;
    private WasteReport currentReport;

    private Call<List<WasteReport>> detailsCall;
    private Call<List<WasteReport>> statusUpdateCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureOpenStreetMapCache();
        setContentView(R.layout.activity_collector_task_details);

        sessionManager = new SessionManager(this);
        reportId = getIntent().getStringExtra(EXTRA_REPORT_ID);

        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        if (!sessionManager.isApprovedCollector()) {
            RoleNavigator.openCorrectHome(this, sessionManager);
            return;
        }

        bindViews();
        configureMap();
        configureActions();

        if (reportId == null || reportId.trim().isEmpty()) {
            showToast("Report ID is missing");
            finish();
        }
    }

    private void configureOpenStreetMapCache() {
        IConfigurationProvider configuration =
                Configuration.getInstance();
        configuration.setUserAgentValue(
                "EcoPrint/1.0 (" + getPackageName() + ")"
        );

        File basePath = new File(getCacheDir(), "osmdroid");
        File tilePath = new File(basePath, "tiles");
        if (!basePath.exists()) {
            basePath.mkdirs();
        }
        if (!tilePath.exists()) {
            tilePath.mkdirs();
        }

        configuration.setOsmdroidBasePath(basePath);
        configuration.setOsmdroidTileCache(tilePath);
    }

    private void bindViews() {
        detailProgress = findViewById(R.id.detailProgress);
        detailsScroll = findViewById(R.id.detailsScroll);
        reportImage = findViewById(R.id.reportImage);
        statusText = findViewById(R.id.statusText);
        reportIdText = findViewById(R.id.reportIdText);
        plasticTypeText = findViewById(R.id.plasticTypeText);
        weightText = findViewById(R.id.weightText);
        descriptionText = findViewById(R.id.descriptionText);
        addressText = findViewById(R.id.addressText);
        coordinatesText = findViewById(R.id.coordinatesText);
        assignedAtText = findViewById(R.id.assignedAtText);
        startedAtText = findViewById(R.id.startedAtText);
        collectedAtText = findViewById(R.id.collectedAtText);
        completedMessageText = findViewById(R.id.completedMessageText);
        mapView = findViewById(R.id.mapView);
        openMapButton = findViewById(R.id.openMapButton);
        statusActionButton = findViewById(R.id.statusActionButton);
    }

    private void configureMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);
        mapView.getController().setZoom(17.0);
    }

    private void configureActions() {
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        openMapButton.setOnClickListener(v -> openInOpenStreetMap());
        statusActionButton.setOnClickListener(v ->
                showStatusConfirmation()
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }

        if (reportId != null && !reportId.trim().isEmpty()) {
            loadReportDetails();
        }
    }

    @Override
    protected void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    private void loadReportDetails() {
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
                            "Unable to load this collection. Error "
                                    + response.code()
                    );
                    return;
                }

                if (response.body().isEmpty()) {
                    showToast("This collection could not be found");
                    finish();
                    return;
                }

                WasteReport report = response.body().get(0);
                if (!sessionManager.getUserId().equals(report.getCollectorId())) {
                    showToast("This collection is not assigned to your account");
                    finish();
                    return;
                }

                bindReport(report);
            }

            @Override
            public void onFailure(
                    Call<List<WasteReport>> call,
                    Throwable throwable
            ) {
                detailsCall = null;
                if (call.isCanceled() || isFinishing() || isDestroyed()) {
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
        currentReport = report;

        Glide.with(this)
                .load(report.getImageUrl())
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(reportImage);

        String status = safeText(report.getStatus(), "Assigned");
        statusText.setText(status);
        applyStatusStyle(statusText, status);

        reportIdText.setText(safeText(report.getId(), reportId));
        plasticTypeText.setText(formatWasteType(report.getWasteType()));
        weightText.setText(formatWeight(report.getEstimatedWeightKg()));
        descriptionText.setText(
                safeText(report.getDescription(), "No description provided")
        );
        addressText.setText(
                safeText(report.getAddress(), "Address unavailable")
        );
        coordinatesText.setText(String.format(
                Locale.getDefault(),
                "Latitude: %.6f  •  Longitude: %.6f",
                report.getLatitude(),
                report.getLongitude()
        ));

        assignedAtText.setText(
                "Assigned: " + formatDate(report.getAssignedAt())
        );
        startedAtText.setText(
                "Started: " + formatDate(report.getStartedAt())
        );
        collectedAtText.setText(
                "Collected: " + formatDate(report.getCollectedAt())
        );

        showWasteLocation(report);
        updateActionForStatus(status);
    }

    private void showWasteLocation(WasteReport report) {
        double latitude = report.getLatitude();
        double longitude = report.getLongitude();

        if (Math.abs(latitude) > 90
                || Math.abs(longitude) > 180
                || (latitude == 0 && longitude == 0)) {
            mapView.setVisibility(View.GONE);
            openMapButton.setVisibility(View.GONE);
            coordinatesText.setText("Map coordinates are unavailable");
            return;
        }

        mapView.setVisibility(View.VISIBLE);
        openMapButton.setVisibility(View.VISIBLE);
        mapView.getOverlays().clear();

        GeoPoint wastePoint = new GeoPoint(latitude, longitude);
        Marker marker = new Marker(mapView);
        marker.setPosition(wastePoint);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle("Plastic Waste Location");
        marker.setSnippet(safeText(report.getAddress(), "Reported location"));
        mapView.getOverlays().add(marker);
        mapView.getController().setCenter(wastePoint);
        mapView.invalidate();
    }

    private void updateActionForStatus(String status) {
        String normalized = status == null
                ? ""
                : status.trim().toLowerCase(Locale.US);

        completedMessageText.setVisibility(View.GONE);
        statusActionButton.setVisibility(View.VISIBLE);
        statusActionButton.setEnabled(true);

        switch (normalized) {
            case "assigned":
                statusActionButton.setText("START COLLECTION");
                break;
            case "in progress":
                statusActionButton.setText("MARK AS COLLECTED");
                break;
            case "collected":
                statusActionButton.setVisibility(View.GONE);
                completedMessageText.setVisibility(View.VISIBLE);
                break;
            default:
                statusActionButton.setVisibility(View.GONE);
                break;
        }
    }

    private void showStatusConfirmation() {
        if (currentReport == null || statusUpdateCall != null) {
            return;
        }

        String currentStatus = safeText(
                currentReport.getStatus(),
                "Assigned"
        );
        String nextStatus;
        String title;
        String message;

        if ("Assigned".equalsIgnoreCase(currentStatus)) {
            nextStatus = "In Progress";
            title = "Start Collection?";
            message = "This confirms that you have started travelling to or collecting the reported plastic waste.";
        } else if ("In Progress".equalsIgnoreCase(currentStatus)) {
            nextStatus = "Collected";
            title = "Complete Collection?";
            message = "Mark this task as Collected only after the plastic waste has been picked up.";
        } else {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(
                        "Confirm",
                        (dialog, which) -> updateCollectionStatus(nextStatus)
                )
                .show();
    }

    private void updateCollectionStatus(String newStatus) {
        if (currentReport == null
                || currentReport.getId() == null
                || statusUpdateCall != null) {
            return;
        }

        statusActionButton.setEnabled(false);
        statusActionButton.setText("UPDATING...");

        WasteReportApi api = SupabaseClient.getClient()
                .create(WasteReportApi.class);

        statusUpdateCall = api.updateCollectionStatus(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                new UpdateCollectionStatusRequest(
                        currentReport.getId(),
                        newStatus
                )
        );

        statusUpdateCall.enqueue(new Callback<List<WasteReport>>() {
            @Override
            public void onResponse(
                    Call<List<WasteReport>> call,
                    Response<List<WasteReport>> response
            ) {
                statusUpdateCall = null;
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (response.code() == 401) {
                    showToast("Your session expired. Please log in again.");
                    redirectToLogin();
                    return;
                }

                if (!response.isSuccessful()
                        || response.body() == null
                        || response.body().isEmpty()) {
                    restoreActionButton();
                    showUpdateError(
                            "The collection status could not be changed. "
                                    + "Make sure the Supabase Module 4 SQL has been applied. "
                                    + "Error " + response.code()
                    );
                    return;
                }

                bindReport(response.body().get(0));
                showToast("Collection updated to " + newStatus);
            }

            @Override
            public void onFailure(
                    Call<List<WasteReport>> call,
                    Throwable throwable
            ) {
                statusUpdateCall = null;
                if (call.isCanceled() || isFinishing() || isDestroyed()) {
                    return;
                }

                restoreActionButton();
                showUpdateError(
                        "Could not connect to the server. Your current collection status has not been changed."
                );
            }
        });
    }

    private void restoreActionButton() {
        if (currentReport != null) {
            updateActionForStatus(currentReport.getStatus());
        }
    }

    private void openInOpenStreetMap() {
        if (currentReport == null) {
            return;
        }

        double latitude = currentReport.getLatitude();
        double longitude = currentReport.getLongitude();
        String url = String.format(
                Locale.US,
                "https://www.openstreetmap.org/?mlat=%.6f&mlon=%.6f#map=18/%.6f/%.6f",
                latitude,
                longitude,
                latitude,
                longitude
        );

        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException exception) {
            showToast("No browser is available to open OpenStreetMap");
        }
    }

    private void applyStatusStyle(TextView view, String status) {
        int color;
        String value = status == null
                ? ""
                : status.trim().toLowerCase(Locale.US);

        switch (value) {
            case "in progress":
                color = Color.parseColor("#FF9F43");
                break;
            case "collected":
                color = Color.parseColor("#7CFF5B");
                break;
            case "assigned":
            default:
                color = Color.parseColor("#B388FF");
                break;
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
            return "Estimated weight not provided";
        }
        return String.format(
                Locale.getDefault(),
                "Estimated weight: %.2f kg",
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
            return "—";
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
                .setTitle("Collection could not be loaded")
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

    private void showUpdateError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Status not updated")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void redirectToLogin() {
        if (sessionManager != null) {
            sessionManager.logout();
        }
        Intent intent = new Intent(this, LoginActivity.class);
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
        if (statusUpdateCall != null) {
            statusUpdateCall.cancel();
            statusUpdateCall = null;
        }
        if (mapView != null) {
            mapView.onDetach();
        }
        super.onDestroy();
    }
}
