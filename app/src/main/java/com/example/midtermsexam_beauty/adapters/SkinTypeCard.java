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

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.models.Product;
import com.example.midtermsexam_beauty.pages.ViewProductDetails;

import java.util.ArrayList;
import java.util.List;

public class SkinTypeCard extends RecyclerView.Adapter<SkinTypeCard.ViewHolder> {

    private final Context context;
    private final List<Product> productList;

    public SkinTypeCard(Context context, List<Product> productList) {
        this.context = context;
        this.productList = new ArrayList<>(productList);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.skin_type_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public void updateList(List<Product> newList) {
        productList.clear();
        productList.addAll(newList);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView displayImage;
        TextView productName, productPrice, productDescription, ratings, productCategory, productSkinType;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            displayImage = itemView.findViewById(R.id.display_image);
            productName = itemView.findViewById(R.id.product_name);
            productPrice = itemView.findViewById(R.id.product_price);
            productDescription = itemView.findViewById(R.id.product_description);
            ratings = itemView.findViewById(R.id.ratings);
            productCategory = itemView.findViewById(R.id.productCategory);
            productSkinType = itemView.findViewById(R.id.productSkinType);
        }

        public void bind(Product product) {
            displayImage.setImageResource(product.getImageId());
            productName.setText(product.getName());
            productPrice.setText(String.format("₱%.2f", product.getPrice()));
            productDescription.setText(product.getDescription());
            ratings.setText("Rating: " + product.getRating());
            productCategory.setText("Category: " + product.getCategory());
            productSkinType.setText("Skintype: " + product.getSkin_type());

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ViewProductDetails.class);
                intent.putExtra("imageId", product.getImageId());
                intent.putExtra("name", product.getName());
                intent.putExtra("price", product.getPrice());
                intent.putExtra("description", product.getDescription());
                intent.putExtra("rating", product.getRating());
                intent.putExtra("category", product.getCategory());
                intent.putExtra("skin_type", product.getSkin_type());
                context.startActivity(intent);
            });
        }
    }
}
