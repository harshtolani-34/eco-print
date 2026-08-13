package com.example.eco_print.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eco_print.R;
import com.example.eco_print.models.UserProfile;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminCollectorAdapter
        extends RecyclerView.Adapter<AdminCollectorAdapter.CollectorViewHolder> {

    public interface OnCollectorClickListener {
        void onCollectorClick(UserProfile profile);
    }

    private final List<UserProfile> collectors = new ArrayList<>();
    private final OnCollectorClickListener listener;

    public AdminCollectorAdapter(OnCollectorClickListener listener) {
        this.listener = listener;
    }

    public void setCollectors(List<UserProfile> newCollectors) {
        collectors.clear();
        if (newCollectors != null) {
            collectors.addAll(newCollectors);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CollectorViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.admin_collector_item, parent, false);
        return new CollectorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull CollectorViewHolder holder,
            int position
    ) {
        UserProfile profile = collectors.get(position);
        String status = safeText(profile.getCollectorStatus(), "Pending");

        holder.nameText.setText(
                safeText(profile.getFullName(), "Unnamed collector")
        );
        holder.statusText.setText(status.toUpperCase(Locale.US));
        applyStatusStyle(holder.statusText, status);
        holder.emailText.setText(
                safeText(profile.getEmail(), "Email unavailable")
        );
        holder.companyCodeText.setText(
                "Company code: "
                        + safeText(profile.getCompanyCode(), "Not provided")
        );
        holder.dateText.setText(
                "Applied: " + formatDate(profile.getCreatedAt())
        );

        holder.itemView.setOnClickListener(v ->
                listener.onCollectorClick(profile)
        );
    }

    @Override
    public int getItemCount() {
        return collectors.size();
    }

    private void applyStatusStyle(TextView view, String status) {
        int color;
        if ("approved".equalsIgnoreCase(status)) {
            color = Color.parseColor("#7CFF5B");
        } else if ("rejected".equalsIgnoreCase(status)) {
            color = Color.parseColor("#FF5C5C");
        } else {
            color = Color.parseColor("#FFC857");
        }

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.argb(
                38,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        ));
        background.setStroke(dpToPx(view, 1), color);
        background.setCornerRadius(dpToPx(view, 50));
        view.setBackground(background);
        view.setTextColor(color);
    }

    private String safeText(String value, String fallback) {
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
        SimpleDateFormat input = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                Locale.US
        );
        SimpleDateFormat output = new SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                Locale.getDefault()
        );

        try {
            Date date = input.parse(datePart);
            return date == null ? rawDate : output.format(date);
        } catch (ParseException ignored) {
            return rawDate;
        }
    }

    private int dpToPx(View view, int dp) {
        return Math.round(dp * view.getResources()
                .getDisplayMetrics().density);
    }

    static class CollectorViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView statusText;
        private final TextView emailText;
        private final TextView companyCodeText;
        private final TextView dateText;

        CollectorViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.nameText);
            statusText = itemView.findViewById(R.id.statusText);
            emailText = itemView.findViewById(R.id.emailText);
            companyCodeText = itemView.findViewById(R.id.companyCodeText);
            dateText = itemView.findViewById(R.id.dateText);
        }
    }
}
