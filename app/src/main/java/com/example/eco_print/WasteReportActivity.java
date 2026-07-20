package com.example.eco_print;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.eco_print.api.WasteReportApi;
import com.example.eco_print.models.StorageUploadResponse;
import com.example.eco_print.models.WasteReport;
import com.example.eco_print.utils.SessionManager;
import com.example.eco_print.utils.SupabaseClient;
import com.example.eco_print.utils.SupabaseConfig;
import com.example.eco_print.utils.WasteReportValidator;
import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WasteReportActivity extends AppCompatActivity {

    private static final int MAX_IMAGE_DIMENSION = 1280;
    private static final int MAX_UPLOAD_BYTES = 2_500_000;
    private static final long LOCATION_TIMEOUT_MS = 15_000L;
    private static final long MAX_LAST_LOCATION_AGE_MS = 10 * 60 * 1000L;

    private static final String STATE_IMAGE_URI = "state_image_uri";
    private static final String STATE_PENDING_CAMERA_URI = "state_pending_camera_uri";
    private static final String STATE_LATITUDE = "state_latitude";
    private static final String STATE_LONGITUDE = "state_longitude";
    private static final String STATE_ADDRESS = "state_address";
    private static final String STATE_LOCATION_SELECTED = "state_location_selected";
    private static final String STATE_WASTE_TYPE_POSITION = "state_waste_type_position";
    private static final String STATE_UPLOADED_IMAGE_URL = "state_uploaded_image_url";
    private static final String STATE_UPLOADED_IMAGE_PATH = "state_uploaded_image_path";

    private ImageButton backButton;
    private ImageView wasteImageView;
    private MaterialButton galleryButton;
    private MaterialButton cameraButton;
    private MaterialButton removeImageButton;
    private MaterialButton locationButton;
    private MaterialButton submitButton;
    private Spinner wasteTypeSpinner;
    private EditText weightEditText;
    private EditText descriptionEditText;
    private TextView locationStatusText;
    private TextView coordinatesText;
    private TextView addressText;
    private TextView loadingStatusText;
    private View imageEmptyState;
    private View imageSelectedBadge;
    private View loadingContainer;

    private Uri selectedImageUri;
    private Uri pendingCameraUri;

    private double selectedLatitude;
    private double selectedLongitude;
    private String selectedAddress = "";
    private boolean locationSelected;
    private boolean isFindingLocation;
    private boolean isSubmitting;

    /**
     * If a photo upload succeeds but the database request fails, these values
     * let a retry save the report without uploading the same photo again.
     */
    private String uploadedImageUrl = "";
    private String uploadedImagePath = "";

    private LocationManager locationManager;
    private SessionManager sessionManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Call<StorageUploadResponse> uploadCall;
    private Call<List<WasteReport>> createReportCall;

    private final Runnable locationTimeoutRunnable = () -> {
        if (!isFindingLocation) {
            return;
        }

        stopLocationUpdates();
        resetLocationButton();
        locationStatusText.setText(
                locationSelected
                        ? "Using the last available location"
                        : "Could not get the current location"
        );
        locationStatusText.setTextColor(Color.parseColor("#FFC857"));

        if (!locationSelected) {
            showErrorDialog(
                    "Location took too long",
                    "Move near a window or an open area, make sure location is turned on, and try again."
            );
        }
    };

    private final ActivityResultLauncher<String[]> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    uri -> {
                        if (uri == null) {
                            return;
                        }

                        persistGalleryPermission(uri);
                        showSelectedImage(uri, true);
                    }
            );

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    photoSaved -> {
                        if (photoSaved && pendingCameraUri != null) {
                            showSelectedImage(pendingCameraUri, true);
                        }
                        pendingCameraUri = null;
                    }
            );

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    this::handleLocationPermissionResult
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waste_report);

        sessionManager = new SessionManager(this);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        bindViews();
        configureWasteTypeSpinner();
        configureClickListeners();
        configureBackHandling();

        if (savedInstanceState == null) {
            clearSelectedImage();
        } else {
            restoreState(savedInstanceState);
        }
    }

    private void bindViews() {
        backButton = findViewById(R.id.backButton);
        wasteImageView = findViewById(R.id.wasteImageView);
        galleryButton = findViewById(R.id.galleryButton);
        cameraButton = findViewById(R.id.cameraButton);
        removeImageButton = findViewById(R.id.removeImageButton);
        locationButton = findViewById(R.id.locationButton);
        submitButton = findViewById(R.id.submitButton);
        wasteTypeSpinner = findViewById(R.id.wasteTypeSpinner);
        weightEditText = findViewById(R.id.weightEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        locationStatusText = findViewById(R.id.locationStatusText);
        coordinatesText = findViewById(R.id.coordinatesText);
        addressText = findViewById(R.id.addressText);
        loadingContainer = findViewById(R.id.loadingContainer);
        loadingStatusText = findViewById(R.id.loadingStatusText);
        imageEmptyState = findViewById(R.id.imageEmptyState);
        imageSelectedBadge = findViewById(R.id.imageSelectedBadge);
    }

    private void configureWasteTypeSpinner() {
        String[] wasteTypes = {
                "Select plastic type (optional)",
                "PET Bottles",
                "HDPE Containers",
                "PVC Plastic",
                "LDPE Bags and Film",
                "PP Containers",
                "PS / Thermocol",
                "Mixed Plastic Waste",
                "Other Plastic"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                wasteTypes
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        wasteTypeSpinner.setAdapter(adapter);
    }

    private void configureClickListeners() {
        backButton.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        galleryButton.setOnClickListener(v ->
                galleryLauncher.launch(new String[]{"image/*"})
        );

        cameraButton.setOnClickListener(v -> openCamera());
        removeImageButton.setOnClickListener(v -> clearSelectedImage());
        locationButton.setOnClickListener(v -> obtainCurrentLocation());
        submitButton.setOnClickListener(v -> validateAndSubmit());
    }

    private void configureBackHandling() {
        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (isSubmitting) {
                            showToast(
                                    "Please wait while your report is being submitted"
                            );
                            return;
                        }
                        finish();
                    }
                }
        );
    }

    private void openCamera() {
        if (isSubmitting) {
            return;
        }

        try {
            File imageDirectory = new File(getCacheDir(), "waste_images");

            if (!imageDirectory.exists() && !imageDirectory.mkdirs()) {
                showErrorDialog(
                        "Camera unavailable",
                        "The app could not prepare a temporary photo file. Please use the gallery instead."
                );
                return;
            }

            File imageFile = File.createTempFile(
                    "waste_",
                    ".jpg",
                    imageDirectory
            );

            pendingCameraUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    imageFile
            );

            cameraLauncher.launch(pendingCameraUri);
        } catch (Exception exception) {
            pendingCameraUri = null;
            showErrorDialog(
                    "Camera unavailable",
                    "The camera could not be opened. You can still choose a photo from the gallery."
            );
        }
    }

    private void persistGalleryPermission(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (SecurityException ignored) {
            // Some document providers only grant access for the current app
            // session. The image can still be used immediately.
        }
    }

    private void showSelectedImage(
            Uri imageUri,
            boolean newSelection
    ) {
        selectedImageUri = imageUri;

        if (newSelection) {
            clearUploadedImageReference();
        }

        Glide.with(this)
                .load(imageUri)
                .centerCrop()
                .into(wasteImageView);

        imageEmptyState.setVisibility(View.GONE);
        imageSelectedBadge.setVisibility(View.VISIBLE);
        removeImageButton.setVisibility(View.VISIBLE);
    }

    private void clearSelectedImage() {
        selectedImageUri = null;
        pendingCameraUri = null;
        clearUploadedImageReference();

        if (wasteImageView != null) {
            Glide.with(this).clear(wasteImageView);
            wasteImageView.setImageDrawable(null);
        }

        if (imageEmptyState != null) {
            imageEmptyState.setVisibility(View.VISIBLE);
        }

        if (imageSelectedBadge != null) {
            imageSelectedBadge.setVisibility(View.GONE);
        }

        if (removeImageButton != null) {
            removeImageButton.setVisibility(View.GONE);
        }
    }

    private void clearUploadedImageReference() {
        uploadedImageUrl = "";
        uploadedImagePath = "";
    }

    private void obtainCurrentLocation() {
        if (locationManager == null) {
            showErrorDialog(
                    "Location unavailable",
                    "This device could not start the location service."
            );
            return;
        }

        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }

        requestCurrentLocation();
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void handleLocationPermissionResult(
            Map<String, Boolean> result
    ) {
        boolean granted = Boolean.TRUE.equals(
                result.get(Manifest.permission.ACCESS_FINE_LOCATION)
        ) || Boolean.TRUE.equals(
                result.get(Manifest.permission.ACCESS_COARSE_LOCATION)
        );

        if (granted) {
            requestCurrentLocation();
            return;
        }

        boolean canExplain = ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) || ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        );

        if (canExplain) {
            new AlertDialog.Builder(this)
                    .setTitle("Location permission needed")
                    .setMessage(
                            "The collector needs the location of the waste. Allow location access and try again."
                    )
                    .setNegativeButton("Not Now", null)
                    .setPositiveButton(
                            "Try Again",
                            (dialog, which) -> obtainCurrentLocation()
                    )
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Allow location in Settings")
                    .setMessage(
                            "Location permission is disabled for Eco-Print. Open the app settings and allow location access."
                    )
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton(
                            "Open Settings",
                            (dialog, which) -> openAppSettings()
                    )
                    .show();
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void requestCurrentLocation() {
        if (!hasLocationPermission() || locationManager == null) {
            return;
        }

        boolean gpsEnabled = locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
        );
        boolean networkEnabled = locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );

        if (!gpsEnabled && !networkEnabled) {
            showLocationSettingsDialog();
            return;
        }

        isFindingLocation = true;
        locationButton.setEnabled(false);
        locationButton.setText("GETTING LOCATION...");
        locationStatusText.setText("Finding your current location...");
        locationStatusText.setTextColor(Color.parseColor("#FFC857"));

        Location lastLocation = findBestLastKnownLocation(
                gpsEnabled,
                networkEnabled
        );

        if (lastLocation != null && isFreshEnough(lastLocation)) {
            handleLocation(lastLocation);
            return;
        }

        mainHandler.removeCallbacks(locationTimeoutRunnable);
        mainHandler.postDelayed(
                locationTimeoutRunnable,
                LOCATION_TIMEOUT_MS
        );

        try {
            if (networkEnabled) {
                locationManager.requestSingleUpdate(
                        LocationManager.NETWORK_PROVIDER,
                        locationListener,
                        getMainLooper()
                );
            }

            if (gpsEnabled) {
                locationManager.requestSingleUpdate(
                        LocationManager.GPS_PROVIDER,
                        locationListener,
                        getMainLooper()
                );
            }
        } catch (SecurityException exception) {
            stopLocationUpdates();
            resetLocationButton();
            showErrorDialog(
                    "Location permission needed",
                    "Please allow location access so the collector can find the reported waste."
            );
        } catch (IllegalArgumentException exception) {
            stopLocationUpdates();
            resetLocationButton();
            showErrorDialog(
                    "Location unavailable",
                    "The device could not start a location request. Please try again."
            );
        }
    }

    private Location findBestLastKnownLocation(
            boolean gpsEnabled,
            boolean networkEnabled
    ) {
        Location best = null;

        try {
            if (networkEnabled) {
                best = locationManager.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER
                );
            }

            if (gpsEnabled) {
                Location gpsLocation = locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                );

                if (gpsLocation != null
                        && (best == null
                        || gpsLocation.getTime() > best.getTime())) {
                    best = gpsLocation;
                }
            }
        } catch (SecurityException ignored) {
            return null;
        }

        return best;
    }

    private boolean isFreshEnough(Location location) {
        long age = System.currentTimeMillis() - location.getTime();
        return age >= 0 && age <= MAX_LAST_LOCATION_AGE_MS;
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
                    if (isFindingLocation) {
                        locationStatusText.setText(
                                "Location provider was turned off"
                        );
                    }
                }

                @Override
                public void onProviderEnabled(
                        @NonNull String provider
                ) {
                    // No action required.
                }
            };

    private void handleLocation(Location location) {
        stopLocationUpdates();

        selectedLatitude = location.getLatitude();
        selectedLongitude = location.getLongitude();
        locationSelected = true;

        locationButton.setEnabled(true);
        locationButton.setText("REFRESH LOCATION");
        locationStatusText.setText("Location ready");
        locationStatusText.setTextColor(Color.parseColor("#7CFF5B"));

        coordinatesText.setText(String.format(
                Locale.getDefault(),
                "Latitude: %.6f\nLongitude: %.6f",
                selectedLatitude,
                selectedLongitude
        ));

        addressText.setText("Finding the nearest address...");
        reverseGeocodeLocation(selectedLatitude, selectedLongitude);
    }

    private void stopLocationUpdates() {
        isFindingLocation = false;
        mainHandler.removeCallbacks(locationTimeoutRunnable);

        if (locationManager == null) {
            return;
        }

        try {
            locationManager.removeUpdates(locationListener);
        } catch (Exception ignored) {
            // No active location request.
        }
    }

    private void resetLocationButton() {
        locationButton.setEnabled(true);
        locationButton.setText(
                locationSelected
                        ? "REFRESH LOCATION"
                        : "USE CURRENT LOCATION"
        );
    }

    private void reverseGeocodeLocation(
            double latitude,
            double longitude
    ) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        if (!Geocoder.isPresent()) {
            applyAddressForLocation(
                    latitude,
                    longitude,
                    null
            );
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1,
                        addresses -> runOnUiThread(() ->
                                applyAddressForLocation(
                                        latitude,
                                        longitude,
                                        addresses
                                )
                        )
                );
            } catch (Exception exception) {
                applyAddressForLocation(latitude, longitude, null);
            }
        } else {
            new Thread(() -> {
                List<Address> addresses = null;

                try {
                    addresses = geocoder.getFromLocation(
                            latitude,
                            longitude,
                            1
                    );
                } catch (Exception ignored) {
                    // Coordinates remain available even if geocoding fails.
                }

                List<Address> finalAddresses = addresses;
                runOnUiThread(() -> applyAddressForLocation(
                        latitude,
                        longitude,
                        finalAddresses
                ));
            }).start();
        }
    }

    private void applyAddressForLocation(
            double latitude,
            double longitude,
            List<Address> addresses
    ) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (!locationSelected
                || Double.compare(latitude, selectedLatitude) != 0
                || Double.compare(longitude, selectedLongitude) != 0) {
            return;
        }

        if (addresses != null && !addresses.isEmpty()) {
            String fullAddress = addresses.get(0).getAddressLine(0);
            selectedAddress = fullAddress == null
                    || fullAddress.trim().isEmpty()
                    ? "Address unavailable"
                    : fullAddress.trim();
        } else {
            selectedAddress = "Address unavailable";
        }

        addressText.setText(selectedAddress);
    }

    private void validateAndSubmit() {
        if (isSubmitting) {
            return;
        }

        weightEditText.setError(null);
        descriptionEditText.setError(null);

        if (!sessionManager.isLoggedIn()) {
            showToast("Your session has expired. Please log in again.");
            redirectToLogin();
            return;
        }

        if (!SupabaseConfig.isConfigured()) {
            showErrorDialog(
                    "Supabase is not configured",
                    "Add the Supabase anon key in SupabaseConfig.java before testing this module."
            );
            return;
        }

        String weightText = weightEditText.getText().toString();
        String description = descriptionEditText
                .getText()
                .toString()
                .trim();

        WasteReportValidator.ValidationResult validation =
                WasteReportValidator.validate(
                        selectedImageUri != null,
                        weightText,
                        description,
                        locationSelected
                );

        if (!validation.isValid()) {
            showValidationError(validation);
            return;
        }

        String wasteType = wasteTypeSpinner.getSelectedItemPosition() == 0
                ? "Not specified"
                : wasteTypeSpinner.getSelectedItem().toString();

        double estimatedWeight = validation.getEstimatedWeight();

        if (!uploadedImageUrl.isEmpty()) {
            setLoading(true, "Saving the report details...");
            createWasteReport(
                    uploadedImageUrl,
                    wasteType,
                    estimatedWeight,
                    description
            );
            return;
        }

        setLoading(true, "Preparing your photo...");

        new Thread(() -> {
            byte[] imageBytes = prepareSelectedImage();

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (imageBytes == null || imageBytes.length == 0) {
                    setLoading(false, null);
                    showErrorDialog(
                            "Photo could not be read",
                            "Please choose the photo again and retry."
                    );
                    return;
                }

                setLoadingStep("Uploading the photo...");
                uploadWasteImage(
                        imageBytes,
                        wasteType,
                        estimatedWeight,
                        description
                );
            });
        }).start();
    }

    private void showValidationError(
            WasteReportValidator.ValidationResult validation
    ) {
        switch (validation.getField()) {
            case PHOTO:
                showErrorDialog("Photo required", validation.getMessage());
                break;
            case WEIGHT:
                weightEditText.setError(validation.getMessage());
                weightEditText.requestFocus();
                break;
            case DESCRIPTION:
                descriptionEditText.setError(validation.getMessage());
                descriptionEditText.requestFocus();
                break;
            case LOCATION:
                showErrorDialog("Location required", validation.getMessage());
                break;
            case NONE:
            default:
                break;
        }
    }

    private byte[] prepareSelectedImage() {
        if (selectedImageUri == null) {
            return null;
        }

        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;

            try (InputStream inputStream = getContentResolver()
                    .openInputStream(selectedImageUri)) {
                if (inputStream == null) {
                    return null;
                }
                BitmapFactory.decodeStream(inputStream, null, bounds);
            }

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = calculateSampleSize(
                    bounds.outWidth,
                    bounds.outHeight,
                    MAX_IMAGE_DIMENSION
            );
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            Bitmap decodedBitmap;
            try (InputStream inputStream = getContentResolver()
                    .openInputStream(selectedImageUri)) {
                if (inputStream == null) {
                    return null;
                }
                decodedBitmap = BitmapFactory.decodeStream(
                        inputStream,
                        null,
                        options
                );
            }

            if (decodedBitmap == null) {
                return null;
            }

            Bitmap orientedBitmap = rotateBitmapIfRequired(
                    decodedBitmap,
                    selectedImageUri
            );

            if (orientedBitmap != decodedBitmap) {
                decodedBitmap.recycle();
            }

            Bitmap resizedBitmap = resizeBitmap(
                    orientedBitmap,
                    MAX_IMAGE_DIMENSION
            );

            if (resizedBitmap != orientedBitmap) {
                orientedBitmap.recycle();
            }

            byte[] result = compressBitmap(resizedBitmap);
            resizedBitmap.recycle();
            return result;
        } catch (Exception exception) {
            return null;
        }
    }

    private int calculateSampleSize(
            int width,
            int height,
            int maximumSize
    ) {
        int sampleSize = 1;

        while (width / sampleSize > maximumSize * 2
                || height / sampleSize > maximumSize * 2) {
            sampleSize *= 2;
        }

        return Math.max(sampleSize, 1);
    }

    private Bitmap rotateBitmapIfRequired(
            Bitmap bitmap,
            Uri uri
    ) {
        int orientation = ExifInterface.ORIENTATION_NORMAL;

        try (InputStream inputStream = getContentResolver()
                .openInputStream(uri)) {
            if (inputStream != null) {
                ExifInterface exif = new ExifInterface(inputStream);
                orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                );
            }
        } catch (Exception ignored) {
            return bitmap;
        }

        Matrix matrix = new Matrix();

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setScale(1, -1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setScale(-1, 1);
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setScale(-1, 1);
                matrix.postRotate(270);
                break;
            case ExifInterface.ORIENTATION_NORMAL:
            case ExifInterface.ORIENTATION_UNDEFINED:
            default:
                return bitmap;
        }

        return Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.getWidth(),
                bitmap.getHeight(),
                matrix,
                true
        );
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

        float scale = Math.min(
                (float) maximumSize / width,
                (float) maximumSize / height
        );

        return Bitmap.createScaledBitmap(
                bitmap,
                Math.round(width * scale),
                Math.round(height * scale),
                true
        );
    }

    private byte[] compressBitmap(Bitmap bitmap) {
        int quality = 88;
        byte[] bytes;

        do {
            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();
            bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    quality,
                    outputStream
            );
            bytes = outputStream.toByteArray();
            quality -= 7;
        } while (bytes.length > MAX_UPLOAD_BYTES && quality >= 60);

        return bytes;
    }

    private void uploadWasteImage(
            byte[] imageBytes,
            String wasteType,
            double estimatedWeight,
            String description
    ) {
        String imagePath = sessionManager.getUserId()
                + "/"
                + UUID.randomUUID()
                + ".jpg";

        RequestBody imageBody = RequestBody.create(
                MediaType.parse("image/jpeg"),
                imageBytes
        );

        WasteReportApi api = SupabaseClient.getClient()
                .create(WasteReportApi.class);

        uploadCall = api.uploadWasteImage(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "false",
                "image/jpeg",
                SupabaseConfig.WASTE_IMAGE_BUCKET,
                imagePath,
                imageBody
        );

        uploadCall.enqueue(new Callback<StorageUploadResponse>() {
            @Override
            public void onResponse(
                    Call<StorageUploadResponse> call,
                    Response<StorageUploadResponse> response
            ) {
                uploadCall = null;

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (!response.isSuccessful()) {
                    if (response.code() == 401) {
                        setLoading(false, null);
                        showToast(
                                "Your login session expired. Log in again."
                        );
                        redirectToLogin();
                        return;
                    }

                    showRetryableError(
                            "Photo upload failed",
                            friendlyServerMessage(
                                    "The photo could not be uploaded",
                                    response.code()
                            ),
                            () -> {
                                setLoading(true, "Uploading the photo...");
                                uploadWasteImage(
                                        imageBytes,
                                        wasteType,
                                        estimatedWeight,
                                        description
                                );
                            }
                    );
                    return;
                }

                uploadedImagePath = imagePath;
                uploadedImageUrl = SupabaseConfig.SUPABASE_URL
                        + "storage/v1/object/public/"
                        + SupabaseConfig.WASTE_IMAGE_BUCKET
                        + "/"
                        + imagePath;

                setLoadingStep("Saving the report details...");
                createWasteReport(
                        uploadedImageUrl,
                        wasteType,
                        estimatedWeight,
                        description
                );
            }

            @Override
            public void onFailure(
                    Call<StorageUploadResponse> call,
                    Throwable throwable
            ) {
                uploadCall = null;

                if (call.isCanceled()
                        || isFinishing()
                        || isDestroyed()) {
                    return;
                }

                showRetryableError(
                        "Photo upload failed",
                        "Check your internet connection and try again. Your form has not been cleared.",
                        () -> {
                            setLoading(true, "Uploading the photo...");
                            uploadWasteImage(
                                    imageBytes,
                                    wasteType,
                                    estimatedWeight,
                                    description
                            );
                        }
                );
            }
        });
    }

    private void createWasteReport(
            String imageUrl,
            String wasteType,
            double estimatedWeight,
            String description
    ) {
        WasteReport report = new WasteReport(
                sessionManager.getUserId(),
                imageUrl,
                wasteType,
                estimatedWeight,
                description,
                selectedLatitude,
                selectedLongitude,
                selectedAddress,
                "Pending"
        );

        WasteReportApi api = SupabaseClient.getClient()
                .create(WasteReportApi.class);

        createReportCall = api.createWasteReport(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                report
        );

        createReportCall.enqueue(new Callback<List<WasteReport>>() {
            @Override
            public void onResponse(
                    Call<List<WasteReport>> call,
                    Response<List<WasteReport>> response
            ) {
                createReportCall = null;

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (response.isSuccessful()) {
                    setLoading(false, null);
                    WasteReport savedReport = response.body() != null
                            && !response.body().isEmpty()
                            ? response.body().get(0)
                            : null;
                    showSuccessDialog(savedReport);
                    return;
                }

                if (response.code() == 401) {
                    setLoading(false, null);
                    showToast(
                            "Your login session expired. Log in again."
                    );
                    redirectToLogin();
                    return;
                }

                showRetryableError(
                        "Report could not be saved",
                        friendlyServerMessage(
                                "The photo was uploaded, but the report details were not saved",
                                response.code()
                        ),
                        () -> {
                            setLoading(
                                    true,
                                    "Saving the report details..."
                            );
                            createWasteReport(
                                    imageUrl,
                                    wasteType,
                                    estimatedWeight,
                                    description
                            );
                        }
                );
            }

            @Override
            public void onFailure(
                    Call<List<WasteReport>> call,
                    Throwable throwable
            ) {
                createReportCall = null;

                if (call.isCanceled()
                        || isFinishing()
                        || isDestroyed()) {
                    return;
                }

                showRetryableError(
                        "Report could not be saved",
                        "The photo was uploaded, but the report could not be saved because of a connection problem. Try again without changing the form.",
                        () -> {
                            setLoading(
                                    true,
                                    "Saving the report details..."
                            );
                            createWasteReport(
                                    imageUrl,
                                    wasteType,
                                    estimatedWeight,
                                    description
                            );
                        }
                );
            }
        });
    }

    private String friendlyServerMessage(
            String action,
            int errorCode
    ) {
        if (errorCode >= 500) {
            return action
                    + " because the server is temporarily unavailable. Please try again.";
        }

        if (errorCode == 403) {
            return action
                    + " because access was denied. Check the Supabase policies for this module.";
        }

        if (errorCode == 404) {
            return action
                    + " because the Supabase table or storage bucket could not be found.";
        }

        if (errorCode == 409) {
            return action
                    + " because the server found conflicting data. Refresh the form and try again.";
        }

        return action
                + ". Please try again. (Error "
                + errorCode
                + ")";
    }

    private void showRetryableError(
            String title,
            String message,
            Runnable retryAction
    ) {
        setLoading(false, null);

        if (isFinishing() || isDestroyed()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Keep Editing", null)
                .setPositiveButton(
                        "Try Again",
                        (dialog, which) -> retryAction.run()
                )
                .show();
    }

    private void showSuccessDialog(WasteReport savedReport) {
        String message = "Your report has been saved with Pending status. "
                + "Open Report History to confirm the photo and details.";

        if (savedReport != null
                && savedReport.getId() != null
                && !savedReport.getId().trim().isEmpty()) {
            message += "\n\nReport ID: " + savedReport.getId();
        }

        new AlertDialog.Builder(this)
                .setTitle("Report submitted successfully")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(
                        "View My Reports",
                        (dialog, which) -> {
                            clearUploadedImageReference();
                            startActivity(new Intent(
                                    WasteReportActivity.this,
                                    ReportHistoryActivity.class
                            ));
                            finish();
                        }
                )
                .setNegativeButton(
                        "Submit Another",
                        (dialog, which) -> resetForm()
                )
                .show();
    }

    private void resetForm() {
        clearSelectedImage();
        stopLocationUpdates();

        locationSelected = false;
        selectedLatitude = 0;
        selectedLongitude = 0;
        selectedAddress = "";

        wasteTypeSpinner.setSelection(0);
        weightEditText.setText("");
        weightEditText.setError(null);
        descriptionEditText.setText("");
        descriptionEditText.setError(null);

        locationStatusText.setText("No location selected");
        locationStatusText.setTextColor(Color.parseColor("#FFC857"));
        coordinatesText.setText("Latitude: --\nLongitude: --");
        addressText.setText("Address will appear here");
        locationButton.setText("USE CURRENT LOCATION");
    }

    private void setLoading(
            boolean loading,
            String statusMessage
    ) {
        isSubmitting = loading;

        loadingContainer.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        if (loading && statusMessage != null) {
            loadingStatusText.setText(statusMessage);
        }

        submitButton.setEnabled(!loading);
        galleryButton.setEnabled(!loading);
        cameraButton.setEnabled(!loading);
        removeImageButton.setEnabled(!loading);
        locationButton.setEnabled(!loading && !isFindingLocation);
        wasteTypeSpinner.setEnabled(!loading);
        weightEditText.setEnabled(!loading);
        descriptionEditText.setEnabled(!loading);

        submitButton.setText(
                loading ? "SUBMITTING..." : "SUBMIT REPORT"
        );
    }

    private void setLoadingStep(String statusMessage) {
        if (isSubmitting) {
            loadingStatusText.setText(statusMessage);
        }
    }

    private void showLocationSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Turn on location")
                .setMessage(
                        "Location services are needed to attach the correct collection point to this report."
                )
                .setPositiveButton(
                        "Open Settings",
                        (dialog, which) -> startActivity(
                                new Intent(
                                        Settings.ACTION_LOCATION_SOURCE_SETTINGS
                                )
                        )
                )
                .setNegativeButton("Not Now", null)
                .show();
    }

    private void showErrorDialog(
            String title,
            String message
    ) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void redirectToLogin() {
        sessionManager.logout();

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

    private void restoreState(Bundle state) {
        selectedLatitude = state.getDouble(STATE_LATITUDE, 0.0);
        selectedLongitude = state.getDouble(STATE_LONGITUDE, 0.0);
        selectedAddress = state.getString(STATE_ADDRESS, "");
        locationSelected = state.getBoolean(
                STATE_LOCATION_SELECTED,
                false
        );
        uploadedImageUrl = state.getString(
                STATE_UPLOADED_IMAGE_URL,
                ""
        );
        uploadedImagePath = state.getString(
                STATE_UPLOADED_IMAGE_PATH,
                ""
        );

        int spinnerPosition = state.getInt(
                STATE_WASTE_TYPE_POSITION,
                0
        );
        wasteTypeSpinner.setSelection(spinnerPosition);

        String pendingCameraUriValue = state.getString(
                STATE_PENDING_CAMERA_URI,
                ""
        );
        pendingCameraUri = pendingCameraUriValue.isEmpty()
                ? null
                : Uri.parse(pendingCameraUriValue);

        String imageUri = state.getString(STATE_IMAGE_URI, "");
        if (!imageUri.isEmpty()) {
            showSelectedImage(Uri.parse(imageUri), false);
        } else {
            clearSelectedImage();
        }

        if (locationSelected) {
            locationStatusText.setText("Location ready");
            locationStatusText.setTextColor(Color.parseColor("#7CFF5B"));
            coordinatesText.setText(String.format(
                    Locale.getDefault(),
                    "Latitude: %.6f\nLongitude: %.6f",
                    selectedLatitude,
                    selectedLongitude
            ));
            addressText.setText(
                    selectedAddress.isEmpty()
                            ? "Address unavailable"
                            : selectedAddress
            );
            locationButton.setText("REFRESH LOCATION");
        }
    }

    @Override
    protected void onSaveInstanceState(
            @NonNull Bundle outState
    ) {
        super.onSaveInstanceState(outState);

        if (selectedImageUri != null) {
            outState.putString(
                    STATE_IMAGE_URI,
                    selectedImageUri.toString()
            );
        }

        if (pendingCameraUri != null) {
            outState.putString(
                    STATE_PENDING_CAMERA_URI,
                    pendingCameraUri.toString()
            );
        }

        outState.putDouble(STATE_LATITUDE, selectedLatitude);
        outState.putDouble(STATE_LONGITUDE, selectedLongitude);
        outState.putString(STATE_ADDRESS, selectedAddress);
        outState.putBoolean(
                STATE_LOCATION_SELECTED,
                locationSelected
        );
        outState.putInt(
                STATE_WASTE_TYPE_POSITION,
                wasteTypeSpinner.getSelectedItemPosition()
        );
        outState.putString(
                STATE_UPLOADED_IMAGE_URL,
                uploadedImageUrl
        );
        outState.putString(
                STATE_UPLOADED_IMAGE_PATH,
                uploadedImagePath
        );
    }

    @Override
    protected void onDestroy() {
        stopLocationUpdates();
        mainHandler.removeCallbacksAndMessages(null);

        if (uploadCall != null) {
            uploadCall.cancel();
            uploadCall = null;
        }

        if (createReportCall != null) {
            createReportCall.cancel();
            createReportCall = null;
        }

        super.onDestroy();
    }
}
