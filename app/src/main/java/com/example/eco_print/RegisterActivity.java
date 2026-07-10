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
import com.example.eco_print.models.AuthResponse;
import com.example.eco_print.models.User;
import com.example.eco_print.utils.SessionManager;
import com.example.eco_print.utils.SupabaseClient;
import com.example.eco_print.utils.SupabaseConfig;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button registerButton;
    private TextView loginText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);
        loginText = findViewById(R.id.loginText);

        registerButton.setOnClickListener(v -> registerUser());

        loginText.setOnClickListener(v -> {
            startActivity(
                    new Intent(
                            RegisterActivity.this,
                            LoginActivity.class
                    )
            );
            finish();
        });
    }

    private void registerUser() {

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

        if (password.length() < 6) {
            passwordEditText.setError(
                    "Password must be at least 6 characters"
            );
            passwordEditText.requestFocus();
            return;
        }

        if (SupabaseConfig.SUPABASE_ANON_KEY
                .equals("PASTE_YOUR_SUPABASE_ANON_KEY_HERE")) {
            showToast("Add your Supabase anon key in SupabaseConfig.java");
            return;
        }

        setLoading(true);

        User user = new User(email, password);

        AuthApi authApi =
                SupabaseClient.getClient().create(AuthApi.class);

        authApi.signUp(
                SupabaseConfig.SUPABASE_ANON_KEY,
                "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY,
                "application/json",
                user
        ).enqueue(new Callback<AuthResponse>() {

            @Override
            public void onResponse(
                    Call<AuthResponse> call,
                    Response<AuthResponse> response
            ) {
                setLoading(false);

                AuthResponse authResponse = response.body();

                if (response.isSuccessful()
                        && authResponse != null
                        && authResponse.getAccessToken() != null
                        && authResponse.getUser() != null) {

                    SessionManager sessionManager =
                            new SessionManager(RegisterActivity.this);

                    sessionManager.saveSession(
                            authResponse.getAccessToken(),
                            authResponse.getRefreshToken(),
                            authResponse.getUser().getId(),
                            authResponse.getUser().getEmail()
                    );

                    showToast("Registration successful");

                    Intent intent = new Intent(
                            RegisterActivity.this,
                            HomeActivity.class
                    );

                    intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );

                    startActivity(intent);
                    return;
                }

                if (response.isSuccessful()) {
                    showToast(
                            "Account created. Verify your email, then log in."
                    );

                    startActivity(
                            new Intent(
                                    RegisterActivity.this,
                                    LoginActivity.class
                            )
                    );

                    finish();
                } else {
                    showToast(
                            "Registration failed. Error code: "
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

    private void setLoading(boolean loading) {
        registerButton.setEnabled(!loading);
        registerButton.setText(
                loading ? "CREATING ACCOUNT..." : "REGISTER"
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
