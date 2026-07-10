package com.example.eco_print;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.eco_print.api.WasteReportApi;
import com.example.eco_print.models.StorageUploadResponse;
import com.example.eco_print.models.WasteReport;
import com.example.eco_print.utils.SessionManager;
import com.example.eco_print.utils.SupabaseClient;
import com.example.eco_print.utils.SupabaseConfig;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WasteReportActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 101;
    private static final int MAX_IMAGE_SIZE = 1280;

    private ImageButton backButton;
    private ImageView wasteImageView;
    private Button galleryButton;
    private Button cameraButton;
    private Button locationButton;
    private Button submitButton;
    private Spinner wasteTypeSpinner;
    private EditText weightEditText;
    private EditText descriptionEditText;
    private TextView locationStatusText;
    private TextView coordinatesText;
    private TextView addressText;
    private ProgressBar progressBar;

    private Uri selectedImageUri;
    private byte[] cameraImageBytes;

    private double selectedLatitude;
    private double selectedLongitude;
    private String selectedAddress = "";
    private boolean locationSelected = false;

    private LocationManager locationManager;
    private SessionManager sessionManager;

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            selectedImageUri = uri;
                            cameraImageBytes = null;
                            wasteImageView.setImageURI(uri);
                            wasteImageView.setScaleType(
                                    ImageView.ScaleType.CENTER_CROP
                            );
                        }
                    }
            );

    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicturePreview(),
                    bitmap -> {
                        if (bitmap != null) {
                            selectedImageUri = null;
                            cameraImageBytes =
                                    bitmapToJpegBytes(bitmap, 90);

                            wasteImageView.setImageBitmap(bitmap);
                            wasteImageView.setScaleType(
                                    ImageView.ScaleType.CENTER_CROP
                            );
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waste_report);

        sessionManager = new SessionManager(this);
        locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);

        bindViews();
        configureWasteTypeSpinner();
        configureClickListeners();
    }

    private void bindViews() {
        backButton = findViewById(R.id.backButton);
        wasteImageView = findViewById(R.id.wasteImageView);
        galleryButton = findViewById(R.id.galleryButton);
        cameraButton = findViewById(R.id.cameraButton);
        locationButton = findViewById(R.id.locationButton);
        submitButton = findViewById(R.id.submitButton);
        wasteTypeSpinner = findViewById(R.id.wasteTypeSpinner);
        weightEditText = findViewById(R.id.weightEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        locationStatusText = findViewById(R.id.locationStatusText);
        coordinatesText = findViewById(R.id.coordinatesText);
        addressText = findViewById(R.id.addressText);
        progressBar = findViewById(R.id.progressBar);
    }

    private void configureWasteTypeSpinner() {

        String[] wasteTypes = {
                "Select plastic type",
                "PET Bottles",
                "HDPE Containers",
                "PVC Plastic",
                "LDPE Bags and Film",
                "PP Containers",
                "PS / Thermocol",
                "Mixed Plastic Waste",
                "Other Plastic"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        wasteTypes
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        wasteTypeSpinner.setAdapter(adapter);
    }

    private void configureClickListeners() {

        backButton.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        galleryButton.setOnClickListener(v ->
                galleryLauncher.launch("image/*")
        );

        cameraButton.setOnClickListener(v ->
                cameraLauncher.launch(null)
        );

        locationButton.setOnClickListener(v ->
                obtainCurrentLocation()
        );

        submitButton.setOnClickListener(v ->
                validateAndSubmit()
        );
    }

    private void obtainCurrentLocation() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST
            );

            return;
        }

        boolean gpsEnabled =
                locationManager.isProviderEnabled(
                        LocationManager.GPS_PROVIDER
                );

        boolean networkEnabled =
                locationManager.isProviderEnabled(
                        LocationManager.NETWORK_PROVIDER
                );

        if (!gpsEnabled && !networkEnabled) {
            showLocationSettingsDialog();
            return;
        }

        locationButton.setEnabled(false);
        locationStatusText.setText("Getting your location...");

        Location lastLocation = null;

        if (gpsEnabled) {
            lastLocation = locationManager.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER
            );
        }

        if (lastLocation == null && networkEnabled) {
            lastLocation = locationManager.getLastKnownLocation(
                    LocationManager.NETWORK_PROVIDER
            );
        }

        if (lastLocation != null) {
            handleLocation(lastLocation);
        }

        String provider = gpsEnabled
                ? LocationManager.GPS_PROVIDER
                : LocationManager.NETWORK_PROVIDER;

        try {
            locationManager.requestSingleUpdate(
                    provider,
                    locationListener,
                    getMainLooper()
            );
        } catch (SecurityException exception) {
            locationButton.setEnabled(true);
            showToast("Location permission was not granted");
        }
    }

    private final LocationListener locationListener =
            new LocationListener() {
                @Override
                public void onLocationChanged(
                        @NonNull Location location
                ) {
                    handleLocation(location);
                }

                @Override
                public void onProviderDisabled(
                        @NonNull String provider
                ) {
                    locationButton.setEnabled(true);
                }

                @Override
                public void onProviderEnabled(
                        @NonNull String provider
                ) {
                    // No action required.
                }
            };

    private void handleLocation(Location location) {

        selectedLatitude = location.getLatitude();
        selectedLongitude = location.getLongitude();
        locationSelected = true;

        locationButton.setEnabled(true);
        locationStatusText.setText("Location captured");

        coordinatesText.setText(
                String.format(
                        Locale.getDefault(),
                        "Latitude: %.6f\nLongitude: %.6f",
                        selectedLatitude,
                        selectedLongitude
                )
        );

        reverseGeocodeLocation();
    }

    private void reverseGeocodeLocation() {

        Geocoder geocoder =
                new Geocoder(this, Locale.getDefault());

        if (!Geocoder.isPresent()) {
            selectedAddress = "Address unavailable";
            addressText.setText(selectedAddress);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(
                    selectedLatitude,
                    selectedLongitude,
                    1,
                    addresses -> applyAddress(addresses)
            );
        } else {
            new Thread(() -> {
                try {
                    List<Address> addresses =
                            geocoder.getFromLocation(
                                    selectedLatitude,
                                    selectedLongitude,
                                    1
                            );

                    runOnUiThread(() ->
                            applyAddress(addresses)
                    );
                } catch (Exception exception) {
                    runOnUiThread(() -> {
                        selectedAddress = "Address unavailable";
                        addressText.setText(selectedAddress);
                    });
                }
            }).start();
        }
    }

    private void applyAddress(List<Address> addresses) {

        if (addresses != null && !addresses.isEmpty()) {
            Address address = addresses.get(0);

            String fullAddress =
                    address.getAddressLine(0);

            if (fullAddress == null || fullAddress.trim().isEmpty()) {
                fullAddress = "Address unavailable";
            }

            selectedAddress = fullAddress;
        } else {
            selectedAddress = "Address unavailable";
        }

        addressText.setText(selectedAddress);
    }

    private void validateAndSubmit() {

        if (!sessionManager.isLoggedIn()) {
            showToast("Your session has expired. Please log in again.");
            redirectToLogin();
            return;
        }

        if (selectedImageUri == null
                && cameraImageBytes == null) {
            showToast("Please add a photo of the plastic waste");
            return;
        }

        if (wasteTypeSpinner.getSelectedItemPosition() == 0) {
            showToast("Please select the plastic type");
            return;
        }

        String weightText =
                weightEditText.getText().toString().trim();

        if (weightText.isEmpty()) {
            weightEditText.setError("Enter estimated weight");
            weightEditText.requestFocus();
            return;
        }

        double estimatedWeight;

        try {
            estimatedWeight = Double.parseDouble(weightText);
        } catch (NumberFormatException exception) {
            weightEditText.setError("Enter a valid weight");
            weightEditText.requestFocus();
            return;
        }

        if (estimatedWeight <= 0 || estimatedWeight > 10000) {
            weightEditText.setError(
                    "Weight must be between 0 and 10,000 kg"
            );
            weightEditText.requestFocus();
            return;
        }

        String description =
                descriptionEditText.getText().toString().trim();

        if (description.length() < 10) {
            descriptionEditText.setError(
                    "Enter at least 10 characters"
            );
            descriptionEditText.requestFocus();
            return;
        }

        if (!locationSelected) {
            showToast("Please capture the waste location");
            return;
        }

        byte[] imageBytes = prepareSelectedImage();

        if (imageBytes == null || imageBytes.length == 0) {
            showToast("Unable to read the selected image");
            return;
        }

        setLoading(true);

        uploadWasteImage(
                imageBytes,
                estimatedWeight,
                description
        );
    }

    private byte[] prepareSelectedImage() {

        if (cameraImageBytes != null) {
            return cameraImageBytes;
        }

        if (selectedImageUri == null) {
            return null;
        }

        try (
                InputStream inputStream =
                        getContentResolver()
                                .openInputStream(selectedImageUri)
        ) {
            Bitmap bitmap =
                    BitmapFactory.decodeStream(inputStream);

            if (bitmap == null) {
                return null;
            }

            Bitmap resizedBitmap =
                    resizeBitmap(bitmap, MAX_IMAGE_SIZE);

            byte[] result =
                    bitmapToJpegBytes(resizedBitmap, 85);

            if (resizedBitmap != bitmap) {
                resizedBitmap.recycle();
            }

            bitmap.recycle();
            return result;

        } catch (Exception exception) {
            return null;
        }
    }

    private Bitmap resizeBitmap(
            Bitmap bitmap,
            int maximumSize
    ) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maximumSize && height <= maximumSize) {
            return bitmap;
        }

        float scale =
                Math.min(
                        (float) maximumSize / width,
                        (float) maximumSize / height
                );

        int resizedWidth =
                Math.round(width * scale);

        int resizedHeight =
                Math.round(height * scale);

        return Bitmap.createScaledBitmap(
                bitmap,
                resizedWidth,
                resizedHeight,
                true
        );
    }

    private byte[] bitmapToJpegBytes(
            Bitmap bitmap,
            int quality
    ) {
        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                quality,
                outputStream
        );

        return outputStream.toByteArray();
    }

    private void uploadWasteImage(
            byte[] imageBytes,
            double estimatedWeight,
            String description
    ) {
        String userId = sessionManager.getUserId();

        String imagePath =
                userId
                        + "/"
                        + UUID.randomUUID()
                        + ".jpg";

        RequestBody imageBody =
                RequestBody.create(
                        imageBytes,
                        MediaType.parse("image/jpeg")
                );

        WasteReportApi api =
                SupabaseClient.getClient()
                        .create(WasteReportApi.class);

        api.uploadWasteImage(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "false",
                "image/jpeg",
                SupabaseConfig.WASTE_IMAGE_BUCKET,
                imagePath,
                imageBody
        ).enqueue(new Callback<StorageUploadResponse>() {

            @Override
            public void onResponse(
                    Call<StorageUploadResponse> call,
                    Response<StorageUploadResponse> response
            ) {
                if (!response.isSuccessful()) {
                    setLoading(false);

                    if (response.code() == 401) {
                        showToast(
                                "Your login session expired. Log in again."
                        );
                        redirectToLogin();
                    } else {
                        showToast(
                                "Image upload failed. Error code: "
                                        + response.code()
                        );
                    }

                    return;
                }

                String imageUrl =
                        SupabaseConfig.SUPABASE_URL
                                + "storage/v1/object/public/"
                                + SupabaseConfig.WASTE_IMAGE_BUCKET
                                + "/"
                                + imagePath;

                createWasteReport(
                        imageUrl,
                        estimatedWeight,
                        description
                );
            }

            @Override
            public void onFailure(
                    Call<StorageUploadResponse> call,
                    Throwable throwable
            ) {
                setLoading(false);

                showToast(
                        "Image upload failed: "
                                + throwable.getMessage()
                );
            }
        });
    }

    private void createWasteReport(
            String imageUrl,
            double estimatedWeight,
            String description
    ) {
        WasteReport report =
                new WasteReport(
                        sessionManager.getUserId(),
                        imageUrl,
                        wasteTypeSpinner
                                .getSelectedItem()
                                .toString(),
                        estimatedWeight,
                        description,
                        selectedLatitude,
                        selectedLongitude,
                        selectedAddress,
                        "Pending"
                );

        WasteReportApi api =
                SupabaseClient.getClient()
                        .create(WasteReportApi.class);

        api.createWasteReport(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                report
        ).enqueue(new Callback<List<WasteReport>>() {

            @Override
            public void onResponse(
                    Call<List<WasteReport>> call,
                    Response<List<WasteReport>> response
            ) {
                setLoading(false);

                if (response.isSuccessful()) {
                    showSuccessDialog();
                    return;
                }

                if (response.code() == 401) {
                    showToast(
                            "Your login session expired. Log in again."
                    );
                    redirectToLogin();
                } else {
                    showToast(
                            "Report submission failed. Error code: "
                                    + response.code()
                    );
                }
            }

            @Override
            public void onFailure(
                    Call<List<WasteReport>> call,
                    Throwable throwable
            ) {
                setLoading(false);

                showToast(
                        "Submission failed: "
                                + throwable.getMessage()
                );
            }
        });
    }

    private void showSuccessDialog() {

        new AlertDialog.Builder(this)
                .setTitle("Report submitted")
                .setMessage(
                        "Your plastic waste report has been saved "
                                + "with Pending status."
                )
                .setCancelable(false)
                .setPositiveButton(
                        "Back to Dashboard",
                        (dialog, which) -> {
                            Intent intent = new Intent(
                                    WasteReportActivity.this,
                                    HomeActivity.class
                            );

                            intent.addFlags(
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            );

                            startActivity(intent);
                            finish();
                        }
                )
                .setNegativeButton(
                        "Submit Another",
                        (dialog, which) ->
                                resetForm()
                )
                .show();
    }

    private void resetForm() {

        selectedImageUri = null;
        cameraImageBytes = null;
        locationSelected = false;
        selectedLatitude = 0;
        selectedLongitude = 0;
        selectedAddress = "";

        wasteImageView.setImageResource(
                android.R.drawable.ic_menu_camera
        );

        wasteImageView.setScaleType(
                ImageView.ScaleType.CENTER
        );

        wasteTypeSpinner.setSelection(0);
        weightEditText.setText("");
        descriptionEditText.setText("");
        locationStatusText.setText(
                "No location selected"
        );
        coordinatesText.setText(
                "Latitude: --\nLongitude: --"
        );
        addressText.setText(
                "Address will appear here"
        );
    }

    private void setLoading(boolean loading) {

        progressBar.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        submitButton.setEnabled(!loading);
        galleryButton.setEnabled(!loading);
        cameraButton.setEnabled(!loading);
        locationButton.setEnabled(!loading);

        submitButton.setText(
                loading
                        ? "SUBMITTING REPORT..."
                        : "SUBMIT REPORT"
        );
    }

    private void showLocationSettingsDialog() {

        new AlertDialog.Builder(this)
                .setTitle("Turn on location")
                .setMessage(
                        "Location services are required "
                                + "to attach the waste location."
                )
                .setPositiveButton(
                        "Open Settings",
                        (dialog, which) ->
                                startActivity(
                                        new Intent(
                                                Settings.ACTION_LOCATION_SOURCE_SETTINGS
                                        )
                                )
                )
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void redirectToLogin() {

        sessionManager.logout();

        Intent intent = new Intent(
                this,
                LoginActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
    }

    private void showToast(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == LOCATION_PERMISSION_REQUEST) {

            boolean granted =
                    grantResults.length > 0
                            && grantResults[0]
                            == PackageManager.PERMISSION_GRANTED;

            if (granted) {
                obtainCurrentLocation();
            } else {
                showToast(
                        "Location permission is required "
                                + "to submit a waste report"
                );
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            locationManager.removeUpdates(locationListener);
        } catch (Exception ignored) {
            // No active location request.
        }
    }
}
