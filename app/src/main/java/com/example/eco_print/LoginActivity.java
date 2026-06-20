package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {

            Toast.makeText(
                    LoginActivity.this,
                    "Please fill all fields",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        User user = new User(email, password);

        AuthApi authApi =
                SupabaseClient.getClient().create(AuthApi.class);

        loginButton.setEnabled(false);

        authApi.login(user).enqueue(new Callback<>() {

            @Override
            public void onResponse(Call<Object> call,
                                   Response<Object> response) {

                loginButton.setEnabled(true);

                if (response.isSuccessful()) {

                    Toast.makeText(
                            LoginActivity.this,
                            "Login Successful!",
                            Toast.LENGTH_SHORT
                    ).show();

                    SessionManager sessionManager =
                            new SessionManager(LoginActivity.this);

                    sessionManager.setLoggedIn(true);

                    startActivity(
                            new Intent(
                                    LoginActivity.this,
                                    HomeActivity.class
                            )
                    );

                    finish();

                } else {

                    Toast.makeText(
                            LoginActivity.this,
                            "Invalid Credentials",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(Call<Object> call,
                                  Throwable t) {

                loginButton.setEnabled(true);

                Toast.makeText(
                        LoginActivity.this,
                        "Network Error: " + t.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }
}