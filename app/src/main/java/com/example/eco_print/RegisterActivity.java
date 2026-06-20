package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eco_print.api.AuthApi;
import com.example.eco_print.models.User;
import com.example.eco_print.utils.SessionManager;
import com.example.eco_print.utils.SupabaseClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.widget.TextView;

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

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validation
        if (email.isEmpty()) {

            Toast.makeText(
                    RegisterActivity.this,
                    "Please enter an email",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (password.isEmpty()) {

            Toast.makeText(
                    RegisterActivity.this,
                    "Please enter a password",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (password.length() < 6) {

            Toast.makeText(
                    RegisterActivity.this,
                    "Password must be at least 6 characters",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        registerButton.setEnabled(false);

        User user = new User(email, password);

        AuthApi authApi =
                SupabaseClient.getClient().create(AuthApi.class);

        authApi.signUp(user).enqueue(new Callback<>() {

            @Override
            public void onResponse(Call<Object> call,
                                   Response<Object> response) {

                registerButton.setEnabled(true);

                if (response.isSuccessful()) {

                    Toast.makeText(
                            RegisterActivity.this,
                            "Registration Successful!",
                            Toast.LENGTH_LONG
                    ).show();

                    // Save Login Session
                    SessionManager sessionManager =
                            new SessionManager(RegisterActivity.this);

                    sessionManager.setLoggedIn(true);

                    // Go to Home Screen
                    startActivity(
                            new Intent(
                                    RegisterActivity.this,
                                    HomeActivity.class
                            )
                    );

                    finish();

                } else {

                    try {

                        String errorMessage =
                                response.errorBody() != null
                                        ? response.errorBody().string()
                                        : "Registration Failed";

                        Toast.makeText(
                                RegisterActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();

                    } catch (Exception e) {

                        Toast.makeText(
                                RegisterActivity.this,
                                "Registration Failed",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Object> call,
                                  Throwable t) {

                registerButton.setEnabled(true);

                Toast.makeText(
                        RegisterActivity.this,
                        "Network Error: " + t.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }
}