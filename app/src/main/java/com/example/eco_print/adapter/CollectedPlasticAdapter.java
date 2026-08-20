package com.example.eco_print.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eco_print.R;
import com.example.eco_print.models.WasteReport;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CollectedPlasticAdapter
        extends RecyclerView.Adapter<CollectedPlasticAdapter.ViewHolder> {

    public interface OnCreateBatchClickListener {
        void onCreateBatch(WasteReport report);
    }

    private final List<WasteReport> reports = new ArrayList<>();
    private final OnCreateBatchClickListener listener;

    public CollectedPlasticAdapter(OnCreateBatchClickListener listener) {
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
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(
                        R.layout.item_collected_plastic,
                        parent,
                        false
                );
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        WasteReport report = reports.get(position);

        String type = report.getWasteType();
        holder.typeText.setText(
                type == null
                        || type.trim().isEmpty()
                        || "Not specified".equalsIgnoreCase(type)
                        ? "Plastic type not provided"
                        : type
        );

        holder.weightText.setText(
                String.format(
                        Locale.getDefault(),
                        "%.2f kg collected",
                        report.getEstimatedWeightKg()
                )
        );

        holder.reportIdText.setText(
                "Report: " + shortId(report.getId())
        );

        holder.dateText.setText(
                report.getCollectedAt() == null
                        || report.getCollectedAt().trim().isEmpty()
                        ? "Collection date unavailable"
                        : report.getCollectedAt()
        );

        holder.createBatchButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCreateBatch(report);
            }
        });
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    private String shortId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return "Unavailable";
        }
        return id.length() <= 8
                ? id
                : id.substring(0, 8) + "...";
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView typeText;
        TextView weightText;
        TextView reportIdText;
        TextView dateText;
        MaterialButton createBatchButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            typeText = itemView.findViewById(R.id.typeText);
            weightText = itemView.findViewById(R.id.weightText);
            reportIdText = itemView.findViewById(R.id.reportIdText);
            dateText = itemView.findViewById(R.id.dateText);
            createBatchButton =
                    itemView.findViewById(R.id.createBatchButton);
        }
    }
}
