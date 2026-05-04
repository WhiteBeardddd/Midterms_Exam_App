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

public class ProductCard extends RecyclerView.Adapter<ProductCard.ViewHolder> {
    private static final int[] ETA_MINUTES = {16, 20, 24, 28, 18, 22, 26, 30};
    private static final int[] REVIEW_COUNTS = {94, 127, 88, 156, 73, 112, 205, 61};

    private final Context context;
    private final List<Product> productList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Product product);
    }

    public ProductCard(Context context, List<Product> productList, OnItemClickListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.product_card_adapter, parent, false);
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
        private final TextView productDescription;
        private final TextView productCategory;
        private final TextView productAvailability;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            productName = itemView.findViewById(R.id.product_name);
            productRating = itemView.findViewById(R.id.rating_text);
            productMeta = itemView.findViewById(R.id.product_meta);
            productDescription = itemView.findViewById(R.id.product_description);
            productCategory = itemView.findViewById(R.id.product_category);
            productAvailability = itemView.findViewById(R.id.product_price);
        }

        void bind(Product product, int position, OnItemClickListener listener) {
            productImage.setImageResource(product.getImageID());
            productName.setText(product.getName());
            productRating.setText(ShopCardFormatter.buildRatingLabel(product.getRating(), position, REVIEW_COUNTS));
            productMeta.setText(ShopCardFormatter.buildMetaLabel(product.getCategory(), position, ETA_MINUTES));
            productDescription.setText(product.getDescription());
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
