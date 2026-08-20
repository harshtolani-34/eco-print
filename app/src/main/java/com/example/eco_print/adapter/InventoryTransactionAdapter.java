package com.example.eco_print.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eco_print.R;
import com.example.eco_print.models.InventoryTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InventoryTransactionAdapter
        extends RecyclerView.Adapter<InventoryTransactionAdapter.ViewHolder> {

    private final List<InventoryTransaction> transactions = new ArrayList<>();

    public void setTransactions(List<InventoryTransaction> newTransactions) {
        transactions.clear();
        if (newTransactions != null) {
            transactions.addAll(newTransactions);
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
                        R.layout.item_inventory_transaction,
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
        InventoryTransaction transaction = transactions.get(position);

        holder.typeText.setText(
                safe(transaction.getTransactionType(), "TRANSACTION")
        );

        holder.quantityText.setText(
                String.format(
                        Locale.getDefault(),
                        "%.2f kg",
                        transaction.getQuantityKg()
                )
        );

        holder.noteText.setText(
                safe(transaction.getReferenceNote(), "No note")
        );

        holder.dateText.setText(
                safe(transaction.getCreatedAt(), "Date unavailable")
        );
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty()
                ? fallback
                : value.trim();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView typeText;
        TextView quantityText;
        TextView noteText;
        TextView dateText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            typeText = itemView.findViewById(R.id.typeText);
            quantityText = itemView.findViewById(R.id.quantityText);
            noteText = itemView.findViewById(R.id.noteText);
            dateText = itemView.findViewById(R.id.dateText);
        }
    }
}
