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
import java.util.Locale;

public class RestaurantFeedAdapter extends RecyclerView.Adapter<RestaurantFeedAdapter.ViewHolder> {

    private final Context context;
    private final List<Product> restaurantList;
    private final ProductCard.OnItemClickListener listener;

    public RestaurantFeedAdapter(Context context, List<Product> restaurantList, ProductCard.OnItemClickListener listener) {
        this.context = context;
        this.restaurantList = restaurantList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Make sure this matches the name of your horizontal/vertical card layout
        View view = LayoutInflater.from(context).inflate(R.layout.item_explore_restaurant_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = restaurantList.get(position);

        holder.restaurantName.setText(product.getName());
        holder.restaurantCategory.setText(product.getCategory());
        holder.restaurantRating.setText(String.format(Locale.US, "%.1f", product.getRating()));
        holder.restaurantDeliveryInfo.setText("Free delivery"); // Or dynamically set this

        // Loads the Shop Logo via Glide
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(product.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.product_1)
                    .into(holder.restaurantImage);
        } else {
            holder.restaurantImage.setImageResource(product.getImageID());
        }

        // Passes the entire product (with the URL) back to the Homepage listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return restaurantList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView restaurantImage;
        TextView restaurantName, restaurantCategory, restaurantRating, restaurantDeliveryInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ensure these IDs match your actual item_explore_restaurant_card.xml
            restaurantImage = itemView.findViewById(R.id.restaurant_image);
            restaurantName = itemView.findViewById(R.id.restaurant_name);
            restaurantCategory = itemView.findViewById(R.id.restaurant_category);
            restaurantRating = itemView.findViewById(R.id.restaurant_rating);
            restaurantDeliveryInfo = itemView.findViewById(R.id.restaurant_delivery_info);
        }
    }
}