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

public class ProductCard extends RecyclerView.Adapter<ProductCard.ViewHolder> {

    private final Context            context;
    private final List<Product>      productList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Product product);
    }

    public ProductCard(Context context, List<Product> productList, OnItemClickListener listener) {
        this.context     = context;
        this.productList = productList;
        this.listener    = listener;
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
        private final TextView  productName;
        private final TextView  productRating;
        private final TextView  productDescription;
        private final TextView  productCategory;
        private final TextView  productAvailability;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage        = itemView.findViewById(R.id.product_image);
            productName         = itemView.findViewById(R.id.product_name);
            productRating       = itemView.findViewById(R.id.rating_text);
            productDescription  = itemView.findViewById(R.id.product_description);
            productCategory     = itemView.findViewById(R.id.product_category);
            productAvailability = itemView.findViewById(R.id.product_price);
        }

        void bind(Product product, int position, OnItemClickListener listener) {
            // ── Image ─────────────────────────────────────────────────────────
            String imageUrl = product.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.product_1)
                        .into(productImage);
            } else {
                productImage.setImageResource(product.getImageID());
            }

            // ── Text fields ───────────────────────────────────────────────────
            productName.setText(product.getName());

            // Shop name as subtitle so buyer knows which restaurant it's from
            String shopName = product.getShopName();
            productDescription.setText(
                    (shopName != null && !shopName.isEmpty()) ? shopName : ""
            );

            // Price
            productAvailability.setText(
                    String.format(Locale.US, "₱%.2f", product.getPrice())
            );


            productCategory.setText(product.getCategory());
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(product);
            });
        }
    }
}