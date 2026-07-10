package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eco_print.api.AuthApi;
import com.example.eco_print.api.ProfileApi;
import com.example.eco_print.models.AuthResponse;
import com.example.eco_print.models.CitizenProfileRequest;
import com.example.eco_print.models.CollectorApplicationRequest;
import com.example.eco_print.models.User;
import com.example.eco_print.models.UserProfile;
import com.example.eco_print.utils.CollectorApplicationManager;
import com.example.eco_print.utils.RoleNavigator;
import com.example.eco_print.utils.SessionManager;
import com.example.eco_print.utils.SupabaseClient;
import com.example.eco_print.utils.SupabaseConfig;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView registerText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerText = findViewById(R.id.registerText);

        loginButton.setOnClickListener(v -> loginUser());

        registerText.setOnClickListener(v ->
                startActivity(
                        new Intent(
                                LoginActivity.this,
                                AccountTypeActivity.class
                        )
                )
        );
    }

    private void loginUser() {

        String email =
                emailEditText.getText().toString().trim();

        String password =
                passwordEditText.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            showToast("Please fill all fields");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email address");
            emailEditText.requestFocus();
            return;
        }

        setLoading(true);

        AuthApi authApi =
                SupabaseClient.getClient()
                        .create(AuthApi.class);

        authApi.login(
                SupabaseConfig.SUPABASE_ANON_KEY,
                "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY,
                "application/json",
                new User(email, password)
        ).enqueue(new Callback<AuthResponse>() {

            @Override
            public void onResponse(
                    Call<AuthResponse> call,
                    Response<AuthResponse> response
            ) {
                AuthResponse authResponse = response.body();

                if (response.isSuccessful()
                        && authResponse != null
                        && authResponse.getAccessToken() != null
                        && authResponse.getUser() != null) {

                    SessionManager sessionManager =
                            new SessionManager(LoginActivity.this);

                    sessionManager.saveSession(
                            authResponse.getAccessToken(),
                            authResponse.getRefreshToken(),
                            authResponse.getUser().getId(),
                            authResponse.getUser().getEmail()
                    );

                    loadOrCreateProfile(sessionManager);
                    return;
                }

                setLoading(false);

                if (response.code() == 400) {
                    showToast("Invalid email or password");
                } else {
                    showToast(
                            "Login failed. Error code: "
                                    + response.code()
                    );
                }
            }

            @Override
            public void onFailure(
                    Call<AuthResponse> call,
                    Throwable throwable
            ) {
                setLoading(false);
                showToast(
                        "Network error: "
                                + throwable.getMessage()
                );
            }
        });
    }

    private void loadOrCreateProfile(
            SessionManager sessionManager
    ) {
        ProfileApi profileApi =
                SupabaseClient.getClient()
                        .create(ProfileApi.class);

        profileApi.getMyProfile(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "eq." + sessionManager.getUserId(),
                "*"
        ).enqueue(new Callback<List<UserProfile>>() {

            @Override
            public void onResponse(
                    Call<List<UserProfile>> call,
                    Response<List<UserProfile>> response
            ) {
                if (response.isSuccessful()
                        && response.body() != null
                        && !response.body().isEmpty()) {

                    finishLogin(
                            sessionManager,
                            response.body().get(0)
                    );
                    return;
                }

                if (response.isSuccessful()) {
                    createMissingProfile(sessionManager);
                    return;
                }

                setLoading(false);
                showToast(
                        "Unable to load account role. Error code: "
                                + response.code()
                );
            }

            @Override
            public void onFailure(
                    Call<List<UserProfile>> call,
                    Throwable throwable
            ) {
                setLoading(false);
                showToast(
                        "Profile error: "
                                + throwable.getMessage()
                );
            }
        });
    }

    private void createMissingProfile(
            SessionManager sessionManager
    ) {
        CollectorApplicationManager applicationManager =
                new CollectorApplicationManager(this);

        ProfileApi profileApi =
                SupabaseClient.getClient()
                        .create(ProfileApi.class);

        if (applicationManager.matchesEmail(
                sessionManager.getUserEmail()
        )) {
            CollectorApplicationRequest request =
                    new CollectorApplicationRequest(
                            applicationManager.getName(),
                            applicationManager.getAge(),
                            sessionManager.getUserEmail(),
                            applicationManager.getCompanyCode()
                    );

            profileApi.applyAsCollector(
                    SupabaseConfig.SUPABASE_ANON_KEY,
                    sessionManager.getAuthorizationHeader(),
                    request
            ).enqueue(profileCreationCallback(
                    sessionManager,
                    applicationManager
            ));

        } else {
            String defaultName = createDefaultName(
                    sessionManager.getUserEmail()
            );

            profileApi.createCitizenProfile(
                    SupabaseConfig.SUPABASE_ANON_KEY,
                    sessionManager.getAuthorizationHeader(),
                    new CitizenProfileRequest(
                            defaultName,
                            sessionManager.getUserEmail()
                    )
            ).enqueue(profileCreationCallback(
                    sessionManager,
                    null
            ));
        }
    }

    private Callback<List<UserProfile>> profileCreationCallback(
            SessionManager sessionManager,
            CollectorApplicationManager applicationManager
    ) {
        return new Callback<List<UserProfile>>() {

            @Override
            public void onResponse(
                    Call<List<UserProfile>> call,
                    Response<List<UserProfile>> response
            ) {
                setLoading(false);

                if (response.isSuccessful()
                        && response.body() != null
                        && !response.body().isEmpty()) {

                    if (applicationManager != null) {
                        applicationManager.clear();
                    }

                    finishLogin(
                            sessionManager,
                            response.body().get(0)
                    );
                    return;
                }

                showToast(
                        "Unable to create account profile. Error code: "
                                + response.code()
                );
            }

            @Override
            public void onFailure(
                    Call<List<UserProfile>> call,
                    Throwable throwable
            ) {
                setLoading(false);
                showToast(
                        "Profile creation failed: "
                                + throwable.getMessage()
                );
            }
        };
    }

    private void finishLogin(
            SessionManager sessionManager,
            UserProfile profile
    ) {
        setLoading(false);

        sessionManager.saveProfile(
                profile.getRole(),
                profile.getFullName(),
                profile.getCollectorStatus()
        );

        showToast("Login successful");

        RoleNavigator.openCorrectHome(
                this,
                sessionManager
        );
    }

    private String createDefaultName(String email) {
        if (email == null || !email.contains("@")) {
            return "Citizen";
        }

        String name = email.substring(0, email.indexOf('@'));
        return name.replace('.', ' ')
                .replace('_', ' ')
                .trim();
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        loginButton.setText(
                loading ? "LOGGING IN..." : "LOGIN"
        );
    }

    private void showToast(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}
