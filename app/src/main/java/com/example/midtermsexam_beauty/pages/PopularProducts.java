package com.example.midtermsexam_beauty.pages;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.adapters.CheckOutCard;
import com.example.midtermsexam_beauty.adapters.ProductCard;
import com.example.midtermsexam_beauty.models.Product;

import java.util.ArrayList;

public class PopularProducts extends AppCompatActivity {

    private final ArrayList<Product> popularProducts = new ArrayList<>();

    ImageButton toPrevious, proceedToFeatured, proceedToHome,
              proceedToSkinTypes, proceedToCheckout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_popular_products);

        ListView popularListView = findViewById(R.id.popular_recycler);

        CheckOutCard popularAdapter = new CheckOutCard(this, popularProducts);
        popularListView.setAdapter(popularAdapter);

        toPrevious = findViewById(R.id.back_btn);
        proceedToFeatured = findViewById(R.id.toFeatured);
        proceedToHome = findViewById(R.id.toHome);
        proceedToSkinTypes = findViewById(R.id.toSkinTypes);
        proceedToCheckout = findViewById(R.id.toPayment);

        popularProducts.addAll(Product.getPopularProducts());

        // Navigation Buttons

        toPrevious.setOnClickListener(view -> {
            startActivity(new Intent(PopularProducts.this, Homepage.class));
        });

        proceedToHome.setOnClickListener(view -> {
            Intent intent = new Intent(PopularProducts.this, Homepage.class);
            startActivity(intent);
        });

        proceedToFeatured.setOnClickListener(view -> {
            Intent intent = new Intent(PopularProducts.this, FeaturedProducts.class);
            startActivity(intent);
        });

        proceedToSkinTypes.setOnClickListener(view -> {
            Intent intent = new Intent(PopularProducts.this, SkinType.class);
            startActivity(intent);
        });

        proceedToCheckout.setOnClickListener(view -> {
            Intent intent = new Intent(PopularProducts.this, Checkout.class);
            startActivity(intent);
        });


    }
}