package com.example.eco_print.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eco_print.R;
import com.example.eco_print.models.RewardTransaction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RewardHistoryAdapter
        extends RecyclerView.Adapter<RewardHistoryAdapter.RewardViewHolder> {

    private final List<RewardTransaction> rewards = new ArrayList<>();

    public void setRewards(List<RewardTransaction> newRewards) {
        rewards.clear();

        if (newRewards != null) {
            rewards.addAll(newRewards);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RewardViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(
                        R.layout.reward_item,
                        parent,
                        false
                );

        return new RewardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull RewardViewHolder holder,
            int position
    ) {
        RewardTransaction reward = rewards.get(position);

        holder.pointsText.setText(
                "+" + reward.getPoints() + " ECO POINTS"
        );

        holder.descriptionText.setText(
                safeText(
                        reward.getDescription(),
                        "Eco-Print reward"
                )
        );

        holder.rewardTypeText.setText(
                formatRewardType(reward.getRewardType())
        );

        holder.dateText.setText(
                formatDate(reward.getCreatedAt())
        );

        String reportId = reward.getReportId();

        holder.reportText.setText(
                reportId == null || reportId.trim().isEmpty()
                        ? "Report unavailable"
                        : "Report: " + reportId
        );
    }

    @Override
    public int getItemCount() {
        return rewards.size();
    }

    private String formatRewardType(String rewardType) {

        if (rewardType == null
                || rewardType.trim().isEmpty()) {
            return "REWARD";
        }

        return rewardType
                .replace('_', ' ')
                .toUpperCase(Locale.US);
    }

    private String safeText(
            String value,
            String fallback
    ) {
        return value == null || value.trim().isEmpty()
                ? fallback
                : value.trim();
    }

    private String formatDate(String rawDate) {

        if (rawDate == null || rawDate.trim().isEmpty()) {
            return "Date unavailable";
        }

        String datePart = rawDate.length() >= 19
                ? rawDate.substring(0, 19)
                : rawDate;

        SimpleDateFormat input =
                new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss",
                        Locale.US
                );

        SimpleDateFormat output =
                new SimpleDateFormat(
                        "dd MMM yyyy, hh:mm a",
                        Locale.getDefault()
                );

        try {
            Date date = input.parse(datePart);

            return date == null
                    ? rawDate
                    : output.format(date);

        } catch (ParseException ignored) {
            return rawDate;
        }
    }

    static class RewardViewHolder
            extends RecyclerView.ViewHolder {

        TextView pointsText;
        TextView rewardTypeText;
        TextView descriptionText;
        TextView reportText;
        TextView dateText;

        RewardViewHolder(@NonNull View itemView) {
            super(itemView);

            pointsText =
                    itemView.findViewById(R.id.pointsText);

            rewardTypeText =
                    itemView.findViewById(R.id.rewardTypeText);

            descriptionText =
                    itemView.findViewById(R.id.descriptionText);

            reportText =
                    itemView.findViewById(R.id.reportText);

            dateText =
                    itemView.findViewById(R.id.dateText);
        }
    }
}