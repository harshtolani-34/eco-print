package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    private Button loginButton;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);

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
    }
}
