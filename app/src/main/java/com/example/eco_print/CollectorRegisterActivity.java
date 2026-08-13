package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eco_print.api.AuthApi;
import com.example.eco_print.api.ProfileApi;
import com.example.eco_print.models.AuthResponse;
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

public class CollectorRegisterActivity
        extends AppCompatActivity {

    private EditText nameEditText;
    private EditText ageEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText companyCodeEditText;
    private Button applyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(
                R.layout.activity_collector_register
        );

        ImageButton backButton =
                findViewById(R.id.backButton);

        nameEditText =
                findViewById(R.id.nameEditText);

        ageEditText =
                findViewById(R.id.ageEditText);

        emailEditText =
                findViewById(R.id.emailEditText);

        passwordEditText =
                findViewById(R.id.passwordEditText);

        companyCodeEditText =
                findViewById(R.id.companyCodeEditText);

        applyButton =
                findViewById(R.id.applyButton);

        backButton.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        applyButton.setOnClickListener(v ->
                submitCollectorApplication()
        );
    }

    private void submitCollectorApplication() {

        String name =
                nameEditText.getText().toString().trim();

        String ageText =
                ageEditText.getText().toString().trim();

        String email =
                emailEditText.getText().toString().trim();

        String password =
                passwordEditText.getText().toString();

        String companyCode =
                companyCodeEditText.getText()
                        .toString()
                        .trim()
                        .toUpperCase();

        if (name.length() < 3) {
            nameEditText.setError("Enter your full name");
            nameEditText.requestFocus();
            return;
        }

        int age;

        try {
            age = Integer.parseInt(ageText);
        } catch (NumberFormatException exception) {
            ageEditText.setError("Enter a valid age");
            ageEditText.requestFocus();
            return;
        }

        if (age < 18 || age > 70) {
            ageEditText.setError(
                    "Collector age must be between 18 and 70"
            );
            ageEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email address");
            emailEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError(
                    "Password must be at least 6 characters"
            );
            passwordEditText.requestFocus();
            return;
        }

        if (companyCode.length() < 4) {
            companyCodeEditText.setError(
                    "Enter the company identification code"
            );
            companyCodeEditText.requestFocus();
            return;
        }

        CollectorApplicationManager applicationManager =
                new CollectorApplicationManager(this);

        applicationManager.saveApplication(
                name,
                age,
                email,
                companyCode
        );

        setLoading(true);

        AuthApi authApi =
                SupabaseClient.getClient()
                        .create(AuthApi.class);

        authApi.signUp(
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
                            new SessionManager(
                                    CollectorRegisterActivity.this
                            );

                    sessionManager.saveSession(
                            authResponse.getAccessToken(),
                            authResponse.getRefreshToken(),
                            authResponse.getUser().getId(),
                            authResponse.getUser().getEmail()
                    );

                    createCollectorProfile(
                            sessionManager,
                            applicationManager,
                            name,
                            age,
                            email,
                            companyCode
                    );
                    return;
                }

                setLoading(false);

                if (response.isSuccessful()) {
                    showToast(
                            "Application account created. "
                                    + "Verify your email and log in to continue."
                    );
                    openLogin();
                } else {
                    showToast(
                            "Application failed. Error code: "
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

    private void createCollectorProfile(
            SessionManager sessionManager,
            CollectorApplicationManager applicationManager,
            String name,
            int age,
            String email,
            String companyCode
    ) {
        ProfileApi profileApi =
                SupabaseClient.getClient()
                        .create(ProfileApi.class);

        profileApi.applyAsCollector(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                new CollectorApplicationRequest(
                        name,
                        age,
                        email,
                        companyCode
                )
        ).enqueue(new Callback<List<UserProfile>>() {

            @Override
            public void onResponse(
                    Call<List<UserProfile>> call,
                    Response<List<UserProfile>> response
            ) {
                setLoading(false);

                if (response.isSuccessful()
                        && response.body() != null
                        && !response.body().isEmpty()) {

                    UserProfile profile =
                            response.body().get(0);

                    sessionManager.saveProfile(
                            profile.getRole(),
                            profile.getFullName(),
                            profile.getCollectorStatus()
                    );

                    applicationManager.clear();
                    showApplicationSubmittedDialog(sessionManager);
                    return;
                }

                showToast(
                        "Account created, but collector profile failed. "
                                + "Log in again to retry."
                );
                openLogin();
            }

            @Override
            public void onFailure(
                    Call<List<UserProfile>> call,
                    Throwable throwable
            ) {
                setLoading(false);
                showToast(
                        "Collector profile failed. Log in again to retry."
                );
                openLogin();
            }
        });
    }

    private void showApplicationSubmittedDialog(
            SessionManager sessionManager
    ) {
        new AlertDialog.Builder(this)
                .setTitle("Application Submitted")
                .setMessage(
                        "Your collector application has been received.\n\n"
                                + "An administrator must approve it before "
                                + "you can view or accept collection tasks."
                )
                .setCancelable(false)
                .setPositiveButton(
                        "VIEW APPLICATION STATUS",
                        (dialog, which) ->
                                RoleNavigator.openCorrectHome(
                                        CollectorRegisterActivity.this,
                                        sessionManager
                                )
                )
                .show();
    }

    private void openLogin() {
        Intent intent = new Intent(
                this,
                LoginActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        applyButton.setEnabled(!loading);
        applyButton.setText(
                loading
                        ? "SUBMITTING APPLICATION..."
                        : "APPLY AS COLLECTOR"
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
