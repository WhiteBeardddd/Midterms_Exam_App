package com.example.midtermsexam_beauty.pages;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.adapters.ProductCard;
import com.example.midtermsexam_beauty.models.Product;

import java.util.ArrayList;
import java.util.List;

public class Homepage extends AppCompatActivity {

    private final ArrayList<Product> popularProducts = new ArrayList<>();
    private final ArrayList<Product> featuredProducts = new ArrayList<>();
    ImageButton proceedToFeatured, proceedToRatings,
            proceedToSkinTypes, proceedToPayment;


    @SuppressLint("SuspiciousIndentation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_homepage);

        RecyclerView featuredListView = findViewById(R.id.featured_recycler);
        RecyclerView popularListView = findViewById(R.id.popular_recycler);

        proceedToSkinTypes = findViewById(R.id.toSkinTypes);
        proceedToPayment = findViewById(R.id.toPayment);
        proceedToRatings = findViewById(R.id.toRatings);
        proceedToFeatured = findViewById(R.id.toFeatured);

        featuredProducts.addAll(Product.getDefaultProducts());
        popularProducts.addAll(Product.getPopularProducts());

        ProductCard featuredAdapter = new ProductCard(this, featuredProducts, product -> {
            Toast.makeText(this, "Clicked: " + product.getName(), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, ViewProductDetails.class);
            intent.putExtra("imageId", product.getImageId());
            intent.putExtra("name", product.getName());
            intent.putExtra("price", product.getPrice());
            intent.putExtra("description", product.getDescription());
            intent.putExtra("rating", product.getRating());
            intent.putExtra("category", product.getCategory());
            intent.putExtra("skin_type", product.getSkin_type());
            startActivity(intent);
        });

        ProductCard popularAdapter = new ProductCard(this, popularProducts,  product -> {
            Toast.makeText(this, "Clicked: " + product.getName(), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, ViewProductDetails.class);
            intent.putExtra("imageId", product.getImageId());
            intent.putExtra("name", product.getName());
            intent.putExtra("price", product.getPrice());
            intent.putExtra("description", product.getDescription());
            intent.putExtra("rating", product.getRating());
            intent.putExtra("category", product.getCategory());
            intent.putExtra("skin_type", product.getSkin_type());
            startActivity(intent);
        });

        featuredListView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        popularListView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        featuredListView.setAdapter(featuredAdapter);
        popularListView.setAdapter(popularAdapter);



        // Navigation Buttons

        proceedToRatings.setOnClickListener(view -> {
            Intent intent = new Intent(Homepage.this, PopularProducts.class);
            startActivity(intent);
        });

        proceedToRatings.setOnClickListener(view -> {
            Intent intent = new Intent(Homepage.this, PopularProducts.class);
            startActivity(intent);
        });

        proceedToFeatured.setOnClickListener(view -> {
            Intent intent = new Intent(Homepage.this, FeaturedProducts.class);
            startActivity(intent);
        });

        proceedToSkinTypes.setOnClickListener(view -> {
            Intent intent = new Intent(Homepage.this, SkinType.class);
            startActivity(intent);
        });

        proceedToPayment.setOnClickListener(view -> {
            Intent intent = new Intent(Homepage.this, Checkout.class);
            startActivity(intent);
        });
    }
}

