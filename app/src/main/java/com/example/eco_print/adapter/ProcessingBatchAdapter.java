package com.example.eco_print.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eco_print.R;
import com.example.eco_print.models.ProcessingBatch;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProcessingBatchAdapter
        extends RecyclerView.Adapter<ProcessingBatchAdapter.ViewHolder> {

    public interface OnBatchClickListener {
        void onOpenBatch(ProcessingBatch batch);
    }

    private final List<ProcessingBatch> batches = new ArrayList<>();
    private final OnBatchClickListener listener;

    public ProcessingBatchAdapter(OnBatchClickListener listener) {
        this.listener = listener;
    }

    public void setBatches(List<ProcessingBatch> newBatches) {
        batches.clear();
        if (newBatches != null) {
            batches.addAll(newBatches);
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
                        R.layout.item_processing_batch,
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
        ProcessingBatch batch = batches.get(position);

        holder.typeText.setText(
                safe(batch.getPlasticType(), "Plastic")
        );

        holder.stageText.setText(
                safe(batch.getStage(), "Received")
        );

        holder.quantityText.setText(
                String.format(
                        Locale.getDefault(),
                        "Input %.2f kg • Usable %.2f kg • Rejected %.2f kg",
                        batch.getInputWeightKg(),
                        batch.getUsableWeightKg(),
                        batch.getRejectedWeightKg()
                )
        );

        holder.batchIdText.setText(
                "Batch: " + shortId(batch.getId())
        );

        holder.openButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOpenBatch(batch);
            }
        });
    }

    @Override
    public int getItemCount() {
        return batches.size();
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty()
                ? fallback
                : value.trim();
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
        TextView stageText;
        TextView quantityText;
        TextView batchIdText;
        MaterialButton openButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            typeText = itemView.findViewById(R.id.typeText);
            stageText = itemView.findViewById(R.id.stageText);
            quantityText = itemView.findViewById(R.id.quantityText);
            batchIdText = itemView.findViewById(R.id.batchIdText);
            openButton = itemView.findViewById(R.id.openButton);
        }
    }
}
