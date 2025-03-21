package com.example.midtermsexam_beauty.pages;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.adapters.CheckOutCard;
import com.example.midtermsexam_beauty.models.Product;

import java.util.ArrayList;

public class FeaturedProducts extends AppCompatActivity {

    private final ArrayList<Product> featuredProducts = new ArrayList<>();

    ImageButton toPrevious, proceedToRatings, proceedToHome,
            proceedToSkinTypes, proceedToCheckout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_featured_products);

        toPrevious = findViewById(R.id.back_btn);
        proceedToRatings = findViewById(R.id.toRatings);
        proceedToHome = findViewById(R.id.toHome);
        proceedToSkinTypes = findViewById(R.id.toSkinTypes);
        proceedToCheckout = findViewById(R.id.toPayment);


        ListView featuredListView = findViewById(R.id.featured_recycler);

        CheckOutCard popularAdapter = new CheckOutCard(this, featuredProducts);
        featuredListView.setAdapter(popularAdapter);

        featuredProducts.addAll(Product.getDefaultProducts());

        // Navigation Buttons

        toPrevious.setOnClickListener(view -> {
            startActivity(new Intent(FeaturedProducts.this, Homepage.class));
        });

        proceedToHome.setOnClickListener(view -> {
            Intent intent = new Intent(FeaturedProducts.this, Homepage.class);
            startActivity(intent);
        });

        proceedToRatings.setOnClickListener(view -> {
            Intent intent = new Intent(FeaturedProducts.this, PopularProducts.class);
            startActivity(intent);
        });

        proceedToSkinTypes.setOnClickListener(view -> {
            Intent intent = new Intent(FeaturedProducts.this, SkinType.class);
            startActivity(intent);
        });

        proceedToCheckout.setOnClickListener(view -> {
            Intent intent = new Intent(FeaturedProducts.this, Checkout.class);
            startActivity(intent);
        });
    }
}