package com.example.eco_print.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.eco_print.R;
import com.example.eco_print.ReportDetailsActivity;
import com.example.eco_print.models.WasteReport;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportHistoryAdapter
        extends RecyclerView.Adapter<ReportHistoryAdapter.ReportViewHolder> {

    private final List<WasteReport> reports = new ArrayList<>();

    public void setReports(List<WasteReport> newReports) {
        reports.clear();
        if (newReports != null) {
            reports.addAll(newReports);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.report_item, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ReportViewHolder holder,
            int position
    ) {
        WasteReport report = reports.get(position);
        Context context = holder.itemView.getContext();

        holder.plasticTypeText.setText(
                formatWasteType(report.getWasteType())
        );
        holder.weightText.setText(
                formatWeight(report.getEstimatedWeightKg())
        );
        holder.dateText.setText(formatDate(report.getCreatedAt()));
        holder.statusText.setText(
                safeText(report.getStatus(), "Pending")
        );
        holder.addressPreviewText.setText(
                safeText(report.getAddress(), "Location unavailable")
        );

        applyStatusStyle(holder.statusText, report.getStatus());

        Glide.with(context)
                .load(report.getImageUrl())
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(holder.reportImage);

        holder.itemView.setOnClickListener(v -> {
            if (report.getId() == null || report.getId().trim().isEmpty()) {
                return;
            }

            Intent intent = new Intent(
                    context,
                    ReportDetailsActivity.class
            );
            intent.putExtra(
                    ReportDetailsActivity.EXTRA_REPORT_ID,
                    report.getId()
            );
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    private String formatWasteType(String wasteType) {
        if (wasteType == null
                || wasteType.trim().isEmpty()
                || wasteType.equalsIgnoreCase("Not specified")) {
            return "Plastic type not provided";
        }
        return wasteType;
    }

    private String formatWeight(double weight) {
        if (weight <= 0) {
            return "Weight not provided";
        }
        return String.format(
                Locale.getDefault(),
                "%.2f kg",
                weight
        );
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty()
                ? fallback
                : value;
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
            if (date != null) {
                return output.format(date);
            }
        } catch (ParseException ignored) {
        }

        return rawDate;
    }

    private void applyStatusStyle(TextView view, String status) {
        int color = getStatusColor(status);

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

    private int getStatusColor(String status) {
        String value = status == null
                ? ""
                : status.trim().toLowerCase(Locale.US);

        switch (value) {
            case "verified":
                return Color.parseColor("#4DA3FF");
            case "assigned":
                return Color.parseColor("#B388FF");
            case "in progress":
                return Color.parseColor("#FF9F43");
            case "collected":
                return Color.parseColor("#7CFF5B");
            case "rejected":
                return Color.parseColor("#FF5C5C");
            case "pending":
            case "reported":
            default:
                return Color.parseColor("#FFC857");
        }
    }

    private int dpToPx(View view, int dp) {
        return Math.round(dp * view.getResources()
                .getDisplayMetrics().density);
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        private final ImageView reportImage;
        private final TextView plasticTypeText;
        private final TextView statusText;
        private final TextView weightText;
        private final TextView dateText;
        private final TextView addressPreviewText;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            reportImage = itemView.findViewById(R.id.reportImage);
            plasticTypeText = itemView.findViewById(R.id.plasticTypeText);
            statusText = itemView.findViewById(R.id.statusText);
            weightText = itemView.findViewById(R.id.weightText);
            dateText = itemView.findViewById(R.id.dateText);
            addressPreviewText = itemView.findViewById(
                    R.id.addressPreviewText
            );
        }
    }
}
