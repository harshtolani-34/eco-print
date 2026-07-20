package com.example.eco_print.adapter;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.eco_print.R;
import com.example.eco_print.models.Article;

import java.util.ArrayList;
import java.util.List;

public class NewsAdapter
        extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private final List<Article> articles = new ArrayList<>();

    public void submitArticles(List<Article> newArticles) {
        articles.clear();

        if (newArticles != null) {
            articles.addAll(newArticles);
        }

        notifyDataSetChanged();
    }

    public boolean hasArticles() {
        return !articles.isEmpty();
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(
                parent.getContext()
        ).inflate(
                R.layout.news_item,
                parent,
                false
        );

        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull NewsViewHolder holder,
            int position
    ) {
        Article article = articles.get(position);
        Context context = holder.itemView.getContext();

        holder.newsTitle.setText(
                safeText(article.getTitle(), "Environmental update")
        );

        holder.newsDescription.setText(
                safeText(
                        article.getDescription(),
                        "Open the article to read the full story."
                )
        );

        Glide.with(context)
                .load(article.getImage())
                .thumbnail(0.25f)
                .centerCrop()
                .placeholder(R.drawable.news_image_placeholder)
                .fallback(R.drawable.news_image_placeholder)
                .error(R.drawable.news_image_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .transition(
                        DrawableTransitionOptions.withCrossFade(180)
                )
                .into(holder.newsImage);

        View.OnClickListener openArticle = v ->
                openArticle(context, article.getUrl());

        holder.itemView.setOnClickListener(openArticle);
        holder.readMore.setOnClickListener(openArticle);
    }

    @Override
    public void onViewRecycled(@NonNull NewsViewHolder holder) {
        Glide.with(holder.itemView.getContext())
                .clear(holder.newsImage);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    private void openArticle(Context context, String url) {
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(
                    context,
                    "Article link is unavailable",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        try {
            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
            );
            context.startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(
                    context,
                    "No browser is available to open this article",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty()
                ? fallback
                : value.trim();
    }

    static class NewsViewHolder
            extends RecyclerView.ViewHolder {

        private final ImageView newsImage;
        private final TextView newsTitle;
        private final TextView newsDescription;
        private final TextView readMore;

        NewsViewHolder(@NonNull View itemView) {
            super(itemView);

            newsImage = itemView.findViewById(R.id.newsImage);
            newsTitle = itemView.findViewById(R.id.newsTitle);
            newsDescription = itemView.findViewById(
                    R.id.newsDescription
            );
            readMore = itemView.findViewById(R.id.readMore);
        }
    }
}
