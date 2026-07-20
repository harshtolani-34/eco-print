package com.example.eco_print.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.eco_print.models.Article;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NewsCacheManager {

    private static final String PREF_NAME = "EcoPrintNewsCache";
    private static final String KEY_ARTICLES = "articlesJson";
    private static final String KEY_CACHED_AT = "cachedAt";
    private static final int MAX_CACHED_ARTICLES = 8;

    private final SharedPreferences preferences;
    private final Gson gson = new Gson();

    public NewsCacheManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );
    }

    public List<Article> getCachedArticles() {
        String json = preferences.getString(KEY_ARTICLES, "");

        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }

        Type listType = new TypeToken<List<Article>>() {
        }.getType();

        try {
            List<Article> articles = gson.fromJson(json, listType);

            if (articles == null) {
                return Collections.emptyList();
            }

            return new ArrayList<>(articles);
        } catch (RuntimeException exception) {
            clear();
            return Collections.emptyList();
        }
    }

    public void saveArticles(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return;
        }

        int endIndex = Math.min(
                articles.size(),
                MAX_CACHED_ARTICLES
        );

        List<Article> cacheCopy = new ArrayList<>(
                articles.subList(0, endIndex)
        );

        preferences.edit()
                .putString(KEY_ARTICLES, gson.toJson(cacheCopy))
                .putLong(KEY_CACHED_AT, System.currentTimeMillis())
                .apply();
    }

    public long getCachedAt() {
        return preferences.getLong(KEY_CACHED_AT, 0L);
    }

    public void clear() {
        preferences.edit().clear().apply();
    }
}
