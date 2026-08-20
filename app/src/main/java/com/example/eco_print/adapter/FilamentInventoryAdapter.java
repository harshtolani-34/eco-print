package com.example.eco_print.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eco_print.R;
import com.example.eco_print.models.FilamentInventory;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FilamentInventoryAdapter
        extends RecyclerView.Adapter<FilamentInventoryAdapter.ViewHolder> {

    public interface OnManageStockClickListener {
        void onManageStock(FilamentInventory item);
    }

    private final List<FilamentInventory> items = new ArrayList<>();
    private final OnManageStockClickListener listener;

    public FilamentInventoryAdapter(OnManageStockClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<FilamentInventory> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
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
                        R.layout.item_filament_inventory,
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
        FilamentInventory item = items.get(position);

        holder.materialText.setText(
                safe(item.getFilamentType(), "Filament")
        );

        String colour = item.getColour();
        holder.colourText.setText(
                colour == null || colour.trim().isEmpty()
                        ? "Colour: Not specified"
                        : "Colour: " + colour
        );

        holder.detailsText.setText(
                String.format(
                        Locale.getDefault(),
                        "%.2f mm • %d spool%s",
                        item.getDiameterMm(),
                        item.getSpoolCount(),
                        item.getSpoolCount() == 1 ? "" : "s"
                )
        );

        holder.stockText.setText(
                String.format(
                        Locale.getDefault(),
                        "%.2f kg available",
                        item.getAvailableStockKg()
                )
        );

        holder.statusText.setText(
                safe(item.getStockStatus(), "Available")
        );

        holder.manageButton.setEnabled(
                item.getAvailableStockKg() > 0
        );

        holder.manageButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onManageStock(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty()
                ? fallback
                : value.trim();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView materialText;
        TextView colourText;
        TextView detailsText;
        TextView stockText;
        TextView statusText;
        MaterialButton manageButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            materialText = itemView.findViewById(R.id.materialText);
            colourText = itemView.findViewById(R.id.colourText);
            detailsText = itemView.findViewById(R.id.detailsText);
            stockText = itemView.findViewById(R.id.stockText);
            statusText = itemView.findViewById(R.id.statusText);
            manageButton = itemView.findViewById(R.id.manageButton);
        }
    }
}
