package com.example.midtermsexam_beauty.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.models.Product;

import java.util.List;

public class RestaurantFeedAdapter extends RecyclerView.Adapter<RestaurantFeedAdapter.ViewHolder> {

    private final Context                    context;
    private final List<Product>              restaurantList;
    private final ProductCard.OnItemClickListener listener;

    public RestaurantFeedAdapter(Context context, List<Product> restaurantList, ProductCard.OnItemClickListener listener) {
        this.context        = context;
        this.restaurantList = restaurantList;
        this.listener       = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_explore_restaurant_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = restaurantList.get(position);

        // ── Name ──────────────────────────────────────────────────────────────
        holder.restaurantName.setText(product.getName());

        // ── Description instead of category ───────────────────────────────────
        String desc = product.getDescription();
        holder.restaurantCategory.setText(
                (desc != null && !desc.isEmpty()) ? desc : "No description available"
        );

        // ── Image via Glide ───────────────────────────────────────────────────
        String imageUrl = product.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.product_1)
                    .into(holder.restaurantImage);
        } else {
            holder.restaurantImage.setImageResource(product.getImageID());
        }

        // ── Click ─────────────────────────────────────────────────────────────
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(product);
        });
    }

    @Override
    public int getItemCount() {
        return restaurantList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView restaurantImage;
        TextView  restaurantName;
        TextView  restaurantCategory;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            restaurantImage    = itemView.findViewById(R.id.restaurant_image);
            restaurantName     = itemView.findViewById(R.id.restaurant_name);
            restaurantCategory = itemView.findViewById(R.id.restaurant_category);
        }
    }
}