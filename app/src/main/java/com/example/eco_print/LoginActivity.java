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

        registerText.setOnClickListener(v -> {
            startActivity(
                    new Intent(
                            LoginActivity.this,
                            RegisterActivity.class
                    )
            );
            finish();
        });
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

        if (SupabaseConfig.SUPABASE_ANON_KEY
                .equals("PASTE_YOUR_SUPABASE_ANON_KEY_HERE")) {
            showToast("Add your Supabase anon key in SupabaseConfig.java");
            return;
        }

        setLoading(true);

        User user = new User(email, password);

        AuthApi authApi =
                SupabaseClient.getClient().create(AuthApi.class);

        authApi.login(
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
                            new SessionManager(LoginActivity.this);

                    sessionManager.saveSession(
                            authResponse.getAccessToken(),
                            authResponse.getRefreshToken(),
                            authResponse.getUser().getId(),
                            authResponse.getUser().getEmail()
                    );

                    showToast("Login successful");

                    Intent intent = new Intent(
                            LoginActivity.this,
                            HomeActivity.class
                    );

                    intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );

                    startActivity(intent);
                    return;
                }

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
