package com.example.eco_print.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.eco_print.R;
import com.example.eco_print.models.Article;

import java.util.List;

public class NewsAdapter
        extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private final List<Article> articles;

    public NewsAdapter(List<Article> articles) {
        this.articles = articles;
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

        holder.newsTitle.setText(
                article.getTitle()
        );

        holder.newsDescription.setText(
                article.getDescription()
        );

        Glide.with(holder.itemView.getContext())
                .load(article.getImage())
                .into(holder.newsImage);
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    static class NewsViewHolder
            extends RecyclerView.ViewHolder {

        ImageView newsImage;
        TextView newsTitle;
        TextView newsDescription;

        public NewsViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            newsImage =
                    itemView.findViewById(R.id.newsImage);

            newsTitle =
                    itemView.findViewById(R.id.newsTitle);

            newsDescription =
                    itemView.findViewById(
                            R.id.newsDescription
                    );
        }
    }
}