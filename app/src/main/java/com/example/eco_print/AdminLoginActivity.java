package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eco_print.api.AuthApi;
import com.example.eco_print.api.ProfileApi;
import com.example.eco_print.models.AuthResponse;
import com.example.eco_print.models.User;
import com.example.eco_print.models.UserProfile;
import com.example.eco_print.utils.RoleNavigator;
import com.example.eco_print.utils.SessionManager;
import com.example.eco_print.utils.SupabaseClient;
import com.example.eco_print.utils.SupabaseConfig;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminLoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        ImageButton backButton = findViewById(R.id.backButton);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);

        backButton.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        loginButton.setOnClickListener(v -> loginAdministrator());
    }

    private void loginAdministrator() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        if (!SupabaseConfig.isConfigured()) {
            showToast("Supabase is not configured in this project");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid administrator email");
            emailEditText.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Enter your password");
            passwordEditText.requestFocus();
            return;
        }

        setLoading(true);

        AuthApi authApi = SupabaseClient.getClient().create(AuthApi.class);
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

                if (!response.isSuccessful()
                        || authResponse == null
                        || authResponse.getAccessToken() == null
                        || authResponse.getUser() == null) {
                    setLoading(false);
                    showToast("Invalid administrator email or password");
                    return;
                }

                SessionManager sessionManager =
                        new SessionManager(AdminLoginActivity.this);

                sessionManager.saveSession(
                        authResponse.getAccessToken(),
                        authResponse.getRefreshToken(),
                        authResponse.getUser().getId(),
                        authResponse.getUser().getEmail()
                );

                verifyAdministratorRole(sessionManager);
            }

            @Override
            public void onFailure(
                    Call<AuthResponse> call,
                    Throwable throwable
            ) {
                setLoading(false);
                showToast("Could not connect to Supabase");
            }
        });
    }

    private void verifyAdministratorRole(SessionManager sessionManager) {
        ProfileApi profileApi = SupabaseClient.getClient()
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
                setLoading(false);

                if (!response.isSuccessful()
                        || response.body() == null
                        || response.body().isEmpty()) {
                    sessionManager.logout();
                    showToast("Administrator profile could not be loaded");
                    return;
                }

                UserProfile profile = response.body().get(0);
                if (!"admin".equalsIgnoreCase(profile.getRole())) {
                    sessionManager.logout();
                    showToast("This account does not have administrator access");
                    return;
                }

                sessionManager.saveProfile(
                        profile.getRole(),
                        profile.getFullName(),
                        profile.getCollectorStatus()
                );

                showToast("Administrator login successful");
                RoleNavigator.openCorrectHome(
                        AdminLoginActivity.this,
                        sessionManager
                );
            }

            @Override
            public void onFailure(
                    Call<List<UserProfile>> call,
                    Throwable throwable
            ) {
                setLoading(false);
                sessionManager.logout();
                showToast("Could not verify administrator access");
            }
        });
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        emailEditText.setEnabled(!loading);
        passwordEditText.setEnabled(!loading);
        loginButton.setText(
                loading ? "VERIFYING..." : "ADMIN LOGIN"
        );
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
