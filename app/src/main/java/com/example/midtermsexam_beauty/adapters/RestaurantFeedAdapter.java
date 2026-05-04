package com.example.midtermsexam_beauty.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.models.Product;

import java.util.List;

public class RestaurantFeedAdapter extends RecyclerView.Adapter<RestaurantFeedAdapter.ViewHolder> {
    private static final int[] ETA_MINUTES = {18, 22, 26, 30, 34, 28, 24, 32, 20, 36};
    private static final int[] REVIEW_COUNTS = {61, 94, 120, 48, 88, 156, 73, 205, 132, 57};

    private final Context context;
    private final List<Product> productList;
    private final ProductCard.OnItemClickListener listener;

    public RestaurantFeedAdapter(Context context, List<Product> productList, ProductCard.OnItemClickListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_explore_restaurant_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(productList.get(position), position, listener);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView productImage;
        private final TextView productName;
        private final TextView productRating;
        private final TextView productMeta;
        private final TextView productCategory;
        private final TextView productAvailability;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.explore_product_image);
            productName = itemView.findViewById(R.id.explore_product_name);
            productRating = itemView.findViewById(R.id.rating_text);
            productMeta = itemView.findViewById(R.id.explore_product_meta);
            productCategory = itemView.findViewById(R.id.explore_product_category);
            productAvailability = itemView.findViewById(R.id.explore_product_availability);
        }

        void bind(Product product, int position, ProductCard.OnItemClickListener listener) {
            productImage.setImageResource(product.getImageID());
            productName.setText(product.getName());
            productRating.setText(ShopCardFormatter.buildRatingLabel(product.getRating(), position, REVIEW_COUNTS));
            productMeta.setText(ShopCardFormatter.buildMetaLabel(product.getCategory(), position, ETA_MINUTES));
            productCategory.setText(product.getCategory());
            productAvailability.setText(product.getAvalability() ? "Open now" : "Currently unavailable");

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(product);
                }
            });
        }
    }
}
