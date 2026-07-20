package com.example.eco_print.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class CollectorReportAdapter
        extends RecyclerView.Adapter<
        CollectorReportAdapter.ReportViewHolder> {

    public interface OnAcceptClickListener {
        void onAcceptClick(WasteReport report);
    }

    private final List<WasteReport> reports =
            new ArrayList<>();

    private final OnAcceptClickListener listener;

    private String acceptingReportId = "";

    public CollectorReportAdapter(
            OnAcceptClickListener listener
    ) {
        this.listener = listener;
    }

    public void setReports(List<WasteReport> newReports) {
        reports.clear();

        if (newReports != null) {
            reports.addAll(newReports);
        }

        notifyDataSetChanged();
    }

    public void setAcceptingReportId(String reportId) {
        acceptingReportId = reportId == null ? "" : reportId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(
                parent.getContext()
        ).inflate(
                R.layout.collector_report_item,
                parent,
                false
        );

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

        holder.addressText.setText(
                safeText(
                        report.getAddress(),
                        "Address unavailable"
                )
        );

        holder.dateText.setText(
                formatDate(report.getCreatedAt())
        );

        Glide.with(context)
                .load(report.getImageUrl())
                .centerCrop()
                .placeholder(
                        android.R.drawable.ic_menu_gallery
                )
                .error(
                        android.R.drawable.ic_menu_report_image
                )
                .into(holder.reportImage);

        boolean accepting = report.getId() != null
                && report.getId().equals(acceptingReportId);

        holder.acceptButton.setEnabled(!accepting);
        holder.acceptButton.setText(
                accepting ? "ACCEPTING..." : "ACCEPT COLLECTION"
        );

        holder.acceptButton.setOnClickListener(v ->
                listener.onAcceptClick(report)
        );

        holder.itemView.setOnClickListener(v -> {
            if (report.getId() == null
                    || report.getId().trim().isEmpty()) {
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

    private String safeText(
            String value,
            String fallback
    ) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value;
    }

    private String formatDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return "Submission time unavailable";
        }

        String datePart = rawDate.length() >= 19
                ? rawDate.substring(0, 19)
                : rawDate;

        SimpleDateFormat inputFormat =
                new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss",
                        Locale.US
                );

        SimpleDateFormat outputFormat =
                new SimpleDateFormat(
                        "dd MMM yyyy, hh:mm a",
                        Locale.getDefault()
                );

        try {
            Date date = inputFormat.parse(datePart);

            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (ParseException ignored) {
            // Fall back to the original value.
        }

        return rawDate;
    }

    static class ReportViewHolder
            extends RecyclerView.ViewHolder {

        private final ImageView reportImage;
        private final TextView plasticTypeText;
        private final TextView weightText;
        private final TextView addressText;
        private final TextView dateText;
        private final Button acceptButton;

        ReportViewHolder(@NonNull View itemView) {
            super(itemView);

            reportImage =
                    itemView.findViewById(R.id.reportImage);

            plasticTypeText =
                    itemView.findViewById(R.id.plasticTypeText);

            weightText =
                    itemView.findViewById(R.id.weightText);

            addressText =
                    itemView.findViewById(R.id.addressText);

            dateText =
                    itemView.findViewById(R.id.dateText);

            acceptButton =
                    itemView.findViewById(R.id.acceptButton);
        }
    }
}
