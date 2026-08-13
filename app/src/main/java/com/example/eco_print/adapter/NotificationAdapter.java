package com.example.eco_print.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eco_print.R;
import com.example.eco_print.models.EcoNotification;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter
        extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(EcoNotification notification);
    }

    private final List<EcoNotification> notifications =
            new ArrayList<>();

    private final OnNotificationClickListener listener;

    public NotificationAdapter(
            OnNotificationClickListener listener
    ) {
        this.listener = listener;
    }

    public void setNotifications(
            List<EcoNotification> newNotifications
    ) {
        notifications.clear();

        if (newNotifications != null) {
            notifications.addAll(newNotifications);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(
                        R.layout.notification_item,
                        parent,
                        false
                );

        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull NotificationViewHolder holder,
            int position
    ) {
        EcoNotification notification =
                notifications.get(position);

        holder.titleText.setText(
                safeText(
                        notification.getTitle(),
                        "Eco-Print Update"
                )
        );

        holder.messageText.setText(
                safeText(
                        notification.getMessage(),
                        "Your report has been updated."
                )
        );

        holder.dateText.setText(
                formatDate(notification.getCreatedAt())
        );

        holder.typeText.setText(
                formatType(notification.getNotificationType())
        );

        if (notification.isRead()) {

            holder.unreadBadge.setVisibility(View.GONE);

            holder.titleText.setTextColor(
                    Color.WHITE
            );

            holder.itemView.setAlpha(0.72f);

        } else {

            holder.unreadBadge.setVisibility(View.VISIBLE);

            holder.titleText.setTextColor(
                    Color.parseColor("#7CFF5B")
            );

            holder.itemView.setAlpha(1.0f);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    private String formatType(String type) {

        if (type == null || type.trim().isEmpty()) {
            return "UPDATE";
        }

        return type
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

        String datePart =
                rawDate.length() >= 19
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

    static class NotificationViewHolder
            extends RecyclerView.ViewHolder {

        TextView titleText;
        TextView messageText;
        TextView dateText;
        TextView typeText;
        TextView unreadBadge;

        NotificationViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            titleText =
                    itemView.findViewById(
                            R.id.titleText
                    );

            messageText =
                    itemView.findViewById(
                            R.id.messageText
                    );

            dateText =
                    itemView.findViewById(
                            R.id.dateText
                    );

            typeText =
                    itemView.findViewById(
                            R.id.typeText
                    );

            unreadBadge =
                    itemView.findViewById(
                            R.id.unreadBadge
                    );
        }
    }
}