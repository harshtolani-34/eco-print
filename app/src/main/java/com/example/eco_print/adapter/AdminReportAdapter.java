package com.example.eco_print.adapter;

import android.content.Context;
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
import com.example.eco_print.models.WasteReport;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminReportAdapter
        extends RecyclerView.Adapter<AdminReportAdapter.ReportViewHolder> {

    public interface OnReportClickListener {
        void onReportClick(WasteReport report);
    }

    private final List<WasteReport> reports = new ArrayList<>();
    private final OnReportClickListener listener;

    public AdminReportAdapter(OnReportClickListener listener) {
        this.listener = listener;
    }

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
                .inflate(R.layout.admin_report_item, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ReportViewHolder holder,
            int position
    ) {
        WasteReport report = reports.get(position);
        Context context = holder.itemView.getContext();

        holder.statusText.setText(safeText(report.getStatus(), "Pending"));
        holder.plasticTypeText.setText(
                safeText(report.getWasteType(), "Plastic type not provided")
        );
        holder.weightText.setText(
                report.getEstimatedWeightKg() <= 0
                        ? "Weight not provided"
                        : String.format(
                        Locale.getDefault(),
                        "%.2f kg",
                        report.getEstimatedWeightKg()
                )
        );
        holder.addressText.setText(
                safeText(report.getAddress(), "Address unavailable")
        );
        holder.dateText.setText(formatDate(report.getCreatedAt()));
        holder.duplicateText.setVisibility(
                report.isPossibleDuplicate() ? View.VISIBLE : View.GONE
        );

        applyStatusStyle(holder.statusText, report.getStatus());

        Glide.with(context)
                .load(report.getImageUrl())
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(holder.reportImage);

        holder.itemView.setOnClickListener(v -> listener.onReportClick(report));
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    private void applyStatusStyle(TextView view, String status) {
        int color = "Pending".equalsIgnoreCase(status)
                ? Color.parseColor("#FFC857")
                : Color.parseColor("#7CFF5B");

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.argb(38, Color.red(color), Color.green(color), Color.blue(color)));
        background.setStroke(dpToPx(view, 1), color);
        background.setCornerRadius(dpToPx(view, 50));
        view.setBackground(background);
        view.setTextColor(color);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String formatDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return "Date unavailable";
        }
        String part = rawDate.length() >= 19 ? rawDate.substring(0, 19) : rawDate;
        SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        SimpleDateFormat output = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        try {
            Date date = input.parse(part);
            return date == null ? rawDate : output.format(date);
        } catch (ParseException ignored) {
            return rawDate;
        }
    }

    private int dpToPx(View view, int dp) {
        return Math.round(dp * view.getResources().getDisplayMetrics().density);
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        private final ImageView reportImage;
        private final TextView statusText;
        private final TextView plasticTypeText;
        private final TextView weightText;
        private final TextView addressText;
        private final TextView dateText;
        private final TextView duplicateText;

        ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            reportImage = itemView.findViewById(R.id.reportImage);
            statusText = itemView.findViewById(R.id.statusText);
            plasticTypeText = itemView.findViewById(R.id.plasticTypeText);
            weightText = itemView.findViewById(R.id.weightText);
            addressText = itemView.findViewById(R.id.addressText);
            dateText = itemView.findViewById(R.id.dateText);
            duplicateText = itemView.findViewById(R.id.duplicateText);
        }
    }
}
