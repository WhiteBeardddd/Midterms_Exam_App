package com.example.midtermsexam_beauty.views.user;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.adapters.NavbarCard;
import com.example.midtermsexam_beauty.adapters.PopularAndFeaturedAdapter;
import com.example.midtermsexam_beauty.models.Product;
import com.example.midtermsexam_beauty.utilities.SessionManager;
import com.example.midtermsexam_beauty.utilities.SupabaseAuthService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FeaturedProducts extends AppCompatActivity {

    private final ArrayList<Product> featuredProducts = new ArrayList<>();
    private ImageButton toPrevious;
    private PopularAndFeaturedAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_featured_products);

        NavbarCard.setupNavbar(this);

        toPrevious = findViewById(R.id.back_btn);
        ListView featuredListView = findViewById(R.id.featured_recycler);

        // Initialize adapter with empty list first
        adapter = new PopularAndFeaturedAdapter(this, featuredProducts);
        featuredListView.setAdapter(adapter);

        featuredListView.setOnItemClickListener((parent, view, position, id) -> {
            Product product = featuredProducts.get(position);
            openProductDetails(product);
        });

        toPrevious.setOnClickListener(view -> finish());

        // Fetch dynamic shops from backend
        fetchDynamicShops();
    }

    private void fetchDynamicShops() {
        SessionManager session = new SessionManager(this);
        SupabaseAuthService supabase = new SupabaseAuthService();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            List<Product> dynamicShops = supabase.getAllShops(session.getToken());
            runOnUiThread(() -> {
                featuredProducts.clear();
                featuredProducts.addAll(dynamicShops);
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void openProductDetails(Product product) {
        Intent intent = new Intent(this, ViewProductDetails.class);
        intent.putExtra("imageId", product.getImageID());
        intent.putExtra("name", product.getName());
        intent.putExtra("price", product.getPrice());
        intent.putExtra("description", product.getDescription());
        intent.putExtra("rating", product.getRating());
        intent.putExtra("category", product.getCategory());
        intent.putExtra("skin_type", product.getSkin_type());
        intent.putExtra("availability", product.getAvalability());

        // CRITICAL: Pass the seller ID so the details page knows whose menu to fetch
        intent.putExtra("sellerId", product.getSellerId());

        startActivity(intent);
    }
}