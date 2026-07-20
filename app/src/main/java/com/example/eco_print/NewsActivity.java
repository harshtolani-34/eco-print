package com.example.eco_print;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eco_print.adapter.NewsAdapter;
import com.example.eco_print.api.NewsApi;
import com.example.eco_print.models.Article;
import com.example.eco_print.models.NewsResponse;
import com.example.eco_print.utils.NewsCacheManager;
import com.example.eco_print.utils.NewsClient;
import com.example.eco_print.utils.SessionManager;
import com.google.android.material.navigation.NavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewsActivity extends AppCompatActivity {

    private static final int NEWS_LIMIT = 8;

    // Keep your existing GNews key here.
    private static final String API_KEY =
            "";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView newsRecyclerView;
    private ProgressBar refreshProgress;
    private LinearLayout loadingState;
    private LinearLayout errorState;
    private TextView newsStatusText;
    private TextView errorText;
    private Button retryButton;

    private NewsAdapter adapter;
    private NewsCacheManager cacheManager;
    private Call<NewsResponse> activeCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        bindViews();
        configureRecyclerView();
        configureNavigation();

        cacheManager = new NewsCacheManager(this);

        retryButton.setOnClickListener(v ->
                fetchLatestNews()
        );

        newsStatusText.setOnClickListener(v -> {
            if (!isRefreshing()) {
                fetchLatestNews();
            }
        });

        showCachedNewsThenRefresh();
    }

    private void bindViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        newsRecyclerView = findViewById(R.id.newsRecyclerView);
        refreshProgress = findViewById(R.id.refreshProgress);
        loadingState = findViewById(R.id.loadingState);
        errorState = findViewById(R.id.errorState);
        newsStatusText = findViewById(R.id.newsStatusText);
        errorText = findViewById(R.id.errorText);
        retryButton = findViewById(R.id.retryButton);

        ImageButton menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(v ->
                drawerLayout.openDrawer(navigationView)
        );
    }

    private void configureRecyclerView() {
        adapter = new NewsAdapter();

        newsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );
        newsRecyclerView.setAdapter(adapter);
        newsRecyclerView.setHasFixedSize(true);
        newsRecyclerView.setItemViewCacheSize(4);
        newsRecyclerView.setItemAnimator(null);
    }

    private void configureNavigation() {
        navigationView.setCheckedItem(R.id.nav_news);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(
                        NewsActivity.this,
                        HomeActivity.class
                ));
                finish();
            } else if (id == R.id.nav_news) {
                drawerLayout.closeDrawers();
            } else if (id == R.id.nav_report) {
                startActivity(new Intent(
                        NewsActivity.this,
                        WasteReportActivity.class
                ));
            } else if (id == R.id.nav_logout) {
                logoutUser();
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }

    private void showCachedNewsThenRefresh() {
        List<Article> cachedArticles = sanitizeArticles(
                cacheManager.getCachedArticles()
        );

        if (!cachedArticles.isEmpty()) {
            adapter.submitArticles(cachedArticles);
            showArticleList();
            newsStatusText.setText(
                    createCacheStatus(cacheManager.getCachedAt())
                            + " • Refreshing…"
            );
        } else {
            showFullLoading();
        }

        fetchLatestNews();
    }

    private void fetchLatestNews() {
        if (activeCall != null) {
            activeCall.cancel();
        }

        if (API_KEY.trim().isEmpty()) {
            handleLoadError(
                    "Add your GNews API key in NewsActivity to load fresh news."
            );
            return;
        }

        if (adapter.hasArticles()) {
            showArticleList();
            setRefreshing(true);
            newsStatusText.setText("Refreshing environmental news…");
        } else {
            showFullLoading();
        }

        NewsApi newsApi = NewsClient.getClient()
                .create(NewsApi.class);

        activeCall = newsApi.getNews(
                "plastic waste OR recycling OR environment",
                "en",
                NEWS_LIMIT,
                API_KEY
        );

        activeCall.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(
                    Call<NewsResponse> call,
                    Response<NewsResponse> response
            ) {
                if (call.isCanceled()) {
                    return;
                }

                activeCall = null;
                setRefreshing(false);

                if (!response.isSuccessful()
                        || response.body() == null) {
                    handleLoadError(
                            "The news service is unavailable right now."
                    );
                    return;
                }

                List<Article> articles = sanitizeArticles(
                        response.body().getArticles()
                );

                if (articles.isEmpty()) {
                    handleLoadError(
                            "No environmental stories were returned."
                    );
                    return;
                }

                adapter.submitArticles(articles);
                cacheManager.saveArticles(articles);
                showArticleList();
                newsStatusText.setText("Updated just now");
            }

            @Override
            public void onFailure(
                    Call<NewsResponse> call,
                    Throwable throwable
            ) {
                if (call.isCanceled()) {
                    return;
                }

                activeCall = null;
                setRefreshing(false);

                handleLoadError(
                        "Couldn’t load fresh news. Check your connection and retry."
                );
            }
        });
    }

    private List<Article> sanitizeArticles(List<Article> rawArticles) {
        List<Article> cleanArticles = new ArrayList<>();

        if (rawArticles == null) {
            return cleanArticles;
        }

        for (Article article : rawArticles) {
            if (article == null
                    || article.getTitle() == null
                    || article.getTitle().trim().isEmpty()) {
                continue;
            }

            cleanArticles.add(article);

            if (cleanArticles.size() == NEWS_LIMIT) {
                break;
            }
        }

        return cleanArticles;
    }

    private void handleLoadError(String message) {
        setRefreshing(false);

        if (adapter.hasArticles()) {
            showArticleList();
            newsStatusText.setText(
                    "Couldn’t refresh • Showing saved news • Tap to retry"
            );
            return;
        }

        newsRecyclerView.setVisibility(View.GONE);
        loadingState.setVisibility(View.GONE);
        errorState.setVisibility(View.VISIBLE);
        errorText.setText(message);
    }

    private void showFullLoading() {
        newsRecyclerView.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
        loadingState.setVisibility(View.VISIBLE);
        newsStatusText.setText("Finding the latest environmental stories…");
        setRefreshing(false);
    }

    private void showArticleList() {
        loadingState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
        newsRecyclerView.setVisibility(View.VISIBLE);
    }

    private void setRefreshing(boolean refreshing) {
        refreshProgress.setVisibility(
                refreshing ? View.VISIBLE : View.GONE
        );
    }

    private boolean isRefreshing() {
        return refreshProgress.getVisibility() == View.VISIBLE;
    }

    private String createCacheStatus(long cachedAt) {
        if (cachedAt <= 0) {
            return "Showing saved news";
        }

        SimpleDateFormat formatter = new SimpleDateFormat(
                "dd MMM, hh:mm a",
                Locale.getDefault()
        );

        return "Saved " + formatter.format(new Date(cachedAt));
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
        finish();
    }

    @Override
    protected void onDestroy() {
        if (activeCall != null) {
            activeCall.cancel();
        }

        super.onDestroy();
    }
}
