package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.eco_print.utils.SessionManager;
import com.google.android.material.navigation.NavigationView;

public class HomeActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton menuButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        menuButton = findViewById(R.id.menuButton);

        menuButton.setOnClickListener(v ->
                drawerLayout.openDrawer(navigationView)
        );

        navigationView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_news) {

                startActivity(
                        new Intent(
                                HomeActivity.this,
                                NewsActivity.class
                        )
                );

            } else if (id == R.id.nav_logout) {

                SessionManager sessionManager =
                        new SessionManager(HomeActivity.this);

                sessionManager.logout();

                startActivity(
                        new Intent(
                                HomeActivity.this,
                                WelcomeActivity.class
                        )
                );

                finish();
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }
}