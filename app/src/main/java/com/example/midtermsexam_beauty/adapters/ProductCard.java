package com.example.midtermsexam_beauty.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.models.Product;
import com.example.midtermsexam_beauty.utilities.ProductManager;

import java.util.List;

public class ProductCard extends RecyclerView.Adapter<ProductCard.ViewHolder> {
    private final Context context;
    private final List<Product> productList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Product product);
    }
    public ProductCard(Context context, List<Product> productList, OnItemClickListener lis) {
        this.context = context;
        this.productList = productList;
        listener = lis;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.product_card_adapter, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product, listener);

    }

    @Override
    public int getItemCount() {
        return productList.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView displayImage;
        TextView productName, productPrice, productDescription, ratings, counterView;
        Button addBtn, subtractBtn, addToCartBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            displayImage = itemView.findViewById(R.id.display_image);
            productName = itemView.findViewById(R.id.product_name);
            productPrice = itemView.findViewById(R.id.product_price);
            productDescription = itemView.findViewById(R.id.product_description);

            ratings = itemView.findViewById(R.id.ratings);
            counterView = itemView.findViewById(R.id.counter);
            addBtn = itemView.findViewById(R.id.add_btn);
            subtractBtn = itemView.findViewById(R.id.subtract_btn);
            addToCartBtn = itemView.findViewById(R.id.add_to_cart);
        }
        @SuppressLint({"DefaultLocale", "SetTextI18n"})
        public void bind (Product product, OnItemClickListener listener) {
            displayImage.setImageResource(product.getImageId());
            productName.setText(product.getName());
            productPrice.setText(String.format("₱%.2f", product.getPrice()));
            productDescription.setText(product.getDescription());
            ratings.setText("Rating: " + product.getRating());
            counterView.setText(String.valueOf(product.getCounter()));


            addBtn.setOnClickListener(v -> {
                product.setCounter(product.getCounter() + 1);
                counterView.setText(String.valueOf(product.getCounter()));
            });

            subtractBtn.setOnClickListener(v -> {
                if (product.getCounter() > 0) {
                    product.setCounter(product.getCounter() - 1);
                    counterView.setText(String.valueOf(product.getCounter()));
                }
            });

            addToCartBtn.setOnClickListener(v -> {
                if (product.getCounter() <= 0){
                    Toast.makeText(v.getContext(), "Can't buy 0 quantity!", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(v.getContext(), "added to cart!", Toast.LENGTH_SHORT).show();
                    ProductManager.getInstance().addProduct(product);
                }
            });

            itemView.setOnClickListener(v -> listener.onItemClick(product));

        }
    }
}
