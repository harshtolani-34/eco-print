package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

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

        navigationView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_home) {

                startActivity(
                        new Intent(
                                NewsActivity.this,
                                HomeActivity.class
                        )
                );

            } else if (id == R.id.nav_logout) {

                SessionManager sessionManager =
                        new SessionManager(NewsActivity.this);

                sessionManager.logout();

                startActivity(
                        new Intent(
                                NewsActivity.this,
                                WelcomeActivity.class
                        )
                );

                finish();
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }

    private void loadNews() {

        NewsApi newsApi =
                NewsClient.getClient()
                        .create(NewsApi.class);

        Call<NewsResponse> call =
                newsApi.getNews(
                        "environment OR recycling OR sustainability",
                        "en",
                        10,
                        API_KEY
                );

        call.enqueue(new Callback<NewsResponse>() {

            @Override
            public void onResponse(
                    Call<NewsResponse> call,
                    Response<NewsResponse> response
            ) {

                if (response.isSuccessful()
                        && response.body() != null) {

                    NewsAdapter adapter =
                            new NewsAdapter(
                                    response.body()
                                            .getArticles()
                            );

                    newsRecyclerView.setAdapter(
                            adapter
                    );
                }
            }

            @Override
            public void onFailure(
                    Call<NewsResponse> call,
                    Throwable t
            ) {

                t.printStackTrace();
            }
        });
    }
}