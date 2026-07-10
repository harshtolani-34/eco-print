package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eco_print.adapter.NewsAdapter;
import com.example.eco_print.api.NewsApi;
import com.example.eco_print.models.NewsResponse;
import com.example.eco_print.utils.NewsClient;
import com.example.eco_print.utils.SessionManager;
import com.google.android.material.navigation.NavigationView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewsActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton menuButton;
    private RecyclerView newsRecyclerView;

    private static final String API_KEY =
            "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        menuButton = findViewById(R.id.menuButton);
        newsRecyclerView = findViewById(R.id.newsRecyclerView);

        newsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        loadNews();

        menuButton.setOnClickListener(v ->
                drawerLayout.openDrawer(navigationView)
        );

        navigationView.setCheckedItem(R.id.nav_news);

        navigationView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(
                        new Intent(
                                NewsActivity.this,
                                HomeActivity.class
                        )
                );
                finish();

            } else if (id == R.id.nav_news) {
                drawerLayout.closeDrawers();

            } else if (id == R.id.nav_report) {
                startActivity(
                        new Intent(
                                NewsActivity.this,
                                WasteReportActivity.class
                        )
                );

            } else if (id == R.id.nav_logout) {
                logoutUser();
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }

    private void loadNews() {

        if (API_KEY.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Add your GNews API key in NewsActivity",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        NewsApi newsApi =
                NewsClient.getClient().create(NewsApi.class);

        newsApi.getNews(
                "environment OR recycling OR sustainability",
                "en",
                10,
                API_KEY
        ).enqueue(new Callback<NewsResponse>() {

            @Override
            public void onResponse(
                    Call<NewsResponse> call,
                    Response<NewsResponse> response
            ) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().getArticles() != null) {

                    NewsAdapter adapter =
                            new NewsAdapter(
                                    response.body().getArticles()
                            );

                    newsRecyclerView.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(
                    Call<NewsResponse> call,
                    Throwable throwable
            ) {
                Toast.makeText(
                        NewsActivity.this,
                        "Unable to load news",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void logoutUser() {

        new SessionManager(this).logout();

        Intent intent = new Intent(
                NewsActivity.this,
                WelcomeActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
    }
}
