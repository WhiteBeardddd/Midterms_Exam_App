package com.example.midtermsexam_beauty.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.models.Product;

import java.util.List;
import java.util.Locale;

public class PopularAndFeaturedAdapter extends BaseAdapter {
    private final Context context;
    private final List<Product> productList;

    public PopularAndFeaturedAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
    }

    @Override
    public int getCount() { return productList.size(); }

    @Override
    public Object getItem(int position) { return productList.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.featured_and_popular_adapter, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Product product = productList.get(position);

        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(product.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.product_1)
                    .into(holder.productImage);
        } else {
            holder.productImage.setImageResource(product.getImageID());
        }

        holder.productName.setText(product.getName());
        holder.productPrice.setText(String.format(Locale.US, "₱%.2f", product.getPrice()));
        holder.productDescription.setText(product.getDescription());
        holder.productCategory.setText(product.getCategory());

        String bottomText = product.getShopName() != null ? "Shop: " + product.getShopName() : "Available now";
        holder.productSkinType.setText(bottomText);

        return convertView;
    }

    private static class ViewHolder {
        ImageView productImage;
        TextView productName, productPrice, productDescription, productCategory, productSkinType;

        ViewHolder(View view) {
            productImage = view.findViewById(R.id.large_product_image);
            productName = view.findViewById(R.id.product_name);
            productPrice = view.findViewById(R.id.product_price);
            productDescription = view.findViewById(R.id.product_description);
            productCategory = view.findViewById(R.id.product_category);
            productSkinType = view.findViewById(R.id.product_skin_type);
        }
    }
}