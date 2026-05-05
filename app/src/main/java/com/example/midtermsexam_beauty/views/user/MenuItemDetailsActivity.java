package com.example.midtermsexam_beauty.views.user;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide; // NEW: Import Glide
import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.models.Product;
import com.example.midtermsexam_beauty.utilities.ProductManager;

public class MenuItemDetailsActivity extends AppCompatActivity {

    private int currentQuantity = 1;
    private double basePrice = 0.0;

    private String itemName;
    private String itemDesc;
    private String shopName;
    private String imageUrl; // NEW: To hold the image link

    private TextView tvQuantity, tvPrice, tvName, tvDesc;
    private Button btnAddToCart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_item_details);

        // Get Views
        ImageButton btnClose = findViewById(R.id.btn_close);
        ImageButton btnMinus = findViewById(R.id.btn_minus);
        ImageButton btnPlus = findViewById(R.id.btn_plus);
        tvQuantity = findViewById(R.id.tv_quantity);
        tvName = findViewById(R.id.item_name);
        tvDesc = findViewById(R.id.item_description);
        tvPrice = findViewById(R.id.item_price);
        btnAddToCart = findViewById(R.id.btn_add_to_cart);
        ImageView ivImage = findViewById(R.id.item_image);

        // Get Data from Intent
        itemName = getIntent().getStringExtra("item_name");
        itemDesc = getIntent().getStringExtra("item_desc");
        basePrice = getIntent().getDoubleExtra("item_price", 0.0);
        shopName = getIntent().getStringExtra("shop_name");
        imageUrl = getIntent().getStringExtra("image_url"); // NEW: Get Image URL

        // NEW: Load dynamic image with Glide
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.product_1)
                    .into(ivImage);
        } else {
            ivImage.setImageResource(R.drawable.product_1);
        }

        // Set Initial Text
        tvName.setText(itemName);
        tvDesc.setText(itemDesc);
        tvPrice.setText(String.format("₱%.2f", basePrice));
        updateCartButton();

        // Button Listeners
        btnClose.setOnClickListener(v -> finish());

        btnPlus.setOnClickListener(v -> {
            currentQuantity++;
            updateCartButton();
        });

        btnMinus.setOnClickListener(v -> {
            if (currentQuantity > 1) {
                currentQuantity--;
                updateCartButton();
            }
        });

        btnAddToCart.setOnClickListener(v -> addToCartAndFinish());
    }

    private void updateCartButton() {
        tvQuantity.setText(String.valueOf(currentQuantity));
        double totalPrice = basePrice * currentQuantity;
        btnAddToCart.setText(String.format("Add to Cart - ₱%.2f", totalPrice));
    }

    private void addToCartAndFinish() {
        Product productToAdd = new Product(
                R.drawable.product_1,
                itemName,
                itemDesc,
                (float) basePrice,
                shopName,
                true,
                5.0f,
                ""
        );

        // NEW: Attach the dynamic image URL to the cart item
        productToAdd.setImageUrl(imageUrl);

        ProductManager.getInstance().addProduct(productToAdd, currentQuantity);

        Toast.makeText(this, "Added " + currentQuantity + " " + itemName + " to cart", Toast.LENGTH_SHORT).show();
        finish();
    }
}