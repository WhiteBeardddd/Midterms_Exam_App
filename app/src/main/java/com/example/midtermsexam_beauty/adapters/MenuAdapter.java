package com.example.midtermsexam_beauty.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.models.MenuItem;
import com.example.midtermsexam_beauty.views.user.MenuItemDetailsActivity;

import java.util.List;
import java.util.Locale;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {
    private final Context context;
    private final List<MenuItem> menuItems;
    private final String shopName;

    public MenuAdapter(Context context, List<MenuItem> menuItems, String shopName) {
        this.context = context;
        this.menuItems = menuItems;
        this.shopName = shopName;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_menu_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuItem item = menuItems.get(position);

        holder.tvName.setText(item.getName());
        holder.tvDesc.setText(item.getDescription() != null ? item.getDescription() : "");
        holder.tvPrice.setText(String.format(Locale.US, "₱%.2f", item.getPrice()));

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.product_1)
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.drawable.product_1);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MenuItemDetailsActivity.class);
            intent.putExtra("item_name", item.getName());
            intent.putExtra("item_desc", item.getDescription());
            intent.putExtra("item_price", item.getPrice());
            intent.putExtra("shop_name", shopName);
            intent.putExtra("image_url", item.getImageUrl());
            intent.putExtra("seller_id", item.getSellerId()); // Pass seller ID
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvDesc, tvPrice, tvRating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.menu_item_image);
            tvName = itemView.findViewById(R.id.menu_item_name);
            tvDesc = itemView.findViewById(R.id.menu_item_description);
            tvPrice = itemView.findViewById(R.id.menu_item_price);
            tvRating = itemView.findViewById(R.id.menu_item_rating);
        }
    }
}