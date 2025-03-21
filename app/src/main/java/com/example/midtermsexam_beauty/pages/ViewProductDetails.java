package com.example.midtermsexam_beauty.pages;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.midtermsexam_beauty.R;

public class ViewProductDetails extends AppCompatActivity {

    ImageView productImage;
    TextView productName, productPrice, productDescription,
    productRating, productCategory, productSkinType;

    ImageButton toPrevious, proceedToFeatured, proceedToHome,
            proceedToRatings, proceedToSkinTypes, proceedToCheckout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_product_details);

        productImage = findViewById(R.id.display_image);
        productName = findViewById(R.id.productName);
        productPrice = findViewById(R.id.productPrice);
        productDescription = findViewById(R.id.productDescription);
        productRating = findViewById(R.id.productRating);
        productCategory = findViewById(R.id.productCategory);
        productSkinType = findViewById(R.id.productSkinType);


        //Navigation//
        toPrevious = findViewById(R.id.back_btn);
        proceedToFeatured = findViewById(R.id.toFeatured);
        proceedToHome = findViewById(R.id.toHome);
        proceedToRatings = findViewById(R.id.toRatings);
        proceedToSkinTypes = findViewById(R.id.toSkinTypes);
        proceedToCheckout = findViewById(R.id.toPayment);


//        if (productImage == null) Log.e("ViewProductDetails", "productImage is null");
//        if (productName == null) Log.e("ViewProductDetails", "productName is null");
//        if (productPrice == null) Log.e("ViewProductDetails", "productPrice is null");
//        if (productDescription == null) Log.e("ViewProductDetails", "productDescription is null");
//        if (productRating == null) Log.e("ViewProductDetails", "productRating is null");
//        if (productCategory == null) Log.e("ViewProductDetails", "productCategory is null");
//        if (productSkinType == null) Log.e("ViewProductDetails", "productSkinType is null");
//        if (backButton == null) Log.e("ViewProductDetails", "backButton is null");

        // Get product details from intent
        Intent intent = getIntent();
            int imageId = intent.getIntExtra("imageId", 0);
            String name = intent.getStringExtra("name");
            float price = intent.getFloatExtra("price", 0.0f);
            String description = intent.getStringExtra("description");
            float rating = intent.getFloatExtra("rating", 0.0f);
            String category = intent.getStringExtra("category");
            String skin_type = intent.getStringExtra("skin_type");

//            intent.putExtra("imageId", product.getImageId());
//            intent.putExtra("name", product.getName());
//            intent.putExtra("price", product.getPrice());
//            intent.putExtra("description", product.getDescription());
//            intent.putExtra("rating", product.getRating());
//            intent.putExtra("category", product.getCategory());
//            intent.putExtra("skin_type", product.getSkin_type());
            // Log the values
//            Log.d("ViewProductDetails", "Image ID: " + imageId);
//            Log.d("ViewProductDetails", "Name: " + name);
//            Log.d("ViewProductDetails", "Price: " + price);
//            Log.d("ViewProductDetails", "Description: " + description);
//            Log.d("ViewProductDetails", "Rating: " + rating);
//            Log.d("ViewProductDetails", "Category: " + category);
//            Log.d("ViewProductDetails", "Skin Type: " + skin_type);

            // Set values
            productImage.setImageResource(imageId);
            productName.setText(name);
            productPrice.setText("₱" + price);
            productDescription.setText(description);
            productRating.setText("Rating: " + rating);
            productCategory.setText("Category: " + category);
            productSkinType.setText("Skin Type: " + skin_type);


        // Navigation
        toPrevious.setOnClickListener(v -> finish());

        proceedToHome.setOnClickListener(view -> {
            finish();
        });

        proceedToRatings.setOnClickListener(view -> {
            startActivity(new Intent(ViewProductDetails.this, PopularProducts.class));
        });

        proceedToFeatured.setOnClickListener(view -> {
            startActivity(new Intent(ViewProductDetails.this, FeaturedProducts.class));
        });

        proceedToSkinTypes.setOnClickListener(view -> {
            startActivity(new Intent(ViewProductDetails.this, SkinType.class));
        });

        proceedToCheckout.setOnClickListener(view -> {
            startActivity(new Intent(ViewProductDetails.this, Checkout.class));
        });
    }
}