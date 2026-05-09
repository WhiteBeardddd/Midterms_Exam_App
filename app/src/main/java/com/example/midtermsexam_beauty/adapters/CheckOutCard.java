package com.example.midtermsexam_beauty.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.models.Product;

import java.util.List;

public class CheckOutCard extends BaseAdapter {
    private final List<Product> productList;
    private final LayoutInflater inflater;
    private final CartActionListener listener;

    // Interface to communicate changes back to Checkout.java
    public interface CartActionListener {
        void onQuantityChanged(Product product, int newQuantity);
        void onItemDeleted(Product product);
    }

    public CheckOutCard(Context context, List<Product> productList, CartActionListener listener) {
        this.productList = productList;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return productList.size();
    }

    @Override
    public Object getItem(int position) {
        return productList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            // Using the updated item_cart_row.xml layout
            convertView = inflater.inflate(R.layout.item_cart_row, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Product product = productList.get(position);

        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(parent.getContext())
                    .load(product.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.product_1)
                    .into(holder.productImage);
        } else {
            holder.productImage.setImageResource(product.getImageId());
        }

        holder.productName.setText(product.getName());
        holder.productPrice.setText(String.format("₱ %.2f", product.getPrice()));
        holder.productQuantity.setText(String.valueOf(product.getCounter()));

        // Handle Quantity Increase
        holder.btnIncrease.setOnClickListener(v -> {
            int currentQty = product.getCounter();
            listener.onQuantityChanged(product, currentQty + 1);
        });

        // Handle Quantity Decrease
        holder.btnDecrease.setOnClickListener(v -> {
            int currentQty = product.getCounter();
            if (currentQty > 1) { // Prevent reducing to 0 here, force them to use delete button
                listener.onQuantityChanged(product, currentQty - 1);
            }
        });

        // Handle Item Deletion
        holder.btnDelete.setOnClickListener(v -> {
            listener.onItemDeleted(product);
        });

        return convertView;
    }

    private static class ViewHolder {
        ImageView productImage;
        TextView productName, productPrice, productQuantity;
        ImageButton btnIncrease, btnDecrease, btnDelete;

        ViewHolder(View view) {
            productImage = view.findViewById(R.id.cart_item_image);
            productName = view.findViewById(R.id.cart_item_name);
            productPrice = view.findViewById(R.id.cart_item_price);
            productQuantity = view.findViewById(R.id.cart_item_qty);
            btnIncrease = view.findViewById(R.id.btn_increase_qty);
            btnDecrease = view.findViewById(R.id.btn_decrease_qty);
            btnDelete = view.findViewById(R.id.btn_delete_item);
        }
    }
}