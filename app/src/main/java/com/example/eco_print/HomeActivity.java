package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.eco_print.api.WasteReportApi;
import com.example.eco_print.models.WasteReport;
import com.example.eco_print.utils.SessionManager;
import com.example.eco_print.utils.SupabaseClient;
import com.example.eco_print.utils.SupabaseConfig;
import com.google.android.material.navigation.NavigationView;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton menuButton;

    private View reportsCard;
    private TextView reportsCountText;
    private TextView wasteWeightText;
    private TextView pointsCountText;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        menuButton = findViewById(R.id.menuButton);
        reportsCard = findViewById(R.id.reportsCard);
        reportsCountText = findViewById(R.id.reportsCountText);
        wasteWeightText = findViewById(R.id.wasteWeightText);
        pointsCountText = findViewById(R.id.pointsCountText);

        sessionManager = new SessionManager(this);

        menuButton.setOnClickListener(v ->
                drawerLayout.openDrawer(navigationView)
        );

        reportsCard.setOnClickListener(v ->
                startActivity(new Intent(
                        HomeActivity.this,
                        ReportHistoryActivity.class
                ))
        );

        navigationView.setCheckedItem(R.id.nav_home);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                drawerLayout.closeDrawers();
            } else if (id == R.id.nav_news) {
                startActivity(new Intent(
                        HomeActivity.this,
                        NewsActivity.class
                ));
            } else if (id == R.id.nav_report) {
                startActivity(new Intent(
                        HomeActivity.this,
                        WasteReportActivity.class
                ));
            } else if (id == R.id.nav_logout) {
                logoutUser();
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardStatistics();
    }

    private void loadDashboardStatistics() {
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        WasteReportApi api = SupabaseClient.getClient()
                .create(WasteReportApi.class);

        api.getMyWasteReports(
                SupabaseConfig.SUPABASE_ANON_KEY,
                sessionManager.getAuthorizationHeader(),
                "*",
                "created_at.desc"
        ).enqueue(new Callback<List<WasteReport>>() {
            @Override
            public void onResponse(
                    Call<List<WasteReport>> call,
                    Response<List<WasteReport>> response
            ) {
                if (response.code() == 401) {
                    redirectToLogin();
                    return;
                }

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(
                            HomeActivity.this,
                            "Unable to load dashboard data",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                List<WasteReport> reports = response.body();
                double totalWeight = 0;

                for (WasteReport report : reports) {
                    totalWeight += report.getEstimatedWeightKg();
                }

                reportsCountText.setText(String.valueOf(reports.size()));
                wasteWeightText.setText(String.format(
                        Locale.getDefault(),
                        "%.2f KG",
                        totalWeight
                ));
                pointsCountText.setText("0");
            }

            @Override
            public void onFailure(
                    Call<List<WasteReport>> call,
                    Throwable throwable
            ) {
                Toast.makeText(
                        HomeActivity.this,
                        "Dashboard error: " + throwable.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void logoutUser() {
        sessionManager.logout();

        Intent intent = new Intent(
                HomeActivity.this,
                WelcomeActivity.class
        );
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void redirectToLogin() {
        sessionManager.logout();

        Intent intent = new Intent(
                HomeActivity.this,
                LoginActivity.class
        );
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
