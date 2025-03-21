package com.example.midtermsexam_beauty.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import android.widget.BaseAdapter;
import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.models.Product;

import java.util.List;

public class CheckOutCard extends BaseAdapter {
    private final Context context;
    private final List<Product> productList;
    private final LayoutInflater inflater;

    public CheckOutCard(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
        this.inflater = LayoutInflater.from(context);
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

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.checkout_card_adapter, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Product product = productList.get(position);
        holder.productImage.setImageResource(product.getImageId());
        holder.productName.setText(product.getName());
        holder.productPrice.setText("₱" + product.getPrice());
        holder.productQuantity.setText("Qty: " + product.getCounter());

        return convertView;
    }

    private static class ViewHolder {
        ImageView productImage;
        TextView productName, productPrice, productQuantity;

        ViewHolder(View view) {
            productImage = view.findViewById(R.id.checkout_product_image);
            productName = view.findViewById(R.id.checkout_product_name);
            productPrice = view.findViewById(R.id.checkout_product_price);
            productQuantity = view.findViewById(R.id.checkout_product_quantity);
        }
    }
}
