package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eco_print.utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager =
                new SessionManager(this);

        if (sessionManager.isLoggedIn()) {

            startActivity(
                    new Intent(
                            MainActivity.this,
                            HomeActivity.class
                    )
            );

        } else {

            startActivity(
                    new Intent(
                            MainActivity.this,
                            WelcomeActivity.class
                    )
            );
        }

        finish();
    }
}