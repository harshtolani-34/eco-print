package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    private Button loginButton;
    private Button registerButton;
    private TextView adminLoginText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        adminLoginText = findViewById(R.id.adminLoginText);

        loginButton.setOnClickListener(v ->
                startActivity(
                        new Intent(
                                WelcomeActivity.this,
                                LoginActivity.class
                        )
                )
        );

        registerButton.setOnClickListener(v ->
                startActivity(
                        new Intent(
                                WelcomeActivity.this,
                                AccountTypeActivity.class
                        )
                )
        );

        adminLoginText.setOnClickListener(v ->
                startActivity(
                        new Intent(
                                WelcomeActivity.this,
                                AdminLoginActivity.class
                        )
                )
        );
    }
}
