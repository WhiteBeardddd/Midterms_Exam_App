package com.example.midtermsexam_beauty.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.models.MenuItem;
import java.util.List;

public class MenuItemAdapter extends RecyclerView.Adapter<MenuItemAdapter.ViewHolder> {

    public interface OnItemAction {
        void onAction(MenuItem item);
    }

    private final List<MenuItem> items;
    private final OnItemAction onEdit;
    private final OnItemAction onDelete;

    public MenuItemAdapter(List<MenuItem> items, OnItemAction onEdit, OnItemAction onDelete) {
        this.items = items;
        this.onEdit = onEdit;
        this.onDelete = onDelete;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_menu_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuItem item = items.get(position);

        holder.tvName.setText(item.getName());
        holder.tvDescription.setText(item.getDescription());
        holder.tvPrice.setText(String.format("₱%.2f", item.getPrice()));
        holder.tvCategory.setText(item.getCategory());

        // Availability status badge
        if (item.isAvailable()) {
            holder.tvStatus.setText("Available");
            holder.tvStatus.setTextColor(0xFF4CAF50);
        } else {
            holder.tvStatus.setText("Unavailable");
            holder.tvStatus.setTextColor(0xFFFF4444);
        }

        // Image
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            holder.ivImage.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(item.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.surface_card)
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setVisibility(View.GONE);
        }

        holder.btnEdit.setOnClickListener(v -> onEdit.onAction(item));
        holder.btnDelete.setOnClickListener(v -> onDelete.onAction(item));
    }

    @Override
    public int getItemCount() { return items.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvDescription, tvPrice, tvCategory, tvStatus;
        Button btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage       = itemView.findViewById(R.id.ivImage);
            tvName        = itemView.findViewById(R.id.tvName);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvPrice       = itemView.findViewById(R.id.tvPrice);
            tvCategory    = itemView.findViewById(R.id.tvCategory);
            tvStatus      = itemView.findViewById(R.id.tvStatus);
            btnEdit       = itemView.findViewById(R.id.btnEdit);
            btnDelete     = itemView.findViewById(R.id.btnDelete);
        }
    }
}