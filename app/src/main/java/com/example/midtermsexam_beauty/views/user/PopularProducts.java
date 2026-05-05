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

public class PopularProducts extends AppCompatActivity {

    private final ArrayList<Product> popularProducts = new ArrayList<>();
    private ImageButton toPrevious;
    private PopularAndFeaturedAdapter popularAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popular_products);

        NavbarCard.setupNavbar(this);

        ListView popularListView = findViewById(R.id.popular_recycler);

        // Initialize adapter with empty list first
        popularAdapter = new PopularAndFeaturedAdapter(this, popularProducts);
        popularListView.setAdapter(popularAdapter);

        popularListView.setOnItemClickListener((parent, view, position, id) -> {
            Product product = popularProducts.get(position);
            openProductDetails(product);
        });

        toPrevious = findViewById(R.id.back_btn);
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
                popularProducts.clear();
                popularProducts.addAll(dynamicShops);
                popularAdapter.notifyDataSetChanged();
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