package com.example.midtermsexam_beauty.views.user;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.adapters.NavbarCard;
import com.example.midtermsexam_beauty.adapters.ProductCard;
import com.example.midtermsexam_beauty.adapters.RestaurantFeedAdapter;
import com.example.midtermsexam_beauty.adapters.SellerCard;
import com.example.midtermsexam_beauty.models.Product;
import com.example.midtermsexam_beauty.models.SellerProfile;
import com.example.midtermsexam_beauty.utilities.SessionManager;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Homepage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        RecyclerView featuredListView = findViewById(R.id.featured_recycler);
        RecyclerView nearbyListView = findViewById(R.id.popular_recycler);
        EditText searchEditText = findViewById(R.id.searchEditText);

        NavbarCard.setupNavbar(this);

        // LISTENER: Passes the dynamic image URL to the details page
        ProductCard.OnItemClickListener listener = product -> {
            Intent intent = new Intent(this, ViewProductDetails.class);
            intent.putExtra("imageId", product.getImageID());
            intent.putExtra("name", product.getName());
            intent.putExtra("price", product.getPrice());
            intent.putExtra("description", product.getDescription());
            intent.putExtra("rating", product.getRating());
            intent.putExtra("category", product.getCategory());
            intent.putExtra("skin_type", product.getSkin_type());
            intent.putExtra("availability", product.getAvalability());
            intent.putExtra("sellerId", product.getSellerId());
            intent.putExtra("imageUrl", product.getImageUrl()); // <- The crucial new line!
            startActivity(intent);
        };

        featuredListView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        nearbyListView.setLayoutManager(new LinearLayoutManager(this));

        SessionManager session = new SessionManager(this);
        SupabaseAuthService supabase = new SupabaseAuthService();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            // Fetches all Featured Shop
            List<SellerProfile> featuredShops = supabase.getFeaturedShops(session.getToken());

            runOnUiThread(() -> {
                SellerCard adapter = new SellerCard(this, featuredShops, seller -> {
                    Intent intent = new Intent(this, ViewProductDetails.class);

                    intent.putExtra("sellerId", seller.getId());
                    intent.putExtra("name", seller.getStoreName());
                    intent.putExtra("description", seller.getDescription());
                    intent.putExtra("address", seller.getAddress());
                    intent.putExtra("isOpen", seller.isOpen());
                    intent.putExtra("imageUrl", seller.getSellerAvatarUrl());
                    intent.putExtra("backgroundUrl", seller.getSellerProfileBg());

                    startActivity(intent);
                });

                featuredListView.setAdapter(adapter);
            });
        });

        executor.execute(() -> {
            // Fetches all shops and their avatar_urls from Supabase
            List<Product> dynamicShops = supabase.getAllShops(session.getToken());
            runOnUiThread(() -> {
                RestaurantFeedAdapter popularAdapter = new RestaurantFeedAdapter(this, dynamicShops, listener);
                nearbyListView.setAdapter(popularAdapter);
            });
        });

        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                startActivity(new Intent(this, PopularProducts.class));
                v.clearFocus();
            }
        });
    }
}