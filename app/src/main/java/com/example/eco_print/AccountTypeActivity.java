package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AccountTypeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_type);

        ImageButton backButton =
                findViewById(R.id.backButton);

        LinearLayout citizenCard =
                findViewById(R.id.citizenCard);

        LinearLayout collectorCard =
                findViewById(R.id.collectorCard);

        TextView loginText =
                findViewById(R.id.loginText);

        backButton.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        citizenCard.setOnClickListener(v ->
                startActivity(
                        new Intent(
                                AccountTypeActivity.this,
                                RegisterActivity.class
                        )
                )
        );

        collectorCard.setOnClickListener(v ->
                startActivity(
                        new Intent(
                                AccountTypeActivity.this,
                                CollectorRegisterActivity.class
                        )
                )
        );

        loginText.setOnClickListener(v ->
                startActivity(
                        new Intent(
                                AccountTypeActivity.this,
                                LoginActivity.class
                        )
                )
        );
    }
}
